import java.io.DataInput;
import java.util.LinkedList;

/**
 * Created by Rui Li on 17/03/17.
 */
public class Testing {
    public static void main(String args[]){
        String one = "public static void main(String args[]){\n\tScanner s = new Scanner(System.in);\n\tSystem.out.println(\"Hello World!\");\n\tSystem.out.println(\"Hello World!\");\n}";
        String two = "public static void main(String args[]){\n\tScanner input = new Scanner(System.in);\n\tSystem.out.println(\"Hello World!\");\n\tSystem.out.println(\"Hello World!\"+\"=\"+5);\n}";
        diff_match_patch d = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> diffs = d.diff_main(one, two);
        d.diff_cleanupSemantic(diffs);
        for(diff_match_patch.Diff x: diffs){
            //System.out.println(x.operation.name() + ": " + x.text);
        }
        String caretContextSuffix = "";
        LinkedList<diff_match_patch.Patch> patches = d.patch_make(diffs);
        String result = (String)d.patch_apply(patches, one)[0];
        //System.out.println(one);
        //System.out.println(result);
        //System.out.println(d.match_main(result, caretContextSuffix, 112));
        //System.out.println(result.substring(112));
        //String s = "\n\n\n\n\n\n\n\n";
        //String search = "\n\nhey t\n\n\n\n";
        //System.out.println(d.match_main(search, s, 1));
        //System.out.println((int)(Math.random()*((Integer.MAX_VALUE))));
        String start = "Hey my name is rui\nHi";
        String end = "Hey my name is Alex\n\nHi";
        System.out.println(end.length());
        LinkedList<diff_match_patch.Diff> d1 = d.diff_main(start, end);
        d.diff_cleanupEfficiency(d1);
        System.out.println(d.diff_xIndex(d1, 18));
    }
}
