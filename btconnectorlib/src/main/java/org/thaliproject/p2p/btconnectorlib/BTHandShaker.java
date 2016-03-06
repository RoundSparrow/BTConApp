// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothSocket;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by juksilve on 11.3.2015.
 */

public class BTHandShaker {

    private final BluetoothSocket mmSocket;
    private final BluetoothBase.BluetoothStatusChanged callback;
    private final boolean isIncoming;

    final String handShakeBuf = "handshake";
    final String shakeBackBuf = "shakehand";

    BTHandShakeSocketThread mBTHandShakeSocketTread = null;

    final CountDownTimer HandShakeTimeOutTimer = new CountDownTimer(4000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            // not using
        }

        @Override
        public void onFinish() {
            callback.HandShakeFailed("TimeOut",isIncoming);
        }
    };

    public BTHandShaker(BluetoothSocket socket, BluetoothBase.BluetoothStatusChanged statusChangeCallback, boolean incoming) {
        print_line("Creating BTHandShaker");
        callback = statusChangeCallback;
        mmSocket = socket;
        isIncoming = incoming;
    }

    public void startHandshake() {
        print_line("startAdvertising");
        HandShakeTimeOutTimer.start();

        mBTHandShakeSocketTread = new BTHandShakeSocketThread(mmSocket,mHandler);
        mBTHandShakeSocketTread.start();

        if (!isIncoming) {
            mBTHandShakeSocketTread.write(handShakeBuf.getBytes());
        }
    }

    public void tryCloseSocket() {
        if (mBTHandShakeSocketTread != null){
            mBTHandShakeSocketTread.closeSocket();
        }
    }

    public void stopBluetooth() {
        print_line("stopListening");
        HandShakeTimeOutTimer.cancel();
        if (mBTHandShakeSocketTread != null) {
            mBTHandShakeSocketTread = null;
        }
    }

    private void print_line(String message){
        Log.d("BTHandShaker",  "BTHandShaker: " + message);
        LogKeeper.addLogEntry("BT_HS", message, LogKeeper.L_A_NORMAL, LogKeeper.L_B_NORMAL);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mBTHandShakeSocketTread != null) {
                switch (msg.what) {
                    case BTHandShakeSocketThread.MESSAGE_WRITE: {
                        LogKeeper.addLogEntry("BT_HS", "MESSAGE_WRITE " + msg.arg1 + " bytes.", LogKeeper.L_A_MEDIUM, LogKeeper.L_B_NORMAL);
                        if (isIncoming) {
                            callback.HandShakeOk(mmSocket, isIncoming);
                        }
                    }
                    break;
                    case BTHandShakeSocketThread.MESSAGE_READ: {
                        print_line("got MESSAGE_READ " + msg.arg1 + " bytes.");
                        if (isIncoming) {
                            mBTHandShakeSocketTread.write(shakeBackBuf.getBytes());
                        } else {
                            callback.HandShakeOk(mmSocket, isIncoming);
                        }
                    }
                    break;
                    case BTHandShakeSocketThread.SOCKET_DISCONNECTED: {

                        callback.HandShakeFailed("SOCKET_DISCONNECTED", isIncoming);
                    }
                    break;
                }
            } else {
                print_line("handleMessage called for NULL thread handler");
            }
        }
    };
}
