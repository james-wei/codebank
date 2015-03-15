#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "jpg.h"

#define LEN_TIFF_ENDIANNESS 2
#define LEN_TIFF_MAGIC_STR 2
#define LEN_TIFF_OFFSET 4
#define LEN_NUM_STRUCTS 2

static int valid = 1;
static size_t exif_offset;
static int exif_found = 0;

int analyze_tiff(FILE *f, size_t len) {

    size_t result, counter, num_bytes_read, offset;

    unsigned char *buffer, 
                  *tiff_header;

    if (f == NULL) { /* file error */
        return -1;
    }

    int file_result;
    fpos_t beg_pos;
    file_result = fgetpos(f, &beg_pos);
    if (file_result != 0) { /* file error */
        return -1;
    }

    buffer = (unsigned char*) malloc(sizeof(unsigned char)* (LEN_TIFF_ENDIANNESS + LEN_TIFF_MAGIC_STR + LEN_TIFF_OFFSET));
    
    if (buffer == NULL) { /* memory error*/
        return 1;
    }

    if (len < 8) { /* invalid file */
        free(buffer);
        return -1;
    }

    result = fread(buffer, 1, LEN_TIFF_ENDIANNESS + LEN_TIFF_MAGIC_STR + LEN_TIFF_OFFSET, f);
    if (result != LEN_TIFF_ENDIANNESS + LEN_TIFF_MAGIC_STR + LEN_TIFF_OFFSET) { /* file error */
        free(buffer);
        return -1;
    }

    /* Construct TIFF header (endinaness and magic string) */
    tiff_header = (unsigned char*) malloc(sizeof(unsigned char)*(LEN_TIFF_ENDIANNESS + LEN_TIFF_MAGIC_STR));
    if (tiff_header == NULL) { /* memory error */
        free(buffer);
        return 1;
    }
    *(tiff_header) = '\x49';
    *(tiff_header + 1) = '\x49';
    *(tiff_header + 2) = '\x2a';
    *(tiff_header + 3) = '\x00';

    /* Check for valid TIFF header */
    counter = 0;
    while (counter < LEN_TIFF_ENDIANNESS + LEN_TIFF_MAGIC_STR) {
        if (*(buffer + counter) != *(tiff_header + counter)) { /* not a PNG */
            free(buffer);
            free(tiff_header);
            return -1;
        }
        counter++;
    }
    free(tiff_header);

    /* Get offset */
    offset = 0;
    offset += (((size_t) buffer[4]));
    offset += (((size_t) buffer[5]) << 8);
    offset += (((size_t) buffer[6]) << 16);
    offset += (((size_t) buffer[7]) << 24);
    free(buffer);

    if (offset < LEN_TIFF_ENDIANNESS + LEN_TIFF_MAGIC_STR + LEN_TIFF_OFFSET) { /* file error */
        return -1;
    }

    if (offset >= len) { /* file error */
        return -1;
    }

    num_bytes_read = LEN_TIFF_ENDIANNESS + LEN_TIFF_MAGIC_STR + LEN_TIFF_OFFSET;
    
    offset = offset - 8;
    result = fseek(f, offset, SEEK_CUR); /* Seek to 0th IFD */
    if (result != 0) { /* error */
        return -1;
    }
    num_bytes_read += offset - 8;

    if (num_bytes_read >= len - 2) { /* invalid file */
        return -1;
    }

    buffer = (unsigned char*) malloc(sizeof(unsigned char)*2);
    if (buffer == NULL) { /* memory error */
        free(buffer);
        return 1;
    }

    result = fread(buffer, sizeof(char), 2, f);
    if (result != 2) { /* file error */
        free(buffer);
        return -1;
    }
    num_bytes_read += result;

    size_t num_structs = 0;
    num_structs += (((size_t) buffer[0]));
    num_structs += (((size_t) buffer[1]) << 8);
    
    if (num_structs == 0) { /* no structs */
        free(buffer);
        return -1;
    }

    free(buffer);
    
    size_t num_structs_read = 0;
    size_t tagid;
    size_t datatype;
    size_t count;
    size_t offset_or_value;
    size_t size;
    int is_value;
    int print;
    unsigned char* value;
    fpos_t curr_pos;

    /* Start first loop */
    while (num_structs_read < num_structs) {
        if (num_bytes_read >= len - 12) { /* invalid */
            return -1;
        }
        buffer = (unsigned char*) malloc(sizeof(unsigned char)*12);
        if (buffer == NULL) { /* no memory */
            free(buffer);
            return 1;
        }
        result = fread(buffer, 1, 12, f);
        if (result != 12) {
            free(buffer);
            return -1;
        }
        num_bytes_read += 12;

        tagid = ((size_t)buffer[0]) | (((size_t)buffer[1])<<8);
        datatype = ((size_t)buffer[2]) | (((size_t)buffer[3])<<8);
        count = ((size_t)buffer[4]) | (((size_t)buffer[5])<<8) | (((size_t)buffer[6])<<16) | (((size_t)buffer[7])<<24);
        offset_or_value = ((size_t)buffer[8]) | (((size_t)buffer[9])<<8) | (((size_t)buffer[10])<<16) | (((size_t)buffer[11])<<24);
        size = num_bytes(datatype, count);

        if (size > len) { /* file error */
            free(buffer);
            return -1;
        }

        if (valid == 0) { /* invalid data type*/
            free(buffer);
            return -1;
        }
        if (size <= 4) {
            is_value = 1;
        }
        else {
            is_value = 0;
        }

        if (is_value) {
            value = (unsigned char*) malloc(4*sizeof(unsigned char));
            if (value == NULL) {
                free(buffer);
                free(value);
                return -1;
            }
            value[0] = buffer[8];
            value[1] = buffer[9];
            value[2] = buffer[10];
            value[3] = buffer[11];
            print = should_print(tagid, datatype, count);
            if (print == 1) {
                print_values(tagid, datatype, count, value);
            }
            if (print == 2) {
                if (exif_found == 1) {
                    free(buffer);
                    free(value);
                    return -1;
                }
                exif_found = 1;
                exif_offset = offset_or_value;
            }
            num_structs_read += 1;
            free(buffer);
            free(value);
        } else {
            free(buffer);
            if (offset_or_value + size > len) { /* outside of the tiff file */
                return -1;
            }

            print = should_print(tagid, datatype, count);
            if (print == 1) {
                file_result = fgetpos(f, &curr_pos);
                if (file_result != 0) { /* error */
                    return -1;
                }

                file_result = fsetpos(f, &beg_pos);
                if (file_result != 0) { /* error */
                    return -1;
                }

                result = fseek(f, offset_or_value, SEEK_CUR);
                if (result != 0) { /* error */
                    return -1;
                }

                buffer = (unsigned char*) malloc(sizeof(unsigned char)*size);
                if (buffer == NULL) { /* memory error */
                    free(buffer);
                    return 1;
                }

                result = fread(buffer, 1, size, f);
                if (result != size) { /* file error */
                    free(buffer);
                    return -1;
                }
                print_offset(tagid, datatype, size, buffer);
                num_structs_read += 1;

                file_result = fsetpos(f, &curr_pos);
                if (file_result != 0) { /* error */
                    free(buffer);
                    return 0;
                }
                free(buffer);
            }
            else if (print == 0) {
                num_structs_read += 1;
            }
            else { /* error */
                return -1;
            }
        }
    }
    /* End first loop*/

    if (exif_found == 0) {
        return 0;
    }

    if (exif_offset < num_bytes_read) { /* file error */
        return -1;
    }

    if (exif_offset >= len) { /* file error */
        return -1;
    }
    
    file_result = fsetpos(f, &beg_pos);
    if (file_result != 0) { /* error */
        return -1;
    }
    result = fseek(f, exif_offset, SEEK_CUR);
    if (result != 0) { /* error */
        return -1;
    }

    num_bytes_read = exif_offset;

    if (num_bytes_read >= len - 2) { /* invalid file */
        return -1;
    }

    buffer = (unsigned char*) malloc(sizeof(unsigned char)*2);
    if (buffer == NULL) { /* memory error */
        free(buffer);
        return 1;
    }

    result = fread(buffer, sizeof(char), 2, f);
    if (result != 2) { /* file error */
        free(buffer);
        return -1;
    }
    num_bytes_read += result;

    num_structs = 0;
    num_structs += (((size_t) buffer[0]));
    num_structs += (((size_t) buffer[1]) << 8);
    
    if (num_structs == 0) { /* no structs */
        free(buffer);
        return -1;
    }

    free(buffer);
    num_structs_read = 0;

    /* Start second loop */
    while (num_structs_read < num_structs) {
        if (num_bytes_read >= len - 12) { /* invalid */
            return -1;
        }
        buffer = (unsigned char*) malloc(sizeof(unsigned char)*12);
        if (buffer == NULL) { /* no memory */
            free(buffer);
            return 1;
        }
        result = fread(buffer, 1, 12, f);
        if (result != 12) {
            free(buffer);
            return -1;
        }
        num_bytes_read += 12;

        tagid = ((size_t)buffer[0]) | (((size_t)buffer[1])<<8);
        datatype = ((size_t)buffer[2]) | (((size_t)buffer[3])<<8);
        count = ((size_t)buffer[4]) | (((size_t)buffer[5])<<8) | (((size_t)buffer[6])<<16) | (((size_t)buffer[7])<<24);
        offset_or_value = ((size_t)buffer[8]) | (((size_t)buffer[9])<<8) | (((size_t)buffer[10])<<16) | (((size_t)buffer[11])<<24);
        size = num_bytes(datatype, count);

        if (size > len) { /* file error */
            free(buffer);
            return -1;
        }

        if (valid == 0) { /* invalid data type*/
            free(buffer);
            return -1;
        }
        if (size <= 4) {
            is_value = 1;
        }
        else {
            is_value = 0;
        }

        if (is_value) {
            value = (unsigned char*) malloc(4*sizeof(unsigned char));
            if (value == NULL) {
                free(buffer);
                free(value);
                return -1;
            }
            value[0] = buffer[8];
            value[1] = buffer[9];
            value[2] = buffer[10];
            value[3] = buffer[11];
            print = should_print(tagid, datatype, count);
            if (print == 1) {
                print_values(tagid, datatype, count, value);
            }
            if (print == 2) {
                free(buffer);
                free(value);
                return -1;
            }
            num_structs_read += 1;
            free(buffer);
            free(value);
        } else {
            free(buffer);
            if (offset_or_value + size > len) { /* outside of the tiff file */
                return -1;
            }

            print = should_print(tagid, datatype, count);
            if (print == 1) {
                file_result = fgetpos(f, &curr_pos);
                if (file_result != 0) { /* error */
                    return -1;
                }

                file_result = fsetpos(f, &beg_pos);
                if (file_result != 0) { /* error */
                    return -1;
                }

                result = fseek(f, offset_or_value, SEEK_CUR);
                if (result != 0) { /* error */
                    return -1;
                }

                buffer = (unsigned char*) malloc(sizeof(unsigned char)*size);
                if (buffer == NULL) { /* memory error */
                    free(buffer);
                    return 1;
                }

                result = fread(buffer, 1, size, f);
                if (result != size) { /* file error */
                    free(buffer);
                    return -1;
                }
                print_offset(tagid, datatype, size, buffer);
                num_structs_read += 1;

                file_result = fsetpos(f, &curr_pos);
                if (file_result != 0) { /* error */
                    free(buffer);
                    return 0;
                }
                free(buffer);
            }
            else if (print == 0) {
                num_structs_read += 1;
            }
            else { /* error */
                return -1;
            }
        }
    }
    /* End second loop */
    return 0;
}

