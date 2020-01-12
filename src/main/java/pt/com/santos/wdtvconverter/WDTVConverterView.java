/*
 * WDTVConverterView.java
 */

package pt.com.santos.wdtvconverter;

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import org.jdesktop.application.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import pt.com.santos.util.FileUtilities;
import pt.com.santos.util.system.SystemUtilities;
import pt.com.santos.util.appframework.DefaultFrameView;
import pt.com.santos.util.subtitle.Language;
import pt.com.santos.util.gui.swing.ErrorHandler;
import pt.com.santos.util.gui.swing.JStreamedTextArea;
import pt.com.santos.util.ProcessEvent;
import pt.com.santos.util.ProcessListener;

/**
 * The application's main frame.
 */
public class WDTVConverterView extends DefaultFrameView<WDTVConverterApp> {
    protected ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(256);
    protected StreamHandler sh;

    public class JTextDialog  extends JDialog {
        JTextArea textArea;

        public JTextDialog(String title) {
            this();
            setTitle(title);
        }

        public JTextDialog() {
            super();
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setViewportView(textArea = new JTextArea());
            scrollPane.setPreferredSize(new Dimension(200,200));
            add(scrollPane);
        }

        public void append(String text) {
            textArea.append(text);
        }

        public void setTextFont(Font font) {
            textArea.setFont(font);
        }

        public String getText() {
            return textArea.getText();
        }
        
    }

    public WDTVConverterView(WDTVConverterApp app) {
        super(app);      
        initComponents();
        initMyComponents();
    }

    private void initMyComponents() {
        if (SystemUtilities.USER_OS != null &&
                SystemUtilities.USER_OS.toLowerCase().equals("windows 7")) {
            Icon homeFolderIcon =
                    getResourceMap().getIcon("Application.homeFolderIcon");
            Icon upFolderIcon =
                    getResourceMap().getIcon("Application.upFolderIcon");
            Icon newFolderIcon =
                    getResourceMap().getIcon("Application.newFolderIcon");
            Icon listViewIcon =
                    getResourceMap().getIcon("Application.listViewIcon");
            Icon detailsViewIcon =
                    getResourceMap().getIcon("Application.detailsViewIcon");
            SystemUtilities.useWindows7FileChooserIcons(homeFolderIcon, upFolderIcon,
                    newFolderIcon, listViewIcon, detailsViewIcon);
        }

        getFrame().setResizable(false);

        for (Language lang : WDTVConverterApp.LANGUAGES.values()) {
            DefaultComboBoxModel dcbm = (DefaultComboBoxModel)
                    languageComboBox.getModel();
            dcbm.addElement(lang);
        }

        bitrateComboBox.setSelectedIndex(
                getIndexForBitrate(getTheApplication().getBitrate()));

        tempDirTextField.setText(
                getTheApplication().getTempDirectory().getAbsolutePath());
        convertDTSAC3CheckBox.setSelected(
                getTheApplication().isConvertDTS2AC3());
        keepOTCheckBox.setSelected(getTheApplication().isKeepOriginalTracks());

        getFrame().pack();
        
        p = new ProcessListener() {
            public void processStart(ProcessEvent pe) {
                currentExecutableTextArea.setText("");
                if (pe.getMessage() != null) {
                    String msg = pe.getMessage().trim();
                    if (!msg.matches("(p|P)rogress:?\\s*\\d{1,3}%?")) {
                        currentExecutableTextArea.append(msg + "\n");
                    }
                } else if (pe.getArgument() != null) {
                    currentExecutableTextArea.append(
                            pe.getArgument().toString() + "\n");
                }
            }

            public void processUpdate(ProcessEvent pe) {
                if (pe.getMessage() != null) {
                    String msg = pe.getMessage().trim();
                    if (!msg.matches("(p|P)rogress:?\\s*\\d{1,3}%?")) {
                        currentExecutableTextArea.append(msg + "\n");
                    }
                } else if (pe.getArgument() != null) {
                    currentExecutableTextArea.append(
                            pe.getArgument().toString() + "\n");
                }
            }

            public void processFinish(ProcessEvent pe) {
                if (pe.getMessage() != null) {
                    String msg = pe.getMessage().trim();
                    if (!msg.matches("(p|P)rogress:?\\s*\\d{1,3}%?")) {
                        currentExecutableTextArea.append(msg + "\n");
                    }
                } else if (pe.getArgument() != null) {
                    currentExecutableTextArea.append(
                            pe.getArgument().toString() + "\n");
                }
            }

            public void processInterrupt(ProcessEvent pe) {
                if (pe.getMessage() != null) {
                    String msg = pe.getMessage().trim();
                    if (!msg.matches("(p|P)rogress:?\\s*\\d{1,3}%?")) {
                        currentExecutableTextArea.append(msg + "\n");
                    }
                } else if (pe.getArgument() != null) {
                    currentExecutableTextArea.append(
                            pe.getArgument().toString() + "\n");
                }
            }
        };

        if (SystemUtilities.osIsWindows()) {
            File mkvtoolnix =
                new File((new File(
                SystemUtilities.getLocation(
                WDTVConverterApp.class).getFile()).getParent() +
                SystemUtilities.FILE_SEPARATOR + "mkvtoolnix").replace("%20", " "));
            File eac3toDir =
                new File((new File(
                SystemUtilities.getLocation(
                WDTVConverterApp.class).getFile()).getParent() +
                SystemUtilities.FILE_SEPARATOR + "eac3to"
                        ).replace("%20", " "));

            System.setProperty("mkvtoolnix.path", mkvtoolnix.getAbsolutePath());
            System.setProperty("eac3to.path", eac3toDir.getAbsolutePath());
        }
        
        outputTextArea.setFont(new Font(null, 0, 11));
        currentExecutableTextArea.setFont(new Font(null, 0, 11));
        
        // Redirect err and out to text area
        OutputStream outs =
                ((JStreamedTextArea)outputTextArea).getOutputStream();
        sh = new StreamHandler(outs, new SimpleFormatter()) {

            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }

        };

