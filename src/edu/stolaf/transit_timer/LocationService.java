package edu.stolaf.transit_timer;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class LocationService extends Service implements LocationListener {
	private final static String TAG = "transit_timer";
	private LocationManager mgr;
	private String bestProvider;
	private SQLiteDatabase db; 
	private static Location location;
	private static String currLoc;
	private Marker currentLocMarker;
	private static boolean serviceStarted;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "LocationService created!");
		this.currentLocMarker = null;
		LocationService.serviceStarted = false;
		
		mgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		
		//Location gpsLocation     = getLocationByProvider(LocationManager.GPS_PROVIDER);
	    //Location networkLocation = getLocationByProvider(LocationManager.NETWORK_PROVIDER);
		
		bestProvider = mgr.getBestProvider(criteria, true);
		Log.d(TAG,"Best provider: " + bestProvider); 
		mgr.requestLocationUpdates(bestProvider, 2000, 1, this);
		
		db = Main.getSQLiteManager().getReadableDatabase();
		currLoc = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mgr.removeUpdates(this);
		LocationService.serviceStarted = false;
		if (currentLocMarker != null) {
			currentLocMarker.remove();
		}
		Log.d(TAG, "LocationService stopped!");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "LocationService onStartCommand!");
		LocationService.serviceStarted = true;
		//getTimedGoogleMapContext
		String res = Main.executeClient("INFLATE", "location", "unnecessary");
		Log.d(TAG,res); 
		scanCurrLocation(db);

		return super.onStartCommand(intent, flags, startId);
	}

	public Location scanCurrLocation(SQLiteDatabase db) {
		currLoc = null; 

		Cursor cursor; 

		try {
			String[] columns = {"loc_name", "loc_lat", "loc_long" }; 

			cursor = db.query(false, "location", columns, null, null, null, null, null, null);
		}
		catch (Exception e) {
			Log.d(TAG, "ERROR with SQlite in scanCurrLocation: " + e.getMessage()); 
			return null;
		}

		try {
			if (bestProvider != null) {
				location = mgr.getLastKnownLocation(bestProvider);
			}
		}
		catch (Exception e) {
			Log.d(TAG, "ERROR with scanCurrLocation: " + e.getMessage()); 
			return null;
		}

		try {
			while (cursor.moveToNext()) {
				String name      = cursor.getString(0);
				double latitude  = cursor.getDouble(1);
				double longitude = cursor.getDouble(2); 

				double la = location.getLatitude() -  latitude;
				double lo = location.getLongitude() - longitude; 
				
				/* bind location data to location */
				Location temp_loc = new Location("temp");
				temp_loc.setLatitude(latitude);
				temp_loc.setLongitude(longitude);
				
				if (location.distanceTo(temp_loc) < location.getAccuracy()) {
					currLoc = name; 
					break; 
				}
			}

			return location;
		}
		catch (Exception e) {
			Log.d(TAG, "ERROR with scanCurrLocation: " + e.getMessage()); 
			return null;
		}
	}

	public static String whereIam() {
		return currLoc;
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d(TAG, scanCurrLocation(db).toString());
		Log.d(TAG, "I am here: " + whereIam());
		double latitute  = LocationService.getCurrentLocation().getLatitude();
		double longitude = LocationService.getCurrentLocation().getLongitude();  
		LatLng latLng = new LatLng(latitute, longitude);
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
		GoogleMap mMap = TimedGoogleMap.getMap();
		if (mMap == null) {
			mMap = TimedGoogleMap.getMap();
			if (mMap != null) {
				mMap.animateCamera(cameraUpdate);
			}
		}
		if (currentLocMarker != null) {
			currentLocMarker.remove();
		}
		currentLocMarker = TimedGoogleMap.addMarker(latitute, longitude, "currentLoc", BitmapDescriptorFactory.fromResource(R.drawable.current_loc));
		
	}
	
	public static boolean isStarted() {
		return serviceStarted;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	public static Location getCurrentLocation() {
		return location;
	}


}
