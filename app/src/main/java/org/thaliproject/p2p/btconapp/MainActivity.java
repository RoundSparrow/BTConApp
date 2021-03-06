// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.thaliproject.p2p.btconnectorlib.BTConnector;
import org.thaliproject.p2p.btconnectorlib.BTConnectorSettings;
import org.thaliproject.p2p.btconnectorlib.ServiceItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements BTConnector.Callback, BTConnector.ConnectSelector {

    final MainActivity that = this;

    /*
        For End-to-End testing we can use timer here to Stop the process
        for example after 1 minute.
        The value is determined by mExitWithDelay
        */

    private ApplicationSettings appSettings = new ApplicationSettings();

    private TextView outputInfoText0;

    BTConnectorSettings conSettings;
    private final List<ServiceItem> connectedArray = new ArrayList<>();

    MyTextSpeech mySpeech = null;

    int sendMessageCounter = 0;
    int gotMessageCounter = 0;
    int ConAttemptCounter = 0;
    int ConnectionCounter = 0;
    int ConCancelCounter = 0;


    boolean amIBigSender = false;
    boolean gotFirstMessage = false;
    boolean wroteFirstMessage = false;
    long wroteDataAmount = 0;
    long gotDataAmount = 0;

    long startTime = 0;

    TestDataFile mTestDataFile = null;

    private final int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter = 0;
    final Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {

            // call function to update timer
            timeCounter = timeCounter + 1;
            String timeShow = "T: " + timeCounter;

       /*     if(mExitWithDelayIsOn) {
                //exit timer for testing
                if (mExitWithDelay > 0) {
                    mExitWithDelay = mExitWithDelay - 1;
                    timeShow = timeShow + ", S: " + mExitWithDelay;
                } else {
                    if(mBTConnector != null) {
                        mBTConnector.Stop();
                        mBTConnector = null;
                    }
                    mExitWithDelayIsOn = false;
                    ShowSummary();
                }
            }*/

            long runTimeSec = ((System.currentTimeMillis() - startTime) / 1000);

            timeShow = timeShow + ", run: ";
            long hours = (runTimeSec / 3600);
            timeShow = timeShow + hours + " h, ";
            runTimeSec = (runTimeSec - (hours * 3600));

            long minutes = (runTimeSec / 60);
            timeShow = timeShow + minutes + " m, ";
            runTimeSec = (runTimeSec - (minutes * 60));

            timeShow = timeShow + runTimeSec + " s.";

            ((TextView) findViewById(R.id.TimeBox)).setText(timeShow);
            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    BTConnector mBTConnector = null;

    BTConnectedThread mBTConnectedThread = null;
    PowerManager.WakeLock mWakeLock = null;

    long receivingTimeOutBaseTime = 0;
    final CountDownTimer BigBufferReceivingTimeOut = new CountDownTimer(2000, 500) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {

            //if the receiving process has taken more than a minute, lets cancel it
            long receivingNow = (System.currentTimeMillis() - receivingTimeOutBaseTime);
            if(receivingNow > appSettings.receiveTimeMaximum) {
                if (mBTConnectedThread != null) {
                    mBTConnectedThread.Stop();
                    mBTConnectedThread = null;
                }
                print_line("CHAT", "We got timeout on receiving data, lets Disconnect.");

                ConCancelCounter = ConCancelCounter + 1;
                ((TextView) findViewById(R.id.cancelCount)).setText("" + ConCancelCounter);
            }else{
                BigBufferReceivingTimeOut.start();
            }
        }
    };

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTime = System.currentTimeMillis();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        conSettings = new BTConnectorSettings();
        conSettings.SERVICE_TYPE = appSettings.serviceTypeIdentifier;
        conSettings.MY_UUID = UUID.fromString(appSettings.BtUUID);
        conSettings.MY_NAME = appSettings.Bt_NAME;

        mySpeech = new MyTextSpeech(this);

        outputInfoText0 = (TextView) findViewById(R.id.outputInfoText0);
        statusBox = ((TextView) findViewById(R.id.statusBox));

        mTestDataFile = new TestDataFile(this);
        mTestDataFile.StartNewFile();

        Button btButton = (Button) findViewById(R.id.appToggle);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appSettings.mExitWithDelayIsOn = false;
                print_line("Debug","Exit with delay is set OFF");
                if(mBTConnector != null){
                    mBTConnector.Stop();
                    mBTConnector = null;
                    ShowSummary();
                }else{
                    mBTConnector = new BTConnector(that,that,that,conSettings, appSettings.instanceEncryptionPWD);
                    mBTConnector.Start();
                }
            }
        });

        timeHandler = new Handler();
        mStatusChecker.run();

        HandlerThread btConnHandlerThread = new HandlerThread("BTConnHandlerThread");
        btConnHandlerThread.start();
        setHandler(btConnHandlerThread.getLooper());

        //for demo & testing to keep lights on
        // ToDo: honor the Menu checkbox
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

        // would need to make sure here that BT & Wifi both are on !!
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if(!bluetooth.isEnabled()){
            bluetooth.enable();
        }
        //create & start connector
        mBTConnector = new BTConnector(this,this,this,conSettings, appSettings.instanceEncryptionPWD);
        mBTConnector.Start();
    }

    private Runnable speakVolumeChange = new Runnable() {
        @Override
        public void run() {
            // ToDo: Settings / Shared Preferences slider
            if (mySpeech.isReady) {
                mySpeech.setVolumeLevel("0.5");
                mySpeech.speak("Volume reduced");
            }
            else
            {
                timeHandler.postDelayed(speakVolumeChange, 1000L);
             }
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        timeHandler.postDelayed(speakVolumeChange, 1500L);

        super.onPostCreate(savedInstanceState);
    }


    public void ShowSummary(){

        if(mTestDataFile != null){
            Intent intent = new Intent(getBaseContext(), DebugSummaryActivity.class);
            startActivity(intent);
        }
    }

    MenuItem menuItem_action_force_screen_on;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menuItem_action_force_screen_on = menu.findItem(R.id.action_force_screen_on);
        menuItem_action_force_screen_on.setChecked(preferences.getBoolean("PREF_KEY_SCREEN_ON", false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_force_screen_on:
                item.setChecked(! item.isChecked());
                if (item.isChecked()) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
        BigBufferReceivingTimeOut.cancel();

        timeHandler.removeCallbacks(mStatusChecker);

        //delete connector
        if(mBTConnector != null) {
            mBTConnector.Start();
            mBTConnector = null;
        }

        mTestDataFile.CloseFile();
        mTestDataFile = null;

        mySpeech.stop();
    }


    private void SayAck(long gotBytes) {
        if (mBTConnectedThread != null) {
            String message = "Got bytes:" + gotBytes;
            print_line("CHAT", "SayAck: " + message);
            mBTConnectedThread.write(message.getBytes());
        }
    }

    private void sayHi() {
        if (mBTConnectedThread != null) {
            String message = "Hello from ";
            print_line("CHAT", "sayHi");
            mBTConnectedThread.write(message.getBytes());
        }
    }

    private void sayItWithBigBuffer() {
        if (mBTConnectedThread != null) {
            byte[] buffer = new byte[appSettings.BUFFER_SIZE_XFER0]; //Megabyte buffer
            new Random().nextBytes(buffer);
            print_line("CHAT", "sayItWithBigBuffer");
            mBTConnectedThread.write(buffer);
        }
    }


    // The Handler that gets information back from the BluetoothChatService
    private Handler mHandler;

    private void setHandler(Looper threadLooper) {
        mHandler = new Handler(threadLooper) {
            @Override
            public void handleMessage(Message msg) {
                // ToDo: reviewing logcat, there seems a major flaw - this runs on Main thread for a long duration. the GUI becomes entirely unresponsive.
                long handlerStartWhen = System.currentTimeMillis();
                android.util.Log.i("MAIN", "mHandler " + msg.what + " Thread " + Thread.currentThread());

                switch (msg.what) {
                    case BTConnectedThread.MESSAGE_WRITE:
                        if (amIBigSender) {
                            timeCounter = 0;
                            wroteDataAmount = wroteDataAmount + msg.arg1;
                            setTextBoxOffThread(R.id.CountBox, "" + wroteDataAmount);
                            if (wroteDataAmount == appSettings.BUFFER_SIZE_XFER0) {
                                if (mTestDataFile != null) {
                                    // lets do saving after we got ack received
                                    //sendMessageCounter = sendMessageCounter+ 1;
                                    //((TextView) findViewById(R.id.msgSendCount)).setText("" + sendMessageCounter);
                                    mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GoBigtData);
                                    long timeval = mTestDataFile.timeBetween(TestDataFile.TimeForState.GoBigtData, TestDataFile.TimeForState.GotData);

                                    final String sayoutloud = "Send " + appSettings.dataTestSizeWord + " in: " + (timeval / 1000) + " seconds.";

                                    // lets do saving after we got ack received
                                    //mTestDataFile.WriteDebugline("BigSender");

                                    print_line("CHAT", sayoutloud);
                                    mySpeech.speak(sayoutloud);
                                }
                            }
                        } else {
                            byte[] writeBuf = (byte[]) msg.obj;// construct a string from the buffer
                            String writeMessage = new String(writeBuf);
                            if (mTestDataFile != null) {
                                mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GotData);
                            }

                            wroteDataAmount = 0;
                            wroteFirstMessage = true;
                            print_line("CHAT", "Wrote: " + writeMessage);
                        }
                        break;
                    case BTConnectedThread.MESSAGE_READ:
                        if (!amIBigSender) {
                            gotDataAmount = gotDataAmount + msg.arg1;
                            timeCounter = 0;
                            setTextBoxOffThread(R.id.CountBox, "" + gotDataAmount);
                            BigBufferReceivingTimeOut.cancel();
                            BigBufferReceivingTimeOut.start();
                            if (gotDataAmount == appSettings.BUFFER_SIZE_XFER0) {
                                BigBufferReceivingTimeOut.cancel();

                                gotFirstMessage = false;
                                gotMessageCounter = gotMessageCounter + 1;
                                setTextBoxOffThread(R.id.msgGotCount, "" + gotMessageCounter);

                                if (mTestDataFile != null) {
                                    mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GoBigtData);

                                    long timeval = mTestDataFile.timeBetween(TestDataFile.TimeForState.GoBigtData, TestDataFile.TimeForState.GotData);
                                    final String sayoutloud = "Got " + appSettings.dataTestSizeWord + " in: " + (timeval / 1000) + " seconds.";

                                    mTestDataFile.WriteDebugline("Receiver");

                                    print_line("CHAT", sayoutloud);
                                    mySpeech.speak(sayoutloud);
                                }

                                //got message
                                setTextBoxOffThreadBGColor(R.id.dataStatusBox, 0xff00ff00); // green
                                SayAck(gotDataAmount);
                            }
                        } else if (gotFirstMessage) {
                            print_line("CHAT", "we got Ack message back, so lets disconnect.");

                            //got message
                            setTextBoxOffThreadBGColor(R.id.dataStatusBox, 0xff00ff00); // green

                            sendMessageCounter = sendMessageCounter + 1;
                            setTextBoxOffThread(R.id.msgSendCount, "" + sendMessageCounter);
                            if (mTestDataFile != null) {
                                mTestDataFile.WriteDebugline("BigSender");
                            }
                            // we got Ack message back, so lets disconnect
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                //There are supposedly a possible race-condition bug with the service discovery
                                // thus to avoid it, we are delaying the service discovery start here
                                public void run() {
                                    if (mBTConnectedThread != null) {
                                        mBTConnectedThread.Stop();
                                        mBTConnectedThread = null;
                                    }
                                    //Re-start the loop
                                    if (mBTConnector != null) {
                                        mBTConnector.Start();
                                    }
                                }
                            }, 1000);
                        } else {
                            byte[] readBuf = (byte[]) msg.obj;// construct a string from the valid bytes in the buffer
                            String readMessage = new String(readBuf, 0, msg.arg1);
                            if (mTestDataFile != null) {
                                mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GotData);
                            }

                            gotFirstMessage = true;
                            print_line("CHAT", "Got message: " + readMessage);
                            if (amIBigSender) {
                                setTextBoxOffThreadBGColor(R.id.dataStatusBox, 0xff0000ff); //Blue
                                sayItWithBigBuffer();
                            }
                        }
                        break;
                    case BTConnectedThread.SOCKET_DISCONNECTED: {

                        setTextBoxOffThreadBGColor(R.id.dataStatusBox, 0xffcccccc); //light Gray

                        if (mBTConnectedThread != null) {
                            mBTConnectedThread.Stop();
                            mBTConnectedThread = null;
                        }
                        print_line("CHAT", "WE are Disconnected now.");
                        //Re-start the loop
                        if (mBTConnector != null) {
                            mBTConnector.Start();
                        }
                    }
                    break;
                }
                android.util.Log.i("MAIN", "END mHandler " + msg.what + " Thread " + Thread.currentThread() + " elapsed: " + (System.currentTimeMillis() - handlerStartWhen));
            }
        };
    }

    private void setTextBoxOffThreadBGColor(final int dataStatusBox, final int colorInt) {
        outputInfoText0.post(new Runnable() {
            @Override
            public void run() {
                findViewById(dataStatusBox).setBackgroundColor(colorInt);
            }
        });
    }

    private void setTextBoxOffThread(final int textBoxId, final String textToSet)
    {
        outputInfoText0.post(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(textBoxId)).setText(textToSet);
            }
        });
    }

    public void startChat(BluetoothSocket socket, boolean incoming) {
        // with this sample we only have one connection at any time
        // thus lets delete the previous if we had any
        if (mBTConnectedThread != null) {
            mBTConnectedThread.Stop();
            mBTConnectedThread = null;
        }

        if(socket != null && socket.getRemoteDevice() != null) {
            ((TextView) findViewById(R.id.remoteHost)).setText("Last RH: " + socket.getRemoteDevice().getName());
            mySpeech.speak("Connected to " + socket.getRemoteDevice().getName());
        }
        amIBigSender = incoming;
        gotFirstMessage = false;
        wroteFirstMessage = false;
        wroteDataAmount = 0;
        gotDataAmount = 0;

        mBTConnectedThread = new BTConnectedThread(socket, mHandler);
        mBTConnectedThread.start();

        if(!amIBigSender) {
            // we'll start the cancel timer in here
            receivingTimeOutBaseTime = System.currentTimeMillis();
            // will be waiting for big buffer
            ((TextView) findViewById(R.id.dataStatusBox)).setBackgroundColor(0xff0000ff); //Blue
            sayHi();
        }
    }

    public void print_line(final String who, final String line) {
        Log.i("BtTestMaa" + who, line);
        // check if on Main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            outputInfoText0.post(new Runnable() {
                @Override
                public void run() {
                    outputInfoText0.append(who);
                    outputInfoText0.append(": ");
                    outputInfoText0.append(line);
                    outputInfoText0.append("\n");
                    timeCounter = 0;
                }
            });
        }
        else
        {
            outputInfoText0.append(who);
            outputInfoText0.append(": ");
            outputInfoText0.append(line);
            outputInfoText0.append("\n");
            timeCounter = 0;
        }
    }

    @Override
    public void Connected(BluetoothSocket socket, boolean incoming) {
        startChat(socket,incoming);
        //At this point the BTConnector is not doing anything additional
        //if we want it to continue, we would need to start it again here
        // with this example we start it after we have done communications
    }

    private TextView statusBox;
    private int COLOR_BACKGROUD_CONNECTING = Color.parseColor("#EF5350");

    @Override
    public void StateChanged(BTConnector.State newState) {

        statusBox.setText("State: " + newState);
        switch(newState){
            case Idle:
                statusBox.setBackgroundColor(0xff444444); //dark Gray
                break;
            case NotInitialized:
                statusBox.setBackgroundColor(0xffcccccc); //light Gray
                break;
            case WaitingStateChange:
                statusBox.setBackgroundColor(0xffEE82EE); // pink
                break;
            case FindingPeers:
                statusBox.setBackgroundColor(0xff00ffff); // Cyan
                break;
            case FindingServices:
                statusBox.setBackgroundColor(0xffffff00); // yellow
                if (mTestDataFile != null) {
                    mTestDataFile.SetTimeNow(TestDataFile.TimeForState.FoundPeers);
                }
                break;
            case Connecting: {
                statusBox.setBackgroundColor(COLOR_BACKGROUD_CONNECTING);  // red Material Design #EF5350, previous was 0xffff0000
                ConAttemptCounter = ConAttemptCounter + 1;
                    ((TextView) findViewById(R.id.conaCount)).setText("" + ConAttemptCounter);
                    if (mTestDataFile != null) {
                        mTestDataFile.SetTimeNow(TestDataFile.TimeForState.Connecting);
                    }
                    statusBox.append(" ");
                    statusBox.append(mBTConnector.getOutputConnectingToDetail());
                }
                break;
            case Connected: {
                statusBox.setBackgroundColor(0xff00ff00); // green
                    ConnectionCounter = ConnectionCounter + 1;
                    ((TextView) findViewById(R.id.conCount)).setText("" + ConnectionCounter);
                    if (mTestDataFile != null) {
                        mTestDataFile.SetTimeNow(TestDataFile.TimeForState.Connected);
                    }
                }
                break;
        }

        print_line("STATE", "New state: " + newState);
    }

    @Override
    public ServiceItem SelectServiceToConnect(List<ServiceItem> available) {
        ServiceItem  ret = null;

        if(connectedArray.size() > 0 && available.size() > 0) {

            int firstNewMatch = -1;
            int firstOldMatch = -1;

            for (int i = 0; i < available.size(); i++) {
                if(firstNewMatch >= 0) {
                    break;
                }
                for (int ii = 0; ii < connectedArray.size(); ii++) {
                    if (available.get(i).deviceAddress.equals(connectedArray.get(ii).deviceAddress)) {
                        if(firstOldMatch < 0 || firstOldMatch > ii){
                            //find oldest one available that we have connected previously
                            firstOldMatch = ii;
                        }
                        firstNewMatch = -1;
                        break;
                    } else {
                        if (firstNewMatch < 0) {
                            firstNewMatch = i; // select first not connected device
                        }
                    }
                }
            }

            if (firstNewMatch >= 0){
                ret = available.get(firstNewMatch);
            }else if(firstOldMatch >= 0){
                ret = connectedArray.get(firstOldMatch);
                // we move this to last position
                connectedArray.remove(firstOldMatch);
            }

            //print_line("EEE", "firstNewMatch " + firstNewMatch + ", firstOldMatch: " + firstOldMatch);

        }else if(available.size() > 0){
            ret = available.get(0);
        }
        if(ret != null){
            connectedArray.add(ret);

            // just to set upper limit for the amount of remembered contacts
            // when we have 101, we remove the oldest (that's the top one)
            // from the array
            if(connectedArray.size() > 100){
                connectedArray.remove(0);
            }
        }

        return ret;
    }
}


