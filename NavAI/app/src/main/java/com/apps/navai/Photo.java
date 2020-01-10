package com.apps.navai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;

import static com.apps.navai.MainActivity.FLOAT_1;
import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.SERVICE_RESPONSE;

public class Photo extends AppCompatActivity {

    private String mCurrentPhotoPath;
    private boolean first;
    private float a0;

    /* Need full audio functionality */
    @Override
    protected void onCreate(Bundle st) {
        super.onCreate(st);
        a0 = getIntent().getFloatExtra(FLOAT_1, -1);
        setContentView(R.layout.activity_photo);
        mCurrentPhotoPath = st != null ? st.getString("photo-path") : null;
        first = st == null || st.getBoolean("first");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(SERVICE_RESPONSE));
        if(first) {
            dispatchTakePictureIntent();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putBoolean("first", first);
        b.putString("photo-path", mCurrentPhotoPath);
    }

    // this will cause memory leaks...
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(SERVICE_RESPONSE)) {
                int code = intent.getIntExtra(INT_1, -1);
                if (code == 1) {
                    Calibrate.DirVector vect = (Calibrate.DirVector)
                            intent.getSerializableExtra("vector");
                    System.out.println("Got the dir vector!");
                    Intent next = new Intent();
                    next.putExtra("photo-path", mCurrentPhotoPath);
                    next.putExtra("vector", vect);
                    Photo.this.setResult(Activity.RESULT_OK, next);
                    Photo.this.finish();
                }
            }
        }
    };

    private void dispatchTakePictureIntent() {
        first = false;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra("android.intent.extra.quickCapture",true);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {}

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.apps.navai.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 0);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_test";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent next;
        if(resultCode == Activity.RESULT_OK) {
            if (requestCode == 0) {
                next = new Intent(getApplicationContext(), Calibrate.class);
                next.putExtra(INT_1, 1);
                next.putExtra(FLOAT_1, a0);
                startService(next);
            } else {
                System.err.println("Bad code.");
            }
        }
    }
}
