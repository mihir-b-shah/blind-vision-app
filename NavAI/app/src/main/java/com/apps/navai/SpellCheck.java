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

    public static String condenseSpaces(String phrase) {
        String[] words = Arrays.stream(phrase.trim().split("\\s+")).
                map(String::toLowerCase).toArray(String[]::new);
        if(words.length>63) return phrase;

        int[] prefix = new int[words.length+1];
        long[] hashes = Arrays.stream(words).mapToLong(SpellCheck::hash).toArray();
        for(int i = 0; i<words.length; ++i) {
            prefix[i+1] = words[i].length() + prefix[i];
        }

        int[] best16 = new int[(words.length >>> 4) + 1];

        for(int k = 0; k<words.length; k+=0x10000) {
            final int LIM = words.length-k < 16 ?
                    (1<<words.length-k-1)-1 : 0x7fff;
            final int LIM_LEN = Math.min(16, words.length-k);
            int best = LIM;
            for(int i = 0; i<LIM; ++i) {
                // probably faster than asymptotically better DP
                long res = longestZeroString(i, LIM_LEN);
                int pos = (int) (res >>> 32);
                int len = (int) (res & 0x7fff_ffff);
                if(prefix[Math.min(pos+len+1,words.length)]-prefix[pos] > 12) {
                    continue;
                }
                // guarantee that this word is length 12 or less

                long hash = hash(words[k]);
                int currLen = words[k].length();
                boolean possible = true;
                int ptr = 1;

                while(ptr < LIM_LEN) {
                    if((i & 1 << ptr-1) == 0) {
                        hash += hashes[k+ptr] << 5*currLen;
                        currLen += words[k+ptr].length();
                    } else {
                        if(!dictionary.contains(hash)) {
                            possible = false;
                            break;
                        }
                        // preload the next one
                        hash = hash(words[k+ptr]);
                        currLen = words[k+ptr].length();
                    }
                    ++ptr;
                }
                if(possible && popCount(i) < popCount(best)) {
                    best = i;
                }
            }
            best16[k >>> 16] = best;
        }

        // generate the string
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<words.length; i+=16) {
            int aux = best16[i];
            for(int j = 0; j<Math.min(16, words.length-i); ++j) {
                sb.append(words[i+j]);
                if((aux&1) == 1) sb.append(' ');
                aux >>>= 1;
            }
        }

        return sb.toString().trim();
    }

    private static int popCount(int x) {
        x = (x & 0x55555555) + ((x >> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >> 2) & 0x33333333);
        x = (x & 0x0F0F0F0F) + ((x >> 4) & 0x0F0F0F0F);
        x = (x & 0x00FF00FF) + ((x >> 8) & 0x00FF00FF);
        return (x & 0x0000FFFF) + ((x >> 16) & 0x0000FFFF);
    }

    // why does java not have goto? nooooooooo
    private static final long longestZeroString(int x, int len) {
        x = ~x & (1 << len) - 1;
        int y,s,apos = 0;
        if (x == 0) {
            apos = len;
            return (long) apos << 32;
        }
        y = x & (x << 1);
        if (y == 0) {
            s = 1;
            apos = 32-Integer.numberOfLeadingZeros(x)-s;
            return ((long) apos << 32) + s;
        }
        x = y & (y << 2);
        if (x == 0) {
            s = 2;
            x = y;
            y = x & (x << 1);
            if (y != 0) {
                s += 1;
                x = y;
            }
            apos = 32-Integer.numberOfLeadingZeros(x)-s;
            return ((long) apos << 32) + s;
        }
        y = x & (x << 4);
        if (y == 0) {
            s = 4;
            y = x & (x << 2);
            if (y != 0) {
                s += 2;
                x = y;
            }
            y = x & (x << 1);
            if (y != 0) {
                s += 1;
                x = y;
            }
            apos = 32-Integer.numberOfLeadingZeros(x)-s;
            return ((long) apos << 32) + s;
        }
        x = y & (y << 8);
        if (x == 0) {
            s = 8;
            x = y;
            y = x & (x << 4);
            if (y != 0) {
                s += 4;
                x = y;
            }
            y = x & (x << 2);
            if (y != 0) {
                s += 2;
                x = y;
            }
            y = x & (x << 1);
            if (y != 0) {
                s += 1;
                x = y;
            }
            apos = 32-Integer.numberOfLeadingZeros(x)-s;
            return ((long) apos << 32) + s;
        }
        if (x == 0xFFFF8000) {
            apos = 0;
            return len + ((long) apos << 32);
        }
        s = 16;
        return ((long) apos << 32) + s;
    }

    private static final long hash(String str) {
        long hash = 0;
        for(int i = 0; i<str.length(); ++i) {
            hash += str.charAt(i)-96L << 5*i;
        }
        return hash;
    }

    private static final long hash(String str, int s, int e) {
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
            System.out.println("Got to finally block.");
            Intent out = new Intent(CallAPI.CALLAPI_RESPONSE);
            out.putExtra("output", output);
            out.putExtra("conf", conf);
            int id = intent.getIntExtra(INT_1, -1);
            System.out.println("Id: " + id);
            out.putExtra(INT_1, id);
            System.out.println("About to send respose.");
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