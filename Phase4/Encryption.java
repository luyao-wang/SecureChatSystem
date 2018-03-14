import javax.crypto.*;
import javax.crypto.spec.*;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Encryption {
	private SecretKey secretKey;
	private String algorithm;
	private static byte[] iv = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	private static final int TAG = 128;
	
	/**
     * Initialize the secretKey with keyBytes
     *
     */
	public Encryption(byte[] secretBytes, String algorithm) {
		// secret key initialization with key bytes of 16 zeros
	    secretKey = new SecretKeySpec(secretBytes, "AES");
		this.algorithm = algorithm;
		System.out.println("PHASE 4   shared secret key: " + secretKey);
	}
	
	/**
     * Encrypt a string with AES_128/GCM/NoPadding algorithm.
     *
     * @param message is the plain text
     * @return the encrypted cipher text
     */
	public String GCMencrypt(String plainText) throws Exception {		
		// initialize cipher with secret key
		Cipher cipher = Cipher.getInstance(algorithm);
		GCMParameterSpec gcmspec = new GCMParameterSpec(TAG,iv);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmspec);
		
		// encrypt plain text
		byte[] cipherText = cipher.doFinal(plainText.getBytes());
		return Base64.getEncoder().encodeToString(cipherText);
	}
	
	/**
     * Decrypt a string with AES_128/GCM/NoPadding algorithm.
     *
     * @param message is the cipher text
     * @return the decrypted plain text
     */
	public String GCMdecrypt(String cipherText) throws Exception, ErrorException {
		byte[] plainText;
		try {
			// initialize cipher with secret key
			Cipher cipher = Cipher.getInstance(algorithm);
			GCMParameterSpec gcmspec = new GCMParameterSpec(TAG,iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmspec);
		
			// decrypt cipher text
			byte[] decoder  = Base64.getDecoder().decode(cipherText);
			plainText = cipher.doFinal(decoder);
		} catch (Exception err) {
			throw new ErrorException(":err DECRYPTION FAILED!\n");
		} 
		
		String str = new String(plainText, "UTF-8");
		return str;
	}
	
	/**
     * Encrypt a string with AES/CBC/PKCS5Padding algorithm.
     *
     * @param message is the plain text
     * @return the encrypted cipher text
     */
	public String encrypt(String plainText) throws Exception {		
		// initialize cipher with secret key
		Cipher cipher = Cipher.getInstance(algorithm);
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
		
		// encrypt plain text
		byte[] cipherText = cipher.doFinal(plainText.getBytes());
		return Base64.getEncoder().encodeToString(cipherText);
	}
	
	/**
     * Decrypt a string with AES/CBC/PKCS5Padding algorithm.
     *
     * @param message is the cipher text
     * @return the decrypted plain text
     */
	public String decrypt(String cipherText) throws Exception, ErrorException {
		byte[] plainText;
		try {
			// initialize cipher with secret key
			Cipher cipher = Cipher.getInstance(algorithm);
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
		
			// decrypt cipher text
			byte[] decoder  = Base64.getDecoder().decode(cipherText);
			plainText = cipher.doFinal(decoder);
		} catch (Exception err) {
			throw new ErrorException(":err DECRYPTION FAILED!\n");
		} 
		
		String str = new String(plainText, "UTF-8");
		return str;
	}
}