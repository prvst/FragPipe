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
package umich.msfragger.util;

import com.github.chhh.utils.swing.StringRepresentable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dmitriya
 */
public class SwingUtils {
  private static final Logger log = LoggerFactory.getLogger(SwingUtils.class);
  private static volatile String[] fontNames = null;
  private static volatile Font[] fonts = null;
  private static final Object fontLock = new Object();

  private SwingUtils() {
  }

  public static String getStrVal(Component c) {

    String val;
    if (c instanceof JFormattedTextField) {
      val = ((JFormattedTextField) c).getText();
    } else if (c instanceof JTextField) {
      val = ((JTextField) c).getText();
    } else if (c instanceof JSpinner) {
      val = ((JSpinner) c).getValue().toString();
    } else if (c instanceof JCheckBox) {
      val = Boolean.valueOf(((JCheckBox) c).isSelected()).toString();
    } else if (c instanceof JComboBox) {
      val = ((JComboBox<?>) c).getModel().getSelectedItem().toString();
    } else {
      throw new UnsupportedOperationException("getStrVal() not implemented for type: " + c.getClass().getCanonicalName());
    }

    return val.trim();
  }

  public static void setStrVal(Component c, String val) {
    if (c instanceof JFormattedTextField) {
      ((JFormattedTextField) c).setText(val);
    } else if (c instanceof JTextField) {
      ((JTextField) c).setText(val);
    } else if (c instanceof JCheckBox) {
      ((JCheckBox) c).setSelected(Boolean.valueOf(val));
    } else if (c instanceof JComboBox) {
      ((JComboBox<?>) c).getModel().setSelectedItem(val);
    } else if (c instanceof JSpinner) {
      ((JSpinner) c).setValue(Double.parseDouble(val));
    } else {
      throw new UnsupportedOperationException("setStrVal() not implemented for type: " + c.getClass().getCanonicalName());
    }
  }

  /**
   * Installs a listener to receive notification when the text of any {@code JTextComponent} is
   * changed. Internally, it installs a {@link DocumentListener} on the text component's {@link
   * Document}, and a {@link PropertyChangeListener} on the text component to detect if the {@code
   * Document} itself is replaced.
   *
   * @param text any text component, such as a {@link JTextField} or {@link JTextArea}
   * @param changeListener a listener to receieve {@link ChangeEvent}s when the text is changed; the
   * source object for the events will be the text component
   * @throws NullPointerException if either parameter is null
   *
   * Taken from http://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
   * @author Boann
   */
  public static void addChangeListener(final JTextComponent text,
      final ChangeListener changeListener) {
    if (text == null || changeListener == null) {
      throw new IllegalArgumentException(
          "Both the text component and the change listener need to be non-null");
    }

    final DocumentListener dl = new DocumentListener() {
      private int lastChange = 0, lastNotifiedChange = 0;

      @Override
      public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        lastChange++;

        SwingUtilities.invokeLater(() -> {
          if (lastNotifiedChange != lastChange) {
            lastNotifiedChange = lastChange;
            changeListener.stateChanged(new ChangeEvent(text));
          }
        });
      }
    };

    PropertyChangeListener pcl = e -> {
      Document d1 = (Document) e.getOldValue();
      Document d2 = (Document) e.getNewValue();
      if (d1 != null) {
        d1.removeDocumentListener(dl);
      }
      if (d2 != null) {
        d2.addDocumentListener(dl);
      }
      dl.changedUpdate(null);
    };
    text.addPropertyChangeListener("document", pcl);

