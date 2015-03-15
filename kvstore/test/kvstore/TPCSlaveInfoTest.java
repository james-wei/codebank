package kvstore;

import static kvstore.KVConstants.ERROR_COULD_NOT_CONNECT;
import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_INVALID_FORMAT;
import static kvstore.KVConstants.ERROR_SOCKET_TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InetSocketAddress.class, Socket.class, TPCSlaveInfo.class})
public class TPCSlaveInfoTest {

    @Test
    public void simpleConstructorTest1() throws KVException {
        TPCSlaveInfo slaveInfo = new TPCSlaveInfo("123456789@somehostname:99");
        assertNotNull(slaveInfo);
        assertEquals(123456789L, slaveInfo.getSlaveID());
        assertEquals("somehostname", slaveInfo.getHostname());
        assertEquals(99, slaveInfo.getPort());        
    }

    @Test
    public void simpleConstructorTest2() throws KVException {
        TPCSlaveInfo slaveInfo = new TPCSlaveInfo("123456789@s0me/test.host_name*:99");
        assertNotNull(slaveInfo);
        assertEquals(123456789L, slaveInfo.getSlaveID());
        assertEquals("s0me/test.host_name*", slaveInfo.getHostname());
        assertEquals(99, slaveInfo.getPort());        
    }

    @Test
    public void shortStringConstructorTest() throws KVException {
        TPCSlaveInfo slaveInfo = new TPCSlaveInfo("0@a:1");
        assertNotNull(slaveInfo);
        assertEquals(0L, slaveInfo.getSlaveID());
        assertEquals("a", slaveInfo.getHostname());
        assertEquals(1, slaveInfo.getPort());
    }

    @Test
    public void longStringConstructorTest1() throws KVException {
        long id = Long.MAX_VALUE;
        String hostname = "aaaaaaaaaa";
        for (int i = 0; i < 6; i++) {
            hostname = hostname + hostname;
        }
        int port = Integer.MAX_VALUE;
        String infoStr = Long.toString(id) + "@" + hostname + ":" + 
                         Integer.toString(port);
        TPCSlaveInfo slaveInfo = new TPCSlaveInfo(infoStr);
        assertNotNull(slaveInfo);
        assertEquals(id, slaveInfo.getSlaveID());
        assertEquals(hostname, slaveInfo.getHostname());
        assertEquals(port, slaveInfo.getPort());
    }

    @Test
    public void longStringConstructorTest2() throws KVException {
        long id = Long.MIN_VALUE;
        String hostname = "aaaaaaaaaa";
        for (int i = 0; i < 6; i++) {
            hostname = hostname + hostname;
        }
        int port = Integer.MAX_VALUE;
        String infoStr = Long.toString(id) + "@" + hostname + ":" + 
                         Integer.toString(port);
        TPCSlaveInfo slaveInfo = new TPCSlaveInfo(infoStr);
        assertNotNull(slaveInfo);
        assertEquals(id, slaveInfo.getSlaveID());
        assertEquals(hostname, slaveInfo.getHostname());
        assertEquals(port, slaveInfo.getPort());
    }

    @Test
    public void negativeIdConstructorTest() throws KVException {
        long id = Long.MIN_VALUE;
        String hostname = "aaaaaaaaaa";
        int port = 50;
        String infoStr = Long.toString(id) + "@" + hostname + ":" + 
                         Integer.toString(port);
        TPCSlaveInfo slaveInfo = new TPCSlaveInfo(infoStr);
        assertNotNull(slaveInfo);
        assertEquals(id, slaveInfo.getSlaveID());
        assertEquals(hostname, slaveInfo.getHostname());
        assertEquals(port, slaveInfo.getPort());
    }

