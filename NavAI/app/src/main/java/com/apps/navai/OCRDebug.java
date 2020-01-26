package com.apps.navai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

public class OCRDebug extends AppCompatActivity {

    private boolean run = true;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (run) {
            run = false;
        } else {
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_gapi);
        ImageView imgView = findViewById(R.id.iview);

        String parent = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        parent += "/CAPTURE680603899.jpg";
        Bitmap bitmap = BitmapFactory.decodeFile(parent);
        bitmap = filter(bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, 1920, 1080, true);
        System.out.println(bitmap.getWidth() + " " + bitmap.getHeight());
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        imgView.setImageBitmap(image.getBitmap());
        System.out.println(image.getBitmap().getByteCount());

        /*
        FirebaseVisionObjectDetectorOptions options =
                new FirebaseVisionObjectDetectorOptions.Builder()
                        .setDetectorMode(
                                FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .build(); */

        FirebaseVisionCloudTextRecognizerOptions options =
                new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setModelType(FirebaseVisionCloudTextRecognizerOptions.SPARSE_MODEL)
                .build();
        FirebaseVisionTextRecognizer detector =
                FirebaseVision.getInstance().getCloudTextRecognizer(options);
        detector.processImage(image).addOnSuccessListener(res->{
            System.out.println(res.getText());
        }).addOnFailureListener(res->{
            System.err.println("NOOOOO!");
        });
    }
}
