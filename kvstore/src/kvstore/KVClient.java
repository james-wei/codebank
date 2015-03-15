package kvstore;

import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_COULD_NOT_RECEIVE_DATA;
import static kvstore.KVConstants.ERROR_NO_SUCH_KEY;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.SUCCESS;
import static kvstore.TPCMaster.TIMEOUT;

import java.io.IOException;
import java.net.Socket;

/**
 * Client API used to issue requests to key-value server.
 */
public class KVClient implements KeyValueInterface {

    public String server;
    public int port;

    /**
     * Constructs a KVClient connected to a server.
     *
     * @param server is the DNS reference to the server
     * @param port is the port on which the server is listening
     */
    public KVClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Creates a socket connected to the server to make a request.
     *
     * @return Socket connected to server
     * @throws KVException if unable to create or connect socket
     */
    public Socket connectHost() throws KVException {
        Socket clientSocket;
        try {
            clientSocket = new Socket(server, port);
        } catch (IOException e) {
            throw new KVException(ERROR_COULD_NOT_CREATE_SOCKET);
        }
        return clientSocket;
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param  sock Socket to be closed
     */
    public void closeHost(Socket sock) {
        try {
            sock.close();
        } catch (Exception e) {
            return;
        } 
    }

    /**
     * Issues a PUT request to the server.
     *
     * @param  key String to put in server as key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void put(String key, String value) throws KVException {
        KVMessage rqst, resp;
        Socket sock = connectHost();        
        try {
            rqst = new KVMessage(PUT_REQ);
            rqst.setKey(key);
            rqst.setValue(value);
            rqst.sendMessage(sock);
            resp = new KVMessage(sock, TIMEOUT);
            if (SUCCESS.equals(resp.getMessage())) {
                return;
            } else if (resp.getMessage() != null) {
                throw new KVException(resp.getMessage());
            } else {
                throw new KVException(ERROR_COULD_NOT_RECEIVE_DATA);
            }
        } finally {
            closeHost(sock);
        }
    }

    /**
     * Issues a GET request to the server.
     *
     * @param  key String to get value for in server
     * @return String value associated with key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public String get(String key) throws KVException {
        KVMessage rqst, resp;
        Socket sock;
        sock = connectHost();
        try {
            rqst = new KVMessage(GET_REQ);
            rqst.setKey(key);
            rqst.sendMessage(sock);
            resp = new KVMessage(sock, TIMEOUT);
            if (resp.getKey() != null && 
                resp.getValue() != null) {
                return resp.getValue();
            } else if (resp.getMessage() != null) {
                throw new KVException(resp.getMessage());
            } else {
                throw new KVException(ERROR_NO_SUCH_KEY);
            }
        } finally {
            closeHost(sock);
        }
    }

    /**
     * Issues a DEL request to the server.
     *
     * @param  key String to delete value for in server
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void del(String key) throws KVException {
        KVMessage rqst, resp;
        Socket sock = connectHost();
        try {
            rqst = new KVMessage(DEL_REQ);
            rqst.setKey(key);
            rqst.sendMessage(sock);
            resp = new KVMessage(sock, TIMEOUT);
            if (SUCCESS.equals(resp.getMessage())) {
                return;
            } else if (resp.getMessage() != null) {
                throw new KVException(resp.getMessage());
            } else {
                throw new KVException(ERROR_NO_SUCH_KEY);
            }
        } finally {
            closeHost(sock);
        }
    }

}
