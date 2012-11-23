package com.amlogic.tvservice;

abstract public class TVEpgScanner{
	public static final int MODE_ADD    = 0;
	public static final int MODE_REMOVE = 1;
	public static final int MODE_SET    = 2;

	public static final int SCAN_PAT = 0x01;
	public static final int SCAN_PMT = 0x02;
	public static final int SCAN_CAT = 0x04;
	public static final int SCAN_SDT = 0x08;
	public static final int SCAN_NIT = 0x10;
	public static final int SCAN_TDT = 0x20;
	public static final int SCAN_EIT_PF_ACT = 0x40;
	public static final int SCAN_EIT_PF_OTH = 0x80;
	public static final int SCAN_EIT_SCHE_ACT = 0x100;
	public static final int SCAN_EIT_SCHE_OTH = 0x200;
	public static final int SCAN_MGT = 0x400;
	public static final int SCAN_VCT = 0x800;
	public static final int SCAN_STT = 0x1000;
	public static final int SCAN_RRT = 0x2000;
	public static final int SCAN_PSIP_EIT   = 0x4000;
	public static final int SCAN_EIT_PF_ALL = SCAN_EIT_PF_ACT | SCAN_EIT_PF_OTH;
	public static final int SCAN_EIT_SCHE_ALL = SCAN_EIT_SCHE_ACT | SCAN_EIT_SCHE_OTH;
	public static final int SCAN_EIT_ALL = SCAN_EIT_PF_ALL | SCAN_EIT_SCHE_ALL;
	public static final int SCAN_ALL = SCAN_PAT | SCAN_PMT | SCAN_CAT | SCAN_SDT | SCAN_NIT | SCAN_TDT | SCAN_EIT_ALL |
			SCAN_MGT | SCAN_VCT | SCAN_STT | SCAN_RRT | SCAN_PSIP_EIT;

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

	private native void native_epg_create(int fend_id, int dmx_id, int src);
	private native void native_epg_destroy();
	private native void native_epg_change_mode(int op, int mode);
	private native void native_epg_monitor_service(int src_id);

	/** Load native library*/
	static{
		System.loadLibrary("jnitvepgscanner");
	}

	private int fend_dev_id = -1;
	private int dmx_dev_id  = -1;
	private int fend_type   = -1;
	private boolean created = false;

	public TVEpgScanner(){
	}

	public void destroy(){
		if(created){
			native_epg_destroy();
			created = false;
		}
	}

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

