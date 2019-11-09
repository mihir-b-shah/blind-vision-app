package com.example.myfirstapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.support.v7.app.AppCompatActivity;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.*;
import com.google.api.services.vision.v1.model.*;

import java.io.*;
import java.util.*;

/*
Need to change confidence values.
 */

public class CallGAPI extends AppCompatActivity {
    private List<AnnotateImageResponse> annotations;
    private Session session;
    private String cachename;
    private String imagepath;
    private String filepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_api);
        imagepath = getIntent().getStringExtra("photo-path");
        filepath = getIntent().getStringExtra("file-path");
        boolean cont = getIntent().getBooleanExtra("gen", true);
        cachename = getIntent().getStringExtra("cache");
        if(cont) {
            new Request().execute(imagepath);
        } else {
            new Read().execute(cachename);
        }
    }

    private void convert() {
        List<Annotation> newAnnotations = new ArrayList<>();

        if(annotations != null)
            for(AnnotateImageResponse air: annotations) {
                TextAnnotation ta = air.getFullTextAnnotation();
                List<EntityAnnotation> eas = air.getLogoAnnotations();
                List<LocalizedObjectAnnotation> loas = air.getLocalizedObjectAnnotations();

                if(eas != null)
                    for(EntityAnnotation ea: eas)
                        newAnnotations.add(new Annotation(ea));
                if(loas != null)
                    for(LocalizedObjectAnnotation loa: loas)
                        newAnnotations.add(new Annotation(loa));
                if(ta != null) {
                    List<Page> pages = ta.getPages();
                    for(Page pg: pages) {
                        List<Block> blocks = pg.getBlocks();
                        for(Block b: blocks) {
                            List<Paragraph> paragraphs = b.getParagraphs();
                            for(Paragraph p: paragraphs)
                                newAnnotations.add(new Annotation(p));
                        }
                    }
                }
            }

        Annotation[] out = new Annotation[newAnnotations.size()];
        out = newAnnotations.toArray(out);
        session = new Session(out, imagepath);
    }

    private String createImageFile(String prefix) {
        try {
            String imageFileName = String.format("REST_RESPONSE_%s", prefix);
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File text = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".txt",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
            return text.getAbsolutePath();
        } catch(IOException e) {}
        return null;
    }

    private class Request extends AsyncTask<String, Integer, List<AnnotateImageResponse>> {
        @Override
        protected List<AnnotateImageResponse> doInBackground(String... params) {
            Vision.Builder vb = new Vision.Builder(new NetHttpTransport(), new AndroidJsonFactory(), null);
            vb.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyDtDkwucsAu5udmyCfBbtn3vnTRj5gJ8GY"));
            vb.setApplicationName("Blind Vision");

            Vision vision = vb.build();
            Feature df = new Feature();
            df.setType("TEXT_DETECTION");
            Feature df2 = new Feature();
            df2.setType("LOGO_DETECTION");
            Feature df3 = new Feature();
            df3.setType("OBJECT_LOCALIZATION");

            /*
            params[0] = absolute path of the photo
            */

            try {
                Bitmap b = BitmapFactory.decodeFile(params[0]);
                b = Bitmap.createScaledBitmap((Bitmap) b, 1920, 1080, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] data = stream.toByteArray();
                stream.close();

                Image img = new Image();
                img.encodeContent(data);
                AnnotateImageRequest request = new AnnotateImageRequest();

                request.setImage(img);
                request.setFeatures(Arrays.asList(df, df2, df3));
                BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                Vision.Images vi = vision.images();
                Vision.Images.Annotate via = vi.annotate(batchRequest);
                BatchAnnotateImagesResponse batchResponse = via.execute();
                annotations = batchResponse.getResponses();

                System.out.println("INTERNAL ANNOTATIONS: " + annotations);

            } catch (IOException e) {
                e.printStackTrace();
            }

            return annotations;
        }

        @Override
        protected void onPostExecute(List<AnnotateImageResponse> result) {
            System.out.println("Got to onPostExecute");
            CallGAPI.this.annotations = result;
            CallGAPI.this.convert();
            System.out.println("Right before serialization!");
            if(cachename != null) {
                new Dump().execute(createImageFile(cachename));
            } else {
                Intent out = new Intent();
                out.putExtra("list-annotation", session);
                CallGAPI.this.setResult(Activity.RESULT_OK, out);
                CallGAPI.this.finish();
            }
        }
    }

    private class Dump extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... strings) {
            String path = strings[0];
            session.set_srcfile(path);
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(path));
                CharBuffer buf = session.outform();
                out.write(buf.get_chars(), 0, buf.size());
                out.flush();
                out.close();
                return 0;
            } catch (IOException e) {return 1;}
        }

        @Override
        protected void onPostExecute(Integer res) {
            if(res == 0) {
                Intent out = new Intent();
                out.putExtra("list-annotation", session);
                CallGAPI.this.setResult(Activity.RESULT_OK, out);
                CallGAPI.this.finish();
            }

        }
    }

    // Provides functionality to read and construct te
    private class Read extends AsyncTask<String, Void, Session> {

        @Override
        protected Session doInBackground(String... strings) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(filepath));
                imagepath = br.readLine();
                int next = Integer.parseInt(br.readLine());
                Annotation[] out = new Annotation[next];

                for(int i = 0; i<next; i++) {
                    String t = br.readLine();
                    t = t.equals("") ? null : t;
                    String d = br.readLine();
                    d = d.equals("") ? null : d;
                    String f = br.readLine();
                    float c = f.equals("") ? -1 : Float.parseFloat(f);
                    int vCt = Integer.parseInt(br.readLine());
                    List<Vertex> vertices = new ArrayList<>();
                    for(int j = 0; j<vCt; j++) {
                        int X = Integer.parseInt(br.readLine());
                        int Y = Integer.parseInt(br.readLine());
                        Vertex v = new Vertex();
                        v.setX(X); v.setY(Y);
                        vertices.add(v);
                    }
                    BoundingPoly bp = new BoundingPoly();
                    bp.setVertices(vertices);
                    out[i] = new Annotation(t,d,c,bp);
                }
                return new Session(out, imagepath, filepath);

            } catch (IOException e) {}
            return null;
        }

        @Override
        protected void onPostExecute(Session result) {
            CallGAPI.this.session = result;
            if(cachename != null) {
                new Dump().execute(createImageFile(cachename));
            } else {
                Intent out = new Intent();
                out.putExtra("list-annotation", session);
                CallGAPI.this.setResult(Activity.RESULT_OK, out);
                CallGAPI.this.finish();
            }
        }
    }
}