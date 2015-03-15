package kvstore;

import static kvstore.KVConstants.ABORT;
import static kvstore.KVConstants.ACK;
import static kvstore.KVConstants.COMMIT;
import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_INVALID_FORMAT;
import static kvstore.KVConstants.ERROR_NO_SUCH_KEY;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.READY;
import static kvstore.KVConstants.REGISTER;
import static kvstore.KVConstants.RESP;
import static kvstore.KVConstants.SUCCESS;
import static kvstore.TPCMaster.TIMEOUT;

import java.io.IOException;
import java.net.Socket;

/**
 * Implements NetworkHandler to handle 2PC operation requests from the Master/
 * Coordinator Server
 */
public class TPCMasterHandler implements NetworkHandler {

    public long slaveID;
    public KVServer kvServer;
    public TPCLog tpcLog;
    public ThreadPool threadpool;

    public static int REGISTRATION_PORT = 9090;

    /**
     * Constructs a TPCMasterHandler with one connection in its ThreadPool
     *
     * @param slaveID the ID for this slave server
     * @param kvServer KVServer for this slave
     * @param log the log for this slave
     */
    public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log) {
        this(slaveID, kvServer, log, 1);
    }

    /**
     * Constructs a TPCMasterHandler with a variable number of connections
     * in its ThreadPool
     *
     * @param slaveID the ID for this slave server
     * @param kvServer KVServer for this slave
     * @param log the log for this slave
     * @param connections the number of connections in this slave's ThreadPool
     */
    public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log, int connections) {
        this.slaveID = slaveID;
        this.kvServer = kvServer;
        this.tpcLog = log;
        this.threadpool = new ThreadPool(connections);
    }

    /**
     * Registers this slave server with the master.
     *
     * @param masterHostname
     * @param server SocketServer used by this slave server (which contains the
     *               hostname and port this slave is listening for requests on
     * @throws KVException with ERROR_INVALID_FORMAT if the response from the
     *         master is received and parsed but does not correspond to a
     *         success as defined in the spec OR any other KVException such
     *         as those expected in KVClient in project 3 if unable to receive
     *         and/or parse message
     */
    public void registerWithMaster(String masterHostname, SocketServer server)
            throws KVException {
        Socket sock;
        try {
            sock = new Socket(masterHostname, REGISTRATION_PORT);
        } catch (IOException e) {
            throw new KVException(ERROR_COULD_NOT_CREATE_SOCKET);
        }

        try {
            KVMessage regReq = new KVMessage(REGISTER);
            String info = Long.toString(slaveID) + "@" + server.getHostname() +
                          ":" + Integer.toString(server.getPort());
            regReq.setMessage(info);
            regReq.sendMessage(sock);

            KVMessage regResp = new KVMessage(sock, TIMEOUT);
            if (!(RESP).equals(regResp.getMsgType()) || 
                !("Successfully registered " + info).equals(regResp.getMessage())) {
                throw new KVException(ERROR_INVALID_FORMAT);
            }
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                return;
            }
        }
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param master Socket connected to the master with the request
     */
    @Override
    public void handle(Socket master) {
        try {
            threadpool.addJob(createJob(master));
        } catch (InterruptedException e) { }
    }

    /**
     * Closes the handler by cleaning up the thread pool.
     */
    public void close() {
        threadpool.close();
    }

    private Runnable createJob(Socket master) {
        final Socket masterFinal = master;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                KVMessage resp;
                try {
                    KVMessage rqst = new KVMessage(masterFinal, TIMEOUT);
                    String rqstType = rqst.getMsgType();

                    if (GET_REQ.equals(rqstType)) {
                        resp = new KVMessage(RESP);
                        resp.setKey(rqst.getKey());
                        resp.setValue(kvServer.get(rqst.getKey()));
                    } else if (PUT_REQ.equals(rqstType)) {
                        String respMsg = kvServer.validateKeyValue(rqst.getKey(), 
                                                                   rqst.getValue());
                        if (SUCCESS.equals(respMsg)) {
                            tpcLog.appendAndFlush(rqst);
                            resp = new KVMessage(READY);
                        } else {
                            resp = new KVMessage(ABORT);
                            resp.setMessage(respMsg);
                        }
                    } else if (DEL_REQ.equals(rqstType)) {
                        if (kvServer.hasKey(rqst.getKey())) {
                            tpcLog.appendAndFlush(rqst);
                            resp = new KVMessage(READY);
                        } else {
                            resp = new KVMessage(ABORT);
                            resp.setMessage(ERROR_NO_SUCH_KEY);
                        }
                    } else if (COMMIT.equals(rqstType)) {
                        KVMessage exec = tpcLog.getLastEntry();
                        if (PUT_REQ.equals(exec.getMsgType())) {
                            tpcLog.appendAndFlush(rqst);
                            kvServer.put(exec.getKey(), exec.getValue());
                        } else if (DEL_REQ.equals(exec.getMsgType())) {
                            tpcLog.appendAndFlush(rqst);
                            kvServer.del(exec.getKey());
                        }
                        resp = new KVMessage(ACK);
                    } else if (ABORT.equals(rqstType)) {
                        tpcLog.appendAndFlush(rqst);
                        resp = new KVMessage(ACK);
                    } else {
                        throw new KVException(ERROR_INVALID_FORMAT);
                    }
                } catch (KVException e) {
                    resp = e.getKVMessage(); 
                }
                try {
                    resp.sendMessage(masterFinal);
                } catch (KVException e) {
                    // Wait for timeout.
                }
            }
        };
        return r;
    }

}
