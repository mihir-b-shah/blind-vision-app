package com.apps.navai;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicConvolve3x3;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
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
import static com.apps.navai.MainActivity.SERVICE_RESPONSE;
import static com.apps.navai.MainActivity.STRING_1;
import static com.apps.navai.MainActivity.STRING_2;
import static com.apps.navai.MainActivity.STRING_3;
import static com.apps.navai.MainActivity.STRING_ARRAY_1;

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

    private FirebaseVisionObjectDetector objectDetector;
    private FirebaseVisionTextRecognizer detector;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        @SuppressWarnings("deprecation")
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null
                    && intent.getAction().equals(SERVICE_RESPONSE)) {
                if(intent.getIntExtra(INT_1, -1) == 0) {
                    ArrayList<String> output = intent.getStringArrayListExtra("output");
                    SpellCheck.FloatVector conf = (SpellCheck.FloatVector)
                            intent.getSerializableExtra("conf");
                    session.setOutput(callNum, output, conf);

                    if(writefile != null) {
                        dump(createFile(writefile));
                    } else {
                        Intent out = new Intent(SERVICE_RESPONSE);
                        out.putExtra(INT_1, id);
                        out.putExtra("session", session);
                        LocalBroadcastManager.getInstance(getApplicationContext())
                                .sendBroadcast(out);
                        CallAPI.this.stopSelf();
                    }
                }
            }
        }
    };

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

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(SERVICE_RESPONSE));
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

    // kind of inefficient but whatever
    public Bitmap filter(Bitmap image) {
        Bitmap newImage = Bitmap.createBitmap(image.getWidth(),
                image.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript rs = RenderScript.create(this);
        Allocation input = Allocation.createFromBitmap(rs, image);
        Allocation output = Allocation.createFromBitmap(rs, newImage);

        ScriptIntrinsicConvolve3x3 convolution = ScriptIntrinsicConvolve3x3.create(
                rs, Element.U8_4(rs));
        convolution.setInput(input);

        final float[] kernel = {-1f, -1f, -1f, -1f, 9f, -1f, -1f, -1f, -1f};
        convolution.setCoefficients(kernel);
        convolution.forEach(output);
        output.copyTo(newImage);

        return newImage;
    }

    private void objectRecognize() {
        Bitmap bitmap = BitmapFactory.decodeFile(imagepath);
        bitmap = filter(bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, CustomCamera.CAMERA_WIDTH,
                CustomCamera.CAMERA_HEIGHT, true);
        photoMap.add(bitmap);

        FirebaseVisionObjectDetectorOptions options =
                new FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(
                            FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .build();

        objectDetector =
                FirebaseVision.getInstance().getOnDeviceObjectDetector(options);
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        objectDetector.processImage(image)
            .addOnSuccessListener(
                    detectedObjects -> {
                        recObjects = detectedObjects;
                        System.out.println("SIZE: " + detectedObjects.size());
                        textRecognize(image);
                    })
            .addOnFailureListener(
                    e -> System.err.printf("Error encountered in object send."));
    }

    private void textRecognize(FirebaseVisionImage image) {

        FirebaseVisionCloudTextRecognizerOptions options =
                new FirebaseVisionCloudTextRecognizerOptions.Builder()
                        .setModelType(FirebaseVisionCloudTextRecognizerOptions.SPARSE_MODEL)
                        .build();
        detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer(options);
        Task<FirebaseVisionText> result =
            detector.processImage(image)
                .addOnSuccessListener(firebaseVisionText -> {
                recText = firebaseVisionText;
                System.out.println("TEXT: " + recText.getText());
                convert();
                session.genDescriptions(callNum);

                Intent spell = new Intent(getApplicationContext(), SpellCheck.class);
                String[] input = session.getDescrArray(callNum);
                spell.putExtra(INT_1, 0);
                spell.putExtra(STRING_ARRAY_1, input);
                startService(spell);
            }).addOnFailureListener(
                        e -> System.err.println("Error encountered in text send."));
    }

    @Override
    public void onDestroy() {
        try {
            if(objectDetector != null) objectDetector.close();
            if(detector != null) detector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

            Intent out = new Intent(SERVICE_RESPONSE);
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
