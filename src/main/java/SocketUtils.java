package main.java;

import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by hulloanson on 12/4/17.
 */
public class SocketUtils {
  private static int getRandomPort() {
    final int MINPORT = 10000;
    final int RANGE = 100;
    return ((int)Math.ceil(Math.random() * RANGE)) + MINPORT;
  }

  public static SecSocket getSocket(SecretKeySpec key) throws UnknownHostException {
    SecSocket sendSocket = null;
    while (true) {
      try {
        sendSocket = new SecSocket(getRandomPort(), InetAddress.getLocalHost(), key);
      } catch (SocketException e) {
        continue;
      }
      break;
    }
    return sendSocket;
  }
}
