package main.java;

import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// An AWT program inherits from the top-level container java.awt.Frame
class InventoryManager extends Frame implements WindowListener {
  // TODO: key exchange
  private SecretKeySpec aesKey = new SecretKeySpec("1234567890123456".getBytes(StandardCharsets.UTF_8), "AES");

  private String role;

  private TextField requestAmountInput;
  private TextField sendAmountInput;
  private Panel selectPanel;

  // Patterns to parse requests
  private Pattern sendPattern = Pattern.compile("^SEND([0-9]+)$");
  private Pattern checkPattern = Pattern.compile("^CHECK$");
  private Pattern requestPattern = Pattern.compile("^REQUEST([0-9]+)$");
  private Pattern refillPattern = Pattern.compile("^REFILL$");
  private Pattern amountPattern = Pattern.compile("^[A-Z]+([0-9]+)$");
  private Pattern invPattern = Pattern.compile("^INV([0-9]+)$");

  // Inventory management
  private int inventory;
  private int peerInventory;

  // Globally-accessible elements
  private Label infoLabel;
  private Label peerInvLabel;
  private Label selfInvLabel;

  // Logging
  private Logger logger;

  // Threads
  private Thread receiverThread;
  private boolean end; // TODO: implement soft end

  // Window events
  @Override
  public void windowOpened(WindowEvent windowEvent) {

  }

  @Override
  public void windowClosing(WindowEvent windowEvent) {
    // TODO: handle window close. it still fails
    try {
      receiverThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public void windowClosed(WindowEvent windowEvent) {

  }

  @Override
  public void windowIconified(WindowEvent windowEvent) {

  }

  @Override
  public void windowDeiconified(WindowEvent windowEvent) {

  }

  @Override
  public void windowActivated(WindowEvent windowEvent) {

  }

  @Override
  public void windowDeactivated(WindowEvent windowEvent) {

  }

  // Button events
  class SendButtonHandler implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      int amount = Integer.parseInt(sendAmountInput.getText());
      sendCmdSend(amount);
    }
  }

