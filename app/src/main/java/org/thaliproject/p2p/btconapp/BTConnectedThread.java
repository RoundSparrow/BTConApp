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
    private final Handler mHandler;

    final String TAG  = "BTConnectedThread";


    public BTConnectedThread(BluetoothSocket socket, Handler handler) {
        super("BTConnectedThread");
        Log.d(TAG, "Creating BTConnectedThread " + Thread.currentThread());
        mHandler = handler;
        mmSocket = socket;

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

    public void run() {
        Log.i(TAG, "BTConnectedThread started " + Thread.currentThread());
        byte[] buffer = new byte[ApplicationSettings.BUFFER_SIZE_XFER0];
        int bytes;

        while (true) {
            try {
                if(mmInStream != null) {
                    Log.d(TAG, "Starting write on Thread " + Thread.currentThread());
                    bytes = mmInStream.read(buffer);
                    //Log.d(TAG, "ConnectedThread read data: " + bytes + " bytes");
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread disconnected: ", e);
                mHandler.obtainMessage(SOCKET_DISCONNECTED, -1,-1 ,e ).sendToTarget();
                break;
            }
        }
    }

    /**
     * Write to the connected OutStream.
     * @param buffer The bytes to write
     */
    public void write(byte[] buffer) {
        try {
            if(mmOutStream != null) {
                Log.d(TAG, "Starting write on Thread " + Thread.currentThread());
                mmOutStream.write(buffer);
                mHandler.obtainMessage(MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
            }
        } catch (IOException e) {
            Log.e(TAG, "ConnectedThread  write failed: ", e);
        }
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