int print_offset(size_t tagid, size_t datatype, size_t size, unsigned char* val) {
    if (tagid == 0x010D) {
        if (size == 0) {
            printf("DocumentName:\n");
            return 0;
        }   
        if (val[size-1] != 0x00){
            return -1;
        }
        printf("DocumentName: %s\n", val);
    }
    else if (tagid == 0x010E) {
        if (size == 0) {
            printf("ImageDescription:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("ImageDescription: %s\n", val);
    }
    else if (tagid == 0x010F) {
        if (size == 0) {
            printf("Make:\n");
            return 0;
        } 
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("Make: %s\n", val);
    }
    else if (tagid == 0x0110) { 
        if (size == 0) {
            printf("Model:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("Model: %s\n", val);
    }
    else if (tagid == 0x0131) { 
        if (size == 0) {
            printf("Software:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("Software: %s\n", val);
    }
    else if (tagid == 0x0132) {
        if (size == 0) {
            printf("DateTime:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("DateTime: %s\n", val);
    }
    else if (tagid == 0x013B) {
        if (size == 0) {
            printf("Artist:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("Artist: %s\n", val);
    }
    else if (tagid == 0x013C) {
        if (size == 0) {
            printf("HostComputer:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("HostComputer: %s\n", val);
    }
    else if (tagid == 0x8298) { 
        if (size == 0) {
            printf("Copyright:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("Copyright: %s\n", val);
    }
    else if (tagid == 0xA004) {
        if (size == 0) {
            printf("RelatedSoundFile:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("RelatedSoundFile: %s\n", val);
    }
    else if (tagid == 0x9003) {
        if (size == 0) {
            printf("DateTimeOriginal:\n");
            return 0;
        } 
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("DateTimeOriginal: %s\n", val);
    }
    else if (tagid == 0x9004) {
        if (size == 0) {
            printf("DateTimeDigitized:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("DateTimeDigitized: %s\n", val);
    }
    else if (tagid == 0x927C) {
        unsigned char* buf = (unsigned char*)malloc(sizeof(unsigned char)*(size+1));
        if (buf == NULL) { /* memory error */
            return -1;
        }
        memmove(buf, val, size);
        buf[size] = '\0';
        printf("MakerNote: %s\n", buf);
        free(buf);
    }
    else if (tagid == 0x9286) {
        if (size < 8) { /* no character set */
            return -1;
        }
        if (val[0] != 0x41) {
            return -1;
        }
        if (val[1] != 0x53) {
            return -1;
        }
        if (val[2] != 0x43) {
            return -1;
        }
        if (val[3] != 0x49) {
            return -1;
        }
        if (val[4] != 0x49) {
            return -1;
        }
        if (val[5] != 0x00) {
            return -1;
        }   
        if (val[6] != 0x00) {
            return -1;
        }
        if (val[7] != 0x00) {
            return -1;
        }
        if (size == 8) {
            printf("UserComment:\n");
            return 1;
        }
        val = val + 8;
        unsigned char* buf = (unsigned char*)malloc(sizeof(unsigned char)*(size-8+1));
        if (buf == NULL) { /* memory error */
            return -1;
        }
        memmove(buf, val, size-8);
        buf[size-8] = '\0';
        printf("UserComment: %s\n", buf);
        free(buf);
    }
    else if (tagid == 0xA420) {
        if (size == 0) {
            printf("ImageUniqueID:\n");
            return 0;
        }
        if (val[size-1] != 0x00) {
            return -1;
        }
        printf("ImageUniqueID: %s\n", val);
    }
    else {
        return 0;
    }
    return 1;
}

int print_values(size_t tagid, size_t datatype, size_t count, unsigned char* val) {
    if (tagid == 0x010D) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("DocumentName: %s\n", val);
    }
    else if (tagid == 0x010E) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("ImageDescription: %s\n", val);
    }
    else if (tagid == 0x010F) {  
        if (val[3] != 0x00) {
            return -1;
        }
        printf("Make: %s\n", val);
    }
    else if (tagid == 0x0110) { 
        if (val[3] != 0x00) {
            return -1;
        }
        printf("Model: %s\n", val);
    }
    else if (tagid == 0x0131) { 
        if (val[3] != 0x00) {
            return -1;
        }
        printf("Software: %s\n", val);
    }
    else if (tagid == 0x0132) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("DateTime: %s\n", val);
    }
    else if (tagid == 0x013B) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("Artist: %s\n", val);
    }
    else if (tagid == 0x013C) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("HostComputer: %s\n", val);
    }
    else if (tagid == 0x8298) { 
        if (val[3] != 0x00) {
            return -1;
        }
        printf("Copyright: %s\n", val);
    }
    else if (tagid == 0xA004) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("RelatedSoundFile: %s\n", val);
    }
    else if (tagid == 0x9003) { 
        if (val[3] != 0x00) {
            return -1;
        }
        printf("DateTimeOriginal: %s\n", val);
    }
    else if (tagid == 0x9004) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("DateTimeDigitized: %s\n", val);
    }
    else if (tagid == 0x927C) {
        if (count < 4 && val[3] == 0x00) {
            printf("MakerNote: %s\n", val);
        } else if (count == 4) {
            printf("MakerNote: %c%c%c%c\n", val[0], val[1], val[2], val[3]);
        } else {
            return -1;
        }
    }
    else if (tagid == 0x9286) {
        // Do nothing
        return -1;
    }
    else if (tagid == 0xA420) {
        if (val[3] != 0x00) {
            return -1;
        }
        printf("ImageUniqueID: %s\n", val);
    }
    else {
        return 0;
    }
    return 1;
}

int should_print(size_t tagid, size_t datatype, size_t count) {
    if (tagid == 0x010D) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
    }
    else if (tagid == 0x010E) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }

    }
    else if (tagid == 0x010F) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x0110) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x0131) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x0132) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x013B) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x013C) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x8298) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0xA004) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x9003) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x9004) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x927C) {
        if (datatype != 0x0007) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x9286) {
        if (datatype != 0x0007) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0xA420) {
        if (datatype != 0x0002) {
            return -1;
        } else {
            return 1;
        }
        
    }
    else if (tagid == 0x8769) {
        if (datatype != 0x0004 || count != 1) {
            return -1;
        }
        else {
            return 2;
        }
    }
    else {
        return 0;
    }
}

