package com.apps.navai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.concurrent.Executor;

public class CustomCamera extends AppCompatActivity {

    private static final int REQUEST_CAMERA_RESULT = 1;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession.StateCallback stateCallback =
            new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            // make call to camera
            try {
                CaptureRequest.Builder builder =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
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

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            super.onReady(session);
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {

        }
    };

    private CameraDevice.StateCallback deviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            usable = true;
            SurfaceView sView = new SurfaceView(CustomCamera.this);
            Arrays.asList(sView.getHolder().getSurface());
            cameraDevice.createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            usable = false;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            usable = false;
            Log.e("CAMERA", String.format("Error %d in camera %s", error, camera.getId()));
        }
    };

    private Handler handler;
    private boolean usable;

    @SuppressWarnings("ConstantConditions")
    private String getRearCameraId() {
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (int i = 0; i < ids.length; ++i) {
                if (cameraManager.getCameraCharacteristics(ids[i]).get(CameraCharacteristics.
                        LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    return ids[i];
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
        handler = new Handler(Looper.getMainLooper());

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
        if(requestCode == REQUEST_CAMERA_RESULT &&
                permissions[0] == android.Manifest.permission.CAMERA &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            cameraManager.openCamera(getRearCameraId(), deviceCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {}
    }
}
