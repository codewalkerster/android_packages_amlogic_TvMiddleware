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
		public static final int EVENT_BLINDSCAN_PROGRESS = 4;
		public static final int EVENT_BLINDSCAN_NEWCHANNEL	= 5;
		public static final int EVENT_BLINDSCAN_END	= 6;

		public int type;
		public int percent;
		public int totalChannelCount;
		public int lockedStatus;
		public int channelNumber;
		public TVChannelParams channelParams;
		public byte[] programName;
		public int programType;
		public String msg;
		
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
		private int channelID; //can be used for manual scan
		/** Atv set */
		private int minFreq;
		private int maxFreq;
		private int tunerStd;
		/** Dtv set */
		private int demuxID;
		private boolean resortAllPrograms;
		private boolean clearSource;
		private boolean mixTvRadio;
		private TVChannelParams ChannelParamsList[];

		/** Dtv-Sx set Unicable settings*/
		private int user_band;
		private int ub_freq;//!< kHz

		public TVScannerParams(TVScanParams sp) {
			super(sp);
		}

		public void setDtvParams(int dmxID, TVChannelParams[] chanelList, 
			boolean resortAll, boolean clearAll, boolean mixTvRadio) {
			this.demuxID = dmxID;
			this.ChannelParamsList = chanelList;
			this.resortAllPrograms = resortAll;
			this.clearSource = clearAll;
			this.mixTvRadio = mixTvRadio;

			/*
			Log.d(TAG, "setDtvParams " + this.ChannelParamsList.length);
			for(int i = 0; i < this.ChannelParamsList.length; i++){
				Log.d(TAG, "setDtvParams " + i + " " + this.ChannelParamsList[i].tv_satparams);
			}
			*/
			
		}


		public void setDtvSxUnicableParams(int user_band, int ub_freq) {
                        this.user_band = user_band;
                        this.ub_freq = ub_freq;
                }

		public void setAtvParams(int minf, int maxf, int tunerStd, int chanID) {

			this.minFreq = minf;
			this.maxFreq = maxf;
			this.tunerStd = tunerStd;
			this.channelID = chanID;
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

