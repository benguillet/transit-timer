package edu.stolaf.transit_timer;

import java.sql.Date;
import java.text.SimpleDateFormat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author      Benjamin Guillet, Rogan Magee <guillet@stolaf.edu, magee@stolaf.edu> 
 * @version     1.0                 
 * @since       2013-01-29 
 * 
 * @param avgWindow  			An EditText for determining the window over which we take the average. 
 * @param webview 				A WebView for getting the caf information. 
 * @param deviceId 				The String deviceId to ship database queries with. 
 * @param avgTime 				The TextView to display the average time for the caf. 
 * @param inBuilding 			The TextView to display how many people are in the building. 
 * 
 * The activity which controls the caf screen. This screen allows the user to see how many people are in the caf
 * and what the average time between start and end is over a mutable window value. 
 * <p>
 */
public class CafScreen extends Activity implements OnClickListener  {
	
	private EditText avgWindow;	
	private WebView webview;
	private String deviceId;
	private TextView avgTime;
	private TextView inBuilding; 
	
	/**
     * @param text Text which we'll toast to the user.
     * @exception none
     * @return void
     */
	public void doToast(CharSequence text) {
		Toast.makeText(this, text + "\n", Toast.LENGTH_LONG).show();
	}

	/**
	 * The method called when this activity's lifestyle begins.
	 * 
     * @param savedInstanceState Saved bundle of preferences. We don't make use of this. 
     * @exception none
     * @return void
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUpCafWindow(); 
		
		ActionBar actionBar = getActionBar();
		actionBar.show();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		deviceId = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
	}	

	/**
	 * A function to set the content view to the xml defined for the caf screen and to grab all
	 * objects on it for editing. These are expected to be TextViews, EditTexts, and a WebView 
	 * for displaying information about the caf. 
	 * 
     * @param none
     * @exception none
     * @return void
     */
	public void setUpCafWindow() {

		setContentView(R.layout.caf_screen);

		Button refresh = (Button) findViewById(R.id.refresh);
		refresh.setOnClickListener(this); 

		Button daily_menu = (Button) findViewById(R.id.daily_menu);
		daily_menu.setOnClickListener(this); 

		Button about_caf = (Button) findViewById(R.id.about_caf);
		about_caf.setOnClickListener(this); 

		avgTime      = (TextView) findViewById(R.id.avg_time);
		inBuilding   = (TextView) findViewById(R.id.in_building_caf);
		avgWindow    = (EditText) findViewById(R.id.avg_window);
		webview      = (WebView) findViewById(R.id.web);
		
		avgWindow.setText("20");
		String res = Main.executeClient("SELECT","in_building:caf_seating:" + avgWindow.getText().toString(), "all");
		inBuilding.setText(" " + Integer.parseInt(res) + " people");
		String res2 = Main.executeClient("SELECT","time_info:caf_entrance:caf_seating:" + Integer.parseInt(avgWindow.getText().toString()), "all");
		avgTime.setText(" " + LocTimer.processTime(Float.parseFloat(res2)));
	}

	/**
	 * Function to control button clicks for the buttons we have available on this screen. 
	 * 
     * @param none
     * @exception none
     * @return void
     */
	@Override
	public void onClick(View v) {
		String[] res = new String[10]; 
		
		switch (v.getId()) {
		case R.id.refresh:
			try {
				res[0] = Main.executeClient("SELECT","in_building:caf_seating:" + avgWindow.getText().toString(), "all");
				inBuilding.setText(" " + Float.parseFloat(res[0]) + " people");
				res[1] = Main.executeClient("SELECT","time_info:caf_entrance:caf_seating:" + Integer.parseInt(avgWindow.getText().toString()), "all");
				avgTime.setText(" " + LocTimer.processTime(Float.parseFloat(res[1])) + " seconds");
			}
			catch (Exception e) {
				doToast("Please enter a number into the 'average over time' field");
			}
			break;
		case R.id.daily_menu:
			webview.loadUrl("http://m.cafebonappetit.com/menu/your-cafe/stolaf");
			webview.requestFocus();
			break;
		case R.id.about_caf:
			webview.loadUrl("http://m.stolaf.edu/dining/stavhall");
			webview.requestFocus(); 
			break; 
		}
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // This is called when the Home (Up) button is pressed
	            // in the Action Bar.
	            Intent parentActivityIntent = new Intent(this, MainMenu.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
}
