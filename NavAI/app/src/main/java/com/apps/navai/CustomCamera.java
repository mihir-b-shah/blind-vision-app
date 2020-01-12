package com.apps.navai;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;

public class CustomCamera extends AppCompatActivity {

    private static final int REQUEST_CAMERA_RESULT = 1;
    private ImageReader imageReader;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private SurfaceHolder surfaceHolder;

    private Handler handler;
    private HandlerThread backThread;

    private CameraCaptureSession.StateCallback stateCallback =
            new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            // make call to camera
            try {
                CaptureRequest.Builder builder =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                builder.addTarget(surfaceHolder.getSurface());
                builder.addTarget(imageReader.getSurface());
                CaptureRequest request = builder.build();
                session.capture(request, captureCallback, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
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
            session.close();
            cameraDevice.close();
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
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder,
                                           int format, int width, int height) {}

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    // Not sure yet
                    holder.getSurface().release();
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
    private boolean gotImage;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        backThread = new HandlerThread("CameraBackground");
        backThread.start();
        handler = new Handler(backThread.getLooper());

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
        //handler = null;
        try {
            backThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            imageReader = ImageReader.newInstance(
                    1920, 1080, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                if(!gotImage) {
                    Image img = reader.acquireLatestImage();
                    java.nio.ByteBuffer buffer = reader.acquireLatestImage().
                            getPlanes()[0].getBuffer();
                    Intent out = new Intent();
                    out.putExtra("bytes", buffer.array());
                    setResult(Activity.RESULT_OK, out);
                    gotImage = true;
                    img.close();
                    finish();
                }
            }, handler);
            cameraManager.openCamera(getRearCameraId(), deviceCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {}
    }
}
