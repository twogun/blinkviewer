/*  
  This code adapted from 
  com/charliemouse/cambozola/shared/StreamSplit.java
  Copyright (C) Andy Wilcock, 2001.
  Available from http://www.charliemouse.com
 */

package com.collomosse.blinkviewer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Hashtable;

import android.util.Log;

public class StreamSplit {
    public static final String BOUNDARY_MARKER_PREFIX  = "--";
    public static final String BOUNDARY_MARKER_TERM    = BOUNDARY_MARKER_PREFIX;

	protected DataInputStream m_dis;
	private boolean m_streamEnd;


	public StreamSplit(DataInputStream dis)
	{
		m_dis = dis;
		m_streamEnd = false;
	}


	public Hashtable<String,String> readHeaders() throws IOException
	{
		Hashtable<String,String> ht = new Hashtable<String,String>();
		String response;
		boolean satisfied = false;

		do {
            //noinspection deprecation
            response = m_dis.readLine();
			if (response == null) {
				m_streamEnd = true;
				break;
			} else if (response.equals("")) {
                if (satisfied) {
				    break;
                } else {
                    // Carry on...
                }
			} else {
                satisfied = true;
            }
            addPropValue(response, ht);
        } while (true);
		return ht;
	}

    protected static void addPropValue(String response, Hashtable<String,String> ht)
    {
        int idx = response.indexOf(":");
        if (idx == -1) {
            return;
        }
        String tag = response.substring(0, idx);
        String val = response.substring(idx + 1).trim();
        ht.put(tag.toLowerCase(), val);
    }


    public static Hashtable readHeaders(URLConnection conn)
    {
        Hashtable ht = new Hashtable();
        int i = 0;
        do {
            String key = conn.getHeaderFieldKey(i);
            if (key == null) {
                if (i == 0) {
                    i++;
                    continue;
                } else {
                    break;
                }
            }
            String val = conn.getHeaderField(i);
            ht.put(key.toLowerCase(), val);
            i++;
        } while (true);
        return ht;
    }


	public void skipToBoundary(String boundary) throws IOException
	{
		readToBoundary(boundary);
	}


	public byte[] readToBoundary(String boundary) throws IOException
	{
		ResizableByteArrayOutputStream baos = new ResizableByteArrayOutputStream();
		StringBuffer lastLine = new StringBuffer();
		int lineidx = 0;
		int chidx = 0;
		byte ch;
		do {
			try {
				ch = m_dis.readByte();
			} catch (EOFException e) {
				m_streamEnd = true;
				break;
			}
			if (ch == '\n' || ch == '\r') {
				//
				// End of line... Note, this will now look for the boundary
                // within the line - more flexible as it can habdle
                // arfle--boundary\n  as well as
                // arfle\n--boundary\n
				//
				String lls = lastLine.toString();
                int idx = lls.indexOf(BOUNDARY_MARKER_PREFIX);
                if (idx != -1) {
                    lls = lastLine.substring(idx);
                    if (lls.startsWith(boundary)) {
                        //
                        // Boundary found - check for termination
                        //
                        String btest = lls.substring(boundary.length());
                        if (btest.equals(BOUNDARY_MARKER_TERM)) {
                            m_streamEnd = true;
                        }
                        chidx = lineidx+idx;
                        break;
                    }
				}
				lastLine = new StringBuffer();
				lineidx = chidx + 1;
			} else {
				lastLine.append((char) ch);
			}
			chidx++;
			baos.write(ch);
		} while (true);
		//
		baos.close();
		baos.resize(chidx);
		//Log.d("BlinkViewer::Dump",baos.toString());
		return baos.toByteArray();
	}

	// JPC adapt for minlen (speed optimization)
	public byte[] readToBoundary(String boundary, int minlen) throws IOException
	{
		ResizableByteArrayOutputStream baos = new ResizableByteArrayOutputStream();
		byte [] bulkbuf=new byte[minlen];
		m_dis.read(bulkbuf);
		baos.write(bulkbuf);
		
		StringBuffer lastLine = new StringBuffer();
		int lineidx = 0;
		int chidx = 0;
		byte ch;
		do {
			try {
				ch = m_dis.readByte();
			} catch (EOFException e) {
				m_streamEnd = true;
				break;
			}
			if (ch == '\n' || ch == '\r') {
				//
				// End of line... Note, this will now look for the boundary
                // within the line - more flexible as it can habdle
                // arfle--boundary\n  as well as
                // arfle\n--boundary\n
				//
				String lls = lastLine.toString();
                int idx = lls.indexOf(BOUNDARY_MARKER_PREFIX);
                if (idx != -1) {
                    lls = lastLine.substring(idx);
                    if (lls.startsWith(boundary)) {
                        //
                        // Boundary found - check for termination
                        //
                        String btest = lls.substring(boundary.length());
                        if (btest.equals(BOUNDARY_MARKER_TERM)) {
                            m_streamEnd = true;
                        }
                        chidx = lineidx+idx;
                        break;
                    }
				}
				lastLine = new StringBuffer();
				lineidx = chidx + 1;
			} else {
				lastLine.append((char) ch);
			}
			chidx++;
			baos.write(ch);
		} while (true);
		//
		baos.close();
		baos.resize(chidx+minlen);
		Log.d("BlinkViewer::Dump","spare "+chidx);
		return baos.toByteArray();
	}

	public boolean isAtStreamEnd()
	{
		return m_streamEnd;
	}
}


class ResizableByteArrayOutputStream extends ByteArrayOutputStream {
	public ResizableByteArrayOutputStream()
	{
		super();
	}


	public void resize(int size)
	{
		count = size;
	}
}
