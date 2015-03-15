#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <zlib.h>
#include "png.h"

#define LEN_PNG_HEADER 8    /* size of the header in bytes */
#define LEN_CHUNK_LENGTH 4  /* size of the length field in bytes */
#define LEN_CHUNK_TYPE 4    /* size of the chunk type field in bytes */
#define LEN_CHECKSUM 4      /* size of the checksum field in bytes */

static int valid = 1;       /* File validity flag */

/*
 * Analyze a PNG file.
 * If it is a PNG file, print out all relevant metadata and return 0.
 * If it isn't a PNG file, return -1 and print nothing.
 */
int analyze_png(FILE *f) {

    /* Variable declarations */
    size_t file_size, 
           result, 
           counter, 
           num_bytes_read,
           length, 
           chunk_type;

    unsigned char *buffer, 
                  *png_header,
                  *len_buf,
                  *type_buf,
                  *data_buf,
                  *chksum_buf;

    if (f == NULL) { /* file error */
        return -1;
    }

    buffer = (unsigned char*) malloc(sizeof(unsigned char)*LEN_PNG_HEADER);
    
    if (buffer == NULL) { /* memory error*/
        return 1;
    }

    /* Find file size */
    fseek(f, 0, SEEK_END);
    file_size = ftell(f);
    rewind(f);

    if (file_size < LEN_PNG_HEADER) {
        free(buffer);
        return -1;
    }

    result = fread(buffer, 1, LEN_PNG_HEADER, f);
    if (result != LEN_PNG_HEADER) { /* file error */
        free(buffer);
        return -1;
    }

    /* Construct PNG header */
    png_header = (unsigned char*) malloc(sizeof(unsigned char)*8);
    if (png_header == NULL) { /* memory error */
        free(buffer);
        return 1;
    }
    *(png_header) = '\x89';
    *(png_header + 1) = '\x50';
    *(png_header + 2) = '\x4e';
    *(png_header + 3) = '\x47';
    *(png_header + 4) = '\x0d';
    *(png_header + 5) = '\x0a';
    *(png_header + 6) = '\x1a';
    *(png_header + 7) = '\x0a';

    /* Check for valid PNG header */
    counter = 0;
    while (counter < 8) {
        if (*(buffer + counter) != *(png_header + counter)) { /* not a PNG */
            free(buffer);
            free(png_header);
            return -1;
        }
        counter++;
    }

    free(png_header);
    free(buffer);

    num_bytes_read = LEN_PNG_HEADER;

    /* MAIN PARSE LOOP */
    while (num_bytes_read < file_size) {
        len_buf = (unsigned char*) malloc(sizeof(unsigned char)*(LEN_CHUNK_LENGTH));
        if (len_buf == NULL) { /* memory error */
            free(len_buf);
            return 1;
        }
        type_buf = (unsigned char*) malloc(sizeof(unsigned char)*(LEN_CHUNK_TYPE));
        if (type_buf == NULL) { /* memory error */
            free(len_buf);
            free(type_buf);
            return 1;
        }
        result = fread(len_buf, 1, LEN_CHUNK_LENGTH, f);
        if (result < LEN_CHUNK_LENGTH) { /* file error */
            free(len_buf);
            free(type_buf);
            return 1;
        }
        result = fread(type_buf, 1, LEN_CHUNK_TYPE, f);
        if (result < LEN_CHUNK_TYPE) { /* file error */
            free(len_buf);
            free(type_buf);
            return 1;
        }
        length = get_length(len_buf);
        if (valid != 1) { /* file error */
            free(len_buf);
            free(type_buf);
            return 1;
        }

        chunk_type = get_type(type_buf);
        if (valid != 1) { /* file error */
            free(len_buf);
            free(type_buf);
            return 1;
        }

        num_bytes_read += (LEN_CHUNK_LENGTH + LEN_CHUNK_TYPE);

        if (length > file_size - LEN_PNG_HEADER - LEN_CHUNK_LENGTH - LEN_CHUNK_TYPE - LEN_CHECKSUM) { /* file error */
            free(len_buf);
            free(type_buf);
            return 1;
        }

        /* Get data and checksum values */
        data_buf = (unsigned char*) malloc(sizeof(unsigned char)*(length));
        if (data_buf == NULL) { /* memory error */
            free(len_buf);
            free(type_buf);
            free(data_buf);
            return 1;
        }
        chksum_buf = (unsigned char*) malloc(sizeof(unsigned char)*(LEN_CHUNK_TYPE));
        if (chksum_buf == NULL) { /* memory error */
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            return 1;
        }
        result = fread(data_buf, 1, length, f);
        if (result < length) { /* file error */
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            return 1;
        }
        result = fread(chksum_buf, 1, LEN_CHECKSUM, f);
        if (result < LEN_CHECKSUM) { /* file error */
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            return 1;
        }

        num_bytes_read += (length + LEN_CHECKSUM);

        if (!pass_checksum(type_buf, data_buf, chksum_buf, length)) { /* file error */
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            return -1;
        }

        if (chunk_type == 4) { /* Not a text chunk */
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            if (valid != 1) {
                return -1;
            }
        }
        else if (chunk_type == 1) {
            handle_text_chunk(data_buf, length);
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            if (valid != 1) {
                return -1;
            }
        }
        else if (chunk_type == 2) {
            handle_ztxt_chunk(data_buf, length);
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            if (valid != 1) {
                return -1;
            }
        }
        else if (chunk_type == 3) {
            if (length != 7) { /* file error */
                free(len_buf);
                free(type_buf);
                free(data_buf);
                free(chksum_buf);
                return 1;
            }
            handle_time_chunk(data_buf, length);
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
        }
        else { /* File error */ 
            free(len_buf);
            free(type_buf);
            free(data_buf);
            free(chksum_buf);
            return 1;
        }
    }
    return 0;
}

