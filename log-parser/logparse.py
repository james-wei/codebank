import argparse
import fnmatch
import os.path
import socket


class OutputNumberException(Exception):
    def __init__(self, value):
        self.value = "invalid output number: " + str(value)
    def __str__(self):
        return repr(self.value)

class LogFileTypeException(Exception):
    def __init__(self, value):
        self.value = "input file " + value + " not of format *.log"
    def __str__(self):
        return repr(self.value)

class InvalidEntryException(Exception):
    def __init__(self, value):
        self.value = "invalid log entry: " + value
    def __str__(self):
        return repr(self.value)


def parse_log(n, file_name="sample.log", verbose=False):
    """Parse a target .log file and output the top n URLs.

    Keyword arguments:
    n -- the number of URLs to output
    file_name -- the path/file name of the target log file_name
    verbose -- write verbose output (boolean)
    """
    # Check input validity:
    if type(n) is not int or n < 0:
        raise OutputNumberException(n)
    if not os.path.isfile(file_name):
        raise FileNotFoundError(file_name)
    if not fnmatch.fnmatch(file_name, "*.log"):
        raise LogFileTypeException(file_name)

    url_count = {}  # Maps URL->frequency
    ip_to_url = {}  # Maps IP addr->URL (reduce reverse IP lookup operations)

    if hasattr(socket, 'setdefaulttimeout'):
        socket.setdefaulttimeout(2)

    # Parse log file:
    with open(file_name, 'r') as f:
        for line in f:
            analyze_log_entry(line, url_count, ip_to_url)
    
    # Write to output file:
    with open(get_output_fn(file_name, verbose), 'a') as o:
        write_entries(n, url_count, o, verbose)

def analyze_log_entry(entry, url_count, ip_to_url):
    """A helper method for parse_log().
    Parse a single line from a .log file for an IP address. Convert
    the IP address into a URL and hash it into url_count.

    Keyword arguments:
    entry -- a line in the log file
    url_count -- dictionary mapping URL to frequency
    ip_to_url -- dictionary mapping IP to URL
    """
    # Check line validity (allow empty/blank lines):
    if entry == "" or entry == "\n":
        return
    split_entry = entry.split()
    if len(split_entry) < 4:
        raise InvalidEntryException(entry)
    ip_addr = split_entry[3]
    url = ""
    try:
        socket.inet_aton(ip_addr)
    except socket.error as e:
        raise InvalidEntryException(entry)
    
    # Find URL corresponding to given IP address:
    if ip_addr in ip_to_url:
        url = ip_to_url[ip_addr]
    else:
        try:
            url = socket.gethostbyaddr(ip_addr)[0]
        except socket.error as e:
            url = ip_addr
        ip_to_url[ip_addr] = url
    
    # Hash URL into dictionary:
    if url not in url_count:
        url_count[url] = 1
    else:
        url_count[url] += 1

def get_output_fn(input_fn, verbose):
    """A helper method for parse_log().
    Constructs an output file name that does not conflict with 
    exisiting filenames in the directory of the target .log file.

    Keyword arguments:
    input_fn -- the input filename
    verbose -- whether to include 'verbose' descriptor in filename
    """
    output_fn = input_fn[:-4] + "_output"
    if verbose:
        output_fn += "_verbose"
    if os.path.isfile(output_fn + ".log"):
        output_fn += "_1"
    ext_val = 2
    while os.path.isfile(output_fn + ".log"):
        output_fn = output_fn[:-1] + str(ext_val)
        ext_val += 1
    output_fn += ".log"
    return output_fn

def write_entries(num_write, url_count, out, verbose):
    """A helper method for parse_log().
    Write the most frequent URLs to the specified output file.

    Keyword arguments:
    num_write -- number of URLs to output
    url_count -- dictionary mapping URLs to frequency
    out -- file handle for output file
    verbose -- whether to write verbose output
    """
    sorted_url = sorted(url_count.items(), key=lambda p: p[1], reverse=True)
    i, len_sort = 0, len(sorted_url)
    if verbose:
        while i < num_write and i < len_sort:
            out.write(str(i+1) + ": " + sorted_url[i][0] + ", frequency: " + 
                      str(sorted_url[i][1]) + "\n")
            i += 1
    else:
        while i < num_write and i < len_sort:
            out.write(sorted_url[i][0] + "\n")
            i += 1

def main():
    parser = argparse.ArgumentParser(description="Parse web server log file")
    parser.add_argument('--file_name', '-f', action='store', 
                        help="Name or path of target web server log file", 
                        default="sample.log", metavar="FN")
    parser.add_argument('--num_output', '-n', action='store', type=int, 
                        help="Number of results to output", default=10, 
                        metavar="N")
    parser.add_argument('--verbose', '-v', action='store_true', default=False,
                        help="Verbose output")
    args = parser.parse_args()
    fn, n, v = "", 0, False
    for name, val in args.__dict__.items():
        if name == "file_name":
            fn = val
        elif name == "num_output":
            n = val
        elif name == "verbose":
            v = val
    if v:
        print("Outputting top " + str(n) + " URLs in " + fn + " (verbose)...")
    else:
        print("Outputting top " + str(n) + " URLs in " + fn + "...")
    print("Please be patient -- reverse IP lookup may take a while.")

    try:
        parse_log(n, file_name=fn, verbose=v)
        print("Finished.")
    except (OutputNumberException, LogFileTypeException, InvalidEntryException) as e:
        print("An error occurred: " + str(e))
    except FileNotFoundError as e:
        print("An error occurred: \'could not find file " + str(e) + "\'")

if __name__ == "__main__":
    main()
