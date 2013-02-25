package edu.stolaf.transit_timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author      Benjamin Guillet, Rogan Magee <guillet@stolaf.edu, magee@stolaf.edu> 
 * @version     1.0                 
 * @since       2013-01-29 
 * 
 * @param telemgr 			A TelephonyManager for getting the deviceId.
 * @param debug 			The debug TextView.
 * @param locationInfo 		The location information TextView. 
 * @param sqlite			The database manager for our local sqlite database. 
 * 
 * The Main activity launches the application, registers the user, contains a
 * backing debug and location info textbox, and then forwards control flow on. 
 * <p>
 */
public class Main extends Activity  {

	private static TelephonyManager telemgr; 
	public static SQLiteManager sqlite; 
	private static String deviceId; 
	
	/**
	 * A helper function for making onscreen Toasts. 
	 * 
	 * @param text The message to toast.  
	 * @exception none
	 * @return void
	 */
	public void doToast(CharSequence text) {
		Toast.makeText(this, text + "\n", Toast.LENGTH_LONG).show();
	}

	/**
	 * A helper function for executing desired clients from anywhere in the package.  
	 * 
	 * @param type 		The type of message desired.
	 * @param message   The message desired.
	 * @param entry		The EditText to pull values from.
	 * @param result	A double for putting into the message if desired.
	 * @param id		The id to tie to this transaction.
	 * 
	 * @exception none
	 * @return String 	The result or error associated with the Client run. 
	 */
	public static String executeClient(String type, String message, EditText entry, double result, String id) {
		Client client = new Client(type, message, entry, result, id);
		Client.launchClient(client); 
		if (client.getErr().contains("ERROR")) {
			MainMenu.writeToDebugConsole(client.getErr());
		}
		else {
			return ("ERROR handling client");
		}

		return client.getResult();
	}

	/**
	 * A helper function for executing desired clients from anywhere in the package.
	 * This one takes a fully processed message.  
	 * 
	 * @param type 		The type of message desired.
	 * @param message   The message desired.
	 * @param id		The id to tie to this transaction.
	 * 
	 * @exception none
	 * @return String 	The result or error associated with the Client run. 
	 */
	public static String executeClient(String type, String message, String id) {
		try {
			Client client = new Client(type, message, id);
			Client.launchClient(client); 
			if (client.getErr().contains("ERROR")) {
				return ("ERROR: " + client.getErr());
			}

			if (client.getResult() != null) {
				return client.getResult();
			}
			else {
				return "no results";
			}
		} 
		catch (Exception e) {
			return "Error launching client";
		}

	}

	/**
	 * The onCreate method, which instantiates the Activity life cycle. 
	 * 
	 * @param savedInstanceState 		The preference Bundle for this object. 
	 *
	 * @exception none
	 * @return void
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		sqlite = new SQLiteManager(this); 

		deviceId = Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID);
		//registerUser();
		Intent newActivity = new Intent(this, MainMenu.class);
		startActivity(newActivity);

		if (LocationService.getCurrentLocation() != null) {
			MainMenu.writeToDebugConsole(LocationService.getCurrentLocation().toString());
		}
	}	

	/**
	 * The ActionBar inflater. In this context, we aren't really using it yet.  
	 * 
	 * @param menu 		The Menu object tied to the UI ActionBar. 
	 * 
	 * @exception none
	 * @return boolean 	Whether we successfully inflated the menu. 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/**
	 * Function to register the deviceID with the user database stored in our backend.
	 * We use the IMEI to associate a unique tag with each user. 
	 * 
	 * @param none
	 * @exception none
	 * @return String 	The result or error associated with the Client run. 
	 */
	private void registerUser() {
		telemgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String resp = executeClient("SELECT", "user_info:" + getDeviceId(), getDeviceId());
		handleResponse(resp);
	}

	public static String getDeviceId() {
		return deviceId;
	}

	/**
	 * Function to handle the response from the Backend about the user's identity.
	 * Either asks for a tag name or says hello!
	 * 
	 * @param resp	The response from the database connection socket. 
	 * @exception none
	 * @return void
	 */
	private void handleResponse(String resp) {
		final Intent newActivity = new Intent(this, MainMenu.class);

		if (resp.equals("need_email")) {
			MainMenu.writeToDebugConsole("New user! Need your email.\n");

			LayoutInflater factory = LayoutInflater.from(this);

			final View textEntryView = factory.inflate(R.layout.login_name, null);
			new AlertDialog.Builder(this)
			.setTitle(R.string.loginPrompt)
			.setView(textEntryView)
			.setPositiveButton("Go!",
					new DialogInterface.OnClickListener() {

				String user = null;
				public void onClick(DialogInterface dialoginterface, int i) {
					final EditText usr = (EditText) findViewById(R.id.login_text);
					
					try {
						user = usr.getText().toString();
						String err = executeClient("INSERT", "user_info:" + getDeviceId() + ":" + 
								user, usr.getText().toString());
						startActivity(newActivity);
						MainMenu.writeToDebugConsole(err);
					} catch (Exception e) {
						MainMenu.writeToDebugConsole("newUser ERROR: " + e.getMessage());				
					}
				}
			})
			.show();
		}
		else if (resp.contains("have_email")) {
			String[] u = resp.split("have_email"); 
			MainMenu.writeToDebugConsole("Welcome back " + u[0] + "!\n");
			doToast("Welcome back " + u[0] + "!\n");
			startActivity(newActivity);
		}
	}	

	/**
	 * Static getter for the database access method we've defined: SQLiteManager.
	 * 
	 * @param none
	 * @exception none
	 * @return SQLiteManager The manager to pass back through the chain. Exposes our sqlite database. 
	 */
	public static SQLiteManager getSQLiteManager() {
		return sqlite;
	}
}
