package main.java;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by hulloanson on 12/4/17.
 */
public class SecSocket extends DatagramSocket {
  private SecretKeySpec aesKey;

  private final Logger logger;

  private final Pattern ackPattern = Pattern.compile("^ACK$");

  private static final int DATA_LENGTH = 1024;
  private static final int ACK_TIMEOUT = 1000; // millisecond

  public SecSocket(SecretKeySpec aesKey) throws SocketException {
    this.aesKey = aesKey;
    this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    this.logger.setLevel(Level.FINE);
  }

  protected SecSocket(DatagramSocketImpl datagramSocket, SecretKeySpec aesKey) {
    super(datagramSocket);
    this.aesKey = aesKey;
    this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    this.logger.setLevel(Level.FINE);
  }

  public SecSocket(SocketAddress socketAddress, SecretKeySpec aesKey) throws SocketException {
    super(socketAddress);
    this.aesKey = aesKey;
    this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    this.logger.setLevel(Level.FINE);
  }

  public SecSocket(int i, SecretKeySpec aesKey) throws SocketException {
    super(i);
    this.aesKey = aesKey;
    this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    this.logger.setLevel(Level.FINE);
  }

  public SecSocket(int i, InetAddress inetAddress, SecretKeySpec aesKey) throws SocketException {
    super(i, inetAddress);
    this.aesKey = aesKey;
    this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    this.logger.setLevel(Level.FINE);
  }


  public boolean send(String message, InetAddress dstAddress, int dstPort) throws IOException {
    return send(message.getBytes(StandardCharsets.UTF_8), dstAddress, dstPort);
  }

  public boolean send(byte[] message, InetAddress dstAddress, int dstPort) throws IOException {
    super.send(new DatagramPacket(
      message,
      message.length,
      dstAddress,
      dstPort
    ));
    return true;
  }

  interface SSCallback {
    void onComplete(String response, Exception error);
  }

  public static void sendMessage(String message, SecretKeySpec aesKey, InetAddress dstAddress, int dstPort, SSCallback callback) {
    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    logger.log(Level.FINE, MessageFormat.format("Requested to send message: {0}", message));
    Runnable sendTask = () -> {
      try {
        SecSocket sendSocket = SocketUtils.getSocket(aesKey);
        sendSocket.sendMessageBlocking(message, aesKey, dstAddress, dstPort,true, true);
      } catch (Exception e) {
        callback.onComplete(null, e);
        return;
      }
      // Send success
      callback.onComplete("OK", null);
    };
    new Thread(sendTask).start();
  }

  public boolean sendMessageBlocking(String message, SecretKeySpec aesKey, InetAddress dstAddress, int dstPort, boolean ack, boolean shouldThrow) throws Exception {
    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    logger.log(Level.FINE, MessageFormat.format("Requested to send message: {0}", message));
    try {
//      sendSocket = (sendSocket == null) ? SocketUtils.getSocket(aesKey) : sendSocket;
      Message toSend = new Message().setKey(aesKey).setMessage(message);
      byte[] packed = null;
      packed = toSend.getUDPReady();
      send(packed, dstAddress, dstPort);
      if (ack) {
        logger.log(Level.FINE, "Waiting ACK after sending message.");
        waitACK();
        logger.log(Level.FINE, "ACK received for sent message Sending back an ACK.");
        sendMessageBlocking("ACK", aesKey, dstAddress, dstPort, false, true);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, MessageFormat.format("Send failed: {0}: {1}", e.getClass(), e.getMessage()));
      if (shouldThrow) throw e;
      else return false;
    }
    return true;
  }

  public String receiveMessage(int timeout, boolean ack) throws Exception {
    logger.log(Level.FINE, "Listening for string... hush...");
    byte[] receiveBuffer = new byte[DATA_LENGTH];
    DatagramPacket received = new DatagramPacket(receiveBuffer, receiveBuffer.length);
    setSoTimeout(timeout);
    receive(received);
    setSoTimeout(0);
    // Extract data from buffer
    int dataOffset = received.getOffset();
    byte[] receivedBytes = Arrays.copyOfRange(received.getData(), dataOffset, dataOffset + received.getLength());
    // Decryption
    Message message = new Message().setByReceived(receivedBytes).setKey(aesKey);
    String receivedMsg = null;
    receivedMsg = message.getPlainMessage();
    if (ack) {
      logger.log(Level.FINE, "Sending ACK after receiving message.");
      sendMessageBlocking("ACK", aesKey, received.getAddress(), received.getPort(),false, true);
      logger.log(Level.FINE, "Waiting for final ACK.");
      waitACK();
      logger.log(Level.FINE, "Final ACK received.");
    }
    logger.log(Level.FINE, MessageFormat.format("Received {0} {1}", ack ? "message" : "ACK", receivedMsg));
    return receivedMsg;
  }

  private void waitACK() throws Exception {
    String response = receiveMessage(ACK_TIMEOUT, false);
    if (!ackPattern.matcher(response).find()) throw new ResponseException("Invalid response: expected ACK");
  }

}
