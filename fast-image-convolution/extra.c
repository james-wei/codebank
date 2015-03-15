#include <emmintrin.h>
#include <string.h> // Imported to support memset and memcpy methods
#include <omp.h>

#define CONV(in, out, data_size_X, data_size_Y, kernel, kernel_x, kernel_y) ({\
    int kern_cent_X = (kernel_x - 1)/2;\
    int kern_cent_Y = (kernel_y - 1)/2;\
    int x, y, i, padded_x, padded_y, kern_uf = 0, kernel_size = kernel_x * kernel_y, bound, copy_kernel = 1;\
    float *padded_in, kern_flipped[kernel_size], kern_local[kernel_size];\
    __m128 result;\
    padded_x = (kernel_x - 1) + data_size_X,\
    padded_y = (kernel_y - 1) + data_size_Y;\
    padded_in = (float*) malloc(padded_x*padded_y*sizeof(float));\
    bound = padded_x*kern_cent_Y;\
    _Pragma("omp parallel for firstprivate(bound)")\
    for (i = 0; i < bound; i++) {\
        padded_in[i] = 0.0;\
    }\
    _Pragma("omp parallel for firstprivate (x, padded_in, in, kern_cent_X, kern_cent_Y, data_size_X, data_size_Y, padded_x)")\
    for (y = 0; y < data_size_Y; y++) {\
        for (x = 0; x < kern_cent_X; x++) {\
            padded_in[(y + kern_cent_Y)*padded_x + x] = 0.0;\
        }\
        for (x = 0; x < data_size_X - 31; x += 32) {\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x, _mm_loadu_ps(in + y*data_size_X + x));\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 4, _mm_loadu_ps(in + y*data_size_X + x + 4));\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 8, _mm_loadu_ps(in + y*data_size_X + x + 8));\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 12, _mm_loadu_ps(in + y*data_size_X + x + 12));\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 16, _mm_loadu_ps(in + y*data_size_X + x + 16));\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 20, _mm_loadu_ps(in + y*data_size_X + x + 20));\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 24, _mm_loadu_ps(in + y*data_size_X + x + 24));\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 28, _mm_loadu_ps(in + y*data_size_X + x + 28));\
        }\
        for ( ; x < data_size_X - 3; x += 4) {\
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x, _mm_loadu_ps(in + y*data_size_X + x));\
        }\
        for ( ; x < data_size_X; x++) {\
            padded_in[(y + kern_cent_Y)*padded_x + kern_cent_X + x] = in[y*data_size_X + x];\
        }\
        for (x = data_size_X + kern_cent_X; x < padded_x; x++) {\
            padded_in[(y + kern_cent_Y)*padded_x + x] = 0.0;\
        }\
    }\
    bound = padded_x*padded_y;\
    _Pragma("omp parallel for firstprivate(padded_x, kern_cent_Y, data_size_Y, bound)")\
    for (i = padded_x*(kern_cent_Y + data_size_Y); i < bound; i++) {\
        padded_in[i] = 0.0;\
    }\
    for (kern_uf = 0; kern_uf < kernel_size; kern_uf++) {\
        kern_flipped[kern_uf] = kernel[kernel_size - 1 - kern_uf];\
    }\
    _Pragma("omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, kernel_x, kernel_y, padded_x)")\
    for (y = 0; y < data_size_Y; y++) {\
        if (copy_kernel) {\
            memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);\
            copy_kernel = 0;\
        }\
        for (x = 0; x < data_size_X - 3; x += 4) {\
            result = _mm_setzero_ps();\
            for (i = 0; i < kernel_size - 8; i += 9) {\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % kernel_x) + padded_x*(y + (i/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + (i + 1) % kernel_x) + padded_x*(y + ((i + 1)/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + (i + 2) % kernel_x) + padded_x*(y + ((i + 2)/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + (i + 3) % kernel_x) + padded_x*(y + ((i + 3)/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + (i + 4) % kernel_x) + padded_x*(y + ((i + 4)/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + (i + 5) % kernel_x) + padded_x*(y + ((i + 5)/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + (i + 6) % kernel_x) + padded_x*(y + ((i + 6)/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 7)), _mm_loadu_ps(padded_in + (x + (i + 7) % kernel_x) + padded_x*(y + ((i + 7)/kernel_x)))));\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 8)), _mm_loadu_ps(padded_in + (x + (i + 8) % kernel_x) + padded_x*(y + ((i + 8)/kernel_x)))));\
            }\
            for ( ; i < kernel_size; i++) {\
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % kernel_x) + padded_x*(y + (i/kernel_x)))));\
            }\
            _mm_storeu_ps(out + (x + y*data_size_X), result);\
        }\
        for ( ; x < data_size_X; x++) {\
            for (i = 0; i < kernel_size; i++) {\
                    out[x + y*data_size_X] += kern_local[i]*padded_in[(x + i % kernel_x) + padded_x*(y + (i/kernel_x))];\
            }\
        }\
    }\
    free(padded_in);\
})

