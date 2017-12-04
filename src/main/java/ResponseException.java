package main.java;

/**
 * Created by hulloanson on 12/2/17.
 */
class ResponseException extends Exception {
  ResponseException(String var1) {
    super(var1);
  }

  ResponseException(String var1, Throwable var2) {
    super(var1, var2);
  }

  ResponseException(Throwable var1) {
    super(var1);
  }

  protected ResponseException(String var1, Throwable var2, boolean var3, boolean var4) {
    super(var1, var2, var3, var4);
  }
}
