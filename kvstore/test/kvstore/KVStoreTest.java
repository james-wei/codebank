package kvstore;

import static autograder.TestUtils.kTimeoutQuick;
import static kvstore.KVConstants.ERROR_NO_SUCH_KEY;
import static kvstore.KVConstants.RESP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;

public class KVStoreTest {

    public static final String TEMPORARY_FILE_NAME = "temp.txt";
    KVStore store;

    @Before
    public void setupStore() {
        store = new KVStore();
    }

    @After
    public void tearDown() {
        File f = new File(TEMPORARY_FILE_NAME);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Verify get returns value just put into store")
    public void putAndGetOneKey() throws KVException {
        String key = "this is the key.";
        String val = "this is the value.";
        store.put(key, val);
        assertEquals(val, store.get(key));
    }

    @Test
    public void testDumpAndRestore() throws KVException {
        // Create several key-value pairs in store
        String letters = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String symbols = "`1234567890-=[]\\;',./ ~!@#$%^&*()_+{}|:\"<>?";
        String whitespace = "   \t\r\n   ";
        store.put("hello", "world");
        store.put("123", "456");
        store.put(letters, "letters key");
        store.put("letters value", letters);
        store.put(symbols, "symbols key");
        store.put("symbols value", symbols);
        store.put(whitespace, "whitespace key");
        store.put("whitespace value", whitespace);
        store.put("abc", "");
        store.put("", "def");
        assertEquals(10, store.store.size());
        assertEquals("world", store.get("hello"));
        assertEquals("456", store.get("123"));
        assertEquals("letters key", store.get(letters));
        assertEquals(letters, store.get("letters value"));
        assertEquals("symbols key", store.get(symbols));
        assertEquals(symbols, store.get("symbols value"));
        assertEquals("whitespace key", store.get(whitespace));
        assertEquals(whitespace, store.get("whitespace value"));
        assertEquals("", store.get("abc"));
        assertEquals("def", store.get(""));

        // Dump store to file, and create store2
        store.dumpToFile(TEMPORARY_FILE_NAME);
        KVStore store2 = new KVStore();
        store2.restoreFromFile(TEMPORARY_FILE_NAME);

        // Check whether store equals store2
        assertEquals(10, store2.store.size());
        assertEquals("def", store2.get(""));
        assertEquals("", store2.get("abc"));
        assertEquals(whitespace, store2.get("whitespace value"));
        assertEquals("whitespace key", store2.get(whitespace));
        assertEquals(symbols, store2.get("symbols value"));
        assertEquals("symbols key", store2.get(symbols));
        assertEquals(letters, store2.get("letters value"));
        assertEquals("letters key", store2.get(letters));
        assertEquals("456", store2.get("123"));
        assertEquals("world", store2.get("hello"));
    }

    @Test
    public void testDumpAndRestore2() throws KVException {
        // Create several key-value pairs in store
        store.put("hello", "world");
        StringBuilder builder = new StringBuilder();
        for (char c = 0; c < 256; c++) {
            // Hopefully weird characters will not be tested...
            // But tab, CR, and LF actually work correctly.
            if (c == '\t' || c == '\r' || c == '\n' || c >= 32) {
                builder.append(c);
            }
        }
        String asciiChars = builder.toString();
        store.put(asciiChars, "ascii chars key");
        store.put("ascii chars value", asciiChars);
        store.put("abc", "def");
        assertEquals(4, store.store.size());
        assertEquals("world", store.get("hello"));
        assertEquals("ascii chars key", store.get(asciiChars));
        assertEquals(asciiChars, store.get("ascii chars value"));
        assertEquals("def", store.get("abc"));

        // Dump store to file, and create store2
        store.dumpToFile(TEMPORARY_FILE_NAME);
        KVStore store2 = new KVStore();
        store2.restoreFromFile(TEMPORARY_FILE_NAME);

        // Check whether store equals store2
        assertEquals(4, store2.store.size());
        assertEquals("def", store2.get("abc"));
        assertEquals(asciiChars, store2.get("ascii chars value"));
        assertEquals("ascii chars key", store2.get(asciiChars));
        assertEquals("world", store2.get("hello"));
    }

}
