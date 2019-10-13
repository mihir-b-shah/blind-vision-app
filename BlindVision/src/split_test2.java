
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class split_test2 {

    private static final HashMap<String, Double> map;
    private static int ct = 0;

    static {
        map = new HashMap<>();
        BufferedReader br;

        try {
            br = new BufferedReader(new FileReader("dict.txt"));

            String line;
            while ((line = br.readLine()) != null) {
                map.put(line, 0D);
            }
            br.close();
            read("Harry_Potter_-_Rowling,_J_K_-_Harry_Potter_and_the_Sorcerer's_Stone.txt");
            read("huckabee.txt");
            read("eea.txt");
            read("game_change.txt");

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                entry.setValue(entry.getValue() / ct);
            }

            PrintWriter pw = new PrintWriter("dict_final.txt");
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                pw.printf("%s,%.10f%n", entry.getKey(), entry.getValue());
            }

            pw.close();

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void read(String file) throws IOException {
        Scanner f1 = new Scanner(new File(file));
        String past = "";
        while (f1.hasNext()) {
            String word = f1.next();
            int index = word.indexOf('-');
            if(index != -1) {
                past = word.substring(0, index);
                continue;
            }
            word += past;
            word = word.replaceAll("\\W+", "").toLowerCase();
            if (!word.isEmpty()) {
                if (map.containsKey(word)) {
                    map.put(word, map.get(word) + 1);
                }
                ++ct;
            }
            past = "";
        }

        f1.close();
    }

    public static void main(String[] args) {
            
    }
}
