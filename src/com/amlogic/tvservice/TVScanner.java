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
		public static final int EVENT_SCAN_END		= 3;

		public int type;
		public int percent;
		public int totalChannelCount;
		public int lockedStatus;
		public int channelNumber;
		public TVChannelParams channelParams;
		public String programName;
		public int programType;

		public Event(int type){
			this.type = type;
		}
	}
	
	private int hScan;
	
	private native int native_tv_scan_start(TVScannerParams scan_para);
	private native int native_tv_scan_destroy(int hscan, boolean store);
	private native int native_get_frontend_status();
	private native int native_get_frontend_signal_strength();
	private native int native_get_frontend_snr();
	private native int native_get_frontend_ber();
	
	/** Load native library*/
	static{
		System.loadLibrary("jnitvscanner");
	}

	public TVScanner(){
		hScan = 0;
	}

	/** This param is invisible to Clients, our service will load this from provider/config */
	public static class TVScannerParams extends TVScanParams{
		/** Atv set */
		private int minFreq;
		private int maxFreq;
		private int videoStd;
		private int audioStd;
		/** Dtv set */
		private int demuxID;
		private int frequencyList[];	

		public TVScannerParams(TVScanParams sp) {
			super(sp);
		}

		public void setDtvParams(int dmxID, int[] freqList) {
			this.demuxID = dmxID;
			this.frequencyList = freqList;
		}

		public void setAtvParams(int minf, int maxf, int vstd, int astd) {
			this.minFreq = minf;
			this.maxFreq = maxf;
			this.videoStd = vstd;
			this.audioStd = astd;
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

	public int getFrontendStatus(){
		return native_get_frontend_status();
	}

	public int getFrontendSignalStrength(){
		return native_get_frontend_signal_strength();
	}

	public int getFrontendSNR(){
		return native_get_frontend_snr();
	}

	public int getFrontendBER(){
		return native_get_frontend_ber();
	}


	abstract public void onEvent(Event evt);
}

