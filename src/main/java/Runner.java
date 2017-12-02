package main.java;

import java.io.IOException;

/**
 * Created by hulloanson on 12/2/17.
 */
public class Runner {
  public static void main(String[] args) throws IOException {
    (new InventoryManager()).handleRequests();
  }
}
