package umich.msfragger.cmd;

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JOptionPane;
import umich.msfragger.gui.InputLcmsFile;
import umich.msfragger.gui.LcmsFileGroup;
import umich.msfragger.params.speclib.SpecLibGen;
import umich.msfragger.util.OsUtils;
import umich.msfragger.util.PythonInfo;
import umich.msfragger.util.UsageTrigger;

public class CmdSpecLibGen extends CmdBase {

  public static final String NAME = "SpecLibGen";

  public CmdSpecLibGen(boolean isRun, Path workDir) {
    super(isRun, workDir);
  }

  @Override
  public String getCmdName() {
    return NAME;
  }

  public boolean configure(Component comp, UsageTrigger usePhilosopher, Path jarFragpipe,
      Map<LcmsFileGroup, Path> mapGroupsToProtxml, String fastaPath, boolean isRunProteinProphet) {

    pbs.clear();
    final SpecLibGen slg = SpecLibGen.get();
    if (!slg.isInitialized()) {
      JOptionPane.showMessageDialog(comp,
          "Spectral Library Generation scripts did not initialize correctly.",
          "Spectral Library Generation Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    if (mapGroupsToProtxml.size() > 1) {
      int res = JOptionPane.showConfirmDialog(comp,
          "<html>You have more than 1 experiment/group and spectral<br/>"
              + "library generation is turned on. In that case a separate<br/>"
              + "spectral library is created for each group.<br/><br/>"
              + "<b>Select Yes</b> to continue.<br/><br/>"
              + "<b>Select No</b> if you want a single spectral library generated<br/>"
              + "from ALL input files.<br/>"
              + "Change Experiment/Group configuration on LCMS files tab.<br/>"
              + "E.g. press the <i>Clear Experiments</i> button there.",
          "SpecLibGen config warning", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (JOptionPane.YES_OPTION != res) {
        return false;
      }
    }

    for (Entry<LcmsFileGroup, Path> e : mapGroupsToProtxml.entrySet()) {
      final LcmsFileGroup group = e.getKey();
      final Path protxml = e.getValue();
      final Path groupWd = group.outputDir(wd);

      if (!isRunProteinProphet && !Files.exists(protxml)) {
        JOptionPane.showMessageDialog(comp,
            "Protein Prophet not selected and the output directory:\n"
                + "    " + groupWd.toString() + "\n"
                + "does not contain a '" + protxml.getFileName().toString() + "' file.\n\n"
                + "Either uncheck Spectral Library Generation checkbox or enable Protein Prophet.",
            "Spec Lib Gen configuration Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }

      // for current implementation of speclibgen scripts mzml files need to be
      // located next to pepxml files
      for (InputLcmsFile lcms : group.lcmsFiles) {
        if (!groupWd.equals(lcms.path.getParent())) {
          final Path copy = groupWd.resolve(lcms.path.getFileName());
          final boolean isWindows = OsUtils.isWindows();
          if (!Files.exists(copy)) {
            // Directory of LCMS file is different from pepxml file
            // and the file does not yet exist.
            // Copy over the file and schedule for deletion.
            List<ProcessBuilder> pbCopy = ToolingUtils
                .pbsCopyFiles(jarFragpipe, groupWd, Collections.singletonList(lcms.path));
            pbs.addAll(pbCopy);
            copy.toFile().deleteOnExit(); // schedule for deletion
          }
        }
      }

      List<String> cmd = new ArrayList<>();
      cmd.add(slg.getPi().getCommand());
      cmd.add("-u"); // PYTHONUNBUFFERED: when mixing subprocess output with Python output, use this to keep the outputs in order
      cmd.add(slg.getScriptSpecLibGenPath().toString());
      cmd.add(fastaPath);
      cmd.add(groupWd.toString()); // this is "Pep xml directory"
      cmd.add(protxml.toString()); // protxml file
      cmd.add(groupWd.toString()); // output directory
      cmd.add("True"); // overwrite (true/false), optional arg
      cmd.add(usePhilosopher.useBin()); // philosopher binary path (optional)

      ProcessBuilder pb = new ProcessBuilder(cmd);
      PythonInfo.modifyEnvironmentVariablesForPythonSubprocesses(pb);
      pb.directory(groupWd.toFile());
      pb.environment().put("PYTHONIOENCODING", "utf-8");

      pbs.add(pb);
    }

    isConfigured = true;
    return true;
  }
}
