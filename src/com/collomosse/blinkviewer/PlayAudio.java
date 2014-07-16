package com.collomosse.blinkviewer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class PlayAudio extends Thread
{

	AudioTrack _audioout;
	int byteswritten=0;
	int bytesplayed=0;
	byte[] _audiobuf;
	
    public PlayAudio()
    {

		// based on decompiled code
    	//8000 Hz
    	// 16 bit
    	// 1 channel
    	// signed
    	// littleEndian
    	

    	_audioout=new AudioTrack(
    			AudioManager.STREAM_MUSIC, 
    			8000, 
    			AudioFormat.CHANNEL_OUT_MONO, 
    			AudioFormat.ENCODING_PCM_16BIT, 
    			16160, 
    			AudioTrack.MODE_STREAM);

    	
    	byteswritten=0;
    	bytesplayed=0;
    	_audiobuf=new byte[1000000];
    }

    public synchronized void writeAudio(byte[] pcm) {
    
    	System.arraycopy(pcm, 0, _audiobuf, byteswritten, pcm.length);    	
    	byteswritten+=pcm.length;
    	
    	boolean stuffgotplayed=false;
    	while ((byteswritten-bytesplayed)>16160) {
    		int res=_audioout.write(_audiobuf, bytesplayed, 16160);
        	bytesplayed+=16160;
        	stuffgotplayed=true;
    		switch (res) {
    		case AudioTrack.ERROR_INVALID_OPERATION:
    			Log.e("IAX2Audio", "Invalid write()");
    			return;
    		case AudioTrack.ERROR_BAD_VALUE:
    			Log.e("IAX2Audio", "Bad arguments to write()");
    			return;
    		}
    	}
   	
    	if (stuffgotplayed) {
    		
        	_audioout.play();
        	System.arraycopy(_audiobuf, bytesplayed, _audiobuf, 0, bytesplayed);
        	bytesplayed=0;
        	byteswritten-=bytesplayed;

    	}
    	
    	

    }
    
    
}


/*** DECOMPILED

//Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
//Jad home page: http://www.kpdus.com/jad.html
//Decompiler options: packimports(3) 
//Source File Name:   PlayAudio.java

package com.charliemouse.cambozola.shared;

import javax.sound.sampled.*;

//Referenced classes of package com.charliemouse.cambozola.shared:
//         Util

public class PlayAudio extends Thread
{

 public PlayAudio()
 {
     m_bRunAudio = true;
     m_bPlayAudio = true;
     m_aud_idx = 0;
     m_AudBuf = new byte[16][16160];
     m_AudBufLen = new int[16];
     m_AudBufStatus = new int[16];
     m_audRec_idx = 0;
     m_audfm = new AudioFormat(8000F, 16, 1, true, false);
     try
     {
         m_line = AudioSystem.getSourceDataLine(m_audfm);
         m_line.open(m_audfm, 16160);
     }
     catch(LineUnavailableException e) { }
 }

 public void init()
 {
 }

 public void setPlayAudio(boolean bPlayAudio)
 {
     m_bPlayAudio = false;
     for(int i = 0; i < 16; i++)
         if(m_AudBufStatus[i] != 0)
             Util.memSet(m_AudBuf[i], 1010, (byte)0);

 }

 public void getAudio(byte audioData[], int audioLen)
 {
     int i;
     i = 0;
     if(!m_bPlayAudio)
         return;
     if(audioLen > 4040)
         return;
_L2:
     do
     {
label0:
         {
             if(i >= audioLen)
                 break MISSING_BLOCK_LABEL_143;
             int len;
             if(i + 16160 <= audioLen)
                 len = 16160;
             else
                 len = audioLen - i;
             if(m_AudBufStatus[m_audRec_idx] != 0)
                 break label0;
             m_AudBufStatus[m_audRec_idx] = 1;
             System.arraycopy(audioData, i, m_AudBuf[m_audRec_idx], 0, len);
             m_AudBufLen[m_audRec_idx] = len;
             m_AudBufStatus[m_audRec_idx] = 2;
             i += len;
             m_audRec_idx = (m_audRec_idx + 1) % 16;
         }
     } while(true);
     Thread.sleep(100L);
     if(true) goto _L2; else goto _L1
_L1:
     InterruptedException ie;
     ie;
 }

 public void run()
 {
     m_line.start();
_L2:
     InterruptedException ie;
     do
     {
label0:
         {
             if(!m_bRunAudio)
                 break MISSING_BLOCK_LABEL_176;
             if(!m_bPlayAudio)
                 break label0;
             if(m_AudBufStatus[m_aud_idx] != 2)
             {
                 try
                 {
                     Thread.sleep(20L);
                 }
                 // Misplaced declaration of an exception variable
                 catch(InterruptedException ie)
                 {
                     break MISSING_BLOCK_LABEL_176;
                 }
             } else
             {
                 m_AudBufStatus[m_aud_idx] = 1;
                 if(m_AudBufLen[m_aud_idx] > 0)
                 {
                     m_line.write(m_AudBuf[m_aud_idx], 0, m_AudBufLen[m_aud_idx]);
                     m_AudBufLen[m_aud_idx] = 0;
                     m_AudBufStatus[m_aud_idx] = 0;
                     m_aud_idx = (m_aud_idx + 1) % 16;
                 } else
                 {
                     m_AudBufStatus[m_aud_idx] = 0;
                     m_aud_idx = (m_aud_idx + 1) % 16;
                 }
             }
         }
     } while(true);
     Thread.sleep(1000L);
     if(true) goto _L2; else goto _L1
_L1:
     ie;
     m_line.drain();
     m_line.flush();
     m_line.close();
     return;
 }

 private AudioFormat m_audfm;
 private SourceDataLine m_line;
 private boolean m_bRunAudio;
 private boolean m_bPlayAudio;
 private int m_aud_idx;
 private int m_audRec_idx;
 private byte m_AudBuf[][];
 private int m_AudBufLen[];
 private int m_AudBufStatus[];
}

*/