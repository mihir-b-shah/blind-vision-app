
package com.apps.navai;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.SizeF;

public class PhotoUtils {

    private static CameraManager manager;

    private static class FloatPair {
        private final float x;
        private final float y;

        FloatPair(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }
        public float getY() {
            return y;
        }
    }

    public static void setCameraManager(CameraManager manager) {
        PhotoUtils.manager = manager;
    }

    public static float calcDist(Calibrate.DirVector first, Calibrate.DirVector second) {
        return 0.0f;
    }

    public static float getVerticalAngle(Annotation annot) {
        float VERTICAL_SIZE = 1080;
        float vertPixels = annot.getRect().exactCenterY();
        if(manager == null) return Float.NaN;
        try {
            CameraCharacteristics props = manager.getCameraCharacteristics(getNormCamera());
            float[] focals = props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            float focLength = focals[0];
            SizeF size = props.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float vertHalfView = 2*(float) Math.atan(size.getHeight()/focLength);
            float pixelDist = 0.5f-vertPixels/(VERTICAL_SIZE/2);
            return (float) Math.atan(pixelDist*Math.tan(vertHalfView));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return Float.NaN;
    }

    public static FloatPair correct(float x, float y) {
        try {
            float xCorr,yCorr;
            float[] K = manager.getCameraCharacteristics(getNormCamera())
                    .get(CameraCharacteristics.LENS_RADIAL_DISTORTION);
            if(K == null) return null;
            float r = (float) Math.hypot(x-0.5,y-0.5);
            float f1 = 1f;
            float rpow = 1; final float rsq = r*r;
            for(int i = 0; i<3; ++i) {
                rpow *= r*r;
                f1 += K[i]*rpow;
            }
            float f2 = 2*x*y;
            xCorr = x*f1+K[3]*f2+K[4]*(rsq+2*x*x);
            yCorr = y*f1+K[4]*f2+K[3]*(rsq+2*y*y);
            return new FloatPair(xCorr, yCorr);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getNormCamera() {
        try {
            for(final String cameraId : manager.getCameraIdList()){
                CameraCharacteristics ch = manager.getCameraCharacteristics(cameraId);
                Integer val = ch.get(CameraCharacteristics.LENS_FACING);
                if(val != null && val == CameraCharacteristics.LENS_FACING_BACK) return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
