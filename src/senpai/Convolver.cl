__kernel void float16InputLayer(__global float4* source, __global float16* dest, __constant float16 krnl, __constant int* dims) {
    int id = get_global_id(0) + 2 + width[0] * (get_global_id(1) + 2);
    float16 out = 0.0;
    int bufIdx, krnlIdx;
    float4 in;
    float16 in16;
    for(int yy = -2; yy < 3; yy++) {
        bufIdx = id + width[0] * yy - 2;
        krnlXIdx = width[1] * yy + 10;
        in = source[bufIdx++];
        in16 = (in, in, in, in);
        out = mad(in16, krnl[krnlIdx++], out);
        in = source[bufIdx++];
        in16 = (in, in, in, in);
        out = mad(in16, krnl[krnlIdx++], out);
        in = source[bufIdx++];
        in16 = (in, in, in, in);
        out = mad(in16, krnl[krnlIdx++], out);
        in = source[bufIdx++];
        in16 = (in, in, in, in);
        out = mad(in16, krnl[krnlIdx++], out);
        in = source[bufIdx++];
        in16 = (in, in, in, in);
        out = mad(in16, krnl[krnlIdx++], out);
    }
    out.s3 = 1;
    out.s7 = 1;
    out.sb = 1;
    out.sf = 1;
    dest[id] = out;
}


__kernel void float4Kernel(__global float4* source, __global float4* dest, __constant float4* krnl, __constant int* width) {
    int id = get_global_id(0) + 2 + width[0] * (get_global_id(1) + 2);
    float4 out = 0.0;
    int bufIdx, krnlXIdx;
    for(int yy = -2; yy < 3; yy++) {
        bufIdx = id + width[0] * yy - 2;
        krnlXIdx = width[1] * yy + 10;
        out = mad(source[bufIdx++], krnl[krnlXIdx++], out);
        out = mad(source[bufIdx++], krnl[krnlXIdx++], out);
        out = mad(source[bufIdx++], krnl[krnlXIdx++], out);
        out = mad(source[bufIdx++], krnl[krnlXIdx++], out);
        out = mad(source[bufIdx++], krnl[krnlXIdx++], out);
    }
    out.s3 = 1;
    dest[id] = out;
}

__kernel void enforcePadding(__global float4* source, __global int* dims) {
    int wid = dims[0];
    int padRadius = dims[1];
    int x = get_global_id(0);
    for(int i = 0; i < padRadius; i++) {
        source[x + wid * i] = (0, 0, 0, 1);
        source[x + wid * (wid - i - 1)] = (0, 0, 0, 1);
        source[x * wid + i] = (0, 0, 0, 1);
        source[x * wid + (wid - i - 1)] = (0, 0, 0, 1);
    }
}

__kernel void ARGBToActivityLayer(__global uchar4* input, __global float4* output) {
    int id = get_global_id(0);
    output[id] = convert_float4(input[id]) / 255.0;
}

__kernel void ActivityLayerToARGB(__global float4* input, __global uchar4* output) {
    int id = get_global_id(0);
    output[id] = convert_uchar4(input[id] * 255.0);
}