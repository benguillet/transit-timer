package edu.stolaf.transit_timer;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * @author      Benjamin Guillet, Rogan Magee <guillet@stolaf.edu, magee@stolaf.edu> 
 * @version     1.0                 
 * @since       2013-01-29 
 * 
 * @param mMap The Google Map
 * @param mapFrag The MapFragment which contains the GoogleMap
 * @param options The GoogleMaps options
 * @param serviceIntent The intent used to launch our location service
 * @param markers An array of GoogleMap Markers
 * 
 * The TimedGoogleMap is an Activity which creates and handles a GoogleMap, 
 */
public class TimedGoogleMap extends android.support.v4.app.FragmentActivity implements OnMarkerClickListener, RecognitionListener {

	private static GoogleMap mMap;
	private MapFragment mapFrag;
	private GoogleMapOptions options;
	private Intent serviceIntent;
	private Marker[] markers;
	private SpeechRecognizer recognizer;

	private String deviceId; 
	private LocTimer timer; 

	private boolean searchMode;
	private boolean timerMode;
	private boolean listenMode;

	private String[] locationChoice;
	private boolean haveCurrLocation; 
	private String currLocation;
	private String desiredLocation;
	private String tempLocation;
	private static final String TAG = "transit_timer";
	private SQLiteDatabase db;

