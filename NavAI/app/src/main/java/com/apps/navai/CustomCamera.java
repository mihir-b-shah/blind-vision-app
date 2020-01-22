package com.apps.navai;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.SERVICE_RESPONSE;

public class CustomCamera extends AppCompatActivity {

    private static final int REQUEST_CAMERA_RESULT = 1;
    private ImageReader imageReader;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder builder;
    private CameraCaptureSession session;
    private SurfaceHolder surfaceHolder;
    private String mCurrentPhotoPath;
    private int previewCtr = 100;

    private boolean configured;
    private int captureCtr;
    private int toneCtr;
    private int state;

    private Queue<Image> images;

    private static final int VOLUME = 85;
    private static final int DURATION = 5000;
    private static final int FREQ = 500;
    private static final int BEEP_LENGTH = 100;

    // copied some google code
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_TAKEN = 4;

    public static final int CAMERA_WIDTH = 1920;
    public static final int CAMERA_HEIGHT = 1080;

    private static final int QUEUE_SIZE = 4;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(SERVICE_RESPONSE)) {
                int code = intent.getIntExtra(INT_1, -1);
                if (code == 1) {
                    float[] rotMat = intent.getFloatArrayExtra("rot-mat");
                    System.out.println("Got the dir vector!");
                    Intent next = new Intent();
                    next.putExtra("photo-path", mCurrentPhotoPath);
                    next.putExtra("rot-mat", rotMat);
                    CustomCamera.this.setResult(Activity.RESULT_OK, next);
                    CustomCamera.this.finish();
                }
            }
        }
    };

    private Handler handler;
    private HandlerThread backThread;

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("call-count", captureCtr);
        bundle.putInt("tone-count", toneCtr);
    }

    private void capturePicture() {
        try {
            CaptureRequest.Builder localBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            localBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            state = STATE_WAITING_LOCK;
            localBuilder.addTarget(surfaceHolder.getSurface());
            localBuilder.addTarget(imageReader.getSurface());
            localBuilder.set(CaptureRequest.EDGE_MODE,
                    CameraMetadata.EDGE_MODE_HIGH_QUALITY);
            localBuilder.set(CaptureRequest.SHADING_MODE,
                    CameraMetadata.SHADING_MODE_HIGH_QUALITY);
            localBuilder.set(CaptureRequest.TONEMAP_MODE,
                    CameraMetadata.TONEMAP_MODE_HIGH_QUALITY);
            localBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
                    CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
            localBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
            localBuilder.set(CaptureRequest.HOT_PIXEL_MODE,
                    CameraMetadata.HOT_PIXEL_MODE_HIGH_QUALITY);
            localBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
                    CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
            localBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);
            localBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO);
            localBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            CaptureRequest req = localBuilder.build();
            localBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);

            session.stopRepeating();
            session.abortCaptures();
            session.capture(req, realCallback, handler);
            //state = STATE_TAKEN;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void preCapture() {
        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        state = STATE_WAITING_PRECAPTURE;
        try {
            System.out.println("We called precapture!");
            session.capture(builder.build(), captureCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback realCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    if (result.getSequenceId() == QUEUE_SIZE) {
                        (new ImageSaver()).run();
                    }
                }
    };

    private CameraCaptureSession.StateCallback stateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    // make call to camera
                    if(!configured) {
                        try {
                            CustomCamera.this.session = session;
                            builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                            builder.addTarget(surfaceHolder.getSurface());
                            builder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            CaptureRequest request = builder.build();
                            configured = true;
                            session.setRepeatingRequest(request, captureCallback, null);
                        } catch (CameraAccessException e) {
                            System.err.println(e.getMessage());
                        }
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
                    CustomCamera.this.session = session;
                    switch(state) {
                        case STATE_PREVIEW:
                            --previewCtr;
                            if(previewCtr > 0) return;
                            System.out.println("Case 0, previewCtr == 100.");
                            capturePicture();
                            break;
                        case STATE_WAITING_LOCK:
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if(afState == null) {
                                System.out.println("Case 1, afstate == null.");
                                capturePicture();
                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                if (aeState == null ||
                                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                    state = STATE_TAKEN;
                                    System.out.println("Case 1, picture state taken.");
                                    capturePicture();
                                } else {
                                    System.out.println("About to precapture 240!");
                                    preCapture();
                                }
                            }
                            break;
                        case STATE_WAITING_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null ||
                                    aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                    aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                                state = STATE_WAITING_NON_PRECAPTURE;
                            }
                            break;
                        }
                        case STATE_WAITING_NON_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                                state = STATE_TAKEN;
                                System.out.println("Capture 260");
                                capturePicture();
                            }
                            break;
                        }
                    }
                }
            };

    private void unlockFocus() throws CameraAccessException {
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        builder.removeTarget(surfaceHolder.getSurface());
        builder.removeTarget(imageReader.getSurface());
        session.stopRepeating();
        session.abortCaptures();
        session.close();
        cameraDevice.close();
    }

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
        @Override
        public void run() {
            File file = null;
            Image best = null;
            int size = 0;
            // could use a stream here...
            while(!images.isEmpty()) {
                Image curr = images.poll();
                int currSize;
                if((currSize = curr.getPlanes()[0].getBuffer().remaining()) > size) {
                    size = currSize;
                    best = curr;
                } else {
                    curr.close();
                }
            }

            java.nio.ByteBuffer buffer = best.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file = createImageFile());
                output.write(bytes);
                System.out.println("File bytes written.");
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                best.close();
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
            System.out.println("WE DONE 360!");
            /*
            System.out.println("Got to intent!");
            ++CustomCamera.this.captureCtr;
            mCurrentPhotoPath = "PATH"; //file.getAbsolutePath();
            Intent next = new Intent(getApplicationContext(), Calibrate.class);
            next.putExtra(INT_1, 1);
            startService(next); */
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
        images = new ArrayDeque<>(QUEUE_SIZE);
        setContentView(R.layout.activity_custom_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        captureCtr = savedInstanceState == null ? 0 :
                savedInstanceState.getInt("call-count", -1);
        toneCtr = savedInstanceState == null ? 0 :
                savedInstanceState.getInt("tone-count", -1);

        /*
        if(toneCtr == captureCtr && toneCtr == 0) {
            final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, VOLUME);
            final int DIFF = FREQ - BEEP_LENGTH;
            for (int time = 0; time < DURATION; time += FREQ) {
                tg.stopTone();
                tg.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, BEEP_LENGTH);
                try {
                    Thread.sleep(DIFF);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            tg.release();
            ++toneCtr;
        }
*/
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
        handler = null;
        try {
            backThread.join();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    private void openCamera() {
        try {
            imageReader = ImageReader.newInstance(
                    CAMERA_WIDTH, CAMERA_HEIGHT, ImageFormat.JPEG, QUEUE_SIZE+1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image img = reader.acquireLatestImage();
                if(img == null) {
                    return;
                }
                images.offer(img);
            }, handler);
            cameraManager.openCamera(getRearCameraId(), deviceCallback, handler);
        } catch (CameraAccessException|SecurityException e) {
        }
    }
}