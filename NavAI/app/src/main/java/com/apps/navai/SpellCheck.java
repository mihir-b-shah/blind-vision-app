package com.apps.navai;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntFunction;

import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.STRING_1;

public class SpellCheck extends IntentService implements SpellCheckerSession.SpellCheckerSessionListener {

    private final int MAX_SUGGESTIONS = 7;

    private final int DELETE_MASK = 20;
    private final int INSERT_MASK = 10;
    private final int RIGHT_MASK = 0x3ff;

    private final int INSERT_ONE = 0x400;
    private final int DELETE_ONE = 0x100000;

    private SpellCheckerSession scs;
    private CharSequence[] output;
    private String[] input;
    private int id;

    public SpellCheck() {
        super("SpellCheck");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String data = intent.getStringExtra(STRING_1);
            id = intent.getIntExtra(INT_1, -1);
            String[] input = data.split("\t");
            TextServicesManager tsm = (TextServicesManager)
                    getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            scs = tsm.newSpellCheckerSession(null, Locale.ENGLISH,
                    this, true);

            TextInfo[] ti = Arrays.stream(input).map(TextInfo::new).toArray(TextInfo[]::new);
            scs.getSentenceSuggestions(ti, MAX_SUGGESTIONS);
        }
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {}

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        output = new String[results.length];
        SentenceSuggestionsInfo obj = results[0];
        String curr;
        for(int i = 0; i<output.length; ++i) {
            SuggestionsInfo info = obj.getSuggestionsInfoAt(i);
            curr = input[i];

            String opt = null;
            double minDist = 1_000_000_000;
            double currDist;

            for(int j = 0; j<info.getSuggestionsCount(); ++j) {
                if(minDist > (currDist = weightedEditDistance(curr, info.getSuggestionAt(j)))) {
                    minDist = currDist;
                    opt = info.getSuggestionAt(j);
                }
            }

            output[i] = opt;
        }

        Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
        out.putExtra(INT_1, id);
        out.putExtra("output", output);
        System.out.println(Arrays.toString(output));
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(out);
        free();
        stopSelf();
    }

    private void free() {
        scs.cancel();
        scs.close();
    }

    private double collapse(int one) {
        return (((one >>> DELETE_MASK) + (one >>> INSERT_MASK)) & RIGHT_MASK)*1.5
                +(one & RIGHT_MASK);
    }

    private void print2DArray(int[][] array, IntFunction func) {
        for(int[] a: array) {
            for(int b: a) {
                System.out.printf("%3d", func.apply(b));
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Utility min function.
     *
     * @param one a replace arg
     * @param two an insert arg
     * @param three a delete arg
     * @return formatted minimum.
     */
    private int packMin(int one, int two, int three, double dist) {
        double v1 = collapse(one); double v2 = collapse(two); double v3 = collapse(three);
        int bias = (int) (4*dist+1); // constrain on range 1,3.
        if ((bias < 1 || bias > 3)) throw new AssertionError();

        if(v1 > v2) {
            if(v1 > v3) {
                return one+1;
            } else {
                return two+INSERT_ONE*bias;
            }
        } else {
            if(v1 > v3) {
                return three+DELETE_ONE*bias;
            } else {
                return one+1;
            }
        }
    }

    private double weightedEditDistance(String word1, String word2) {
        int[][] dp = new int[2][1+word1.length()];
        for(int i = 0; i<1+word1.length(); ++i)
            dp[0][i] = i << DELETE_MASK;

        for(int i = 1; i<1+word2.length(); ++i) {
            //print2DArray(dp, SpellCheck::collapse);
            for(int j = 1; j<1+word1.length(); ++j) {
                if(word2.charAt(i-1) == word1.charAt(j-1)) {
                    dp[1][j] = dp[0][j-1];
                } else {
                    dp[1][j] = packMin(dp[0][j-1], dp[0][j], dp[1][j-1],
                            Math.min(j-1, word1.length()-j)/word1.length());
                }
            }

            dp[0] = dp[1];
            dp[1] = new int[1+word1.length()];
            dp[1][0] = i << INSERT_MASK;
        }

        int val = dp[0][word1.length()];
        return (val & RIGHT_MASK)+(((val >>> INSERT_MASK) & RIGHT_MASK)
                +((val >>> DELETE_MASK) & RIGHT_MASK))*1.5;
    }

    private int editDistance(String word1, String word2) {
        // space saving optimization
        if(word1.length() > word2.length()) {
            String temp = word2;
            word2 = word1;
            word1 = temp;
        }
        int[][] dp = new int[2][1+word1.length()];
        for(int i = 0; i<1+word1.length(); ++i)
            dp[0][i] = i;

        for(int i = 1; i<1+word2.length(); ++i) {
            for(int j = 1; j<1+word1.length(); ++j) {
                if(word2.charAt(i-1) == word1.charAt(j-1)) {
                    dp[1][j] = dp[0][j-1];
                } else {
                    dp[1][j] = 1 + Math.min(dp[0][j-1], Math.min(dp[0][j], dp[1][j-1]));
                }
            }

            dp[0] = dp[1];
            dp[1] = new int[1+word1.length()];
            dp[1][0] = i;
        }

        return dp[0][word1.length()];
    }
}
