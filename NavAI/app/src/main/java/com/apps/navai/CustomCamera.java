
package com.apps.navai;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import static com.apps.navai.MainActivity.FLOAT_1;
import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.SERVICE_RESPONSE;

public class CustomCamera extends AppCompatActivity {

    private static final int REQUEST_CAMERA_RESULT = 1;
    private ImageReader imageReader;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private SurfaceHolder surfaceHolder;
    private String mCurrentPhotoPath;
    private float a0;
    private int state;
    private Semaphore openCloseLock;

    private static final int WAIT_LOCK = 1;

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
                    CustomCamera.this.setResult(Activity.RESULT_OK, next);
                    CustomCamera.this.finish();
                }
            }
        }
    };

    private Handler handler;
    private File file;
    private HandlerThread backThread;

    private CameraCaptureSession.StateCallback stateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    // make call to camera
                    try {
                        CaptureRequest.Builder builder =
                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                CameraMetadata.CONTROL_AF_TRIGGER_START);
                        builder.addTarget(surfaceHolder.getSurface());
                        builder.addTarget(imageReader.getSurface());
                        builder.set(CaptureRequest.NOISE_REDUCTION_MODE,
                                CameraMetadata.NOISE_REDUCTION_MODE_FAST);
                        CaptureRequest request = builder.build();
                        session.setRepeatingRequest(builder.build(), null, null);
                        session.capture(request, captureCallback, handler);
                    } catch (CameraAccessException e) {
                        System.err.println(e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("CAMERA", "Configuration of camera failed.");
                }
            };

    private CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    //session.close();
                    //cameraDevice.close();
                }
            };

    private CameraDevice.StateCallback deviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            SurfaceHolder holder = ((SurfaceView) findViewById(R.id.sview)).getHolder();
            holder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    surfaceHolder = holder;
                    Surface surface = holder.getSurface();

                    try {
                        cameraDevice.createCaptureSession(Arrays.asList(surface,
                                imageReader.getSurface()),
                                stateCallback, handler);
                    } catch (CameraAccessException e) {
                        System.err.println(e.getMessage());
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    //holder.getSurface().release();
                }
            });
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            Log.e("CAMERA", String.format("Error %d in camera %s", error, camera.getId()));
            finish();
        }
    };

    @SuppressWarnings("ConstantConditions")
    private String getRearCameraId() {
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (final String id : ids) {
                if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.
                        LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            Log.e("CAMERA", e.getMessage());
        }
        return null;
    }

    // from Google's code
    private class ImageSaver implements Runnable {
        private final Image image;
        private final File file;

        ImageSaver(Image img, File f) {
            image = img; file = f;
        }

        @Override
        public void run() {
            java.nio.ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
                System.out.println("File bytes written.");
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                image.close();
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
                System.out.println("Got to intent!");
                mCurrentPhotoPath = file.getAbsolutePath();
                Intent next = new Intent(getApplicationContext(), Calibrate.class);
                next.putExtra(INT_1, 1);
                next.putExtra(FLOAT_1, a0);
                startService(next);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "CAPTURE";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);

        try {
            file = createImageFile();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        openCloseLock = new Semaphore(WAIT_LOCK);
        a0 = getIntent().getFloatExtra("initDir",0);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        backThread = new HandlerThread("CameraBackground");
        backThread.start();
        handler = new Handler(backThread.getLooper());

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(SERVICE_RESPONSE));

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    android.Manifest.permission.CAMERA},REQUEST_CAMERA_RESULT);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if((requestCode == REQUEST_CAMERA_RESULT) &&
                permissions[0].equals(Manifest.permission.CAMERA) &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            openCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backThread.quitSafely();
        //imageReader.close();
        handler = null;
        try {
            backThread.join();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    private void openCamera() {
        try {
            imageReader = ImageReader.newInstance(
                    1920, 1080, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image img = reader.acquireLatestImage();
                if(img == null || img.getHeight() == 0 || img.getWidth() == 0) return;
                (new ImageSaver(img, file)).run();
            }, handler);
            cameraManager.openCamera(getRearCameraId(), deviceCallback, handler);
        } catch (CameraAccessException|SecurityException e) {
        }
    }
}
