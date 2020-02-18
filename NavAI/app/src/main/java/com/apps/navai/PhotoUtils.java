
package com.apps.navai;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.SizeF;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static java.lang.Math.*;

public class PhotoUtils {
    private static CameraManager manager;

    private static final float K1 = 0.103827769f;
    private static final float K2 = 0.905547841f;
    private static final float P1 = -0.00389067972f;
    private static final float P2 = -0.00469542985f;
    private static final float K3 = -5.15973196f;

    private static float[][] rotMatrices;
    private static float[] focusDistances;
    private static float angleAvg;

    /**
     * In meters, the length of the arm on the stick.
     * Specific to the device AND this stick.
     */
    private static final double ROT_RADIUS = 0.0575;
    private static float focLength;
    private static SizeF size;

    static {
        rotMatrices = new float[2][];
        focusDistances = new float[2];
    }

    private static void setup() {
        try {
            CameraCharacteristics props = manager.getCameraCharacteristics(getNormCamera());
            float[] focals = props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            focLength = focals[0];
            size = props.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static class PolarVector implements Serializable {
        private double mgn;
        private double dir;

        PolarVector(double mgn, double dir) {
            this.mgn = mgn; this.dir = dir;
        }

        public double getMgn() {return mgn;}
        public double getDir() {return dir;}

        public void writeObject(ObjectOutputStream oos) {
            try {
                oos.writeDouble(mgn);
                oos.writeDouble(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void readObject(ObjectInputStream ois) {
            try {
                mgn = ois.readDouble();
                dir = ois.readDouble();
            } catch (IOException e) {

            }
        }
    }

    public static PolarVector calcTrajectory(CameraManager manager, float fd1, float fd2,
                                             Annotation a1, Annotation a2,
                                             float[] rotMat1, float[] rotMat2) {
        PhotoUtils.manager = manager;
        rotMatrices[0] = rotMat1; rotMatrices[1] = rotMat2;
        focusDistances[0] = fd1; focusDistances[1] = fd2;
        correct(a1); correct(a2);
        setup();
        PolarVector polarVector = calcTrajectory(a1, a2);
        PhotoUtils.manager = null;
        return polarVector;
    }

    private static PolarVector calcTrajectory(Annotation a1, Annotation a2) {
        final double Z1 = asin(getVerticalComp(0,
                0.5 - a1.getRect().exactCenterY()/CustomCamera.CAMERA_HEIGHT,
                -0.5 + a1.getRect().exactCenterX()/CustomCamera.CAMERA_WIDTH));
        final double Z2 = asin(getVerticalComp(1,
                0.5 - a2.getRect().exactCenterY()/CustomCamera.CAMERA_HEIGHT,
                -0.5 + a2.getRect().exactCenterX()/CustomCamera.CAMERA_WIDTH));
        final double B1 = asin(getVerticalComp(0, 0, 0));
        final double B2 = asin(getVerticalComp(1, 0,0));
        return new PolarVector(ROT_RADIUS*((sin(B2-Z2)-sin(B1-Z2))/sin(Z2-Z1)+cos(B1))*cos(B1),
                angleAvg/2f);
    }

    private static double getVerticalComp(int index, double normVertical, double normHorizontal) {
        double theta = getVerticalAngle(normVertical, focusDistances[index]);
        double alpha = getHorizontalAngle(normHorizontal, focusDistances[index]);
        angleAvg += alpha;

        float[] rotMatrix = rotMatrices[index];
        double x = sin(theta); double y = -cos(theta)*sin(alpha); double z = -cos(theta)*cos(alpha);
        return x*rotMatrix[6]+y*rotMatrix[7]+z*rotMatrix[8];
    }

    /**
     * call only after setup()
     *
     * @param normVertical the normalized vertical pixel component
     * @return get angle of elevation
     */
    private static double getVerticalAngle(double normVertical, double fd) {
        return atan(normVertical*size.getHeight()/(focLength*(fd < 0 ? 1 : fd+1)));
    }

    private static double getHorizontalAngle(double normHorizontal, double fd) {
        return atan(normHorizontal*size.getWidth()/(focLength*(fd < 0 ? 1 : fd+1)));
    }

    private static void correct(Annotation annot) {
        try {
            final float[] K = manager.getCameraCharacteristics(getNormCamera())
                    .get(CameraCharacteristics.LENS_RADIAL_DISTORTION);
            System.out.println("K == null: " + (K==null));
            if(K == null) {
                // use the values computed by OpenCV
                // this is specific to the current device testing
                final Rect rect = annot.getRect();
                final float halfWidth = CustomCamera.CAMERA_WIDTH >>> 1;
                final float halfHeight = CustomCamera.CAMERA_HEIGHT >>> 1;

                final float xCurr = (rect.exactCenterX()-halfWidth)/CustomCamera.CAMERA_WIDTH;
                final float yCurr = (-rect.exactCenterY()+halfHeight)/CustomCamera.CAMERA_HEIGHT;

                final float r2 = xCurr*xCurr+yCurr*yCurr;
                final float rT = K1*r2+K2*r2*r2+K3*r2*r2*r2;
                float dx = xCurr*rT + P1*(r2 + 2*xCurr*xCurr) + 2*P2*xCurr*yCurr;
                float dy = yCurr*rT + P2*(r2 + 2*xCurr*yCurr) + 2*P1*xCurr*yCurr;

                // dx is on the range [-0.5,0.5]
                dx *= CustomCamera.CAMERA_WIDTH;
                dy *= CustomCamera.CAMERA_HEIGHT;

                rect.left += dx; rect.right += dx;
                rect.bottom -= dy; rect.top -= dy;
                return;
            }

            final Rect rect = annot.getRect();
            final double x = rect.exactCenterX();
            final double y = rect.exactCenterY();
            double r = hypot(x-0.5,y-0.5);
            double f1 = 1;
            double rpow = 1; final double rsq = r*r;
            for(int i = 0; i<3; ++i) {
                rpow *= r*r;
                f1 += K[i]*rpow;
            }

            final double f2 = 2*x*y;
            final double dx = x*(f1+K[3]*f2+K[4]*(rsq+2*x*x)-1);
            double dy = y*(f1+K[4]*f2+K[3]*(rsq+2*y*y)-1);
            rect.left += dx; rect.right += dx;
            rect.bottom += dy; rect.top += dy;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
