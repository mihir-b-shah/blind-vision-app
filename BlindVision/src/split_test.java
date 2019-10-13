
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class split_test {

    private static final HashSet_String dict;
    private static int maxsize;

    static {
        dict = new HashSet_String(3);
        BufferedReader br = null;
        maxsize = 0;

        try {
            br = new BufferedReader(new FileReader("dict.txt"));

            String line;
            while ((line = br.readLine()) != null) {
                maxsize = Math.max(maxsize, line.length());
                dict.add(line);
            }

            System.out.println(dict);
            br.close();

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void split(String word) {
        word = word.toLowerCase();
        String[] words_gen = word.split(" ");
        boolean[] possible = new boolean[word.length() + 1];
        possible[word.length()] = true;
        ArrayList<String> words = new ArrayList<>();

        for (int i = word.length() - 1; i >= 0; --i) {            
            boolean or = false;
            for (int j = 1; j <= maxsize; ++j) {          
                if (i + j <= word.length()) {
                    or |= dict.contains(word, i, i + j);
                }
            }
            possible[i] = or;
        }
        System.out.println(Arrays.toString(possible));
    }

    public static void main(String[] args) {
        split("redhonda");
    }
}