        WDTVUtils.getLogger().addHandler(sh);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = WDTVConverterApp.getApplication().getMainFrame();
            aboutBox = new WDTVConverterAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        WDTVConverterApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        sourceTextField = new javax.swing.JTextField();
        destinationTextField = new javax.swing.JTextField();
        sourceButton = new javax.swing.JButton();
        destinationButton = new javax.swing.JButton();
        subsScrollPane = new javax.swing.JScrollPane();
        subsTable = new javax.swing.JTable();
        subtitleTextField = new javax.swing.JTextField();
        subtitleButton = new javax.swing.JButton();
        languageComboBox = new javax.swing.JComboBox();
        defaultCheckBox = new javax.swing.JCheckBox();
        sourceLabel = new javax.swing.JLabel();
        destinationLabel = new javax.swing.JLabel();
        additionalSubsLabel = new javax.swing.JLabel();
        audioBitrateLabel = new javax.swing.JLabel();
        bitrateComboBox = new javax.swing.JComboBox();
        kbitsLabel = new javax.swing.JLabel();
        runButton = new javax.swing.JButton();
        languageLabel = new javax.swing.JLabel();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        tempDirLabel = new javax.swing.JLabel();
        tempDirTextField = new javax.swing.JTextField();
        tempDirButton = new javax.swing.JButton();
        convertDTSAC3CheckBox = new javax.swing.JCheckBox();
        keepOTCheckBox = new javax.swing.JCheckBox();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        fileChooser = new javax.swing.JFileChooser();
        outputDialog = new javax.swing.JDialog();
        jSplitPane1 = new javax.swing.JSplitPane();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new pt.com.santos.util.gui.swing.JStreamedTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        currentExecutableTextArea = new javax.swing.JTextArea();

