package kvstore;

import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.ERROR_INVALID_FORMAT;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.RESP;
import static kvstore.KVConstants.SUCCESS;
import static kvstore.TPCMaster.TIMEOUT;

import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * It uses a threadPool to ensure that none of it's methods are blocking.
 */
public class TPCClientHandler implements NetworkHandler {

    public TPCMaster tpcMaster;
    public ThreadPool threadPool;

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     */
    public TPCClientHandler(TPCMaster tpcMaster) {
        this(tpcMaster, 1);
    }

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCClientHandler(TPCMaster tpcMaster, int connections) {
        this.tpcMaster = tpcMaster;
        this.threadPool = new ThreadPool(connections);
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
        try {
            threadPool.addJob(createJob(client));
        } catch (InterruptedException e) { }
    }

    /**
     * Closes the handler by cleaning up the thread pool.
     */
    public void close() {
        threadPool.close();
    }

    private Runnable createJob(Socket client) {
        final Socket clientFinal = client;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                KVMessage resp = new KVMessage(RESP);
                try {
                    KVMessage rqst = new KVMessage(clientFinal, TIMEOUT);
                    if (GET_REQ.equals(rqst.getMsgType())) {
                        resp.setKey(rqst.getKey());
                        resp.setValue(tpcMaster.handleGet(rqst));
                    } else if (PUT_REQ.equals(rqst.getMsgType())) {
                        tpcMaster.handleTPCRequest(rqst, true);
                        resp.setMessage(SUCCESS);
                    } else if (DEL_REQ.equals(rqst.getMsgType())) {
                        tpcMaster.handleTPCRequest(rqst, false);
                        resp.setMessage(SUCCESS);
                    } else {
                        throw new KVException(ERROR_INVALID_FORMAT);
                    }
                } catch (KVException e) {
                    resp = e.getKVMessage(); 
                }
                try {
                    resp.sendMessage(clientFinal);
                } catch (KVException e) {
                    // Wait for timeout.
                }
            }
        };
        return r;
    }

}
