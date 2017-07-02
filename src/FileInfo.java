import java.io.File;
import java.io.Serializable;

/**
 * Created by Rui Li on 27/03/17.
 */
public class FileInfo implements Serializable{
    private static final long serialVersionUID = 5950169519410163575L;
    /***** File info class for organizing file tabs *****/
    private File file;
    private String originalText;
    private String text;
    private int id;
    private String title;
    public FileInfo(){
        title = "untitled";
    }
    public FileInfo(String title, String text, int id){
        this.title = title;
        this.text = text;
        this.originalText = text;
        this.id = id;
    }
    public FileInfo(int id){
        this.id = id;
    }
    public FileInfo(File file, String title, String originalText, int id){
        this.file = file;
        this.originalText = originalText;
        this.text = originalText;
        this.id = id;
        this.title = title;
    }
    public FileInfo(File file, String title, String text, String originalText, int id){
        this.file = file;
        this.originalText = originalText;
        this.text = text;
        this.id = id;
        this.title = title;
    }
    public FileInfo(String title, String text, String originalText, int id){
        this.title = title;
        this.text = text;
        this.originalText = originalText;
        this.id = id;
    }
    public String toString(){
        return this.title;
    }
    public File getFile(){
        return file;
    }
    public String getOriginalText(){
        return originalText;
    }
    public void setFile(File f){
        file = f;
    }
    public void setOriginalText(String originalText){
        this.originalText = originalText;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) { this.id = id; }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
