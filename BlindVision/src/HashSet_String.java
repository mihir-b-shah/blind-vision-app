
import java.util.Arrays;

public class HashSet_String {

    private final Vector_String[] hashtable;
    private final int N;

    public HashSet_String(int N) {
        this.N = N;
        hashtable = new Vector_String[(int) (N*1.3)];
        for(int i = 0; i<hashtable.length; ++i) {
            hashtable[i] = new Vector_String();
        }
    }

    public void add(String s) {
        hashtable[mod(s.hashCode())].add(s);
    }
    
    public int mod(int i) {
        return Integer.signum(i)*(i%hashtable.length);
    }

    public boolean contains(String s, int st, int end) {
        //System.out.printf("%s: %d%n", s.substring(st,end), mod(string_hashcode(s,st,end)));
        return hashtable[mod(string_hashcode(s,st,end))].contains(s,st,end);
    }

    public static final int string_hashcode(String s, int st, int end) {
        int h = 0;
        for (int i = st; i<end; ++i) {
            h = 0x1f * h + s.charAt(i);
        }
        return h;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(hashtable);
    }
}
