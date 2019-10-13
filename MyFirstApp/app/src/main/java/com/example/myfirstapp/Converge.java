package com.example.myfirstapp;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.PriorityQueue;

public class Converge {

    private static Annotation curr;
    private static final PriorityQueue<Annotation> top;

    static {
        top = new PriorityQueue<>();
    }

    public static Annotation converge(Annotation[] annot, String keyword, String descr) {
        
        for(Annotation ant: annot) {
            curr = ant;
            new TwinwordCall().execute(ant.d, keyword);
        }
        // pick n=3 to narrow by
        for(int i = 0; i<3; ++i) {
            Annotation a = top.poll();
        }



        return null;
    }

    private static class TwinwordCall extends AsyncTask<String, Void, Float> {

        @Override
        protected Float doInBackground(String... strings) {
            URL url;
            HttpURLConnection con;
            try {
                url = new URL("https://twinword-text-similarity-v1.p.rapidapi.com/similarity/");
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("x-rapidapi-host",
                        "twinword-text-similarity-v1.p.rapidapi.com");
                con.setRequestProperty("x-rapidapi-key",
                        "mEzuaof4WMmshVAhJdkB5EN3I5cTp1JKC8kjsnEavaa8BCjYmS");
                OutputStream os = con.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                osw.write(String.format("text1:%s,%ntext2:%s", strings[0], strings[1]));
                osw.flush();
                osw.close();
                con.connect();
                if(con.getResponseCode() == 200) {
                    InputStream instr = con.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(instr));
                    String json = br.readLine();
                    Float sim = Float.parseFloat(
                            json.substring(json.indexOf(':'),json.indexOf(",\"value\"")-1));
                    return sim;
                } else {
                    System.err.println("Request did not go through correctly.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Float f) {
            curr.s = f;
            top.offer(curr);
        }
    }
}