size_t get_length(unsigned char* buf) {
    size_t length;
    if (buf == NULL) {
        valid = 0;
        return 0;
    }
    length = 0;
    length = length + (((size_t) buf[0]) << 24);
    length = length + (((size_t) buf[1]) << 16);
    length = length + (((size_t) buf[2]) << 8);
    length = length + ((size_t) buf[3]);
    return length;
}

size_t get_type(unsigned char* buf) {
    /* Return value key:
     *  0 --> error
     *  1 --> tEXt chunk
     *  2 --> zTXt chunk
     *  3 --> tIME chunk
     *  4 --> not a text chunk
     */
    if (buf == NULL) {
        valid = 0;
        return 0;
    }
    if (buf[0] == 0x74 && buf[1] == 0x45 && buf[2] == 0x58 && buf[3] == 0x74) {
        return 1;
    }
    else if (buf[0] == 0x7A && buf[1] == 0x54 && buf[2] == 0x58 && buf[3] == 0x74) {
        return 2;
    }
    else if (buf[0] == 0x74 && buf[1] == 0x49 && buf[2] == 0x4D && buf[3] == 0x45) {
        return 3;
    }
    else {
        return 4;
    }
}


int pass_checksum(unsigned char* type_buf, unsigned char* data_buf, unsigned char* chksum_buf, size_t data_len) {
    unsigned char* buf;
    unsigned long chksum_calc, chksum;
    buf = (unsigned char*) malloc(sizeof(unsigned char)*(data_len + LEN_CHUNK_TYPE));
    if (buf == NULL) {
        free(buf);
        return 0;
    }
    memmove(buf, type_buf, LEN_CHUNK_TYPE);
    memmove(buf + LEN_CHUNK_TYPE, data_buf, data_len);
    chksum_calc = crc32(0L, buf, (unsigned int) (LEN_CHUNK_TYPE + data_len));
    chksum = ((((unsigned long)chksum_buf[0]) << 24) | (((unsigned long)chksum_buf[1]) << 16) | (((unsigned long)chksum_buf[2]) << 8) | (((unsigned long)chksum_buf[3])));
    free(buf);
    return (chksum == chksum_calc);
}