    Document d = text.getDocument();
    if (d != null) {
      d.addDocumentListener(dl);
    }
  }

  public static void enableComponents(Container container, boolean enabled) {
    enableComponents(container, enabled, false);
  }

  public static void enableComponents(Container container, boolean enabled, boolean applyToContainer) {
    if (applyToContainer)
      container.setEnabled(enabled);
    Component[] components = container.getComponents();
    for (Component component : components) {
      component.setEnabled(enabled);
//            if (component instanceof JScrollPane) {
//                JScrollPane jsp = (JScrollPane)component;
//                enableComponents(jsp.getViewport(), enable);
//            }
      if (component instanceof Container) {
        enableComponents((Container) component, enabled, applyToContainer);
      }
    }
  }

  /**
   * Creates a non-editable JEditorPane that has the same styling as default JLabels. Hyperlink
   * clicks are opened using the default browser.
   *
   * @param text Your text to be displayed in HTML context. Don't add the opening and closing HTML
   * tags. To include links use the regular A tags.
   */
  public static JEditorPane createClickableHtml(String text) {
    return createClickableHtml(text, true);
  }

  /**
   * Creates a non-editable JEditorPane that has the same styling as default JLabels and with
   * hyperlinks clickable. They will be opened in the system default browser.
   *
   * @param text Your text to be displayed in HTML context. Don't add the opening and closing HTML
   * tags. To include links use the regular A tags.
   * @param handleHyperlinks Add a handler for hyperlinks to be opened in the
   * default system browser.
   */
  public static JEditorPane createClickableHtml(String text, boolean handleHyperlinks) {
    // for copying style
    JLabel label = new JLabel();
    Font font = label.getFont();

    // create some css from the label's font
    StringBuilder style = new StringBuilder("font-family:" + font.getFamily() + ";");
    style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
    style.append("font-size:").append(font.getSize()).append("pt;");

    JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">"
        + text
        + "</body></html>");
    ep.setEditable(false);

    // handle link events
    if (handleHyperlinks) {
      ep.addHyperlinkListener(e -> {
        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
          try {
            openBrowserOrThrow(e.getURL().toURI());
          } catch (URISyntaxException ex) {
            throw new IllegalStateException("Incorrect url/uri", ex);
          }

        }
      });
    }

    return ep;
  }

  /**
   * Tries to open the default browser.
   * @throws IllegalStateException if the operation fails.
   */
  public static void openBrowserOrThrow(URI uri) {
    try {
      Desktop.getDesktop().browse(uri);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not open link in default system browser", ex);
    }
  }

  /**
   * Tries to open the default browser. Does nothing if the operation fails.
   * @param doLog Log the error with slf4j or not.
   */
  public static void openBrowserOrLog(URI uri, boolean doLog) {
    try {
      Desktop.getDesktop().browse(uri);
    } catch (IOException e) {
      if (doLog) {
        log.error("Could not open link in default system browser", e);
      }
    }
  }

  public static boolean isEnabledAndChecked(JCheckBox checkbox) {
    return checkbox.isEnabled() && checkbox.isSelected();
  }

  /**
   * Drills down a {@link Container}, mapping all components that 1) have their name set, 2) are
   * {@link StringRepresentable} and returns the mapping.<br/>
   * Useful for persisting values from Swing windows.
   */
  public static Map<String, String> valuesToMap(Container origin) {
    Map<String, Component> comps = SwingUtils.mapComponentsByName(origin, true);
    Map<String, String> map = new HashMap<>(comps.size());
    for (Entry<String, Component> e : comps.entrySet()) {
      final String name = e.getKey();
      if (name == null || name.isEmpty()) {
        continue;
      }
      final Component comp = e.getValue();
      if (!(comp instanceof StringRepresentable)) {
        log.debug(String
            .format("SwingUtils.valuesToMap() found component of type [%s] by name [%s] which "
                    + "does not implement [%s]",
                comp.getClass().getSimpleName(), comp.getName(),
                StringRepresentable.class.getSimpleName()));
        continue;
      }
      map.put(name, ((StringRepresentable) comp).asString());
    }
    return map;
  }

  /**
   * Sets values for components in a {@link Container}. Components must 1) have their name set,
   * 2) be {@link StringRepresentable}.
   */
  public static void valuesFromMap(Container origin, Map<String, String> map) {
    Map<String, Component> comps = SwingUtils.mapComponentsByName(origin, true);
    for (Entry<String, String> kv : map.entrySet()) {
      final String name = kv.getKey();
      Component component = comps.get(name);
      if (component != null) {
        if (!(component instanceof StringRepresentable)) {
          log.trace(String
              .format("SwingUtils.valuesFromMap() Found component of type [%s] by name [%s] which does not implement [%s]",
                  component.getClass().getSimpleName(), name,
                  StringRepresentable.class.getSimpleName()));
          continue;
        }
        ((StringRepresentable) component).fromString(kv.getValue());
      }
    }
  }

  /**
   * @return Null if none of the fonts are available, otherwise the first available font found.
   */
  public static String checkFontAvailable(String... fontName) {
    final String[] fontsLocal = getAvailableFontNames();
    Font[] availableFonts = getAvailableFonts();
    for (String fontSearched : fontName) {
      for (String fontSystem : fontsLocal) {
        if (fontSystem.equals(fontSearched)) {
          return fontSystem;
        }
      }
    }
    return null;
  }

  public static String[] getAvailableFontNames() {
    String[] fontsLocal = fontNames;
    if (fontsLocal == null) {
      synchronized (fontLock) {
        fontsLocal = fontNames;
        if (fontsLocal == null) {
          GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
          fontNames = fontsLocal = ge.getAvailableFontFamilyNames();

        }
      }
    }
    return fontsLocal;
  }

  public static Font[] getAvailableFonts() {
    Font[] fontsLocal = fonts;
    if (fontsLocal == null) {
      synchronized (fontLock) {
        fontsLocal = fonts;
        if (fontsLocal == null) {
          GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
          fonts = fontsLocal = ge.getAllFonts();
        }
      }
    }
    return fontsLocal;
  }

  /**
   * Traverses from origin down the hierarchy putting all components with names set to
   * a map.
   */
  public static Map<String, Component> mapComponentsByName(Container origin,
      boolean includeOrigin) {
    if (origin == null) {
      return Collections.emptyMap();
    }
    Map<String, Component> map = new HashMap<>();
    ArrayDeque<Component> fifo = new ArrayDeque<>();
    synchronized (origin.getTreeLock()) {
      if (includeOrigin) {
        fifo.addLast(origin);
      } else {
        for (Component c : origin.getComponents()) {
          fifo.addLast(c);
        }
      }
      while (!fifo.isEmpty()) {
        Component c = fifo.removeFirst();
        String name = c.getName();
        if (!StringUtils.isNullOrWhitespace(name)) {
          map.put(name, c);
        }
        if (c instanceof Container) {
          for (Component child: ((Container)c).getComponents()) {
            fifo.addLast(child);
          }
        }
      }
    }
    return map;
  }

  /**
   * Show a message dialog wrapped into a scroll pane.
   * @param parent The parent for the dialog, null is ok.
   * @param component The component to be used as the message.
   */
  public static void showDialog(Component parent, final Component component) {
    JOptionPane.showMessageDialog(parent, wrapInScrollForDialog(component));
  }

  /**
   * Show a message dialog wrapped into a scroll pane.
   * @param parent The parent for the dialog, null is ok.
   * @param component The component to be used as the message.
   */
  public static void showConfirmDialog(Component parent, final Component component) {
    JOptionPane.showConfirmDialog(parent, wrapInScrollForDialog(component));
  }

  /**
   * Wraps the given component in a scroll pane and attaches a hierarchy listener
   * that makes the parent dialog resizeable if the component is attached to a {@link Dialog}.
   * This is mainly for use with {@link JOptionPane#showMessageDialog(Component, Object)} and
   * the likes.
   */
  public static JScrollPane wrapInScrollForDialog(Component component) {
    // wrap a scrollpane around the component
    final JScrollPane scrollPane = new JScrollPane(component);
    // make the dialog resizable
    component.addHierarchyListener(e -> {
      Window window = SwingUtilities.getWindowAncestor(component);
      if (window instanceof java.awt.Dialog) {
        Dialog dialog = (Dialog) window;
        if (!dialog.isResizable()) {
          dialog.setResizable(true);
        }
      }
    });
    return scrollPane;
  }

  /**
   * Sets the uncaught exception handler for the thread this method is invoked in
   * to a handler that shows a Swing GUI message dialog with error stacktrace.
   */
  public static void setUncaughtExceptionHandlerMessageDialog(Component parent) {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw, true));
      String notes = sw.toString();

      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panel.add(new JLabel("Something unexpected happened"), BorderLayout.PAGE_START);
      JTextArea notesArea = new JTextArea(40, 80);
      notesArea.setText(notes);
      JScrollPane notesScroller = new JScrollPane();
      notesScroller.setBorder(BorderFactory.createTitledBorder("Details: "));
      notesScroller.setViewportView(notesArea);
      panel.add(notesScroller, BorderLayout.CENTER);

      //JOptionPane.showMessageDialog(frame, "Some error details:\n\n" + notes, "Error", JOptionPane.ERROR_MESSAGE);
      //JOptionPane.showMessageDialog(frame, panel, "Error", JOptionPane.ERROR_MESSAGE);
      showDialog(parent, panel);
    });
  }

  public static boolean setFileChooserPath(JFileChooser fc, Path path) {
    try {
      if (Files.exists(path)) {
        fc.setCurrentDirectory(path.toFile());
        return true;
      }
    } catch (Exception ignored) {}
    fc.setCurrentDirectory(null);
    return false;
  }

  public static boolean setFileChooserPath(JFileChooser fc, String path) {
    try {
      Path p = Paths.get(path);
      return setFileChooserPath(fc, p);
    } catch (Exception ignored) {}
    fc.setCurrentDirectory(null);
    return false;
  }

  /**
   * Bubbles up the component hierarchy searching for first instance of a {@link JFrame}.
   */
  public static Component findParentFrameForDialog(Component origin) {
    if (origin == null) {
      return null;
    }
    if (origin instanceof JFrame) {
      return origin;
    }

    Container parent = origin.getParent();
    while (parent != null && !(parent instanceof JFrame)) {
      parent = parent.getParent();
    }
    return parent;
  }

  /**
   * Tries to set the LAF to native for the platform. Does nothing if the LAF is not available.
   *
   * @return true if setting the LAF succeeded.
   */
  public static boolean setPlatformLookAndFeel() {
    try {
      String laf = UIManager.getSystemLookAndFeelClassName();
      UIManager.setLookAndFeel(laf);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Tries to set the LAF to native for the platform or Nimbus if failed.
   *
   * @return true if setting the LAF succeeded.
   */
  public static boolean setPlatformLafOrNimbus() {
    if (setPlatformLookAndFeel())
      return true;
    try {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          return true;
        }
      }
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {}
    return false;
  }

  /**
   * Centers a JFrame on screen.
   *
   * @param frame the frame to be centered
   */
  public static void centerFrame(JFrame frame) {
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation(dim.width / 2 - frame.getSize().width / 2,
        dim.height / 2 - frame.getSize().height / 2);
  }

  /**
   * Sets the icons for a frame. These are also used to display icons in the taskbar.
   *
   * @param frame The frame to set the icons for.
   * @param iconPaths The simplest way is to provide just the file names.
   * @param classToFindResources A class relative to which the icons will be searched. This is a
   * kludge to make things more fool-proof.
   */
  public static void setFrameIcons(JFrame frame, java.util.List<String> iconPaths,
      Class<?> classToFindResources) {
    java.util.List<Image> icons = new ArrayList<>();
    for (String iconPath : iconPaths) {
      java.net.URL imgURL = classToFindResources.getResource(iconPath);
      ImageIcon image = new ImageIcon(imgURL);
      icons.add(image.getImage());
    }
    frame.setIconImages(icons);
  }

  /**
   * Nicely closes the frame, respecting its {@code setOnCloseOperation()} settings.
   *
   * @param frame the frame to be closed
   */
  public static void closeFrameNicely(JFrame frame) {
    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
  }

  public static boolean isGraphicalEnvironmentAvailable() {
    boolean headless = true;

    String nm = System.getProperty("java.awt.headless");

    if (nm == null) {
      /* No need to ask for DISPLAY when run in a browser */
      if (System.getProperty("javaplugin.version") != null) {
        headless = Boolean.FALSE;
      } else {
        String osName = System.getProperty("os.name");
        headless = ("Linux".equals(osName) ||
            "SunOS".equals(osName)) && (System.getenv("DISPLAY") == null);
      }
    } else if (nm.equals("true")) {
      headless = Boolean.TRUE;
    } else {
      headless = Boolean.FALSE;
    }

    return !headless;
  }
}
