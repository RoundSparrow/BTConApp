// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 11.3.2015.
 */

public class BTConnectedThread extends Thread {

    public static final int MESSAGE_READ         = 1;
    public static final int MESSAGE_WRITE        = 2;
    public static final int SOCKET_DISCONNECTED  = 3;

    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private final Handler callerHandler;
    private byte[] buffer;
    private int bufferSizeDesired = 1024;

    final String TAG  = "BTConnectedThread";


    public BTConnectedThread(BluetoothSocket socket, Handler handler, int bufferSizeToSet) {
        super("BTConnectedThread");
        Log.d(TAG, "Creating BTConnectedThread " + Thread.currentThread());
        callerHandler = handler;
        mmSocket = socket;
        bufferSizeDesired = bufferSizeToSet;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        // Get the BluetoothSocket input and output streams
        try {
            if(mmSocket != null) {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            }
        } catch (IOException e) {
            Log.e(TAG, "Creating temp sockets failed: ", e);
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        Log.d(TAG, "Created BTConnectedThread " + Thread.currentThread());
    }


    private boolean keepReading = true;

    @Override
    public void run() {
        Log.i(TAG, "BTConnectedThread run() started " + Thread.currentThread());
        byte[] readBuffer = new byte[bufferSizeDesired];
        int bytesRead;

        while (keepReading) {
            try {
                if(mmInStream != null) {
                    Log.d(TAG, "Starting read on Thread " + Thread.currentThread());
                    bytesRead = mmInStream.read(readBuffer);
                    //Log.d(TAG, "ConnectedThread read data: " + bytes + " bytes");
                    callerHandler.obtainMessage(MESSAGE_READ, bytesRead, -1, readBuffer).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread disconnected: ", e);
                callerHandler.obtainMessage(SOCKET_DISCONNECTED, -1, -1, e).sendToTarget();
                break;
            }
        }
    }


    public void setBufferContent(byte[] bufferToSet)
    {
        buffer = bufferToSet;
    }

    /**
     * Write to the connected OutStream.
     * the buffer should be set by caller before this method is called
     */
    public void write() {
        Thread writeThread = new Thread() {
            @Override
            public void run() {
                try {
                    if(mmOutStream != null) {
                        Log.d(TAG, "Starting write on Thread " + Thread.currentThread());
                        mmOutStream.write(buffer);
                        callerHandler.obtainMessage(MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "ConnectedThread  write failed: ", e);
                }
            }
        };

        writeThread.setName("BTConnectedThreadWriteThread");
        writeThread.start();
    }

    public void Stop() {
        if (mmInStream != null) {
            try {mmInStream.close();} catch (Exception e) {}
            mmInStream = null;
        }

        if (mmOutStream != null) {
            try {mmOutStream.close();} catch (Exception e) {}
            mmOutStream = null;
        }

        if (mmSocket != null) {
            try {mmSocket.close();} catch (Exception e) {}
            mmSocket = null;
        }
    }
}