	public void doToast(CharSequence text) {
		Toast.makeText(this, text + "\n", Toast.LENGTH_SHORT).show();
	}

	
	/**
	 * onCreate sets up the view and create the Map
	 * 
	 * @param savedInstanceState saved bundle
	 * @exception none
	 * @return void
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_screen);
		mapFrag = MapFragment.newInstance();
		android.app.FragmentTransaction fragTransac = getFragmentManager().beginTransaction();
		fragTransac.add(android.R.id.content, mapFrag).commit();

		ActionBar actionBar = getActionBar();
		actionBar.show();
		actionBar.setDisplayHomeAsUpEnabled(true);

		timer = new LocTimer();

		deviceId = Main.getDeviceId(); 
		
		SQLiteManager sqlite = Main.getSQLiteManager();
		db = sqlite.getReadableDatabase();

		setUpTGM();

	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpTGM();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mMap = null;
	}

	public void setUpTGM() {
		options = new GoogleMapOptions();
		options.mapType(GoogleMap.MAP_TYPE_TERRAIN)
		.compassEnabled(false)
		.rotateGesturesEnabled(false)
		.tiltGesturesEnabled(false)
		.zoomGesturesEnabled(true);

		searchMode = timerMode = listenMode = false;

		deviceId = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId(); 

		currLocation = desiredLocation = "empty";
		locationChoice = new String[]{"empty", "empty"};
		haveCurrLocation = false; 

		setUpMapIfNeeded();

		recognizer = SpeechRecognizer.createSpeechRecognizer(this); 
		recognizer.setRecognitionListener(this); 
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			mMap = mapFrag.getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {

				// The Map is verified. It is now safe to manipulate the map.
				LatLng latLng = new LatLng(44.4598971, -93.1828771);
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
				mMap.animateCamera(cameraUpdate);
				mMap.setOnMarkerClickListener(this);


				String currLoc = "pouet";
				Log.d(TAG, Main.executeClient("INFLATE", "location", "eferfrgf"));
				Cursor c = Main.sqlite.readSQLiteDatabase("location");
				c.moveToNext();
				Log.d(TAG, c.getString(1));
				Log.d(TAG, addAllMarkers(db, currLoc) + "");

			}
		}

	}


	private Marker addMarker(float latitude, float longitude, String name, int icon) {
		MarkerOptions mo = new MarkerOptions()
		.position(new LatLng(latitude, longitude))
		.title(name);
		Marker mark = mMap.addMarker(mo);
		mark.setDraggable(false);

		return mark;
	}

	public static Marker addMarker(double latitude, double longitude, String name, BitmapDescriptor icon) {
		MarkerOptions mo = new MarkerOptions()
		.position(new LatLng(latitude, longitude))
		.title(name)
		.icon(icon);
		Marker mark = mMap.addMarker(mo);
		mark.setDraggable(false);

		return mark;
	}

	private boolean addAllMarkers(SQLiteDatabase db, String currLoc) {
		try {
			String[] columns = { "loc_name", "loc_lat", "loc_long" };
			Cursor cursor = db.query("location", columns, null, null, null, null, null);

			markers = new Marker[cursor.getCount()];

			int count = 0; 
			while (cursor.moveToNext()) {
				String name      = cursor.getString(0);
				String latitude  = cursor.getString(1);
				String longitude = cursor.getString(2);

				if (name.equals(currLoc))
					markers[count] = addMarker(Float.parseFloat(latitude), Float.parseFloat(longitude), name, 0);
				else
					markers[count] = addMarker(Float.parseFloat(latitude), Float.parseFloat(longitude), name, 1);

				count++; 
			}

			return true; 
		}
		catch (Exception e) {		

			return false;
		}
	}

	public void noLocationServiceTimer(String name) {
		if (timerMode && !listenMode) {
			if (timer.currLocation.equals("empty")) {
				timer.startTimer(); 
				doToast("Started manual timer");
				timer.currLocation = name; 
			}
			else {
				timer.finalLocation = name; 
				String res = timer.stopTimer(); 
				timerMode = false; 
				Main.executeClient("INSERT", res, deviceId);
				timer.currLocation = timer.finalLocation = "empty";
				doToast("Stopped manual timer");
			}
		}
	}

	public void handleTimer() {
		if (timerMode && LocationService.isStarted()) {
			String res = timer.stopTimer(); 
			Main.executeClient("INSERT", res, deviceId);
			timerMode = false; 
			timer.currLocation = timer.finalLocation = "empty";
		}
		else {
			if (LocationService.isStarted()) {
				doToast("Location service off!");
				timerMode = true;
			}
			else {
				String res = timer.startTimer(); 
				doToast(res);
				timerMode = true; 
			}
		}
		doToast("Timer is: " + (timerMode ? "ON" : "OFF"));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case 1: {
			if (resultCode == RESULT_OK && null != data) {
				ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				tempLocation = (text.get(0));
				if (verifyLocation(tempLocation))
				{}
				else
					tempLocation = "unrecognized";
			}
			break;
		}

		}
	}

	public boolean verifyLocation(String tempLocation) {
		try {
			String[] columns = { "loc_name", "loc_lat", "loc_long" };
			Cursor cursor = db.query("location", columns, null, null, null, null, null);

			while (cursor.moveToNext()) {
				String name      = cursor.getString(0);

				if (name.equals(tempLocation))
					return true;
				else
					continue;
			}

			return false; 
		}
		catch (Exception e) {		
			return false;
		}
	}

	public void handleSearch(Marker marker) {
		if (searchMode) {
			if (!haveCurrLocation) {
				currLocation = locationChoice[0] = marker.getTitle();
				haveCurrLocation = true;
			}
			else {
				desiredLocation = locationChoice[1]  = marker.getTitle();
				Arrays.sort(locationChoice);
				String res;
				if (deviceId != null)
					res = Main.executeClient("SELECT", "time_info:" + locationChoice[0] + ":" + locationChoice[1] + ":" + 10000000, deviceId);
				else
					res = Main.executeClient("SELECT", "time_info:" + locationChoice[0] + ":" + locationChoice[1] + ":" + 10000000, "all");
				doToast("Time between: " + currLocation + " and " + desiredLocation + " is: " + LocTimer.processTime(Float.parseFloat(res)));
				haveCurrLocation = false; 
				currLocation = desiredLocation = "empty";
				locationChoice[0] = locationChoice[1] = "empty";
			}

		}
		else {
			doToast("Search mode is: " + (searchMode ? "ON" : "OFF"));
		}
	}

	public void handleListen() {
		if (LocationService.isStarted()) {
			stopService(serviceIntent);
		}
		else {
			serviceIntent = new Intent("edu.stolaf.transit_timer.START_SERVICE");
			startService(serviceIntent);
		}
		doToast("Listen is: " + (LocationService.isStarted() ? "OFF" : "ON"));

	}

	protected boolean isRouteDisplayed() {
		return false;
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		// same as using a normal menu
		switch(item.getItemId()) {
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
		case R.id.search_mode:
			searchMode = !searchMode;
			doToast("Search mode is: " + (searchMode ? "ON" : "OFF"));
			break;
		case R.id.timer_mode:
			handleTimer();
			break;
		case R.id.listen_mode:
			listenMode = !listenMode;
			handleListen();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		handleSearch(marker);
		noLocationServiceTimer(marker.getTitle()); 
		return false;
	}

	public static GoogleMap getMap() {
		return mMap;
	}

	@Override
	public void onBeginningOfSpeech() {

	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEndOfSpeech() {

	}

	@Override
	public void onError(int error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReadyForSpeech(Bundle params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResults(Bundle results) {

	}

	@Override
	public void onRmsChanged(float rmsdB) {
		// TODO Auto-generated method stub

	}

	public Context getTimedGoogleMapContext() {
		return this;
	}
}