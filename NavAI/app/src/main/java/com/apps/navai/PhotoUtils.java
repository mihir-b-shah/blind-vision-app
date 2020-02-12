
package com.apps.navai;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.SizeF;

import java.util.List;

import static java.lang.Math.*;

public class PhotoUtils {
    private static CameraManager manager;
    private static float[][] rotMatrices;
    private static double[] cacheVertAngles;
    private static double[] cacheHorAngles;

    /**
     * In meters, the length of the arm on the stick.
     * Specific to the device AND this stick.
     */
    private static final double PIVOT_RADIUS = 0.057;

    private static float focLength;
    private static SizeF size;

    static {
        rotMatrices = new float[2][];
        cacheVertAngles = new double[2];
        cacheHorAngles = new double[2];
    }

    private static void setup() {
        try {
            CameraCharacteristics props = manager.getCameraCharacteristics(getNormCamera());
            List<CameraCharacteristics.Key<?>> keys = props.getKeys();
            for(CameraCharacteristics.Key key: keys) {
                System.out.println(key.getName());
            }
            float[] focals = props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            focLength = focals[0];
            size = props.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static class PolarVector {
        private final double mgn;
        private final double dir;

        PolarVector(double mgn, double dir) {
            this.mgn = mgn; this.dir = dir;
        }

        public double getMgn() {return mgn;}
        public double getDir() {return dir;}
    }

    private static class DirVector {
        private final double x,y,z;
        DirVector(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }

        static double dot(DirVector v1, DirVector v2) {
            return v1.x*v2.x+v1.y*v2.y+v1.z*v2.z;
        }
        double zCosine() {return acos(z/sqrt(x*x+y*y+z*z));}
        double horizAngle() {return x/sqrt(x*x+y*y+z*z);}
    }

    public static PolarVector calcTrajectory(CameraManager manager, Annotation a1, Annotation a2,
                                             float[] rotMat1, float[] rotMat2) {
        PhotoUtils.manager = manager;
        rotMatrices[0] = rotMat1; rotMatrices[1] = rotMat2;
        correct(a1); correct(a2);
        setup();
        PolarVector polarVector = calcTrajectory(a1, a2);
        manager = null;
        return polarVector;
    }

    private static PolarVector calcTrajectory(Annotation a1, Annotation a2) {
        final DirVector v1 = getLocationVector(0,
                0.5 - a1.getRect().exactCenterY()/CustomCamera.CAMERA_HEIGHT,
                -0.5 + a1.getRect().exactCenterX()/CustomCamera.CAMERA_WIDTH);
        final DirVector v2 = getLocationVector(1,
                0.5 - a2.getRect().exactCenterY()/CustomCamera.CAMERA_HEIGHT,
                -0.5 + a1.getRect().exactCenterX()/CustomCamera.CAMERA_WIDTH);

        final DirVector pv1 = getLocationVector(0, 0, 0);
        final DirVector pv2 = getLocationVector(1, 0, 0);

        double ang1 = v1.zCosine(); double ang2 = v2.zCosine();

        double beta = Math.acos(DirVector.dot(pv1, pv2)/
                (DirVector.dot(pv1, pv1)*DirVector.dot(pv2, pv2)));
        double dist = (PIVOT_RADIUS*(cos(beta)-1)-PIVOT_RADIUS*sin(beta)/tan(ang2))/
                        (cos(ang1)-sin(ang1)/tan(ang2));
        return new PolarVector(dist*cos(ang1), v1.horizAngle());
    }

    private static DirVector getLocationVector(int index, double normVertical, double normHorizontal) {
        double theta = getVerticalAngle(normVertical);
        cacheVertAngles[index] = theta;
        double alpha = getHorizontalAngle(normHorizontal);
        cacheHorAngles[index] = alpha;
        float[] rotMatrix = rotMatrices[index];
        double x = sin(theta); double y = -tan(alpha); double z = -cos(theta);

        return new DirVector(x*rotMatrix[0]+y*rotMatrix[3]+z*rotMatrix[6],
                             x*rotMatrix[1]+y*rotMatrix[4]+z*rotMatrix[7],
                             x*rotMatrix[2]+y*rotMatrix[5]+z*rotMatrix[8]);
    }

    /**
     * call only after setup()
     *
     * @param normVertical the normalized vertical pixel component
     * @return get angle of elevation
     */
    public static double getVerticalAngle(double normVertical) {
        return atan(2*normVertical*tan(2*atan(size.getHeight()/focLength)));
    }

    private static double getHorizontalAngle(double normHorizontal) {
        return atan(2*normHorizontal*tan(2*atan(size.getWidth()/focLength)));
    }

    // may not get called on a majority of devices due to the lack of the key
    // and design flaws for the device.
    private static void correct(Annotation annot) {
        try {
            final float[] K = manager.getCameraCharacteristics(getNormCamera())
                    .get(CameraCharacteristics.LENS_RADIAL_DISTORTION);
            if(K == null) return;
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
