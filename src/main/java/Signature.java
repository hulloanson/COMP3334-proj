package main.java;
import java.security.*;
import java.io.FileOutputStream;

/**
 * Created by Ian & Tony on 03/12/2017.
 */
public class Signature {
    public static void main(String[] args) throws Throwable{

        /*Hash an input
        String test="abc";
        byte [] input=test.getBytes();
        MessageDigest hi=MessageDigest.getInstance("SHA-256");
        hi.update(input);
        byte [] digest=hi.digest();
        String mystring=new String(digest);
        System.out.println(mystring);
        */
        Signature signature = new Signature();
        Message.Signature();

        // Generate public key and private key
        SecureRandom rand = new SecureRandom();
        rand.setSeed(input);
        KeyPairGenerator key = KeyPairGenerator.getInstance("Signature");
        key.initialize(1024,rand);

        KeyPair pair = key.generateKeyPair();
        PrivateKey private = pair.getPrivate();
        PublicKey public = pair.getPublic();
        // print the two key
        System.out.println("Public Key:" + public);
        System.out.println("Private Key:" + private);

        // get the signature
        java.security.Signature sign = java.security.Signature.getInstance("sha256withrsa");
        // initialize the signature
        sign.initSign(private);

        // add content to the sign object
        sign.update(data);
        // Sign on the hash
        byte[] realSig = sign.sign();
        String mystring1 = new String(realSig);
        System.out.println("Signed:"+mystring1);

        // verify the signature
        java.security.Signature ver = java.security.Signature.getInstance("sha256withrsa");
        ver.initVerify(public);
        ver.update(data);
        boolean verifies = ver.verify(realSig);

        //print true if the content has not been modified
        System.out.println("Signature verifies: " + verifies);
    }
}
