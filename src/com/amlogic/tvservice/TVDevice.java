package com.amlogic.tvservice;

import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVConst;
import java.io.File;
import android.util.Log;
import android.amlogic.Tv;
import android.amlogic.Tv.SourceSwitchListener;
import android.amlogic.Tv.SrcInput;
import android.amlogic.Tv.StatusDTVChangeListener;
import android.os.Handler;
import android.os.Message;

abstract public class TVDevice implements StatusDTVChangeListener,SourceSwitchListener{
		
	
	
		
 
	public class DTVRecordParams{
		public File file;
		public TVProgram.Video video;
		public TVProgram.Audio audio[];
		public TVProgram.Subtitle subtitle[];
		public TVProgram.Teletext teletext[];
		public int currAudio;
		public int currSubtitle;
		public int currTeletext;
		public int recTotalSize;
		public int recTotalTime;
		public int recCurrPos;
	}

	public class Event{
		public static final int EVENT_SET_INPUT_SOURCE_OK     = 0;
		public static final int EVENT_SET_INPUT_SOURCE_FAILED = 1;
		public static final int EVENT_VCHIP_BLOCKED           = 2;
		public static final int EVENT_VCHIP_UNBLOCKED         = 3;
		public static final int EVENT_FRONTEND                = 4;
		public static final int EVENT_DTV_NO_DATA             = 5;
		public static final int EVENT_DTV_CANNOT_DESCRAMLE    = 6;
		public static final int EVENT_RECORD                  = 7;

		public int             type;
		public TVChannelParams feParams;
		public int             feStatus;
		public DTVRecordParams recParams;

		public Event(int type){
			this.type = type;
		}
	}
	
	Handler handler = new Handler() {
		//Event event  = new Event();
		public void handleMessage(Message msg) {
			int status = 0;
			switch (msg.what) {			
			case EVENT_FRONTEND:
				status = (Integer)msg.obj;
					Event myEvent = new Event(Event.EVENT_FRONTEND);
				if(status == NATVIVE_EVENT_SIGNAL_OK ){
					Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK");
					myEvent.feStatus = TVChannelParams.FE_HAS_LOCK;
				}else
				if(status == NATVIVE_EVENT_SIGNAL_NOT_OK ){
					Log.v(TAG,"NATVIVE_EVENT_SIGNAL_NOT_OK");
					myEvent.feStatus = TVChannelParams.FE_TIMEDOUT;
				}
				
				int mode = msg.getData().getInt("mode");
				int freq = msg.getData().getInt("freq");
				int para1 = msg.getData().getInt("para1");
				int para2 = msg.getData().getInt("para2");
				Log.v(TAG,"mode freq para1 para2:" + mode + "," + freq+ "," + para1+ "," + para2);
				switch(mode){
					case TVChannelParams.MODE_OFDM:
					
						break;
					case TVChannelParams.MODE_QAM:
						Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK MODE_QAM");
						myEvent.feParams  = TVChannelParams.dvbcParams(freq, para2, para1);
						break;
					case TVChannelParams.MODE_ANALOG:
						//*****************temp default set pat I************************
						Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK MODE_ANALOG");
						myEvent.feParams  = TVChannelParams.analogParams(freq, TVChannelParams.STD_PAL_I, 0);
						break;
				}
				TVDevice.this.onEvent(myEvent);
			break;
			
			
			case EVENT_SOURCE_SWITCH:
				status = (Integer)msg.obj;
				Event myEvent1 = new Event(Event.EVENT_SET_INPUT_SOURCE_OK);
				TVDevice.this.onEvent(myEvent1);
			break;
			}
		}
	};

	private int native_handle;
	private boolean destroy;
	private String  TAG = "TVDevice";
	public static Tv tv = null;
	public static final int NATVIVE_EVENT_FRONTEND    	= 	1;
	public static final int NATVIVE_EVENT_PLAYER    	=   2;
	public static final int NATVIVE_EVENT_SIGNAL_OK   	= 	1;
	public static final int NATVIVE_EVENT_SIGNAL_NOT_OK    	=   0;
	
	public static final int EVENT_FRONTEND    			= 	1<<1;
	public static final int EVENT_SOURCE_SWITCH    			= 	1<<2;
	



	private native void native_device_init();
	private native void native_device_destroy();
	private native void native_set_input_source(int src);
	private native void native_set_frontend(TVChannelParams params);
	private native TVChannelParams native_get_frontend();
	private native int  native_get_frontend_status();
	private native int  native_get_frontend_signal_strength();
	private native int  native_get_frontend_snr();
	private native int  native_get_frontend_ber();
	private native void native_free_frontend();
	private native void native_start_vbi(int flags);
	private native void native_stop_vbi(int flags);
	private native void native_play_atv();
	private native void native_stop_atv();
	private native void native_play_dtv(int vpid, int vfmt, int apid, int afmt);
	private native void native_stop_dtv();
	private native void native_start_recording(DTVRecordParams params);
	private native DTVRecordParams native_stop_recording();
	private native void native_start_timeshifting(DTVRecordParams params);
	private native DTVRecordParams native_stop_timeshifting();
	private native void native_start_playback(DTVRecordParams params);
	private native void native_stop_playback();
	private native void native_fast_forward(int speed);
	private native void native_fast_backward(int speed);
	private native void native_pause();
	private native void native_resume();
	private native void native_seek_to(int pos);

	static{
		System.loadLibrary("am_adp");
		System.loadLibrary("am_mw");
		System.loadLibrary("zvbi");
		System.loadLibrary("jnitvdevice");
	}

