package kvstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.net.InetAddress;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class TPCEndToEndTest {

    String hostname;
    KVClient client;
    TPCMaster master;
    KVCache spyCache;
    ServerRunner masterClientRunner;
    ServerRunner masterSlaveRunner;
    HashMap<String, ServerRunner> slaveRunners;
    KVServer slave1;
    KVServer slave2;
    KVServer slave3;
    KVServer slave4;

    static final int CLIENTPORT = 8888;
    static final int SLAVEPORT = 9090;

    static final int NUMSLAVES = 4;

    static final long SLAVE1 = 4611686018427387903L;  // Long.MAX_VALUE/2
    static final long SLAVE2 = 9223372036854775807L;  // Long.MAX_VALUE
    static final long SLAVE3 = -4611686018427387903L; // Long.MIN_VALUE/2
    static final long SLAVE4 = -0000000000000000001;  // Long.MIN_VALUE

    static final String KEY1 = "6666666666666666666"; // 2846774474343087985
    static final String KEY2 = "9999999999999999999"; // 8204764838124603412
    static final String KEY3 = "0000000000000000000"; //-7869206253219942869
    static final String KEY4 = "3333333333333333333"; //-2511215889438427442

    @Before
    public void setUp() throws Exception {
        hostname = InetAddress.getLocalHost().getHostAddress();

        startMaster();

        slaveRunners = new HashMap<String, ServerRunner>();
        slave1 = startSlave(SLAVE1);
        slave2 = startSlave(SLAVE2);
        slave3 = startSlave(SLAVE3);
        slave4 = startSlave(SLAVE4);

        client = new KVClient(hostname, CLIENTPORT);
    }

    @After
    public void tearDown() throws InterruptedException {
        masterClientRunner.stop();
        masterSlaveRunner.stop();

        for (ServerRunner slaveRunner : slaveRunners.values()) {
            slaveRunner.stop();
        }

        client = null;
        master = null;
        slaveRunners = null;
    }

    protected void startMaster() throws Exception {
        spyCache = spy(new KVCache(1,4));
        master = spy(new TPCMaster(NUMSLAVES, spyCache));
        SocketServer clientSocketServer = new SocketServer(hostname, CLIENTPORT);
        clientSocketServer.addHandler(new TPCClientHandler(master));
        masterClientRunner = new ServerRunner(clientSocketServer, "masterClient");
        masterClientRunner.start();
        SocketServer slaveSocketServer = new SocketServer(hostname, SLAVEPORT);
        slaveSocketServer.addHandler(new TPCRegistrationHandler(master));
        masterSlaveRunner = new ServerRunner(slaveSocketServer, "masterSlave");
        masterSlaveRunner.start();
        Thread.sleep(100);
    }

    protected KVServer startSlave(long slaveID) throws Exception {
        String name = new Long(slaveID).toString();
        ServerRunner sr = slaveRunners.get(name);
        if (sr != null) {
            sr.start();
            return null;
        }

        SocketServer ss = new SocketServer(InetAddress.getLocalHost().getHostAddress(), 0);
        KVServer slaveKvs = new KVServer(100, 10);
        Long id = new Long(slaveID);
        File temp = File.createTempFile(id.toString() + "calbandgreat",".txt");
        temp.deleteOnExit();
        String logPath = temp.getPath(); //"bin/log." + slaveID + "@" + ss.getHostname();
        TPCLog log = new TPCLog(logPath, slaveKvs);
        TPCMasterHandler handler = new TPCMasterHandler(slaveID, slaveKvs, log);
        ss.addHandler(handler);
        ServerRunner slaveRunner = new ServerRunner(ss, name);
        slaveRunner.start();
        slaveRunners.put(name, slaveRunner);

        handler.registerWithMaster(InetAddress.getLocalHost().getHostAddress(), ss);
        return slaveKvs;
    }

    protected void stopSlave(String name) throws InterruptedException {
        ServerRunner sr = slaveRunners.get(name);
        if (sr == null) {
            throw new RuntimeException("Slave does not exist!");
        } else {
            sr.stop();
        }
    }

    private void assertCacheCount(KVCache cache, int getCount, int putCount, int delCount) {
        verify(cache, times(getCount)).get(anyString());
        verify(cache, times(putCount)).put(anyString(), anyString());
        verify(cache, times(delCount)).del(anyString());
    }

    private void assertReplicaCount(TPCMaster master, int primaryCount, int secondaryCount) {
        verify(master, times(primaryCount)).findFirstReplica(anyString());
        verify(master, times(secondaryCount)).findSuccessor((TPCSlaveInfo) anyObject());
    }

    private void assertNoSuchKey(KVServer slave, String key) {
        try {
            slave.get(key);
        } catch (KVException e) {
            assertEquals(KVConstants.ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    private void clearCache(KVCache cache) {
        spyCache.del(KEY1);
        spyCache.del(KEY2);
        spyCache.del(KEY3);
        spyCache.del(KEY4);
    }

    @Test
    public void testBadGet() throws KVException {
        KVMessage putMsg, getMsg;

        // Good PUT request
        putMsg = new KVMessage(KVConstants.PUT_REQ);
        putMsg.setKey(KEY1);
        putMsg.setValue("asdf");
        master.handleTPCRequest(putMsg, true);

        // Bad GET requests
        getMsg = new KVMessage(KVConstants.GET_REQ);
        assertNull(getMsg.getKey());
        try {
            master.handleGet(getMsg);
            fail("Get with null key should have failed");
        } catch (KVException e) {}

        getMsg = new KVMessage(KVConstants.GET_REQ);
        getMsg.setKey("");
        try {
            master.handleGet(getMsg);
            fail("Get with empty key should have failed");
        } catch (KVException e) {}

        getMsg = new KVMessage(KVConstants.GET_REQ);
        getMsg.setKey(KEY1);
        getMsg.setValue("this should not exist");
        try {
            master.handleGet(getMsg);
            fail("Get with value should have failed");
        } catch (KVException e) {}

        // Good GET request
        getMsg = new KVMessage(KVConstants.GET_REQ);
        getMsg.setKey(KEY1);
        assertEquals("asdf", master.handleGet(getMsg));
        assertEquals("asdf", spyCache.get(KEY1));
        assertEquals("asdf", slave1.get(KEY1));
        assertEquals("asdf", slave2.get(KEY1));
        assertNoSuchKey(slave3, KEY1);
        assertNoSuchKey(slave4, KEY1);
    }

    @Test
    public void testBadPut() throws KVException {
        KVMessage putMsg, getMsg;

        // Bad PUT requests
        putMsg = new KVMessage(KVConstants.PUT_REQ);
        assertNull(putMsg.getKey());
        putMsg.setValue("asdf");
        try {
            master.handleTPCRequest(putMsg, true);
            fail("Put with null key should have failed");
        } catch (KVException e) {}

        putMsg = new KVMessage(KVConstants.PUT_REQ);
        putMsg.setKey(KEY1);
        assertNull(putMsg.getValue());
        try {
            master.handleTPCRequest(putMsg, true);
            fail("Put with null value should have failed");
        } catch (KVException e) {}

        putMsg = new KVMessage(KVConstants.PUT_REQ);
        putMsg.setKey("");
        putMsg.setValue("asdf");
        try {
            master.handleTPCRequest(putMsg, true);
            fail("Put with empty key should have failed");
        } catch (KVException e) {}

        putMsg = new KVMessage(KVConstants.PUT_REQ);
        putMsg.setKey(KEY1);
        putMsg.setValue("");
        try {
            master.handleTPCRequest(putMsg, true);
            fail("Put with empty value should have failed");
        } catch (KVException e) {}

        // Good PUT request
        putMsg = new KVMessage(KVConstants.PUT_REQ);
        putMsg.setKey(KEY1);
        putMsg.setValue("asdf");
        master.handleTPCRequest(putMsg, true);

        // Good GET request
        getMsg = new KVMessage(KVConstants.GET_REQ);
        getMsg.setKey(KEY1);
        assertEquals("asdf", master.handleGet(getMsg));
        assertEquals("asdf", spyCache.get(KEY1));
        assertEquals("asdf", slave1.get(KEY1));
        assertEquals("asdf", slave2.get(KEY1));
        assertNoSuchKey(slave3, KEY1);
        assertNoSuchKey(slave4, KEY1);
    }

    @Test
    public void testBadDel() throws KVException {
        KVMessage putMsg, delMsg, getMsg;

        // Good PUT request
        putMsg = new KVMessage(KVConstants.PUT_REQ);
        putMsg.setKey(KEY1);
        putMsg.setValue("asdf");
        master.handleTPCRequest(putMsg, true);

        // Bad DEL requests
        delMsg = new KVMessage(KVConstants.DEL_REQ);
        assertNull(delMsg.getKey());
        try {
            master.handleTPCRequest(delMsg, false);
            fail("Get with null key should have failed");
        } catch (KVException e) {}

        delMsg = new KVMessage(KVConstants.DEL_REQ);
        delMsg.setKey("");
        try {
            master.handleTPCRequest(delMsg, false);
            fail("Get with empty key should have failed");
        } catch (KVException e) {}

        delMsg = new KVMessage(KVConstants.DEL_REQ);
        delMsg.setKey(KEY1);
        delMsg.setValue("this should not exist");

        try {
            master.handleTPCRequest(delMsg, false);
            fail("Get with value should have failed");
        } catch (KVException e) {}

        // Good DEL request
        delMsg = new KVMessage(KVConstants.DEL_REQ);
        delMsg.setKey(KEY1);
        master.handleTPCRequest(delMsg, false);

        // Good GET request
        getMsg = new KVMessage(KVConstants.GET_REQ);
        getMsg.setKey(KEY1);
        assertNull(spyCache.get(KEY1));
        assertNoSuchKey(slave1, KEY1);
        assertNoSuchKey(slave2, KEY1);
        assertNoSuchKey(slave3, KEY1);
        assertNoSuchKey(slave4, KEY1);
        try {
            master.handleGet(getMsg);
            fail("Get after delete should have failed");
        } catch (KVException e) {
            assertEquals(KVConstants.ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void testPutGetDel() throws KVException {
        KVMessage msg1, msg2, msg3, msg4, delReq;
        TPCSlaveInfo slaveInfo1 = master.getSlave(SLAVE1);
        assertNotNull(slaveInfo1);
        TPCSlaveInfo slaveInfo2 = master.getSlave(SLAVE2);
        assertNotNull(slaveInfo2);
        TPCSlaveInfo slaveInfo3 = master.getSlave(SLAVE3);
        assertNotNull(slaveInfo3);
        TPCSlaveInfo slaveInfo4 = master.getSlave(SLAVE4);
        assertNotNull(slaveInfo4);

        // Insert new values, one per slave
        assertReplicaCount(master, 0, 0);
        msg1 = new KVMessage(KVConstants.PUT_REQ);
        msg1.setKey(KEY1);
        msg1.setValue("hello1");
        master.handleTPCRequest(msg1, true);
        assertReplicaCount(master, 1, 1);
        assertCacheCount(spyCache, 0, 1, 0);
        msg1 = new KVMessage(KVConstants.GET_REQ);
        msg1.setKey(KEY1);
        assertEquals("hello1", master.handleGet(msg1));
        assertCacheCount(spyCache, 1, 1, 0);
        assertEquals("hello1", slave1.get(KEY1));
        assertEquals("hello1", slave2.get(KEY1));
        assertNoSuchKey(slave3, KEY1);
        assertNoSuchKey(slave4, KEY1);
        assertReplicaCount(master, 1, 1);

        msg2 = new KVMessage(KVConstants.PUT_REQ);
        msg2.setKey(KEY2);
        msg2.setValue("hello2");
        master.handleTPCRequest(msg2, true);
        assertReplicaCount(master, 2, 2);
        assertCacheCount(spyCache, 1, 2, 0);
        msg2 = new KVMessage(KVConstants.GET_REQ);
        msg2.setKey(KEY2);
        assertEquals("hello2", master.handleGet(msg2));
        assertCacheCount(spyCache, 2, 2, 0);
        assertEquals("hello1", slave1.get(KEY1));
        assertEquals("hello1", slave2.get(KEY1));
        assertEquals("hello2", slave2.get(KEY2));
        assertEquals("hello2", slave3.get(KEY2));
        assertNoSuchKey(slave4, KEY2);
        assertNoSuchKey(slave1, KEY2);
        assertReplicaCount(master, 2, 2);

        msg3 = new KVMessage(KVConstants.PUT_REQ);
        msg3.setKey(KEY3);
        msg3.setValue("hello3");
        master.handleTPCRequest(msg3, true);
        assertReplicaCount(master, 3, 3);
        assertCacheCount(spyCache, 2, 3, 0);
        msg3 = new KVMessage(KVConstants.GET_REQ);
        msg3.setKey(KEY3);
        assertEquals("hello3", master.handleGet(msg3));
        assertCacheCount(spyCache, 3, 3, 0);
        assertEquals("hello1", slave1.get(KEY1));
        assertEquals("hello1", slave2.get(KEY1));
        assertEquals("hello2", slave2.get(KEY2));
        assertEquals("hello2", slave3.get(KEY2));
        assertEquals("hello3", slave3.get(KEY3));
        assertEquals("hello3", slave4.get(KEY3));
        assertNoSuchKey(slave1, KEY3);
        assertNoSuchKey(slave2, KEY3);
        assertReplicaCount(master, 3, 3);

        msg4 = new KVMessage(KVConstants.PUT_REQ);
        msg4.setKey(KEY4);
        msg4.setValue("hello4");
        master.handleTPCRequest(msg4, true);
        assertCacheCount(spyCache, 3, 4, 0);
        msg4 = new KVMessage(KVConstants.GET_REQ);
        msg4.setKey(KEY4);
        assertEquals("hello4", master.handleGet(msg4));
        assertReplicaCount(master, 4, 4);
        assertCacheCount(spyCache, 4, 4, 0);
        assertEquals("hello1", slave1.get(KEY1));
        assertEquals("hello1", slave2.get(KEY1));
        assertEquals("hello2", slave2.get(KEY2));
        assertEquals("hello2", slave3.get(KEY2));
        assertEquals("hello3", slave3.get(KEY3));
        assertEquals("hello3", slave4.get(KEY3));
        assertEquals("hello4", slave4.get(KEY4));
        assertEquals("hello4", slave1.get(KEY4));
        assertNoSuchKey(slave2, KEY4);
        assertNoSuchKey(slave3, KEY4);
        assertReplicaCount(master, 4, 4);

        // Overwrite the old values
        msg1 = new KVMessage(KVConstants.PUT_REQ);
        msg1.setKey(KEY1);
        msg1.setValue("hello11");
        master.handleTPCRequest(msg1, true);
        assertReplicaCount(master, 5, 5);
        assertCacheCount(spyCache, 4, 5, 0);
        msg1 = new KVMessage(KVConstants.GET_REQ);
        msg1.setKey(KEY1);
        assertEquals("hello11", master.handleGet(msg1));
        assertCacheCount(spyCache, 5, 5, 0);
        assertEquals("hello11", slave1.get(KEY1));
        assertEquals("hello11", slave2.get(KEY1));
        assertEquals("hello2", slave2.get(KEY2));
        assertEquals("hello2", slave3.get(KEY2));
        assertEquals("hello3", slave3.get(KEY3));
        assertEquals("hello3", slave4.get(KEY3));
        assertEquals("hello4", slave4.get(KEY4));
        assertEquals("hello4", slave1.get(KEY4));
        assertNoSuchKey(slave3, KEY1);
        assertNoSuchKey(slave4, KEY1);
        assertReplicaCount(master, 5, 5);

        msg2 = new KVMessage(KVConstants.PUT_REQ);
        msg2.setKey(KEY2);
        msg2.setValue("hello22");
        master.handleTPCRequest(msg2, true);
        assertReplicaCount(master, 6, 6);
        assertCacheCount(spyCache, 5, 6, 0);
        msg2 = new KVMessage(KVConstants.GET_REQ);
        msg2.setKey(KEY2);
        assertEquals("hello22", master.handleGet(msg2));
        assertCacheCount(spyCache, 6, 6, 0);
        assertEquals("hello11", slave1.get(KEY1));
        assertEquals("hello11", slave2.get(KEY1));
        assertEquals("hello22", slave2.get(KEY2));
        assertEquals("hello22", slave3.get(KEY2));
        assertEquals("hello3", slave3.get(KEY3));
        assertEquals("hello3", slave4.get(KEY3));
        assertEquals("hello4", slave4.get(KEY4));
        assertEquals("hello4", slave1.get(KEY4));
        assertNoSuchKey(slave4, KEY2);
        assertNoSuchKey(slave1, KEY2);
        assertReplicaCount(master, 6, 6);

        msg3 = new KVMessage(KVConstants.PUT_REQ);
        msg3.setKey(KEY3);
        msg3.setValue("hello33");
        master.handleTPCRequest(msg3, true);
        assertReplicaCount(master, 7, 7);
        assertCacheCount(spyCache, 6, 7, 0);
        msg3 = new KVMessage(KVConstants.GET_REQ);
        msg3.setKey(KEY3);
        assertEquals("hello33", master.handleGet(msg3));
        assertCacheCount(spyCache, 7, 7, 0);
        assertEquals("hello11", slave1.get(KEY1));
        assertEquals("hello11", slave2.get(KEY1));
        assertEquals("hello22", slave2.get(KEY2));
        assertEquals("hello22", slave3.get(KEY2));
        assertEquals("hello33", slave3.get(KEY3));
        assertEquals("hello33", slave4.get(KEY3));
        assertEquals("hello4", slave4.get(KEY4));
        assertEquals("hello4", slave1.get(KEY4));
        assertNoSuchKey(slave1, KEY3);
        assertNoSuchKey(slave2, KEY3);
        assertReplicaCount(master, 7, 7);

        msg4 = new KVMessage(KVConstants.PUT_REQ);
        msg4.setKey(KEY4);
        msg4.setValue("hello44");
        master.handleTPCRequest(msg4, true);
        assertReplicaCount(master, 8, 8);
        assertCacheCount(spyCache, 7, 8, 0);
        msg4 = new KVMessage(KVConstants.GET_REQ);
        msg4.setKey(KEY4);
        assertEquals("hello44", master.handleGet(msg4));
        assertCacheCount(spyCache, 8, 8, 0);
        assertEquals("hello11", slave1.get(KEY1));
        assertEquals("hello11", slave2.get(KEY1));
        assertEquals("hello22", slave2.get(KEY2));
        assertEquals("hello22", slave3.get(KEY2));
        assertEquals("hello33", slave3.get(KEY3));
        assertEquals("hello33", slave4.get(KEY3));
        assertEquals("hello44", slave4.get(KEY4));
        assertEquals("hello44", slave1.get(KEY4));
        assertNoSuchKey(slave2, KEY4);
        assertNoSuchKey(slave3, KEY4);
        assertReplicaCount(master, 8, 8);

        // Clear the cache, forcing lookup in primary replica
        clearCache(spyCache);
        assertCacheCount(spyCache, 8, 8, 4);
        assertEquals("hello11", slave1.get(KEY1));
        assertEquals("hello11", slave2.get(KEY1));
        assertEquals("hello22", slave2.get(KEY2));
        assertEquals("hello22", slave3.get(KEY2));
        assertEquals("hello33", slave3.get(KEY3));
        assertEquals("hello33", slave4.get(KEY3));
        assertEquals("hello44", slave4.get(KEY4));
        assertEquals("hello44", slave1.get(KEY4));
        doReturn("hello11").when(master).getValueFromReplica(eq(slaveInfo1), (KVMessage) anyObject());
        doReturn("hello22").when(master).getValueFromReplica(eq(slaveInfo2), (KVMessage) anyObject());
        doReturn("hello33").when(master).getValueFromReplica(eq(slaveInfo3), (KVMessage) anyObject());
        doReturn("hello44").when(master).getValueFromReplica(eq(slaveInfo4), (KVMessage) anyObject());

        msg1 = new KVMessage(KVConstants.GET_REQ);
        msg1.setKey(KEY1);
        try {
            assertEquals("hello11", master.handleGet(msg1));
        } catch (KVException e) {
            fail("Should not fail");
        }
        assertReplicaCount(master, 9, 8);
        assertCacheCount(spyCache, 9, 9, 4);

        msg2 = new KVMessage(KVConstants.GET_REQ);
        msg2.setKey(KEY2);
        try {
            assertEquals("hello22", master.handleGet(msg2));
        } catch (KVException e) {
            fail("Should not fail");
        }
        assertReplicaCount(master, 10, 8);
        assertCacheCount(spyCache, 10, 10, 4);

        msg3 = new KVMessage(KVConstants.GET_REQ);
        msg3.setKey(KEY3);
        try {
            assertEquals("hello33", master.handleGet(msg3));
        } catch (KVException e) {
            fail("Should not fail");
        }
        assertReplicaCount(master, 11, 8);
        assertCacheCount(spyCache, 11, 11, 4);

        msg4 = new KVMessage(KVConstants.GET_REQ);
        msg4.setKey(KEY4);
        try {
            assertEquals("hello44", master.handleGet(msg4));
        } catch (KVException e) {
            fail("Should not fail");
        }
        assertReplicaCount(master, 12, 8);
        assertCacheCount(spyCache, 12, 12, 4);

        // Clear the cache and bring down slave1, forcing KEY1 lookup in slave2
        clearCache(spyCache);
        assertCacheCount(spyCache, 12, 12, 8);
        doReturn(null).when(master).getValueFromReplica(eq(slaveInfo1), (KVMessage) anyObject());
        doReturn("hello11").when(master).getValueFromReplica(eq(slaveInfo2), (KVMessage) anyObject());

        msg1 = new KVMessage(KVConstants.GET_REQ);
        msg1.setKey(KEY1);
        assertEquals("hello11", slave2.get(KEY1));
        try {
            assertEquals("hello11", master.handleGet(msg1));
        } catch (KVException e) {
            fail("Should not fail");
        }
        assertReplicaCount(master, 13, 9);
        assertCacheCount(spyCache, 13, 13, 8);

        // Delete KEY3, so that looking up KEY3 raises an exception
        delReq = new KVMessage(KVConstants.DEL_REQ);
        delReq.setKey(KEY3);
        try {
            master.handleTPCRequest(delReq, false);
        } catch (KVException e) {
            fail("Should not fail");
        }
        assertReplicaCount(master, 14, 10);
        assertCacheCount(spyCache, 13, 13, 9);
        assertNoSuchKey(slave3, KEY3);
        assertNoSuchKey(slave4, KEY3);
        doReturn(null).when(master).getValueFromReplica(eq(slaveInfo3), (KVMessage) anyObject());
        doReturn(null).when(master).getValueFromReplica(eq(slaveInfo4), (KVMessage) anyObject());

        msg3 = new KVMessage(KVConstants.GET_REQ);
        msg3.setKey(KEY3);
        try {
            master.handleGet(msg3);
            fail("Should not get here");
        } catch (KVException e) {}
        assertReplicaCount(master, 15, 11);
        assertCacheCount(spyCache, 14, 13, 9);
    }

}
