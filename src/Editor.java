import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by Rui Li on 17/03/17.
 */
public class Editor {

    /***** UI and main class *****/
    Client c;
    Server s;
    JFrame f;
    JPanel currentFilePanel;
    JList currentFiles;
    JList collabFileList;
    JPanel collabFilePanel;
    JPanel usersPanel;
    JMenuBar menuBar;
    JMenu connectMenu;
    JMenu fileMenu;
    JMenu runMenu;
    String name;
    String ip;
    Editor editor;
    JMenuItem disconnect;
    JMenuItem startServer;
    JMenuItem connectSetup;
    JMenuItem connect;
    JMenuItem open;
    JMenuItem save;
    JMenuItem saveAll;
    JMenuItem newFile;
    JMenuItem runJava;
    JMenuItem compileJava;
    JMenuItem rename;
    JMenuItem saveAs;
    JMenuItem openCollabFile;
    JMenuItem newCollabFile;
    JMenuItem addCurrentCollabFile;
    JMenuItem renameCollabFile;
    DnDTabbedPane tabMenu;
    Vector<ButtonTabComponent> tabs = new Vector<>();
    Vector<FileInfo> collabFiles = new Vector<>();
    JSplitPane filePanel;
    JSplitPane mainPanel;
    JSplitPane editorPanel;
    JSplitPane fileAndEditorPanel;
    boolean ran = false;
    ExecThread runThread;
    ExecCompileThread compileThread;
    int dividorPrev = -1;

