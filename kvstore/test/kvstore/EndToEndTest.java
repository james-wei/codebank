package kvstore;

import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_INVALID_KEY;
import static kvstore.KVConstants.ERROR_INVALID_VALUE;
import static kvstore.KVConstants.ERROR_NO_SUCH_KEY;
import static kvstore.KVConstants.ERROR_OVERSIZED_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

public class EndToEndTest extends EndToEndTemplate {

    @Test
    public void singleEntryTest() throws KVException {
        client.put("key", "value");
        assertEquals("value", client.get("key"));
        client.del("key");
    }

    @Test
    public void multipleEntriesTest() throws KVException {
        client.put("IM", "Imran Mahmood");
        client.put("JW", "James Wei");
        client.put("VN", "Varun Naik");
        client.put("MC", "Mukund Chillakanti");
        
        assertEquals("Mukund Chillakanti", client.get("MC"));
        assertEquals("Varun Naik", client.get("VN"));
        assertEquals("James Wei", client.get("JW"));
        assertEquals("Imran Mahmood", client.get("IM"));
        
        assertEquals("Imran Mahmood", client.get("IM"));
        assertEquals("James Wei", client.get("JW"));
        assertEquals("Varun Naik", client.get("VN"));
        assertEquals("Mukund Chillakanti", client.get("MC"));
        
        client.del("IM");
        client.del("JW");
        client.del("VN");
        client.del("MC");
    }

    @Test
    public void overwriteValueTest() throws KVException {
        client.put("chicken", "nuggets");
        assertEquals("nuggets", client.get("chicken"));
        client.put("chicken", "sandwich");
        client.put("chicken", "fingers");
        assertEquals("fingers", client.get("chicken"));
        client.del("chicken");
    }

    @Test
    public void getBadKeyTest() throws KVException {
        client.put("chicken", "strips");
        assertEquals("strips", client.get("chicken"));
        client.del("chicken");
        try {
            client.get("chicken");
            fail("Should have thrown a KVException: ERROR_NO_SUCH_KEY");
        } catch (KVException e) {
            assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void delBadKeyTest() throws KVException {
        client.put("chicken", "strips");
        assertEquals("strips", client.get("chicken"));
        client.del("chicken");
        try {
            client.del("chicken");
            fail("Should have thrown a KVException: ERROR_NO_SUCH_KEY");
        } catch (KVException e) {
            assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void oversizedKeyTest() throws KVException {
        String longKey = "";
        while (longKey.length() < 260) {
            longKey = longKey + "k";
        }
        try {
            client.put(longKey, "some value");
            fail("Should have thrown a KVException: ERROR_OVERSIZED_KEY");
        } catch (KVException e) {
            assertEquals(ERROR_OVERSIZED_KEY, e.getKVMessage().getMessage());
        }
        try {
            client.get(longKey);
            fail("Should have thrown a KVException: ERROR_NO_SUCH_KEY");
        } catch (KVException e) {
            assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void getPutDeleteTest() throws KVException {
        try {
            client.get("some key");
            fail("Bad ordering.");
            client.put("some key", "some value");
            client.del("some key");
            fail("Bad ordering.");
        } catch (KVException e) {
            assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void getDeletePutTest() throws KVException {
        try {
            client.get("some key");
            fail("Bad ordering.");
            client.del("some key");
            client.put("some key", "some value");
            fail("Bad ordering.");
        } catch (KVException e) {
            assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void deletePutGetTest() throws KVException {
        try {
            client.del("some key");
            fail("Bad ordering.");
            client.put("some key", "some value");
            client.get("some key");
            fail("Bad ordering.");
        } catch (KVException e) {
            assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void deleteGetPutTest() throws KVException {
        try {
            client.del("some key");
            fail("Bad ordering.");
            client.get("some key");
            client.put("some key", "some value");
            fail("Bad ordering.");
        } catch (KVException e) {
            assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badConnectionTest() throws KVException {
        try {
            tearDown();
        } catch (InterruptedException e1) {
            fail("Couldn't tear down.");
        }
        try {
            client.put("key", "value");
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CREATE_SOCKET");
        } catch (KVException e) {
            assertEquals(ERROR_COULD_NOT_CREATE_SOCKET, e.getKVMessage().getMessage());
        }
        try {
            client.get("key");
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CREATE_SOCKET");
        } catch (KVException e) {
            assertEquals(ERROR_COULD_NOT_CREATE_SOCKET, e.getKVMessage().getMessage());
        }
        try {
            client.del("key");
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CREATE_SOCKET");
        } catch (KVException e) {
            assertEquals(ERROR_COULD_NOT_CREATE_SOCKET, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void emptyKeyTest() throws KVException {
        try {
            client.put("", "blue");
            fail("Should have thrown a KVException: ERROR_INVALID_KEY");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void nullKeyTest() throws KVException {
        try {
            client.put(null, "purple");
            fail("Should have thrown a KVException: ERROR_INVALID_KEY");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void emptyValueTest() throws KVException {
        try {
            client.put("orange", "");
            fail("Should have thrown a KVException: ERROR_INVALID_VALUE");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_VALUE, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void nullValueTest() throws KVException {
        try {
            client.put("yellow", null);
            fail("Should have thrown a KVException: ERROR_INVALID_VALUE");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_VALUE, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void nonAsciiTest() throws KVException {
        int a = 0xe386b0;
        int b = 0xe38594;
        int c = 0xf0919a83;
        int d = 0xf0928688;
        String key = String.valueOf(a) + String.valueOf(b);
        String value = String.valueOf(c) + String.valueOf(d);
        client.put(key, value);
        assertEquals(value, client.get(key));
        client.del(key);
    }

    @Test
    public void randomStressTest() throws KVException {
        Random rand = new Random();
        Map<String, String> map = new HashMap<String, String>();
        String key, val;
        for (int i = 0; i < 500; i++) {
            key = Integer.toString(rand.nextInt());
            val = Integer.toString(rand.nextInt());
            client.put(key, val);
            assertEquals(val, client.get(key));
            map.put(key, val);
        }
        Iterator<Map.Entry<String, String>> mapIter = map.entrySet().iterator();
        Map.Entry<String, String> pair;
        while(mapIter.hasNext()) {
            pair = mapIter.next();
            assertEquals(pair.getValue(), client.get(pair.getKey()));
            client.del(pair.getKey());
        }
    }

    @Test
    public void overwriteValuesStressTest() throws KVException {
        client.put("based key", "-1");
        assertEquals("-1", client.get("based key"));
        for (int i = 0; i < 500; i++) {
            client.put("based key", Integer.toString(i));
            assertEquals(Integer.toString(i), client.get("based key"));
        }
    }

    @Test
    public void badGetStressTest() throws KVException {
        client.put("mouse", "pad");
        client.put("click", "wheel");
        client.put("touch", "pad");
        for (int i = 0; i < 500; i++) {
            try {
                client.get(Integer.toString(i));
                fail("Should have thrown a KVException: ERROR_NO_SUCH_KEY");
            } catch (KVException e) {
                assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
            }
        }
    }

    @Test
    public void badDelStressTest() throws KVException {
        client.put("for", "tran");
        client.put("pas", "cal");
        for (int i = 0; i < 500; i++) {
            try {
                client.del(Integer.toString(i));
                fail("Should have thrown a KVException: ERROR_NO_SUCH_KEY");
            } catch (KVException e) {
                assertEquals(ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
            }
        }
    }

}
