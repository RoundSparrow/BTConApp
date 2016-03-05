package org.thaliproject.p2p.btconapp;

/**
 * Created by adminsag on 3/4/16.
 */
public class ApplicationSettings {
	// public static final int BUFFER_SIZE_XFER0 = 1048576;
	// public String dataTestSizeWord = "megabyte";
	// public long receiveTimeMaximum = 30000L;

	public static final int BUFFER_SIZE_XFER0 = 1048576 * 4;
	public String dataTestSizeWord = "Four megabytes";
	public long receiveTimeMaximum = 70000L;

	private int mExitWithDelay = 120; // 60 seconds test before exiting
	protected boolean mExitWithDelayIsOn = true; // set false if we are not uisng this app for testing

	final String instanceEncryptionPWD = "CHANGEYOURPASSWORDHERE";
	//   final String serviceTypeIdentifier = "_BTCL_p2p._tcp";
	final String BtUUID                = "fa87c0d0-afac-11de-8a39-0800200c9a66";
	final String Bt_NAME               = "Thaili_Bluetooth";

	//todo remove after tests
	final String serviceTypeIdentifier = "_HUMPPAA._tcp";
}