        mainPanel.setName("mainPanel"); // NOI18N

        sourceTextField.setEditable(false);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(pt.com.santos.wdtvconverter.WDTVConverterApp.class).getContext().getResourceMap(WDTVConverterView.class);
        sourceTextField.setText(resourceMap.getString("sourceTextField.text")); // NOI18N
        sourceTextField.setName("sourceTextField"); // NOI18N

        destinationTextField.setEditable(false);
        destinationTextField.setText(resourceMap.getString("destinationTextField.text")); // NOI18N
        destinationTextField.setName("destinationTextField"); // NOI18N

        sourceButton.setIcon(resourceMap.getIcon("sourceButton.icon")); // NOI18N
        sourceButton.setText(resourceMap.getString("sourceButton.text")); // NOI18N
        sourceButton.setName("sourceButton"); // NOI18N
        sourceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sourceButtonActionPerformed(evt);
            }
        });

        destinationButton.setIcon(resourceMap.getIcon("destinationButton.icon")); // NOI18N
        destinationButton.setText(resourceMap.getString("destinationButton.text")); // NOI18N
        destinationButton.setName("destinationButton"); // NOI18N
        destinationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                destinationButtonActionPerformed(evt);
            }
        });

        subsScrollPane.setName("subsScrollPane"); // NOI18N

        subsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "File", "Language", "Default"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        subsTable.setName("subsTable"); // NOI18N
        subsTable.getTableHeader().setReorderingAllowed(false);
        subsScrollPane.setViewportView(subsTable);
        subsTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("subsTable.columnModel.title0")); // NOI18N
        subsTable.getColumnModel().getColumn(1).setResizable(false);
        subsTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("subsTable.columnModel.title1")); // NOI18N
        subsTable.getColumnModel().getColumn(2).setResizable(false);
        subsTable.getColumnModel().getColumn(2).setHeaderValue(resourceMap.getString("subsTable.columnModel.title2")); // NOI18N

        subtitleTextField.setEditable(false);
        subtitleTextField.setText(resourceMap.getString("subtitleTextField.text")); // NOI18N
        subtitleTextField.setName("subtitleTextField"); // NOI18N

        subtitleButton.setIcon(resourceMap.getIcon("subtitleButton.icon")); // NOI18N
        subtitleButton.setText(resourceMap.getString("subtitleButton.text")); // NOI18N
        subtitleButton.setName("subtitleButton"); // NOI18N
        subtitleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subtitleButtonActionPerformed(evt);
            }
        });

        languageComboBox.setModel(new DefaultComboBoxModel());
        languageComboBox.setName("languageComboBox"); // NOI18N

        defaultCheckBox.setText(resourceMap.getString("defaultCheckBox.text")); // NOI18N
        defaultCheckBox.setName("defaultCheckBox"); // NOI18N

        sourceLabel.setText(resourceMap.getString("sourceLabel.text")); // NOI18N
        sourceLabel.setName("sourceLabel"); // NOI18N

        destinationLabel.setText(resourceMap.getString("destinationLabel.text")); // NOI18N
        destinationLabel.setName("destinationLabel"); // NOI18N

        additionalSubsLabel.setText(resourceMap.getString("additionalSubsLabel.text")); // NOI18N
        additionalSubsLabel.setName("additionalSubsLabel"); // NOI18N

        audioBitrateLabel.setText(resourceMap.getString("audioBitrateLabel.text")); // NOI18N
        audioBitrateLabel.setName("audioBitrateLabel"); // NOI18N

        bitrateComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "192", "256", "384", "448", "640" }));
        bitrateComboBox.setName("bitrateComboBox"); // NOI18N
        bitrateComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bitrateComboBoxActionPerformed(evt);
            }
        });

        kbitsLabel.setText(resourceMap.getString("kbitsLabel.text")); // NOI18N
        kbitsLabel.setName("kbitsLabel"); // NOI18N

        runButton.setIcon(resourceMap.getIcon("runButton.icon")); // NOI18N
        runButton.setText(resourceMap.getString("runButton.text")); // NOI18N
        runButton.setName("runButton"); // NOI18N
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });

        languageLabel.setText(resourceMap.getString("languageLabel.text")); // NOI18N
        languageLabel.setName("languageLabel"); // NOI18N

        addButton.setIcon(resourceMap.getIcon("addButton.icon")); // NOI18N
        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        removeButton.setIcon(resourceMap.getIcon("removeButton.icon")); // NOI18N
        removeButton.setText(resourceMap.getString("removeButton.text")); // NOI18N
        removeButton.setName("removeButton"); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        tempDirLabel.setText(resourceMap.getString("tempDirLabel.text")); // NOI18N
        tempDirLabel.setName("tempDirLabel"); // NOI18N

        tempDirTextField.setEditable(false);
        tempDirTextField.setText(resourceMap.getString("tempDirTextField.text")); // NOI18N
        tempDirTextField.setName("tempDirTextField"); // NOI18N

        tempDirButton.setIcon(resourceMap.getIcon("tempDirButton.icon")); // NOI18N
        tempDirButton.setText(resourceMap.getString("tempDirButton.text")); // NOI18N
        tempDirButton.setName("tempDirButton"); // NOI18N
        tempDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tempDirButtonActionPerformed(evt);
            }
        });

        convertDTSAC3CheckBox.setText(resourceMap.getString("convertDTSAC3CheckBox.text")); // NOI18N
        convertDTSAC3CheckBox.setName("convertDTSAC3CheckBox"); // NOI18N
        convertDTSAC3CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                convertDTSAC3CheckBoxActionPerformed(evt);
            }
        });

        keepOTCheckBox.setText(resourceMap.getString("keepOTCheckBox.text")); // NOI18N
        keepOTCheckBox.setName("keepOTCheckBox"); // NOI18N
        keepOTCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keepOTCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(subsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(tempDirLabel)
                        .addContainerGap(482, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(tempDirTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tempDirButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(destinationLabel)
                            .addComponent(sourceLabel)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(destinationTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
                                    .addComponent(sourceTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(destinationButton, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE)
                                    .addComponent(sourceButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                                .addComponent(subtitleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 482, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(subtitleButton)))
                        .addContainerGap())
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(convertDTSAC3CheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(keepOTCheckBox)
                        .addGap(12, 12, 12)
                        .addComponent(audioBitrateLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bitrateComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(kbitsLabel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(languageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
                        .addComponent(languageComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(defaultCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(addButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(additionalSubsLabel)
                        .addContainerGap(490, Short.MAX_VALUE))))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tempDirLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tempDirTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tempDirButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sourceLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sourceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sourceButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(destinationLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(destinationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(destinationButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(convertDTSAC3CheckBox)
                    .addComponent(audioBitrateLabel)
                    .addComponent(bitrateComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(kbitsLabel)
                    .addComponent(keepOTCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(additionalSubsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(subsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(subtitleButton)
                    .addComponent(subtitleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(removeButton)
                    .addComponent(runButton)
                    .addComponent(languageComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(languageLabel)
                    .addComponent(defaultCheckBox)
                    .addComponent(addButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(pt.com.santos.wdtvconverter.WDTVConverterApp.class).getContext().getActionMap(WDTVConverterView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        fileChooser.setName("fileChooser"); // NOI18N

        outputDialog.setTitle(resourceMap.getString("outputDialog.title")); // NOI18N
        outputDialog.setName("outputDialog"); // NOI18N

        jSplitPane1.setDividerLocation(275);
        jSplitPane1.setName("jSplitPane1"); // NOI18N

        outputScrollPane.setName("outputScrollPane"); // NOI18N

        outputTextArea.setColumns(20);
        outputTextArea.setEditable(false);
        outputTextArea.setRows(5);
        outputTextArea.setName("outputTextArea"); // NOI18N
        outputScrollPane.setViewportView(outputTextArea);

        jSplitPane1.setLeftComponent(outputScrollPane);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        currentExecutableTextArea.setColumns(20);
        currentExecutableTextArea.setRows(5);
        currentExecutableTextArea.setName("currentExecutableTextArea"); // NOI18N
        jScrollPane1.setViewportView(currentExecutableTextArea);

        jSplitPane1.setRightComponent(jScrollPane1);

        javax.swing.GroupLayout outputDialogLayout = new javax.swing.GroupLayout(outputDialog.getContentPane());
        outputDialog.getContentPane().setLayout(outputDialogLayout);
        outputDialogLayout.setHorizontalGroup(
            outputDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 572, Short.MAX_VALUE)
        );
        outputDialogLayout.setVerticalGroup(
            outputDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void sourceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceButtonActionPerformed
        File currentDir = fileChooser.getCurrentDirectory();
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(""));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(MKV_FILTER);
        fileChooser.setFileFilter(AVI_FILTER);
        int returnVal = fileChooser.showOpenDialog(this.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentSource = fileChooser.getSelectedFile();
            sourceTextField.setText(currentSource.getAbsolutePath());
        }

        fileChooser.removeChoosableFileFilter(MKV_FILTER);
        fileChooser.removeChoosableFileFilter(AVI_FILTER);
        fileChooser.setAcceptAllFileFilterUsed(true);
    }//GEN-LAST:event_sourceButtonActionPerformed

    private void destinationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_destinationButtonActionPerformed
        File currentDir = fileChooser.getCurrentDirectory();
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(""));
        if (currentSource == null) {
            JOptionPane.showMessageDialog(this.getFrame(),
                    "Source not selected.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dir = currentSource.getParentFile().getAbsolutePath();
        String name = currentSource.getName();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setSelectedFile(new File(dir + 
                SystemUtilities.FILE_SEPARATOR +
                FileUtilities.getNameWithoutExtension(name) + ".mkv"));
        int prevType = fileChooser.getDialogType();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileFilter(MKV_FILTER);
        int returnVal = fileChooser.showSaveDialog(this.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            if (f.equals(currentSource)) {
                JOptionPane.showMessageDialog(this.getFrame(),
                    "Cannot override source. Please selected a diferent "
                    + "location for the destination.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                name = f.getName();
                String ext = FileUtilities.getExtension(name);
                if (ext.isEmpty() || ext.compareToIgnoreCase("mkv") != 0) {
                    String apath = f.getAbsolutePath();
                    currentDestination = new File(apath +
                            (ext.isEmpty() ? ".mkv" : ""));
                } else currentDestination = f;
                destinationTextField.setText(
                        currentDestination.getAbsolutePath());
            }
        }
        fileChooser.removeChoosableFileFilter(MKV_FILTER);
        fileChooser.setDialogType(prevType);
        fileChooser.setAcceptAllFileFilterUsed(true);
    }//GEN-LAST:event_destinationButtonActionPerformed

    private void subtitleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subtitleButtonActionPerformed
        File currentDir = fileChooser.getCurrentDirectory();
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(""));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(SRT_FILTER);
        int returnVal = fileChooser.showOpenDialog(this.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentSubtitle = fileChooser.getSelectedFile();
            subtitleTextField.setText(currentSubtitle.getAbsolutePath());
        }
        fileChooser.removeChoosableFileFilter(SRT_FILTER);
        fileChooser.setAcceptAllFileFilterUsed(true);
    }//GEN-LAST:event_subtitleButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        if (currentSubtitle == null) return;
        DefaultTableModel dtm = (DefaultTableModel) subsTable.getModel();
        Language lang = (Language) languageComboBox.getSelectedItem();
        if (!hasSubtitle(currentSubtitle)) {
            if (defaultCheckBox.isSelected()) cleanDefault();
            dtm.addRow(new Object[]{ currentSubtitle,
                    lang , defaultCheckBox.isSelected() });
        }
    }//GEN-LAST:event_addButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        int index = subsTable.getSelectedRow();
        if (index < 0) return;
        DefaultTableModel dtm = (DefaultTableModel) subsTable.getModel();
        dtm.removeRow(index);
    }//GEN-LAST:event_removeButtonActionPerformed

    private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
        if (currentSource == null) {
            JOptionPane.showMessageDialog(this.getFrame(),
                    "Source not selected.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (currentDestination == null) {
            JOptionPane.showMessageDialog(this.getFrame(),
                    "Destination not selected.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        DefaultTableModel dtm = (DefaultTableModel) subsTable.getModel();
        Vector vv = dtm.getDataVector();
        Iterator it = vv.iterator();
        final Configuration conf = new Configuration();
        conf.setKeepOT(getTheApplication().isKeepOriginalTracks());
        conf.setConvertAC32DTS(getTheApplication().isConvertDTS2AC3());
        while(it.hasNext()) {
            Vector v = (Vector) it.next();
            File theFile = (File) v.get(0);
            Language lang = (Language) v.get(1);
            boolean isDefault = (Boolean) v.get(2);
            conf.addSubtitle(theFile, lang, isDefault);
        }
        conf.setBitRate(getTheApplication().getBitrate());
        new Thread() {
            @Override
            public void run() {
                outputDialog.setModal(false);
                outputDialog.setDefaultCloseOperation(
                        WindowConstants.DO_NOTHING_ON_CLOSE);
                outputTextArea.setText("");
                outputDialog.pack();
                outputDialog.setLocationRelativeTo(getFrame());
                outputDialog.setVisible(true);
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                try {
                    runButton.setEnabled(false);
                    long time = WDTVUtils.convert(currentSource,
                            currentDestination,
                            getTheApplication().getTempDirectory(), conf, p);
                    outputTextArea.setText("");
                    outputDialog.setVisible(false);
                    runButton.setEnabled(true);
                    JOptionPane.showMessageDialog(
                            WDTVConverterView.this.getFrame(),
                            "Completed in " + time/1000.0 + "s.",
                            "Done", JOptionPane.INFORMATION_MESSAGE);
                } catch(Exception e) {
                    ErrorHandler.displayThrowable(e, "Error", null,
                            WDTVConverterView.this.getFrame());
                    outputTextArea.setText("");
                    outputDialog.setVisible(false);
                    runButton.setEnabled(true);
                }
            }
        }.start();
    }//GEN-LAST:event_runButtonActionPerformed

    private void tempDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tempDirButtonActionPerformed
        File currentDir = fileChooser.getCurrentDirectory();
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fileChooser.showOpenDialog(this.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File tempDir = fileChooser.getSelectedFile();
            tempDirTextField.setText(tempDir.getAbsolutePath());
            getTheApplication().setTempDirectory(tempDir);
        }
    }//GEN-LAST:event_tempDirButtonActionPerformed

    private void convertDTSAC3CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_convertDTSAC3CheckBoxActionPerformed
        getTheApplication().setConvertDTS2AC3(convertDTSAC3CheckBox.isSelected());
        if (convertDTSAC3CheckBox.isSelected()) {
            bitrateComboBox.setEnabled(true);
            keepOTCheckBox.setEnabled(true);
            keepOTCheckBox.setSelected(
                    getTheApplication().isKeepOriginalTracks());
        } else {
            bitrateComboBox.setEnabled(false);
            keepOTCheckBox.setEnabled(false);
            keepOTCheckBox.setSelected(true);
            getTheApplication().setKeepOriginalTracks(true);
        }
    }//GEN-LAST:event_convertDTSAC3CheckBoxActionPerformed

    private void keepOTCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keepOTCheckBoxActionPerformed
        getTheApplication().setKeepOriginalTracks(keepOTCheckBox.isSelected());
    }//GEN-LAST:event_keepOTCheckBoxActionPerformed

    private void bitrateComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bitrateComboBoxActionPerformed
        // TODO add your handling code here:
        getTheApplication().setBitrate(getCurrentBitrate());
    }//GEN-LAST:event_bitrateComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JLabel additionalSubsLabel;
    private javax.swing.JLabel audioBitrateLabel;
    private javax.swing.JComboBox bitrateComboBox;
    private javax.swing.JCheckBox convertDTSAC3CheckBox;
    private javax.swing.JTextArea currentExecutableTextArea;
    private javax.swing.JCheckBox defaultCheckBox;
    private javax.swing.JButton destinationButton;
    private javax.swing.JLabel destinationLabel;
    private javax.swing.JTextField destinationTextField;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel kbitsLabel;
    private javax.swing.JCheckBox keepOTCheckBox;
    private javax.swing.JComboBox languageComboBox;
    private javax.swing.JLabel languageLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JDialog outputDialog;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JButton removeButton;
    private javax.swing.JButton runButton;
    private javax.swing.JButton sourceButton;
    private javax.swing.JLabel sourceLabel;
    private javax.swing.JTextField sourceTextField;
    private javax.swing.JScrollPane subsScrollPane;
    private javax.swing.JTable subsTable;
    private javax.swing.JButton subtitleButton;
    private javax.swing.JTextField subtitleTextField;
    private javax.swing.JButton tempDirButton;
    private javax.swing.JLabel tempDirLabel;
    private javax.swing.JTextField tempDirTextField;
    // End of variables declaration//GEN-END:variables

    private JDialog aboutBox;
    private File currentSource;
    private File currentDestination;
    private File currentSubtitle;

    private ProcessListener p;

    private static final FileFilter SRT_FILTER = new
            FileNameExtensionFilter("Subtitles (.srt)", "srt");
    private static final FileFilter MKV_FILTER = new
            FileNameExtensionFilter("Matroska (.mkv)", "mkv");
    private static final FileFilter AVI_FILTER = new
            FileNameExtensionFilter("AVI (.avi)", "avi");

    private boolean hasSubtitle(File f) {
        DefaultTableModel dtm = (DefaultTableModel) subsTable.getModel();
        Vector vv = dtm.getDataVector();
        Iterator it = vv.iterator();
        while(it.hasNext()) {
            Vector v = (Vector) it.next();
            File theFile = (File) v.get(0);
            if (theFile.equals(f)) return true;
        }
        return false;
    }

    private int getCurrentBitrate() {
        int index = bitrateComboBox.getSelectedIndex();
        switch(index) {
            case 0: return WDTVConverterApp.BITRATE_192;
            case 1: return WDTVConverterApp.BITRATE_286;
            case 2: return WDTVConverterApp.BITRATE_384;
            case 3: return WDTVConverterApp.BITRATE_448;
            case 4: return WDTVConverterApp.BITRATE_640;
            default: return WDTVConverterApp.BITRATE_448;
        }
    }

    private int getIndexForBitrate(int bitrate) {
        switch(bitrate) {
            case WDTVConverterApp.BITRATE_192: return 0;
            case WDTVConverterApp.BITRATE_286: return 1;
            case WDTVConverterApp.BITRATE_384: return 2;
            case WDTVConverterApp.BITRATE_448: return 3;
            case WDTVConverterApp.BITRATE_640: return 4;
            default: return 3;
        }
    }

    private void cleanDefault() {
        DefaultTableModel dtm = (DefaultTableModel) subsTable.getModel();
        Vector vv = dtm.getDataVector();
        Iterator it = vv.iterator();
        while(it.hasNext()) {
            Vector v = (Vector) it.next();
            boolean isDefault = (Boolean) v.get(2);
            if (isDefault) v.set(2, false);
        }
    }
    
}
