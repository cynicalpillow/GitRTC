import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Rui Li on 17/03/17.
 */
public class PacketData implements Serializable{

    /***** Class that represents each packet of info sent to server from client and vice versa *****/

    private static final long serialVersionUID = 5950169519310163575L;

    private LinkedList<diff_match_patch.Diff> diffs; /***** Queue of diffs to apply *****/
    private int n; /***** Client version number *****/
    private int m; /***** Server version number *****/
    private String name; /***** Name of client sending info *****/
    private boolean performed; /***** Server returns state of change *****/
    private boolean reinit = false; /***** Reinitializing when out of sync *****/
    private int id = -1; /***** ID of currently edited file *****/
    private boolean createFile = false; /***** Create file or nah? *****/
    private boolean deleteFile = false; /***** Delete file or nah? *****/
    private boolean requestFile = false; /***** Request file or nah? *****/
    private boolean renameFile = false; /***** Rename file or nah? *****/
    private String fileName; /***** File name *****/
    private String fileText; /***** File text *****/
    private ArrayList<FileInfo> files; /***** Array of files for initialization *****/
    private File temp = null; /***** For use by the person creating the file, this ensures it receives the correct file save location as well when opening a file to collaboration *****/

    /***** Constructors *****/
    public PacketData(ArrayList<FileInfo> files, int n, int m, boolean performed){
        this.files = files;
        this.n = n;
        this.m = m;
        this.performed = performed;
    }
    public PacketData(LinkedList<diff_match_patch.Diff> diffs, int n, int m, boolean performed, int id){
        this.diffs = diffs;
        this.n = n;
        this.m = m;
        this.performed = performed;
        this.id = id;
    }
    public PacketData(LinkedList<diff_match_patch.Diff> diffs, int n, int m, int id){
        this.diffs = diffs;
        this.n = n;
        this.m = m;
        this.id = id;
    }
    public PacketData(String fileText, int n, int m, boolean reinit, int id){
        this.fileText = fileText;
        this.reinit = reinit;
        this.n = n;
        this.m = m;
        this.id = id;
    }
    public PacketData(int n, int m, boolean performed){
        this.n = n;
        this.m = m;
        this.performed = performed;
    }
    public PacketData(String name, boolean performed){
        this.name = name;
        this.n = n;
        this.performed = performed;
    }
    public PacketData(File temp, String fileName, String fileText, int id, boolean createFile){
        this.id = id;
        this.fileName = fileName;
        this.fileText = fileText;
        this.createFile = createFile;
        this.temp = temp;
    }
    public PacketData(File temp, String fileName, String fileText, boolean createFile){
        this.temp = temp;
        this.fileName = fileName;
        this.fileText = fileText;
        this.createFile = createFile;
    }
    public PacketData(int id, boolean requestFile){
        this.id = id;
        this.requestFile = requestFile;
    }
    public PacketData(String fileName, String fileText, int id, boolean requestFile){
        this.fileName = fileName;
        this.fileText = fileText;
        this.id = id;
        this.requestFile = requestFile;
    }
    public PacketData(int id){
        this.id = id;
    }

    public PacketData(int id, String fileName, boolean renameFile){
        this.id = id;
        this.fileName = fileName;
        this.renameFile = renameFile;
    }

    /***** Getters and setters for member variables *****/
    public int getClientVersionNumber() {
        return n;
    }
    public void setClientVersionNumber(int n) {
        this.n = n;
    }
    public int getServerVersionNumber() {
        return m;
    }
    public void setServerVersionNumber(int m) {
        this.m = m;
    }
    public LinkedList<diff_match_patch.Diff> getDiffQueue() {
        return diffs;
    }
    public void setDiffQueue(LinkedList<diff_match_patch.Diff> diffs) {
        this.diffs = diffs;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isPerformed() {
        return performed;
    }
    public void setPerformed(boolean performed) {
        this.performed = performed;
    }
    public boolean isReinit() {
        return reinit;
    }
    public void setReinit(boolean reinit) {
        this.reinit = reinit;
    }
    public boolean isCreateFile() {
        return createFile;
    }
    public void setCreateFile(boolean createFile) {
        this.createFile = createFile;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public ArrayList<FileInfo> getFiles() {
        return files;
    }
    public void setFiles(ArrayList<FileInfo> files) {
        this.files = files;
    }
    public String getFileText() {
        return fileText;
    }
    public void setFileText(String fileText) {
        this.fileText = fileText;
    }
    public File getTemp() {
        return temp;
    }
    public void setTemp(File temp) {
        this.temp = temp;
    }
    public boolean isDeleteFile() {
        return deleteFile;
    }
    public void setDeleteFile(boolean deleteFile) {
        this.deleteFile = deleteFile;
    }
    public boolean isRequestFile() {
        return requestFile;
    }
    public void setRequestFile(boolean requestFile) {
        this.requestFile = requestFile;
    }
    public boolean isRenameFile() {
        return renameFile;
    }
    public void setRenameFile(boolean renameFile) {
        this.renameFile = renameFile;
    }
}