  class CheckButtonHandler implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      sendCmdCheck();
    }
  }

  class RequestButtonHandler implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      sendCmdRequest();
    }
  }

  class RoleSelectListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      String role = ((Button) actionEvent.getSource()).getLabel();
      setRole(role);
      remove(selectPanel);
      revalidate();
      setUpMainScreen();
    }
  }

  /*************************** Command senders ******************************/
  private void sendCmdSend(int amount) {
    if (amount > inventory) {
      infoLabel.setText("Don't have enough to send.");
      logger.log(Level.INFO, "Tried to manually send more than what was there");
    }
    sendToSocket("SEND" + amount, (response, error) -> {
      logSSCallback("SEND", response, error);
      if (error != null) {
        changeInventory(0 - amount);
        if (inventory == 0) sendCmdRefill();
      }
    });
  }

  private void sendCmdCheck() {
    sendToSocket("CHECK", (response, error) -> {
      logSSCallback("CHECK", response, error);
    });
  }

  private void sendCmdRequest() {
    int requestAmount = Integer.parseInt(requestAmountInput.getText());
    sendToSocket("REQUEST" + requestAmount, (response, error) -> {
      logSSCallback("REQUEST", response, error);
    });
  }

  private void sendCmdRefill() {
    sendToSocket("REFILL", (response, error) -> {
      logSSCallback("REFILL", response, error);
    });
  }

  private void sendCmdInsufficient() {
    sendToSocket("INSUFFICIENT", (response, error) -> {
      logSSCallback("INSUFFICIENT", response, error);
    });
  }

  private void sendCmdInv() {
    sendToSocket("INV" + this.inventory, (response, error) -> {
      logSSCallback("INV", response, error);
    });
  }
  /************** Command senders ends *********************/

  private void logSSCallback(String command, String response, Exception error) {
    String message = MessageFormat.format("Send {0} {1}", command, error == null ? "succeeded" : "failed");
    infoLabel.setText(message);
  }

  private void setRole(String role) {
    this.role = role;
  }

  // Constructor to setup InventoryManager components and event handlers
  private Panel constructSendUI() {
    Panel sendPanel = new Panel(new FlowLayout());
    Label sendLabel = new Label("Send");
    sendPanel.add(sendLabel);

    sendAmountInput = new TextField("0", 10);
    sendAmountInput.setEditable(true);
    sendPanel.add(sendAmountInput);

    Button sendButton = new Button("Send");
    sendPanel.add(sendButton);

    sendButton.addActionListener(new SendButtonHandler());
    return sendPanel;
  }

  private Panel constructCheckUI() {
    Panel checkPanel = new Panel(new FlowLayout());
    Label checkLabel = new Label("Check");
    checkPanel.add(checkLabel);
    Button checkButton = new Button("Check");
    checkButton.addActionListener(new CheckButtonHandler());
    checkPanel.add(checkButton);
    return checkPanel;
  }

  private Panel constructRequestUI() {
    Panel requestPanel = new Panel(new FlowLayout());
    Label requestLabel = new Label("Request");
    requestPanel.add(requestLabel);

    requestAmountInput = new TextField("0", 10);
    requestAmountInput.setEditable(true);
    requestPanel.add(requestAmountInput);

    Button requestButton = new Button("Request");
    requestPanel.add(requestButton);

    requestButton.addActionListener(new RequestButtonHandler());
    return requestPanel;
  }

  private Panel constructInfoPanel() {
    Panel infoPanel = new Panel(new FlowLayout());
    infoLabel = new Label("Message will be shown here");
    infoPanel.add(infoLabel);
    selfInvLabel = new Label("Self: 1000");
    peerInvLabel = new Label("Peer: 1000");
    infoPanel.add(selfInvLabel);
    infoPanel.add(peerInvLabel);
    return infoPanel;
  }

  private void showSelectScreen() {
    Button aliceButt = new Button("Alice");
    Button bobButt = new Button("Bob");
    RoleSelectListener selectListener = new RoleSelectListener();
    aliceButt.addActionListener(selectListener);
    bobButt.addActionListener(selectListener);
    selectPanel = new Panel(new FlowLayout());
    selectPanel.add(aliceButt);
    selectPanel.add(bobButt);
    add(selectPanel);
  }

  private void setUpMainScreen() {
    // Data, logging
    setUpBackend();
    // Networking
    startListener();
    // UI
    setTitle("Inventory Manager - " + role);
    logger.log(Level.INFO, "Constructing send panel...");
    add(constructSendUI());
    logger.log(Level.INFO, "Constructing check panel...");
    add(constructCheckUI());
    logger.log(Level.INFO, "Constructing request panel...");
    add(constructRequestUI());
    logger.log(Level.INFO, "Constructing display panel...");
    add(constructInfoPanel());
    logger.log(Level.INFO, "Done!");
    revalidate();
  }

  private void setUpBackend() {
    this.inventory = 1000;
    this.peerInventory = 1000;
    // Logging
    logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    FileHandler logFileHandler;
    try {
      logFileHandler = new FileHandler(this.role + "-invManager.log", true);
      logFileHandler.setFormatter(new SimpleFormatter());
      logger.addHandler(logFileHandler);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    logger.setLevel(Level.FINE);
  }

  private InventoryManager() {
    setLayout(new GridLayout(8, 2));

    setTitle("Inventory Manager");
    setSize(600, 480);

  }

  private int parseAmount(String message) throws RequestException {
    Matcher matcher = amountPattern.matcher(message);
    if (!matcher.find())
      throw new RequestException("invalid request");
    logger.log(Level.FINE, "extracted int is: {0}", new Object[]{matcher.group(1)});
    return Integer.parseInt(matcher.group(1));
  }

  private synchronized void changeInventory(int change) {
    this.inventory += change;
    this.peerInventory -= change;
    updateInventoryLabels();
  }

  private void updateInventoryLabels() {
    selfInvLabel.setText("Self: " + inventory);
    peerInvLabel.setText("Peer: " + peerInventory);
  }

  private void sendToSocket(String message, SecSocket.SSCallback callback) {
    try {
      SecSocket.sendMessage(message,
        aesKey, InetAddress.getLocalHost(), role.equals("Alice") ? 9999 : 9998,
        callback
      );
    } catch (UnknownHostException e) {
      infoLabel.setText("Error preparing send: unknown host");
    }
  }

  private synchronized void processRequest(String req) {
//    String req = requestQueue.get(0);
    logger.log(Level.FINER, MessageFormat.format("Incoming request: {0}", req));
    // default last change to 0 in case request exception occurs before receiver makes change to inventory
    try {
      if (checkPattern.matcher(req).find()) {
        sendCmdInv();
      } else if (sendPattern.matcher(req).find()) { // SEND received
        changeInventory(parseAmount(req)); // Add to inventory
      } else if (requestPattern.matcher(req).find()) { // REQUEST received
        int reqAmount = parseAmount(req); // Parse request amount
        if (reqAmount > this.inventory) {
          sendCmdInsufficient();
        } else {
          sendCmdSend(parseAmount(req));
        }
      } else if (refillPattern.matcher(req).find()) {
        if (500 > this.inventory) {
          sendCmdInsufficient();
        } else {
          sendCmdSend(500);
        }
      } else if (invPattern.matcher(req).find()) {
        int peerAmount = parseAmount(req);
        infoLabel.setText("Peer Inventory check: " + (parseAmount(req) == peerInventory ? "Match" : "Does not match"));
        logger.log(Level.FINE, "Peer Inventory check: {0}, self is {1}, peer is {2}", new Object[]{
          parseAmount(req) == inventory ? "Match" : "Does not match",
          peerInventory, peerAmount
        });
      } else {
        throw new RequestException(MessageFormat.format("Incomprehensible message {0}. Dropped", req));
      }
    } catch (RequestException e) {
      logger.log(Level.INFO, "RequestException: {0}", new Object[]{e.getMessage()});
    }
  }

  private SecSocket makeReceiver() throws UnknownHostException, SocketException {
    return new SecSocket(
      role.equals("Bob") ? 9999 : 9998,
      InetAddress.getLocalHost(),
      aesKey
    );
  }

  private void startListener() {
    receiverThread = new Thread(() -> {
      try {
        SecSocket receiver = makeReceiver();
        while (!end) {
          String message = receiver.receiveMessage(0, true);
          processRequest(message);
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }, "invManager-receiver");
    receiverThread.start();
  }

  public static void main(String[] args) {
    InventoryManager invManager = new InventoryManager();
    // Select to be Bob or Alice
    invManager.showSelectScreen();
    // Stuffs to do before main screen start
    invManager.setVisible(true);
  }
}