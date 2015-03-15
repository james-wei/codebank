package kvstore;

import static org.junit.Assert.*;
import static kvstore.KVConstants.*;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import kvstore.TPCMaster.SlaveList;
import kvstore.TPCMaster.SlaveNode;

import org.junit.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.api.mockito.PowerMockito;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Socket.class, KVMessage.class, TPCMasterHandler.class, TPCSlaveInfo.class, TPCMaster.class})
public class TPCMasterTest {

    TPCMaster master;
    KVCache masterCache;
    static final long SLAVE1 = 4611686018427387903L;  // Long.MAX_VALUE/2
    static final long SLAVE2 = 9223372036854775807L;  // Long.MAX_VALUE
    static final long SLAVE3 = -4611686018427387903L; // Long.MIN_VALUE/2
    static final long SLAVE4 = -0000000000000000001;  // Long.MIN_VALUE
    static final long SLAVE5 = 6230492013836775123L;  // Arbitrary long value

    TPCSlaveInfo slave1;
    TPCSlaveInfo slave2;
    TPCSlaveInfo slave3;
    TPCSlaveInfo slave4;
    TPCSlaveInfo slave5;

    @Before
    public void setupMaster() throws KVException {
        masterCache = new KVCache(5, 5);
        master = new TPCMaster(4, masterCache);

        slave1 = new TPCSlaveInfo(SLAVE1 + "@111.111.111.111:1");
        slave2 = new TPCSlaveInfo(SLAVE2 + "@111.111.111.111:2");
        slave3 = new TPCSlaveInfo(SLAVE3 + "@111.111.111.111:3");
        slave4 = new TPCSlaveInfo(SLAVE4 + "@111.111.111.111:4");
        slave5 = new TPCSlaveInfo(SLAVE5 + "@111.111.111.111:5");
    }

