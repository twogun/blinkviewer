package com.collomosse.blinkviewer;


public class ADPCMDecoder
{

 public ADPCMDecoder()
 {
 }

 public static byte[] decodeBlock(byte adpcm[], int offset)
 {
     byte data[] = new byte[1010];
     int outPos = 0;
     int inPos = offset;
     data[outPos++] = adpcm[inPos++];
     data[outPos++] = adpcm[inPos++];
     int lastOutput = data[0] & 0xff | data[1] << 8;
     int stepIndex = adpcm[inPos++];
     inPos++;
     boolean highNibble = false;
     for(int i = 1; i < 505; i++)
     {
         int delta;
         if(highNibble)
         {
             delta = ((adpcm[inPos] & 0xf0) << 24) >> 28;
             highNibble = false;
             inPos++;
         } else
         {
             delta = ((adpcm[inPos] & 0xf) << 28) >> 28;
             highNibble = true;
         }
         int step = ADPCM.STEPSIZE[stepIndex];
         int deltaMagnitude = delta & 7;
         int valueAdjust = 0;
         if((deltaMagnitude & 4) != 0)
             valueAdjust += step;
         step >>= 1;
         if((deltaMagnitude & 2) != 0)
             valueAdjust += step;
         step >>= 1;
         if((deltaMagnitude & 1) != 0)
             valueAdjust += step;
         step >>= 1;
         valueAdjust += step;
         if(deltaMagnitude != delta)
         {
             lastOutput -= valueAdjust;
             if(lastOutput < -32768)
                 lastOutput = -32768;
         } else
         {
             lastOutput += valueAdjust;
             if(lastOutput > 32767)
                 lastOutput = 32767;
         }
         stepIndex += ADPCM.STEPINCREMENT_MAGNITUDE[deltaMagnitude];
         if(stepIndex < 0)
             stepIndex = 0;
         else
         if(stepIndex >= ADPCM.STEPSIZE.length)
             stepIndex = ADPCM.STEPSIZE.length - 1;
         data[outPos++] = (byte)(lastOutput & 0xff);
         data[outPos++] = (byte)(lastOutput >> 8 & 0xff);
     }

     return data;
 }

 public static byte[] decode(byte adpcm[])
 {
     int iBlockNumber = adpcm.length / 256;
     byte outData[] = new byte[iBlockNumber * 505 * 2];
     for(int i = 0; i < iBlockNumber; i++)
     {
         byte data[] = decodeBlock(adpcm, i * 256);
         System.arraycopy(data, 0, outData, i * 505 * 2, data.length);
     }

     return outData;
 }

 public static final int BLOCKSAMPLES = 505;
 public static final int SAMPLERATE = 8000;
 public static final int BLOCKBYTES = 256;
}
