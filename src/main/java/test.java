package main.java;
import java.util.regex.*;
/**
 * Created by hulloanson on 12/2/17.
 */
public class test {
  public static void main(String[] args) throws Exception {
    String testStr = "SEND12356";
    Pattern pattern = Pattern.compile("^SEND([0-9]+)$");
    Matcher matcher = pattern.matcher(testStr);
    if (matcher.find()) {
      System.out.println(matcher.group(1));
    }
  }
}
