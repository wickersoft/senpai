/*
 * JOCL - Java bindings for OpenCL
 * 
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */
package senpai;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.jocl.*;

/**
 * Self-Enhancing Numeric Processing Artificial Intelligence.
 */
public class SenpAI {

    static final int platformIndex = 1;
    static final int deviceIndex = 0;
    static final int[] sourceSize = {2000};
    static final String kernelSourceName = "Convolver.cl";
    static final BufferedImage sourceImg = new BufferedImage(sourceSize[0], sourceSize[0], BufferedImage.TYPE_INT_ARGB);
    static final BufferedImage destImg = new BufferedImage(sourceSize[0], sourceSize[0], BufferedImage.TYPE_INT_ARGB);
    static final Graphics sourceImgGraphics = sourceImg.getGraphics();

    /**
     * The entry point of this sample
     *
     * @param args Not used
     */
    public static void main(String args[]) {
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        cl_platform_id platform = CLUtil.getPlatforms()[platformIndex];
        cl_device_id device = CLUtil.getDevices(platform)[deviceIndex];
        System.out.println("Using Device: " + CLUtil.getDeviceName(device));

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);

        // Create a context for the selected device
        cl_context context = CL.clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        //Create a command-queue for the selected device
        cl_command_queue commandQueue
                = CL.clCreateCommandQueue(context, device, 0, null);

        // The platform, device type and device number
        // that will be used
        float[] kernelMatrix = {-1, -1, -1, -1, 0, 0, 0, 0, 1, 1, 1, 1,
            -2, -2, -2, -2, 0, 0, 0, 0, 2, 2, 2, 2,
            -1, -1, -1, -1, 0, 0, 0, 0, 1, 1, 1, 1};
        //int[] kernelMatrix = {-1, -1, -1, -1, -2, -2, -2, -2, -1, -1, -1, -1, 
        //0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        //1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1};

        Pointer pKernelMatrix = Pointer.to(kernelMatrix);

        // Create the program from the source code
        try {
            BufferedImage source = ImageIO.read(new File("C:/users/dennis/desktop/hue.png"));
            ImageUtil.fillFitNewSourceImage(sourceImgGraphics, source, sourceSize[0], sourceSize[0]);
            cl_program program = CLUtil.compileProgramFromResource(context, "/senpai/" + kernelSourceName);
            CL.clBuildProgram(program, 0, null, null, null, null);

            cl_kernel kernel = CL.clCreateKernel(program, "convolveKernel", null);
            cl_kernel argbToActivityKernel = CL.clCreateKernel(program, "ARGBToActivityLayer", null);
            cl_kernel activityToArgbKernel = CL.clCreateKernel(program, "ActivityLayerToARGB", null);

            int[] sourceBackBuffer = ((DataBufferInt) sourceImg.getRaster().getDataBuffer()).getData();
            Pointer pSourceBackBuffer = Pointer.to(sourceBackBuffer);
            int[] destBackBuffer = ((DataBufferInt) destImg.getRaster().getDataBuffer()).getData();
            Pointer pDestBackBuffer = Pointer.to(destBackBuffer);

            cl_mem clKernelMatrix = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, kernelMatrix.length * Sizeof.cl_float, null, null);
            cl_mem clParamBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, Sizeof.cl_int, Pointer.to(sourceSize), null);

            cl_mem clSourceBackBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, Sizeof.cl_uchar4 * sourceBackBuffer.length, null, null);
            cl_mem clInputActivity = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_float4 * sourceBackBuffer.length, null, null);
            cl_mem clOutputActivity = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_float4 * sourceBackBuffer.length, null, null);
            cl_mem clDestBackBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_uchar4 * destBackBuffer.length, null, null);

            
            CL.clEnqueueWriteBuffer(commandQueue, clSourceBackBuffer, true, 0, Sizeof.cl_uchar4 * sourceBackBuffer.length, pSourceBackBuffer, 0, null, null);

            
            // Set the arguments for the kernel
            CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(clSourceBackBuffer));
            CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(clDestBackBuffer));
            CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(clKernelMatrix));
            CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(clParamBuffer));

            // Set the work-item dimensions
            long global_work_size[] = new long[]{sourceSize[0] * sourceSize[0]};
            long local_work_size[] = new long[]{1};

            // Execute the kernel
            CL.clEnqueueNDRangeKernel(commandQueue, kernel,
                    1, null,
                    global_work_size, local_work_size, 0, null, null);

            CL.clEnqueueReadBuffer(commandQueue, clDestBackBuffer, true, 0, destBackBuffer.length, pDestBackBuffer, 0, null, null);

            // Release kernel, program, and memory objects
            CL.clReleaseMemObject(clDestBackBuffer);
            CL.clReleaseKernel(kernel);
            CL.clReleaseProgram(program);
            CL.clReleaseCommandQueue(commandQueue);
            CL.clReleaseContext(context);

            ImageIO.write(destImg, "PNG", new File("C:/users/dennis/desktop/" + System.currentTimeMillis() + ".png"));
            ImageIO.write(sourceImg, "PNG", new File("C:/users/dennis/desktop/" + System.currentTimeMillis() + "-orig.png"));
        } catch (IOException ex) {
            System.err.println("fuck");
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
