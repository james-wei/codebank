package kvstore;

import static kvstore.KVConstants.ERROR_INVALID_FORMAT;
import static kvstore.KVConstants.REGISTER;
import static kvstore.KVConstants.RESP;
import static kvstore.TPCMaster.TIMEOUT;

import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class TPCRegistrationHandler implements NetworkHandler {

    private ThreadPool threadpool;
    private TPCMaster master;

    /**
     * Constructs a TPCRegistrationHandler with a ThreadPool of a single thread.
     *
     * @param master TPCMaster to register slave with
     */
    public TPCRegistrationHandler(TPCMaster master) {
        this(master, 1);
    }

    /**
     * Constructs a TPCRegistrationHandler with ThreadPool of thread equal to the
     * number given as connections.
     *
     * @param master TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCRegistrationHandler(TPCMaster master, int connections) {
        this.threadpool = new ThreadPool(connections);
        this.master = master;
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param slave Socket connected to the slave with the request
     */
    @Override
    public void handle(Socket slave) {
        try {
            threadpool.addJob(createJob(slave));
        } catch (InterruptedException e) { }
    }

    /**
     * Closes the handler by cleaning up the thread pool.
     */
    public void close() {
        threadpool.close();
    }

    private Runnable createJob(Socket slave) {
        final Socket slaveFinal = slave;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                KVMessage resp = new KVMessage(RESP);
                try {
                    KVMessage rqst = new KVMessage(slaveFinal, TIMEOUT);
                    if (REGISTER.equals(rqst.getMsgType())) {
                        master.registerSlave(new TPCSlaveInfo(rqst.getMessage()));
                        resp.setMessage("Successfully registered " + rqst.getMessage());
                    } else {
                        throw new KVException(ERROR_INVALID_FORMAT);
                    }
                } catch (KVException e) {
                    resp = e.getKVMessage(); 
                }
                try {
                    resp.sendMessage(slaveFinal);
                } catch (KVException e) {
                    // Wait for timeout.
                }
            }
        };
        return r;
    }

}
