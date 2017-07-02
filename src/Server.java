import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Created by Rui Li on 17/03/17.
 */
public class Server {

    /***** Main server class that will be responsible for all backend operations *****/

    final static int PORT_NUMBER = 4547; /***** Port *****/

    ArrayList<ConnectionInfo> connections; /***** All client connections and their respective data *****/
    ServerSocket serverSocket; /***** Server socket *****/
    volatile ArrayList<FileInfo> files = new ArrayList<>(); /***** Multi file editing *****/
    boolean alive = true;

    public Server(Vector<ButtonTabComponent> files){
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
            connections = new ArrayList<>();
            for(ButtonTabComponent f : files)createFile(f.getFile().getTitle(), f.getTextArea().getText());
            new ServerThread().start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public class ServerThread extends Thread{
        public void run() {
            while (alive) {
                try {
                    Socket s = serverSocket.accept();
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());

                    PacketData init = (PacketData) in.readObject();
                    /***** Create the shadows *****/
                    ArrayList<Shadow> serverShadows = new ArrayList<>();
                    ArrayList<Shadow> backupShadows = new ArrayList<>();
                    for(FileInfo f : files){
                        System.out.println(f.getId());
                        serverShadows.add(new Shadow(f.getText(), f.getId()));
                        backupShadows.add(new Shadow(f.getText(), f.getId()));
                    }
                    ConnectionInfo c = new ConnectionInfo(s, out, in, serverShadows, backupShadows, init.getName());
                    connections.add(c);

                    /***** Initialize text *****/
                    out.writeObject(new PacketData(files, 0, 0, true));
                    out.flush();
                    new ClientThread(c).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
    public class ClientThread extends Thread {
        private ConnectionInfo connectionInfo;
        private LinkedList<diff_match_patch.Diff> stack;
        public ClientThread(ConnectionInfo connectionInfo){
            this.connectionInfo = connectionInfo;
        }
        public void run(){
            while(alive) {
                try {
                    PacketData data = (PacketData) connectionInfo.getInputStream().readObject();
                    int id = data.getId();
                    System.out.println(id + " " + connectionInfo.getServerShadows().size());
                    Shadow selectedServerShadow = connectionInfo.getServerShadow(id);
                    Shadow selectedBackupShadow = connectionInfo.getBackupShadow(id);
                    //if(!data.isCreateFile())System.out.println(files.get(findText(id)).getTitle());
                    //if(!data.isCreateFile())System.out.println("CLIENT SENT: " + data.getServerVersionNumber() + " " + data.getClientVersionNumber());
                    //if(!data.isCreateFile())System.out.println("SERVER SHADOW: " + selectedServerShadow.getServerVersionNumber() + " " + selectedServerShadow.getClientVersionNumber());
                    //if(!data.isCreateFile())System.out.println("BACKUP SHADOW: " + selectedBackupShadow.getServerVersionNumber() + " " + selectedBackupShadow.getClientVersionNumber());

                    if (data.isRequestFile()) {
                        selectedServerShadow.setClientVersionNumber(0);
                        selectedServerShadow.setServerVersionNumber(0);
                        selectedBackupShadow.setClientVersionNumber(0);
                        selectedBackupShadow.setServerVersionNumber(0);
                        String text = files.get(findText(id)).getText();
                        selectedServerShadow.setText(text);
                        selectedBackupShadow.setText(text);
                        FileInfo f = files.get(findText(id));
                        connectionInfo.getOutputStream().writeObject(new PacketData(f.getTitle(), f.getText(), f.getId(), true));
                        connectionInfo.getOutputStream().flush();
                    } else if (data.isCreateFile()) {
                        String title = data.getFileName();
                        String text = data.getFileText();
                        int idx = createFile(title, text);
                        for (ConnectionInfo c : connections) {
                            c.getServerShadows().add(new Shadow(text, idx));
                            c.getBackupShadows().add(new Shadow(text, idx));
                            c.getOutputStream().writeObject(new PacketData(data.getTemp(), title, text, idx, true));
                            c.getOutputStream().flush();
                            c.getOutputStream().reset();
                        }
                    } else if (data.isRenameFile()) {
                        String title = data.getFileName();
                        FileInfo f = files.get(findText(id));
                        f.setTitle(title);
                        for (ConnectionInfo c : connections) {
                            c.getOutputStream().writeObject(new PacketData(id, title, true));
                            c.getOutputStream().flush();
                            c.getOutputStream().reset();
                        }
                    } else if(data.isDeleteFile()){
                        files.remove(findText(id));
                        for (ConnectionInfo c : connections) {
                            PacketData p = new PacketData(id);
                            p.setDeleteFile(true);
                            c.getOutputStream().writeObject(p);
                            c.getOutputStream().flush();
                            c.getOutputStream().reset();
                        }
                    } else if(selectedServerShadow == null || selectedBackupShadow == null){
                        System.out.println("what");
                    } else if (data.getClientVersionNumber() - 1 < selectedServerShadow.getClientVersionNumber()
                            && data.getServerVersionNumber() == selectedServerShadow.getServerVersionNumber()) {
                        /***** Duplicate packet protocol *****/
                        connectionInfo.getOutputStream().writeObject(
                                new PacketData(selectedServerShadow.getClientVersionNumber(),
                                        selectedServerShadow.getServerVersionNumber(), true));
                        connectionInfo.getOutputStream().flush();
                    } else if (data.getServerVersionNumber() != selectedServerShadow.getServerVersionNumber()
                            && (data.getServerVersionNumber() == selectedServerShadow.getServerVersionNumber())) {
                        /***** Packet loss on return protocol *****/

                        /***** Server shadow patches backup *****/
                        LinkedList<diff_match_patch.Diff> serverToBackup = diffServerShadowToBackupShadow(selectedServerShadow, selectedBackupShadow);
                        updateServerShadow(selectedServerShadow, serverToBackup, data.getClientVersionNumber());

                        /***** Patch only server text from client edits *****/
                        stack = data.getDiffQueue();
                        int selectedText = findText(id);
                        String result = files.get(selectedText).getText();
                        result = updateServerText(stack, result);
                        setServerText(selectedText, result);

                        /***** Create backup *****/
                        LinkedList<diff_match_patch.Diff> backupDiff = diffBackupShadow(
                                selectedServerShadow, selectedBackupShadow);
                        updateBackupShadow(selectedBackupShadow, backupDiff);
                        selectedBackupShadow.setServerVersionNumber(selectedServerShadow.getServerVersionNumber());
                        selectedBackupShadow.setClientVersionNumber(data.getClientVersionNumber());

                        /***** Increase server version number *****/
                        selectedServerShadow.setServerVersionNumber(selectedServerShadow.getServerVersionNumber() + 1);

                        /***** Patch server shadow with server text *****/
                        LinkedList<diff_match_patch.Diff> shadowDiff = diffServerShadow(selectedServerShadow, files.get(selectedText).getText());
                        updateServerShadow(selectedServerShadow, shadowDiff, data.getClientVersionNumber());

                        /***** Send packet *****/
                        connectionInfo.getOutputStream().writeObject(new PacketData(shadowDiff, data.getClientVersionNumber(), selectedServerShadow.getServerVersionNumber(), true, id));
                        connectionInfo.getOutputStream().flush();
                    } else if(data.getClientVersionNumber()-1 != selectedServerShadow.getClientVersionNumber()
                            && data.getServerVersionNumber() != selectedServerShadow.getServerVersionNumber()){
                        /***** Reinitialize client due to irreparable problems *****/
                        int selectedText = findText(id);
                        Shadow serverShadow = new Shadow(selectedServerShadow.getId());
                        LinkedList<diff_match_patch.Diff> diffs = diffServerShadow(serverShadow, files.get(selectedText).getText());
                        updateServerShadow(serverShadow, diffs, 0);
                        Shadow backupShadow = new Shadow(selectedBackupShadow.getId());
                        updateBackupShadow(backupShadow, diffBackupShadow(serverShadow, backupShadow));

                        connectionInfo.setServerShadowAt(serverShadow, connectionInfo.getServerShadowIndex(id));
                        connectionInfo.setBackupShadowAt(backupShadow, connectionInfo.getBackupShadowIndex(id));

                        PacketData d = new PacketData(files.get(selectedText).getText(), 0, 0, true, id);
                        connectionInfo.getOutputStream().writeObject(d);
                        connectionInfo.getOutputStream().flush();
                    } else {
                        /***** Patch server shadow text and server text *****/
                        stack = data.getDiffQueue();
                        int selectedText = findText(id);
                        String result = files.get(selectedText).getText();
                        updateServerShadow(selectedServerShadow, stack, data.getClientVersionNumber());
                        result = updateServerText(stack, result);
                        setServerText(selectedText, result);
                        System.out.println("UPDATED: " + result);

                        /***** Create backup *****/
                        LinkedList<diff_match_patch.Diff> backupDiff = diffBackupShadow(
                                selectedServerShadow, selectedBackupShadow);
                        updateBackupShadow(selectedBackupShadow, backupDiff);
                        selectedBackupShadow.setServerVersionNumber(selectedBackupShadow.getServerVersionNumber());
                        selectedBackupShadow.setClientVersionNumber(data.getClientVersionNumber());

                        /***** Increase server version number *****/
                        selectedServerShadow.setServerVersionNumber(selectedServerShadow.getServerVersionNumber()+1);

                        /***** Patch server shadow with server text *****/
                        LinkedList<diff_match_patch.Diff> shadowDiff = diffServerShadow(selectedServerShadow, files.get(selectedText).getText());
                        updateServerShadow(selectedServerShadow, shadowDiff, data.getClientVersionNumber());

                        /***** Send packet *****/
                        connectionInfo.getOutputStream().writeObject(new PacketData(shadowDiff, data.getClientVersionNumber(), selectedServerShadow.getServerVersionNumber(), true, id));
                        connectionInfo.getOutputStream().flush();
                    }
                    connectionInfo.getOutputStream().reset();
                } catch (EOFException e){
                    connections.remove(connectionInfo);
                    try {
                        connectionInfo.getOutputStream().close();
                        connectionInfo.getInputStream().close();
                    } catch (Exception ze){
                        ze.printStackTrace();
                    }
                    return;
                } catch (Exception e) {
                    try {
                        connectionInfo.getOutputStream().close();
                        connectionInfo.getInputStream().close();
                    } catch (Exception ze){
                        ze.printStackTrace();
                    }
                    e.printStackTrace();
                    return;
                }
            }
            try {
                connectionInfo.getOutputStream().close();
                connectionInfo.getInputStream().close();
            } catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("FINISHED " + connectionInfo.getName());
        }
    }
    public int findText(int id){
        int c = 0;
        for(FileInfo f : files){
            if(f.getId() == id)return c;
            c++;
        }
        return -1;
    }
    private synchronized void setServerText(int selectedText, String text){
        this.files.get(selectedText).setText(text);
    }

    private synchronized int createFile(String title, String text){
        boolean check = false;
        int id = (int)(Math.random()*((Integer.MAX_VALUE)));
        while(!check){
            check = true;
            for(FileInfo f : files)if(f.getId() == id)check = false;
            if(check)break;
            id = (int)(Math.random()*((Integer.MAX_VALUE)));
        }
        FileInfo f = new FileInfo(title, text, text, id);
        files.add(f);
        return id;
    }

    private String updateServerText(LinkedList<diff_match_patch.Diff> diffs, String text){
        diff_match_patch d = new diff_match_patch();
        LinkedList<diff_match_patch.Patch> patches = d.patch_make(diffs);
        return (String)d.patch_apply(patches, text)[0];
    }

    /***** Server shadow diff and patch *****/
    private LinkedList<diff_match_patch.Diff> diffServerShadow(Shadow serverShadow, String text){
        diff_match_patch d = new diff_match_patch();

        String serverShadowText = serverShadow.getText();
        LinkedList<diff_match_patch.Diff> diffs = d.diff_main(serverShadowText, text);
        d.diff_cleanupEfficiency(diffs);

        return diffs;
    }
    private void updateServerShadow(Shadow serverShadow, LinkedList<diff_match_patch.Diff> diffs, int n){
        diff_match_patch d = new diff_match_patch();
        String serverShadowText = serverShadow.getText();
        LinkedList<diff_match_patch.Patch> patches = d.patch_make(diffs);
        String result = (String)d.patch_apply(patches, serverShadowText)[0];
        serverShadow.setText(result);
        serverShadow.setClientVersionNumber(n);
    }

    /***** Backup shadow diff and patch *****/
    private LinkedList<diff_match_patch.Diff> diffBackupShadow(Shadow serverShadow, Shadow backupShadow){
        diff_match_patch d = new diff_match_patch();
        String serverShadowText = serverShadow.getText();
        String backupShadowText = backupShadow.getText();
        LinkedList<diff_match_patch.Diff> diffs = d.diff_main(backupShadowText, serverShadowText);
        d.diff_cleanupEfficiency(diffs);
        return diffs;
    }
    private LinkedList<diff_match_patch.Diff> diffServerShadowToBackupShadow(Shadow serverShadow, Shadow backupShadow){
        diff_match_patch d = new diff_match_patch();
        String serverShadowText = serverShadow.getText();
        String backupShadowText = backupShadow.getText();
        LinkedList<diff_match_patch.Diff> diffs = d.diff_main(serverShadowText, backupShadowText);
        d.diff_cleanupEfficiency(diffs);
        return diffs;
    }
    private void updateBackupShadow(Shadow backupShadow, LinkedList<diff_match_patch.Diff> diffs) {
        diff_match_patch d = new diff_match_patch();
        String backupShadowText = backupShadow.getText();
        LinkedList<diff_match_patch.Patch> patches = d.patch_make(diffs);
        String result = (String)d.patch_apply(patches, backupShadowText)[0];
        backupShadow.setText(result);
    }
}
