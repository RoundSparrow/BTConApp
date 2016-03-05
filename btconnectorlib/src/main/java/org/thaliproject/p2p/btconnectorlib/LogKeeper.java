package org.thaliproject.p2p.btconnectorlib;

import java.util.ArrayList;

/**
 * Created by adminsag on 3/5/16.
 */
public class LogKeeper {
	private static ArrayList<String> logHolder = new ArrayList<>();
	private static boolean isEnabled = true;
	private static long timeRefWhen = System.currentTimeMillis();
	private static final int MAXIMUM_LOG_SIZE = 250;

	public static void addLogEntry(String who, String content, int levelA, int levelB) {
		if (! isEnabled)
		{
			return;
		}
		else
		{
			logHolder.add("[" + (System.currentTimeMillis() - timeRefWhen) + "] " + who + ": " + content);
			if (logHolder.size() > MAXIMUM_LOG_SIZE)
			{
				logHolder.remove(0);
			}
		}
	}
}
