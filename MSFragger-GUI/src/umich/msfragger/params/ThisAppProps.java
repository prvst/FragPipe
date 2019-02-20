/* 
 * Copyright (C) 2018 Dmitry Avtonomov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package umich.msfragger.params;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.text.JTextComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.msfragger.Version;
import umich.msfragger.gui.MsfraggerGuiFrame;
import umich.msfragger.gui.api.SearchTypeProp;
import umich.msfragger.util.BundleUtils;
import umich.msfragger.util.CacheUtils;
import umich.msfragger.util.PathUtils;
import umich.msfragger.util.PropertiesUtils;

public class ThisAppProps extends Properties {

  public static final String PROP_LAB_SITE_URL = "lab.site.url";
  public static final String PROP_MANUSCRIPT_URL = "manuscript.url";
  private static final Logger log = LoggerFactory.getLogger(ThisAppProps.class);
    //private static final Logger log = LoggerFactory.getLogger(ThisAppProps.class);
    public static final String PROP_DB_FILE_IN = "path.db.file.in";
    public static final String PROP_FRAGGER_PARAMS_FILE_IN = "path.fragger.params.file.in";
    public static final String PROP_BINARIES_IN = "path.params.bins.in";
    public static final String PROP_LCMS_FILES_IN = "path.lcms.files.in";
    public static final String PROP_FILE_OUT = "path.file.out";

    public static final String SYS_TEMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String APP_TEMP_DIR = "fragpipe";
    public static final String TEMP_FILE_EXT = ".cache";
    public static final String TEMP_FILE_NAME = "msfragger" + TEMP_FILE_EXT;

    public static final String PROP_BIN_PATH_MSCONVERT = "path.textfield.msconvert";
    public static final String PROP_BIN_PATH_MSFRAGGER = "path.textfield.msfragger";
    public static final String PROP_BIN_PATH_PHILOSOPHER = "path.textfield.peptide-prophet";
  public static final String PROP_BIN_PATH_PYTHON = "path.bin.python";
  public static final String PROP_TEXTFIELD_PATH_PROTEIN_PROPHET = "path.textfield.protein-prophet";
  public static final String PROP_TEXTFIELD_REPORT_ANNOTATE = "report.annotate";
  public static final String PROP_TEXTFIELD_REPORT_FILTER = "report.filter";
  public static final String PROP_TEXTFIELD_REPORT_ABACUS = "report.abacus";
  public static final String PROP_TEXTFIELD_LABELFREE = "report.labelfree";
  public static final String PROP_TEXTFIELD_SEQUENCE_DB = "sequence.db";
  public static final String PROP_TEXTFIELD_DECOY_TAG = "decoy.tag";
  public static final String PROP_CHECKBOX_REPORT_PROTEIN_LEVEL_FDR = "report.proteinlevelfdr";
  public static final String PROP_CHECKBOX_PROCESS_GROUPS_SEPARATELY = "process.groups.separately";
  public static final String PROP_CHECKBOX_REPORT_ABACUS = "report.run.abacus";

  public static final String PROP_TEXT_CMD_PEPTIDE_PROPHET = "peptideprophet.cmd.line.opts";
  public static final String PROP_TEXT_CMD_PROTEIN_PROPHET = "proteinprophet.cmd.line.opts";

  public static final String PROP_MSADJUSTER_USE = "msadjuster.use";
  public static final String PROP_CRYSTALC_USE = "crystalc.use";
  public static final String PROP_SPECLIBGEN_RUN = "speclibgen.run";

  public static final String PROP_MGF_WARNING = "warn.mgf";

  public static final String JAR_FILE_AS_RESOURCE_EXT = ".jazz";
  public static final Path UNPACK_TEMP_SUBDIR = Paths.get("fragpipe");
  public static final String DEFAULT_LCMS_GROUP_NAME = "";

  public static final String PATH_BUNDLE = "umich/msfragger/gui/Bundle";
  public static final List<String> PROPERTIES_URLS = Arrays.asList(
      "https://raw.githubusercontent.com/Nesvilab/FragPipe/master/MSFragger-GUI/src/" + PATH_BUNDLE + ".properties",
      "https://raw.githubusercontent.com/chhh/FragPipe/updates/MSFragger-GUI/src/" + PATH_BUNDLE + ".properties",
      "https://raw.githubusercontent.com/chhh/FragPipe/master/MSFragger-GUI/src/" + PATH_BUNDLE + ".properties"
  );

  private static class Holder {
    private static final Properties propsLocal = PropertiesUtils.initProperties("Bundle.properties", MsfraggerGuiFrame.class);
    private static final Properties propsRemote = PropertiesUtils.initProperties(PROPERTIES_URLS);
    private static final Properties properties;

    static {
      properties = new Properties();
      properties.putAll(propsLocal);
      properties.putAll(propsRemote);
    }

    public static Properties getProperties() {
      return properties;
    }

    public static Properties getLocalProperties() {
      return propsLocal;
    }

    public static Properties getRemoteProperties() {
      return propsRemote;
    }
  }

  public static Properties getProperties() {
    return Holder.getProperties();
  }

  public static Properties getLocalProperties() {
    return Holder.getLocalProperties();
  }

  public static Properties getRemoteProperties() {
    return Holder.getRemoteProperties();
  }

  public static ResourceBundle getLocalBundle() {
    return BundleUtils.getBundle(PATH_BUNDLE);
  }

  public ThisAppProps() {
          this.setProperty(Version.PROP_VER, Version.version());
      }

    public static void clearCache() {
        ThisAppProps thisAppProps = new ThisAppProps();
        thisAppProps.save();
    }
    
    
    /**
     * Tries to load previously saved properties.
     * @return null if the file didn't exist or could not be loaded.
     */
    public static ThisAppProps loadFromTemp()  {
      try {

        final Path path = CacheUtils.locateTempFile(TEMP_FILE_NAME);
        final ThisAppProps props = new ThisAppProps();
        props.load(new FileInputStream(path.toFile()));
        return props;

      } catch (FileNotFoundException ex) {
        return null;
      } catch (IOException ex) {
        log.debug("Could not load properties from temporary directory: {}", ex.getMessage());
      }

      return null;
    }

    
    /**
     * Attempts to set file chooser's directory to the one saved in the property.
     * @param fileChooser
     * @param propName 
     */
    public static void load(String propName, JFileChooser fileChooser) {
        try {
          ThisAppProps thisAppProps = ThisAppProps.loadFromTemp();
          if (thisAppProps == null) {
            return;
          }
          String inputPath = thisAppProps.getProperty(propName);
          if (inputPath != null) {
            File file = Paths.get(inputPath).toFile();
            fileChooser.setCurrentDirectory(file);
          }
        } catch (Exception ignored) {}
    }

    public static void save(String propName, JTextComponent txt) {
        String text = txt.getText().trim();
        if (!text.isEmpty()) {
            ThisAppProps.save(propName, text);
        }
    }

    public static void save(String propName, File file) {
        ThisAppProps thisAppProps = ThisAppProps.loadFromTemp();
        if (thisAppProps == null) {
            thisAppProps = new ThisAppProps();
        }
        thisAppProps.setProperty(propName, file.getAbsolutePath());
        thisAppProps.save();
    }

    public static boolean load(String propName, JTextComponent txt) {
        String cached = ThisAppProps.load(propName);
        if (cached == null) {
            return false;
        }
        txt.setText(cached);
        return true;
    }

    /**
     * Attempts to search for properties in cache, returns the first non-null found.
     * @param props  List of properties to search for.
     * @param locateJar  If no property was found, will try to locate the current jar
     *                   and return its location.
     * @return Null in case path could not be found.
     */
    public static String tryFindPath(List<String> props, boolean locateJar) {
        for (String prop : props) {
            String path = ThisAppProps.load(prop);
            if (path != null) {
                return path;
            }
        }
        if (locateJar) {
            URI thisJarUri = PathUtils.getCurrentJarUri();
            if (thisJarUri != null) {
                return Paths.get(thisJarUri).toString();
            }
        }
        return null;
    }

  public static boolean load(JTextComponent text, String propName) {
      String val = load(propName);
      if (val != null) {
          text.setText(val);
          return true;
      }
      return false;
  }

  public static boolean load(JCheckBox box, String propName) {
      String val = load(propName);
      if (val != null) {
          Boolean bool = Boolean.valueOf(val);
          box.setSelected(bool);
          return true;
      }
      return false;
  }

  public static void save(JCheckBox box, String propName) {
      save(propName, Boolean.toString(box.isSelected()));
  }

  public static void save(JTextComponent text, String propName) {
      save(propName, text.getText().trim());
  }

  public static void loadFromBundle(JTextComponent text, String propName, SearchTypeProp type) {
      final String prop = propName + "." + type.name();
      loadFromBundle(text, prop);
  }

  public static void loadFromBundle(JTextComponent text, String propName) {
      String val = getProperties().getProperty(propName);
      text.setText(val);
      save(propName, val);
  }

  public static void loadFromBundle(JCheckBox checkBox, String propName, SearchTypeProp type) {
      final String prop = propName + "." + type.name();
      loadFromBundle(checkBox, prop);
  }

  public static void loadFromBundle(JCheckBox checkBox, String propName) {
      String val = getProperties().getProperty(propName);
      checkBox.setSelected(Boolean.valueOf(val));
      save(propName, val);
  }

  public static String cacheComments() {
    return Version.PROGRAM_TITLE + " (" + Version.version() + ") runtime properties";
  }

  private static Path getCacheFilePath() {
    return CacheUtils.getTempFile(TEMP_FILE_NAME);
  }

    public void save() {
      try (OutputStream os = Files.newOutputStream(getCacheFilePath())) {
          store(os, cacheComments());
          os.flush();
      } catch (IOException ex) {
          //log.warn("Could not load properties from temporary directory: {}", ex.getMessage());
      }
    }
    
    public static void save(String propName, String propVal) {
        if (propName == null)
            throw new IllegalArgumentException("Property name must be non-null");
        log.debug("ThisAppProps saving property: {} = {} to {}", propName, propVal, getCacheFilePath().toString());
        ThisAppProps thisAppProps = ThisAppProps.loadFromTemp();
        if (thisAppProps == null)
            thisAppProps = new ThisAppProps();
        if (propVal == null || "".equals(propVal))
            thisAppProps.remove(propName);
        else
            thisAppProps.setProperty(propName, propVal);
        thisAppProps.save();
    }
    
    public static String load(String propName) {
        if (propName == null)
            throw new IllegalArgumentException("Property name must be non-null");
        ThisAppProps thisAppProps = ThisAppProps.loadFromTemp();
        if (thisAppProps == null)
            return null;
        return thisAppProps.getProperty(propName);
    }
    
    public static String load(String propName, String defaultVal) {
        if (propName == null)
            throw new IllegalArgumentException("Property name must be non-null");
        ThisAppProps thisAppProps = ThisAppProps.loadFromTemp();
        if (thisAppProps == null)
            return defaultVal;
        return thisAppProps.getProperty(propName, defaultVal);
    }
}
