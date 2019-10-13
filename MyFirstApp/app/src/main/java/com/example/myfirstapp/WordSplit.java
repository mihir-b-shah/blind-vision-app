package com.example.myfirstapp;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class WordSplit {

    private static final HashSet_String dict;
    private static int maxsize;
    private static int mask_2 = 0xffff;

    static {
        dict = new HashSet_String(20_000);
        maxsize = 0;
        new FileIO().execute("dict.txt");
    }

    public static ArrayList<String> split(String word) {
        word = word.toLowerCase();
        String[] words_gen = word.split(" ");
        boolean[] possible = new boolean[word.length()+1];
        possible[word.length()] = true;
        ArrayList<String> words = new ArrayList<>();

        for(int i = word.length()-1; i>=0; --i) {
            for(int j = 0; j<maxsize; ++j) {
                boolean or = true;
                if(i+j < word.length() && dict.contains(word, i, i+j)) {
                    or |= possible[i+j];
                }
                possible[i] = or;
            }
        }
    }

    private static class FileIO extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {

            BufferedReader br = null;
            maxsize = 0;

            try {
                br = new BufferedReader(new FileReader(params[0]));

                String line;
                while((line = br.readLine()) != null) {
                    maxsize = Math.max(maxsize, line.length());
                    dict.add(line);
                }

                br.close();

            } catch (IOException e1) {
                System.err.println(e1.getMessage());
            }
            return null;
        }
    }
}