    @Test
    public void negativePortConstructorTest() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        int port = -22;
        String infoStr = Long.toString(id) + "@" + hostname + ":" + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badIdConstructorTest() throws KVException {
        String hostname = "aaaaaaaaaa";
        int port = 10;
        String infoStr = "123XYZ456" + "@" + hostname + ":" + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badPortConstructorTest1() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        String infoStr = Long.toString(id) + "@" + hostname + ":" + 
                         "1X2Y3Z";
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badPortConstructorTest2() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        String infoStr = Long.toString(id) + "@" + hostname + ":" + 
                         "123XYZ";
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest1() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        int port = 22;
        String infoStr = Long.toString(id) + ":" + hostname + "@" + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest2() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        int port = 22;
        String infoStr = Long.toString(id) + "@@@" + hostname + ":::" + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest3() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        int port = 22;
        String infoStr = Long.toString(id) + "@aaa:aaa@" + hostname + ":" + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest4() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        int port = 22;
        String infoStr = Long.toString(id) + hostname + Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest5() throws KVException {
        long id = 500L;
        String hostname = "aaaaaaaaaa";
        int port = 22;
        String infoStr = Long.toString(id) + ":" + hostname + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest6() throws KVException {
        long id = 500L;
        String hostname = "aa aaaaa  aaa";
        int port = 22;
        String infoStr = Long.toString(id) + ":" + hostname + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest7() throws KVException {
        String hostname = "aaaaaaaaaa";
        int port = 22;
        String infoStr = "--123456789@" + hostname + ":" + 
                         Integer.toString(port);
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void badFormatConstructorTest8() throws KVException {
        String hostname = "aaaaaaaaaa";
        int port = 22;
        String infoStr = "123456789@" + hostname + ":" + 
                         Integer.toString(port) + "\n";
        try {
            new TPCSlaveInfo(infoStr);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void connectHostSimpleTest() throws KVException {
        try {
            Socket sockMock = mock(Socket.class);
            InetSocketAddress sockAddrMock = mock(InetSocketAddress.class);
            PowerMockito.whenNew(Socket.class).
                withNoArguments().
                thenReturn(sockMock);
            PowerMockito.whenNew(InetSocketAddress.class).
                withParameterTypes(String.class, int.class).
                withArguments(anyString(), anyInt()).
                thenReturn(sockAddrMock);
            doNothing().when(sockMock).connect(refEq(sockAddrMock), anyInt());
            TPCSlaveInfo slaveInfo = new TPCSlaveInfo("123456789@somehostname:99");
            Socket sock = slaveInfo.connectHost(5000);
            assertEquals(sockMock, sock);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

    @Test
    public void cannotCreateSocketTest() throws KVException {
        try {
            PowerMockito.whenNew(Socket.class).
                withNoArguments().
                thenThrow(new Exception());
            TPCSlaveInfo slaveInfo = new TPCSlaveInfo("123456789@somehostname:99");
            slaveInfo.connectHost(5000);
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CREATE_SOCKET");
        } catch (KVException e) {
            assertEquals(ERROR_COULD_NOT_CREATE_SOCKET, e.getKVMessage().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CREATE_SOCKET");
        }
    }

    @Test
    public void socketTimeoutTest() throws KVException {
        try {
            Socket sockMock = mock(Socket.class);
            InetSocketAddress sockAddrMock = mock(InetSocketAddress.class);
            PowerMockito.whenNew(Socket.class).
                withNoArguments().
                thenReturn(sockMock);
            PowerMockito.whenNew(InetSocketAddress.class).
                withParameterTypes(String.class, int.class).
                withArguments(anyString(), anyInt()).
                thenReturn(sockAddrMock);
            PowerMockito.doThrow(new SocketTimeoutException()).
                when(sockMock).connect(refEq(sockAddrMock), anyInt());
            TPCSlaveInfo slaveInfo = new TPCSlaveInfo("123456789@somehostname:99");
            slaveInfo.connectHost(5000);
            fail("Should have thrown a KVException: ERROR_SOCKET_TIMEOUT");
        } catch (KVException e) {
            assertEquals(ERROR_SOCKET_TIMEOUT, e.getKVMessage().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should have thrown a KVException: ERROR_SOCKET_TIMEOUT");
        }
    }

    @Test
    public void couldNotConnectTest() throws KVException {
        try {
            Socket sockMock = mock(Socket.class);
            InetSocketAddress sockAddrMock = mock(InetSocketAddress.class);
            PowerMockito.whenNew(Socket.class).
                withNoArguments().
                thenReturn(sockMock);
            PowerMockito.whenNew(InetSocketAddress.class).
                withParameterTypes(String.class, int.class).
                withArguments(anyString(), anyInt()).
                thenReturn(sockAddrMock);
            PowerMockito.doThrow(new IOException()).
                when(sockMock).connect(refEq(sockAddrMock), anyInt());
            TPCSlaveInfo slaveInfo = new TPCSlaveInfo("123456789@somehostname:99");
            slaveInfo.connectHost(5000);
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CONNECT");
        } catch (KVException e) {
            assertEquals(ERROR_COULD_NOT_CONNECT, e.getKVMessage().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CONNECT");
        }
    }

    @Test
    public void closeHostTest() throws KVException {
        try {
            Socket sockMock = mock(Socket.class);
            doNothing().when(sockMock).close();
            TPCSlaveInfo slaveInfo = new TPCSlaveInfo("123456789@somehostname:99");
            slaveInfo.closeHost(sockMock);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

    @Test
    public void closeBadHostTest() throws KVException {
        try {
            Socket sockMock = mock(Socket.class);
            PowerMockito.doThrow(new IOException()).when(sockMock).close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

}
