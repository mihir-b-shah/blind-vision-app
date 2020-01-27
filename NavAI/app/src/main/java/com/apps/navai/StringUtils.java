package com.apps.navai;

import android.content.Context;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.Arrays;
import java.util.Locale;

public class StringUtils {

    private static final int MAX_SUGGESTIONS = 4;

    public static String[] correct(Context context, String input) {
        SpellCheck spellCheck = new SpellCheck(context, input.split("\\s"));
        while(!spellCheck.outputReady) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return spellCheck.output;
    }

    private static int editDistance(String word1, String word2) {
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

        return dp[0][word1.length()]-Math.abs(word1.length()-word2.length());
    }

    private static class SpellCheck implements SpellCheckerSession.SpellCheckerSessionListener {

        private SpellCheckerSession scs;
        private String[] output;
        private String[] input;
        private boolean outputReady;

        public SpellCheck(Context context, String[] input) {
            this.input = input;
            TextServicesManager tsm =
                    (TextServicesManager) context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            scs = tsm.newSpellCheckerSession(null, Locale.ENGLISH,
                    this, true);

            TextInfo[] ti = Arrays.stream(input).map(TextInfo::new).toArray(TextInfo[]::new);
            scs.getSentenceSuggestions(ti,MAX_SUGGESTIONS);
        }

        private void free() {
            scs.cancel();
            scs.close();
        }

        // Deprecated anyway who cares?
        @Override
        public void onGetSuggestions(SuggestionsInfo[] suggestionsInfos) {}

        @Override
        public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfos) {
            output = new String[sentenceSuggestionsInfos.length];
            SentenceSuggestionsInfo obj = sentenceSuggestionsInfos[0];
            String curr;
            for(int i = 0; i<output.length; ++i) {
                SuggestionsInfo info = obj.getSuggestionsInfoAt(i);
                curr = input[i];

                String opt = null;
                int minDist = 1_000_000_000;
                int currDist;

                for(int j = 0; j<info.getSuggestionsCount(); ++j) {
                    if(minDist > (currDist = editDistance(curr, info.getSuggestionAt(j)))) {
                        minDist = currDist;
                        opt = info.getSuggestionAt(j);
                    }
                }

                output[i] = opt;
            }
            free();
            outputReady = true;
        }
    }
}