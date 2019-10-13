package com.example.myfirstapp;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class NLP {//implements AsyncResponse<String>{

    public static final String filepath = "/storage/emulated/0/Android/data/com.example.myfirstapp/files/wordfile.txt";
    public static HashSet<String> wordsWithEdits;
    public static int maxSize = 0;
    public static boolean interrupt;
    public static boolean[] spaces;
    public static ArrayList<String> words;
    public static int numEdits = 0;

    public NLP() {

        words = new ArrayList<String>(){
            @Override
            public String toString() {

                StringBuilder s = new StringBuilder();
                s.append('[');

                for(int i = 0; i<size(); i++) {
                    if(get(i) != null) {
                        s.append(get(i));
                        s.append(", ");
                    }
                }

                s.delete(s.length()-2, s.length());
                s.append(']');

                return s.toString();
            }
        };

        FileIO fi = new FileIO();
        //fi.deleg = this;
        fi.execute();
    }

    public static ArrayList<String> fix(String word) {

        int frIndex = 0;
        int index = word.indexOf(' ');

        while(index != -1) {
            ++numEdits;
            System.out.println(word.substring(frIndex, index));
            words.addAll(findSpaces(word.substring(frIndex, index)));
            frIndex = index+1;
            index = word.indexOf(' ', index+1);
            interrupt = false;
        }

        words.addAll(findSpaces(word.substring(frIndex)));

        for(int i = 0; i<words.size() - 1; i++) {

            String concat = words.get(i).concat(words.get(i+1));

            if(wordsWithEdits.contains(concat)) {
                words.set(i, null);
                words.set(i+1, concat);
            }
        }

        words.add(Double.toString(1 - (double) numEdits/word.length()));
        return words;

    }

    public static ArrayList<String> findSpaces(String word) {

        ArrayList<String> wordsLoc = new ArrayList<>();

        wordsLoc.clear();
        boolean[] exists = new boolean[word.length() + 1];
        spaces = exists.clone();
        recurHelper(-1, word, spaces, exists);

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i<word.length(); i++) {
            sb.append(word.charAt(i));
            if(spaces[i]) {
                wordsLoc.add(sb.toString());
                sb = new StringBuilder();
            }
        }

        wordsLoc.add(sb.toString());
        return wordsLoc;
    }

    public static int recurHelper(int pos, String word, boolean[] arr, boolean[] exists) {

        boolean found = false;

        for (int i = 1; i <= maxSize; i++) {
            if (!interrupt && !exists[pos + 1] && pos + i < word.length() && (isProperNoun(word.substring(pos + 1, pos + i + 1)) || wordsWithEdits.contains(word.substring(pos + 1, pos + i + 1).toLowerCase()))) {

                System.out.println(word.substring(pos+1, pos+i+1));

                if(pos != -1)
                    arr[pos] = true;

                found = true;
                int result = recurHelper(pos + i, word, arr.clone(), exists);

                if (result == 0) {
                    interrupt = true;
                }
            }
        }

        if (pos + 1 == word.length()) {
            spaces = arr;
            return 0;
        }

        if (!found) {
            exists[pos + 1] = true;
        }

        if (!interrupt) {
            return -1;
        } else {
            return 0;
        }
    }

    public void finish(Collection<String> words) {
        wordsWithEdits = (HashSet<String>) words;
    }

    private class FileIO extends AsyncTask<Object, Integer, String> {
        // Do the long-running work in here

        //private AsyncResponse deleg = null;

        @Override
        protected String doInBackground(Object... params) {

            BufferedReader br = null;
            wordsWithEdits = new HashSet<>();
            maxSize = -1;

            try {
                br = new BufferedReader(new FileReader(filepath));

                String line;
                while((line = br.readLine()) != null) {
                    maxSize = Math.max(maxSize, line.length());
                    wordsWithEdits.add(line);
                }

                br.close();

            } catch (IOException e1) {
                System.err.println(e1.getMessage());
            }

            return "";
        }
        /*
        @Override
        protected void onPostExecute(String result) {
            deleg.finish(wordsWithEdits);
        }
        */
    }

    public static boolean isProperNoun(String s) {
        return !wordsWithEdits.contains(s.toLowerCase()) && s.charAt(0) < 97;
    }
}
