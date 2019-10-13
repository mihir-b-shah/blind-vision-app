
import java.util.Arrays;

public class Vector_String {
    private String[] ref;
    private int size;
    
    public Vector_String() {
        ref = new String[2];
    }
    
    public void add(String s) {
        if(size == ref.length) {
            String[] aux = new String[size << 1];
            System.arraycopy(ref, 0, aux, 0, size);
            ref = aux;
        }
        ref[size++] = s;
    }
    
    public boolean contains(String s, int str, int e) {
        int st = 0;
        int end = size;
        int mid;
        int comp;
        
        while(st < end) {
            mid = (st+end) >> 1;
            comp = compare(ref[mid], s, str, e);
            if(comp == 0) {
                return true;
            } else if(comp > 0){
                end = mid;
            } else {
                st = mid+1;
            }
            
            //System.out.printf("%d %d%n", st, end);
        }
        
        return false;
    }
    
    // copy of jdk8's string compare
    public int compare(String s1, String s2, int st2, int end2) {
        
        int lim = Math.min(s1.length(), end2-st2);
        int k = 0;
        while (k < lim) {
            char c1 = s1.charAt(k);
            char c2 = s2.charAt(k+st2);
            if (c1 != c2) {
                return c1 - c2;
            }
            ++k;
        }
        return s1.length()-end2+st2;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(int i = 0; i<size; ++i) {
            sb.append(ref[i]);
            sb.append(',');
            sb.append(' ');
        }
        if(size>0) {
            sb.deleteCharAt(sb.length()-1);
            sb.deleteCharAt(sb.length()-1);
        }
        
        sb.append(']');
        return sb.toString();
    }
}
