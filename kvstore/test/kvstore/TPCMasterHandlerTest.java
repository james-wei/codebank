package kvstore;

import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_INVALID_FORMAT;
import static kvstore.KVConstants.REGISTER;
import static kvstore.KVConstants.RESP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KVMessage.class, KVServer.class, Socket.class, 
                 SocketServer.class, ThreadPool.class, TPCLog.class, 
                 TPCMasterHandler.class})
public class TPCMasterHandlerTest {

    @Test
    public void handleTest() {
        try {
            KVServer serverMock = mock(KVServer.class);
            Socket sockMock = mock(Socket.class);
            TPCLog logMock = mock(TPCLog.class);
            ThreadPool tpMock = mock(ThreadPool.class);
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            doNothing().when(tpMock).addJob(any(Runnable.class));
            TPCMasterHandler mHandle = new TPCMasterHandler(123L, serverMock, logMock, 10);
            mHandle.handle(sockMock);
            verify(tpMock, times(1)).addJob(any(Runnable.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

    @Test
    public void badHandleTest() {
        try {
            KVServer serverMock = mock(KVServer.class);
            Socket sockMock = mock(Socket.class);
            TPCLog logMock = mock(TPCLog.class);
            ThreadPool tpMock = mock(ThreadPool.class);
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            doThrow(new InterruptedException()).when(tpMock).addJob(any(Runnable.class));
            TPCMasterHandler mHandle = new TPCMasterHandler(123L, serverMock, logMock, 10);
            mHandle.handle(sockMock);
            verify(tpMock, times(1)).addJob(any(Runnable.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

    @Test
    public void registrationTest() {
        try {
            KVMessage rqstMock = mock(KVMessage.class);
            KVMessage respMock = mock(KVMessage.class);
            KVServer serverMock = mock(KVServer.class);
            Socket sockMock = mock(Socket.class);
            SocketServer sockServMock = mock(SocketServer.class);
            TPCLog logMock = mock(TPCLog.class);
            ThreadPool tpMock = mock(ThreadPool.class);
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            PowerMockito.whenNew(Socket.class).
                withParameterTypes(String.class, int.class).
                withArguments(eq("masterHostName"), eq(9090)).
                thenReturn(sockMock);
            PowerMockito.whenNew(KVMessage.class).
                withParameterTypes(String.class).
                withArguments(eq(REGISTER)).
                thenReturn(rqstMock);
            PowerMockito.whenNew(KVMessage.class).
                withParameterTypes(Socket.class, int.class).
                withArguments(refEq(sockMock), anyInt()).
                thenReturn(respMock);
            when(sockServMock.getHostname()).thenReturn("myhostname");
            when(sockServMock.getPort()).thenReturn(51);
            when(respMock.getMsgType()).thenReturn(RESP);
            when(respMock.getMessage()).thenReturn("Successfully registered 123@myhostname:51");
            doNothing().when(sockMock).close();
            TPCMasterHandler mHandle = new TPCMasterHandler(123L, serverMock, logMock, 10);
            mHandle.registerWithMaster("masterHostName", sockServMock);
            verify(sockServMock, atLeastOnce()).getHostname();
            verify(sockServMock, atLeastOnce()).getPort();
            verify(respMock, atLeastOnce()).getMsgType();
            verify(respMock, atLeastOnce()).getMessage();
            verify(sockMock, times(1)).close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

    @Test
    public void couldNotCreateSocketTest() {
        try {
            KVServer serverMock = mock(KVServer.class);
            SocketServer sockServMock = mock(SocketServer.class);
            TPCLog logMock = mock(TPCLog.class);
            ThreadPool tpMock = mock(ThreadPool.class);
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            PowerMockito.whenNew(Socket.class).
                withParameterTypes(String.class, int.class).
                withArguments(eq("masterHostName"), eq(9090)).
                thenThrow(new IOException());
            TPCMasterHandler mHandle = new TPCMasterHandler(123L, serverMock, logMock, 10);
            mHandle.registerWithMaster("masterHostName", sockServMock);
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CREATE_SOCKET");
        } catch (KVException e) {
            assertEquals(ERROR_COULD_NOT_CREATE_SOCKET, e.getKVMessage().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should have thrown a KVException: ERROR_COULD_NOT_CREATE_SOCKET");
        }
    }

    @Test
    public void badRegistrationResponseTest1() {
        KVMessage rqstMock = mock(KVMessage.class);
        KVMessage respMock = mock(KVMessage.class);
        KVServer serverMock = mock(KVServer.class);
        Socket sockMock = mock(Socket.class);
        SocketServer sockServMock = mock(SocketServer.class);
        TPCLog logMock = mock(TPCLog.class);
        ThreadPool tpMock = mock(ThreadPool.class);
        try {
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            PowerMockito.whenNew(Socket.class).
                withParameterTypes(String.class, int.class).
                withArguments(eq("masterHostName"), eq(9090)).
                thenReturn(sockMock);
            PowerMockito.whenNew(KVMessage.class).
                withParameterTypes(String.class).
                withArguments(eq(REGISTER)).
                thenReturn(rqstMock);
            PowerMockito.whenNew(KVMessage.class).
                withParameterTypes(Socket.class, int.class).
                withArguments(refEq(sockMock), anyInt()).
                thenReturn(respMock);
            when(sockServMock.getHostname()).thenReturn("myhostname");
            when(sockServMock.getPort()).thenReturn(51);
            when(respMock.getMsgType()).thenReturn(RESP);
            when(respMock.getMessage()).thenReturn("Successfully registered 1234@myhostname:51");
            doNothing().when(sockMock).close();
            TPCMasterHandler mHandle = new TPCMasterHandler(123L, serverMock, logMock, 10);
            mHandle.registerWithMaster("masterHostName", sockServMock);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            verify(sockServMock, atLeastOnce()).getHostname();
            verify(sockServMock, atLeastOnce()).getPort();
            verify(respMock, atLeastOnce()).getMessage();
            try {
                verify(sockMock, times(1)).close();
            } catch (IOException e1) {
                fail("Should have successfully called closed() even in event of failure");
            }
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        }
    }

    @Test
    public void badRegistrationResponseTest2() {
        KVMessage rqstMock = mock(KVMessage.class);
        KVMessage respMock = mock(KVMessage.class);
        KVServer serverMock = mock(KVServer.class);
        Socket sockMock = mock(Socket.class);
        SocketServer sockServMock = mock(SocketServer.class);
        TPCLog logMock = mock(TPCLog.class);
        ThreadPool tpMock = mock(ThreadPool.class);
        try {
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            PowerMockito.whenNew(Socket.class).
                withParameterTypes(String.class, int.class).
                withArguments(eq("masterHostName"), eq(9090)).
                thenReturn(sockMock);
            PowerMockito.whenNew(KVMessage.class).
                withParameterTypes(String.class).
                withArguments(eq(REGISTER)).
                thenReturn(rqstMock);
            PowerMockito.whenNew(KVMessage.class).
                withParameterTypes(Socket.class, int.class).
                withArguments(refEq(sockMock), anyInt()).
                thenReturn(respMock);
            when(sockServMock.getHostname()).thenReturn("myhostname");
            when(sockServMock.getPort()).thenReturn(51);
            when(respMock.getMsgType()).thenReturn(null);
            when(respMock.getMessage()).thenReturn("Successfully registered 123@myhostname:51");
            doNothing().when(sockMock).close();
            TPCMasterHandler mHandle = new TPCMasterHandler(123L, serverMock, logMock, 10);
            mHandle.registerWithMaster("masterHostName", sockServMock);
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        } catch (KVException e) {
            verify(sockServMock, atLeastOnce()).getHostname();
            verify(sockServMock, atLeastOnce()).getPort();
            verify(respMock, atLeastOnce()).getMsgType();
            try {
                verify(sockMock, times(1)).close();
            } catch (IOException e1) {
                fail("Should have successfully called closed() even in event of failure");
            }
            assertEquals(ERROR_INVALID_FORMAT, e.getKVMessage().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should have thrown a KVException: ERROR_INVALID_FORMAT");
        }
    }

}
