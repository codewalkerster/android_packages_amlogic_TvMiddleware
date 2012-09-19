package com.amlogic.tvservice;

import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVChannelParams;

abstract public class TVScanner{
	public class Event{
		public static final int EVENT_SCAN_PROGRESS = 0;
		public static final int EVENT_STORE_BEGIN   = 1;
		public static final int EVENT_STORE_END     = 2;

		public int type;
		public int percent;
		public int channelNumber;
		public TVChannelParams channelParams;
		public String programName;
		public int programType;
	}

	public void scan(TVScanParams params){
	}

	public void stop(boolean store){
	}

	abstract public void onEvent(Event evt);
}

