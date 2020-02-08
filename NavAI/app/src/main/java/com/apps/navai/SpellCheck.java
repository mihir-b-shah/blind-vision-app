package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.textrazor.AnalysisException;
import com.textrazor.NetworkException;
import com.textrazor.TextRazor;
import com.textrazor.annotations.Response;
import com.textrazor.annotations.Sentence;
import com.textrazor.annotations.Word;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.STRING_1;

public class SpellCheck extends IntentService {

    private static final int DELETE_MASK = 20;
    private static final int INSERT_MASK = 10;
    private static final int RIGHT_MASK = 0x3ff;

    private static final int INSERT_ONE = 0x400;
    private static final int DELETE_ONE = 0x100000;

    public SpellCheck() {
        super("SpellCheck");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String[] data = intent.getStringArrayExtra(STRING_1);
        String[] output = new String[data.length];
        float[] conf = new float[data.length];
        String input = join(data);
        TextRazor textRazor = new TextRazor(getString(R.string.textrazor_key));
        textRazor.setExtractors(Arrays.asList("spelling"));

        try {
            Response result = textRazor.analyze(input).getResponse();
            final List<Sentence> sentences = result.getSentences();
            int wordCtr = 0;

            for (Sentence sentence : sentences) {
                final List<Word> words = sentence.getWords();
                for (Word word : words) {
                    final List<Word.Suggestion> suggestions = word.getSpellingSuggestions();
                    String curr = data[wordCtr];

                    String opt = null;
                    double minDist = 1_000_000_000;
                    double currDist;

                    final int size = suggestions.size();
                    Word.Suggestion sug;
                    for (int i = 0; i < size; ++i) {
                        sug = suggestions.get(i);
                        if (minDist > (currDist = sug.getScore() *
                                (1 - weightedEditDistance(curr, sug.getSuggestion()) / curr.length()))) {
                            minDist = currDist;
                            opt = sug.getSuggestion();
                        }
                    }

                    conf[wordCtr] = (float) minDist;
                    output[wordCtr++] = opt;
                }
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

    private static String join(String[] data) {
        StringBuilder sb = new StringBuilder();
        for (String s : data) {
            sb.append(s);
            sb.append(' '); sb.append(' '); // differentiate
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static double collapse(int one) {
        return (((one >>> DELETE_MASK) + (one >>> INSERT_MASK)) & RIGHT_MASK) * 1.5
                + (one & RIGHT_MASK);
    }

    private static void print2DArray(int[][] array, IntFunction func) {
        for (int[] a : array) {
            for (int b : a) {
                System.out.printf("%3d", func.apply(b));
            }
            System.out.println();
        }
        System.out.println();
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