package com.amlogic.tvservice;

import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVChannelParams;


public class TVScanner{
	public class Event{
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

	public void onEvent(Event evt){
	}
}

