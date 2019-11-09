package com.example.myfirstapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.Locale;

public class SpellCheck extends AppCompatActivity implements SpellCheckerSession.SpellCheckerSessionListener {

    private SpellCheckerSession scs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spell_check);

        TextServicesManager tsm =
                (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);
        SpellCheckerSession scs = tsm.newSpellCheckerSession(null, Locale.ENGLISH,
                this, true);
        ArrayList<String> s = getIntent().getStringArrayListExtra("input_data");
        TextInfo[] ti = new TextInfo[s.size()];
        for(int i = 0; i<ti.length; ++i) {
            ti[i] = new TextInfo(s.get(i));
        }
        scs.getSentenceSuggestions(ti,1);
    }

    public void free() {
        scs.cancel();
        scs.close();
    }

    // Deprecated anyway who cares?
    @Override
    public void onGetSuggestions(SuggestionsInfo[] suggestionsInfos) {
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfos) {
        Intent out = new Intent();
        String[] correction = new String[sentenceSuggestionsInfos.length];
        SentenceSuggestionsInfo obj = sentenceSuggestionsInfos[0];
        for(int i = 0; i<correction.length; ++i) {
            correction[i] = obj.getSuggestionsInfoAt(i).getSuggestionAt(0);
        }
        out.putExtra("corrections", correction);
        setResult(Activity.RESULT_OK, out);
        free();
        finish();
    }
}
