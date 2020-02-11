package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.textrazor.AnalysisException;
import com.textrazor.NetworkException;
import com.textrazor.TextRazor;
import com.textrazor.annotations.Response;
import com.textrazor.annotations.Word;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static com.apps.navai.MainActivity.INT_1;

public class SpellCheck extends IntentService {

    private static final int DELETE_MASK = 20;
    private static final int INSERT_MASK = 10;
    private static final int RIGHT_MASK = 0x3ff;

    private static final int INSERT_ONE = 0x400;
    private static final int DELETE_ONE = 0x100000;

    private static LongSet dictionary;

    public SpellCheck() {
        super("SpellCheck");
    }

    public static void loadDict() {
        dictionary = new LongSet(8191);
        try {
            BufferedReader br = new BufferedReader(new FileReader("hashdict.txt"));
            String line;
            while((line = br.readLine()) != null) {
                dictionary.insert(Long.parseLong(line));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void freeDict() {
        dictionary = null;
    }

    public static String findSpaces(String str) {
        if(str.length() > 64) return str;
        long[] dp = new long[str.length()+1];
        for(int i = 1; i<1+str.length(); ++i) {
            final int LIM = Math.min(i+1,13);
            long base = 0L;
            for(int j = 1; j<LIM; ++j) {
                base <<= 5;
                base += str.charAt(i-j)-96L;
                if(dictionary.contains(base)) {
                    dp[i] = dp[i-j]+(i==64?0:(1L<<i));
                }
            }
        }
        long aux = dp[str.length()];
        StringBuilder sb = new StringBuilder();

        int ptr = 0;
        aux >>>= 1;

        while(aux > 0) {
            int jump = 1+Long.numberOfTrailingZeros(aux);
            sb.append(str, ptr, ptr+jump); sb.append(' ');
            ptr += jump;
            aux >>>= jump;
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    // to be implemented
    public static String condenseSpaces(String phrase) {
        // we utilize a UFDS to model the problem.
        String[] words = Arrays.stream(phrase.split(" ")).
                map(String::toLowerCase).toArray(String[]::new);
        int[] ufds = new int[words.length];
        boolean[] cont = new boolean[words.length];
        for(int i = 0; i<words.length-1; ++i) {
            long hash = hash(words[i]+words[i+1], 0,
                    words[i].length()+words[i+1].length());
            if(dictionary.contains(hash)) {
                cont[i] = true;
            }
        }


    }

    public static final long hash(String str, int s, int e) {
        long hash = 0;
        for(int i = 0; i<e-s; ++i) {
            hash += str.charAt(i+s)-96L << 5*i;
        }
        return hash;
    }

    class FloatVector implements Serializable {
        private float[] data;
        private int ptr;

        FloatVector() {
            data = new float[4];
        }

        void add(float dat) {
            if(ptr == data.length) {
                float[] aux = new float[ptr << 1];
                System.arraycopy(data, 0, aux, 0, ptr);
                data = aux;
            }
            data[ptr++] = dat;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for(int i = 0; i<ptr; ++i) {
                sb.append(data[i]);
                sb.append(','); sb.append(' ');
            }
            sb.delete(sb.length()-2, sb.length());
            sb.append(']');
            return sb.toString();
        }

        public float get(int i) {
            return data[i];
        }

        public void writeObject(ObjectOutputStream oos) throws IOException {
            oos.writeInt(data.length);
            for(int i = 0; i<ptr; ++i) {
                oos.writeFloat(data[i]);
            }
            oos.writeFloat(Float.NaN);
        }

        public void readObject(ObjectInputStream ois) throws IOException {
            data = new float[data.length];
            ptr = 0;
            float buffer;
            while(!Float.isNaN(buffer = ois.readFloat())) {
                data[ptr++] = buffer;
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String[] data = intent.getStringArrayExtra(MainActivity.STRING_ARRAY_1);
        String input = join(data);
        TextRazor textRazor = new TextRazor(getString(R.string.textrazor_key));
        textRazor.setExtractors(Arrays.asList("spelling"));

        ArrayList<String> output = new ArrayList<>();
        FloatVector conf = new FloatVector();

        try {
            Response result = textRazor.analyze(input).getResponse();
            final List<Word> sentences = result.getWords();

            for (Word word : sentences) {
                String curr = input.substring(word.getStartingPos(), word.getEndingPos());
                if(curr.length() == 0) continue;
                final List<Word.Suggestion> suggestions = word.getSpellingSuggestions();

                String opt = null;
                double minDist = 1_000_000_000;
                double currDist;

                final int size = suggestions.size();

                Word.Suggestion sug;
                for (int i = 0; i < size; ++i) {
                    sug = suggestions.get(i);
                    System.out.println(sug.getSuggestion());
                    if (minDist > (currDist = sug.getScore() *
                            (1 - weightedEditDistance(curr, sug.getSuggestion()) / curr.length()))) {
                        minDist = currDist;
                        opt = sug.getSuggestion();
                    }
                }

                conf.add((float) minDist);
                output.add(curr);
                output.add(opt);
            }
        } catch (NetworkException | AnalysisException e) {
            System.err.println(e.getMessage());
        } finally {
            Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
            out.putExtra("output", output);
            out.putExtra("conf", conf);
            out.putExtra(INT_1, intent.getIntExtra(INT_1, -1));
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(out);
            stopSelf();
        }
    }

    public static String join(String[] data) {
        System.out.println(Arrays.toString(data));
        boolean flag = false;
        StringBuilder sb = new StringBuilder();
        for (String s : data) {
            flag = true;
            sb.append(s);
            sb.append('\t');
        }
        if(flag) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private static double collapse(int one) {
        return (((one >>> DELETE_MASK) + (one >>> INSERT_MASK)) & RIGHT_MASK) * 1.5
                + (one & RIGHT_MASK);
    }

    /**
     * Utility min function.
     *
     * @param one   a replace arg
     * @param two   an insert arg
     * @param three a delete arg
     * @return formatted minimum.
     */
    private static int packMin(int one, int two, int three, double dist) {
        double v1 = collapse(one);
        double v2 = collapse(two);
        double v3 = collapse(three);
        int bias = (int) (4 * dist + 1); // constrain on range 1,3.
        if ((bias < 1 || bias > 3)) throw new AssertionError();

        if (v1 > v2) {
            if (v1 > v3) {
                return one + 1;
            } else {
                return two + INSERT_ONE * bias;
            }
        } else {
            if (v1 > v3) {
                return three + DELETE_ONE * bias;
            } else {
                return one + 1;
            }
        }
    }

    private static double weightedEditDistance(String word1, String word2) {
        int[][] dp = new int[2][1 + word1.length()];
        for (int i = 0; i < 1 + word1.length(); ++i)
            dp[0][i] = i << DELETE_MASK;

        for (int i = 1; i < 1 + word2.length(); ++i) {
            //print2DArray(dp, SpellCheck::collapse);
            for (int j = 1; j < 1 + word1.length(); ++j) {
                if (word2.charAt(i - 1) == word1.charAt(j - 1)) {
                    dp[1][j] = dp[0][j - 1];
                } else {
                    dp[1][j] = packMin(dp[0][j - 1], dp[0][j], dp[1][j - 1],
                            Math.min(j - 1, word1.length() - j) / word1.length());
                }
            }

            dp[0] = dp[1];
            dp[1] = new int[1 + word1.length()];
            dp[1][0] = i << INSERT_MASK;
        }

        int val = dp[0][word1.length()];
        return (val & RIGHT_MASK) + (((val >>> INSERT_MASK) & RIGHT_MASK)
                + ((val >>> DELETE_MASK) & RIGHT_MASK)) * 1.5;
    }

    private static int editDistance(String word1, String word2) {
        // space saving optimization
        if (word1.length() > word2.length()) {
            String temp = word2;
            word2 = word1;
            word1 = temp;
        }
        int[][] dp = new int[2][1 + word1.length()];
        for (int i = 0; i < 1 + word1.length(); ++i)
            dp[0][i] = i;

        for (int i = 1; i < 1 + word2.length(); ++i) {
            for (int j = 1; j < 1 + word1.length(); ++j) {
                if (word2.charAt(i - 1) == word1.charAt(j - 1)) {
                    dp[1][j] = dp[0][j - 1];
                } else {
                    dp[1][j] = 1 + Math.min(dp[0][j - 1], Math.min(dp[0][j], dp[1][j - 1]));
                }
            }

            dp[0] = dp[1];
            dp[1] = new int[1 + word1.length()];
            dp[1][0] = i;
        }

        return dp[0][word1.length()];
    }
}