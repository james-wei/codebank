#ifndef EXIF_H_GUARD
#define EXIF_H_GUARD

int analyze_tiff(FILE *f, size_t len);
int print_offset(size_t tagid, size_t datatype, size_t size, unsigned char* val);
int print_values(size_t tagid, size_t datatype, size_t count, unsigned char* val);
int should_print(size_t tagid, size_t datatype, size_t count);
size_t num_bytes (size_t datatype, size_t count);
int analyze_jpg(FILE *f);

#endif
