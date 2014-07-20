package com.collomosse.blinkviewer;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

// Main Activity
public class MainActivity extends Activity implements VideoPumpListener {

	public ImageView _camImage=null;
	public TextView _statusBar=null;
	
	VideoPump _vpump=null;
	
	public void onFrameReceived(final Bitmap img, final int frameIndex, final int roomTemperature) {
        
		runOnUiThread(new Runnable() {
            @Override
            public void run() {      
            	
            	if (_camImage!=null) {
            		_camImage.setImageBitmap(img);
            	}
    
            	if (_statusBar!=null) {
            		_statusBar.setText("Frame #"+frameIndex+"  Temperature "+roomTemperature);
            		
            	}
            	
            	
            }
        });

	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String ipAddress = sharedPref.getString("blinkIPaddress", "10.0.0.250");

		URL url=null;
		try {
			url=new URL("http://" + ipAddress + "/?action=appletvastream");
		}
		catch (MalformedURLException e){}
		
		if (_vpump==null) {
			_vpump=new VideoPump(this,url,this);
		}
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
		    startActivityForResult(new Intent(this, SettingsActivity.class), 1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		MainActivity _ma=null;
		
		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			_ma=((MainActivity)getActivity());

			
			_ma._camImage=(ImageView)rootView.findViewById(R.id.camStream);
			_ma._statusBar=(TextView)rootView.findViewById(R.id.statusText);

			return rootView;
		}
	}

}
