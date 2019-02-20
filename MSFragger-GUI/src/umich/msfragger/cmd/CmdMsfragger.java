package umich.msfragger.cmd;

import static umich.msfragger.util.PathUtils.testFilePath;

import java.awt.Component;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.msfragger.gui.InputLcmsFile;
import umich.msfragger.params.dbslice.DbSlice;
import umich.msfragger.params.fragger.FraggerMigPanel;
import umich.msfragger.params.fragger.MsfraggerParams;
import umich.msfragger.util.PythonInfo;
import umich.msfragger.util.StringUtils;
import umich.msfragger.util.UsageTrigger;

public class CmdMsfragger extends CmdBase {
  private static final Logger log = LoggerFactory.getLogger(CmdMsfragger.class);
  public static final String NAME = "MsFragger";

  public CmdMsfragger(boolean isRun, Path workDir) {
    super(isRun, workDir);
  }

  @Override
  public String getCmdName() {
    return NAME;
  }

  private String getPepxmlFn(InputLcmsFile f, String ext) {
    return StringUtils.upToLastDot(f.path.getFileName().toString()) + "." + ext;
  }

  public Map<InputLcmsFile, Path> outputs(List<InputLcmsFile> inputs, String ext, Path workDir) {
    Map<InputLcmsFile, Path> m = new HashMap<>();
    for (InputLcmsFile f : inputs) {
      String pepxmlFn = getPepxmlFn(f, ext);
      m.put(f, f.outputDir(workDir).resolve(pepxmlFn));
    }
    return m;
  }

  public boolean configure(Component comp, boolean isDryRun,
      FraggerMigPanel fp, Path jarFragpipe, UsageTrigger binFragger, String pathFasta,
      List<InputLcmsFile> lcmsFiles) {

    pbs.clear();
    final int numSlices = fp.getNumDbSlices();
    final boolean isSlicing = numSlices > 1;
    if (isSlicing) {
      // slicing requested
      if (!DbSlice.get().isInitialized()) {
        JOptionPane.showMessageDialog(comp,
            "MSFragger number of DB slices requested was more than 1.\n"
                + "However not all preconditions for enabling slicing were met.\n"
                + "Check the bottom of \"Config\" tab for details.",
            "Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }

    if (StringUtils.isNullOrWhitespace(binFragger.getBin())) {
      JOptionPane
          .showMessageDialog(comp, "Binary for running Fragger can not be an empty string.\n",
              "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    if (testFilePath(binFragger.getBin(), "") == null) {
      JOptionPane
          .showMessageDialog(comp, "Binary for running Fragger not found or could not be run.\n"
                  + "Neither on PATH, nor in the working directory",
              "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    // Fasta file
    if (pathFasta == null) {
      JOptionPane.showMessageDialog(comp, "Fasta file path (Fragger) can't be empty",
          "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    // Search parameter file
    MsfraggerParams params = fp.getParams();
    params.setDatabaseName(pathFasta);
    Path savedParamsPath = wd.resolve(MsfraggerParams.CACHE_FILE);
    if (!isDryRun) {
      try {
        params.save(new FileOutputStream(savedParamsPath.toFile()));
        // cache the params
        params.save();
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(comp,
            "Could not save fragger.params file to working dir.\n",
            "Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }

    int ramGb = fp.getRamGb();

    // 32k symbols splitting for regular command.
    // But for slicing it's all up to the python script.
    //final int commandLenLimit = isSlicing ? Integer.MAX_VALUE : 1 << 15;
    final int commandLenLimit = 1 << 15;

    if (isSlicing) {
      // schedule to always try to delete the temp dir when FragPipe finishes execution
      final String tempDirName = "split_peptide_index_tempdir";
      Path toDelete = wd.resolve(tempDirName).toAbsolutePath().normalize();
      try {
        if (Files.exists(toDelete)) {
          FileUtils.deleteDirectory(toDelete.toFile());
        }
      } catch (IOException e) {
        log.warn("Could not delete leftover temporary directory: {}", toDelete.toString());
      }
      toDelete.toFile().deleteOnExit();
    }

    int fileIndex = 0;
    StringBuilder sb = new StringBuilder();

    final String ext = fp.getOutputFileExt();
    Map<InputLcmsFile, Path> mapLcmsToPepxml = outputs(lcmsFiles, ext, wd);

    final List<String> javaCmd = ramGb > 0 ?
            Arrays.asList("java", "-jar", "-Dfile.encoding=UTF-8", "-Xmx" + ramGb + "G") :
            Arrays.asList("java", "-jar", "-Dfile.encoding=UTF-8");
    final List<String> slicingCmd = isSlicing ?
            Arrays.asList(
                    PythonInfo.get().getCommand(),
                    DbSlice.get().getScriptDbslicingPath().toAbsolutePath().normalize().toString(),
                    Integer.toString(numSlices),
                    "\"" + String.join(" ", javaCmd) + "\"")
            : null;
    while (fileIndex < lcmsFiles.size()) {
      ArrayList<String> cmd = new ArrayList<>();
      if (isSlicing) {
        cmd.addAll(slicingCmd);
      } else {
        cmd.addAll(javaCmd);
      }
      cmd.add(binFragger.useBin());
      cmd.add(savedParamsPath.toString());

      // check if the command length is ok so far
      sb.append(String.join(" ", cmd));
      if (sb.length() > commandLenLimit) {
        JOptionPane.showMessageDialog(comp,
            "MSFragger command line length too large even for a single file.",
            "Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }

      List<InputLcmsFile> addedLcmsFiles = new ArrayList<>();
      while (fileIndex < lcmsFiles.size()) {
        InputLcmsFile f = lcmsFiles.get(fileIndex);
        // if adding this file to the command line will make the command length
        // longer than the allowed maximum, stop adding files
        if (sb.length() + f.path.toString().length() + 1 > commandLenLimit) {
          break;
        }
        sb.append(f.path.toString()).append(" ");
        cmd.add(f.path.toString());
        addedLcmsFiles.add(f);
        fileIndex++;
      }

      ProcessBuilder pb = new ProcessBuilder(cmd);
      PythonInfo.modifyEnvironmentVariablesForPythonSubprocesses(pb);
      pb.directory(wd.toFile());
      pb.environment().put("PYTHONIOENCODING", "utf-8");
      pbs.add(pb);
      sb.setLength(0);

      // move the pepxml files if the output directory is not the same as where
      // the lcms files were
      for (InputLcmsFile f : addedLcmsFiles) {
        Path pepxmlWhereItShouldBe = mapLcmsToPepxml.get(f);
        if (pepxmlWhereItShouldBe == null)
          throw new IllegalStateException("LCMS file mapped to no pepxml file");
        String pepxmlFn = pepxmlWhereItShouldBe.getFileName().toString();
        Path pepxmlAsCreatedByFragger = f.path.getParent().resolve(pepxmlFn);
        if (!pepxmlAsCreatedByFragger.equals(pepxmlWhereItShouldBe)) {
          pbs.addAll(ToolingUtils
              .pbsMoveFiles(jarFragpipe, pepxmlWhereItShouldBe.getParent(),
                  Collections.singletonList(pepxmlAsCreatedByFragger)));
        }
      }
    }

    isConfigured = true;
    return true;
  }

  @Override
  public int getPriority() {
    return 50;
  }
}
