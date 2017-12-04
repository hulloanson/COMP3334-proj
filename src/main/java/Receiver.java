package main.java;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Receiver {
  // Patterns to parse requests
  private Pattern ackPattern;

  // Networking config
  private static final int DATA_LENGTH = 1024;
  private static final int TIMEOUT_LENGTH = 1000; // millisecond

  // Networking vars
  private DatagramSocket socket;
  private int port;
  private int peerPort;
  private InetAddress address;
  private InetAddress peerAddress;

  // Encryption
  private final String transformation = "AES/CBC/PKCS5Padding";
  private SecretKeySpec key;

  // InventoryManager to pass data to
  private InventoryManager invManager;

  // Flags
  private boolean end;
  private boolean keyExchanged;

  // Logging
  private Logger logger;

  Receiver(int port, int peerPort, InetAddress address, InetAddress peerAddress,
           InventoryManager inventoryManager) {
    this.invManager = inventoryManager;
    this.port = port;
    this.peerPort = peerPort;
    this.address = address;
    this.peerAddress = peerAddress;
    this.ackPattern = Pattern.compile("^ACK$");
    this.logger = inventoryManager.logger;
    this.end = false;

    // TODO: temporary. Please change when implementing key exchange
    keyExchanged = true;
    key = new SecretKeySpec(getUTF8Bytes("1234567890123456"), "AES");
  }

  private static byte[] getUTF8Bytes(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }

  private void sendString(byte[] bytes) throws IOException {
    this.socket.send(new DatagramPacket(
      bytes,
      bytes.length,
      peerAddress,
      peerPort
    ));
  }

  private void sendString(String string) throws IOException {
    sendString(string.getBytes(string));
  }

  private String receiveString(int timeout) throws IOException, SocketTimeoutException {
    logger.log(Level.FINE, "Listening for string... hush...");
    byte[] receiveBuffer = new byte[DATA_LENGTH];
    DatagramPacket received = new DatagramPacket(receiveBuffer, receiveBuffer.length);
    this.socket.setSoTimeout(timeout);
    this.socket.receive(received);
    this.socket.setSoTimeout(0);
    // Extract data from buffer
    int dataOffset = received.getOffset();
    byte[] receivedBytes = Arrays.copyOfRange(received.getData(), dataOffset, dataOffset + received.getLength());
    Message message = new Message().setByReceived(receivedBytes).setKey(key);
    String receivedMsg = null;
    try {
      receivedMsg = message.getPlainMessage();
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException("Error while getting plaintext");
    }
    logger.log(Level.FINE, MessageFormat.format("Received string {0}", receivedMsg));
    return receivedMsg;
  }

  private boolean waitACK() throws IOException {
    try {
      String response = receiveString(1000);
      if (!ackPattern.matcher(response).find()) throw new ResponseException("Invalid response: expected ACK");
      return true;
    } catch (ResponseException | SocketTimeoutException e) {
      logger.log(Level.SEVERE, MessageFormat.format("ACK failed: {0}", e.getClass()));
      return false;
    }
  }

  public boolean send(String message) {
    logger.log(Level.FINE, MessageFormat.format("Requested to send message: {0}", message));
    Message toSend = new Message().setKey(key).setMessage(message);
    // Make it send-ready
    byte[] packed = null;
    try {
      packed = toSend.getUDPReady();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
//    // Encrypt message
//    byte[] encrypted = null;
//    try {
//      logger.log(Level.FINE, "Started AES encryption");
//      encrypted = toSend.encrypt().getEncrypted();
//      logger.log(Level.FINE, "Encryption done.");
//    } catch (Exception e) {
//      e.printStackTrace();
//      logger.log(Level.WARNING, MessageFormat.format("Encryption failed due to IOException: {0}", e.getMessage()));
//      return false;
//    }
//    try  {
//      // Hash message, represented in base64
//      toSend.hash();
//    } catch (IOException e) {
//      logger.log(Level.WARNING, "Failed hashing: {0}", e.getMessage());
//      return false;
//    }
    try {
      sendString(packed);
//      if (!waitACK()) return false;
//      sendString("ACK");
      return true;
    } catch (IOException e) {
      logger.log(Level.WARNING, MessageFormat.format("Send failed due to IOException: {0}", e.getMessage()));
      e.printStackTrace();
      return false;
    }
  }

  public void handleRequests() throws IOException {
    System.out.println("Starting...");
    this.socket = new DatagramSocket(port, address);
    System.out.println("Started server on port " + port);
    while (!this.end) {
      String request = receiveString(0);
//      sendString("ACK");
//      if (!waitACK()) {
//        logger.log(Level.INFO, "Request dropped due to timeout");
//        continue;
//      }
      // ACK-ed, process request
      invManager.processRequest(request);
    }
  }
}
