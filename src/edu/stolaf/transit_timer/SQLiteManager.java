package edu.stolaf.transit_timer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author      Benjamin Guillet, Rogan Magee <guillet@stolaf.edu, magee@stolaf.edu> 
 * @version     1.0                 
 * @since       2013-01-29 
 * 
 * @param DATABASE_NAME The name of our SQLite database. 
 * @param DATABASE_VERSION The version of our database. 
 * 
 * The SQLiteManager handles the construction and maintenance of our local SQLite database. 
 */
public class SQLiteManager extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "transit_timer.db";
	private static final int DATABASE_VERSION = 1;

	/**
	 * onCreate sets up the database by populating it with two tables. 
	 * 
	 * @param db The Database object we're handling
	 * @exception none
	 * @return void
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE location (" +
				"loc_name text primary key," + 
				"loc_lat float, " + 
				"loc_long float " + 
				");");
		db.execSQL("CREATE TABLE user (" +
				"user_name text primary key," + 
				"user_loc_name text references location(loc_name), " + 
				"user_arrive_time float, " + 
				"user_leave_time float " + 
				");");
	}

	/**
	 * onUpgrade drops our records on request
	 * 
	 * @param db The database object we're handling.
	 * @param oldVersion The old version number.
	 * @param newVersion The new version number.
	 * @exception none
	 * @return none
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS location;");
		db.execSQL("DROP TABLE IF EXISTS user;");
		onCreate(db);
	}
	
	/**
	 * The Constructor calls the SQLiteHelper constructor with the appropriate database name.
	 * This is where the actual storage is managed. 
	 * 
	 * @param none
	 * @exception none
	 * @return none
	 */
	public SQLiteManager(Context ctx) { 
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	/**
	 * Resets the tables that we have in our database. 
	 * 
	 * @param db The database object. 
	 * @exception none
	 * @return void
	 */
	public void resetLocations(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS location;");
		db.execSQL("DROP TABLE IF EXISTS times;");
		db.execSQL("CREATE TABLE location (" +
				"loc_name text primary key," + 
				"loc_lat float, " + 
				"loc_long float " + 
				");");
		db.execSQL("CREATE TABLE times (" +
				"loc_from text references location(loc_name)," + 
				"loc_to text references location(loc_name), " + 
				"time int not null, " + 
				"time_entered int not null, " +
				"username text " +
				");");
	}

	/**
	 * Pulls messages from Backend and puts the contents into the database until terminate signal is reached.  
	 * 
	 * @param in The Socket associated with the Backend connection. 
	 * @exception none
	 * @return String The error message that was pulled from the connection. 
	 */
	public String inflateDatabase(Socket in) {
		StringBuilder txt = new StringBuilder();
		SQLiteDatabase db = getReadableDatabase();
		
		resetLocations(db);
		
		// receiving a message of type INSERT-table_name:loc_name:loc_lat:loc_long
		BufferedReader br = null;
		try {
			br = new BufferedReader (new InputStreamReader(in.getInputStream()));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String mess = null; 
		boolean reading = true; 
		while (reading) {
			
			try {
				mess = br.readLine();
				txt.append("Received: " + mess + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (mess.equals("INSERT-inflate_finished-")) {
				reading = false; break; }
			else {
				String[] a = mess.split(":");
				if (a[0].contains("location")) {
					ContentValues values = new ContentValues();
					values.put("loc_name",a[1]);
					values.put("loc_lat",Float.parseFloat(a[2]));
					values.put("loc_long",Float.parseFloat(a[3]));
					db.insert("location", null, values);
					txt.append("Committed row\n");
				}
				else if (a[0].contains("time_info")) {
					ContentValues values = new ContentValues();
					values.put("loc_from",a[1]);
					values.put("loc_to",a[2]);
					values.put("time",Integer.parseInt(a[3]));
					values.put("time_entered",Integer.parseInt(a[4]));
					values.put("username",a[5]);
					db.insert("times", null, values);
					txt.append("Committed row\n");
				}
			}
		}
		return txt.toString();
	}
	
	/**
	 * This function grabs a cursor which pulls all roles out of the relation specified. 
	 * 
	 * @param table The relation we want to traverse. 
	 * @exception none
	 * @return Cursor A manager for the database object. 
	 */
	public Cursor readSQLiteDatabase(String table) {
		String[] FROM = null;
		SQLiteDatabase db = getReadableDatabase(); 
		
		Cursor cursor = db.query(table, FROM, null, null, null,
				null, null);
		
		return cursor; 
	}
}