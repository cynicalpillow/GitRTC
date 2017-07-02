/**
 * Created by Rui Li on 17/03/17.
 */
public class Shadow {

    /***** Shadow class for text values and client/server version numbers *****/

    private String text; /***** The shadow's text *****/
    private int n; /***** Client version number *****/
    private int m; /***** Server version number *****/
    private int id;
    //private String caretDotContextSuffix;
    //private String caretMarkContextSuffix;
    private int caretDotPos = 0;
    private int caretMarkPos = 0;
    //private boolean newLineDot = false;
    //private boolean newLineMark = false;

    /***** Constructors *****/
    public Shadow(){
        this.text = "";
        this.n = 0;
        this.m = 0;
    }
    public Shadow(int id){
        this.text = "";
        this.id = id;
        this.n = 0;
        this.m = 0;
    }
    public Shadow(String text){
        this.text = text;
        this.n = 0;
        this.m = 0;
    }
    public Shadow(String text, int id){
        this.text = text;
        this.id = id;
        this.n = 0;
        this.m = 0;
    }

    /***** Getters and setters for member variables *****/
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
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
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getCaretDotPos() {
        return caretDotPos;
    }
    public void setCaretDotPos(int caretDotPos) {
        this.caretDotPos = caretDotPos;
    }
    public int getCaretMarkPos() {
        return caretMarkPos;
    }
    public void setCaretMarkPos(int caretMarkPos) {
        this.caretMarkPos = caretMarkPos;
    }
}
