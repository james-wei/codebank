package kvstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import kvstore.TPCMaster.SlaveList;
import kvstore.TPCMaster.SlaveNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TPCMaster.class, SlaveList.class, SlaveNode.class, TPCSlaveInfo.class})
public class SlaveListTest {

    @Test
    public void emptyListTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());
    }

    @Test
    public void addSlaveSimpleTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());
        try {
            list.addSlave(new TPCSlaveInfo("100@hostname:123"));
        } catch (KVException e) {
            fail("Should not have thrown an exception");
        }
        assertEquals(1, list.size());
        assertNotNull(list.getFront());
        SlaveNode front = list.getFront();
        assertEquals(100L, front.getInfo().getSlaveID());
        assertEquals("hostname", front.getInfo().getHostname());
        assertEquals(123, front.getInfo().getPort());
    }

    @Test
    public void addSlavesInOrderTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        String info = "@192.168.1.1:5000";
        String currInfo = null;
        TPCSlaveInfo slave = null;
        for (int i = 1; i <= 10; i++) {
            currInfo = Integer.toString(i) + info;
            try {
                slave = new TPCSlaveInfo(currInfo);
                list.addSlave(slave);
                assertEquals(i, list.size());
                assertNotNull(list.getFront());
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        int visitedNodes = 0;
        long greatestIdSeen = 0;
        SlaveNode currNode = list.getFront();
        while (!list.isHead(currNode)) {
            if (currNode.getInfo().getSlaveID() < greatestIdSeen) {
                fail("List is out of order.");
            }
            greatestIdSeen = currNode.getInfo().getSlaveID();
            visitedNodes++;
            assertEquals(greatestIdSeen, visitedNodes);
            currNode = currNode.getNext();
        }
        assertEquals(10L, greatestIdSeen);
        assertEquals(10, visitedNodes);
    }

    @Test
    public void addSlavesReverseOrderTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        String info = "@192.168.1.1:5000";
        String currInfo = null;
        TPCSlaveInfo slave = null;
        for (int i = 10; i > 0; i--) {
            currInfo = Integer.toString(i) + info;
            try {
                slave = new TPCSlaveInfo(currInfo);
                list.addSlave(slave);
                assertEquals(11 - i, list.size());
                assertNotNull(list.getFront());
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        int visitedNodes = 0;
        long greatestIdSeen = 0;
        SlaveNode currNode = list.getFront();
        while (!list.isHead(currNode)) {
            if (currNode.getInfo().getSlaveID() < greatestIdSeen) {
                fail("List is out of order.");
            }
            greatestIdSeen = currNode.getInfo().getSlaveID();
            visitedNodes++;
            assertEquals(greatestIdSeen, visitedNodes);
            currNode = currNode.getNext();
        }
        assertEquals(10L, greatestIdSeen);
        assertEquals(10, visitedNodes);
    }

    @Test
    public void addSlavesRandomOrderTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        int[] ids = {5, 2, 7, 9, 10, 1, 8, 4, 3, 6};
        String info = "@192.168.1.1:5000";
        String currInfo = null;
        TPCSlaveInfo slave = null;
        int count = 0;
        for (int i : ids) {
            currInfo = Integer.toString(i) + info;
            try {
                slave = new TPCSlaveInfo(currInfo);
                list.addSlave(slave);
                count++;
                assertEquals(count, list.size());
                assertNotNull(list.getFront());
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        int visitedNodes = 0;
        long greatestIdSeen = 0;
        SlaveNode currNode = list.getFront();
        while (!list.isHead(currNode)) {
            if (currNode.getInfo().getSlaveID() < greatestIdSeen) {
                fail("List is out of order.");
            }
            greatestIdSeen = currNode.getInfo().getSlaveID();
            visitedNodes++;
            assertEquals(greatestIdSeen, visitedNodes);
            currNode = currNode.getNext();
        }
        assertEquals(10L, greatestIdSeen);
        assertEquals(10, visitedNodes);
    }

    @Test
    public void addDuplicateSlavesTest() {
        TPCSlaveInfo slave = null;
        try {
            slave = new TPCSlaveInfo("9999@192.168.1.1:5000");
        } catch (KVException e) {
            fail("Should not have thrown an exception.");
        }
        assertNotNull(slave);

        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        list.addSlave(slave);
        assertEquals(1, list.size());
        assertNotNull(list.getFront());
        SlaveNode front = list.getFront();
        assertEquals(9999L, front.getInfo().getSlaveID());
        assertEquals("192.168.1.1", front.getInfo().getHostname());
        assertEquals(5000, front.getInfo().getPort());

        for (int i = 0; i < 10; i++) {
            list.addSlave(slave);
            assertEquals(1, list.size());
            assertEquals(front, list.getFront());
            assertTrue(list.isHead(front.getNext()));
            assertTrue(list.isHead(front.getPrev()));
        }
    }

    @Test
    public void removeSlaveSimpleTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());
        try {
            list.addSlave(new TPCSlaveInfo("100@hostname:123"));
        } catch (KVException e) {
            fail("Should not have thrown an exception");
        }
        assertEquals(1, list.size());
        assertNotNull(list.getFront());
        TPCSlaveInfo front = list.getFront().getInfo();
        list.removeSlave(front);
        assertEquals(0, list.size());
        assertNull(list.getFront());
    }

    @Test
    public void removeSlavesInOrderTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        String info = "@192.168.1.1:5000";
        String currInfo = null;
        TPCSlaveInfo slave = null;
        for (int i = 1; i <= 10; i++) {
            currInfo = Integer.toString(i) + info;
            try {
                slave = new TPCSlaveInfo(currInfo);
                list.addSlave(slave);
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        long largestIdSeen = 0;
        TPCSlaveInfo currSlave = null;
        for (int i = 10; i > 0; i--) {
            assertEquals(i, list.size());
            assertNotNull(list.getFront());
            currSlave = list.getFront().getInfo();
            assertNotNull(currSlave);
            if (largestIdSeen > currSlave.getSlaveID()) {
                fail("List is out of order.");
            }
            largestIdSeen = currSlave.getSlaveID();
            list.removeSlave(currSlave);
            assertEquals(i - 1, list.size());
            currSlave = null;
        }
        assertEquals(0, list.size());
        assertNull(list.getFront());
    }

    @Test
    public void removeSlavesReverseOrderTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        String info = "@192.168.1.1:5000";
        String currInfo = null;
        TPCSlaveInfo slave = null;
        for (int i = 1; i <= 10; i++) {
            currInfo = Integer.toString(i) + info;
            try {
                slave = new TPCSlaveInfo(currInfo);
                list.addSlave(slave);
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        SlaveNode head = list.getFront().getPrev();

        long smallestIdSeen = 11;
        TPCSlaveInfo currSlave = null;
        for (int i = 10; i > 0; i--) {
            assertEquals(i, list.size());
            assertNotNull(list.getFront());
            currSlave = head.getPrev().getInfo();
            assertNotNull(currSlave);
            if (smallestIdSeen < currSlave.getSlaveID()) {
                fail("List is out of order.");
            }
            smallestIdSeen = currSlave.getSlaveID();
            list.removeSlave(currSlave);
            assertEquals(i - 1, list.size());
            currSlave = null;
        }
        assertEquals(0, list.size());
        assertNull(list.getFront());
    }

    @Test
    public void removeSlavesRandomOrderTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        String info = "@192.168.1.1:5000";
        String currInfo = null;
        int[] ids = {5, 2, 7, 9, 10, 1, 8, 4, 3, 6};
        TPCSlaveInfo[] slaves = new TPCSlaveInfo[10];
        for (int i = 0; i < 10; i++) {
            try {
                currInfo = Integer.toString(ids[i]) + info;
                slaves[i] = new TPCSlaveInfo(currInfo);
                list.addSlave(slaves[i]);
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        for (int i = 10; i > 0; i--) {
            assertEquals(i, list.size());
            assertNotNull(list.getFront());
            list.removeSlave(slaves[i - 1]);
            assertEquals(i - 1, list.size());
        }
        assertEquals(0, list.size());
        assertNull(list.getFront());
    }

    @Test
    public void removeBadSlavesTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        String info = "@192.168.1.1:5000";
        String currInfo = null;
        TPCSlaveInfo slave = null;
        for (int i = 1; i <= 10; i++) {
            currInfo = Integer.toString(i) + info;
            try {
                slave = new TPCSlaveInfo(currInfo);
                list.addSlave(slave);
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        list.removeSlave(null);
        assertEquals(10, list.size());

        TPCSlaveInfo badSlave = null;
        try {
            badSlave = new TPCSlaveInfo("11@192.168.1.1:5000");
        } catch (KVException e) {
            fail("Should not have thrown an exception.");
        }
        for (int i = 0; i < 10; i++) {
            list.removeSlave(badSlave);
            assertEquals(10, list.size());
        }
    }

    @Test
    public void stressTest() {
        SlaveList list = new SlaveList();
        assertEquals(0, list.size());
        assertNull(list.getFront());

        String info = "@192.168.1.1:5000";
        String currInfo = null;
        TPCSlaveInfo slave = null;
        for (int i = 1; i < 101; i++) {
            currInfo = Integer.toString(i) + info;
            try {
                slave = new TPCSlaveInfo(currInfo);
                list.addSlave(slave);
                assertEquals(i, list.size());
                assertNotNull(list.getFront());
            } catch (KVException e) {
                fail("Should not have thrown an exception");
            }
        }

        for (int i = 100; i > 0; i--) {
            assertNotNull(list.getFront());
            slave = list.getFront().getInfo();
            assertEquals(i, list.size());
            list.removeSlave(slave);
            assertEquals(i - 1, list.size());
        }

        assertNull(list.getFront());
    }

}
