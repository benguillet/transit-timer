package edu.stolaf.transit_timer;

import java.sql.Date;
import java.text.SimpleDateFormat;

import android.content.Intent;
import android.speech.RecognizerIntent;

/**
 * @author      Benjamin Guillet, Rogan Magee <guillet@stolaf.edu, magee@stolaf.edu> 
 * @version     1.0                 
 * @since       2013-01-29 
 * 
 * @param startTime 	Time when timer was toggled.
 * @param endTime		Time when timer was turned off.
 * @param elapsedTime	Time between the two.
 * @param currLocation	Text description of current location.
 * @param finalLocation Text description of final location (after timer off). 
 * 
 * The LocTimer class handles the timing of location service events. 
 * Gets location information from the LocationService. 
 */

public class LocTimer {
	double startTime, stopTime; 
	double elapsedTime; 
	String currLocation, finalLocation; 
	
	/**
	 * Constructor for LocTimer. Sets all state variables to empty. 
	 * 
     * @param none
     * @exception none
     * @return none
     */
	public LocTimer() {
		elapsedTime = startTime = stopTime = 0.0; 
		currLocation = "empty";
		finalLocation = "empty"; 
	}
	
	/**
	 * Starts the timer with the expectation that we're running the location service. 
	 * Sets the current location and then gets the system time. 
	 * If no service, it finishes. 
	 * 
     * @param none
     * @exception none
     * @return String The status of the timer: running or error due to location service. 
     */
	public String startTimer() {
		startTime = System.currentTimeMillis();
		if (LocationService.isStarted()) {
			if (currLocation.equals("empty")) {
				currLocation = LocationService.whereIam();
				if (currLocation == null) {
					currLocation = "empty";
				}
			}
			else {} 
			return "Timer attempted:" + currLocation;
		}
		else { 
			return "Location service off!"; 
		}
	} 
	
	/**
	 * Stops the timer with the expectation that we're running the location service.
	 * Then generates a query to record this time into the database.
	 * Calculates elapsed time based on milliseconds from system start.  
	 * 
     * @param none
     * @exception none
     * @return String The query to insert into the database, committing the time that was just recorded.  
     */
	public String stopTimer() {
		stopTime = System.currentTimeMillis();
		elapsedTime = ((stopTime - startTime) / 1000);
		if (LocationService.isStarted()) {
			finalLocation = LocationService.whereIam(); 
		}
		else {
			
		}
		
		return "time_info:" + currLocation + 
				":" + finalLocation + ":" + elapsedTime;
	}
	
	/**
	 * Returns a HH:MM:SS String from seconds input. 
	 * 
     * @param time Time in seconds. 
     * @exception none
     * @return String The HH:MM:SS value. 
     */
	public static String processTime(float time) {
		int t = (int)time; 
		int hours = t / (60*60);
		int minutes = (t % (60*60)) / (60); 
		int seconds = ((t % (60*60)) % (60)); 
		return "" + (hours < 10 ? ("0" + hours) : (hours)) + ":" + 
				(minutes < 10 ? ("0" + minutes) : (minutes)) + ":" +
				(seconds < 10 ? ("0" + seconds) : (seconds)); 
	}
}