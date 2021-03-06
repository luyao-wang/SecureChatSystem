import java.io.*;
import java.net.*;

public class Client {
	private static DataInputStream in;
	private static DataOutputStream out;
	private static BufferedReader buf;
	private static Socket soc;
	private static String myCipherSuite = "ecdh-secp224r1+nocert+AES/CBC/PKCS5Padding";
	private static KeyExchange myKey;
	private static Encryption cov;
	private static String keyEstAlgor;	// key establish algorithm
	private static String keyEstSpec;	// specific parameter for key establish algorithm
	private static String integrity;		// a means for ensuring integrity of public key
	private static String symCipher;		// symmetric cipher
	private static int ka1Length = ":ka1 ".length();
	
    public static void main (String args[]) throws Exception {
    		// greetings message
    		greeting();

		// initialization
		initialize(1235);
		
		// chat process
		run();
		
		// ending message
		ending();
    }
    
    /**
     * run the chat application in 4 phases
     *
     */
    public static void run() throws Exception{
    	// ******************* PHASE 1: send :ka cipherSuite ******************* //
		System.out.println("PHASE 1 :ka "+ myCipherSuite);
		out.writeUTF(":ka "+ myCipherSuite);
		out.flush();
		
		// ******************* PHASE 2: waiting for cipher suite confirmation ******************* //
		String receivedCipherSuite = "";
		while(receivedCipherSuite == "") {
			// ***** PHASE 2.1: receive :kaok ciphersuite ****** //
			try {
				receivedCipherSuite = in.readUTF();
				System.out.println("PHASE 2.1 " + receivedCipherSuite);
			} catch (Exception err) {
				System.err.println("NO RESPONSE FROM SERVER!");
				System.exit(-1);
			}
			
			// check whether received command equals to :kaok
			try {
				String kaok = getCommand(receivedCipherSuite);
				commandEqual(kaok, ":kaok");	
			} catch (ErrorException fail) {
				System.err.print(fail);
				System.exit(-1);
			}
			finally {
				// use the received cipher suite to generate key
				makeMyKey(getCipherSuite(receivedCipherSuite));
			}
		}
		
		// ******************* PHASE 3: waiting for key agreement ******************* //
		int receiveSize = 0;
		while (receiveSize == 0) {
			// ***** PHASE 3.1: receive :ka1 server's encoded public key ***** //
			byte[] ka1serverPublic =" ".getBytes();
			try {
				receiveSize = in.readInt();
				ka1serverPublic = new byte[receiveSize];
				in.readFully(ka1serverPublic);
				System.out.println("PHASE 3.1 :ka1 " + ka1serverPublic.toString());
			} catch (Exception err) {
				System.err.println("NO RESPONSE FROM SERVER!");
				System.exit(-1);
			}
			
			// check whether received command equals to :ka1
			try {
				byte[] ka1 = getKa1(ka1serverPublic);
				commandEqual(ka1, ":ka1 ");
			} catch (ErrorException fail) {
				System.err.print(fail);
				System.exit(-1);
			}
			
			finally {
				// ***** PHASE 2.2: send :ka1 based64 encoded public key ***** //
				byte[] encodedPublic = myKey.getEncodedPublic();
				byte[] ka1encodedPublic = addKa1(encodedPublic);
				System.out.println("PHASE 2.2 :ka1 "+ encodedPublic.toString());		
				out.writeInt(ka1encodedPublic.length);
				out.write(ka1encodedPublic);
				out.flush();
				
				// ***** PHASE 3.2: generate shared secret ***** //
				myKey.doECDH(splitEncodedPublic(ka1serverPublic));
			}
		}		
		
		// ******************* PHASE 4: chat w/ msg encryption ******************* //
		// strings to hold conversation contents
		String msgIn = "";
		String msgOut = "";
		String msgSecure = "";
	
		// initialize Encryption object
		cov = new Encryption(myKey.getSecret(), symCipher);
	
		// terminate chat if server says "end"
		while(!msgSecure.equals("bye")) {
			// read keyboard input for client's message
			System.out.print("You: ");
			msgOut = buf.readLine();
			
			// encrypt and display client's encrypted message
			msgSecure = cov.encrypt(msgOut);
			System.out.println("(Encrypted into cipher text: " + msgSecure + ")");
			
			// sent the encrypted message to server
			out.writeUTF(msgSecure);
			out.flush();
			
			if (msgOut.equals("bye")) {
				// terminate chat if client says "end"
				break;
			}
			else {
				// read and decrypt server's message 
				msgIn = in.readUTF();
				try {
					msgSecure = cov.decrypt(msgIn);
				} catch (ErrorException err) {
					System.err.print(err);
					System.exit(-1);
				}
				finally {
					// display received message
					System.out.print("Friend: ");
					System.out.println(msgSecure);
					
					// display the encrypted message from server before decryption
					System.out.println("(Decrypted from cipher text: " + msgIn + ")");
				}
			}
			
		}
		in.close();
		out.close();
		soc.close();
    }
	
