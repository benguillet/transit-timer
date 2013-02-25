package edu.stolaf.transit_timer;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.series.XYSeries;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeriesFormatter;
/**
 * Code based HEAVILY on the examples at androidplot.com/ 
 * No but seriously, it's based almost entirely on their demo code
 * We hacked together the data retrieval functions
 * 
 * Thank you to them for this fantastic library!!!
 * 
 */
public class AnalyticsScreen extends Activity implements OnTouchListener
{
	private XYPlot mySimpleXYPlot;
	private PointF minXY;
	private PointF maxXY; 

	private TextView labels; 

	// Definition of the touch states
	static final int NONE = 0;
	static final int ONE_FINGER_DRAG = 1;
	static final int TWO_FINGERS_DRAG = 2;
	int mode = NONE;
	PointF firstFinger;
	float lastScrolling;
	float distBetweenFingers;
	float lastZooming; 
	public String series_type = null; 
	public int spacing = 20; 
	
	public void doToast(CharSequence text) {
		Toast.makeText(this, text + "\n", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xy_plot);

		ActionBar actionBar = getActionBar();
		actionBar.show();
		actionBar.setDisplayHomeAsUpEnabled(true);

		labels = (TextView) findViewById(R.id.series_labels);
		labels.setText("Series labels..."); 

		// initialize our XYPlot reference:
		mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		mySimpleXYPlot.setOnTouchListener(this);

		//Plot layout configurations
		mySimpleXYPlot.getGraphWidget().setTicksPerRangeLabel(1);
		mySimpleXYPlot.getGraphWidget().setTicksPerDomainLabel(1);
		mySimpleXYPlot.getGraphWidget().setRangeValueFormat(
				new DecimalFormat("#####.##"));
		mySimpleXYPlot.getGraphWidget().setDomainValueFormat(
				new DecimalFormat("#####.##"));
		mySimpleXYPlot.getGraphWidget().setRangeLabelWidth(25);
		mySimpleXYPlot.setRangeLabel("");
		mySimpleXYPlot.setDomainLabel("");
		mySimpleXYPlot.disableAllMarkup(); 

		mySimpleXYPlot.redraw();

		//Set of internal variables for keeping track of the boundaries
		mySimpleXYPlot.calculateMinMaxVals();
		minXY=new PointF(mySimpleXYPlot.getCalculatedMinX().floatValue(),mySimpleXYPlot.getCalculatedMinY().floatValue());
		maxXY=new PointF(mySimpleXYPlot.getCalculatedMaxX().floatValue(),mySimpleXYPlot.getCalculatedMaxY().floatValue());
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.analytics_menu, menu);
		return true;
	}

	public void makeChart(String type) {
		Number[] series = getSeriesFromDatabase(type);

		StringBuilder app = new StringBuilder(); 

		int[] lineCol  = {0, 100, 0};
		int[] pointCol = {0, 200, 0}; 
		addSeries(mySimpleXYPlot, getXYSeriesYValsOnly(series,"all_times"), lineCol, pointCol);
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
		case R.id.all_times:
			makeChart("all_times");
			break;
		case R.id.fastest_contributors:
			makeChart("fastest_contributors");
			break;
		case R.id.top_contributors:
			makeChart("top_contributors");
			break;
		case R.id.time_distribution:
			makeChart("time_distribution");
			break;
		}

		return true;
	}

	public void addSeries(XYPlot xyplot, XYSeries series1, int[] lineCol, int[] pointCol) {

		LineAndPointFormatter series1Format = new LineAndPointFormatter(
				Color.rgb(lineCol[0], lineCol[1], lineCol[2]),                   // line color
				Color.rgb(pointCol[0], pointCol[1], pointCol[2]),                   // point color
				null);                                  // fill color (none)

		// add a new series' to the xyplot:
		xyplot.addSeries(series1, series1Format);

		// reduce the number of range labels
		xyplot.setTicksPerRangeLabel(3);

		//OURS: now find the min and max in the data set and make the boundaries accordingly
		int minX = 999999, maxX = 0, minY = 999999, maxY = 0; 
		for (int i=0; i<series1.size(); i++) {
			if (series1.getY(i).intValue() > maxY) {
				maxY = series1.getY(i).intValue();
			}
			if (series1.getX(i).intValue() > maxX) {
				maxX = series1.getX(i).intValue();
			}
			if (series1.getY(i).intValue() < minY) {
				minY = series1.getY(i).intValue();
			}
			if (series1.getX(i).intValue() < minX) {
				minX = series1.getY(i).intValue();
			}
		}
		minXY = new PointF(); maxXY = new PointF();
		minXY.x = minX-10; minXY.y = minY-10; maxXY.x = maxX-10; maxXY.y = maxY-10; 

		mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.AUTO);
		mySimpleXYPlot.redraw();
	}

	public XYSeries getXYSeriesYValsOnly(Number[] in, String name) {
		return new SimpleXYSeries(
				Arrays.asList(in),          // SimpleXYSeries takes a List so turn our array into a List
				SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
				name);    
	}

	public String meetCharCount(String s, int count) {
		StringBuilder str = new StringBuilder(); 
		str.append(s);
		for (int i=0; i<count-s.length();i++)
			str.append(" ");
		
		return str.toString(); 
	}
	
	public Number[] getSeriesFromDatabase(String message) {
		StringBuilder labelBuilder = new StringBuilder(); 

		Main.executeClient("INFLATE", "time_info", "unnecessary"); 
		Cursor cursor;
		Number[] result;

		if (message.equals("all_times")) {
			try {

				String[] columns = {"loc_from", "loc_to", "time", "time_entered", "username"}; 
				cursor = Main.getSQLiteManager().getReadableDatabase().query(false, "times", 
						columns, null, null, null, null, null, null);

				result = new Number[cursor.getCount()];
				doToast("New series has: " + cursor.getCount() + " data points.");
				int count = 0; 
				while (cursor.moveToNext()) {
					result[count] = cursor.getInt(2); 
					count++; 
				}

				labels.setText("No series labels to display.\n");
				return result;
			}
			catch (Exception e) {
				return null;
			}
		}
		else if (message.equals("time_distribution")) {
			cursor = Main.getSQLiteManager()
					.getReadableDatabase()
					.rawQuery("SELECT loc_from, loc_to, COUNT(*) FROM times WHERE username = 'magee' GROUP BY loc_from, loc_to;", null); 
			result = new Number[cursor.getCount()];

			String[] columns = { "loc_from", "loc_to", "entries" };
			for (int i=0; i<columns.length; i++) 
				labelBuilder.append(meetCharCount(columns[i],spacing) + "| ");
			labelBuilder.append("\n");
			labelBuilder.append(" ------ \n");
			doToast("New series has: " + cursor.getCount() + " data points.");
			int count = 0; 
			while (cursor.moveToNext()) {
				labelBuilder.append(meetCharCount(cursor.getString(0),spacing) + "| " + meetCharCount(cursor.getString(1),spacing) + "| " + meetCharCount(cursor.getInt(2) + "",spacing) + "|\n"); 
				result[count] = cursor.getInt(2); 
				count++; 
			}

			labels.setText(labelBuilder.toString());
			return result;
		}
		else if (message.equals("top_contributors")) {
			cursor = Main.getSQLiteManager()
					.getReadableDatabase()
					.rawQuery("SELECT username, COUNT(*) AS contributions FROM times GROUP BY username ORDER BY contributions DESC LIMIT 10;", null); 
			result = new Number[cursor.getCount()];

			String[] columns = { "username", "contributions" };
			for (int i=0; i<columns.length; i++) 
				labelBuilder.append(meetCharCount(columns[i],spacing)+ "| ");
			labelBuilder.append("\n");
			labelBuilder.append(" ------ \n");

			doToast("New series has: " + cursor.getCount() + " data points.");
			int count = 0; 
			while (cursor.moveToNext()) {
				labelBuilder.append(meetCharCount(cursor.getString(0),spacing) + "| " + meetCharCount(cursor.getInt(1) + "", spacing) + "|\n"); 
				result[count] = cursor.getInt(1); 
				count++; 
			}

			labels.setText(labelBuilder.toString());
			return result;
		}
		else if (message.equals("fastest_contributors")) {
			cursor = Main.getSQLiteManager()
					.getReadableDatabase()
					.rawQuery("SELECT username, AVG(time) as average_time FROM times GROUP BY username ORDER BY average_time ASC LIMIT 10;", null); 
			result = new Number[cursor.getCount()];

			String[] columns = { "username", "average_time" };
			for (int i=0; i<columns.length; i++) 
				labelBuilder.append(meetCharCount(columns[i],spacing) + "| ");
			labelBuilder.append("\n");
			labelBuilder.append(" ------ \n");

			doToast("New series has: " + cursor.getCount() + " data points.");
			int count = 0; 
			while (cursor.moveToNext()) {
				labelBuilder.append(meetCharCount(cursor.getString(0),spacing) + "| " + meetCharCount(cursor.getInt(1) + "", spacing) + "|\n"); 
				result[count] = cursor.getInt(1); 
				count++; 
			}

			labels.setText(labelBuilder.toString());
			return result;
		}
		else 
			return null;
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: // Start gesture
			firstFinger = new PointF(event.getX(), event.getY());
			mode = ONE_FINGER_DRAG;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			//When the gesture ends, a thread is created to give inertia to the scrolling and zoom
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					while(Math.abs(lastScrolling)>1f || Math.abs(lastZooming-1)<1.01){
						lastScrolling*=.8;
						scroll(lastScrolling);
						lastZooming+=(1-lastZooming)*.2;
						zoom(lastZooming);
						mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.AUTO);
						mySimpleXYPlot.redraw();
					}
				}
			}, 0);
		case MotionEvent.ACTION_POINTER_DOWN: // second finger
			//distBetweenFingers = spacing(event);
			distBetweenFingers = 6f; 
			// the distance check is done to avoid false alarms
			if (distBetweenFingers > 5f) {
				PointF oldFirstFinger=firstFinger;
				firstFinger=new PointF(event.getX(), event.getY());
				lastScrolling=oldFirstFinger.x-firstFinger.x;
				scroll(lastScrolling);
				lastZooming=(firstFinger.y-oldFirstFinger.y)/mySimpleXYPlot.getHeight();
				if (lastZooming<0)
					lastZooming=1/(1-lastZooming);
				else
					lastZooming+=1;
				zoom(lastZooming);
				mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.AUTO);
				mySimpleXYPlot.redraw();
				mode = TWO_FINGERS_DRAG;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == ONE_FINGER_DRAG) {
				PointF oldFirstFinger=firstFinger;
				firstFinger=new PointF(event.getX(), event.getY());
				lastScrolling=oldFirstFinger.x-firstFinger.x;
				scroll(lastScrolling);
				lastZooming=(firstFinger.y-oldFirstFinger.y)/mySimpleXYPlot.getHeight();
				if (lastZooming<0)
					lastZooming=1/(1-lastZooming);
				else
					lastZooming+=1;
				zoom(lastZooming);
				mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.AUTO);
				mySimpleXYPlot.redraw();
			} else if (mode == TWO_FINGERS_DRAG) {
				float oldDist =distBetweenFingers;
				distBetweenFingers=spacing(event);
				lastZooming=oldDist/distBetweenFingers;
				zoom(lastZooming);
				mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.AUTO);
				mySimpleXYPlot.redraw();
			}
			break;
		}
		return true;
	} 

	private void zoom(float scale) {
		float domainSpan = maxXY.x    - minXY.x;
		float domainMidPoint = maxXY.x        - domainSpan / 2.0f;
		float offset = domainSpan * scale / 2.0f;
		minXY.x=domainMidPoint- offset;
		maxXY.x=domainMidPoint+offset;
	}
	private void scroll(float pan) {
		float domainSpan = maxXY.x    - minXY.x;
		float step = domainSpan / mySimpleXYPlot.getWidth();
		float offset = pan * step;
		minXY.x+= offset;
		maxXY.x+= offset;
	}

	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	} 
}