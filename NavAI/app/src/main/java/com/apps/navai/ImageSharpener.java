
package com.apps.navai;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageSharpener {
    public static void main(String[] args) throws IOException {
        File input = new File("Photo.jpg");
        BufferedImage image = ImageIO.read(input);
        System.out.println(image.getType());

        float[] kernel = {-1f,-1f,-1f,-1f,9f,-1f,-1f,-1f,-1f};
        ConvolveOp convolution = new ConvolveOp(new Kernel(3,3,kernel));
        BufferedImage newImage = convolution.filter(image, null);

        ImageIO.write(newImage, "jpg", new File("NewPhoto.jpg"));
        BufferedImage newImage2 = filter(image);
        ImageIO.write(newImage2, "jpg", new File("NewPhoto2.jpg"));
    }

    // model this convolution for some interface of image
    public static BufferedImage filter(BufferedImage image) {
        BufferedImage newImage = new BufferedImage(image.getWidth(),
                image.getHeight(), image.getType());
        WritableRaster dst = newImage.getRaster();
        Raster src = image.getRaster();
        float[] kern = {-1f,-1f,-1f,-1f,9f,-1f,-1f,-1f,-1f};
        Kernel kernel = new Kernel(3,3,kern);
        try {
            SampleModel sm = src.getSampleModel();
            int numBands = src.getNumBands();
            int srcHeight = src.getHeight();
            int srcWidth = src.getWidth();
            int xOrigin = kernel.getXOrigin();
            int yOrigin = kernel.getYOrigin();
            int kWidth = kernel.getWidth();
            int kHeight = kernel.getHeight();
            float[] data = kernel.getKernelData(null);
            int srcMinX = src.getMinX();
            int srcMinY = src.getMinY();
            int dstMinX = dst.getMinX();
            int dstMinY = dst.getMinY();
            int srcConvMaxX = srcWidth - (kWidth - xOrigin - 1);
            int srcConvMaxY = srcHeight - (kHeight - yOrigin - 1);
            int[] maxValues = new int[numBands];
            int[] masks = new int[numBands];
            int[] sampleSizes = sm.getSampleSize();
            for (int i=0; i < numBands; i++){
                maxValues[i] = (1 << sampleSizes[i]) - 1;
                masks[i] = ~(maxValues[i]);
            }
            // Processing bounds
            float[] pixels = null;
            pixels = src.getPixels(srcMinX, srcMinY, srcWidth, srcHeight, pixels);
            float[] newPixels = new float[pixels.length];
            int rowLength = srcWidth*numBands;
            // Cycle over pixels to be calculated
            for (int i = yOrigin; i < srcConvMaxY; i++){
                for (int j = xOrigin; j < srcConvMaxX; j++){
                    // Take kernel data in backward direction, convolution
                    int kernelIdx = data.length - 1;
                    int pixelIndex = i * rowLength + j * numBands;
                    for (int hIdx = 0, rasterHIdx = i - yOrigin;
                         hIdx < kHeight;
                         hIdx++, rasterHIdx++
                    ){
                        for (int wIdx = 0, rasterWIdx = j - xOrigin;
                             wIdx < kWidth;
                             wIdx++, rasterWIdx++
                        ){
                            int curIndex = rasterHIdx * rowLength + rasterWIdx * numBands;
                            for (int idx=0; idx < numBands; idx++){
                                newPixels[pixelIndex+idx] += data[kernelIdx] * pixels[curIndex+idx];
                            }
                            kernelIdx--;
                        }
                    }
                    // Check for overflow now
                    for (int idx=0; idx < numBands; idx++){
                        if (((int)newPixels[pixelIndex+idx] & masks[idx]) != 0) {
                            if (newPixels[pixelIndex+idx] < 0) {
                                newPixels[pixelIndex+idx] = 0;
                            } else {
                                newPixels[pixelIndex+idx] = maxValues[idx];
                            }
                        }
                    }
                }
            }
            dst.setPixels(dstMinX, dstMinY, srcWidth, srcHeight, newPixels);
        } catch (Exception e) { // Something goes wrong, signal error
            return null;
        }
        newImage.setData(dst);
        return newImage;
    }
}
