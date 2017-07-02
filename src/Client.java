import org.fife.ui.rtextarea.RTextScrollPane;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Rui Li on 17/03/17.
 */
public class Client {

    /***** Class that will be responsible for handling networking operations for the client *****/

    final static int PORT_NUMBER = 4547; /***** Port *****/
    //final static int CONTEXT_LENGTH = 10;

    Socket connectionSocket; /***** Socket for the connection with the server *****/
    ObjectInputStream inputStream; /***** Input stream for reading PacketData objects *****/
    ObjectOutputStream outputStream; /***** Output stream for sending PacketData objects *****/
    String name; /***** Name of the client *****/
    Editor editor; /***** Editor object *****/
    int ms = 2000;
    boolean connected = false;
    ArrayList<Shadow> clientShadows = new ArrayList<>();
    boolean added = false;

    public Client(String ip, String name, Editor editor, boolean performed){
        try {
            System.out.println("INITIAL SIZE: " + clientShadows.size() + " " + editor.collabFiles.size());
            connectionSocket = new Socket(ip, PORT_NUMBER);
            outputStream = new ObjectOutputStream(connectionSocket.getOutputStream());
            inputStream = new ObjectInputStream(connectionSocket.getInputStream());
            this.name = name;
            this.editor = editor;
            /***** Handshake with server *****/
            outputStream.writeObject(new PacketData(name, performed));
            outputStream.flush();
            connected = true;

            /***** Initialize text *****/
            if(!performed) {
                /***** Not server host, thus create all the tabs as well *****/
                PacketData data = (PacketData) inputStream.readObject();
                updateAllFiles(data.getFiles());
                for(FileInfo f : data.getFiles()){
                    clientShadows.add(new Shadow(f.getText(), f.getId()));
                    updateCaretContext(((RTextScrollPane)editor.tabMenu.getSelectedComponent()).getTextArea().getText(), 0, 0, f.getId());
                }
                //Collaboration file list
                for(FileInfo file : data.getFiles())editor.collabFiles.add(file);
                System.out.println("REACHED 1: " + clientShadows.size());
            } else {
                /***** Server host *****/
                PacketData data = (PacketData) inputStream.readObject();
                ArrayList<FileInfo> f = data.getFiles();
                for(int i = 0; i < f.size(); i++)editor.tabs.get(i).getFile().setId(f.get(i).getId());
                for(FileInfo z : data.getFiles()){
                    System.out.println("ADDING FILE: " + z.getId());
                    clientShadows.add(new Shadow(z.getText(), z.getId()));
                    updateCaretContext(((RTextScrollPane)editor.tabMenu.getSelectedComponent()).getTextArea().getText(), 0, 0, z.getId());
                }
                //Collaboration file list
                for(FileInfo file : f)editor.collabFiles.add(file);
                System.out.println(clientShadows.size());
            }
            editor.addCollabList();
            new SendThread(connectionSocket).start();
            new ReceiveThread(connectionSocket).start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public class ReceiveThread extends Thread {
        Socket connection;
        public ReceiveThread(Socket connection){
            this.connection = connection;
        }
        public void run(){
            while(connected) {
                try {
                    /***** Read and update *****/
                    PacketData data = (PacketData) inputStream.readObject();
                    if (data.isRequestFile()) {
                        createTab(data.getFileName(), data.getFileText(), "", data.getId(), data.getTemp());
                        clientShadows.remove(findShadow(data.getId()));
                        clientShadows.add(new Shadow(data.getFileText(), data.getId()));
                        updateCaretContext(data.getFileText(), 0, 0, data.getId());
                        FileInfo f = editor.findFile(data.getId());
                        f.setTitle(data.getFileName());
                        f.setText(data.getFileText());
                        f.setId(data.getId());
                        System.out.println("REQUESTING: " + data.getFileText());
                        java.awt.EventQueue.invokeLater(() -> {
                            editor.collabFileList.updateUI();
                        });
                    } else if (data.isCreateFile()) {
                        System.out.println("HELLO");
                        createTab(data.getFileName(), data.getFileText(), data.getId(), data.getTemp());
                        clientShadows.add(new Shadow(data.getFileText(), data.getId()));
                        updateCaretContext(data.getFileText(), 0, 0, data.getId());
                        editor.collabFiles.add(new FileInfo(data.getFileName(), data.getFileText(), data.getId()));
                        java.awt.EventQueue.invokeLater(() -> {
                            editor.collabFileList.updateUI();
                        });
                    } else if (data.isReinit()) {
                        ButtonTabComponent updateTab = findTab(data.getId());
                        diff_match_patch d = new diff_match_patch();
                        LinkedList<diff_match_patch.Diff> diffs = d.diff_main(updateTab.getTextArea().getText(), data.getFileText());
                        int[] vals = findCaret(diffs, updateTab);
                        int vVal = updateTab.getScrollPane().getVerticalScrollBar().getValue();
                        int hVal = updateTab.getScrollPane().getHorizontalScrollBar().getValue();
                        if (updateTab != null) updateText(vVal, hVal, vals, data.getFileText(), updateTab);
                        Shadow s = findShadow(data.getId());
                        if (s != null) {
                            s.setClientVersionNumber(data.getClientVersionNumber());
                            s.setServerVersionNumber(data.getServerVersionNumber());
                        }
                    } else if(data.isDeleteFile()) {
                        ButtonTabComponent b = editor.findTab(data.getId());
                        FileInfo f = editor.findFile(data.getId());
                        if(b != null)b.getFile().setId(-1);
                        if(f != null)editor.collabFiles.remove(f);
                        if(f != null || b != null) {
                            java.awt.EventQueue.invokeLater(() -> {
                                editor.collabFileList.updateUI();
                            });
                        }
                    } else if(data.isRenameFile()){
                        ButtonTabComponent b = editor.findTab(data.getId());
                        FileInfo f = editor.findFile(data.getId());
                        if(f != null)f.setTitle(data.getFileName());
                        if(b != null){
                            b.getFile().setTitle(data.getFileName());
                            int idx = editor.findIDXTab(data.getId());
                            if(idx != -1)editor.tabMenu.setTitleAt(idx, data.getFileName());
                        }
                        if(f != null || b != null) {
                            java.awt.EventQueue.invokeLater(() -> {
                                editor.collabFileList.updateUI();
                                editor.currentFiles.updateUI();
                            });
                        }
                    } else {
                        ButtonTabComponent updateTab = findTab(data.getId());
                        Shadow s = findShadow(data.getId());
                        if(updateTab != null && s != null) {
                            LinkedList<diff_match_patch.Diff> stack = data.getDiffQueue();
                            if (stack == null || data.getClientVersionNumber() < s.getClientVersionNumber()) {
                                if (s.getServerVersionNumber() < data.getServerVersionNumber())
                                    s.setServerVersionNumber(data.getServerVersionNumber());
                            } else {
                                if (stack.size() == 1 && stack.get(0).operation == diff_match_patch.Operation.EQUAL) {
                                    s.setServerVersionNumber(data.getServerVersionNumber());
                                } else if (!added) {
                                    String result = updateTab.getTextArea().getText();
                                    patchClientShadow(stack, data.getServerVersionNumber(), findShadow(data.getId()));
                                    result = patchClient(stack, result);
                                    int[] vals = findCaret(stack, updateTab);
                                    int vVal = updateTab.getScrollPane().getVerticalScrollBar().getValue();
                                    int hVal = updateTab.getScrollPane().getHorizontalScrollBar().getValue();
                                    updateText(vVal, hVal, vals, result, updateTab);
                                    /***** Prevent duplication *****/
                                    added = true;
                                }
                            }
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    connected = false;
                    return;
                }
            }
        }
    }
    public class SendThread extends Thread {
        Socket connection;
        public SendThread(Socket connection){
            this.connection = connection;
        }
        public void run(){
            while(connected) {
                try {
                    /***** Diff and patch then send to server *****/
                    int idx = editor.tabMenu.getSelectedIndex();
                    if(idx >= 0) {
                        ButtonTabComponent selectedTab = editor.tabs.get(idx);
                        System.out.println(editor.tabMenu.getSelectedIndex());
                        int id = selectedTab.getFile().getId();
                        if(id != -1) {
                            System.out.println(id);
                            Shadow clientShadow = findShadow(id);
                            System.out.println(clientShadow + " " + clientShadows.size() + " " + editor.collabFiles.size());
                            String text = selectedTab.getTextArea().getText();
                            System.out.println("AFTER REQUEST SENT: " + text + " " + clientShadow.getText());
                            LinkedList<diff_match_patch.Diff> diffs = diffClientTextShadow(text, clientShadow);
                            patchClientShadow(diffs, clientShadow.getServerVersionNumber(), clientShadow);
                            clientShadow.setClientVersionNumber(clientShadow.getClientVersionNumber() + 1);
                            System.out.println("THIS IS THE ID: " + id);
                            outputStream.writeObject(new PacketData(diffs, clientShadow.getClientVersionNumber(), clientShadow.getServerVersionNumber(), id));
                            outputStream.flush();
                            outputStream.reset();
                            /***** Allow for one packet to be received *****/
                            added = false;
                        }
                        /***** Simple adaptive timing algorithm *****/
                        ms = Math.min(2000, ms + 300);
                    }
                    Thread.sleep(ms);
                } catch (Exception e) {
                    e.printStackTrace();
                    connected = false;
                    editor.collabFiles.removeAllElements();
                    java.awt.EventQueue.invokeLater(() -> editor.filePanel.remove(editor.collabFilePanel));
                    return;
                }
            }
        }
    }

    /***** Updates from UI *****/
    /*public void updateScrollBar(JViewport v, int id){
        Shadow selectedShadow = findShadow(id);
        selectedShadow.setViewport(v);
        System.out.println("SCROLL POS: " + v.getName());
    }*/
    public void updateCaretContext(String text, int caretDot, int caretMark, int id){
        Shadow selectedShadow = findShadow(id);
        System.out.println("CARET POS: " + caretDot + " " + caretMark);
        selectedShadow.setCaretDotPos(caretDot);
        selectedShadow.setCaretMarkPos(caretMark);
    }
    /* OLD IMPLEMENTATION WITH CONTEXT MATCHING*/
    /*
    public void updateCaretContext(String text, int caretDot, int caretMark, int id){
        Shadow selectedShadow = findShadow(id);
        selectedShadow.setCaretDotContextSuffix(text.substring(caretDot, Math.min(caretDot+CONTEXT_LENGTH, text.length())));
        selectedShadow.setCaretMarkContextSuffix(text.substring(caretMark, Math.min(caretMark+CONTEXT_LENGTH, text.length())));
        selectedShadow.setNewLineDot(selectedShadow.getCaretDotContextSuffix().matches("[\\n\\r]+"));
        selectedShadow.setNewLineMark(selectedShadow.getCaretMarkContextSuffix().matches("[\\n\\r]+"));
        int addDot = 1;
        int addMark = 1;
        if(selectedShadow.isNewLineDot() && (caretDot==0 || (caretDot - 1 >= 0 && (text.substring(caretDot-1, caretDot).equals(System.lineSeparator()))))){
            while(selectedShadow.getCaretDotContextSuffix().matches("[\\n\\r]+")){
                selectedShadow.setCaretDotContextSuffix(text.substring(caretDot, Math.min(caretDot+CONTEXT_LENGTH+addDot, text.length())));
                if(caretDot + CONTEXT_LENGTH + addDot > text.length())break;
                addDot++;
            }
        }
        if(selectedShadow.isNewLineMark() && (caretMark==0 || (caretMark - 1 >= 0 && (text.substring(caretMark-1, caretMark).equals(System.lineSeparator()))))){
            while(selectedShadow.getCaretMarkContextSuffix().matches("[\\n\\r]+")){
                selectedShadow.setCaretMarkContextSuffix(text.substring(caretMark, Math.min(caretMark+CONTEXT_LENGTH+addMark, text.length())));
                if(caretMark + CONTEXT_LENGTH + addMark > text.length())break;
                addMark++;
            }
        }
        if(selectedShadow.isNewLineDot() && text.length() < caretDot + CONTEXT_LENGTH + addDot && (caretDot==0 || (caretDot - 1 >= 0 && (text.substring(caretDot-1, caretDot).equals(System.lineSeparator()))))){
            selectedShadow.setCaretDotContextSuffix(selectedShadow.getCaretDotContextSuffix() + "\u0296");
        }
        if(selectedShadow.isNewLineMark() && text.length() < caretMark + CONTEXT_LENGTH + addMark && (caretMark==0 || (caretMark - 1 >= 0 && (text.substring(caretMark-1, caretMark).equals(System.lineSeparator()))))){
            selectedShadow.setCaretMarkContextSuffix(selectedShadow.getCaretMarkContextSuffix() + "\u0296");
        }
        selectedShadow.setCaretDotPos(caretDot);
        selectedShadow.setCaretMarkPos(caretMark);
        System.out.println("CARET CONTEXT: " + selectedShadow.getCaretDotContextSuffix() + " " + selectedShadow.getCaretMarkContextSuffix());
        System.out.println("CARET POSITIONS: " + selectedShadow.getCaretDotPos() + " " + selectedShadow.getCaretMarkPos());
    }*/

    /***** Find diff between clientShadowText and ClientText *****/
    public LinkedList<diff_match_patch.Diff> diffClientTextShadow(String text, Shadow clientShadow){
        diff_match_patch d = new diff_match_patch();
        String clientShadowText = clientShadow.getText();
        LinkedList<diff_match_patch.Diff> diffs = d.diff_main(clientShadowText, text);
        d.diff_cleanupEfficiency(diffs);

        return diffs;
    }

    /***** Patch client shadow text *****/
    public void patchClientShadow(LinkedList<diff_match_patch.Diff> diffs, int m, Shadow clientShadow){
        diff_match_patch d = new diff_match_patch();
        String clientShadowText = clientShadow.getText();
        LinkedList<diff_match_patch.Patch> patches = d.patch_make(diffs);
        String result = (String)d.patch_apply(patches, clientShadowText)[0];
        clientShadow.setText(result);
        clientShadow.setServerVersionNumber(m);
    }

    /***** Patch the client text *****/
    public String patchClient(LinkedList<diff_match_patch.Diff> diffs, String curr){
        diff_match_patch d = new diff_match_patch();
        LinkedList<diff_match_patch.Patch> patches = d.patch_make(diffs);
        String result = (String)d.patch_apply(patches, curr)[0];
        return result;
    }

    /***** Preserve caret position as best as possible *****/
    public int[] findCaret(LinkedList<diff_match_patch.Diff> diffs, ButtonTabComponent z){
        Shadow selectedFile = findShadow(z.getFile().getId());
        diff_match_patch d = new diff_match_patch();
        int caretDotIDX = d.diff_xIndex(diffs, selectedFile.getCaretDotPos());
        int caretMarkIDX = d.diff_xIndex(diffs, selectedFile.getCaretMarkPos());
        System.out.println("CARET BEFORE UPDATE: " + caretDotIDX + " " + caretMarkIDX);
        selectedFile.setCaretDotPos(caretDotIDX);
        selectedFile.setCaretMarkPos(caretMarkIDX);
        int[] result = {caretDotIDX, caretMarkIDX};
        return result;
    }
    /*
    public void findCaret(String result, ButtonTabComponent z){
        Shadow selectedFile = findShadow(z.getFile().getId());
        result = result + "\u0296";
        diff_match_patch d = new diff_match_patch();
        int caretDot = selectedFile.getCaretDotPos();
        int caretMark = selectedFile.getCaretMarkPos();
        if (selectedFile.getCaretDotContextSuffix().equals("") || selectedFile.getCaretMarkContextSuffix().equals("")) {
            if (selectedFile.getCaretDotContextSuffix().equals("")) caretDot = result.length()-1;
            if (selectedFile.getCaretMarkContextSuffix().equals("")) caretMark = result.length()-1;
        } else {
            caretDot = d.match_main(result, selectedFile.getCaretDotContextSuffix(), selectedFile.getCaretDotPos());
            caretMark = d.match_main(result, selectedFile.getCaretMarkContextSuffix(), selectedFile.getCaretMarkPos());
        }
        if (caretDot < 0) caretDot = Math.min(selectedFile.getCaretDotPos(), result.length()-1);
        if (caretMark < 0) caretMark = Math.min(selectedFile.getCaretMarkPos(), result.length()-1);
        caretDot = Math.min(caretDot, result.length()-1);
        caretMark = Math.min(caretMark, result.length()-1);

        updateCaret(caretMark, caretDot, z);
        selectedFile.setCaretDotPos(caretDot);
        selectedFile.setCaretMarkPos(caretMark);
    }*/

    public Shadow findShadow(int id){
        for(Shadow s : clientShadows){
            if(s.getId() == id) return s;
        }
        return null;
    }

    public ButtonTabComponent findTab(int id){
        for(ButtonTabComponent c : editor.tabs){
            if(c.getFile().getId() == id){
                return c;
            }
        }
        return null;
    }
    public void updateAllFiles(ArrayList<FileInfo> files){
        for(FileInfo f : files){
            editor.createTab(f.getTitle(), f.getText(), f.getId());
            System.out.println(f.getTitle() + " " + f.getText() + " " + f.getId());
        }
    }

    public void sendCreate(FileInfo file){
        try {
            outputStream.writeObject(new PacketData(file.getFile(), file.getTitle(), file.getText(), true));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void requestFile(int id){
        try {
            if(id != -1) {
                outputStream.writeObject(new PacketData(id, true));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendDelete(int id){
        try{
            if(id != -1) {
                PacketData p = new PacketData(id);
                p.setDeleteFile(true);
                outputStream.writeObject(p);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendRename(String name, int id){
        try {
            if(id != -1) {
                PacketData p = new PacketData(id);
                p.setRenameFile(true);
                p.setFileName(name);
                outputStream.writeObject(p);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendOnClose(int id, String text){
        try {
            /***** Diff and patch then send to server *****/
            if (id != -1) {
                Shadow clientShadow = findShadow(id);
                LinkedList<diff_match_patch.Diff> diffs = diffClientTextShadow(text, clientShadow);
                patchClientShadow(diffs, clientShadow.getServerVersionNumber(), clientShadow);
                clientShadow.setClientVersionNumber(clientShadow.getClientVersionNumber() + 1);
                outputStream.writeObject(new PacketData(diffs, clientShadow.getClientVersionNumber(), clientShadow.getServerVersionNumber(), id));
                outputStream.flush();
                outputStream.reset();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /***** Update UI *****/
    public void updateText(int vVal, int hVal, int[] vals, String result, ButtonTabComponent z){
        java.awt.EventQueue.invokeLater(() -> {
            z.getTextArea().replaceRange(result, 0, z.getTextArea().getText().length());
            z.getFile().setText(result);
            z.getTextArea().getCaret().setDot(vals[1]);
            z.getTextArea().getCaret().moveDot(vals[0]);
            updateScroll(vVal, hVal, z);
        });
    }
    /***** KEEP SCROLLBARS CONSISTENT :D *****/
    public void updateScroll(int vVal, int hVal, ButtonTabComponent z){
        java.awt.EventQueue.invokeLater(() -> {
            z.getScrollPane().getVerticalScrollBar().setValue(vVal);
            z.getScrollPane().getHorizontalScrollBar().setValue(hVal);
        });
    }
    public void createTab(String filename, String fileText, int id, File f){
        java.awt.EventQueue.invokeLater(() -> {
            editor.createTab(filename, fileText, id, f);
        });
    }
    public void createTab(String filename, String fileText, String originalText, int id, File f){
        java.awt.EventQueue.invokeLater(() -> {
            editor.createTab(filename, fileText, originalText, id, f);
        });
    }
    /*
    public void createTab(String filename, String fileText, int id){
        java.awt.EventQueue.invokeLater(() -> {
            editor.createTab(filename, fileText, id);
        });
    }*/
}