    @Test
    public void testMaxSlaves() throws KVException {
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);
        assertTrue(master.getNumRegisteredSlaves() == 4);
    }

    @Test
    public void testMoreThanMaxSlaves() throws KVException {
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);
        master.registerSlave(slave5);
        assertTrue(master.getNumRegisteredSlaves() == 4);
        assertEquals(master.getSlave(SLAVE5), null);
    }

    @Test
    public void testReconnectSlave() throws KVException {
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);

        assertTrue(master.getNumRegisteredSlaves() == 4);

        slave1 = new TPCSlaveInfo(SLAVE1 + "@111.111.111.111:8080");
        master.registerSlave(slave1);

        assertTrue(master.getNumRegisteredSlaves() == 4);
    }
    
    @Test
    public void testFindFirstReplica() throws KVException {
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);

        String key = "6666666666666666666";
        assertEquals(TPCMaster.hashTo64bit(key), 2846774474343087985L);
        String key2 = "6666666666666666665";
        assertEquals(TPCMaster.hashTo64bit(key2), 2846774474343087984L);
        TPCSlaveInfo firstReplica = master.findFirstReplica(key);
        assertEquals(firstReplica, slave1);
        TPCSlaveInfo firstReplica2 = master.findFirstReplica(key2);
        assertEquals(firstReplica2, slave1);
    }
    
    @Test
    public void testFindSlaveNodeAfterLong() throws KVException {
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE1, SLAVE2));
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE2, SLAVE3));
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE3, SLAVE4));

        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);

        assertTrue(master.getNumRegisteredSlaves() == 4);
        assertEquals(master.findSlaveAfterLong(SLAVE1 - 1L).getSlaveID(), SLAVE1);
        assertEquals(master.findSlaveAfterLong(SLAVE1).getSlaveID(), SLAVE1);
        assertEquals(master.findSlaveAfterLong(SLAVE1 + 1L).getSlaveID(), SLAVE2);
        assertEquals(master.findSlaveAfterLong(SLAVE2 - 1L).getSlaveID(), SLAVE2);
        assertEquals(master.findSlaveAfterLong(SLAVE2).getSlaveID(), SLAVE2);
        assertEquals(master.findSlaveAfterLong(SLAVE2 + 1L).getSlaveID(), SLAVE3);
        assertEquals(master.findSlaveAfterLong(SLAVE3 - 1L).getSlaveID(), SLAVE3);
        assertEquals(master.findSlaveAfterLong(SLAVE3).getSlaveID(), SLAVE3);
        assertEquals(master.findSlaveAfterLong(SLAVE3 + 1L).getSlaveID(), SLAVE4);
        assertEquals(master.findSlaveAfterLong(SLAVE4 - 1L).getSlaveID(), SLAVE4);
        assertEquals(master.findSlaveAfterLong(SLAVE4).getSlaveID(), SLAVE4);
        assertEquals(master.findSlaveAfterLong(SLAVE4 + 1L).getSlaveID(), SLAVE1);
    }

    @Test
    public void testFindSuccessor() throws KVException {
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);

        assertEquals(master.findSuccessor(slave1), slave2);
        assertEquals(master.findSuccessor(slave2), slave3);
        assertEquals(master.findSuccessor(slave3), slave4);
        assertEquals(master.findSuccessor(slave4), slave1);
    }

    @Test
    public void testFindSuccessorDecreasingOrder() throws KVException {
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE1, SLAVE2));
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE2, SLAVE3));
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE3, SLAVE4));

        master.registerSlave(slave4);
        master.registerSlave(slave3);
        master.registerSlave(slave2);
        master.registerSlave(slave1);

        assertTrue(master.getNumRegisteredSlaves() == 4);
        assertEquals(master.findSuccessor(slave1), slave2);
        assertEquals(master.findSuccessor(slave2), slave3);
        assertEquals(master.findSuccessor(slave3), slave4);
        assertEquals(master.findSuccessor(slave4), slave1);
    }

    @Test
    public void testFindSuccessorArbitraryOrder() throws KVException {
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE1, SLAVE5));
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE5, SLAVE2));
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE5, SLAVE3));
        assertTrue(TPCMaster.isLessThanUnsigned(SLAVE5, SLAVE4));

        master.registerSlave(slave2);
        master.registerSlave(slave5);
        master.registerSlave(slave1);
        master.registerSlave(slave4);

        assertTrue(master.getNumRegisteredSlaves() == 4);
        assertEquals(master.findSuccessor(slave1), slave5);
        assertEquals(master.findSuccessor(slave5), slave2);
        assertEquals(master.findSuccessor(slave2), slave4);
        assertEquals(master.findSuccessor(slave4), slave1);
    }

    @Test
    public void testFindInvalidSuccessor() throws KVException {
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);
        assertEquals(master.findSuccessor(slave5), slave2);
    }

    @Test
    public void testDuplicateRegistrations() throws KVException {
        TPCSlaveInfo dup1 = new TPCSlaveInfo(SLAVE1 + "@111.111.111.111:0");
        TPCSlaveInfo dup2 = new TPCSlaveInfo(SLAVE2 + "@111.111.111.111:65535");
        TPCSlaveInfo dup3 = new TPCSlaveInfo(SLAVE3 + "@111.111.111.111:32");
        TPCSlaveInfo dup4 = new TPCSlaveInfo(SLAVE4 + "@111.111.111.111:162");

        /* Make sure the actual TPCSlaveInfo objects are stored. */
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);
        assertTrue(master.getNumRegisteredSlaves() == 4);
        assertSame(master.getSlave(SLAVE1), slave1);
        assertSame(master.getSlave(SLAVE2), slave2);
        assertSame(master.getSlave(SLAVE3), slave3);
        assertSame(master.getSlave(SLAVE4), slave4);
        assertNull(master.getSlave(SLAVE5));

        /* Make sure new TPCSlaveInfo objects with the same IDs are stored. */
        master.registerSlave(dup1);
        master.registerSlave(dup4);
        master.registerSlave(dup3);
        master.registerSlave(dup2);
        assertTrue(master.getNumRegisteredSlaves() == 4);
        assertSame(master.getSlave(SLAVE1), dup1);
        assertSame(master.getSlave(SLAVE2), dup2);
        assertSame(master.getSlave(SLAVE3), dup3);
        assertSame(master.getSlave(SLAVE4), dup4);
        assertNull(master.getSlave(SLAVE5));
        assertEquals(dup1.getPort(), 0);
        assertEquals(dup2.getPort(), 65535);
        assertEquals(dup3.getPort(), 32);
        assertEquals(dup4.getPort(), 162);

        /* Make sure that TPCSlaveInfo objects with new IDs are not stored. */
        master.registerSlave(slave1);
        master.registerSlave(slave2);
        master.registerSlave(slave3);
        master.registerSlave(slave4);
        master.registerSlave(slave5);
        assertTrue(master.getNumRegisteredSlaves() == 4);
        assertSame(master.getSlave(SLAVE1), slave1);
        assertSame(master.getSlave(SLAVE2), slave2);
        assertSame(master.getSlave(SLAVE3), slave3);
        assertSame(master.getSlave(SLAVE4), slave4);
        assertNull(master.getSlave(SLAVE5));
        assertEquals(slave1.getPort(), 1);
        assertEquals(slave2.getPort(), 2);
        assertEquals(slave3.getPort(), 3);
        assertEquals(slave4.getPort(), 4);
    }

    @Test
    public void testDataStructureLengths() throws KVException {
        SlaveList slaveList = new SlaveList();
        Map<Long, TPCSlaveInfo> slaveIdMap = new HashMap<Long, TPCSlaveInfo>();
        Map<TPCSlaveInfo, SlaveNode> slaveNodeMap = new HashMap<TPCSlaveInfo, SlaveNode>();
        master = new TPCMaster(4, masterCache, slaveList, slaveIdMap, slaveNodeMap);

        TPCSlaveInfo dup1 = new TPCSlaveInfo(SLAVE1 + "@111.111.111.111:0");
        TPCSlaveInfo dup2 = new TPCSlaveInfo(SLAVE2 + "@111.111.111.111:65535");

        assertEquals(master.getNumRegisteredSlaves(), 0);
        assertEquals(slaveList.size(), 0);
        assertEquals(slaveIdMap.size(), 0);
        assertEquals(slaveNodeMap.size(), 0);
        master.registerSlave(slave1);
        assertEquals(master.getNumRegisteredSlaves(), 1);
        assertEquals(slaveList.size(), 1);
        assertEquals(slaveIdMap.size(), 1);
        assertEquals(slaveNodeMap.size(), 1);
        master.registerSlave(slave2);
        assertEquals(master.getNumRegisteredSlaves(), 2);
        assertEquals(slaveList.size(), 2);
        assertEquals(slaveIdMap.size(), 2);
        assertEquals(slaveNodeMap.size(), 2);
        master.registerSlave(dup1);
        assertEquals(master.getNumRegisteredSlaves(), 2);
        assertEquals(slaveList.size(), 2);
        assertEquals(slaveIdMap.size(), 2);
        assertEquals(slaveNodeMap.size(), 2);
        master.registerSlave(dup2);
        assertEquals(master.getNumRegisteredSlaves(), 2);
        assertEquals(slaveList.size(), 2);
        assertEquals(slaveIdMap.size(), 2);
        assertEquals(slaveNodeMap.size(), 2);
        master.registerSlave(slave3);
        assertEquals(master.getNumRegisteredSlaves(), 3);
        assertEquals(slaveList.size(), 3);
        assertEquals(slaveIdMap.size(), 3);
        assertEquals(slaveNodeMap.size(), 3);
        master.registerSlave(slave4);
        assertEquals(master.getNumRegisteredSlaves(), 4);
        assertEquals(slaveList.size(), 4);
        assertEquals(slaveIdMap.size(), 4);
        assertEquals(slaveNodeMap.size(), 4);
        master.registerSlave(slave5);
        assertEquals(master.getNumRegisteredSlaves(), 4);
        assertEquals(slaveList.size(), 4);
        assertEquals(slaveIdMap.size(), 4);
        assertEquals(slaveNodeMap.size(), 4);
    }

    @Test
    public void testSimpleHandleGet() {
        try {
            //Setting up Master
            masterCache = new KVCache(5, 5);
            // masterCache = mock(KVCache.class);
            master = new TPCMaster(2, masterCache);
            slave1 = mock(TPCSlaveInfo.class);
            slave2 = mock(TPCSlaveInfo.class);

            //Mocking!!
            Socket sockMock = mock(Socket.class);
            KVMessage kvmGetMock = mock(KVMessage.class);
            KVMessage kvmRespMock = mock(KVMessage.class);
            TPCSlaveInfo slaveInfoMock = mock(TPCSlaveInfo.class);

            PowerMockito.whenNew(Socket.class).withAnyArguments().thenReturn(sockMock);
            PowerMockito.whenNew(TPCSlaveInfo.class).withAnyArguments().thenReturn(slaveInfoMock);
            PowerMockito.whenNew(KVMessage.class).withArguments(GET_REQ).thenReturn(kvmGetMock);
            PowerMockito.whenNew(KVMessage.class).withArguments(RESP).thenReturn(kvmRespMock);
            PowerMockito.whenNew(KVMessage.class).withArguments(any(Socket.class), any(Integer.class)).thenReturn(kvmRespMock);

            when(slave1.connectHost(any(Integer.class))).thenReturn(sockMock);
            when(kvmGetMock.getKey()).thenReturn("I'm kvmRespMock key!");
            doNothing().when(kvmGetMock).setKey(any(String.class));
            doNothing().when(kvmGetMock).sendMessage(any(Socket.class));
            doNothing().when(kvmRespMock).sendMessage(any(Socket.class));
            when(slave1.getSlaveID()).thenReturn(1L);
            when(slave2.getSlaveID()).thenReturn(2L);

            when(kvmRespMock.getMsgType()).thenReturn(RESP);
            when(kvmRespMock.getKey()).thenReturn("I'm kvmRespMock key!");
            when(kvmRespMock.getValue()).thenReturn("I'm kvmRespMock value!");

            master.registerSlave(slave1);
            master.registerSlave(slave2);
            KVMessage msg = new KVMessage(GET_REQ);
            msg.setKey("I'm kvmRespMock key!");
            assertEquals(master.handleGet(msg), "I'm kvmRespMock value!");
            //Test to see that phase 1 wasn't entered
            verify(kvmRespMock, times(1)).getValue();
            verify(kvmRespMock, times(0)).getKey();
        } catch (Exception e) {
            e.printStackTrace();
            fail("This shouldn't fail");
        }
    }

}
