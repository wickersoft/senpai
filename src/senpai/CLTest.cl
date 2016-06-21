__kernel void sampleKernel(__global int *c) {
    c[get_global_id(0) % 100]++;
}