	/**
     * initialize socket, data stream, and buffer reader
     * 
     * @param the port number for socket
     */
	public static void initialize(int portNum) throws Exception{
		// socket connect to server with server's IP (local) and port number
		soc = new Socket("127.0.0.1", portNum);	
		
		// stream for getting input and output
		in = new DataInputStream(soc.getInputStream());
		out = new DataOutputStream(soc.getOutputStream());

		// buffer to store message from keyboard input
		buf = new BufferedReader(new InputStreamReader(System.in));
	}
	
	/**
     * initialize KeyExchange object myKey with given cipher suite
     * 
     * @param the cipher suite sent by server
     */
	public static void makeMyKey(String receivedCipherSuite) throws Exception {
		// separate cipher Suite tokens
		String[] trim1 = receivedCipherSuite.split("\\+");	// separate tokens
		String[] trim2 = trim1[0].split("\\-");				// separate algorithm and spec. parameters
				
		// get the individual element of cipher suite
		keyEstAlgor = trim2[0];
		keyEstSpec = trim2[1];
		integrity = trim1[1];
		symCipher = trim1[2];					
		
		// initialize KeyExchange object
		myKey = new KeyExchange(keyEstAlgor, keyEstSpec, integrity);
	}
	
	
	/**
     * check for matching strings
     * 
     * @param two strings to be check
     * @return if matches, return TRUE; otherwise return FALSE
     */
	public static void commandEqual(String toBeCheck, String template) throws ErrorException{
		if	(toBeCheck.equals(template)) {}
		else {
			throw new ErrorException(":fail COMMAND " + template + " NOT FOUND!\n");
		}
	}
	
	/**
     * check for matching byte[] and string
     * conver the toBeCheck into string first then check with  template 
     * 
     * @param byte[] and strings to be check
     * @return if matches, return TRUE; otherwise return FALSE
     */
	public static void commandEqual(byte[] toBeCheck, String template) throws ErrorException{
		String toBeCheck_string = new String(toBeCheck);
		if	(toBeCheck_string.equals(template)) {}
		else {
			throw new ErrorException(":fail COMMAND " + template + " NOT FOUND!\n");
		}
	}
	
	/**
     * get the command string before the " "
     * 
     * @param wholeString including command and message
     * @return the string before " "
     */
	public static String getCommand(String wholeString) {
		String[] split = wholeString.split(" ");
		return split[0];
	}
	
	/**
     * get the cipher suite after the " "
     * 
     * @param wholeString including command and message
     * @return the string after " "
     */
	public static String getCipherSuite(String wholeString) {
		String[] split = wholeString.split(" ");
		return split[1];
	}
	
	/**
     * add byte[] of :ka1 to the base64 encode public key 
     * 
     * @param the base64 encoded string of public key
     * @return the combination of :ka1 and base64 encoded string of public key 
     */
	public static byte[] addKa1(byte[] encodedPublic) {
		byte[] ka1 = ":ka1 ".getBytes();
		byte[] combined = new byte[ka1Length + encodedPublic.length];

		for (int i = 0; i < combined.length; i++) {
		    combined[i] = i < ka1Length ? ka1[i] : encodedPublic[i - ka1Length];
		}
		return combined;
	}
	
	/**
     * get the byte[] of :ka1 
     * 
     * @param the combination of :ka1 and base64 encoded string of public key 
     * @return the byte[] of :ka1  
     */
	public static byte[] getKa1(byte[] combined) {
		byte[] ka1new = new byte[ka1Length];
		
		for (int i = 0; i < ka1Length; i++) {
			ka1new[i] = combined[i];
		}
		return ka1new;
	}
	
	/**
     * get byte[] of base64 encoded string of public key
     * 
     * @param the combination of :ka1 and base64 encoded string of public key 
     * @return byte[] of base64 encoded string of public key 
     */
	public static byte[] splitEncodedPublic(byte[] combined) {
		byte[] split = new byte[combined.length-ka1Length];
		
		for (int i = 0; i < split.length; i++) {
			split[i] = combined[i+ka1Length];
		}
		return split;
	}
	
	/**
     * display greeting message
     */
	public static void greeting() {
		System.out.println("************** Welcome to the Chat Hub! **************");
		System.out.println("*  - Your friend is starting a chat with you         *");
		System.out.println("*  - Please wait for your friend's first message     *");
		System.out.println("*  - When finish, enter \"bye\" to exit the chat       *");
		System.out.println("******************************************************");
	}
	
	/**
     * display ending message
     */
	public static void ending() {
		System.out.println("\n************ Thanks for chatting! ************\n");
	}
}