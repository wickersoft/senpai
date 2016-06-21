/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package senpai;

import java.io.IOException;
import java.util.ArrayList;
import org.jocl.CL;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import static senpai.CLConversionTest.kernelSourceName;

/**
 *
 * @author Dennis
 */
public class NeuronUtil {

    private static NeuronUtil instance;
    private ArrayList<cl_mem> allocatedCLMemObjects = new ArrayList<>();
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_kernel argbToActivityKernel;
    private cl_kernel activityToArgbKernel;

    private static final int PLATFORM_INDEX = 1;
    private static final int DEVICE_INDEX = 1;

    private NeuronUtil() {
    }

    public static NeuronUtil getInstance() {
        if (instance == null) {
            NeuronUtil tempInstance = new NeuronUtil();
            if (!tempInstance.load(PLATFORM_INDEX, DEVICE_INDEX)) {
                return null;
            }
            instance = tempInstance;
        }
        return instance;
    }

    private boolean load(int platformIndex, int deviceIndex) {
        CL.setExceptionsEnabled(true);

        cl_platform_id platform = CLUtil.getPlatforms()[platformIndex];
        cl_device_id device = CLUtil.getDevices(platform)[deviceIndex];

        System.out.println("Using Device: " + CLUtil.getDeviceName(device));

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);

        // Create a context for the selected device
        context = CL.clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        //Create a command-queue for the selected device
        commandQueue
                = CL.clCreateCommandQueue(context, device, 0, null);

        try {
            program = CLUtil.compileProgramFromResource(context, "/senpai/" + kernelSourceName);
            // Build the program
            CL.clBuildProgram(program, 0, null, null, null, null);
            argbToActivityKernel = CL.clCreateKernel(program, "ARGBToActivityLayer", null);
            activityToArgbKernel = CL.clCreateKernel(program, "ActivityLayerToARGB", null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return true;
    }

    public cl_mem allocate(int size) {
        return allocate(size, CL.CL_MEM_READ_WRITE);
    }

    public cl_mem allocate(int size, long flags) {
        cl_mem newCLBuffer = CL.clCreateBuffer(context, flags, size, null, null);
        allocatedCLMemObjects.add(newCLBuffer);
        return newCLBuffer;
    }

    public void unload() {
        for (cl_mem mem : allocatedCLMemObjects) {
            CL.clReleaseMemObject(mem);
        }
        CL.clReleaseKernel(argbToActivityKernel);
        CL.clReleaseKernel(activityToArgbKernel);
        CL.clReleaseProgram(program);
        CL.clReleaseCommandQueue(commandQueue);
        CL.clReleaseContext(context);

    }
}
