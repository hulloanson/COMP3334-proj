package main.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Receiver {
  // Patterns to parse requests
  private Pattern sendPattern;
  private Pattern checkPattern;
  private Pattern requestPattern;
  private Pattern amountPattern;
  
  // Networking config
  private static final int DATA_LENGTH = 1024;
  private static final int LISTEN_PORT = 9999;

  // Networking vars
  private DatagramSocket socket;
  private DatagramPacket currReceived;

  // Inventory management
  private int inventory;
  private int lastChange;
  
  // Flags
  private boolean end;

  public Receiver() {
    this.inventory = 500;
    this.sendPattern = Pattern.compile("^SEND([0-9]+)$");
    this.checkPattern = Pattern.compile("^CHECK$");
    this.requestPattern = Pattern.compile("^REQUEST([0-9]+)$");
    this.amountPattern = Pattern.compile("^.*[0-9]$");
    this.end = false;
  }

  private int parseAmount(String message) throws RequestException {
    Matcher matcher = amountPattern.matcher(message);
    if (!matcher.find())
      throw new RequestException("invalid request");
    return Integer.parseInt(matcher.group(1));
  }

  private void changeInventory(int change) {
    lastChange = change;
    this.inventory += change;
  }

  private String processRequest(String req) throws RequestException {
    if (checkPattern.matcher(req).find()) {
      return "INV" + this.inventory;
    } else if (sendPattern.matcher(req).find()) {
      changeInventory(parseAmount(req));
      return "INV" + this.inventory;
    } else if (requestPattern.matcher(req).find()) {
      int reqAmount = parseAmount(req);
      if (reqAmount > this.inventory) {
        return "INSUFFICIENT";
      } else {
        changeInventory(0 - reqAmount);
        lastChange = 0 - reqAmount;
        this.inventory += lastChange;
        return "REQUESTOK" + this.inventory;
      }
    } else {
      throw new RequestException("invalid request");
    }
  }
  
  private void sendString(String string) throws IOException {
    byte[] toSend;
    this.socket.send(new DatagramPacket(
      toSend = string.getBytes(),
      toSend.length,
      currReceived.getAddress(),
      currReceived.getPort()
    ));
  }

  private void handleRequests() throws IOException {
    this.socket = new DatagramSocket(LISTEN_PORT);
    try {
      while (!this.end) {
        byte[] receiveBuffer = new byte[DATA_LENGTH];
        currReceived = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        this.socket.receive(currReceived);
        String response = processRequest(new String(currReceived.getData(), currReceived.getOffset(), currReceived.getLength()));
        sendString(response);
      }
    } catch (RequestException e) { // to catch exceptions due to malformed data
      // Rollback changes
      this.inventory -= lastChange;
      // Send error
      sendString("ERR");
    }
  }

  public static void main(String[] args) throws IOException {
    (new Receiver()).handleRequests();
  }
}
