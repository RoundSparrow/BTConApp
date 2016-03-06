package org.thaliproject.p2p.btconapp;

import android.util.Log;

/**
 * Created by adminsag on 3/4/16.
 * @author Stephen A. Gutknecht
 * portions (c) Copyright 2016 Stephen A. Gutknecht. All Rights Reserved.
 */
public class ApplicationSettings {

	public int bluetoothXFerBufferSize = 1048576 * 4;
	public String dataTestSizeWord = "Four megabytes";
	public long receiveTimeMaximum = 70000L;

	/*
	For End-to-End testing we can use timer here to stopListening the process
	for example after 1 minute.
	The value is determined by mExitWithDelay
	*/
	public int mExitWithDelay = 120; // 60 seconds test before exiting
	protected boolean mExitWithDelayIsOn = true; // set false if we are not uisng this app for testing

	final String instanceEncryptionPWD = "CHANGEYOURPASSWORDHERE";
	//   final String serviceTypeIdentifier = "_BTCL_p2p._tcp";
	final String BtUUID                = "fa87c0d0-afac-11de-8a39-0800200c9a66";
	final String Bt_NAME               = "Thaili_Bluetooth";

	//todo remove after tests
	final String serviceTypeIdentifier = "_HUMPPAA._tcp";

	public int maximumConnectedBeforeRemoval = 100;


	public void setBluetoothDataSizeIndexTo(int zeroToFour)
	{
		switch (zeroToFour)
		{
			case 0:
				bluetoothXFerBufferSize = 1024 * 512;
				receiveTimeMaximum = 20L * 1000L;
				dataTestSizeWord = "half megabyte";
				break;
			case 1:
				bluetoothXFerBufferSize = 1024 * 512 * 2;
				receiveTimeMaximum = 35L * 1000L;
				dataTestSizeWord = "megabyte";
				break;
			case 2:
				bluetoothXFerBufferSize = 1024 * 512 * 4;
				receiveTimeMaximum = 55L * 1000L;
				dataTestSizeWord = "two megabytes";
				break;
			case 3:
				bluetoothXFerBufferSize = 1024 * 512 * 8;
				receiveTimeMaximum = 100L * 1000L;
				dataTestSizeWord = "four megabytes";
				break;
			case 4:
				bluetoothXFerBufferSize = 1024 * 512 * 16;
				receiveTimeMaximum = 180L * 1000L;
				dataTestSizeWord = "eight megabytes";
				break;
		}
		Log.d("MainActivity", "Bluetooth data transfer set to bytes: " + bluetoothXFerBufferSize + " timeout: " + receiveTimeMaximum);
	}
}
