package edu.stolaf.transit_timer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.telephony.TelephonyManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author      Benjamin Guillet, Rogan Magee <guillet@stolaf.edu, magee@stolaf.edu> 
 * @version     1.0                 
 * @since       2013-01-29 
 * 
 * @param socket The Socket in this Client.	
 * @param outStream The OutputStream of this Client.
 * @param inStream The InputStream of the client.
 * @param op The type of operation to run.
 * @param message The content of the operation to run. 
 * 
 * The Client class implements Runnable so that we can call a new thread to interact with the
 * Postgres DB via a new Socket. We can't run sockets in the main Activity thread, so we must
 * do so via these Clients.
 */
public class Client implements Runnable {
	private Socket socket;
	private OutputStream outStream;
	private InputStream inStream;
	private String op;
	private String message;
	private EditText timeEntry;
	private double elapsedTime;
	private String err;
	private String result;
	private String deviceId; 

	/**
	 * Constructor for a client. This one allows the client access to the TextView associated with a function we're targeting. 
	 * 
	 * @param none
	 * @exception none
	 * @return none
	 */
	Client(String op, String message, EditText timeEntry, double elapsedTime, String deviceId) {
		this.op = op;
		this.message = message;
		this.timeEntry = timeEntry;
		this.elapsedTime = elapsedTime;
		this.err = null;
		this.result = null;
		this.deviceId = deviceId; 
	}

	/**
	 * Constructor for a client. This one does not have access to other objects, only the message type, content, and the source. 
	 * 
	 * @param none
	 * @exception none
	 * @return none
	 */
	Client(String op, String message, String deviceId) {
		this.op = op;
		this.message = message;
		this.err = null;
		this.result = null; 
		this.timeEntry = null;
		this.elapsedTime = -1;
		this.deviceId = deviceId; 
	}

	/**
	 * Function to launch client by calling a Thread and then waiting for its activity to finish (we don't need high level parallel
	 * performance. 
	 * 
	 * @param client A new instance of Client. The function is static, so it can be called without instantiating a Client object. 
	 * @exception none
	 * @return void
	 */
	public static void launchClient(Client client) {
		Thread t = new Thread(client);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * The run() method which we need to override for Runnable. Establishes a new connection and then writes a 
	 * message to the OutputStream, waiting for a response over the InputStream. 
	 * 
	 * @param none
	 * @exception none
	 * @return void
	 */
	@Override
	public void run() {

		try {
			socket    = new Socket("rms201-3.cs.stolaf.edu", 27331);
			outStream = socket.getOutputStream();
			inStream  = socket.getInputStream();
		} catch (Exception e) {
			err = "Invalid connection!"; 
		}

		try {

			if (op.equals("SELECT")) { 

				if (deviceId.equals("all"))
					outStream.write(("SELECT-" + message + ":all" + "-\n").getBytes());
				else
					outStream.write(("SELECT-" + message + ":" + deviceId + "-\n").getBytes());
			}
			else if (op.equals("INSERT")) {
				if (timeEntry == null) {
					outStream.write(("INSERT-" + message + ":" + System.currentTimeMillis() + ":" + deviceId + "-\n").getBytes());	
				}
				else {
					if (timeEntry.getText().toString().contains("timer")) {
						outStream.write(("INSERT-" + message + (int)Math.round(elapsedTime) + ":"
								+ System.currentTimeMillis() + ":" + deviceId + "-\n").getBytes());
					}
					else {
						outStream.write(("INSERT-" + message + Integer.parseInt(timeEntry.getText().toString()) + ":"
								+ System.currentTimeMillis() + ":" + deviceId + "-\n").getBytes());
					}
				}
			}
			else if (op.equals("INFLATE")) {
				this.result = inflateDatabase(message);
				Main.getSQLiteManager().readSQLiteDatabase("location"); 
			}

			if (!op.equals("INFLATE")) {
				outStream.flush();
				result = getResponse(inStream);
			}

		} catch (IOException e) {
			err = "dbQuery IO ERROR: " + e.getMessage() + "\n";
		}

		err += "Connected, closing.\n";
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function to get the parsed message from the response sent from the server. Messages
	 * are split up on the main (type, content) by the - character, so we just return whatever
	 * is to the right of it. 
	 * 
	 * @param inStream The InputStream of the Socket we're connecting over. 
	 * @exception none
	 * @return String The content of the message response. 
	 */
	private String getResponse(InputStream inStream) {
		try { 
			String mess = null; 
			BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
			mess = br.readLine(); 
			err = "Read: " + mess.length() + " bytes.\n";
			String[] data = mess.toString().split("-");
			err += "Grabbed message: " +
					mess + " from stream.\n";
			return data[1]; }
		catch (IOException e) { 
			MainMenu.writeToDebugConsole("getMessage ERROR: " + e.getMessage() + "\n");
			return null; 
		}
	}

	/**
	 * Function to ask for an INFLATE command from the PostgresDB. This needs
	 * to be handle separately because the transmission will involve multiple
	 * response messages which must be handled by the same Client. 
	 * 
	 * @param s A message to be sent with the INFLATE request, referring to the 
	 * database which is being requested. 
	 * @exception none
	 * @return String Any errors which are thrown when sending the message. 
	 */
	private String inflateDatabase(String s) {
		this.err = null;
		try {
			outStream.write(("INFLATE-" + s + "-\n").getBytes());
			outStream.flush(); 
			result = Main.getSQLiteManager().inflateDatabase(socket); 
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			MainMenu.writeToDebugConsole("sqLite ERROR: " + e.getMessage() + "\n");
			err = ("sqLite ERROR: " + e.getMessage() + "\n");
		}
		return err;
	}

	/**
	 * Getter for the OutputStream of the Client Socket. 
	 * 
	 * @param none
	 * @exception none
	 * @return OutputStream The OutputStream of the Client. 
	 */
	public OutputStream getOutputStream() {
		return outStream; 
	}

	/**
	 * Getter for the InputStream of the Client Socket. 
	 * 
	 * @param none
	 * @exception none
	 * @return InputStream The InputStream of the Client. 
	 */
	public InputStream getInputStream() {
		return inStream; 
	}

	/**
	 * Getter for the Socket of the Client. 
	 * 
	 * @param none
	 * @exception none
	 * @return Socket The Socket of the Client. 
	 */
	public Socket getSocket() {
		return this.socket;
	}

	/**
	 * Getter for the err String from the Client (where errors are stored). 
	 * 
	 * @param none
	 * @exception none
	 * @return String The accumulated error message from a run of this Client. 
	 */
	public String getErr() {
		return this.err;
	}

	/**
	 * Getter for the result String from the Client (where results of queries are stored). 
	 * 
	 * @param none
	 * @exception none
	 * @return String The accumulated result message from a run of this Client. 
	 */
	public String getResult() {
		return this.result;
	}
}