#ifndef PNG_H_GUARD
#define PNG_H_GUARD

int analyze_png(FILE *f);
size_t get_length(unsigned char* buf);
size_t get_type(unsigned char* buf);
int pass_checksum(unsigned char* type_buf, unsigned char* data_buf, unsigned char* chksum_buf, size_t data_len);
int handle_text_chunk(unsigned char* buf, size_t len);
int handle_ztxt_chunk(unsigned char* buf, size_t len);
int handle_time_chunk(unsigned char* buf, size_t len);

#endif