int handle_text_chunk(unsigned char* buf, size_t len) {
    unsigned char *key, *value;
    size_t num_bytes_read, num;

    key = malloc(sizeof(unsigned char)*len);
    
    if (key == NULL) {
        free(key);
        valid = 0;
        return -1;
    }

    num_bytes_read = 0;
    while (num_bytes_read < len && buf[num_bytes_read] != '\0') {
        *(key + num_bytes_read) = *(buf + num_bytes_read);
        num_bytes_read += 1;
    }

    if (num_bytes_read == len) { /* file error */
        free(key);
        valid = 0;
        return -1;
    }

    if (buf[num_bytes_read] != '\0') { /* file error */
        free(key);
        valid = 0;
        return -1;
    }

    *(key + num_bytes_read) = '\0';
    num_bytes_read += 1;

    if (num_bytes_read == len) { /* empty value */
        printf("%s:", key);
        free(key);
        return 1;
    }

    value = malloc(sizeof(unsigned char)*(len - num_bytes_read + 1));
    
    if (value == NULL) {
        free(key);
        free(value);
        valid = 0;
        return -1;
    }

    num = 0;
    while (num_bytes_read < len) {
        *(value+num) = *(buf+num_bytes_read);
        num_bytes_read += 1;
        num += 1;
    }
    *(value+num) = '\0';
    printf("%s: %s\n", key, value);
    free(key);
    free(value);
    return 0;
}

int handle_ztxt_chunk(unsigned char* buf, size_t len) {
    unsigned char* key = malloc(sizeof(unsigned char)*len);
    if (key == NULL) {
        free(key);
        valid = 0;
        return -1;
    }

    size_t num_bytes_read = 0;
    while (num_bytes_read < len && buf[num_bytes_read] != '\0') {
        *(key+num_bytes_read) = *(buf+num_bytes_read);
        num_bytes_read += 1;
    }

    if (num_bytes_read == len) { /* file error */
        free(key);
        valid = 0;
        return -1;
    }

    if (buf[num_bytes_read] != '\0') { /* file error */
        free(key);
        valid = 0;
        return -1;
    }

    *(key+num_bytes_read) = '\0';
    num_bytes_read += 1;

    if (num_bytes_read == len) { /* file error */
        free(key);
        valid = 0;
        return -1;
    }

    if (buf[num_bytes_read] != '\0') { /* file error */
        free(key);
        valid = 0;
        return -1;
    }

    num_bytes_read += 1;

    if (num_bytes_read == len) { /* empty value */
        printf("%s:", key);
        free(key);
        return 1;
    }

    unsigned char* new_buf = malloc(sizeof(unsigned char)*(len-num_bytes_read));
    if (new_buf == NULL) {
        free(key);
        free(new_buf);
        valid = 0;
        return -1;
    }
    memmove(new_buf, buf+num_bytes_read, len-num_bytes_read);

    int z_result;
    uLongf dest_len = (uLongf)(2*len);
    unsigned char* dest = (unsigned char*) malloc(dest_len*sizeof(unsigned char));
    if (dest == NULL) {
        free(key);
        free(new_buf);
        free(dest);
        valid = 0;
        return -1;
    }

    len = len - num_bytes_read;
    num_bytes_read = 0;
    uLong source_len = (uLong)len;
    z_result = uncompress((Bytef*)dest, &dest_len, (Bytef*) new_buf, source_len);
    while (z_result != Z_OK) {
        if (z_result == Z_MEM_ERROR) { /* memory error */
            free(key);
            free(new_buf);
            free(dest);
            valid = 0;
            return -1;
        }
        else if (z_result == Z_DATA_ERROR) { /* file error */
            free(key);
            free(new_buf);
            free(dest);
            valid = 0;
            return -1;
        }
        dest_len = dest_len * 2;
        dest = (unsigned char*) realloc(dest, dest_len*sizeof(unsigned char));
        if (dest == NULL) {
            free(key);
            free(new_buf);
            free(dest);
            valid = 0;
            return -1;
        }

        z_result = uncompress((Bytef*)dest, &dest_len, (Bytef*) new_buf, source_len);
    }

    
    /* malloc one more for null character */
    unsigned char* value = (unsigned char*) malloc(sizeof(unsigned char)*(dest_len+1));
    if (value == NULL) { /* memory error */
        free(key);
        free(new_buf);
        free(dest);
        free(value);
        valid = 0;
        return -1;
    }
    memmove(value, dest, dest_len);
    *(value + dest_len) = '\0';
    printf("%s: %s\n", key, value);
    free(key);
    free(new_buf);
    free(dest);
    free(value);
    return 0;
}

int handle_time_chunk(unsigned char* buf, size_t len) {
    int year = (((unsigned int) buf[0]) << 8);
    year += ((unsigned int) buf[1]);
    printf("Timestamp: %d/%d/%d %d:%d:%d\n", buf[2], buf[3], year, buf[4], buf[5], buf[6]);
    return 0;
}