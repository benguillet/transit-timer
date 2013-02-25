package edu.stolaf.transit_timer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author      Benjamin Guillet, Rogan Magee <guillet@stolaf.edu, magee@stolaf.edu> 
 * @version     1.0                 
 * @since       2013-01-29 
 * 
 * @param telemgr 			A TelephonyManager for getting the deviceId. 
 * 
 * The MainMenu class handles the choices of the app, allowing us to launch different activities.
 * It also has an underlying XML layout that contains the debug and location TextViews. 
 * <p>
 */
public class MainMenu extends Activity implements OnClickListener  {
	
	private static TextView debug; 
	private TextView locationInfo;
	
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
	 * A helper function for logging debug messages thrown in program. 
	 * 
	 * @param err The message to append to debugger.  
	 * @exception none
	 * @return void
	 */
	public static void writeToDebugConsole(String err) {
		debug.append(err);
	}
	
	/**
	 * The onCreate function handles the creation of the activity. 
	 * 
	 * @param savedInstanceState Bundle which contains the user preferences. We don't make use of them. 
	 * @exception none
	 * @return void
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUpMenuWindow(); 
	}	

	/**
	 * The function which sets up the UI of the MainMenu, enabling all the listening
	 * for the buttons. 
	 * 
	 * @param none 
	 * @exception none
	 * @return void
	 */
	public void setUpMenuWindow() {
	
		setContentView(R.layout.debug_screen);
		debug   = (TextView) findViewById(R.id.debug); 
		locationInfo = (TextView) findViewById(R.id.location_info);
		
		setContentView(R.layout.menu_screen);

		Button map_button = (Button) findViewById(R.id.map_button);
		map_button.setOnClickListener(this);

		Button caf_button = (Button) findViewById(R.id.caf_button);
		caf_button.setOnClickListener(this);

		Button addcampus_button = (Button) findViewById(R.id.analytics_button);
		addcampus_button.setOnClickListener(this);

		Button about_button = (Button) findViewById(R.id.about_button);
		about_button.setOnClickListener(this);

		Button debug_button = (Button) findViewById(R.id.debug_button);
		debug_button.setOnClickListener(this);
	}


	/**
	 * Function to build and show the about dialog. 
	 * 
	 * @param none
	 * @exception none
	 * @return void
	 */
	public void buildAboutDialog() {
		Dialog d = new Dialog(this); 
		d.setTitle(R.string.about_label);
		d.setContentView(R.layout.about_dialog);
		d.show();
	}
	/**
	 * The function which controls clicks for the Button. 
	 * 
	 * @param v The View, meaning the Button, which has been clicked and which we need to control. 
	 * @exception none
	 * @return void
	 */
	@Override
	public void onClick(View v) {
		Intent newActivity;

		switch (v.getId()) {
		case R.id.map_button:
			newActivity = new Intent(this, TimedGoogleMap.class);
			startActivity(newActivity);
			break;
		case R.id.caf_button:
			newActivity = new Intent(this, CafScreen.class);
			startActivity(newActivity);
			break;
		case R.id.debug_button:
			setContentView(R.layout.debug_screen);
			break;
		case R.id.about_button:
			buildAboutDialog(); 
			break;
		case R.id.analytics_button:
			newActivity = new Intent(this, AnalyticsScreen.class);
			startActivity(newActivity);
			break;
		}
	}	
}
