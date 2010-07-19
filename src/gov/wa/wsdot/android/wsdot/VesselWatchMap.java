/*
 * Copyright (c) 2010 Washington State Department of Transportation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package gov.wa.wsdot.android.wsdot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public abstract class VesselWatchMap extends MapActivity {

	private static final int IO_BUFFER_SIZE = 4 * 1024;
	private static final String DEBUG_TAG = "VesselWatchMap";
	private ArrayList<VesselWatchItem> vesselWatchItems = null;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	VesselsItemizedOverlay vesselsItemizedOverlay;
	MapView map = null;
	private Handler handler = new Handler();
	private Timer timer = new Timer();
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup the unique latitude, longitude and zoom level
        prepareMap();
        // Schedule the map to update every 30 seconds
        timer.schedule(new MyTimerTask(), 0, 30000);
    }
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		timer.cancel();
	}

	abstract void prepareMap();

    public class MyTimerTask extends TimerTask {
        private Runnable runnable = new Runnable() {
            public void run() {
                new GetFerryLocations().execute();
            }
        };

        public void run() {
            handler.post(runnable);
        }
    }
	
	private class GetFerryLocations extends AsyncTask<String, Void, String> {
		private final ProgressDialog dialog = new ProgressDialog(VesselWatchMap.this);

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage("Retrieving ferry locations ...");
			this.dialog.show();
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				URL url = new URL("http://www.wsdot.wa.gov/ferries/vesselwatch/Vessels.ashx");
				URLConnection urlConn = url.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				String jsonFile = "";
				String line;
				while ((line = in.readLine()) != null)
					jsonFile += line;
				in.close();
				
		        mapOverlays = map.getOverlays();
		        mapOverlays.clear();
				JSONObject obj = new JSONObject(jsonFile);
				JSONArray items = obj.getJSONArray("vessellist");
				vesselWatchItems = new ArrayList<VesselWatchItem>();
				VesselWatchItem i = null;
				
				for (int j=0; j < items.length(); j++) {
					JSONObject item = items.getJSONObject(j);
					i = new VesselWatchItem();
					if (item.getString("inservice").equalsIgnoreCase("false")) {
						continue;
					}
					
					String nextDeparture = item.getString("nextdep");
					if (nextDeparture.equals("")) nextDeparture = "available when at dock";
					
					i.setName(item.getString("name"));
					i.setRoute(item.getString("route"));
					i.setLastDock(item.getString("lastdock"));
					i.setNextDep(nextDeparture);
					i.setHead(item.getInt("head"));
					i.setHeadTxt(item.getString("headtxt"));
					i.setSpeed(item.getDouble("speed"));
					i.setLat(item.getDouble("lat"));
					i.setLon(item.getDouble("lon"));
					
					vesselWatchItems.add(i);
					drawable = loadImageFromNetwork("http://www.wsdot.wa.gov/ferries/vesselwatch/" + item.getString("icon"));
			        vesselsItemizedOverlay = new VesselsItemizedOverlay(drawable, VesselWatchMap.this);				
					GeoPoint point = new GeoPoint((int)(i.getLat() * 1E6), (int)(i.getLon() * 1E6));
					OverlayItem overlayitem = new OverlayItem(point, i.getName(), "Route: " + i.getRoute() + "\nLast Dock: " + i.getLastDock() + "\nNext Departure: " + i.getNextDep() + "\nHeading: " + i.getHead().toString() + "\u00b0 " + i.getHeadTxt() + "\nSpeed: " + i.getSpeed().toString() + " knots");
					vesselsItemizedOverlay.addOverlay(overlayitem);
					mapOverlays.add(vesselsItemizedOverlay);
				}
				
			} catch (Exception e) {
				Log.e(DEBUG_TAG, "Error in network call", e);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			map.postInvalidate();
		}
	}
	
    private Drawable loadImageFromNetwork(String url) {
    	BufferedInputStream in;
        BufferedOutputStream out;  
        
        try {
            in = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
            copy(in, out);
            out.flush();
            final byte[] data = dataStream.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);                        
            final Drawable image = new BitmapDrawable(bitmap);
            return image;
	    } catch (Exception e) {
	        Log.e(DEBUG_TAG, "Error retrieving camera images", e);
	    }
	    return null;	    
    }
    
    /**
     * Copy the content of the input stream into the output stream, using a
     * temporary byte array buffer whose size is defined by
     * {@link #IO_BUFFER_SIZE}.
     * 
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     * @throws IOException If any error occurs during the copy.
     */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    } 	
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}