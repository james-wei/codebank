package kvstore;

import static kvstore.KVConstants.ABORT;
import static kvstore.KVConstants.ACK;
import static kvstore.KVConstants.COMMIT;
import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.ERROR_INVALID_FORMAT;
import static kvstore.KVConstants.ERROR_INVALID_KEY;
import static kvstore.KVConstants.ERROR_NO_SUCH_KEY;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.READY;
import static kvstore.KVConstants.RESP;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class TPCMaster {

    public int numSlaves;
    public KVCache masterCache;

    public static final int TIMEOUT = 3000;

    /**
     * Keep track of number of registered slaves. Modifications to numRegistered
     * are locked by numRegisteredLock.
     */
    private int numRegistered;
    private final Object numRegisteredLock;

    /**
     * A custom data-structure (linked list of SlaveNode objects) that holds
     * all registered slaves (TPCSlaveInfo objects) in sorted order by
     * ascending slave IDs.
     */
    private SlaveList slaveList;

    /**
     * Maps slave ID to corresponding TPCSlaveInfo object.
     * Allows for constant time performance in getSlave(long).
     */
    private Map<Long, TPCSlaveInfo> slaveIdMap;

    /**
     * Maps a TPCSlaveInfo object to its corresponding SlaveNode entry in
     * this.slaveList.
     * Allows for constant time lookup of the successor slave.
     */
    private Map<TPCSlaveInfo, SlaveNode> slaveNodeMap;

    /**
     * Creates TPCMaster, expecting numSlaves slave servers to eventually register
     *
     * @param numSlaves number of slave servers expected to register
     * @param cache KVCache to cache results on master
     */
    public TPCMaster(int numSlaves, KVCache cache) {
        this(numSlaves, cache, new SlaveList(),
             new HashMap<Long, TPCSlaveInfo>(),
             new HashMap<TPCSlaveInfo, SlaveNode>());
    }

    /**
     * (For testing only) Creates TPCMaster, setting all instance variables to
     * the parameters provided.
     */
    TPCMaster(int numSlaves, KVCache cache, SlaveList slaveList,
              Map<Long, TPCSlaveInfo> slaveIdMap,
              Map<TPCSlaveInfo, SlaveNode> slaveNodeMap) {
        this.numSlaves = numSlaves;
        this.masterCache = cache;
        this.slaveList = slaveList;
        this.slaveIdMap = slaveIdMap;
        this.slaveNodeMap = slaveNodeMap;
        this.numRegistered = 0;
        this.numRegisteredLock = new Object();
    }

    /**
     * Registers a slave. Drop registration request if numSlaves already
     * registered. Note that a slave re-registers under the same slaveID when
     * it comes back online.
     *
     * @param slave the slaveInfo to be registered
     */
    public void registerSlave(TPCSlaveInfo slave) {
        if (slave == null) {
            return;
        } else if (slaveIdMap.containsKey(slave.getSlaveID())) {
            /* Re-registration. */
            TPCSlaveInfo oldInfo = slaveIdMap.get(slave.getSlaveID());
            SlaveNode node = slaveNodeMap.get(oldInfo);
            node.setInfo(slave);
            slaveIdMap.put(slave.getSlaveID(), slave);
            slaveNodeMap.remove(oldInfo);
            slaveNodeMap.put(slave, node);
        } else if (slaveList.size() >= numSlaves) {
            // Wait for request to time out.
            return;
        } else {
            SlaveNode slaveNode = slaveList.addSlave(slave);
            slaveIdMap.put(slave.getSlaveID(), slave);
            slaveNodeMap.put(slave, slaveNode);

            synchronized (numRegisteredLock) {
                numRegistered++;
                numRegisteredLock.notifyAll();
            }
        }
    }

    /**
     * Converts Strings to 64-bit longs. Borrowed from http://goo.gl/le1o0W,
     * adapted from String.hashCode().
     *
     * @param string String to hash to 64-bit
     * @return long hashcode
     */
    public static long hashTo64bit(String string) {
        long h = 1125899906842597L;
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = (31 * h) + string.charAt(i);
        }
        return h;
    }

    /**
     * Compares two longs as if they were unsigned (Java doesn't have unsigned
     * data types except for char). Borrowed from http://goo.gl/QyuI0V
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than unsigned n2
     */
    public static boolean isLessThanUnsigned(long n1, long n2) {
        return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
    }

    /**
     * Compares two longs as if they were unsigned, uses isLessThanUnsigned
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than or equal to unsigned n2
     */
    public static boolean isLessThanEqualUnsigned(long n1, long n2) {
        return isLessThanUnsigned(n1, n2) || (n1 == n2);
    }

    /**
     * Find primary replica for a given key.
     *
     * @param key String to map to a slave server replica
     * @return SlaveInfo of first replica
     */
    public TPCSlaveInfo findFirstReplica(String key) {
        if (slaveList.size() == 0 || key == null) {
            return null;
        }
        return findSlaveAfterLong(hashTo64bit(key));
    }

    /**
     * Return the slave whose id is the first after n.
     *
     * @param n The number that we are performing unsigned comparisons with
     * @return The requested SlaveTPCSlaveInfo (possibly at the front of slaveList)
     */
    TPCSlaveInfo findSlaveAfterLong(long n) {
        long currId;
        SlaveNode currNode = slaveList.getFront();
        while (!slaveList.isHead(currNode)) {
            currId = currNode.getInfo().getSlaveID();
            if (isLessThanEqualUnsigned(n, currId)) {
                return currNode.getInfo();
            }
            currNode = currNode.getNext();
        }
        return slaveList.getFront().getInfo();
    }

    /**
     * Find the successor of firstReplica.
     *
     * @param firstReplica SlaveInfo of primary replica
     * @return SlaveInfo of successor replica
     */
    public TPCSlaveInfo findSuccessor(TPCSlaveInfo firstReplica) {
        if (slaveList.size() == 0 || firstReplica == null) {
            return null;
        }
        SlaveNode firstNode = slaveNodeMap.get(firstReplica);
        if (firstNode == null) {
            /* firstReplica is not in the list of registered slaves. */
            SlaveNode currNode = slaveList.getFront();
            Long currId,
                 targetId = firstReplica.getSlaveID();
            while (!slaveList.isHead(currNode)) {
                currId = currNode.getInfo().getSlaveID();
                if (isLessThanEqualUnsigned(targetId, currId)) {
                    return currNode.getInfo();
                }
                currNode = currNode.getNext();
            }
        } else if (firstNode.getNext() != null &&
                   firstNode.getNext().getInfo() != null) {
            return firstNode.getNext().getInfo();
        }
        return slaveList.getFront().getInfo();        
    }

    /**
     * @return The number of slaves currently registered.
     */
    public int getNumRegisteredSlaves() {
        return numRegistered;
    }

    /**
     * (For testing only) Attempt to get a registered slave's info by ID.
     * @return The requested TPCSlaveInfo if present, otherwise null.
     */
    TPCSlaveInfo getSlave(long slaveId) {
        return slaveIdMap.get(slaveId);
    }

    /**
     * Perform 2PC operations from the master node perspective. This method
     * contains the bulk of the two-phase commit logic. It performs phase 1
     * and phase 2 with appropriate timeouts and retries.
     *
     * See the spec for details on the expected behavior.
     *
     * @param msg KVMessage corresponding to the transaction for this TPC request
     * @param isPutReq boolean to distinguish put and del requests
     * @throws KVException if the operation cannot be carried out for any reason
     */
    public synchronized void handleTPCRequest(KVMessage msg, boolean isPutReq)
            throws KVException {

        /* Wait until numSlave slaves have been registered before performing
         * any TPC operations. */
        synchronized (numRegisteredLock) {
            while (numRegistered < numSlaves) {
                try {
                    numRegisteredLock.wait();
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        /* Validate the TPC request. */
        String key = msg.getKey();
        if (!isValidTPCRequest(msg)) {
            if (key == null || key.length() == 0) {
                throw new KVException(ERROR_INVALID_KEY);
            } else {
                throw new KVException(ERROR_INVALID_FORMAT);
            }
        }

        /* Find the two slaves to be used for this TPC transaction. */
        SlaveNode[] slaves = new SlaveNode[2];
        slaves[0] = slaveNodeMap.get(findFirstReplica(key));
        slaves[1] = slaveNodeMap.get(findSuccessor(slaves[0].getInfo()));

        TPCSlaveInfo currSlave = null;
        Socket currSocket = null;
        KVMessage phase1Resp = null;
        boolean globalAbort = false;
        String abortMessage = null;

        /* [PHASE 1] Send out vote request to each slave and wait for each of
         * their responses. Determine whether to perform a global commit or a
         * global abort. */
        for (SlaveNode currNode : slaves) {
            if (currNode == null) {
                globalAbort = true;
                continue;
            }
            try {
                currSlave = currNode.getInfo();
                currSocket = currSlave.connectHost(TIMEOUT);
                msg.sendMessage(currSocket);
                phase1Resp = new KVMessage(currSocket, TIMEOUT);
                if (!READY.equals(phase1Resp.getMsgType())) {
                    globalAbort = true;
                    abortMessage = phase1Resp.getMessage();
                }
            } catch (KVException e) {
                globalAbort = true;
                abortMessage = e.getKVMessage().getMessage();
            } catch (Exception e) {
                globalAbort = true;
            } finally {
                if (currSlave != null) {
                    currSlave.closeHost(currSocket);
                }
            }
        }

        KVMessage phase2Resp = null;
        KVMessage phase2Msg = null;
        boolean invalidFormat = false;

        /* [PHASE 2] Send each slave the phase 2 message (either commit or
         * abort). Continue sending the message in TIMEOUT intervals until
         * each slave responds with an ACK. */
        if (globalAbort) {
            phase2Msg = new KVMessage(ABORT);
        } else {
            phase2Msg = new KVMessage(COMMIT);
        }

        for (SlaveNode currNode : slaves) {
            if (currNode == null) {
                continue;
            }
            while (true) {
                try {
                    currSlave = currNode.getInfo();
                    currSocket = currSlave.connectHost(TIMEOUT);
                    phase2Msg.sendMessage(currSocket);
                    phase2Resp = new KVMessage(currSocket, TIMEOUT);
                } catch (KVException e) {
                    try {
                        Thread.sleep(TIMEOUT);
                    } catch (InterruptedException e1) { }
                    continue;
                } finally {
                    if (currSlave != null) {
                        currSlave.closeHost(currSocket);
                    }
                }
                if (!ACK.equals(phase2Resp.getMsgType())) {
                    invalidFormat = true;
                }
                break;
            }
        }

        if (globalAbort) {
            if (abortMessage != null) {
                throw new KVException(abortMessage);
            } else {
                throw new KVException(ERROR_INVALID_FORMAT);
            }
        }

        if (invalidFormat) {
            throw new KVException(ERROR_INVALID_FORMAT);
        }

        /* Flush changes to master cache. */
        if (!globalAbort && !invalidFormat) {
            Lock lock = masterCache.getLock(msg.getKey());
            lock.lock();
            try {
                if (isPutReq) {
                    masterCache.put(msg.getKey(), msg.getValue());
                } else {
                    masterCache.del(msg.getKey());
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Perform GET operation in the following manner:
     * - Try to GET from cache, return immediately if found
     * - Try to GET from first/primary replica
     * - If primary succeeded, return value
     * - If primary failed, try to GET from the other replica
     * - If secondary succeeded, return value
     * - If secondary failed, return KVExceptions from both replicas
     *
     * @param msg KVMessage containing key to get
     * @return value corresponding to the Key
     * @throws KVException with ERROR_NO_SUCH_KEY if unable to get
     *         the value from either slave for any reason
     */
    public String handleGet(KVMessage msg) throws KVException {

        /* Wait until numSlave slaves have been registered before performing
         * any GET operations. */
        synchronized (numRegisteredLock) {
            while (numRegistered < numSlaves) {
                try {
                    numRegisteredLock.wait();
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        /* Validate GET request. */
        String key = msg.getKey();
        if (!isValidGetRequest(msg)) {
            if (key == null || key.length() == 0) {
                throw new KVException(ERROR_NO_SUCH_KEY);
            } else {
                throw new KVException(ERROR_INVALID_FORMAT);
            }
        }

        String value = null;
        Lock lock = masterCache.getLock(key);
        lock.lock();

        try {
            /* Check in cache. */
            value = masterCache.get(key);
            if (value != null) {
                return value;
            }

            /* Check in primary replica. */
            TPCSlaveInfo primary = findFirstReplica(key);
            if (primary != null) {
                value = getValueFromReplica(primary, msg);
            }
            if (value != null) {
                masterCache.put(key, value);
                return value;
            }

            /* Check in secondary replica. */
            TPCSlaveInfo secondary = findSuccessor(primary);
            if (secondary != null) {
                value = getValueFromReplica(secondary, msg);
            }
            if (value != null) {
                masterCache.put(key, value);
                return value;
            }
            throw new KVException(ERROR_NO_SUCH_KEY);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the value from the replica, or return null.
     */
    String getValueFromReplica(TPCSlaveInfo slaveInfo, KVMessage msg) {
        Socket sock;
        KVMessage resp;

        if (slaveInfo == null || msg == null) {
            return null;
        }

        try {
            sock = slaveInfo.connectHost(TIMEOUT);
        } catch (KVException e) {
            return null;
        }
        try {
            msg.sendMessage(sock);
            resp = new KVMessage(sock, TIMEOUT);
            if (RESP.equals(resp.getMsgType())) {
                return resp.getValue();
            }
        } catch (KVException e) {
            // Do nothing
        } finally {
            slaveInfo.closeHost(sock);
        }

        return null;
    }

    /**
     * Checks whether given message is a valid GET request.
     * 
     * @param msg GET request to be validated
     * @return true if given request is valid GET request
     */
    private boolean isValidGetRequest(KVMessage msg) {
        return (msg.getKey() != null &&
               (msg.getKey().length() > 0) &&
               (msg.getValue() == null) &&
               (msg.getMessage() == null));
    }

    /**
     * Checks whether given message is a valid TPC request.
     * 
     * @param msg TPC request to be validated
     * @return true if given request is valid TPC request
     */
    private boolean isValidTPCRequest(KVMessage msg) {
        if (PUT_REQ.equals(msg.getMsgType())) {
            return (msg.getMessage() == null &&
                   (msg.getKey() != null) &&
                   (msg.getKey().length() > 0) &&
                   (msg.getValue() != null));
        }
        else if (DEL_REQ.equals(msg.getMsgType())) {
            return (msg.getMessage() == null &&
                   (msg.getKey() != null) &&
                   (msg.getKey().length() > 0) &&
                   (msg.getValue() == null));
        } else {
            return false;
        }
    }

    /**
     * Circularly-doubly-linked list data structure that maintains a list of
     * TPCSlaveInfo objects in ascending order by slave ID.
     * Nodes in the doubly-linked list are SlaveNode objects.
     */
    static class SlaveList {

        private int size;
        private SlaveNode head;

        SlaveList() {
            size = 0;
            head = new SlaveNode();
            head.setNext(head);
            head.setPrev(head);
        }

        SlaveNode getFront() {
            if (size == 0) {
                return null;
            } else {
                return head.getNext();
            }
        }

        SlaveNode addSlave(TPCSlaveInfo slave) {
            if (slave == null) {
                return null;
            }
            SlaveNode newNode = new SlaveNode(slave);
            if (size == 0) {
                insertBefore(newNode, head);
            } else {
                SlaveNode currNode = getFront();
                while (currNode != head) {
                    if (currNode.compareTo(newNode) < 0) {
                        currNode = currNode.getNext();
                    } else if (currNode.compareTo(newNode) == 0) {
                        /* The slave is already in the list */
                        newNode = currNode;
                        break;
                    } else {
                        insertBefore(newNode, currNode);
                        break;
                    }
                }
                /* Insert at end of list. */
                if (currNode == head) {
                    insertBefore(newNode, head);
                }
            }
            return newNode;
        }

        void removeSlave(TPCSlaveInfo slave) {
            if (size == 0 || slave == null) {
                return;
            }
            SlaveNode delNode = new SlaveNode(slave);
            SlaveNode currNode = head.getNext();
            while (currNode != head) {
                if (currNode.compareTo(delNode) == 0) {
                    currNode.getPrev().setNext(currNode.getNext());
                    currNode.getNext().setPrev(currNode.getPrev());
                    currNode.clearNode();
                    delNode.clearNode();
                    size--;
                    return;
                }
                currNode = currNode.getNext();
            }
        }

        int size() {
            return size;
        }

        boolean isHead(SlaveNode n) {
            return n == head;
        }

        public String toString() {
            String repr = "[SlaveList]\n";
            repr = repr + "Size: " + Integer.toString(size) + "\n";
            int i = 0;
            SlaveNode currNode = getFront();
            if (currNode != null) {
                while (!isHead(currNode)) {
                    repr = repr + Integer.toString(i) + ": " +
                           Long.toString(currNode.getInfo().getSlaveID()) +
                           "\n";
                    currNode = currNode.getNext();
                    i++;
                }
            }
            return repr;
        }

        /**
         * Inserts newNode before node.
         * REQUIRES: node is in this SlaveList.
         */
        private void insertBefore(SlaveNode newNode, SlaveNode node) {
            node.getPrev().setNext(newNode);
            newNode.setPrev(node.getPrev());
            newNode.setNext(node);
            node.setPrev(newNode);
            size++;
        }
    }

    /**
     * Container for elements in SlaveList.
     */
    static class SlaveNode {

        private SlaveNode next;
        private SlaveNode prev;
        private TPCSlaveInfo info;

        SlaveNode() {
            this(null);
        }

        SlaveNode(TPCSlaveInfo info) {
            this.info = info;
            this.next = null;
            this.prev = null;
        }

        SlaveNode getNext() {
            return next;
        }

        void setNext(SlaveNode next) {
            this.next = next;
        }

        SlaveNode getPrev() {
            return prev;
        }

        void setPrev(SlaveNode prev) {
            this.prev = prev;
        }

        TPCSlaveInfo getInfo() {
            return info;
        }

        void setInfo(TPCSlaveInfo info) {
            this.info = info;
        }

        void clearNode() {
            this.next = null;
            this.prev = null;
            this.info = null;
        }

        int compareTo(SlaveNode other) {
            long thisId = this.info.getSlaveID();
            long otherId = other.getInfo().getSlaveID();
            if (TPCMaster.isLessThanUnsigned(thisId, otherId)) {
                return -1;
            } else if (TPCMaster.isLessThanEqualUnsigned(thisId, otherId)) {
                return 0;
            } else {
                return 1;
            }
        }
    }

}
