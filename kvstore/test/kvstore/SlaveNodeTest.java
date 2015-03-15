package kvstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import kvstore.TPCMaster.SlaveNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TPCMaster.class, SlaveNode.class, TPCSlaveInfo.class})
public class SlaveNodeTest {

    @Test
    public void gettersTest() {
        SlaveNode node = new SlaveNode();
        assertNull(node.getInfo());
        assertNull(node.getPrev());
        assertNull(node.getNext());
    }

    @Test
    public void settersTest() {
        SlaveNode prevNode = mock(SlaveNode.class);
        SlaveNode nextNode = mock(SlaveNode.class);
        TPCSlaveInfo info = mock(TPCSlaveInfo.class);
        SlaveNode node = new SlaveNode(info);
        assertEquals(info, node.getInfo());
        assertNull(node.getPrev());
        assertNull(node.getNext());
        node.setPrev(prevNode);
        node.setNext(nextNode);
        assertEquals(info, node.getInfo());
        assertEquals(prevNode, node.getPrev());
        assertEquals(nextNode, node.getNext());
    }

    @Test
    public void clearNodeTest() {
        SlaveNode prevNode = mock(SlaveNode.class);
        SlaveNode nextNode = mock(SlaveNode.class);
        TPCSlaveInfo info = mock(TPCSlaveInfo.class);
        SlaveNode node = new SlaveNode(info);
        node.setPrev(prevNode);
        node.setNext(nextNode);
        assertEquals(info, node.getInfo());
        assertEquals(prevNode, node.getPrev());
        assertEquals(nextNode, node.getNext());
        node.clearNode();
        assertNull(node.getInfo());
        assertNull(node.getPrev());
        assertNull(node.getNext());
    }

    @Test
    public void compareToTest() {
        long id1 = 4611686018427387903L;
        long id2 = 9223372036854775807L;
        long id3 = -4611686018427387903L;
        long id4 = -0000000000000000001;
        long id5 = 6230492013836775123L;

        TPCSlaveInfo info1 = mock(TPCSlaveInfo.class);
        TPCSlaveInfo info2 = mock(TPCSlaveInfo.class);
        TPCSlaveInfo info3 = mock(TPCSlaveInfo.class);
        TPCSlaveInfo info4 = mock(TPCSlaveInfo.class);
        TPCSlaveInfo info5 = mock(TPCSlaveInfo.class);

        when(info1.getSlaveID()).thenReturn(id1);
        when(info2.getSlaveID()).thenReturn(id2);
        when(info3.getSlaveID()).thenReturn(id3);
        when(info4.getSlaveID()).thenReturn(id4);
        when(info5.getSlaveID()).thenReturn(id5);

        SlaveNode slaveNode1 = new SlaveNode(info1);
        SlaveNode slaveNode2 = new SlaveNode(info2);
        SlaveNode slaveNode3 = new SlaveNode(info3);
        SlaveNode slaveNode4 = new SlaveNode(info4);
        SlaveNode slaveNode5 = new SlaveNode(info5);

        assertEquals(0, slaveNode1.compareTo(slaveNode1));
        assertEquals(-1, slaveNode1.compareTo(slaveNode2));
        assertEquals(-1, slaveNode1.compareTo(slaveNode3));
        assertEquals(-1, slaveNode1.compareTo(slaveNode4));
        assertEquals(-1, slaveNode1.compareTo(slaveNode5));

        assertEquals(1, slaveNode2.compareTo(slaveNode1));
        assertEquals(0, slaveNode2.compareTo(slaveNode2));
        assertEquals(-1, slaveNode2.compareTo(slaveNode3));
        assertEquals(-1, slaveNode2.compareTo(slaveNode4));
        assertEquals(1, slaveNode2.compareTo(slaveNode5));

        assertEquals(1, slaveNode3.compareTo(slaveNode1));
        assertEquals(1, slaveNode3.compareTo(slaveNode2));
        assertEquals(0, slaveNode3.compareTo(slaveNode3));
        assertEquals(-1, slaveNode3.compareTo(slaveNode4));
        assertEquals(1, slaveNode3.compareTo(slaveNode5));

        assertEquals(1, slaveNode4.compareTo(slaveNode1));
        assertEquals(1, slaveNode4.compareTo(slaveNode2));
        assertEquals(1, slaveNode4.compareTo(slaveNode3));
        assertEquals(0, slaveNode4.compareTo(slaveNode4));
        assertEquals(1, slaveNode4.compareTo(slaveNode5));

        assertEquals(1, slaveNode5.compareTo(slaveNode1));
        assertEquals(-1, slaveNode5.compareTo(slaveNode2));
        assertEquals(-1, slaveNode5.compareTo(slaveNode3));
        assertEquals(-1, slaveNode5.compareTo(slaveNode4));
        assertEquals(0, slaveNode5.compareTo(slaveNode5));
    }

}
