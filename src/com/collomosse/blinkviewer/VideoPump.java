package com.collomosse.blinkviewer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

public class VideoPump extends Thread {

	MainActivity _cxt;
	URL _cameraURI;
	DataInputStream _cameraDataStream;
	Hashtable<String,String> _headers;
	PlayAudio _audioengine;

    byte _img[];
    byte _pcm[];
    
    VideoPumpListener _callback;
    

	public VideoPump(MainActivity cxt, URL url, VideoPumpListener callback) {
		_callback=callback;
		_cxt=cxt;
		_cameraURI=url;
		_audioengine=new PlayAudio();
		this.start();
	}
	
	public void run() {
		
		try {
			runStream();
		}
		catch (final IOException e) {
            Log.d("BlinkViewer::Connect" , "Unable to connect: "+e);
    		_cxt.runOnUiThread(new Runnable() {
                @Override
                public void run() {      
                	
                    Toast.makeText(_cxt, "HTTP Connection failed\n\n"+e.toString(), Toast.LENGTH_LONG).show();                	
                	
                }
            });

		}
		
	}
	
	public void runStream() throws IOException {
		
			// Connect
	        URLConnection conn = _cameraURI.openConnection();
	        conn.setRequestProperty("User-Agent", "BlinkViewerApp");
	        conn.setRequestProperty("Host", _cameraURI.getHost());
	        _cameraDataStream = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
	        
	        // Parse HTML headers
	        _headers = StreamSplit.readHeaders(conn);
            for (Enumeration enm = _headers.keys();enm.hasMoreElements();)
            {
                String hkey = (String)enm.nextElement();
                Log.d("BlinkViewer::StreamHeaderDump" ,hkey + " = " + _headers.get(hkey));
            }

	        StreamSplit ssplit = new StreamSplit(_cameraDataStream);
	 
			String ctype = (String) _headers.get("content-type");
			boolean success=true;
			if (ctype == null) {
				Log.e("BlinkViewer::Connect","No main content type");
				success=false;
			} else if (ctype.indexOf("text") != -1) {
				String response;
                //noinspection deprecation
                while ((response = _cameraDataStream.readLine()) != null) {
					Log.e("BlinkViewer::Connect","Unexpected HTML response: "+response);
				}
                success=false;
			}
			
			if (success) {
				Log.i("BlinkViewer::Connect","Success content type = "+ctype);

				// Get boundary marker text
				int bidx = ctype.indexOf("boundary=");
				String boundary = StreamSplit.BOUNDARY_MARKER_PREFIX;
				if (bidx != -1) {
					boundary = ctype.substring(bidx + 9);
					ctype = ctype.substring(0, bidx);
	                if (boundary.startsWith("\"") && boundary.endsWith("\""))
	                {
	                    boundary = boundary.substring(1, boundary.length()-1);
	                }
	                if (!boundary.startsWith(StreamSplit.BOUNDARY_MARKER_PREFIX)) {
						boundary = StreamSplit.BOUNDARY_MARKER_PREFIX + boundary;
					}
				}
				Log.i("BlinkViewer::Connect","Boundary = ["+boundary+"]");
					
				// Read up to first boundary marker
				if (ctype.startsWith("multipart/x-mixed-replace")) {
					ssplit.skipToBoundary(boundary);
				}
				
			while (true) {				
				while (!ssplit.isAtStreamEnd()) {
					// read headers
			        _headers = ssplit.readHeaders();
		            for (Enumeration enm = _headers.keys();enm.hasMoreElements();)
		            {
		                String hkey = (String)enm.nextElement();
		                Log.d("BlinkViewer::StreamHeaderDump-Inner" ,hkey + " = " + _headers.get(hkey));
		            }
		            
		            if (ssplit.isAtStreamEnd()) {
						break;
					}
					ctype = (String)_headers.get("content-type");
					if (ctype == null) {
						Log.e("BlinkView::Connect","No part content type");
					}
					Log.i("BlinkViewer::Connect","Inner-content type = "+ctype);
//					ssplit.skipToBoundary(boundary);

					if (ctype.startsWith("multipart/x-mixed-replace")) {
						// Skip
//						bidx = ctype.indexOf("boundary=");
//						boundary = ctype.substring(bidx + 9);
						//ssplit.skipToBoundary(boundary);
						continue;
					}
					else {
						int clen = Integer.parseInt((String)_headers.get("content-length"));
						byte[] data = ssplit.readToBoundary(boundary,clen); // JPC optimize
						Log.i("BlinkViewer::Connect","Read image of "+data.length+" bytes (expected "+clen);
						
						int audioType;
				        int headerType;
				        int iLength;
				        int audio_start_offset;
				        int m_ResetFlag = data[0] & 1;
				        audioType = data[0] & 6;
				        headerType = data[0] & 8;
				        iLength = byteArrayToInt_MSB(data, 1);
				        int m_imgCurrentIndex = byteArrayToInt_MSB(data, 5);
				        int m_resolutionJpeg = data[9];
				        int m_ResetAudioBufferCount = data[10];
				        int temperature = byteArrayToInt_MSB(data, 11);
				        audio_start_offset = 0;
				        
				        if(headerType == 0) {
				            audio_start_offset = 15;
				        }
				        else if(headerType == 8) {
				            audio_start_offset = 56;
				        }
				        else {
				        	continue; //abort
				        }
				        
				        byte adpcm[];
				        adpcm = new byte[iLength];
				        System.arraycopy(data, audio_start_offset, adpcm, 0, iLength);

				        // decompress audio
				        if(audioType == 2) 
				                _pcm = adpcm;
				        else if (audioType == 0) 
				                _pcm = ADPCMDecoder.decode(adpcm);
				        else
				                _pcm = null;
				        
				        // extract jpeg data
				        int ImageDataLen = data.length - (audio_start_offset + iLength);
				        _img = new byte[ImageDataLen];
				        System.arraycopy(data, audio_start_offset + iLength, _img, 0, _img.length);
				        
				        notifyNewFrame(m_imgCurrentIndex,temperature);

					}
						
				} // end proc blocks
				try {
					Thread.sleep(20);
				} catch (InterruptedException ignored) {}

			}				// end inf while
					
			} // end if success
	        
	}
	
    public static byte[] intToByteArray_MSB(int value)
    {
        byte b[] = new byte[4];
        for(int i = 0; i < 4; i++)
        {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte)(value >>> offset & 0xff);
        }

        return b;
    }

    public static int byteArrayToInt_MSB(byte b[], int offset)
    {
        int value = 0;
        for(int i = 0; i < 4; i++)
        {
            int shift = (3 - i) * 8;
            value += (b[i + offset] & 0xff) << shift;
        }

        return value;
    }
	
    public void notifyNewFrame(int frameIndex, int roomTemp) {
    	
    	Log.i("BlinkViewer::Connect","Obtained camera frame "+frameIndex);
    	Log.i("BlinkViewer::Connect","JPEG: "+_img.length +" PCM: "+_pcm.length);
    	Bitmap b=BitmapFactory.decodeByteArray (_img, 0, _img.length);
    	
    	_audioengine.writeAudio(_pcm);
    	
    	_callback.onFrameReceived(b, frameIndex, roomTemp);
    }
}
