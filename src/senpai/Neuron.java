/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package senpai;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_mem;

/**
 *
 * @author Dennis
 */
public class Neuron {

    private final cl_mem clKernelBuffer;

    public Neuron(cl_mem clKernelBuffer) {
        this.clKernelBuffer = clKernelBuffer;
    }

    public void apply(cl_mem source, cl_mem dest) {
        CL.clSetKernelArg(argbToActivityKernel, 0, Sizeof.cl_mem, Pointer.to(clSourceBackBuffer));
        CL.clSetKernelArg(argbToActivityKernel, 1, Sizeof.cl_mem, Pointer.to(clActivityLayerBuffer));
    }

}
