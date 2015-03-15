package kvstore;

import static autograder.TestUtils.kTimeoutQuick;
import static kvstore.KVConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.net.Socket;

import javax.xml.parsers.*;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.w3c.dom.*;
import org.xml.sax.*;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;

public class KVMessageTest {

    private Socket sock;
    private InputStream stream;
    private PrintStream stdErr;
    private static File tempFile;

    @BeforeClass
    public static void setupTempFile() throws IOException {
        tempFile = File.createTempFile("TestKVMessage-", ".txt");
        tempFile.deleteOnExit();
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "KVMessage fields must equal what they were set to")
    public void makeAndCheckMsgContents() {
        String msgTxt = "example message";
        String key = "foo";
        String val = "bar";
        KVMessage msg = new KVMessage(GET_REQ, msgTxt);
        msg.setKey(key);
        msg.setValue(val);
        assertEquals(GET_REQ, msg.getMsgType());
        assertEquals(msgTxt, msg.getMessage());
        assertEquals(key, msg.getKey());
        assertEquals(val, msg.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Must be able to parse del request successfully")
    public void successfullyParsesDelReq() throws KVException {
        sock = Utils.setupReadFromFile("delreq.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(DEL_REQ, kvm.getMsgType());
        assertNull(kvm.getMessage());
        assertNotNull(kvm.getKey());
        assertNull(kvm.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Must be able to parse del response successfully")
    public void successfullyParsesDelResp() throws KVException {
        sock = Utils.setupReadFromFile("delresp.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(RESP, kvm.getMsgType());
        assertTrue(SUCCESS.equalsIgnoreCase(kvm.getMessage()));
        assertNull(kvm.getKey());
        assertNull(kvm.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Must be able to parse error response successfully")
    public void successfullyParsesErrorResp() throws KVException {
        sock = Utils.setupReadFromFile("errorresp.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(RESP, kvm.getMsgType());
        assertNotNull(kvm.getMessage());
        assertNull(kvm.getKey());
        assertNull(kvm.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Must be able to parse get request successfully")
    public void successfullyParsesGetReq() throws KVException {
        sock = Utils.setupReadFromFile("getreq.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(GET_REQ, kvm.getMsgType());
        assertNull(kvm.getMessage());
        assertNotNull(kvm.getKey());
        assertNull(kvm.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Must be able to parse get response successfully")
    public void successfullyParsesGetResp() throws KVException {
        sock = Utils.setupReadFromFile("getresp.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(RESP, kvm.getMsgType());
        assertNull(kvm.getMessage());
        assertNotNull(kvm.getKey());
        assertNotNull(kvm.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Must be able to parse put request successfully")
    public void successfullyParsesPutReq() throws KVException {
        sock = Utils.setupReadFromFile("putreq.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(PUT_REQ, kvm.getMsgType());
        assertNull(kvm.getMessage());
        assertNotNull(kvm.getKey());
        assertNotNull(kvm.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Must be able to parse put response successfully")
    public void successfullyParsesPutResp() throws KVException {
        sock = Utils.setupReadFromFile("putresp.txt");
        KVMessage kvm = new KVMessage(sock);
        assertNotNull(kvm);
        assertEquals(RESP, kvm.getMsgType());
        assertTrue(SUCCESS.equalsIgnoreCase(kvm.getMessage()));
        assertNull(kvm.getKey());
        assertNull(kvm.getValue());
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 2,
        desc = "Non XML in socket results in ERROR_PARSER or ERROR_INVALID_FORMAT")
    public void handlesNotXML() {
        sock = Utils.setupReadFromFile("garbage.txt");
        muteStdErr();
        try {
            @SuppressWarnings("unused")
            KVMessage kvm = new KVMessage(sock);
            fail("After read failure, expect error message!");
        } catch (KVException e) {
            KVMessage failure = e.getKVMessage();
            assertEquals(RESP, failure.getMsgType());
            // allow both for different parsers
            assertTrue(failure.getMessage().equals(ERROR_PARSER) ||
                failure.getMessage().equals(ERROR_INVALID_FORMAT));
            assertNull(failure.getKey());
            assertNull(failure.getValue());
        }
        unmuteStdErr();
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 2,
        desc = "Test valid KVMessage serializes properly")
    public void successfullyDumpsBasicXml() throws KVException {
        KVMessage kvm = new KVMessage(GET_REQ);
        kvm.setKey("hello");
        Node messageNode = parseMessage(kvm);
        assertNotNull(messageNode);
        NamedNodeMap attrs = messageNode.getAttributes();
        assertEquals(GET_REQ, attrs.getNamedItem("type").getNodeValue());
        assertEquals(1, attrs.getLength());
        NodeList children = messageNode.getChildNodes();
        boolean foundKey = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child == null || child.getNodeType() != Node.ELEMENT_NODE) {
                // the text must be simply whitespace.
                assertEquals(0, child.getTextContent().trim().length());
                continue;
            }
            assertFalse(foundKey);
            assertEquals("Key", child.getNodeName());
            assertEquals("hello", child.getTextContent());
            foundKey = true;
        }
    }

    private KVMessage callSendMessage(KVMessage msg) throws KVException {
        Socket oSockMock = mock(Socket.class);
        Socket iSockMock = mock(Socket.class);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            when(oSockMock.getOutputStream()).thenReturn(os);
        } catch (IOException e) {
            fail("This should not fail");
        }
        msg.sendMessage(oSockMock);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        try {
            when(iSockMock.getInputStream()).thenReturn(is);
        } catch (IOException e) {
            fail("This should not fail");
        }
        return new KVMessage(iSockMock);
    }

    private void assertKVM(KVMessage msg, String msgType, String key, String value, String message) {
        assertEquals(msgType, msg.getMsgType());
        assertEquals(key, msg.getKey());
        assertEquals(value, msg.getValue());
        assertEquals(message, msg.getMessage());
    }

    @Test
    public void testSendMessage() throws KVException {
        KVMessage msg1, msg2;

        // Test valid GET_REQ, PUT_REQ, ABORT, COMMIT
        msg1 = new KVMessage(GET_REQ);
        msg1.setKey("Hello, world!");
        assertKVM(msg1, GET_REQ, "Hello, world!", null, null);
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, GET_REQ, "Hello, world!", null, null);

        msg1 = new KVMessage(PUT_REQ);
        msg1.setKey("abc");
        msg1.setValue("def");
        assertKVM(msg1, PUT_REQ, "abc", "def", null);
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, PUT_REQ, "abc", "def", null);

        msg1 = new KVMessage(ABORT);
        msg1.setMessage("oh noes there was an error");
        assertKVM(msg1, ABORT, null, null, "oh noes there was an error");
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, ABORT, null, null, "oh noes there was an error");

        msg1 = new KVMessage(COMMIT);
        assertKVM(msg1, COMMIT, null, null, null);
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, COMMIT, null, null, null);

        // Test invalid KVMessages, to make sure that KVMessage does
        // not check for valid message contents
        msg1 = new KVMessage(GET_REQ);
        msg1.setKey("");
        msg1.setValue("I am a value");
        msg1.setMessage("I am a message");
        assertKVM(msg1, GET_REQ, "", "I am a value", "I am a message");
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, GET_REQ, "", "I am a value", "I am a message");

        msg1 = new KVMessage(COMMIT);
        msg1.setKey("K");
        msg1.setValue("V");
        msg1.setMessage("");
        assertKVM(msg1, COMMIT, "K", "V", "");
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, COMMIT, "K", "V", "");

        msg1 = new KVMessage("lol");
        msg1.setKey("1");
        msg1.setValue("2");
        msg1.setMessage("3");
        assertKVM(msg1, "lol", "1", "2", "3");
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, "lol", "1", "2", "3");

        msg1 = new KVMessage((String) null);
        msg1.setKey("4");
        msg1.setValue("5");
        msg1.setMessage("6");
        assertKVM(msg1, null, "4", "5", "6");
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, null, "4", "5", "6");
    }

    @Test
    public void testSpecialCharacters() throws KVException {
        String letters = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String symbols = "`1234567890-=[]\\;',./ ~!@#$%^&*()_+{}|:\"<>?";
        String whitespace = "   \t\r\n   ";
        KVMessage msg1, msg2;

        msg1 = new KVMessage(PUT_REQ);
        msg1.setKey(letters);
        msg1.setValue("letters key");
        assertKVM(msg1, PUT_REQ, letters, "letters key", null);
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, PUT_REQ, letters, "letters key", null);

        msg1 = new KVMessage(PUT_REQ);
        msg1.setKey(symbols);
        msg1.setValue("symbols key");
        assertKVM(msg1, PUT_REQ, symbols, "symbols key", null);
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, PUT_REQ, symbols, "symbols key", null);

        msg1 = new KVMessage(PUT_REQ);
        msg1.setKey(whitespace);
        msg1.setValue("whitespace key");
        assertKVM(msg1, PUT_REQ, whitespace, "whitespace key", null);
        msg2 = callSendMessage(msg1);
        assertKVM(msg2, PUT_REQ, whitespace, "whitespace key", null);
    }

    /* ----------------------- BEGIN HELPER METHODS ------------------------ */

    /* Definitely don't make the parse code available to students */
    private static Node parseMessage(KVMessage kvm) throws KVException {
        String out = kvm.toXML();
        assertNotNull(out);
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(out)));
            NodeList messages = doc.getElementsByTagName("KVMessage");
            assertEquals(1, messages.getLength());
            return messages.item(0);
        } catch (ParserConfigurationException e) {
            System.err.println("unkown error: test failed to run due to config");
            throw new RuntimeException(e);
        } catch (SAXException e) {
            fail("toXML output was not parseable");
        } catch (IOException e) {
            // this shouldn't be possible.
            System.err.println("unkown error: test failed to run due to I/O error");
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Redirects the standard error to a temporary file, if possible.
     * Otherwise, informs the user there should be at least one error message
     * below.
     *
     * This is used to quiet javax parsing, which doesn't seem to have built-in
     * methods to control its verbosity.
     */
    private void muteStdErr() {
        stdErr = System.err;
        try {
            System.setErr(new PrintStream(tempFile));
        } catch (IOException e) {
            System.out.println("Error message(s) expected below:");
        }
    }

    /**
     * Redirects the standard error back to the actual standard error, which is
     * expected to be stored in the class variable.
     */
    private void unmuteStdErr() {
        System.setErr(stdErr);
    }

    // George: Test too specific. Didn't ask them to check types.
    // @Test(timeout = kTimeoutQuick)
    // @Category(AG_PROJ3_CODE.class)
    // @AGTestDetails(points = 2,
    //     desc = "temp")
    // public void errorsWhileDumpingBadMsg() {
    //     KVMessage kvm = new KVMessage("this is not a valid message type");
    //     try {
    //         kvm.toXML();
    //         fail("expected toXML to fail. This is a bad message type!");
    //     } catch (KVException e) {
    //         assertNotNull(e.getKVMessage().getMessage());
    //         assertTrue(e.getKVMessage().getMessage().startsWith(ERROR_INVALID_FORMAT));
    //     }
    // }

    // TODO: Almost every group is failing this test. Please check this out Nick.
    // @Test(timeout = kTimeoutQuick)
    // @Category(AG_PROJ3_CODE.class)
    // @AGTestDetails(points = 2, desc = "temp")
    // public void successfullyDumpsMoreXml() throws KVException {
    //     String msg = "Success";
    //     String key = "Nick";
    //     String val = "the bomb";
    //     KVMessage kvm = new KVMessage(RESP);
    //     kvm.setMessage(msg);
    //     kvm.setKey(key);
    //     kvm.setValue(val);
    //     Node messageNode = parseMessage(kvm);
    //     assertNotNull(messageNode);
    //     NamedNodeMap attrs = messageNode.getAttributes();
    //     assertEquals(RESP, attrs.getNamedItem("type").getNodeValue());
    //     assertEquals(1, attrs.getLength());
    //     NodeList children = messageNode.getChildNodes();
    //     Map<String, String> map = new HashMap<String, String>();
    //     map.put("Message", msg);
    //     map.put("Key", key);
    //     map.put("Value", val);
    //     int elemCount = 0;
    //     for (int i = 0; i < children.getLength(); i++) {
    //         Node child = children.item(i);
    //         if (child == null || child.getNodeType() != Node.ELEMENT_NODE) {
    //             // the text must be simply whitespace.
    //             assertEquals(0, child.getTextContent().trim().length());
    //             continue;
    //         }
    //         assertTrue(map.containsKey(child.getNodeName()));
    //         String entry = map.remove(child.getNodeName());
    //         assertEquals(entry, child.getTextContent());
    //         elemCount++;
    //     }
    //     assertEquals(3, elemCount);
    // }

// TODO write end-to-end tests somewhere involving multiple clients, server
// TODO write tests involving socket closure.  This was a major bug

//    George: Too specific.
//    @Ignore
//    @Test(timeout=5000)
//    public void nullSocketFails() {
//        try {
//            new KVMessage((Socket) null);
//            fail("creating a KVMessage from a null Socket should throw a"
//                    + " KVException");
//        } catch (KVException e) {
//            assertEquals(RESP, e.getKVMessage().getMsgType());
//        }
//    }
//
//    /*
//     * Nick: do we care about this case?  Seems awfully specific.  It's testing
//     * behavior when the socket throws a SocketException on setting timeout,
//     * which seems laughably unlikely and even questionably useful imo.
//     */
//    @Test(timeout=5000)
//    public void handlesSocketTimeoutSettingException() {
//        sock = mock(Socket.class);
//        try {
//            doThrow(new SocketException()).when(sock).setSoTimeout(anyInt());
//        } catch (SocketException e) {
//            // this is pretty unlikely to happen; the mock would be misbehaving
//            e.printStackTrace();
//        }
//        try {
//            @SuppressWarnings("unused")
//            KVMessage kvm = new KVMessage(sock);
//            fail("after socket timeout-specifying error, expect error message!");
//        } catch (KVException e) {
//            KVMessage failure = e.getKVMessage();
//            assertEquals(RESP, failure.getMsgType());
//            // TODO decide if we care what the following error message is. Most recent spec didn't specify.
//            assertNotNull(failure.getMessage());
//            assertNull(failure.getKey());
//            assertNull(failure.getValue());
//        }
//    }
//
//    /*
//     * TODO the staff code actually fails this test.  Not sure if this should be
//     * tested, though as this appears to be the default behavior for the XML
//     * parser and would probably just give the students headaches.
//     */
//    @Ignore
//    @Test(timeout=5000)
//    public void errorsOnXMLWithNoProlog() {
//        sock = Utils.setupReadFromFile("no-prologue.txt");
//        muteStdErr();
//        try {
//            @SuppressWarnings("unused")
//            KVMessage kvm = new KVMessage(sock);
//            fail("After read failure, expect error message!");
//        } catch (KVException e) {
//            KVMessage failure = e.getKVMessage();
//            assertEquals(RESP, failure.getMsgType());
//            // TODO decide if we care what the following error message is. Most recent spec didn't specify.
//            assertEquals(ERROR_PARSER, failure.getMessage());
//            assertNull(failure.getKey());
//            assertNull(failure.getValue());
//        }
//        unmuteStdErr();
//    }

}