	public TVDevice(){
		destroy = false;
		//native_device_init();
		
		tv = SingletonTv.getTvInstance();
        tv.SetStatusDTVChangeListener(this);
        tv.SetSourceSwitchListener(this);
        //tv.INIT_TV();
		
	}

	public void setInputSource(TVConst.SourceType source){
		//native_set_input_source(source.ordinal());
		Log.v(TAG,"setInputSource");
		Log.v(TAG, "^^^^^^^^^^^^^^^^^ & ^------^^ & ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		if(source == TVConst.SourceType.SOURCE_TYPE_DTV){
			Log.v(TAG,"setInputSource SOURCE_TYPE_DTV");
			tv.SetSourceInput(Tv.SrcInput.DTV);
		}else
		if(source == TVConst.SourceType.SOURCE_TYPE_ATV){
			Log.v(TAG,"setInputSource SOURCE_TYPE_ATV");
			tv.SetSourceInput(Tv.SrcInput.TV);
		
		//**********************temp************************
		//Event myEvent = new Event(Event.EVENT_SET_INPUT_SOURCE_OK);
		//this.onEvent(myEvent);
		//*********************finish************************
	
		}else
		if(source == TVConst.SourceType.SOURCE_TYPE_HDMI1){
			Log.v(TAG,"setInputSource SOURCE_TYPE_HDMI1");
			tv.SetSourceInput(Tv.SrcInput.HDMI1);
		}else
		if(source == TVConst.SourceType.SOURCE_TYPE_VGA){
			Log.v(TAG,"setInputSource SOURCE_TYPE_VGA");
			tv.SetSourceInput(Tv.SrcInput.VGA);
		}
	}

	public void setFrontend(TVChannelParams params){
		//native_set_frontend(params);
		 tv.INIT_TV();
		 if(params.mode == TVChannelParams.MODE_QAM)
			tv.SetFrontEnd(params.mode,params.frequency,params.symbolRate,params.modulation);
		 if(params.mode == TVChannelParams.MODE_ANALOG)
			tv.SetFrontEnd(params.mode,params.frequency,params.standard,0);
	}

	public TVChannelParams getFrontend(){
		return native_get_frontend();
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

	public void freeFrontend(){
		//native_free_frontend();
		tv.FreeFrontEnd();
		
	}

	public void startVBI(int flags){
		native_start_vbi(flags);
	}

	public void stopVBI(int flags){
		native_stop_vbi(flags);
	}

	public void playATV(){
		//native_play_atv();
		tv.StartTV((int)TVConst.SourceType.SOURCE_TYPE_ATV.ordinal(),  0 , 0 , 0 , 0);
	}

	public void stopATV(){
		//native_stop_atv();
		Log.v(TAG,"stopATV");
		tv.StopTV((int)TVConst.SourceType.SOURCE_TYPE_ATV.ordinal());
	}

	public void playDTV(int vpid, int vfmt, int apid, int afmt){
		//native_play_dtv(vpid, vfmt, apid, afmt);
		Log.v(TAG,"SourceType SOURCE_TYPE_DTV" + (int)TVConst.SourceType.SOURCE_TYPE_DTV.ordinal());
		tv.StartTV((int)TVConst.SourceType.SOURCE_TYPE_DTV.ordinal(),  vpid ,  apid , vfmt , afmt);
	}
	


	public void stopDTV(){
		//native_stop_dtv();
		Log.v(TAG,"stopDTV");
		tv.StopTV((int)TVConst.SourceType.SOURCE_TYPE_DTV.ordinal());
	}

	public void startRecording(DTVRecordParams params){
		native_start_recording(params);
	}

	public DTVRecordParams stopRecording(){
		return native_stop_recording();
	}

	public void startTimeshifting(DTVRecordParams params){
	}

	public DTVRecordParams stopTimeshifting(){
		return native_stop_timeshifting();
	}

	public void startPlayback(DTVRecordParams params){
		native_start_playback(params);
	}

	public void stopPlayback(){
		native_stop_playback();
	}

	public void pause(){
		native_pause();
	}

	public void resume(){
		native_resume();
	}

	public void fastForward(int speed){
		native_fast_forward(speed);
	}

	public void fastBackward(int speed){
		native_fast_backward(speed);
	}

	public void seekTo(int pos){
		native_seek_to(pos);
	}

	abstract public void onEvent(Event event);
	
	protected void finalize() throws Throwable {
		if(!destroy){
			destroy = false;
			native_device_destroy();
		}
	}
	
	public void onStatusDTVChange(int type,int state,int mode,int freq,int para1,int para2) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onStatusDTVChange:	" +type + "  " +state + "  " +mode + "  " +freq + "  " +para1 + "  " +para2);
		Message msg;
	   
		if(type == NATVIVE_EVENT_FRONTEND ){ //frontEnd
				msg = handler.obtainMessage(EVENT_FRONTEND, new Integer(state));
				msg.getData().clear();
				msg.getData().putInt("mode", mode);
				msg.getData().putInt("freq", freq);
				msg.getData().putInt("para1", para1);
				msg.getData().putInt("para2", para2);
				handler.sendMessage(msg);
		}
	}
	
	
	public void onSourceSwitchStatusChange(SrcInput input, int state){
		Log.v(TAG,"onSourceSwitchStatusChange:	" + input.toString() + state);
		Message  msg = handler.obtainMessage(EVENT_SOURCE_SWITCH, new Integer(state));
		 handler.sendMessage(msg);
	}
}

class SingletonTv {
		static   Tv  instance = null;
		public synchronized static  Tv getTvInstance() {
			if (instance == null) {
				instance = Tv.open();           
			}        
			return instance;
		}
}