int conv2D(float* in, float* out, int data_size_X, int data_size_Y,
                    float* kernel, int kernel_x, int kernel_y)
{
    
    if (kernel_x == 3 && kernel_y == 3) {
        // Proj3 code:
        // the x coordinate of the kernel's center
        int kern_cent_X = 1;
        // the y coordinate of the kernel's center
        int kern_cent_Y = 1;

        int x, y, i,
            padded_x,       // x-dimension of padded matrix
            padded_y,       // y-dimension of padded matrix
            kern_uf = 0,
            kernel_size = 9,
            bound,
            copy_kernel = 1;    // Boolean whether to create local kernel copy
            
        float *padded_in,                   // Pointer to padded matrix
              kern_flipped[kernel_size],    // Pointer to flipped kernel
              kern_local[kernel_size];      // Pointer to local flipped kernel

        __m128 result;
        // Allocate memory for a padded matrix:
        padded_x = (2) + data_size_X,
        padded_y = (2) + data_size_Y;
        padded_in = (float*) malloc(padded_x*padded_y*sizeof(float));
        // Pad top row(s):
        bound = padded_x*kern_cent_Y;
        #pragma omp parallel for firstprivate(bound)
        for (i = 0; i < bound; i++) {
            padded_in[i] = 0.0;
        }
        // Pad left/right edges and fill in middle with data:
        #pragma omp parallel for firstprivate (x, padded_in, in, kern_cent_X, kern_cent_Y, data_size_X, data_size_Y, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            // Pad left:
            for (x = 0; x < kern_cent_X; x++) {
                padded_in[(y + kern_cent_Y)*padded_x + x] = 0.0;
            }
            // Fill data:
            for (x = 0; x < data_size_X - 31; x += 32) {
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x, _mm_loadu_ps(in + y*data_size_X + x));
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 4, _mm_loadu_ps(in + y*data_size_X + x + 4));
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 8, _mm_loadu_ps(in + y*data_size_X + x + 8));
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 12, _mm_loadu_ps(in + y*data_size_X + x + 12));
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 16, _mm_loadu_ps(in + y*data_size_X + x + 16));
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 20, _mm_loadu_ps(in + y*data_size_X + x + 20));
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 24, _mm_loadu_ps(in + y*data_size_X + x + 24));
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 28, _mm_loadu_ps(in + y*data_size_X + x + 28));
            }
            for ( ; x < data_size_X - 3; x += 4) {
                _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x, _mm_loadu_ps(in + y*data_size_X + x));
            }
            for ( ; x < data_size_X; x++) {
                padded_in[(y + kern_cent_Y)*padded_x + kern_cent_X + x] = in[y*data_size_X + x];
            }
            // Pad right:
            for (x = data_size_X + kern_cent_X; x < padded_x; x++) {
                padded_in[(y + kern_cent_Y)*padded_x + x] = 0.0;
            }
        }
        // Pad bottom row(s):
        bound = padded_x*padded_y;
        #pragma omp parallel for firstprivate(padded_x, kern_cent_Y, data_size_Y, bound)
        for (i = padded_x*(kern_cent_Y + data_size_Y); i < bound; i++) {
            padded_in[i] = 0.0;
        }
        // Flip the kernel:
        for (kern_uf = 0; kern_uf < kernel_size; kern_uf++) {
            kern_flipped[kern_uf] = kernel[kernel_size - 1 - kern_uf];
        }
        // Convolution:
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, \
                                              kern_flipped, result, out, padded_in, \
                                              data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local), _mm_loadu_ps(padded_in + x + padded_x*y)));                
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 1), _mm_loadu_ps(padded_in + (x + 1) + padded_x*y)));                  
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 2), _mm_loadu_ps(padded_in + (x + 2) + padded_x*y)));                  
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 3), _mm_loadu_ps(padded_in + x + padded_x*(y + 1))));                  
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 4), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + 1))));                
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 5), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + 1))));                
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 6), _mm_loadu_ps(padded_in + x + padded_x*(y + 2))));                  
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 7), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + 2))));                
                result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + 8), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + 2))));
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            // Convolute elements that don't fit inside of 128-bit vector:
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 3) + padded_x*(y + (i/3))];
                }
            }
        }
        free(padded_in);
        return 1;
    }

    int kern_cent_X = (kernel_x - 1)/2;
    int kern_cent_Y = (kernel_y - 1)/2;
    int x, y, i,
        padded_x,       // x-dimension of padded matrix
        padded_y,       // y-dimension of padded matrix
        kern_uf = 0,
        kernel_size = kernel_x*kernel_y,
        bound,
        copy_kernel = 1; // Boolean whether to create local kernel copy
        
    float *padded_in,                   // Pointer to padded matrix
          kern_flipped[kernel_size],    // Pointer to flipped kernel
          kern_local[kernel_size];      // Pointer to local flipped kernel

    __m128 result;
    // Allocate memory for a padded matrix:
    padded_x = (kernel_x - 1) + data_size_X;
    padded_y = (kernel_y - 1) + data_size_Y;
    padded_in = (float*) malloc(padded_x*padded_y*sizeof(float));
    // Pad top row(s):
    bound = padded_x*kern_cent_Y;
    
    #pragma omp parallel for firstprivate(bound)
    for (i = 0; i < bound; i++) {
        padded_in[i] = 0.0;
    }
    // Pad left/right edges and fill in middle with data:
    #pragma omp parallel for firstprivate (x, padded_in, in, kern_cent_X, kern_cent_Y, data_size_X, data_size_Y, padded_x)
    for (y = 0; y < data_size_Y; y++) {
        // Pad left:
        for (x = 0; x < kern_cent_X; x++) {
            padded_in[(y + kern_cent_Y)*padded_x + x] = 0.0;
        }
        // Fill data:
        for (x = 0; x < data_size_X - 31; x += 32) {
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x, _mm_loadu_ps(in + y*data_size_X + x));
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 4, _mm_loadu_ps(in + y*data_size_X + x + 4));
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 8, _mm_loadu_ps(in + y*data_size_X + x + 8));
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 12, _mm_loadu_ps(in + y*data_size_X + x + 12));
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 16, _mm_loadu_ps(in + y*data_size_X + x + 16));
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 20, _mm_loadu_ps(in + y*data_size_X + x + 20));
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 24, _mm_loadu_ps(in + y*data_size_X + x + 24));
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x + 28, _mm_loadu_ps(in + y*data_size_X + x + 28));
        }
        for ( ; x < data_size_X - 3; x += 4) {
            _mm_storeu_ps(padded_in + (y + kern_cent_Y)*padded_x + kern_cent_X + x, _mm_loadu_ps(in + y*data_size_X + x));
        }
        for ( ; x < data_size_X; x++) {
            padded_in[(y + kern_cent_Y)*padded_x + kern_cent_X + x] = in[y*data_size_X + x];
        }
        // Pad right:
        for (x = data_size_X + kern_cent_X; x < padded_x; x++) {
            padded_in[(y + kern_cent_Y)*padded_x + x] = 0.0;
        }
    }
    // Pad bottom row(s):
    bound = padded_x*padded_y;
    #pragma omp parallel for firstprivate(padded_x, kern_cent_Y, data_size_Y, bound)
    for (i = padded_x*(kern_cent_Y + data_size_Y); i < bound; i++) {
        padded_in[i] = 0.0;
    }
    // Flip the kernel:
    for (kern_uf = 0; kern_uf < kernel_size; kern_uf++) {
        kern_flipped[kern_uf] = kernel[kernel_size - 1 - kern_uf];
    }

    // Custom convolution loops:
    if (kernel_x == 3) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 3 - 1; i += 3) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 3) + padded_x*(y + (i/3)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 3) + padded_x*(y + (i/3))];
                }
            }
        }
    }
    else if (kernel_x == 5) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 5 - 1; i += 5) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 5) + padded_x*(y + (i/5)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 5) + padded_x*(y + (i/5))];
                }
            }
        }
    }
    else if (kernel_x == 7) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 7 - 1; i += 7) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + 5) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + 6) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 7) + padded_x*(y + (i/7)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 7) + padded_x*(y + (i/7))];
                }
            }
        }
    }
    else if (kernel_x == 9) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 9 - 1; i += 9) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + 5) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + 6) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 7)), _mm_loadu_ps(padded_in + (x + 7) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 8)), _mm_loadu_ps(padded_in + (x + 8) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 9) + padded_x*(y + (i/9)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 9) + padded_x*(y + (i/9))];
                }
            }
        }
    }
    else if (kernel_x == 11) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 11 - 1; i += 11) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + 5) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + 6) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 7)), _mm_loadu_ps(padded_in + (x + 7) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 8)), _mm_loadu_ps(padded_in + (x + 8) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 9)), _mm_loadu_ps(padded_in + (x + 9) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 10)), _mm_loadu_ps(padded_in + (x + 10) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 11) + padded_x*(y + (i/11)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 11) + padded_x*(y + (i/11))];
                }
            }
        }
    }
    else if (kernel_x == 13) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 13 - 1; i += 13) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + 5) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + 6) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 7)), _mm_loadu_ps(padded_in + (x + 7) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 8)), _mm_loadu_ps(padded_in + (x + 8) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 9)), _mm_loadu_ps(padded_in + (x + 9) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 10)), _mm_loadu_ps(padded_in + (x + 10) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 11)), _mm_loadu_ps(padded_in + (x + 11) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 12)), _mm_loadu_ps(padded_in + (x + 12) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 13) + padded_x*(y + (i/13)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 13) + padded_x*(y + (i/13))];
                }
            }
        }
    }
    else if (kernel_x == 15) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 15 - 1; i += 15) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + 5) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + 6) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 7)), _mm_loadu_ps(padded_in + (x + 7) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 8)), _mm_loadu_ps(padded_in + (x + 8) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 9)), _mm_loadu_ps(padded_in + (x + 9) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 10)), _mm_loadu_ps(padded_in + (x + 10) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 11)), _mm_loadu_ps(padded_in + (x + 11) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 12)), _mm_loadu_ps(padded_in + (x + 12) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 13)), _mm_loadu_ps(padded_in + (x + 13) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 14)), _mm_loadu_ps(padded_in + (x + 14) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 15) + padded_x*(y + (i/15)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 15) + padded_x*(y + (i/15))];
                }
            }
        }
    }
    else if (kernel_x == 17) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 17 - 1; i += 17) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + 5) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + 6) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 7)), _mm_loadu_ps(padded_in + (x + 7) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 8)), _mm_loadu_ps(padded_in + (x + 8) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 9)), _mm_loadu_ps(padded_in + (x + 9) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 10)), _mm_loadu_ps(padded_in + (x + 10) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 11)), _mm_loadu_ps(padded_in + (x + 11) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 12)), _mm_loadu_ps(padded_in + (x + 12) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 13)), _mm_loadu_ps(padded_in + (x + 13) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 14)), _mm_loadu_ps(padded_in + (x + 14) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 15)), _mm_loadu_ps(padded_in + (x + 15) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 16)), _mm_loadu_ps(padded_in + (x + 16) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 17) + padded_x*(y + (i/17)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 17) + padded_x*(y + (i/17))];
                }
            }
        }
    }
    else if (kernel_x == 19) {
        #pragma omp parallel for firstprivate(i, x, copy_kernel, kern_local, kern_flipped, result, out, padded_in, data_size_Y, data_size_X, kernel_size, padded_x)
        for (y = 0; y < data_size_Y; y++) {
            if (copy_kernel) {
                memcpy(kern_local, (const float*) kern_flipped, sizeof(float)*kernel_size);
                copy_kernel = 0;
            }
            for (x = 0; x < data_size_X - 3; x += 4) {
                result = _mm_setzero_ps();
                int temp = 0;
                for (i = 0; i < kernel_size - 19 - 1; i += 19) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 1)), _mm_loadu_ps(padded_in + (x + 1) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 2)), _mm_loadu_ps(padded_in + (x + 2) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 3)), _mm_loadu_ps(padded_in + (x + 3) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 4)), _mm_loadu_ps(padded_in + (x + 4) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 5)), _mm_loadu_ps(padded_in + (x + 5) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 6)), _mm_loadu_ps(padded_in + (x + 6) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 7)), _mm_loadu_ps(padded_in + (x + 7) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 8)), _mm_loadu_ps(padded_in + (x + 8) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 9)), _mm_loadu_ps(padded_in + (x + 9) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 10)), _mm_loadu_ps(padded_in + (x + 10) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 11)), _mm_loadu_ps(padded_in + (x + 11) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 12)), _mm_loadu_ps(padded_in + (x + 12) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 13)), _mm_loadu_ps(padded_in + (x + 13) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 14)), _mm_loadu_ps(padded_in + (x + 14) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 15)), _mm_loadu_ps(padded_in + (x + 15) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 16)), _mm_loadu_ps(padded_in + (x + 16) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 17)), _mm_loadu_ps(padded_in + (x + 17) + padded_x*(y + temp))));
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + (i + 18)), _mm_loadu_ps(padded_in + (x + 18) + padded_x*(y + temp))));
                    temp++;
                }
                for ( ; i < kernel_size; i++) {
                    result = _mm_add_ps(result, _mm_mul_ps(_mm_load1_ps(kern_local + i), _mm_loadu_ps(padded_in + (x + i % 19) + padded_x*(y + (i/19)))));
                }
                _mm_storeu_ps(out + (x + y*data_size_X), result);
            }
            for ( ; x < data_size_X; x++) {
                for (i = 0; i < kernel_size; i++) {
                        out[x + y*data_size_X] += 
                            kern_local[i]*padded_in[(x + i % 19) + padded_x*(y + (i/19))];
                }
            }
        }
    }
    else {
        CONV(in, out, data_size_X, data_size_Y, kernel, kernel_x, kernel_y);
    }

    free(padded_in);
    return 1;
}
