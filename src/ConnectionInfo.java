import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Rui Li on 17/03/17.
 */
public class ConnectionInfo {

    /***** Connection info class that encapsulates all user data for Server reference *****/

    private Socket connectionSocket; /***** Socket that represents the connection *****/
    private ObjectOutputStream outputStream; /***** Output stream to send PacketData objects *****/
    private ObjectInputStream inputStream; /***** Input stream to read PacketData objects *****/
    private ArrayList<Shadow> serverShadows; /***** Server shadow that is synced with client text *****/
    private ArrayList<Shadow> backupShadows; /***** In case of packet loss or time outs, this provides a backup *****/
    private String name; /***** Name of client *****/

    /***** Constructors *****/
    public ConnectionInfo(Socket connectionSocket, ObjectOutputStream outputStream, ObjectInputStream inputStream, String name){
        this.connectionSocket = connectionSocket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.name = name;
    }
    public ConnectionInfo(Socket connectionSocket, ObjectOutputStream outputStream, ObjectInputStream inputStream, ArrayList<Shadow> serverShadows, ArrayList<Shadow> backupShadows, String name){
        this.connectionSocket = connectionSocket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.serverShadows = serverShadows;
        this.backupShadows = backupShadows;
        this.name = name;
    }
    public ConnectionInfo(){}

    public int getServerShadowIndex(int id){
        int c = 0;
        for(Shadow s : serverShadows){
            if(s.getId() == id)return c;
            c++;
        }
        return -1;
    }
    public int getBackupShadowIndex(int id){
        int c = 0;
        for(Shadow s : backupShadows){
            if(s.getId() == id)return c;
            c++;
        }
        return -1;
    }

    public Shadow getServerShadow(int id){
        for(Shadow s : serverShadows){
            if(s.getId() == id)return s;
        }
        return null;
    }
    public Shadow getBackupShadow(int id){
        for(Shadow s : backupShadows){
            if(s.getId() == id)return s;
        }
        return null;
    }

    public void setServerShadowAt(Shadow shadow, int idx){
        if(idx >= 0 && idx < serverShadows.size()) {
            serverShadows.remove(idx);
            serverShadows.add(idx, shadow);
        }
    }
    public void setBackupShadowAt(Shadow shadow, int idx){
        if(idx >= 0 && idx < backupShadows.size()) {
            backupShadows.remove(idx);
            backupShadows.add(idx, shadow);
        }
    }

    /***** Getters and setters for member variables *****/
    public Socket getSocket() {
        return connectionSocket;
    }
    public void setSocket(Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }
    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }
    public void setOutputStream(ObjectOutputStream outputStream) {
        this.outputStream = outputStream;
    }
    public ObjectInputStream getInputStream() {
        return inputStream;
    }
    public void setInputStream(ObjectInputStream inputStream) {
        this.inputStream = inputStream;
    }
    public ArrayList<Shadow> getServerShadows() {
        return serverShadows;
    }
    public void setServerShadows(ArrayList<Shadow> serverShadows) {
        this.serverShadows = serverShadows;
    }
    public ArrayList<Shadow> getBackupShadows() {
        return backupShadows;
    }
    public void setBackupShadows(ArrayList<Shadow> backupShadows) {
        this.backupShadows = backupShadows;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
