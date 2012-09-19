package com.amlogic.tvservice;

abstract public class TVEpgScanner{
	public static final int SCAN_ACT_PF_EIT  = 1;
	public static final int SCAN_ACT_SCH_EIT = 2;
	public static final int SCAN_OTH_PF_EIT  = 4;
	public static final int SCAN_OTH_SCH_EIT = 8;
	public static final int SCAN_PAT_PMT     = 16;
	public static final int SCAN_ACT_SDT     = 32;
	public static final int SCAN_OTH_SDT     = 64;
	public static final int SCAN_BAT         = 128;
	public static final int SCAN_NIT         = 256;
	public static final int SCAN_TDT         = 512;
	public static final int SCAN_VCT         = 1024;
	public static final int SCAN_RRT         = 2048;
	public static final int SCAN_ALL         = 0xFFFFFFFF;

	public class Event{
		public static final int EVENT_PF_EIT_END  = 1;
		public static final int EVENT_SCH_EIT_END = 2;
		public static final int EVENT_PMT_END     = 3;
		public static final int EVENT_SDT_END     = 4;
		public static final int EVENT_TDT_END     = 5;
		public static final int EVENT_NIT_END     = 6;

		public int type;
		public int channelID;
		public int dvbOrigNetID;
		public int dvbTSID;
		public int dvbServiceID;
		public long time;
		public int dvbVersion;
	}

	public int channelID;

	public synchronized void setChannelID(int id){
		channelID = id;
	}

	private void startTable(int mask){
	}

	private void stopTable(int mask){
	}

	public void start(int channelID){
		synchronized(this){
			this.channelID = channelID;
		}

		startTable(SCAN_ALL);
	}

	public void stop(){
		stopTable(SCAN_ALL);

		synchronized(this){
			this.channelID = -1;
		}
	}

	abstract void onEvent(Event event);
}

