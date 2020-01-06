package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.STRING_1;
import static com.apps.navai.MainActivity.STRING_2;
import static com.apps.navai.MainActivity.STRING_3;

public class CallAPI extends IntentService {
    private List<FirebaseVisionObject> recObjects;
    private FirebaseVisionText recText;
    private Session session;
    private int id;
    private String readfile;
    private String imagepath;
    private String writefile;
    private static Bitmap photoMap;

    private int imageWidth;
    private int imageHeight;

    public CallAPI() {
        super("CallAPI");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        id = intent.getIntExtra(INT_1, -1);
        imagepath = intent.getStringExtra(STRING_1);
        writefile = intent.getStringExtra(STRING_2);
        readfile = intent.getStringExtra(STRING_3);
        if(readfile == null) {
            objectRecognize();
        } else {
            read();
        }
    }

    private void convert() {
        List<Annotation> newAnnotations = new ArrayList<>();
        if(recObjects != null) {
            for(FirebaseVisionObject object: recObjects) {
                newAnnotations.add(new Annotation(object));
            }
        }
        if(recText != null) {
            final List<FirebaseVisionText.TextBlock> list = recText.getTextBlocks();
            for(FirebaseVisionText.TextBlock textBlock: list) {
                newAnnotations.add(new Annotation(textBlock));
            }
        }

        Annotation[] out = new Annotation[newAnnotations.size()];
        out = newAnnotations.toArray(out);
        session = new Session(out, imagepath, imageWidth, imageHeight);
    }

    private String createFile(String prefix) {
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

    public static Bitmap getBitmap() {
        return photoMap;
    }

    private void objectRecognize() {
        /* params[0] = absolute path of the photo */
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        bitmapOptions.inMutable = false;
        Bitmap bitmap = BitmapFactory.decodeFile(imagepath, bitmapOptions);
        bitmapOptions.inJustDecodeBounds = false;
        int area = bitmapOptions.outWidth*bitmapOptions.outHeight;
        System.out.printf("Width orig: %d, Height orig: %d%n", bitmapOptions.outWidth, bitmapOptions.outHeight);
        if(area>2_500_000) {
            bitmapOptions.inSampleSize = (int) Math.ceil(Math.sqrt(area/2_500_000));
        }
        bitmap = BitmapFactory.decodeFile(imagepath, bitmapOptions);
        photoMap = bitmap;
        imageWidth = bitmap.getWidth();
        imageHeight = bitmap.getHeight();
        System.out.printf("Width: %d, Height: %d%n", imageWidth, imageHeight);

        FirebaseVisionObjectDetectorOptions options =
                new FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(
                            FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .build();

        final FirebaseVisionObjectDetector objectDetector =
                FirebaseVision.getInstance().getOnDeviceObjectDetector(options);
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        objectDetector.processImage(image)
            .addOnSuccessListener(
                new OnSuccessListener<List<FirebaseVisionObject>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionObject> detectedObjects) {
                        recObjects = detectedObjects;
                        textRecognize(image);
                    }
                })
            .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.err.printf("Error encountered in send.");
                    }
                });
    }

    private void textRecognize(FirebaseVisionImage image) {
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer();

        Task<FirebaseVisionText> result =
            detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                    recText = firebaseVisionText;
                    System.out.println(recText.getText());
                    convert();
                    session.genDescriptions();

                    if(writefile != null) {
                        dump(createFile(writefile));
                    } else {
                        Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
                        out.putExtra(INT_1, id);
                        out.putExtra("session", session);
                        LocalBroadcastManager.getInstance(getApplicationContext())
                                .sendBroadcast(out);
                        CallAPI.this.stopSelf();
                    }
                    }
                })
                .addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                        System.err.println("Error encountered in send.");
                        }
                    });
    }

    public void dump(String path) {
        session.setSourceFile(path);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            CharBuffer buf = session.outform();
            bw.write(buf.getChars(), 0, buf.size());
            bw.flush();
            bw.close();

            Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
            out.putExtra(INT_1, id);
            out.putExtra("session", session);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(out);
            System.out.println("Just dumped.");
            CallAPI.this.stopSelf();

        } catch (IOException e) {e.printStackTrace();}
    }

    private String getPath() throws IOException {
        File folder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File[] list = folder.listFiles();

        if (list.length > 100) {
            throw new IOException("Too many cached files in storage");
        }
        long latest = 0;
        String index = null;

        for (int i = 0; i < list.length; ++i) {
            if (list[i].getName().contains(readfile) && list[i].lastModified() > latest) {
                index = list[i].getAbsolutePath();
                latest = list[i].lastModified();
            }
        }

        if (index == null) {
            throw new FileNotFoundException("Readfile not found");
        } else {
            return index;
        }
    }

    public void read() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(getPath()));

            imagepath = br.readLine();
            imageWidth = Integer.parseInt(br.readLine());
            imageHeight = Integer.parseInt(br.readLine());
            int next = Integer.parseInt(br.readLine());
            Annotation[] out = new Annotation[next];

            for (int i = 0; i < next; ++i) {
                String t = br.readLine();
                t = t.equals("") ? null : t;
                String d = br.readLine();
                d = d.equals("") ? null : d;
                String f = br.readLine();
                float c = f.equals("") ? -1 : Float.parseFloat(f);
                int vCt = Integer.parseInt(br.readLine());
                Rect rect = null;
                if(vCt != 0) {
                    rect = new Rect(Integer.parseInt(br.readLine()),
                            Integer.parseInt(br.readLine()),
                            Integer.parseInt(br.readLine()),
                            Integer.parseInt(br.readLine()));
                }
                out[i] = new Annotation(t.charAt(0), d, c, rect);
            }
            br.close();
            session = new Session(out, imagepath, readfile, imageWidth, imageHeight);

            Intent intent = new Intent();
            intent.putExtra(INT_1, 3);
            intent.putExtra(STRING_1, session);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(intent);
            CallAPI.this.stopSelf();

        } catch (IOException e) {
            System.err.println("IO EXCEPTION NOOOO!");
        }
    }
}
