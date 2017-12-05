package main.java;

import com.sun.deploy.util.ArrayUtil;
import org.apache.commons.crypto.cipher.CryptoCipher;
import org.apache.commons.crypto.cipher.CryptoCipherFactory;
import org.apache.commons.crypto.random.CryptoRandom;
import org.apache.commons.crypto.random.CryptoRandomFactory;
import org.apache.commons.crypto.utils.Utils;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/**
 * Created by hulloanson on 12/3/17.
 */
public class Message {
  private byte[] original = null;

  private byte[] signature = null;

  private byte[] signedHash = null;

  private SecretKeySpec key = null;

  private byte[] encrypted = null;

  private final String transformation = "AES/CBC/PKCS5Padding";

  Message() {
  }

  public Message setMessage(String string) {
    original = string.getBytes(StandardCharsets.UTF_8);
    return this;
  }

  public Message setMessage(byte[] bytes) {
    original = bytes;
    return this;
  }

  public Message setKey(SecretKeySpec key) {
    this.key = key;
    return this;
  }

  public Message setEncrypted(byte[] encrypted) {
    this.encrypted = encrypted;
    return this;
  }

  public byte[] getEncrypted() throws Exception {
    encrypt();
    return encrypted;
  }

  public byte[] getDecrypted() throws Exception {
    decrypt();
    return original;
  }

  public byte[] getOriginal() {
    return original;
  }

  public String toString() {
    return original == null ? null : new String(original, StandardCharsets.UTF_8);
  }

  private static byte[] getUTF8Bytes(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }

  private static int getOutputLength(int messageLength) {
    return new BigDecimal(messageLength).divide(new BigDecimal(128), BigDecimal.ROUND_CEILING).intValue() * 128;
  }

  public Message encrypt() throws Exception {
    // Generate random IV
    CryptoRandom random = CryptoRandomFactory.getCryptoRandom();
    byte[] ivBytes = new byte[16];
    random.nextBytes(ivBytes);
    final IvParameterSpec iv = new IvParameterSpec(ivBytes);

    Properties properties = new Properties();
//    properties.setProperty(CryptoCipherFactory.CLASSES_KEY, CipherProvider.OPENSSL.getClassName());
    //Creates a CryptoCipher instance with the transformation and properties.
    CryptoCipher encipher = Utils.getCipherInstance(transformation, properties);
    System.out.println(MessageFormat.format("Cipher: {0}", encipher.getClass().getCanonicalName()));

    System.out.println(MessageFormat.format("About to encrypt: {0}", this));
    System.out.println(MessageFormat.format("Message length: {0}", original.length));
    int outputLength = getOutputLength(original.length);
    System.out.println(MessageFormat.format("Output buffer length: {0}", outputLength));
    byte[] output = new byte[outputLength];

    //Initializes the cipher with ENCRYPT_MODE, key and iv.
    encipher.init(Cipher.ENCRYPT_MODE, key, iv);
    //Continues a multiple-part encryption/decryption operation for byte array.
    int updateBytes = encipher.update(original, 0, original.length, output, 0);
    System.out.println(MessageFormat.format("{0} bytes were updated by encipher.update", updateBytes));
    //We must call doFinal at the end of encryption/decryption.
    int finalBytes = encipher.doFinal(original, 0, 0, output, updateBytes);
    System.out.println(MessageFormat.format("{0} bytes were updated by encipher.doFinal", finalBytes));
    //Closes the cipher.
    encipher.close();
    encrypted = ArrayUtils.addAll(ivBytes, Arrays.copyOf(output, updateBytes + finalBytes));
    return this;
  }

  public Message decrypt() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(CryptoCipherFactory.CLASSES_KEY, CryptoCipherFactory.CipherProvider.JCE.getClassName());
    CryptoCipher decipher = Utils.getCipherInstance(transformation, properties);
    System.out.println(MessageFormat.format("Cipher:  {0}", decipher.getClass().getCanonicalName()));

    // Extract IV
    IvParameterSpec iv = new IvParameterSpec(Arrays.copyOf(encrypted, 16));
    decipher.init(Cipher.DECRYPT_MODE, key, iv);
    byte[] decoded = new byte[1024];
    int decryptedLength = decipher.doFinal(encrypted, 16, encrypted.length - 16, decoded, 0);
    System.out.println(MessageFormat.format("Decrypted message length: {0}", decryptedLength));
    // Convert bytes to String
    original = Arrays.copyOf(decoded, decryptedLength);
    System.out.println(MessageFormat.format("message: {0}", this.toString()));
    return this;
  }

  public Message setSignature(byte[] signature) {
    return this;
  }

  public byte[] getSignature() throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return Base64.getEncoder().encode(digest.digest(encrypted));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new IOException("Hash with SHA-256 failed: No such algorithm.");
    }
  }

  public Message signature() throws IOException {
    this.signature = getSignature();
    System.out.println(MessageFormat.format("signature is: {0}", this.signature.length));
    return this;
  }

  public Message signatureVerify() throws IOException {
    byte[] tmpSig = getSignature();
    System.out.println(MessageFormat.format("tmpSig, length: {0}, {1}", new Object[]{tmpSig, tmpSig.length}));
    System.out.println(MessageFormat.format("signature, length: {0}, {1}", new Object[]{signature, signature.length}));
    if (!Arrays.equals(tmpSig, signature)) throw new IOException("Incorrect signature");
    return this;
  }

  public Message setByReceived(byte[] received) {
    // received: first 44 byte is base64 encoded hash TODO: get signed hash
    signature = Arrays.copyOf(received, 44);
    encrypted = Arrays.copyOfRange(received, 44, received.length);
    return this;
  }

  /**
   * public static String signHash(String plainText, PrivateKey privateKey) throws Exception {
   * Signature privateSignature = Signature.getInstance("SHA256withRSA");
   * privateSignature.initSign(privateKey);
   * privateSignature.update(plainText.getBytes(UTF_8));
   * <p>
   * byte[] signature = privateSignature.sign();
   * <p>
   * return Base64.getEncoder().encodeToString(signature);
   * return this;
   * }
   * <p>
   * public static Boolean verifyHash(String plainText, String signature, PublicKey publicKey) throws Exception {
   * Signature publicSignature = Signature.getInstance("SHA256withRSA");
   * publicSignature.initVerify(publicKey);
   * publicSignature.update(plainText.getBytes(UTF_8));
   * <p>
   * if(signature == signature) {
   * byte[] signatureBytes = Base64.getDecoder().decode(signature);
   * return publicSignature.verify(signatureBytes);
   * }
   * return false;
   * return this;
   * }
   */


  public byte[] getUDPReady() throws Exception {
    if (key == null || original == null) {
      throw new IOException("No key or plain text specified");
    }
    // TODO: sign the hash as well
    this.encrypt().signature();
    return ArrayUtils.addAll(signature, encrypted);

    String signature = messageSign("messagesign", pair.getPrivate());
//    signHash(String plainText, PrivateKey privateKey);
//    verifyHash(String plainText, String signature, PublicKey publicKey);
//    getDecrypted();

//    this.encrypt().hash().signHash();
//    return ArrayUtils.addAll(signedHash, encrypted);
  }

  public String getPlainMessage() throws Exception {
    // TODO: unsign hash as well
//    this.unsignHash().hashVerify().decrypt();


    boolean sigVerify = verify("messagesign", sign, pair.getPublic());
    System.out.println("Verified signature: " + sigVerify);

    this.signatureVerify().decrypt();
    return this.toString();
  }

}