    public Editor(){
        //Set look and feel for system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /***** Creating menu bar *****/
        menuBar = new JMenuBar();
        connectMenu = new JMenu("Setup Connection");
        fileMenu = new JMenu("File");
        runMenu = new JMenu("Run");

        /***** File menu section *****/
        rename = new JMenuItem("Rename file");
        rename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = JOptionPane.showInputDialog(f, "Rename File", "Enter file name", JOptionPane.INFORMATION_MESSAGE);
                if(title != null && !title.equals("")) {
                    int x = tabMenu.getSelectedIndex();
                    ButtonTabComponent b = tabs.get(x);
                    b.getFile().setTitle(title);
                    tabMenu.setTitleAt(x, title);
                    currentFiles.updateUI();
                }
            }
        });
        open = new JMenuItem("Open File");
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                int returnVal = fc.showOpenDialog(f);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    try{
                        File yourFile = fc.getSelectedFile();
                        String text = "";
                        Scanner s = new Scanner(yourFile);
                        while(s.hasNextLine()){
                            text+=s.nextLine()+"\n";
                        }
                        System.out.println("FILE OPENED: " + yourFile.getName());
                        FileInfo fileInfo = new FileInfo(yourFile, yourFile.getName(), text, -1);
                        createTab(yourFile.getName(), text, fileInfo);
                    } catch (Exception z){
                        JOptionPane.showMessageDialog(f, "Select a valid file");
                    }
                }
            }
        });
        save = new JMenuItem("Save");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int id = tabMenu.getSelectedIndex();
                ButtonTabComponent selected = tabs.get(id);
                File f = selected.getFile().getFile();
                String text = selected.getTextArea().getText();
                if(selected.getFile().getFile() == null || !selected.getFile().getFile().getName().equals(selected.getFile().getTitle()))f = saveFilePrompt(new File(selected.getFile().getTitle()), text);
                else saveFile(selected.getFile().getFile(), text);
                if(f != null){
                    selected.getFile().setFile(f);
                    tabMenu.setTitleAt(id, f.getName());
                    selected.getFile().setOriginalText(text);
                    selected.getFile().setText(text);
                    selected.getFile().setTitle(f.getName());
                }
            }
        });
        saveAll = new JMenuItem("Save all");
        saveAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int c = 0;
                for(ButtonTabComponent selected : tabs){
                    File f = null;
                    String text = selected.getTextArea().getText();
                    if(selected.getFile().getFile() == null || !selected.getFile().getFile().getName().equals(selected.getFile().getTitle()))f = saveFilePrompt(new File(selected.getFile().getTitle()), text);
                    else saveFile(selected.getFile().getFile(), text);
                    if(f != null){
                        selected.getFile().setFile(f);
                        tabMenu.setTitleAt(c, f.getName());
                        selected.getFile().setOriginalText(text);
                        selected.getFile().setText(text);
                        selected.getFile().setTitle(f.getName());
                    }
                    c++;
                }
            }
        });
        saveAs = new JMenuItem("Save as");
        saveAs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int id = tabMenu.getSelectedIndex();
                ButtonTabComponent selected = tabs.get(id);
                File f = null;
                String text = selected.getTextArea().getText();
                if(selected.getFile().getFile() == null || !selected.getFile().getFile().getName().equals(selected.getFile().getTitle()))f = saveFilePrompt(new File(selected.getFile().getTitle()), text);
                else f = saveFilePrompt(selected.getFile().getFile(), text);
                if(f != null){
                    selected.getFile().setFile(f);
                    tabMenu.setTitleAt(id, f.getName());
                    selected.getFile().setOriginalText(text);
                    selected.getFile().setText(text);
                    selected.getFile().setTitle(f.getName());
                }
            }
        });
        newFile = new JMenuItem("New File");
        newFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createTab("untitled.txt", "", -1);
            }
        });
        openCollabFile = new JMenuItem("Open file for collaboration");
        openCollabFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                int returnVal = fc.showOpenDialog(f);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    try{
                        File yourFile = fc.getSelectedFile();
                        String text = "";
                        Scanner s = new Scanner(yourFile);
                        while(s.hasNextLine()){
                            text+=s.nextLine()+"\n";
                        }
                        FileInfo fileInfo = new FileInfo(yourFile, yourFile.getName(), text, -1);
                        c.sendCreate(fileInfo);
                    } catch (Exception z){
                        JOptionPane.showMessageDialog(f, "Select a valid file");
                    }
                }
            }
        });
        newCollabFile = new JMenuItem("New file for collaboration");
        newCollabFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                c.sendCreate(new FileInfo("untitled.txt", "", -1));
            }
        });
        addCurrentCollabFile = new JMenuItem("Add current file to collaboration");
        addCurrentCollabFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(tabs.get(tabMenu.getSelectedIndex()).getFile().getId() == -1) {
                    ButtonTabComponent b = tabs.get(tabMenu.getSelectedIndex());
                    tabs.remove(tabMenu.getSelectedIndex());
                    tabMenu.remove(tabMenu.getSelectedIndex());
                    c.sendCreate(new FileInfo(b.getFile().getFile(), b.getFile().getTitle(), b.getTextArea().getText(), -1));
                } else {
                    JOptionPane.showMessageDialog(f, "File is already in collaboration", "Error", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        renameCollabFile = new JMenuItem("Rename file for collaboration");
        renameCollabFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = JOptionPane.showInputDialog(f, "Rename File", "Enter file name", JOptionPane.INFORMATION_MESSAGE);
                if(title != null && !title.equals("")) {
                    c.sendRename(title, tabs.get(tabMenu.getSelectedIndex()).getFile().getId());
                }
            }
        });
        fileMenu.add(newFile);
        fileMenu.add(open);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.add(saveAll);
        fileMenu.add(rename);

        /***** Connection menu section *****/
        disconnect = new JMenuItem("Disconnect");
        disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    c.connected = false;
                    c.connectionSocket.close();
                    c = null;
                    connectMenu.remove(disconnect);
                    collabFiles.removeAllElements();
                    resetAllFiles();
                    filePanel.remove(collabFilePanel);
                    if(s != null){
                        s.alive = false;
                        s.serverSocket.close();
                        s = null;
                        disconnect.setText("Disconnect");
                    }
                    connectMenu.add(connectSetup);
                    connectMenu.add(connect);
                    connectMenu.add(startServer);
                    fileMenu.remove(newCollabFile);
                    fileMenu.remove(openCollabFile);
                    fileMenu.remove(addCurrentCollabFile);
                    fileMenu.remove(renameCollabFile);
                } catch (Exception z){
                    z.printStackTrace();
                    return;
                }
            }
        });
        connectSetup = new JMenuItem("Connection setup");
        connectSetup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                name = JOptionPane.showInputDialog(f, "Enter a username", "Connection setup", JOptionPane.INFORMATION_MESSAGE);
                if(name != null && !name.equals(""))ip = JOptionPane.showInputDialog(f, "Enter server ip", "Connection setup", JOptionPane.INFORMATION_MESSAGE);
                else if(name != null && name.equals(""))JOptionPane.showMessageDialog(f, "Please enter a username", "Error", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        connectMenu.add(connectSetup);
        connect = new JMenuItem("Connect to server");
        connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(name != null && ip != null){
                    c = new Client(ip, name, editor, false);
                    if(!c.connected){
                        JOptionPane.showMessageDialog(f, "Server connection error", "Error", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        connectMenu.remove(connect);
                        connectMenu.remove(connectSetup);
                        connectMenu.remove(startServer);
                        connectMenu.add(disconnect);
                        fileMenu.remove(rename);
                        fileMenu.add(newCollabFile);
                        fileMenu.add(openCollabFile);
                        fileMenu.add(addCurrentCollabFile);
                        fileMenu.add(renameCollabFile);
                        fileMenu.add(rename);
                    }
                } else {
                    JOptionPane.showMessageDialog(f, "Please complete connection setup", "Error", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        connectMenu.add(connect);
        startServer = new JMenuItem("Start server");
        startServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(tabs.size() > 0) {
                    s = new Server(tabs);
                    if (name != null) c = new Client("localhost", name, editor, true);
                    else c = new Client("localhost", "Server host", editor, true);
                    connectMenu.remove(startServer);
                    connectMenu.remove(connect);
                    connectMenu.remove(connectSetup);
                    disconnect.setText("Stop server");
                    connectMenu.add(disconnect);
                    fileMenu.remove(rename);
                    fileMenu.add(newCollabFile);
                    fileMenu.add(openCollabFile);
                    fileMenu.add(addCurrentCollabFile);
                    fileMenu.add(renameCollabFile);
                    fileMenu.add(rename);
                } else {
                    JOptionPane.showMessageDialog(f, "There must be at least one file open", "Error", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        connectMenu.add(startServer);

        /***** Run menu section *****/
        runJava = new JMenuItem("Run Java program");
        runJava.addActionListener(new ActionListener() {
            JTextArea printArea;
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(runThread != null)if(runThread.isAlive() && runThread.pro != null) runThread.pro.destroy();
                    if(compileThread != null)if(compileThread.isAlive() && compileThread.pro != null) compileThread.pro.destroy();
                    if(mainPanel.getBottomComponent() != null)mainPanel.remove(mainPanel.getBottomComponent());
                    int id = tabMenu.getSelectedIndex();
                    ButtonTabComponent selected = tabs.get(id);
                    String text = selected.getTextArea().getText();
                    File fe = selected.getFile().getFile();
                    if(selected.getFile().getFile() == null || !selected.getFile().getFile().getName().equals(selected.getFile().getTitle()))fe = saveFilePrompt(new File(selected.getFile().getTitle()), selected.getTextArea().getText());
                    else saveFile(selected.getFile().getFile(), text);
                    if(fe != null && fe.getName().indexOf(".") > 0) {
                        selected.getFile().setFile(fe);
                        tabMenu.setTitleAt(id, fe.getName());
                        selected.getFile().setOriginalText(text);
                        selected.getFile().setText(text);
                        selected.getFile().setTitle(fe.getName());
                        JPanel panel = new JPanel(new BorderLayout());

                        JButton closeButton = new JButton("Close Console");
                        closeButton.addActionListener(new CloseButtonListenerBottomPanel());
                        panel.add(closeButton, BorderLayout.NORTH);

                        panel.setPreferredSize(new Dimension(f.getWidth(), 50));
                        printArea = new JTextArea();
                        printArea.setTabSize(4);
                        printArea.setBorder(BorderFactory.createCompoundBorder(printArea.getBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                        JScrollPane scroll = new JScrollPane(printArea);
                        scroll.isWheelScrollingEnabled();
                        scroll.setAutoscrolls(true);
                        panel.add(scroll, BorderLayout.CENTER);
                        mainPanel.setBottomComponent(panel);
                        if(ran && f.getHeight() - mainPanel.getDividerLocation() >= 5)mainPanel.setDividerLocation(mainPanel.getDividerLocation());
                        else if(dividorPrev != -1 && f.getHeight() - dividorPrev >= 5) mainPanel.setDividerLocation(dividorPrev);
                        else mainPanel.setDividerLocation(0.75);
                        ran = true;
                        printArea.requestFocus();
                        runThread = new ExecThread(printArea, fe);
                        runThread.start();
                    }
                    System.out.println(dividorPrev);
                } catch (Exception z){
                    z.printStackTrace();
                }
            }
        });
        compileJava = new JMenuItem("Compile Java program");
        compileJava.addActionListener(new ActionListener() {
            JTextArea printArea;
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(runThread != null)if(runThread.isAlive() && runThread.pro != null) runThread.pro.destroy();
                    if(compileThread != null)if(compileThread.isAlive() && compileThread.pro != null) compileThread.pro.destroy();
                    if(mainPanel.getBottomComponent() != null)mainPanel.remove(mainPanel.getBottomComponent());
                    int id = tabMenu.getSelectedIndex();
                    ButtonTabComponent selected = tabs.get(id);
                    String text = selected.getTextArea().getText();
                    File fe = selected.getFile().getFile();
                    if(selected.getFile().getFile() == null || !selected.getFile().getFile().getName().equals(selected.getFile().getTitle()))fe = saveFilePrompt(new File(selected.getFile().getTitle()), selected.getTextArea().getText());
                    else saveFile(selected.getFile().getFile(), text);
                    if(fe != null && fe.getName().indexOf(".") > 0) {
                        selected.getFile().setFile(fe);
                        tabMenu.setTitleAt(id, fe.getName());
                        selected.getFile().setOriginalText(text);
                        selected.getFile().setText(text);
                        selected.getFile().setTitle(fe.getName());
                        JPanel panel = new JPanel(new BorderLayout());

                        JButton closeButton = new JButton("Close Console");
                        closeButton.addActionListener(new CloseButtonListenerBottomPanel());
                        panel.add(closeButton, BorderLayout.NORTH);

                        panel.setPreferredSize(new Dimension(f.getWidth(), 50));
                        printArea = new JTextArea();
                        printArea.setEditable(false);
                        printArea.setTabSize(4);
                        printArea.setBorder(BorderFactory.createCompoundBorder(printArea.getBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                        JScrollPane scroll = new JScrollPane(printArea);
                        scroll.isWheelScrollingEnabled();
                        scroll.setAutoscrolls(true);
                        panel.add(scroll, BorderLayout.CENTER);
                        mainPanel.setBottomComponent(panel);
                        if(ran && f.getHeight() - mainPanel.getDividerLocation() >= 5)mainPanel.setDividerLocation(mainPanel.getDividerLocation());
                        else if(dividorPrev != -1 && f.getHeight() - dividorPrev >= 5) mainPanel.setDividerLocation(dividorPrev);
                        else mainPanel.setDividerLocation(0.75);
                        ran = true;
                        printArea.requestFocus();
                        compileThread = new ExecCompileThread(printArea, fe);
                        compileThread.start();
                    }
                } catch (Exception z){
                    z.printStackTrace();
                }
            }
        });
        runMenu.add(runJava);
        runMenu.add(compileJava);

        /***** Add the menus *****/
        menuBar.add(fileMenu);
        menuBar.add(connectMenu);
        menuBar.add(runMenu);

        /***** Create current files list *****/
        currentFiles = new JList(tabs);
        currentFiles.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }
            @Override
            public void mousePressed(MouseEvent evt){
                JList list = (JList)evt.getSource();
                if(evt.isPopupTrigger()){
                    currentFiles.setSelectedIndex(list.locationToIndex(evt.getPoint()));
                    doPop(evt, list.locationToIndex(evt.getPoint()));
                } else {
                    int index = list.locationToIndex(evt.getPoint());
                    tabMenu.setSelectedIndex(index);
                }
            }
            @Override
            public void mouseReleased(MouseEvent evt){
                JList list = (JList)evt.getSource();
                if(evt.isPopupTrigger()){
                    currentFiles.setSelectedIndex(list.locationToIndex(evt.getPoint()));
                    doPop(evt, list.locationToIndex(evt.getPoint()));
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
            private void doPop(MouseEvent e, int index){
                boolean check = c != null;
                if(c != null)check = c.connected;
                if(check)check = tabs.get(index).getFile().getId() == -1;
                FilePanelPopup menu = new FilePanelPopup(index, check);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        currentFilePanel = new JPanel(new BorderLayout());
        currentFilePanel.setBorder(BorderFactory.createCompoundBorder(currentFilePanel.getBorder(), BorderFactory.createEmptyBorder(5, 5, 0, 0)));
        currentFilePanel.add(new JLabel("Current files open"), BorderLayout.PAGE_START);
        JScrollPane scroll = new JScrollPane(currentFiles);
        scroll.isWheelScrollingEnabled();
        scroll.setAutoscrolls(false);
        currentFilePanel.add(scroll);

        /***** Create user list *****/
        usersPanel = new JPanel(); //For user list

        /***** Create main editor pane *****/
        tabMenu = new DnDTabbedPane(this);
        createTab("untitled.txt", "", -1);
        tabMenu.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabMenu.addChangeListener(e -> currentFiles.setSelectedIndex(tabMenu.getSelectedIndex()));

        /***** Create file panel *****/
        filePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT); //For file lists
        filePanel.setTopComponent(currentFilePanel);

        /***** File panel + main editor pane *****/
        fileAndEditorPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        fileAndEditorPanel.setLeftComponent(filePanel);
        fileAndEditorPanel.setRightComponent(tabMenu);

        /***** Create editor panel (editor pane + user list) *****/
        editorPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        editorPanel.setLeftComponent(fileAndEditorPanel);

        /***** Create main panel *****/
        mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainPanel.setTopComponent(editorPanel);

        /***** Flatten for better look *****/
        flattenSplitPane(mainPanel);
        flattenSplitPane(editorPanel);
        flattenSplitPane(filePanel);
        flattenSplitPane(fileAndEditorPanel);

        /***** Show JFrame *****/
        f.setSize(new Dimension(700, 700));
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setJMenuBar(menuBar);
        f.add(mainPanel);
        f.setVisible(true);
        fileAndEditorPanel.setDividerLocation(0.25);
        //new BotThread().start(); //For bot
    }

    /***** Flatten the split panes *****/
    public static void flattenSplitPane(JSplitPane jSplitPane) {
        jSplitPane.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {
                    }
                };
            }
        });
        jSplitPane.setBorder(null);
    }

    /***** Create tabs *****/
    public void createTab(String title, String text, String originalText, int id, File f){
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setTabSize(4);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setText(text);
        //Padding
        textArea.setBorder(BorderFactory.createCompoundBorder(textArea.getBorder(), BorderFactory.createEmptyBorder(1, 1, 0, 0)));
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.isWheelScrollingEnabled();
        scrollPane.setAutoscrolls(false);

        ButtonTabComponent b = new ButtonTabComponent(tabMenu, this, textArea, scrollPane, tabs);
        textArea.addKeyListener(new TypingListener(b));
        textArea.addCaretListener(new CaretPosListener(b));
        //scrollPane.getViewport().addChangeListener(new ScrollListener(b));
        b.setFile(new FileInfo(f, title, text, originalText, id));
        tabMenu.add(title, scrollPane);
        tabs.add(b);
        tabMenu.setTabComponentAt(tabMenu.getTabCount()-1, b);
        currentFiles.updateUI();
    }
    public void createTab(String title, String text, int id, File f){
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setTabSize(4);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setText(text);
        //Padding
        textArea.setBorder(BorderFactory.createCompoundBorder(textArea.getBorder(), BorderFactory.createEmptyBorder(1, 1, 0, 0)));
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.isWheelScrollingEnabled();
        scrollPane.setAutoscrolls(false);

        ButtonTabComponent b = new ButtonTabComponent(tabMenu, this, textArea, scrollPane, tabs);
        textArea.addKeyListener(new TypingListener(b));
        textArea.addCaretListener(new CaretPosListener(b));
        //scrollPane.getViewport().addChangeListener(new ScrollListener(b));
        b.setFile(new FileInfo(f, title, text, id));
        tabMenu.add(title, scrollPane);
        tabs.add(b);
        tabMenu.setTabComponentAt(tabMenu.getTabCount()-1, b);
        currentFiles.updateUI();
    }
    public void createTab(String title, String text, FileInfo f){
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setTabSize(4);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setText(text);
        //Padding
        textArea.setBorder(BorderFactory.createCompoundBorder(textArea.getBorder(), BorderFactory.createEmptyBorder(1, 1, 0, 0)));
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.isWheelScrollingEnabled();
        scrollPane.setAutoscrolls(false);

        ButtonTabComponent b = new ButtonTabComponent(tabMenu, this, textArea, scrollPane, tabs);
        textArea.addKeyListener(new TypingListener(b));
        textArea.addCaretListener(new CaretPosListener(b));
        //scrollPane.getViewport().addChangeListener(new ScrollListener(b));
        b.setFile(f);
        tabMenu.add(title, scrollPane);
        tabs.add(b);
        tabMenu.setTabComponentAt(tabMenu.getTabCount()-1, b);
        currentFiles.updateUI();
    }
    public void createTab(String title, String text, int id){
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setTabSize(4);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setText(text);
        //Padding
        textArea.setBorder(BorderFactory.createCompoundBorder(textArea.getBorder(), BorderFactory.createEmptyBorder(1, 1, 0, 0)));
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.isWheelScrollingEnabled();
        scrollPane.setAutoscrolls(false);

        ButtonTabComponent b = new ButtonTabComponent(tabMenu, this, textArea, scrollPane, tabs);
        textArea.addKeyListener(new TypingListener(b));
        textArea.addCaretListener(new CaretPosListener(b));
        //scrollPane.getViewport().addChangeListener(new ScrollListener(b));
        b.setFile(new FileInfo(title, text, id));
        tabMenu.add(title, scrollPane);
        tabs.add(b);
        tabMenu.setTabComponentAt(tabMenu.getTabCount()-1, b);
        currentFiles.updateUI();
    }

    //Find tab with the id
    public ButtonTabComponent findTab(int id){
        ButtonTabComponent z = null;
        for(ButtonTabComponent b : tabs){
            if(b.getFile().getId() == id){
                z = b;
            }
        }
        return z;
    }

    //Find tab with the id
    public int findIDXTab(int id){
        int idx = -1;
        int c = 0;
        for(ButtonTabComponent b : tabs){
            if(b.getFile().getId() == id){
                idx = c;
            }
            c++;
        }
        return idx;
    }

    //Find file with the id
    public FileInfo findFile(int id){
        FileInfo z = null;
        for(FileInfo b : collabFiles){
            if(b.getId() == id){
                z = b;
            }
        }
        return z;
    }

    /***** Create collab list and its panel *****/
    public void addCollabList(){
        collabFilePanel = new JPanel(new BorderLayout());
        collabFilePanel.setBorder(BorderFactory.createCompoundBorder(collabFilePanel.getBorder(), BorderFactory.createEmptyBorder(5, 5, 0, 0)));
        collabFilePanel.add(new JLabel("Collaborative files"), BorderLayout.PAGE_START);
        collabFileList = new JList(collabFiles);
        collabFileList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }
            @Override
            public void mousePressed(MouseEvent e) {
                JList list = (JList)e.getSource();
                if(e.isPopupTrigger()){
                    if(collabFiles.size() > 0) {
                        collabFileList.setSelectedIndex(list.locationToIndex(e.getPoint()));
                        doPop(e, list.locationToIndex(e.getPoint()));
                    }
                } else {
                    if(collabFiles.size() > 0) {
                        int index = list.locationToIndex(e.getPoint());
                        int id = collabFiles.get(index).getId();
                        ButtonTabComponent b = findTab(id);
                        if (b == null) {
                            if (c != null) {
                                c.requestFile(id);
                            }
                        } else {
                            tabMenu.setSelectedIndex(tabs.indexOf(b));
                        }
                    }
                }
                collabFileList.clearSelection();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                JList list = (JList)e.getSource();
                if(e.isPopupTrigger()){
                    if(collabFiles.size() > 0) {
                        collabFileList.setSelectedIndex(list.locationToIndex(e.getPoint()));
                        doPop(e, list.locationToIndex(e.getPoint()));
                    }
                }
                collabFileList.clearSelection();
            }
            @Override
            public void mouseEntered(MouseEvent e) {

            }
            @Override
            public void mouseExited(MouseEvent e) {

            }

            public void doPop(MouseEvent e, int index){
                CollabPanelPopup menu = new CollabPanelPopup(index);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        JScrollPane scroll = new JScrollPane(collabFileList);
        scroll.isWheelScrollingEnabled();
        scroll.setAutoscrolls(false);
        collabFilePanel.add(scroll);
        filePanel.setBottomComponent(collabFilePanel);
        filePanel.setDividerLocation(0.5);
    }

    /***** Remove all ids *****/
    public void resetAllFiles(){
        for(ButtonTabComponent b : tabs){
            b.getFile().setId(-1);
        }
    }

    /***** Listeners *****/
    private class TypingListener implements KeyListener{
        ButtonTabComponent b;
        public TypingListener(ButtonTabComponent b){
            this.b = b;
        }
        @Override
        public void keyTyped(KeyEvent e) {
        }
        @Override
        public void keyPressed(KeyEvent e) {
        }
        @Override
        public void keyReleased(KeyEvent e) {
            if(c != null && c.connected){
                /***** Adaptive time algorithm *****/
                //This can be adjusted for best performance and results
                c.ms=Math.max(100, c.ms/2);
            }
            if(c != null && !c.connected){
                if(name != null && ip != null){
                    String temp = b.getTextArea().getText();
                    c = new Client(ip, name, editor, false);
                    if(!c.connected){
                        b.getTextArea().setText(temp);
                        JOptionPane.showMessageDialog(f, "Server connection error", "Error", JOptionPane.INFORMATION_MESSAGE);
                        c = null;
                        collabFiles.removeAllElements();
                        resetAllFiles();
                        filePanel.remove(collabFilePanel);
                        connectMenu.remove(disconnect);
                        connectMenu.add(connectSetup);
                        connectMenu.add(connect);
                        connectMenu.add(startServer);
                        fileMenu.remove(newCollabFile);
                        fileMenu.remove(openCollabFile);
                        fileMenu.remove(addCurrentCollabFile);
                        fileMenu.remove(renameCollabFile);
                    } else {
                        connectMenu.add(disconnect);
                    }
                } else {
                    JOptionPane.showMessageDialog(f, "Server connection error", "Error", JOptionPane.INFORMATION_MESSAGE);
                    c = null;
                }
            }
        }
    }
    private class CaretPosListener implements CaretListener {
        ButtonTabComponent b;
        public CaretPosListener(ButtonTabComponent b){
            this.b = b;
        }
        @Override
        public void caretUpdate(CaretEvent e) {
            //DefaultCaret z = (DefaultCaret) editor.textArea.getCaret();
            //z.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            b.setCaretDot(e.getDot());
            b.setCaretMark(e.getMark());
            if (c != null && c.connected && b.getFile().getId() != -1) {
                c.updateCaretContext(b.getTextArea().getText(), b.getCaretDot(), b.getCaretMark(), b.getFile().getId());
            }
            if (c != null && !c.connected) {
                if (name != null && ip != null) {
                    String temp = b.getTextArea().getText();
                    c = new Client(ip, name, editor, false);
                    if (!c.connected) {
                        b.getTextArea().setText(temp);
                        JOptionPane.showMessageDialog(f, "Server connection error", "Error", JOptionPane.INFORMATION_MESSAGE);
                        c = null;
                        collabFiles.removeAllElements();
                        resetAllFiles();
                        filePanel.remove(collabFilePanel);
                        connectMenu.remove(disconnect);
                        connectMenu.add(connectSetup);
                        connectMenu.add(connect);
                        connectMenu.add(startServer);
                        fileMenu.remove(newCollabFile);
                        fileMenu.remove(openCollabFile);
                        fileMenu.remove(addCurrentCollabFile);
                        fileMenu.remove(renameCollabFile);
                    } else {
                        connectMenu.add(disconnect);
                    }
                } else {
                    JOptionPane.showMessageDialog(f, "Server connection error", "Error", JOptionPane.INFORMATION_MESSAGE);
                    c = null;
                }
            }
        }
    }

    public int saveFileClosing(File curr, String text){
        Object[] options = {"Save", "Don't Save", "Cancel"};
        int op = JOptionPane.showOptionDialog(f, "Save changes to \"" + curr.getName() + "\" before closing?\nYour changes will be lost if you don't save them.", "Save Confirmation", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
        if(op == 0){
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(curr);
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            int returnVal = fc.showSaveDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File yourFile = fc.getSelectedFile();
                try {
                    FileWriter writer = new FileWriter(yourFile);
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    return 0;
                } catch (Exception d) {
                    d.printStackTrace();
                }
            }
        } else if(op == 1){
            return 1;
        }
        return 2;
    }
    public File saveFilePrompt(File curr, String text){
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(curr);
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        int returnVal = fc.showSaveDialog(f);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File yourFile = fc.getSelectedFile();
            try {
                FileWriter writer = new FileWriter(yourFile);
                writer.write(text);
                writer.flush();
                writer.close();
                return yourFile;
            } catch (Exception d) {
                d.printStackTrace();
            }
        }
        return null;
    }
    public void saveFile(File yourFile, String text){
        try {
            FileWriter writer = new FileWriter(yourFile);
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (Exception d) {
            d.printStackTrace();
        }
    }

    /***** RUNNING PROGRAMS *****/
    public class ExecThread extends Thread {
        private JTextArea printArea;
        private File f;
        private int prevCaretPos;
        private OutputListener listening;
        Process pro;

        public ExecThread(JTextArea printArea, File f) {
            this.printArea = printArea;
            this.f = f;
            prevCaretPos = 0;
        }

        public void run() {
            try {
                if (f.getName().indexOf(".") > 0) {
                    String name = f.getName().substring(0, f.getName().lastIndexOf("."));
                    runProcess("javac " + f.getName());
                    runProcess("java " + name);
                    printArea.setEditable(false);
                }
            } catch (Exception e) {
                printArea.append(e.getMessage());
            }
            System.out.println("Finished");
        }

        private void printLines(InputStream ins) throws Exception {
            String line;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(ins));
            while ((line = in.readLine()) != null) {
                printArea.append(line + "\n");
                printArea.setCaretPosition(printArea.getText().length());
                listening.prevCaretPos = printArea.getCaretPosition();
            }
        }

        private void runProcess(String command) throws Exception {
            pro = Runtime.getRuntime().exec(command, null, f.getParentFile());
            listening = new OutputListener(pro.getOutputStream(), printArea, prevCaretPos);
            printArea.addKeyListener(listening);
            printLines(pro.getInputStream());
            printLines(pro.getErrorStream());
            pro.waitFor();
        }
    }
    public class ExecCompileThread extends Thread{
        private JTextArea printArea;
        private File f;
        private int prevCaretPos;
        private OutputListener listening;
        Process pro;
        public ExecCompileThread(JTextArea printArea, File f){
            this.printArea = printArea;
            this.f = f;
            prevCaretPos = 0;
        }
        public void run(){
            try {
                if(f.getName().indexOf(".") > 0) {
                    runProcess("javac " + f.getName());
                    printArea.setEditable(false);
                    if(printArea.getText().length() == 0){
                        printArea.append("Compiled successfully");
                    }
                }
            } catch (Exception e){
                printArea.append(e.getMessage());
            }
        }
        private void printLines(InputStream ins) throws Exception {
            String line;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(ins));
            while ((line = in.readLine()) != null) {
                printArea.append(line + "\n");
                printArea.setCaretPosition(printArea.getText().length());
                prevCaretPos = printArea.getCaretPosition();
                listening.prevCaretPos = prevCaretPos;
            }
        }
        private void runProcess(String command) throws Exception {
            pro = Runtime.getRuntime().exec(command, null, f.getParentFile());
            listening = new OutputListener(pro.getOutputStream(), printArea, prevCaretPos);
            printArea.addKeyListener(listening);
            printLines(pro.getInputStream());
            printLines(pro.getErrorStream());
            pro.waitFor();
        }
    }
    private class OutputListener implements KeyListener{
        OutputStream o;
        JTextArea parent;
        PrintStream s;
        int prevCaretPos;
        public OutputListener(OutputStream o, JTextArea parent, int prevCaretPos){
            this.o = o;
            this.parent = parent;
            this.prevCaretPos = prevCaretPos;
            s = new PrintStream(o, true);
        }
        @Override
        public void keyTyped(KeyEvent e) {
        }
        @Override
        public void keyPressed(KeyEvent e) {
        }
        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                String r = parent.getText().substring(prevCaretPos);
                s.print(r);
                s.flush();
                prevCaretPos = parent.getText().length();
            }
        }
    }
    public class CloseButtonListenerBottomPanel implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            dividorPrev = mainPanel.getDividerLocation();
            if(runThread != null)if(runThread.isAlive() && runThread.pro != null) runThread.pro.destroy();
            if(compileThread != null)if(compileThread.isAlive() && compileThread.pro != null) compileThread.pro.destroy();
            mainPanel.remove(mainPanel.getBottomComponent());
            ran = false;
        }
    }

    /***** Popup classes *****/
    private class FilePanelPopup extends JPopupMenu {
        JMenuItem open;
        JMenuItem close;
        JMenuItem rename;
        JMenuItem addItem;
        int index;
        public FilePanelPopup(int idx, boolean check){
            this.index = idx;
            open = new JMenuItem("Open");
            open.addActionListener(e -> {
                tabMenu.setSelectedIndex(index);
                currentFiles.setSelectedIndex(index);
            });
            add(open);
            this.rename = new JMenuItem("Rename file");
            this.rename.addActionListener(e -> {
                String title = JOptionPane.showInputDialog(f, "Rename File", "Enter file name", JOptionPane.INFORMATION_MESSAGE);
                if(title != null && !title.equals("")) {
                    if(tabs.get(index).getFile().getId() == -1) {
                        ButtonTabComponent b = tabs.get(index);
                        b.getFile().setTitle(title);
                        tabMenu.setTitleAt(index, title);
                        currentFiles.updateUI();
                    } else {
                        ButtonTabComponent b = tabs.get(index);
                        b.getFile().setTitle(title);
                        tabMenu.setTitleAt(index, title);
                        c.sendRename(title, tabs.get(index).getFile().getId());
                        currentFiles.updateUI();
                    }
                }
            });
            add(this.rename);
            close = new JMenuItem("Close");
            close.addActionListener(e -> {
                tabs.remove(index);
                tabMenu.remove(index);
                currentFiles.updateUI();
            });
            add(close);
            if(check){
                this.addItem = new JMenuItem("Add file to collaboration");
                this.addItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ButtonTabComponent b = tabs.get(index);
                        tabs.remove(index);
                        tabMenu.remove(index);
                        c.sendCreate(new FileInfo(b.getFile().getFile(), b.getFile().getTitle(), b.getTextArea().getText(), -1));
                    }
                });
                add(addItem);
            }
        }
    }
    private class CollabPanelPopup extends JPopupMenu {
        JMenuItem open;
        JMenuItem close;
        JMenuItem rename;
        int index;
        public CollabPanelPopup(int idx){
            this.index = idx;
            open = new JMenuItem("Open file for collaboration");
            open.addActionListener(e -> {
                int id = collabFiles.get(index).getId();
                ButtonTabComponent b = findTab(id);
                if(b == null){
                    if(c != null && c.connected)c.requestFile(id);
                } else {
                    tabMenu.setSelectedIndex(tabs.indexOf(b));
                }
            });
            add(open);
            rename = new JMenuItem("Rename file for collaboration");
            rename.addActionListener(e -> {
                if(c != null && c.connected){
                    String title = JOptionPane.showInputDialog(f, "Rename File", "Enter file name", JOptionPane.INFORMATION_MESSAGE);
                    if(title != null && !title.equals("")) c.sendRename(title, collabFiles.get(index).getId());
                }
            });
            add(rename);
            close = new JMenuItem("Delete file from collaboration");
            close.addActionListener(e -> {
                if(c != null)c.sendDelete(collabFiles.get(index).getId());
            });
            add(close);
        }
    }

    /***** For testing purposes, this bot randomly inputs text *****/
    public class BotThread extends Thread{
        String[] vals = {"A", "B", "C", "D", "E", "F", " ", "\t"};
        public void run(){
            while(true){
                try {
                    int val = (int) (Math.random() * 8);
                    tabs.get(tabMenu.getSelectedIndex()).getTextArea().insert(vals[val], tabs.get(tabMenu.getSelectedIndex()).getTextArea().getCaretPosition());
                    tabs.get(tabMenu.getSelectedIndex()).getTextArea().getCaret().setDot(tabs.get(tabMenu.getSelectedIndex()).getTextArea().getCaret().getDot() + 1);
                    Thread.sleep(10 + (int)(Math.random()*((500-10)+1)));
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public static void main(String args[]){
        Editor mainUI = new Editor();
        mainUI.editor = mainUI;
    }
}
