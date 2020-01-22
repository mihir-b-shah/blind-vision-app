package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import static com.apps.navai.MainActivity.INT_2;
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
    private int callNum;

    private String writefile;
    private static final List<Bitmap> photoMap;

    static {
        photoMap = new ArrayList<>(2);
    }

    public CallAPI() {
        super("CallAPI");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        id = intent.getIntExtra(INT_1, -1);
        callNum = intent.getIntExtra(INT_2, -1);
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
        if(callNum == 1) {
            session = new Session(null, out, null, imagepath);
        } else {
            session = new Session(out, null, imagepath, null);
        }

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

    public static Bitmap getFirstBitmap() {
        return photoMap.get(0);
    }

    public static Bitmap getSecondBitmap() {
        return photoMap.get(1);
    }

    public static Bitmap filter(Bitmap image) {
        Bitmap newImage = Bitmap.createBitmap(image.getWidth(),
                image.getHeight(), Bitmap.Config.ARGB_8888);
        final int[] kernel = {-1,-1,-1,-1,9,-1,-1,-1,-1};
        final int numBands = 3;
        int[] maxValues = new int[numBands];
        int[] masks = new int[numBands];
        int[] sampleSizes = {8,8,8};

        for (int i=0; i < numBands; ++i){
            maxValues[i] = (1 << sampleSizes[i]) - 1;
            masks[i] = ~(maxValues[i]);
        }

        // Cycle over pixels to be calculated
        for (int i = 1; i < image.getHeight()-kernel.length; ++i){
            for (int j = 1; j < image.getWidth()-kernel.length; ++j){
                // Take kernel data in backward direction, convolution
                int kernelIdx = kernel.length - 1;
                int currPixel = image.getPixel(i,j);
                for (int hIdx = 0, rasterHIdx = i - 1; hIdx < kernel.length; ++hIdx, ++rasterHIdx){
                    for (int wIdx = 0, rasterWIdx = j - 1; wIdx < kernel.length; ++wIdx, ++rasterWIdx){
                        int pixel = 0;
                        for (int idx=0; idx < numBands; ++idx){
                            pixel <<= sampleSizes[idx];
                            pixel += kernel[kernelIdx]*(currPixel >> i*sampleSizes[i] & masks[i]);
                        }
                        --kernelIdx;
                        newImage.setPixel(i,j,pixel);
                    }
                }
                // Check for overflow
                currPixel = newImage.getPixel(i,j);
                for (int idx=0; idx < numBands; ++idx){
                    if ((currPixel >> i*sampleSizes[i] & masks[i]) != 0) {
                        currPixel = currPixel < 0 ? 0 : maxValues[idx];
                    }
                }
            }
        }

        return newImage;
    }

    private void objectRecognize() {
        /* params[0] = absolute path of the photo */
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = false;
        bitmapOptions.inMutable = false;
        bitmapOptions.outWidth = CustomCamera.CAMERA_WIDTH;
        bitmapOptions.outHeight = CustomCamera.CAMERA_HEIGHT;
        Bitmap bitmap = BitmapFactory.decodeFile(imagepath, bitmapOptions);
        bitmap = filter(bitmap);
        photoMap.add(bitmap);

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
                    detectedObjects -> {
                        recObjects = detectedObjects;
                        textRecognize(image);
                    })
            .addOnFailureListener(
                    e -> System.err.printf("Error encountered in send."));
    }

    private void textRecognize(FirebaseVisionImage image) {
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer();

        Task<FirebaseVisionText> result =
            detector.processImage(image)
                .addOnSuccessListener(firebaseVisionText -> {
                recText = firebaseVisionText;
                System.out.println(recText.getText());
                convert();

                // PROBLEM SPOT
                session.genDescriptions(0);
                session.genDescriptions(1);

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
                })
                .addOnFailureListener(
                        e -> System.err.println("Error encountered in send."));
    }

    public void dump(String path) {
        if(callNum == 0) {
            session.setSourceFileOne(path);
        } else {
            session.setSourceFileTwo(path);
        }
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
            if (list[i].getName().contains(readfile+(callNum == 0 ?"" : "2"))
                    && list[i].lastModified() > latest) {
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
            Session session;
            if(callNum == 0) {
                session = new Session(out, null, imagepath, null, readfile, null);
            } else {
                session = new Session(null, out, null, imagepath, null, readfile);
            }

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