size_t num_bytes (size_t datatype, size_t count) {
    size_t size;
    if (datatype == 0x0001) {
        size = 1;
    }
    else if (datatype == 0x0002) {
        return count;
    }
    else if (datatype == 0x0003) {
        size = 2;
    }
    else if (datatype == 0x0004) {
        size = 4;
    }
    else if (datatype == 0x0005) {
        size = 8;
    }
    else if (datatype == 0x0007) {
        size = 1;
    }
    else if (datatype == 0x0008) {
        size = 2;
    }
    else if (datatype == 0x0009) {
        size = 4;
    }
    else if (datatype == 0x000a) {
        size = 8;
    }
    else if (datatype == 0x000b) {
        size = 4;
    }
    else if (datatype == 0x000c) {
        size = 8;
    }
    else {
        size = 0;
        valid = 0;
    }
    return size*count;
}

/*
 * Analyze a JPG file that contains Exif data.
 * If it is a JPG file, print out all relevant metadata and return 0.
 * If it isn't a JPG file, return -1 and print nothing.
 */
int analyze_jpg(FILE *f) {
    /* YOU WRITE THIS PART */
    if (f == NULL) { /* file error */
        return -1;
    }
    fseek(f, 0, SEEK_END);
    size_t file_size = ftell(f);
    rewind(f);

    unsigned char* buffer = (unsigned char*) malloc(sizeof(unsigned char)*2);
    if (buffer == NULL) { /* memory error */
        free(buffer);
        return 1;
    }

    size_t result = fread(buffer, 1, 2, f);
    if (result < 2) { /* not a valid jpg */
        return -1;
    }
    if (buffer[0] != 0xFF || buffer[1] != 0xD8) { /* not a valid jpg */
        return -1;
    }
    size_t num_bytes_read = 2;
    free(buffer);

    size_t length;
    int file_result;
    while (num_bytes_read < file_size) {
        buffer = (unsigned char*) malloc(sizeof(unsigned char)*2);
        if (buffer == NULL) { /* memory error */
            free(buffer);
            return 1;
        }

        result = fread(buffer, 1, 2, f);
        if (result == 0) { /* reached end of file */
            free(buffer);
            return 0;
        }
        if (result < 2) { /* invalid */
            free(buffer);
            return -1;
        }

        num_bytes_read += 2;

        if (buffer[0] != 0xFF) { /* invalid */
            free(buffer);
            return -1;
        }
        if (buffer[1] == 0xFF) { /* invalid */
            free(buffer);
            return -1;
        }

        if (buffer[1] == 0xD9) { /* end of file */
            free(buffer);
            return 0;
        }

        if (buffer[1] == 0xD8) { /* invalid file */
            free(buffer);
            return -1;
        }

        if (buffer[1] == 0x00) { /* invalid file */
            free(buffer);
            return 0;
        }
        if ((buffer[1] < 0xD0 || buffer[1] > 0xDA) && buffer[1] != 0xE1) { /* standard chunk */
            result = fread(buffer, 1, 2, f);
            if (result < 2) { /* invalid file */
                free(buffer);
                return -1;
            }

            length = 0;
            length = length + (((size_t)buffer[0]) << 8);
            length = length + ((size_t)buffer[1]);
            if (length > 1) {
                length -= 2;
            }

            if (length > file_size) { /* file error */
                free(buffer);
                return 1;
            }

            num_bytes_read += 2;
            buffer = (unsigned char*) realloc(buffer, sizeof(unsigned char)*length);
            if (buffer == NULL) { /* memory error */
                free(buffer);
                return 1;
            }

            result = fread(buffer, 1, length, f);
            if (result < length) { /* file error */
                free(buffer);
                return 0;
            }
            num_bytes_read += length;
        }
        else if (buffer[1] == 0xE1) { /* APP1 */
            result = fread(buffer, 1, 2, f);
            if (result < 2) { /* invalid file */
                free(buffer);
                return -1;
            }

            length = 0;
            length = length + (((size_t)buffer[0]) << 8);
            length = length + ((size_t)buffer[1]);
            if (length > file_size) { /* file error */
                free(buffer);
                return 1;
            }
            if (length > 7) {
                length -= 8;
            }
            else { /* invalid file */
                free(buffer);
                return 0;
            }

            buffer = (unsigned char*) realloc(buffer, sizeof(unsigned char)*8);
            result = fread(buffer, 1, 6, f);
            if (result < 6) { /* invalid file */
                free(buffer);
                return 0;
            }

            unsigned char* pre_header = (unsigned char*) malloc(sizeof(unsigned char)*6);
            if (pre_header == NULL) { /* memory error */
                free(buffer);
                free(pre_header);
                return 1;
            }

            *pre_header = '\x45';
            *(pre_header+1) = '\x78';
            *(pre_header+2) = '\x69';
            *(pre_header+3) = '\x66';
            *(pre_header+4) = '\x00';
            *(pre_header+5) = '\x00';

            int counter = 0;
            while (counter < 6) { /* invalid file */
                if (buffer[counter] != pre_header[counter]) {
                    free(buffer);
                    free(pre_header);
                    return 0;
                }
                counter += 1;
            }

            free(buffer);
            free(pre_header);
            analyze_tiff(f, length);
            return 0;
        }
        else { /* super chunk */
            free(buffer);
            fpos_t curr_pos;
            file_result = fgetpos(f, &curr_pos);
            if (file_result != 0) { /* error */
                return 0;
            }
            unsigned char* curr_char = (unsigned char*)malloc(sizeof(unsigned char)*1);
            if (curr_char == NULL) { /* out of memory */
                return 0;
            }

            unsigned char* next_char = (unsigned char*)malloc(sizeof(unsigned char)*1);
            if (next_char == NULL) { /* out of memory */
                return 0;
            }
            while (num_bytes_read < file_size) {
                file_result = fgetpos(f, &curr_pos);
                if (file_result != 0) { /* error */
                    return 0;
                }

                result = fread(curr_char, 1, 1, f);
                if (result != 1) {
                    free(curr_char);
                    free(next_char);
                    return -1;
                }
                num_bytes_read += 1;

                if (*curr_char == 0xFF) {
                    result = fread(next_char, 1, 1, f);
                    if (result != 1) {
                        free(curr_char);
                        free(next_char);
                        return -1;
                    }
                    num_bytes_read += 1;
                    if (*next_char != 0x00) { /* found the next chunk */
                        file_result = fsetpos(f, &curr_pos);
                        if (file_result != 0) { /* error */
                            free(curr_char);
                            free(next_char);
                            return 0;
                        }
                        num_bytes_read -= 2;
                        break;
                    }
                }
            }
            free(curr_char);
            free(next_char);
        }
    }    
    return 0;
}
