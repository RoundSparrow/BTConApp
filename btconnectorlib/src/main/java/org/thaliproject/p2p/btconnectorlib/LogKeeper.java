package org.thaliproject.p2p.btconnectorlib;

import java.util.ArrayList;

/**
 * Created by adminsag on 3/5/16.
 * @author Stephen A. Gutknecht
 * Copyright 2016 Stephen A Gutknecht, All Rights Reserved.
*/
public class LogKeeper {
	public static final int L_A_NORMAL = 0;
	public static final int L_A_MEDIUM = 2;
	public static final int L_A_HIGH = 3;
	public static final int L_A_SHOW_A = 4;  // Highlight Show A

	public static final int L_B_NORMAL = 0;

	private static boolean isEnabled = true;
	private static ArrayList<String> logHolder = new ArrayList<>();
	private static long timeRefWhen = System.currentTimeMillis();
	private static final int MAXIMUM_LOG_SIZE = 300;
	private static int logEntriesRemovedCount = 0;

	public static void addLogEntry(String who, String content, int levelA, int levelB) {
		if (! isEnabled)
		{
			return;
		}
		else
		{
			logHolder.add(levelA + ":" + levelB + "[" + (System.currentTimeMillis() - timeRefWhen) + "] " + who + ": " + content);
			if (logHolder.size() > MAXIMUM_LOG_SIZE)
			{
				logHolder.remove(0);
				logEntriesRemovedCount++;
			}
		}
	}

	public static int getLogEntriesRemovedCount()
	{
		return logEntriesRemovedCount;
	}

	public static ArrayList<String> getLog() {
		return logHolder;
	}
}
