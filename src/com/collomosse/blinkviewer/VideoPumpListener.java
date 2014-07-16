package com.collomosse.blinkviewer;

import android.graphics.Bitmap;

public interface VideoPumpListener {

	public abstract void onFrameReceived(Bitmap img, int frameIndex, int roomTemperature);
	
}
