package kvstore;

import static kvstore.KVConstants.ERROR_COULD_NOT_CONNECT;
import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_INVALID_FORMAT;
import static kvstore.KVConstants.ERROR_SOCKET_TIMEOUT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Data structure to maintain information about SlaveServers
 */
public class TPCSlaveInfo {

    public long slaveID;
    public String hostname;
    public int port;

    /**
     * Construct a TPCSlaveInfo to represent a slave server.
     *
     * @param info as "SlaveServerID@Hostname:Port"
     * @throws KVException ERROR_INVALID_FORMAT if info string is invalid
     */
    public TPCSlaveInfo(String info) throws KVException {
        if (info == null || info.length() < 5) {
            throw new KVException(ERROR_INVALID_FORMAT);
        } else if (!info.matches("^-{0,1}([0-9])+@([^\\s@:]+):([0-9])+$")) {
            throw new KVException(ERROR_INVALID_FORMAT);
        }
        String[] tok = info.split("[@:]");
        if (tok.length == 3) {
            try {
                slaveID = Long.parseLong(tok[0]);
                hostname = tok[1];
                port = Integer.parseInt(tok[2]);
            } catch (NumberFormatException e) {
                throw new KVException(ERROR_INVALID_FORMAT);
            }
        } else {
            throw new KVException(ERROR_INVALID_FORMAT);
        }
    }

    public long getSlaveID() {
        return slaveID;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    /**
     * Create and connect a socket within a certain timeout.
     *
     * @return Socket object connected to SlaveServer, with timeout set
     * @throws KVException ERROR_SOCKET_TIMEOUT, ERROR_COULD_NOT_CREATE_SOCKET,
     *         or ERROR_COULD_NOT_CONNECT
     */
    public Socket connectHost(int timeout) throws KVException {
        Socket socket;
        try {
            socket = new Socket();
        } catch (Exception e) {
            throw new KVException(ERROR_COULD_NOT_CREATE_SOCKET);
        }
        try {
            socket.connect(new InetSocketAddress(hostname, port), timeout);
        } catch (SocketTimeoutException e) {
            throw new KVException(ERROR_SOCKET_TIMEOUT);
        } catch (IOException e) {
            throw new KVException(ERROR_COULD_NOT_CONNECT);
        }
        return socket;
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param sock Socket to be closed
     */
    public void closeHost(Socket sock) {
        try {
            sock.close();
        } catch (Exception e) {
            return;
        }
    }

    public String toString() {
        return Long.toString(slaveID) + "@" + hostname + ":" +
               Integer.toString(port);
    }

}
