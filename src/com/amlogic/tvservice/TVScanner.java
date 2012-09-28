package com.amlogic.tvservice;

import android.util.Log;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVChannelParams;

abstract public class TVScanner{
	private static final String TAG = "TVScanner";
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
	
	private int hScan;
	
	private native int native_tv_scan_start(TVScannerParams scan_para);
	private native int native_tv_scan_destroy(int hscan, boolean store);
	/** Load native library*/
	static{
		System.loadLibrary("jnitvscanner");
	}

	public TVScanner(){
		hScan = 0;
	}

	//This param is invisible to Clients, our service will load this from provider/config
	public static class TVScannerParams extends TVScanParams{
		/** Atv set */
		public int minFreq;
		public int maxFreq;
		public int videoStd;
		public int audioStd;
		/** Dtv set */
		public int dbNativeHandle;
		public int demuxID;
		public int frequencyList[];	

		public TVScannerParams(TVScanParams sp) {
			super(sp);
			Log.d(TAG, "TVScannerParams end");
		}
	};

	public void scan(TVScannerParams params){
		hScan = native_tv_scan_start(params);
	}

	public void stop(boolean store){
		if (hScan != 0) {
			native_tv_scan_destroy(hScan, store);
		}
	}

	abstract public void onEvent(Event evt);
}

