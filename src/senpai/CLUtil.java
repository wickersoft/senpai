package senpai;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import static org.jocl.Sizeof.cl_event;
import static org.jocl.Sizeof.cl_kernel;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/**
 *
 * @author Dennis
 */
public class CLUtil {

    private static ArrayList<cl_mem> allocatedCLMemObjects = new ArrayList<>();
    private static HashMap<String, cl_kernel> kernelNames = new HashMap<>();
    private static cl_context context;
    private static cl_command_queue commandQueue;
    private static cl_program program;

    public static boolean start(int platformIndex, int deviceIndex, String programPath) {
        CL.setExceptionsEnabled(true);

        System.out.println(getPlatforms().length);
        
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
            program = CLUtil.compileProgramFromResource(context, programPath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return true;
    }

    public static cl_mem allocate(int size) {
        return allocate(size, CL.CL_MEM_READ_WRITE);
    }

    public static cl_mem allocate(int size, long flags) {
        cl_mem newCLBuffer = CL.clCreateBuffer(context, flags, size, null, null);
        allocatedCLMemObjects.add(newCLBuffer);
        return newCLBuffer;
    }

    public static void enqueueKernelExec(String kernelName, long[] dims, cl_mem... args) {
        cl_kernel kernel;
        cl_event evt = new cl_event();
        if (kernelNames.containsKey(kernelName)){
            kernel = kernelNames.get(kernelName);
        } else {
            kernel = CL.clCreateKernel(program, kernelName, null);
            kernelNames.put(kernelName, kernel);
        }

        long[] gwo = new long[dims.length];
        for (int i = 0; i < args.length; i++) {
            CL.clSetKernelArg(kernel, i, Sizeof.cl_mem, Pointer.to(args[i]));
        }
        CL.clEnqueueNDRangeKernel(commandQueue, kernel, dims.length, gwo, dims, null, 0, null, evt);
        try {
            CL.clWaitForEvents(1, new cl_event[] {evt});
        } catch (CLException ex) {
            ex.printStackTrace();
        }
    }
    
    public static void copyToCLBuffer(byte[] hostBuffer, cl_mem deviceBuffer) {
        CL.clEnqueueWriteBuffer(commandQueue, deviceBuffer, true, 0, Sizeof.cl_char * hostBuffer.length, Pointer.to(hostBuffer), 0, null, null);
    }
    
    public static void copyToCLBuffer(int[] hostBuffer, cl_mem deviceBuffer) {
        CL.clEnqueueWriteBuffer(commandQueue, deviceBuffer, true, 0, Sizeof.cl_int * hostBuffer.length, Pointer.to(hostBuffer), 0, null, null);
    }
    
    public static void copyToCLBuffer(float[] hostBuffer, cl_mem deviceBuffer) {
        CL.clEnqueueWriteBuffer(commandQueue, deviceBuffer, true, 0, Sizeof.cl_float * hostBuffer.length, Pointer.to(hostBuffer), 0, null, null);
    }

    public static void copyFromCLBuffer(cl_mem deviceBuffer, byte[] hostBuffer) {
        CL.clEnqueueReadBuffer(commandQueue, deviceBuffer, true, 0, Sizeof.cl_char * hostBuffer.length, Pointer.to(hostBuffer), 0, null, null);
    }

    public static void copyFromCLBuffer(cl_mem deviceBuffer, int[] hostBuffer) {
        CL.clEnqueueReadBuffer(commandQueue, deviceBuffer, true, 0, Sizeof.cl_int * hostBuffer.length, Pointer.to(hostBuffer), 0, null, null);
    }

    public static void copyFromCLBuffer(cl_mem deviceBuffer, float[] hostBuffer) {
        CL.clEnqueueReadBuffer(commandQueue, deviceBuffer, true, 0, Sizeof.cl_float * hostBuffer.length, Pointer.to(hostBuffer), 0, null, null);
    }

    public static void stop() {
        for (cl_mem mem : allocatedCLMemObjects) {
            CL.clReleaseMemObject(mem);
        }
        for (cl_kernel kernel : kernelNames.values()) {
            CL.clReleaseKernel(kernel);
        }
        CL.clReleaseProgram(program);
        CL.clReleaseCommandQueue(commandQueue);
        CL.clReleaseContext(context);
    }

    private static cl_platform_id[] getPlatforms() {
        int numPlatformsArray[] = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        CL.clGetPlatformIDs(platforms.length, platforms, null);
        return platforms;
    }

    private static cl_device_id[] getDevices(cl_platform_id platform) {
        return getDevices(platform, CL.CL_DEVICE_TYPE_ALL);
    }

    private static cl_device_id[] getDevices(cl_platform_id platform, long deviceType) {
        int numDevicesArray[] = new int[1];
        CL.clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        cl_device_id devices[] = new cl_device_id[numDevices];
        CL.clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        return devices;
    }

    private static String getDeviceName(cl_device_id device) {
        byte[] bin = new byte[64];
        long[] returnedSize = new long[1];
        Pointer _bin = Pointer.to(bin);
        CL.clGetDeviceInfo(device, CL.CL_DEVICE_NAME, 64, _bin, returnedSize);
        return new String(bin, 0, (int) returnedSize[0] - 1);
    }

    private static cl_program compileProgramFromResource(cl_context context, String kernelSource) throws IOException {
        InputStream is = SenpAI.class.getResourceAsStream(kernelSource);
        byte[] bin = new byte[is.available()];
        is.read(bin);
        is.close();
        cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{new String(bin)}, null, null);
        // Build the program
        CL.clBuildProgram(program, 0, null, null, null, null);
        return program;
    }

}
