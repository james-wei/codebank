package kvstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MasterEndToEndTest {

    KVClient client;
    ServerRunner serverRunner;
    TPCMaster master;
    KVCache masterCache;

    @Before
    public void setUp() throws IOException, InterruptedException {
        String hostname = InetAddress.getLocalHost().getHostAddress();
        masterCache = new KVCache(5, 5);
        masterCache.put("hi", "bye");
        master = new TPCMaster(0, masterCache);
        SocketServer ss = new SocketServer(hostname, 8080);
        ss.addHandler(new TPCClientHandler(master, 20));
        serverRunner = new ServerRunner(ss, "server");
        serverRunner.start();

        client = new KVClient(hostname, 8080);
    }
    
    @Test
    public void cacheTest() {
        try {
            assertEquals("bye", client.get("hi"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        serverRunner.stop();
    }
}
