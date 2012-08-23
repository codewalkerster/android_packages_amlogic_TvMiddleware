package com.amlogic.tvservice;

public class TVEpgScanner{
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
		public static final int EVENT_ACT_PF_EIT  = 1;
		public static final int EVENT_ACT_SCH_EIT = 2;
		public static final int EVENT_OTH_PF_EIT  = 3;
		public static final int EVENT_OTH_SCH_EIT = 4;
		public static final int EVENT_PMT         = 5;
		public static final int EVENT_ACT_SDT     = 6;
		public static final int EVENT_OTH_SDT     = 6;
		public static final int EVENT_BAT         = 7;
		public static final int EVENT_NIT         = 8;

		public int type;
		public int dvbServiceID;
	}

	public void startScan(int mask){
	}

	public void stopScan(int mask){
	}
}

