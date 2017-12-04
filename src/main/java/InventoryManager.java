package main.java;

import java.awt.*;        // Using AWT container and component classes
import java.awt.event.*;  // Using AWT event classes and listener interfaces
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// An AWT program inherits from the top-level container java.awt.Frame
class InventoryManager extends Frame implements WindowListener {
  private String role;

  private TextField requestAmountInput;
  private TextField sendAmountInput;
  private Panel selectPanel;

  private Receiver receiver;

  private ArrayList<String> requestQueue;

  // Patterns to parse requests
  private Pattern sendPattern = Pattern.compile("^SEND([0-9]+)$");
  private Pattern checkPattern = Pattern.compile("^CHECK$");
  private Pattern requestPattern = Pattern.compile("^REQUEST([0-9]+)$");
  private Pattern refillPattern = Pattern.compile("^REFILL$");
  private Pattern amountPattern = Pattern.compile("^[A-Z]+([0-9]+)$");
  private Pattern errPattern = Pattern.compile("^ERR$");
  private Pattern invPattern = Pattern.compile("^INV([0-9]+)$");

  // Inventory management
  private int inventory;
  private int peerInventory;
  private int lastChange;

  // Globally-accessible elements
  Label infoLabel;
  Label peerInvLabel;
  Label selfInvLabel;

  // Logging
  public Logger logger;

  // Threads
  private Thread receiverThread;


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

  class SendButtonHandler implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      int amount = Integer.parseInt(sendAmountInput.getText());
      sendAmount(amount);
    }
  }

  private boolean sendAmount(int amount) {
    if (amount > inventory) {
      infoLabel.setText("Don't have enough to send.");
      logger.log(Level.WARNING, "Tried to manually send more than what was there");
      return false;
    }
    if (receiver.send("SEND" + amount)) {
      changeInventory(0 - amount);
      if (inventory == 0) requestRefill();
      return true;
    }
    return false;
  }

  private void requestRefill() {
    if (!receiver.send("REFILL")) {
      logger.log(Level.FINE, "Failed to send REFILL command");
    }
  }

  class CheckButtonHandler implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      receiver.send("CHECK");
    }
  }

  class RequestButtonHandler implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      int requestAmount = Integer.parseInt(requestAmountInput.getText());
      receiver.send("REQUEST" + requestAmount);
    }
  }

  class RoleSelectListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      String role = ((Button) actionEvent.getSource()).getLabel();
      setRole(role);
      remove(selectPanel);
      revalidate();
      setUpMainScreen(role);
    }
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
    selfInvLabel = new Label("Self: 500");
    peerInvLabel = new Label("Peer: 500");
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

  private void setUpMainScreen(String role) {
    // Data, logging
    setUpBackend();
    // Networking
    startListener(role);
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
    this.inventory = 500;
    this.peerInventory = 500;
    // Logging
    logger = Logger.getLogger(Receiver.class.getName());
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
    lastChange = change;
    this.inventory += change;
    this.peerInventory -= change;
    updateInventoryLabels();
  }

  private void rollbackChange() {
    logger.log(Level.INFO, MessageFormat.format("Rolling back change. The change :{0}", lastChange));
    inventory -= lastChange;
    peerInventory += lastChange;
    updateInventoryLabels();
  }

  private void updateInventoryLabels() {
    selfInvLabel.setText("Self: " + inventory);
    peerInvLabel.setText("Peer: " + peerInventory);
  }

  public synchronized void processRequest(String req) {
//    String req = requestQueue.get(0);
    logger.log(Level.FINER, MessageFormat.format("Incoming request: {0}", req));
    // default last change to 0 in case request exception occurs before receiver makes change to inventory
    try {
      if (checkPattern.matcher(req).find()) {
        lastChange = 0;
        receiver.send("INV" + this.inventory);
      } else if (sendPattern.matcher(req).find()) { // SEND received
        changeInventory(parseAmount(req)); // Add to inventory
      } else if (requestPattern.matcher(req).find()) { // REQUEST received
        int reqAmount = parseAmount(req); // Parse request amount
        if (reqAmount > this.inventory) {
          lastChange = 0;
          receiver.send("INSUFFICIENT");
        } else {
          sendAmount(parseAmount(req));
        }
      } else if (refillPattern.matcher(req).find()) {
        if (500 > this.inventory) {
          lastChange = 0;
          receiver.send("INSUFFICIENT");
        } else {
          sendAmount(500);
        }
      } else if (errPattern.matcher(req).find()) {
        rollbackChange();
      } else if (invPattern.matcher(req).find()) {
        int peerAmount = parseAmount(req);
        infoLabel.setText("Inventory check: " + (parseAmount(req) == inventory ? "Match" : "Does not match"));
        logger.log(Level.FINE, "Inventory check: {0}, self is {1}, peer is {2}", new Object[]{
          parseAmount(req) == inventory ? "Match" : "Does not match",
          inventory, peerAmount
        });
      } else {
        logger.log(Level.WARNING, "Dropped incomprehensible message: {0}", req);
      }
    } catch (RequestException e) {
      // Send err
      receiver.send("ERR");
    }
  }

  private void startListener(String role) {
    try {
      receiver = new Receiver(
        role.equals("Bob") ? 9999 : 9998,
        role.equals("Bob") ? 9998 : 9999,
        InetAddress.getLocalHost(), InetAddress.getLocalHost(),
        this
      );
    }catch (UnknownHostException e) {
      e.printStackTrace();
      System.exit(1);
    }
    final Runnable listener = () -> {
      try {
        receiver.handleRequests();
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    };
    receiverThread = new Thread(listener, "receiver-thread");
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