// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by juksilve on 6.3.2015.
 */
public class BluetoothBase {

    public interface BluetoothStatusChanged{
        void Connected(BluetoothSocket socket);
        void GotConnection(BluetoothSocket socket);
        void ConnectionFailed(String reason);
        void ListeningFailed(String reason);
        void BluetoothStateChanged(int state);
        void HandShakeOk(BluetoothSocket socket, boolean incoming);
        void HandShakeFailed(String reason, boolean incoming);
    }

    private BluetoothStatusChanged callBack = null;
    private BluetoothAdapter bluetooth = null;

    private BtBroadCastReceiver receiver = null;
    private final Context context;

    public BluetoothBase(Context inContext, BluetoothStatusChanged callbackHandler) {
        this.context = inContext;
        this.callBack = callbackHandler;

        //bluetooth = new BluetoothAdapter(this);
        bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean start() {

        boolean ret = false;
        if(bluetooth != null) {
            ret = true;
            Log.d("BluetoothBase", "My BT: " + bluetooth.getAddress() + " name: " + bluetooth.getName() + " , state: " + bluetooth.getState());

            receiver = new BtBroadCastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            this.context.registerReceiver(receiver, filter);
        }
        return ret;
    }

    public void stop() {
        this.context.unregisterReceiver(receiver);
    }

    public void setBluetoothEnabled(boolean seton) {
        if (bluetooth != null) {
            if (seton) {
                bluetooth.enable();
            } else {
                bluetooth.disable();
            }
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetooth != null && bluetooth.isEnabled();
    }

    public BluetoothAdapter getAdapter(){
        return bluetooth;
    }

    public String getAddress() {
        String ret = "";
        if (bluetooth != null){
            ret = bluetooth.getAddress();
            if (ret.equals("02:00:00:00:00:00"))
            {
                // Workaround, expect this to fail on Android N, but works on Android M
                // reference: http://stackoverflow.com/questions/33377982/get-bluetooth-local-mac-address-in-marshmallow
                ret =  android.provider.Settings.Secure.getString(context.getContentResolver(), "bluetooth_address");
            }
        }
        return ret;
    }

    public String getName() {
        String ret = "";
        if (bluetooth != null){
            ret = bluetooth.getName();
        }
        return ret;
    }

    public BluetoothDevice getRemoteDevice(String address) {
        BluetoothDevice device = null;
        if (bluetooth != null){
            device = bluetooth.getRemoteDevice(address);
        }
        return device;
    }

    private class BtBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                if (callBack != null) {
                    callBack.BluetoothStateChanged(mode);
                }
            }
        }
    }
}
