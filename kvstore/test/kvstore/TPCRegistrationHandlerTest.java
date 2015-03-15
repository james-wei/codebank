package kvstore;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.Socket;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Socket.class, ThreadPool.class, TPCMaster.class, TPCRegistrationHandler.class})
public class TPCRegistrationHandlerTest {

    @Test
    public void handleTest() {
        try {
            TPCMaster masterMock = mock(TPCMaster.class);
            Socket sockMock = mock(Socket.class);
            ThreadPool tpMock = mock(ThreadPool.class);
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            doNothing().when(tpMock).addJob(any(Runnable.class));
            TPCRegistrationHandler regHandle = new TPCRegistrationHandler(masterMock);
            regHandle.handle(sockMock);
            verify(tpMock, times(1)).addJob(any(Runnable.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

    @Test
    public void badHandleTest() {
        try {
            TPCMaster masterMock = mock(TPCMaster.class);
            Socket sockMock = mock(Socket.class);
            ThreadPool tpMock = mock(ThreadPool.class);
            PowerMockito.whenNew(ThreadPool.class).
                withParameterTypes(int.class).
                withArguments(anyInt()).
                thenReturn(tpMock);
            doThrow(new InterruptedException()).when(tpMock).addJob(any(Runnable.class));
            TPCRegistrationHandler regHandle = new TPCRegistrationHandler(masterMock);
            regHandle.handle(sockMock);
            verify(tpMock, times(1)).addJob(any(Runnable.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
    }

}
