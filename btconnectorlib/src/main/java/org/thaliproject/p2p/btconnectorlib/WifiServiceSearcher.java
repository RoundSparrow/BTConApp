package org.thaliproject.p2p.btconnectorlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;


/**
 * Created by juksilve on 28.2.2015.
 */

public class WifiServiceSearcher {

    private final Context context;
    private BroadcastReceiver receiver;
    private final String SERVICE_TYPE;

    private final WifiBase.WifiStatusCallBack callback;
    private final WifiP2pManager p2p;
    private final WifiP2pManager.Channel channel;
    private WifiP2pManager.PeerListListener peerListListener;

    enum ServiceState{
        NONE,
        DiscoverPeer,
        DiscoverService
    }

    ServiceState myServiceState = ServiceState.NONE;

    final List<ServiceItem> myServiceList = new ArrayList<>();

    final CountDownTimer ServiceDiscoveryTimeOutTimer = new CountDownTimer(60000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            // not using
        }

        @Override
        public void onFinish() {
            stopDiscovery();
            startPeerDiscovery();
        }
    };

    CountDownTimer peerDiscoveryTimer = null;

    public WifiServiceSearcher(Context inContext, WifiP2pManager inManager, WifiP2pManager.Channel managerChannel, WifiBase.WifiStatusCallBack handler,String serviceType) {
        this.context = inContext;
        this.p2p = inManager;
        this.channel = managerChannel;
        this.callback = handler;
        this.SERVICE_TYPE = serviceType;

        Random ran = new Random(System.currentTimeMillis());

        // if this 4 seconds minimum, then we see this
        // triggering before we got all services
        long millisInFuture = 5000 + (ran.nextInt(5000));

        debug_print("peerDiscoveryTimer timeout value:" + millisInFuture);

        peerDiscoveryTimer = new CountDownTimer(millisInFuture, 1000) {
            public void onTick(long millisUntilFinished) {
                // not using
            }
            public void onFinish() {
                myServiceState = ServiceState.NONE;
                if(callback != null) {
                    callback.gotServicesList(myServiceList);
                }else{
                    startPeerDiscovery();
                }
            }
        };
    }

    public List<ServiceItem> GetServiceList(){
        return  myServiceList;
    }

    public void start() {

        receiver = new ServiceSearcherReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);

        this.context.registerReceiver(receiver, filter);

        peerListListener = new WifiP2pManager.PeerListListener() {

            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {

                final WifiP2pDeviceList pers = peers;
                if (pers.getDeviceList().size() > 0) {
                    // this is called still multiple time time-to-time
                    // so need to make sure we only make one service discovery call
                    if (myServiceState != ServiceState.DiscoverService) {

                        if (callback != null) {
                            callback.gotPeersList(pers.getDeviceList());
                        }
                        //tests have shown that if we have multiple peers with services advertising
                        // who disappear same time when we do this, there is a chance that we get stuck
                        // thus, if this happens, in 60 seconds we'll cancel this query and start peer discovery again
                        ServiceDiscoveryTimeOutTimer.start();
                        startServiceDiscovery();
                    }
                }
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {

            @Override
            public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                LogKeeper.addLogEntry("WSS_", "Found Service: " + instanceName + " type: " + serviceType, LogKeeper.L_A_HIGH, LogKeeper.L_B_NORMAL);

                if (serviceType.startsWith(SERVICE_TYPE)) {
                    boolean addService = true;
                    for (ServiceItem aMyServiceList : myServiceList) {
                        if (aMyServiceList.deviceAddress.equals(device.deviceAddress)) {
                            addService = false;
                        }
                    }
                    if (addService) {
                        myServiceList.add(new ServiceItem(instanceName, serviceType, device.deviceAddress, device.deviceName));
                    }

                } else {
                    debug_print("Not our Service: " + SERVICE_TYPE + "!=" + serviceType);
                }

                ServiceDiscoveryTimeOutTimer.cancel();

                peerDiscoveryTimer.cancel();
                peerDiscoveryTimer.start();
            }
        };

        p2p.setDnsSdResponseListeners(channel, serviceListener, null);
        startPeerDiscovery();
    }

    public void stop() {
        this.context.unregisterReceiver(receiver);
        ServiceDiscoveryTimeOutTimer.cancel();
        peerDiscoveryTimer.cancel();
        stopDiscovery();
        stopPeerDiscovery();
    }

    private void startPeerDiscovery() {
        p2p.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                myServiceState = ServiceState.DiscoverPeer;
                debug_print("Started peer discovery");
            }

            @Override
            public void onFailure(int reason) {
                myServiceState = ServiceState.NONE;
                debug_print("Starting peer discovery failed, error code " + reason);
                //lets try again after 1 minute time-out !
                ServiceDiscoveryTimeOutTimer.start();
            }
        });
    }

    private void stopPeerDiscovery() {
        p2p.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                debug_print("Stopped peer discovery");
            }

            @Override
            public void onFailure(int reason) {
                debug_print("Stopping peer discovery failed, error code " + reason);
            }
        });
    }

    private void startServiceDiscovery() {

        myServiceState = ServiceState.DiscoverService;

        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(SERVICE_TYPE);
        final Handler handler = new Handler();
        p2p.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                debug_print("Added service request");
                handler.postDelayed(new Runnable() {
                    //There are supposedly a possible race-condition bug with the service discovery
                    // thus to avoid it, we are delaying the service discovery start here
                    public void run() {
                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                myServiceList.clear();
                                debug_print("Started service discovery");
                                myServiceState = ServiceState.DiscoverService;
                            }

                            @Override
                            public void onFailure(int reason) {
                                stopDiscovery();
                                myServiceState = ServiceState.NONE;
                                debug_print("Starting service discovery failed, error code " + reason);
                                //lets try again after 1 minute time-out !
                                ServiceDiscoveryTimeOutTimer.start();
                            }
                        });
                    }
                }, 1000L);
            }

            @Override
            public void onFailure(int reason) {
                myServiceState = ServiceState.NONE;
                debug_print("Adding service request failed, error code " + reason);
                //lets try again after 1 minute time-out !
                ServiceDiscoveryTimeOutTimer.start();
            }
        });
    }

    private void stopDiscovery() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() { debug_print("Cleared service requests"); }

            @Override
            public void onFailure(int reason) { debug_print("Clearing service requests failed, error code " + reason); }
        });
    }

    private void debug_print(String output) {
        Log.i("WiFiServiceSearcher", output);
        LogKeeper.addLogEntry("WiFi_SS", output, LogKeeper.L_A_NORMAL, LogKeeper.L_B_NORMAL);
    }

    private class ServiceSearcherReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(myServiceState != ServiceState.DiscoverService) {
                    p2p.requestPeers(channel, peerListListener);
                }
            } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                String persTatu = "Discovery state changed to ";

                if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                    persTatu = persTatu + "Stopped.";
                    startPeerDiscovery();
                }else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED){
                    persTatu = persTatu + "Started.";
                }else{
                    persTatu = persTatu + "unknown  " + state;
                }
                debug_print(persTatu);
            }
        }
    }
}
