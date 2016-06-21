/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package senpai;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.jocl.CL;
import org.jocl.Sizeof;
import org.jocl.cl_mem;

/**
 *
 * @author Dennis
 */
public class Float4Test {

    static final int platformIndex = 1;
    static final int deviceIndex = 0;
    static final int[] sourceSize = {3054};
    static final BufferedImage sourceImg = new BufferedImage(sourceSize[0], sourceSize[0], BufferedImage.TYPE_INT_ARGB);
    static final BufferedImage destImg = new BufferedImage(sourceSize[0], sourceSize[0], BufferedImage.TYPE_INT_ARGB);
    static final Graphics sourceImgGraphics = sourceImg.getGraphics();

    public static void main(String args[]) {
        float[] kernelMatrix = {
            1, 1, 1, 1, 0, 0, 0, 0, 2, 2, 2, 2, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            -2, -2, -2, -2, -1, -1, -1, -1, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 
            0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, 0, 0, 0, 0, -1, -1, -1, -1,
            1, 1, 1, 1, 1, 1, 1, 1, -2, -2, -2, -2, 0, 0, 0, 0, -1, -1, -1, -1};
        
                /* float[] kernelMatrix = {-1, -1, -1, -1, 0, 0, 0, 0, 1, 1, 1, 1,
            -2, -2, -2, -2, 0, 0, 0, 0, 2, 2, 2, 2,
            -1, -1, -1, -1, 0, 0, 0, 0, 1, 1, 1, 1};
                 */
        CLUtil.start(platformIndex, deviceIndex, "/senpai/Convolver.cl");

        try {
            BufferedImage source = ImageIO.read(new File("hue.png"));
            ImageUtil.fillFitNewSourceImage(sourceImgGraphics, source, sourceSize[0], sourceSize[0]);

            int[] sourceBackBuffer = ((DataBufferInt) sourceImg.getRaster().getDataBuffer()).getData();
            int[] destBackBuffer = ((DataBufferInt) destImg.getRaster().getDataBuffer()).getData();

            cl_mem clSourceBackBuffer = CLUtil.allocate(Sizeof.cl_uchar4 * sourceBackBuffer.length);
            cl_mem clDestBackBuffer = CLUtil.allocate(Sizeof.cl_uchar4 * destBackBuffer.length);
            cl_mem clActivityLayerBuffer = CLUtil.allocate(Sizeof.cl_float4 * sourceBackBuffer.length);
            cl_mem clSecondActivityLayerBuffer = CLUtil.allocate(Sizeof.cl_float4 * sourceBackBuffer.length);
            cl_mem clKernelBuffer = CLUtil.allocate(Sizeof.cl_float * kernelMatrix.length, CL.CL_MEM_READ_ONLY);
            cl_mem clPaddingDims = CLUtil.allocate(8);
            cl_mem clKernelDims = CLUtil.allocate(8);
            int[] paddingDims = {sourceSize[0], 2};
            int[] kernelDims = {sourceSize[0], 5};

            CLUtil.copyToCLBuffer(kernelMatrix, clKernelBuffer);
            CLUtil.copyToCLBuffer(sourceBackBuffer, clSourceBackBuffer);
            CLUtil.copyToCLBuffer(paddingDims, clPaddingDims);
            CLUtil.copyToCLBuffer(kernelDims, clKernelDims);
            CLUtil.enqueueKernelExec("ARGBToActivityLayer", new long[]{sourceSize[0] * sourceSize[0]}, clSourceBackBuffer, clActivityLayerBuffer);
            CLUtil.enqueueKernelExec("enforcePadding", new long[]{sourceSize[0]}, clActivityLayerBuffer, clPaddingDims);
            long nanos = System.nanoTime();
            CLUtil.enqueueKernelExec("float4Kernel", new long[]{sourceSize[0] - 4, sourceSize[0] - 4}, clActivityLayerBuffer, clSecondActivityLayerBuffer, clKernelBuffer, clKernelDims);            
            System.out.println("Kernel execution time: " + (System.nanoTime() - nanos) + "ns");
            CLUtil.enqueueKernelExec("ActivityLayerToARGB", new long[]{sourceSize[0] * sourceSize[0]}, clSecondActivityLayerBuffer, clDestBackBuffer);

            CLUtil.copyFromCLBuffer(clDestBackBuffer, destBackBuffer);

            //ImageIO.write(destImg, "PNG", new File(System.currentTimeMillis() + ".png"));
            //ImageIO.write(sourceImg, "PNG", new File("C:/users/dennis/desktop/" + System.currentTimeMillis() + "-orig.png"));
        } catch (IOException ex) {
            System.err.println("fuck");
            ex.printStackTrace();
        }
        CLUtil.stop();
    }

}
