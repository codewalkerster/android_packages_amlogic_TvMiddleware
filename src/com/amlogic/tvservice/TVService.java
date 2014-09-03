package com.amlogic.tvservice;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.os.Message;
import android.os.Looper;
import android.os.Handler;
import android.os.RemoteException;
import android.database.Cursor;
import java.util.Date;
import android.os.RemoteCallbackList;
import android.util.Log;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVConst.CC_ATV_AUDIO_STANDARD;
import com.amlogic.tvutil.TVConst.CC_ATV_VIDEO_STANDARD;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVChannel;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVStatus;
import com.amlogic.tvutil.TVBooking;
import com.amlogic.tvutil.DTVPlaybackParams;
import com.amlogic.tvutil.DTVRecordParams;
import com.amlogic.tvutil.TvinInfo;
import com.amlogic.tvutil.TVDimension;
import com.amlogic.tvutil.TVEvent;
import com.amlogic.tvutil.TVRegion;
import com.amlogic.tvutil.TVDBTransformer;
import com.amlogic.tvutil.TVMultilingualText;
import com.amlogic.tvdataprovider.TVDataProvider;
import android.os.SystemClock;
import android.os.Looper;
import com.amlogic.tvutil.TvinInfo;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

public class TVService extends Service implements TVConfig.Update{
	private static final String TAG = "TVService";

	/*Message types*/
	private static final int MSG_SET_SOURCE    = 1949;
	private static final int MSG_PLAY_PROGRAM  = 1950;
	private static final int MSG_STOP_PLAYING  = 1951;
	private static final int MSG_START_TIMESHIFTING = 1952;
	private static final int MSG_STOP_TIMESHIFTING = 1953;
	private static final int MSG_START_PLAYBACK  = 1954;
	private static final int MSG_STOP_PLAYBACK    = 1955;
	private static final int MSG_START_SCAN      = 1956;
	private static final int MSG_STOP_SCAN       = 1957;
	private static final int MSG_START_RECORDING = 1958;
	private static final int MSG_STOP_RECORDING  = 1959;
	private static final int MSG_PAUSE           = 1960;
	private static final int MSG_RESUME          = 1961;
	private static final int MSG_FAST_FORWARD    = 1962;
	private static final int MSG_FAST_BACKWARD   = 1963;
	private static final int MSG_SEEK_TO         = 1964;
	private static final int MSG_DEVICE_EVENT    = 1965;
	private static final int MSG_EPG_EVENT       = 1966;
	private static final int MSG_SCAN_EVENT      = 1967;
	private static final int MSG_BOOK_EVENT       = 1968;
	private static final int MSG_SET_PROGRAM_TYPE = 1969;
	private static final int MSG_CONFIG_CHANGED   = 1970;
	private static final int MSG_SWITCH_AUDIO     = 1971;
	private static final int MSG_RESET_ATV_FORMAT = 1972;
	private static final int MSG_FINE_TUNE        = 1973;
	private static final int MSG_RESTORE_FACTORY_SETTING = 1974;
	private static final int MSG_PLAY_VALID       = 1975;
	private static final int MSG_SET_VGA_AUTO_ADJUST = 1976;
	private static final int MSG_REPLAY              = 1977;
	private static final int MSG_CHECK_BLOCK         = 1978;
	private static final int MSG_UNBLOCK             = 1979;
	private static final int MSG_CVBS_AMP_OUT        = 1980;
	private static final int MSG_SEC             = 1981;
	private static final int MSG_LOCK             = 1982;
	
	/*Restore flags*/
	private static final int RESTORE_FL_DATABASE  = 0x1;
	private static final int RESTORE_FL_CONFIG    = 0x2;

	final RemoteCallbackList<ITVCallback> callbacks
			= new RemoteCallbackList<ITVCallback>();
	
	private synchronized void sendMessage(TVMessage msg){
		final int N = callbacks.beginBroadcast();
		for (int i = 0; i < N; i++){
			try{
				callbacks.getBroadcastItem(i).onMessage(msg);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		callbacks.finishBroadcast();
	}

	private final ITVService.Stub mBinder = new ITVService.Stub(){

		public TVStatus getStatus(){
			TVStatus s = new TVStatus();

			synchronized(TVService.this){
				s.programType = programType;
				s.programNo   = programNum;
				s.programID   = programID;
			}

			return s;
		}

		public int getFrontendStatus(){
			int ret;

			synchronized(TVService.this){
				if(isScanning)
					ret = scanner.getFrontendStatus();
				else
					ret = device.getFrontendStatus();
			}

			return ret;
		}

		public int getFrontendSignalStrength(){
			int ret;

			synchronized(TVService.this){
				if(isScanning)
					ret = scanner.getFrontendSignalStrength();
				else
					ret = device.getFrontendSignalStrength();
			}

			return ret;
		}

		public int getFrontendSNR(){
			int ret;

			synchronized(TVService.this){
				if(isScanning)
					ret = scanner.getFrontendSNR();
				else
					ret = device.getFrontendSNR();
			}

			return ret;
		}

		public int getFrontendBER(){
			int ret;

			synchronized(TVService.this){
				if(isScanning)
					ret = scanner.getFrontendBER();
				else
					ret = device.getFrontendBER();
			}

			return ret;
		}
		
		public DTVPlaybackParams getPlaybackParams(){
			DTVPlaybackParams ret = null;
			
			synchronized(TVService.this){
				ret = device.getPlaybackParams();
			}
			
			return ret;
		}
		
		public DTVRecordParams getRecordingParams() {
			DTVRecordParams ret = null;
			
			synchronized(TVService.this){
				ret = recorder.getRecordingParams();
			}
			
			return ret;
		}

		public void registerCallback(ITVCallback cb){
			synchronized(TVService.this){
				if(cb !=null){
					callbacks.register(cb);
				}
			}
		}

		public void unregisterCallback(ITVCallback cb){
			synchronized(TVService.this){
				if(cb != null){
					callbacks.unregister(cb);
				}
			}
		}

		public void setConfig(String name, TVConfigValue value){
			try{
				config.set(name, value);
				onUpdate(name, value);
			}catch(Exception e){
				Log.e(TAG, "failed to set config "+name);
			}
		}

		public TVConfigValue getConfig(String name){
			TVConfigValue v = null;

			try{
				v = config.get(name);
			}catch(Exception e){
				Log.e(TAG, "failed to get config "+name);
			}

			return v;
		}

		public void registerConfigCallback(String name, ITVCallback cb){
			try{
				Log.d(TAG, "register config "+name+" callback "+cb);
				config.registerRemoteCallback(name, cb);
			}catch(Exception e){
				Log.d(TAG, "registerRemoteCallback "+name+" failed");
			}
		}

		public void unregisterConfigCallback(String name, ITVCallback cb){
			try{
				Log.d(TAG, "unregister config "+name+" callback "+ cb);
				config.unregisterRemoteCallback(name, cb);
			}catch(Exception e){
				Log.e(TAG, "unregisterRemoteCallback "+name+" failed");
			}
		}

		public void setVideoWindow(int x, int y, int w, int h){
			device.setVideoWindow(x, y, w, h);
		}

		public long getTime(){
			return time.getTime();
		}

		public void switchAudioTrack(int aud_track){
			switchAudTrack(aud_track);
		}

		public void setInputSource(int source){
			Message msg = handler.obtainMessage(MSG_SET_SOURCE, new Integer(source));
			handler.sendMessage(msg);
		}

		public int getCurInputSource(){
			return device.getCurInputSource().ordinal();
		}

		public void importDatabase(String inputXmlPath){
			new transformDBThread(transformDBThread.IMPORT, inputXmlPath).start();
		}

		public void exportDatabase(String outputXmlPath){
			new transformDBThread(transformDBThread.EXPORT, outputXmlPath).start();
		}

		public void setProgramType(int type){
			Message msg = handler.obtainMessage(MSG_SET_PROGRAM_TYPE, new Integer(type));
			handler.sendMessage(msg);
		}

		public void playProgram(TVPlayParams tp){
			Message msg = handler.obtainMessage(MSG_PLAY_PROGRAM, tp);
			handler.sendMessage(msg);
		}

		public void stopPlaying(){
			Message msg = handler.obtainMessage(MSG_STOP_PLAYING);
			handler.sendMessage(msg);
		}

		public void switchAudio(int id){
			Message msg = handler.obtainMessage(MSG_SWITCH_AUDIO, new Integer(id));
			handler.sendMessage(msg);
		}

		public void resetATVFormat(){
			Message msg = handler.obtainMessage(MSG_RESET_ATV_FORMAT);
			handler.sendMessage(msg);
		}

		public void startTimeshifting(){
			Message msg = handler.obtainMessage(MSG_START_TIMESHIFTING);
			handler.sendMessage(msg);
		}
		
		public void stopTimeshifting(){
			Message msg = handler.obtainMessage(MSG_STOP_TIMESHIFTING);
                        handler.sendMessage(msg);
		}

		public void startRecording(long duration){
			Message msg = handler.obtainMessage(MSG_START_RECORDING, new Long(duration));
                        handler.sendMessage(msg);
		}

		public void stopRecording(){
			Message msg = handler.obtainMessage(MSG_STOP_RECORDING);
			handler.sendMessage(msg);
		}

		public void startPlayback(String filePath){
			Message msg = handler.obtainMessage(MSG_START_PLAYBACK, new String(filePath));
                        handler.sendMessage(msg);
		}
		
		public void stopPlayback(){
			Message msg = handler.obtainMessage(MSG_STOP_PLAYBACK);
                        handler.sendMessage(msg);
		}

		public void startScan(TVScanParams sp){
			Message msg = handler.obtainMessage(MSG_START_SCAN, sp);
			handler.sendMessage(msg);
		}

		public void stopScan(boolean store){
			Message msg = handler.obtainMessage(MSG_STOP_SCAN, new Boolean(store));
			handler.sendMessage(msg);
		}

		public void startBooking(int bookingID){
			TVBookManager.Event evt = bookManager.new Event(TVBookManager.Event.EVENT_NEW_BOOKING_START);
			evt.bookingID = bookingID;
			Message msg = handler.obtainMessage(MSG_BOOK_EVENT, evt);
			handler.sendMessage(msg);
		}

		public void pause(){
			Message msg = handler.obtainMessage(MSG_PAUSE);
			handler.sendMessage(msg);
		}

		public void resume(){
			Message msg = handler.obtainMessage(MSG_RESUME);
			handler.sendMessage(msg);
		}

		public void fastForward(int speed){
			Message msg = handler.obtainMessage(MSG_FAST_FORWARD, new Integer(speed));
			handler.sendMessage(msg);
		}

		public void fastBackward(int speed){
			Message msg = handler.obtainMessage(MSG_FAST_BACKWARD, new Integer(speed));
			handler.sendMessage(msg);
		}

		public void seekTo(int pos){
			Message msg = handler.obtainMessage(MSG_SEEK_TO, new Integer(pos));
			handler.sendMessage(msg);
		}

		public void fineTune(int freq){
			Message msg = handler.obtainMessage(MSG_FINE_TUNE, new Integer(freq));
			handler.sendMessage(msg);
		}

		public void setCvbsAmpOut(int freq){
			Message msg = handler.obtainMessage(MSG_CVBS_AMP_OUT, new Integer(freq));
			handler.sendMessage(msg);
		}
        
		public void restoreFactorySetting(int flags){
			Message msg = handler.obtainMessage(MSG_RESTORE_FACTORY_SETTING, new Integer(flags));
			handler.sendMessage(msg);
		}
		
		public void playValid(){
			Message msg = handler.obtainMessage(MSG_PLAY_VALID);
			handler.sendMessage(msg);
		}

		public void setVGAAutoAdjust(){
			Message msg = handler.obtainMessage(MSG_SET_VGA_AUTO_ADJUST);
			handler.sendMessage(msg);
		}
		
		public void replay(){
			Message msg = handler.obtainMessage(MSG_REPLAY);
			handler.sendMessage(msg);
		}
		
		public void unblock(){
			Message msg = handler.obtainMessage(MSG_UNBLOCK);
			handler.sendMessage(msg);
		}

		public void secRequest(TVMessage sec_msg){
			Message msg = handler.obtainMessage(MSG_SEC, sec_msg);
			handler.sendMessage(msg);
		}	

		public void switch_video_blackout(int val){
			device.switch_video_blackout(val);
		}	
		
		public void lock(TVChannelParams curParams){
			Message msg = handler.obtainMessage(MSG_LOCK, curParams);
			handler.sendMessage(msg);
		}	

        @Override
        public int GetSrcInputType(){
            return device.GetSrcInputType();
        }

        @Override
        public TvinInfo getCurrentSignalInfo() 
        {
            // TODO Auto-generated method stub
            return device.GetCurrentSignalInfo();
        }

		
	};

	/*Message handler*/
	private Handler handler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
				case MSG_SET_SOURCE:
					int val = (Integer)msg.obj;
					TVConst.SourceInput type = TVConst.SourceInput.values()[val];

					resolveSetInputSource(type);
					break;
				case MSG_SET_PROGRAM_TYPE:
					resolveSetProgramType((Integer)msg.obj);
					break;
				case MSG_PLAY_PROGRAM:
					resolvePlayProgram((TVPlayParams)msg.obj);
					break;
				case MSG_STOP_PLAYING:
					resolveStopPlaying();
					break;
				case MSG_START_TIMESHIFTING:
					resolveStartTimeshifting();
					break;
				case MSG_STOP_TIMESHIFTING:
					resolveStopTimeshifting();
					break;
				case MSG_START_PLAYBACK:
					resolveStartPlayback((String)msg.obj);
					break;
				case MSG_STOP_PLAYBACK:
					resolveStopPlayback();
					break;
				case MSG_START_SCAN:
					resolveStartScan((TVScanParams)msg.obj);
					break;
				case MSG_STOP_SCAN:
					resolveStopScan((Boolean)msg.obj);
					break;
				case MSG_START_RECORDING:
					resolveStartRecording((Long)msg.obj);
					break;
				case MSG_STOP_RECORDING:
					resolveStopRecording();
					break;
				case MSG_PAUSE:
					resolvePause();
					break;
				case MSG_RESUME:
					resolveResume();
					break;
				case MSG_FAST_FORWARD:
					resolveFastForward((Integer)msg.obj);
					break;
				case MSG_FAST_BACKWARD:
					resolveFastBackward((Integer)msg.obj);
					break;
				case MSG_SEEK_TO:
					resolveSeekTo((Integer)msg.obj);
					break;
				case MSG_DEVICE_EVENT:
					resolveDeviceEvent((TVDevice.Event)msg.obj);
					break;
				case MSG_EPG_EVENT:
					resolveEpgEvent((TVEpgScanner.Event)msg.obj);
					break;
				case MSG_SCAN_EVENT:
					resolveScanEvent((TVScanner.Event)msg.obj);
					break;
				case MSG_BOOK_EVENT:
					resolveBookEvent((TVBookManager.Event)msg.obj);
					break;
				case MSG_CONFIG_CHANGED:
					resolveConfigChanged((String)msg.obj);
					break;
				case MSG_SWITCH_AUDIO:
					resolveSwitchAudio((Integer)msg.obj);
					break;
				case MSG_RESET_ATV_FORMAT:
					resolveResetATVFormat();
					break;
				case MSG_FINE_TUNE:
					resolveFineTune((Integer)msg.obj);
					break;
                		case MSG_CVBS_AMP_OUT:
					resolveSetCvbsAmpOut((Integer)msg.obj);
					break;    
                    
				case MSG_RESTORE_FACTORY_SETTING:
					resolveRestoreFactorySetting((Integer)msg.obj);
					break;
				case MSG_PLAY_VALID:
					resolvePlayValid();
					break;
				case MSG_SET_VGA_AUTO_ADJUST:
					resolveSetVGAAutoAdjust();
					break;
				case MSG_CHECK_BLOCK:
					resolveReplay(false);
					break;
				case MSG_REPLAY:
					resolveReplay(true);
					break;
				case MSG_UNBLOCK:
					resolveUnblock();
					break;
				case MSG_LOCK:
					resolveLock((TVChannelParams)msg.obj);
					break;		
				case MSG_SEC:
					resolveSec((TVMessage)msg.obj);
					break;					
			}
		}
	};

	private TVTime time = new TVTime();
	private TVConfig config;
	private TVDevice device;

	private TVScanner scanner = new TVScanner(){
		/*Scanner event handler*/
		public void onEvent(TVScanner.Event event){
			Message msg = handler.obtainMessage(MSG_SCAN_EVENT, event);
			handler.sendMessage(msg);
		}
	};

	private TVEpgScanner epgScanner = new TVEpgScanner(){
		/*EPG event handler*/
		public void onEvent(TVEpgScanner.Event event){
			switch(event.type){
				case TVEpgScanner.Event.EVENT_TDT_END:
					Log.d(TAG, "TDT time update to " + (event.time*1000));
					time.setTime(event.time*1000);
					try{
						config.set("tv:time:diff_value", new TVConfigValue(String.valueOf(time.getDiffTime())));
					}catch(Exception e){
						e.printStackTrace();
					}
					return;
				case TVEpgScanner.Event.EVENT_NIT_END:			
					//Log.d(TAG, "-------------TVEpgScanner.Event.EVENT_NIT_END---------version number="+event.dvbVersion);	
					try{
						boolean nit_scan=config.getBoolean("tv:dtv:nit_scan");
						if(nit_scan){
							String mode = config.getString("tv:dtv:mode");
							if(mode.contains("dvbs"))
								return;
							if(mode.contains("dvbc")||mode.contains("dvbt")||mode.contains("dvbt2")||mode.contains("isdbt")){
								TVMessage msg = new TVMessage(TVMessage.TYPE_NIT_TABLE_VER_CHANGED,event.dvbVersion);
								sendMessage(msg);
							}	
						}
					}catch(Exception e){
						e.printStackTrace();
					}	
					break;
			}

			//Message msg = handler.obtainMessage(MSG_EPG_EVENT, event);
			//handler.sendMessage(msg);
		}
	};
	
	private TVRecorder recorder = new TVRecorder(this){
		/*Recorder event handler*/
		@Override
		public void onEvent(TVRecorder.Event event){
			switch(event.type){
				case TVRecorder.Event.EVENT_RECORDS_UPDATE:
					Log.d(TAG, "Detect PVR records update.");
					sendMessage(TVMessage.recordsUpdate());
					break;
			}
		}
	};
	
	private TVBookManager bookManager = new TVBookManager(this){
		/*book manager event handler*/
		public void onEvent(TVBookManager.Event event){
			Message msg = handler.obtainMessage(MSG_BOOK_EVENT, event);
			handler.sendMessage(msg);
		}
	};
	
	private Runnable programBlockCheck = new Runnable() {
		public void run() {
			Message msg = handler.obtainMessage(MSG_CHECK_BLOCK);
			handler.sendMessage(msg);
			checkBlockHandler.postDelayed(this, 2000);
		}
	};

	private Runnable dataSync = new Runnable() {
		public void run() {
			TVDataProvider.sync();
			dataSyncHandler.postDelayed(this, 5000);
		}
	};

	private class transformDBThread extends Thread {
		public static final int IMPORT = 0;
		public static final int EXPORT = 1;
		
		private int transformType = -1;
		private String strXmlPath = "";
		
		public transformDBThread(int type, String xmlPath){
			transformType = type;
			strXmlPath = xmlPath;
		}
		public void run(){
			int errorCode = TVMessage.TRANSDB_ERR_NONE;
			
			sendMessage(TVMessage.transformDBStart());

			try{
				Log.d(TAG, "Start transform database, type: "+
					transformType+", xml path: "+strXmlPath);
				
				if (transformType == IMPORT && !strXmlPath.isEmpty()){
					TVDataProvider.importDatabase(TVService.this, strXmlPath);
				}else if (transformType == EXPORT && !strXmlPath.isEmpty()){
					TVDataProvider.exportDatabase(TVService.this, strXmlPath);
				}
			}catch (TVDBTransformer.InvalidFileException e){
				errorCode = TVMessage.TRANSDB_ERR_INVALID_FILE;
				Log.d(TAG, "Transform failed: " + e.getMessage());
			}catch (Exception e){
				errorCode = TVMessage.TRANSDB_ERR_SYSTEM;
				Log.d(TAG, "Transform failed: " + e.getMessage());
			}

			sendMessage(TVMessage.transformDBEnd(errorCode));
			Log.d(TAG, "Transform done !");
		}
	}

	private enum TVRunningStatus{
		STATUS_SET_INPUT_SOURCE,
		STATUS_SET_FRONTEND,
		STATUS_PLAY_ATV,
		STATUS_PLAY_DTV,
		STATUS_TIMESHIFTING,
		STATUS_PLAYBACK,
		STATUS_STOPPED,
		STATUS_SCAN,
		STATUS_ANALYZE_CHANNEL,
	}

	private TVRunningStatus status;
	private TVConst.SourceInput inputSource;
	private TVConst.SourceInput reqInputSource;
	private TVConst.SourceInput lastTVSource;
	private TVPlayParams atvPlayParams;
	private TVPlayParams dtvTVPlayParams;
	private TVPlayParams dtvRadioPlayParams;
	private int dtvProgramType = TVProgram.TYPE_TV;
	private TVChannelParams channelParams;
	private int channelID = -1;
	private int programID = -1;
	private int programVideoPID = -1;
	private int programAudioPID = -1;
	private int programType = TVProgram.TYPE_TV;
	private int dtvMode = -1;
	private TVProgramNumber programNum;
	private boolean programBlocked = false;
	private boolean channelLocked = false;
	private boolean recording = false;
	private boolean isScanning = false;
	private boolean checkBlock = true;
	private boolean isAtvEnabled = false;
	private Handler checkBlockHandler = new Handler();
	private Handler dataSyncHandler = new Handler();

	private void setDTVPlayParams(TVPlayParams params){
		if(params != null && params.getType()==TVPlayParams.PLAY_PROGRAM_ID){
			try{
				TVProgram prog = TVProgram.selectByID(this, params.getProgramID());

				if(prog != null){
					if(prog.getType() == TVProgram.TYPE_TV){
						dtvTVPlayParams = params;
					}else if(prog.getType() == TVProgram.TYPE_RADIO){
						dtvRadioPlayParams = params;
					}
				}
			}catch(Exception e){
			}

			return;
		}

		if(dtvProgramType == TVProgram.TYPE_TV)
			dtvTVPlayParams = params;
		else if(dtvProgramType == TVProgram.TYPE_RADIO)
			dtvRadioPlayParams = params;
	}

	private TVPlayParams getDTVPlayParams(){
		if(dtvProgramType == TVProgram.TYPE_TV)
			return dtvTVPlayParams;
		else if(dtvProgramType == TVProgram.TYPE_RADIO)
			return dtvRadioPlayParams;

		return null;
	}

	private int getCurProgramType(){
		int type = TVProgram.TYPE_UNKNOWN;

		try{
			if(config.getBoolean("tv:mix_atv_dtv")){
				type = TVProgram.TYPE_UNKNOWN;
			}else if(inputSource == TVConst.SourceInput.SOURCE_DTV){
				if(config.getBoolean("tv:dtv:mix_tv_radio"))
					type = TVProgram.TYPE_DTV;
				else
					type = dtvProgramType;
			}else{
					type = TVProgram.TYPE_ATV;
			}
		}catch(Exception e){
		}

		return type;
	}
	
	private TVProgram getCurrentProgram(){
		TVProgram p = null;

		if(inputSource == TVConst.SourceInput.SOURCE_ATV){
			if(atvPlayParams != null){
				p = playParamsToProgram(atvPlayParams);
			}
		}else if(inputSource == TVConst.SourceInput.SOURCE_DTV){
			TVPlayParams tp = getDTVPlayParams();
			if(tp == null){
				tp = dtvTVPlayParams;
			}
			if(tp == null){
				tp = dtvRadioPlayParams;
			}
			if(tp != null){
				p = playParamsToProgram(tp);
			}
		}
		
		return p;
	}

	private TVRegion getCurrentRegion(){
		String region = null;
		
		try {
			region = config.getString("tv:scan:dtv:region");
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot read dtv region !!!");
		}
		
		return TVRegion.selectByName(this, region);
	}

	private TVChannelParams getChannelParamsByNo(int channelNo){
		TVChannelParams cp = null;
		
		TVRegion curReg = getCurrentRegion();
		if (curReg != null){
			cp = curReg.getChannelParams(channelNo);
		}
		
		return cp;
	}
	
	private int getChannelNoByParams(TVChannelParams cp){
		int chanNo = -1;
		
		if (cp != null){
			TVRegion curReg = getCurrentRegion();
			if (curReg != null){
				chanNo = curReg.getChannelNo(cp.getFrequency());
			}
		}
		
		return chanNo;
	}

	private TVProgram getValidProgram(int type){
		int lastPlayedID = -1;
		TVProgram p;
		boolean typeMatch = false;
		
		try{
			lastPlayedID = config.getInt("tv:last_program_id");
		}catch(Exception e){
		}

		p = TVProgram.selectByID(this, lastPlayedID);
		if (p != null){
			if (p.getType() == type){
				typeMatch = true;
			}else if (type == TVProgram.TYPE_DTV && 
				(p.getType() == TVProgram.TYPE_TV || 
				 p.getType() == TVProgram.TYPE_RADIO)){
				 typeMatch = true;
			}
		}

		if (! typeMatch){
			Log.d(TAG, "Get first valid program at type " + type); 
			
			p = TVProgram.selectFirstValid(this, type);
		}

		return p;
	}

	private void setInputSourceToConfig(){
		try{
			String strSource = null;

			if (inputSource == TVConst.SourceInput.SOURCE_ATV){
				strSource = "atv";
			}else if (inputSource == TVConst.SourceInput.SOURCE_DTV){
				strSource = "dtv";
			}else{
				/* no use, to indicate that we are not in TV mode */
				strSource = "mpeg";
			}
			config.set("tv:input_source", new TVConfigValue(strSource));
		}catch (Exception e){

		}  
	}

	private void stopPlaying(){
		if(status == TVRunningStatus.STATUS_PLAY_ATV){
			device.stopATV();
			sendMessage(TVMessage.programStop(programID));
			status = TVRunningStatus.STATUS_STOPPED;
		}else if(status == TVRunningStatus.STATUS_PLAY_DTV){
			device.stopDTV();
			sendMessage(TVMessage.programStop(programID));
			status = TVRunningStatus.STATUS_STOPPED;
		}else if(status == TVRunningStatus.STATUS_TIMESHIFTING){
			device.stopTimeshifting();
			status = TVRunningStatus.STATUS_STOPPED;
		}else if(status == TVRunningStatus.STATUS_PLAYBACK){
			device.stopPlayback();
			status = TVRunningStatus.STATUS_STOPPED;
		}else if (status == TVRunningStatus.STATUS_ANALYZE_CHANNEL){
			stopChannelAnalyzing(false);
			status = TVRunningStatus.STATUS_STOPPED;
		}

		synchronized(this){
			programID = -1;
		}
		
		/* ternminate the current viewing book */
		TVBooking[] viewingBook = TVBooking.selectPlayBookingsByStatus(this, TVBooking.ST_STARTED);
		if (viewingBook != null){
			for (int i=0; i<viewingBook.length; i++){
				viewingBook[i].updateStatus(TVBooking.ST_END);
			}
		}

		checkBlock=true;
		programBlocked=false;
	}

	private void stopScan(boolean store){
		if(status == TVRunningStatus.STATUS_SCAN ||
		   status == TVRunningStatus.STATUS_ANALYZE_CHANNEL){
			scanner.stop(store);

			if(store){
				atvPlayParams = null;
				dtvTVPlayParams = null;
				dtvRadioPlayParams = null;
			}

			synchronized(this){
				channelID  = -1;
				programID  = -1;
				programNum = null;
				if (status == TVRunningStatus.STATUS_SCAN)
					channelParams = null;
			}

			status = TVRunningStatus.STATUS_STOPPED;
		}
	}

	
	public void switchAudTrack(int aud_track){
		String value=null;
		switch(aud_track){
			case 0:
				value="s";
				break;
			case 1:
				value="l";
				break;
			case 2:
				value="r";
				break;
			case 3:
				value="c";
				break;
		}

		try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/amaudio/audio_channels_mask"));
            try {
                writer.write(value);
                } finally {
                    writer.close();
                }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (Exception e) {
                Log.e(TAG,"set audio track ERROR!",e);
				return;
        } 

	}
	
	private int switchRecording(TVProgram requestProgram){
		TVProgram playingProgram = null;
		
		if (requestProgram == null)
			return 0;
			
		if (getDTVPlayParams() != null){
			 playingProgram = playParamsToProgram(getDTVPlayParams());
		}

		int playChanID = (playingProgram!=null) ? playingProgram.getChannel().getID() : -1;
		int recChanID = requestProgram.getChannel().getID();

		if(playChanID != recChanID){
			Log.d(TAG, "Channel changed, will start record after playing the new program");
			return 3;
		}
		/*if (recordingProgram.getID() == playingProgram.getID()){
			Log.d(TAG, "Program "+recordingProgram.getID()+" is already recording.");
		}else{
			*send message to clients to solve this conflict
		}*/
		
		if (programID != -1) {
			return 1;
		} else {
			return 2;
		}
	}
	
	
	private void startBooking(TVBooking book){
		boolean needPlay = false;
		
		TVProgram playingProgram = null;
		TVProgram requestProgram = book.getProgram();
		if (requestProgram == null)
			return;
			
		if (getDTVPlayParams() != null){
			 playingProgram = playParamsToProgram(getDTVPlayParams());
		}
		
		if ((book.getFlag() & TVBooking.FL_PLAY) != 0){
			needPlay = true;
		}

		TVRecorder.TVRecorderParams param = recorder.new TVRecorderParams();

 		if ((book.getFlag() & TVBooking.FL_RECORD) != 0){
 			/* stop the current recording */
 			stopRecording();
			/* check if the channel changed */
			int playChanID = (playingProgram!=null) ? playingProgram.getChannel().getID() : -1;
			int recChanID = requestProgram.getChannel().getID();
			
			param.booking = book;
			param.isTimeshift = false;
			if(playChanID != recChanID){
				Log.d(TAG, "Channel changed, will start record after playing the new program");
				needPlay = true;
				param.fendLocked = false;
			}else{
				param.fendLocked = (programID != -1);
			}
		}
		
		if (needPlay){
			TVPlayParams tp = TVPlayParams.playProgramByID(requestProgram.getID());
			resolvePlayProgram(tp);
		}

		if ((book.getFlag() & TVBooking.FL_RECORD) != 0){
			recorder.startRecord(param);
		}
	}
	
	private void recordCurrentProgram(long duration, boolean isTimeshift){
		TVProgram playingProgram = null;
		if (getDTVPlayParams() != null){
			playingProgram = playParamsToProgram(getDTVPlayParams());
			if(playingProgram == null){
				Log.d(TAG, "Cannot get current playing program");
				return;
			}
		}else{
			Log.d(TAG, "Cannot get current playing program");
			return;
		}
		
		TVProgram recordingProgram = recorder.getRecordingProgram();
		if (recordingProgram != null){
			if (recordingProgram.getID() == playingProgram.getID()){
				if (!isTimeshift){
					Log.d(TAG, "Program "+recordingProgram.getID()+" is already recording.");
					return;
				}
				else if (recorder.isTimeshifting()){
					Log.d(TAG, "Program "+recordingProgram.getID()+" is already in timeshifting.");
					return;
				}	
			}
			/**send message to clients to solve this conflict*/
			sendMessage(TVMessage.recordConflict(
				isTimeshift ? TVMessage.REC_CFLT_START_TIMESHIFT : TVMessage.REC_CFLT_START_NEW, 
				playingProgram.getID()));
			return;
		}
		
		TVRecorder.TVRecorderParams param = recorder.new TVRecorderParams();
		param.booking = new TVBooking(playingProgram, time.getTime(), duration);
		if (isTimeshift){
			/* In timeshifting mode, start the playback first to 
			 * receive the record data */
			long timeshiftDuration = 600*1000;
			try{
				timeshiftDuration = config.getInt("tv:dtv:timeshifting_time_long");
			}catch(Exception e){
			}

			Log.d(TAG, "timeshifting duration " + duration);
			DTVPlaybackParams dtp = new DTVPlaybackParams(
				recorder.getTimeshiftingFilePath(), timeshiftDuration);		
			/* Stop current play */
			stopPlaying();
			/* Start the playback */
			device.startTimeshifting(dtp);
			status = TVRunningStatus.STATUS_TIMESHIFTING;
		}
		
		/* Start the recorder */
		param.isTimeshift = isTimeshift;
		param.fendLocked = isTimeshift ? true : (programID != -1);
		recorder.startRecord(param);
	}

	private void stopRecording(){
		recorder.stopRecord();
	}

	private boolean isInTVMode(){
		if((inputSource == TVConst.SourceInput.SOURCE_ATV) ||
				(inputSource == TVConst.SourceInput.SOURCE_DTV))
			return true;
		return false;
	}

	private boolean isInFileMode(){
		if(!isInTVMode())
			return false;

		if((status == TVRunningStatus.STATUS_TIMESHIFTING) ||
				(status == TVRunningStatus.STATUS_PLAYBACK))
			return true;

		return false;
	}

	private TVProgram playParamsToProgram(TVPlayParams params){
		TVProgram p = null;

		try{
			switch(params.getType()){
				case TVPlayParams.PLAY_PROGRAM_NUMBER:
					boolean lcn = config.getBoolean("tv:dtv:dvbt:lcn");
					int type = getCurProgramType();
					TVProgramNumber num = params.getProgramNumber();
					p = TVProgram.selectByNumber(this, type, num);
					if (isAtvEnabled && num != null && num.isATSCMode()){
						if (p == null || (num.getMinor() < 0 && p.getType() == TVProgram.TYPE_ATV)){
							TVChannelParams cp = getChannelParamsByNo(num.getMajor());
							
							/*check DTV programs in channel of num.getMajor()*/
							TVChannel ch = TVChannel.selectByParams(this, cp);
							if (ch != null){
								TVProgram[] dtvProg = TVProgram.selectByChannel(this, ch.getID(), TVProgram.TYPE_DTV);
								if (dtvProg != null){
									p = dtvProg[0];
								}
							}
						}
					}
					break;
				case TVPlayParams.PLAY_PROGRAM_ID:
					p = TVProgram.selectByID(this, params.getProgramID());
					break;
			}
		}catch(Exception e){
		}

		return p;
	}

	private boolean checkProgramBlock(){
		boolean ret = false;
		TVMessage blockMsg = null;
		TVProgram prog = getCurrentProgram();
		if(prog == null){
			return ret;
		}
		if (! checkBlock){
			programBlocked = false;
			return programBlocked;
		}
		/* is blocked by user lock ? */
		if(prog.getLockFlag()){
			ret = true;
			if (!programBlocked){
				blockMsg = TVMessage.programBlock(programID);
			}
		}
		
		/* is blocked by vchip or parental control ? */		
		if(!ret){
			if(inputSource == TVConst.SourceInput.SOURCE_ATV){
			
			}else if(inputSource == TVConst.SourceInput.SOURCE_DTV){
				TVEvent presentEvent = prog.getPresentEvent(this, time.getTime());
				if (presentEvent != null){
					if(dtvMode == TVChannelParams.MODE_ATSC){
						/* ATSC V-Chip */
						try{
							if(config.getBoolean("tv:vchip:enable")){
								TVDimension.VChipRating[] definedRatings = presentEvent.getVChipRatings();
								for (int i=0; definedRatings!=null && i<definedRatings.length; i++){
									if (TVDimension.isBlocked(this, definedRatings[i])){
										ret = true;
										if (!programBlocked){
											TVDimension dm = TVDimension.selectByIndex(this, 
															definedRatings[i].getRegion(), 
															definedRatings[i].getDimension());
											String dmName = dm.getName();
											String abbrev = dm.getAbbrev(definedRatings[i].getValue());
											String text   = dm.getText(definedRatings[i].getValue());
											Log.d(TAG, "Program blocked by Dimension:'"+dmName+
												"' Abbrev:'"+abbrev+"' Value:'"+text+"'");
										
											blockMsg = TVMessage.programBlock(programID, dmName, abbrev, text);
										}
										break;
									}
								}
							}
						}catch(Exception e){
						}				
					}else{
						/* DVB parental control */
						try{
							int parental_rating_age = config.getInt("tv:dtv:dvb:parent_rate");
							int pr = presentEvent.getDVBViewAge();

							Log.d(TAG,"DVB parental control parental_rating_age="+parental_rating_age+"---pr="+pr);
							
                            if(parental_rating_age!=0 && pr>0 && pr<0x10){
                                pr += 3;
                                int set = parental_rating_age;
                                if(set <= pr){
                                   ret = true;
								   blockMsg = TVMessage.programBlock(programID);
								}  
                            }
								
						}catch(Exception e){
							e.printStackTrace();
						}	
						}
					}else{
						Log.d(TAG, "Present event of playing program not received yet, will unblock this program.");

				}
			}
		}

		if(ret != programBlocked){
			programBlocked = ret;

			if(ret && blockMsg != null){
				Log.d(TAG, "block the program by type "+blockMsg.getProgramBlockType());
				sendMessage(blockMsg);
			}else{
				Log.d(TAG, "unblock the program");
				sendMessage(TVMessage.programUnblock(programID));
			}
		}

		return programBlocked;
	}

	private void playCurrentProgramAV(){
		Log.d(TAG, "try to playCurrentProgramAV");
		
		if(inputSource == TVConst.SourceInput.SOURCE_ATV){
			TVProgram p;
		
			p = playParamsToProgram(atvPlayParams);
			if(p != null){
				synchronized(this){
					programID = p.getID();
					
					try{
						config.set("tv:last_program_id", new TVConfigValue(programID));
					}catch (Exception e){

					}
				}

				if(!checkProgramBlock()){
					Log.d(TAG, "play ATV "+programID);
					device.playATV();
				}

				sendMessage(TVMessage.programStart(programID));

				status = TVRunningStatus.STATUS_PLAY_ATV;
			}
		}else if(inputSource == TVConst.SourceInput.SOURCE_DTV){
			TVProgram p;
			TVProgram.Video video;
			TVProgram.Audio audio;
			int vpid = 0x1fff, apid = 0x1fff, pcr = 0x1fff, vfmt = -1, afmt = -1;

			p = playParamsToProgram(getDTVPlayParams());
			if(p != null){

				synchronized(this){
					programID = p.getID();
					
					try{
						config.set("tv:last_program_id", new TVConfigValue(programID));
					}catch (Exception e){

					}
				}

				/*Scan the program's EPG*/
				epgScanner.enterProgram(programID);

				/*Play the DTV program's AV*/
				video = p.getVideo();

				try{
					/* Get the current audio from this program */
					int aud_idx = p.getCurrentAudio(config.getString("tv:audio:language"));
					if (aud_idx >= 0){
						audio = p.getAudio(aud_idx);
					}else{
						audio = null;
					}
					
					apid = audio.getPID();
					afmt = audio.getFormat();
				}catch(Exception e){
					audio = p.getAudio();
					if(audio != null){
						apid = audio.getPID();
						afmt = audio.getFormat();
					}
				}

				if(video != null){
					vpid = video.getPID();
					vfmt = video.getFormat();
				}

				programVideoPID = vpid;
				programAudioPID = apid;
				pcr = p.getPCRPID();

				if(!checkProgramBlock()){
					Log.d(TAG, "play dtv "+programID+" video "+vpid+" format "+vfmt+" audio "+apid+" format "+afmt+" pcr "+pcr);
					device.playDTV(vpid, vfmt, apid, afmt, pcr);
					switchAudTrack(p.getAudTrack());
				}

				sendMessage(TVMessage.programStart(programID));

				status = TVRunningStatus.STATUS_PLAY_DTV;
			}
		}
	}

	private void playCurrentProgram(){
		TVProgram p = null;
		TVChannelParams fe_params;

		Log.d(TAG, "try to playCurrentProgram");

		if(inputSource == TVConst.SourceInput.SOURCE_ATV){
			if(atvPlayParams == null){
				status = TVRunningStatus.STATUS_STOPPED;
				return;
			}
			p = playParamsToProgram(atvPlayParams);
		}else if(inputSource == TVConst.SourceInput.SOURCE_DTV){
			TVPlayParams tp = getDTVPlayParams();
			if(tp == null){
				tp = dtvTVPlayParams;
			}
			if(tp == null){
				tp = dtvRadioPlayParams;
			}
			if(tp == null){
				status = TVRunningStatus.STATUS_STOPPED;
				return;
			}
			p = playParamsToProgram(tp);
		}

		if(p == null)
			return;

		fe_params = p.getChannel().getParams();
		if(fe_params.getMode() == TVChannelParams.MODE_QPSK){
			try{
				boolean ub_switch = config.getBoolean("tv:dtv:unicable_switch");
				int user_band = -1;
				int ub_freq= 0;
				
				if(ub_switch){
					user_band = config.getInt("tv:dtv:unicableuseband");
					ub_freq = config.getInt("tv:dtv:unicableuseband" + user_band + "freq");						
				}

				fe_params.tv_satparams.setUnicableParams(user_band, ub_freq);			
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG, "Cannot read dtv sx unicable !!!");
			}		
		}else if(fe_params.isDVBTMode()&&fe_params.getOFDM_Mode()==TVChannelParams.OFDM_MODE_DVBT2){
			//Add for DVBT2
			device.setFrontendProp(43,p.getDvbt2PlpID());
		}else if (fe_params.isISDBTMode()){
			//ISDBT layer setting
			device.setFrontendProp(41, fe_params.getISDBTLayer());
		}
		
		synchronized(this){
			programNum = p.getNumber();
			programType = p.getType();
			channelID = p.getChannel().getID();
		}
		
		if (programNum.isATSCMode()){
			Log.d(TAG, "Program number: "+programNum.getMajor()+"-"+programNum.getMinor());
		}else{
			Log.d(TAG, "Program number: "+programNum.getNumber());
		}

		Log.d(TAG, "try to playCurrentProgram lock");

		if((channelParams == null) || !channelParams.equals(fe_params)){
			channelParams = fe_params;
			channelLocked = false;

			/*Stop the epg scanner.*/
			epgScanner.leaveChannel();

			/*Reset the frontend.*/
			Log.d(TAG, "Set frontend to mode "+fe_params.getMode()+", frequency "+fe_params.getFrequency());
			device.setFrontend(fe_params);
			Log.d(TAG, "Set frontend end");
			status = TVRunningStatus.STATUS_SET_FRONTEND;

			 if(fe_params.isDVBTMode()&&fe_params.getOFDM_Mode()==TVChannelParams.OFDM_MODE_DVBT2){
			//Add for DVBT2
				device.setFrontendProp(43,p.getDvbt2PlpID());
			}else if (fe_params.isISDBTMode()){
				//ISDBT layer setting
				device.setFrontendProp(41, fe_params.getISDBTLayer());
			}
			
			return;
		}

		playCurrentProgramAV();
	}
	

	/*Start scan a specified channel*/
	private void startChannelAnalyzing(int channelNo){
		TVChannelParams cp = getChannelParamsByNo(channelNo);
		if (cp == null || cp.getFrequency() <= 0){
			Log.d(TAG, "Channel "+channelNo+" not exist, cannot analyze this channel!");
			return;
		}
		Log.d(TAG, "Start analyzing DTV in channel "+channelNo+"("+cp.getFrequency()+" Hz)...");
		/*Send scan dtv channel start message.*/
		sendMessage(TVMessage.scanDTVChannelStart(channelNo));
		
		TVScanParams sp = TVScanParams.dtvManualScanParams(0, cp);
		resolveStartScan(sp);
		channelParams = cp;
		status = TVRunningStatus.STATUS_ANALYZE_CHANNEL;
	}

	private void startChannelAnalyzing(TVChannelParams cp){
		if (cp == null || cp.getFrequency() <= 0){
			Log.d(TAG, "Channel not exist, cannot analyze this channel!");
			return;
		}
		Log.d(TAG, "Start analyzing DTV in channel ("+cp.getFrequency()+" Hz)...");
		/*Send scan dtv channel start message.*/
		sendMessage(TVMessage.scanDTVChannelStart(0));

		TVScanParams sp = TVScanParams.dtvManualScanParams(0, cp);
		resolveStartScan(sp);
		channelParams = cp;
		status = TVRunningStatus.STATUS_ANALYZE_CHANNEL;
	}

	/*Stop scanning current channel*/
	private void stopChannelAnalyzing(boolean store){
		Log.d(TAG, "Stop analyzing channel.");
		resolveStopScan(store);
	}

	/*Set a channel to atv, only available for ATSC*/
	private TVProgram setChannelToATV(int channelNo){
		TVChannelParams chanParams = getChannelParamsByNo(channelNo);
		if (chanParams == null || chanParams.getFrequency() <= 0){
			Log.d(TAG, "Channel "+channelNo+" not exist, cannot stay at ATV!");
			return null;
		}
		if (chanParams.getMode() != TVChannelParams.MODE_ANALOG){
			TVChannelParams cp = TVChannelParams.analogParams(chanParams.getFrequency(),0,0,0);
			chanParams = cp;
		}
		TVChannel ch = new TVChannel(this, chanParams);

		return new TVProgram(this, ch.getID(), TVProgram.TYPE_ATV, new TVProgramNumber(channelNo, 0), 2/*special flag*/);
	}
	
	private void replayCurrentChannel(){
		if (channelParams == null)
			return;
		TVProgram validProgram = null;
		TVChannel ch = TVChannel.selectByParams(this, channelParams);
		if (ch != null){
			TVProgram[] progInChan = TVProgram.selectByChannel(this, ch.getID(), TVProgram.TYPE_DTV);
			if (progInChan == null && channelParams.isATSCMode()){
				/*Has ATV Program in this channel?*/
				progInChan = TVProgram.selectByChannel(this, ch.getID(), TVProgram.TYPE_ATV);		
			}
			if (progInChan != null)
				validProgram = progInChan[0];
		}
		if (validProgram == null && channelParams.isATSCMode()){
			/*stay at the analog mode*/
			validProgram = setChannelToATV(getChannelNoByParams(channelParams));
		} 
		if (validProgram == null){
			Log.d(TAG, "Cannot get any valid program in this channel, cannot replay");
			return;
		}
		
		channelParams = null;

		/* update the current dtv program type */
		if (validProgram.getType() != dtvProgramType)
			dtvProgramType = validProgram.getType();

		/*play this validProgram*/
		resolvePlayProgram(TVPlayParams.playProgramByID(validProgram.getID()));
	}

	/*Reset the input source.*/
	private void resolveSetInputSource(TVConst.SourceInput src){
		Log.d(TAG, "try to set input source to "+src.name());

		if((src == reqInputSource) && (src == device.getCurInputSource()))
			return;

		reqInputSource = src;

		if((src == TVConst.SourceInput.SOURCE_ATV) || (src == TVConst.SourceInput.SOURCE_DTV)){
			lastTVSource = src;
		}

		if(isInTVMode()){
			stopPlaying();
			stopRecording();
			stopScan(false);
			channelParams = null;
		}

		device.setInputSource(src);

		status = TVRunningStatus.STATUS_SET_INPUT_SOURCE;
	}

	/*Reset the program's type.*/
	private void resolveSetProgramType(int type){
		TVPlayParams tp;

		if((inputSource == TVConst.SourceInput.SOURCE_DTV) && (type == dtvProgramType))
			return;

		dtvProgramType = type;

		if(inputSource != TVConst.SourceInput.SOURCE_DTV){
			return;
		}

		tp = getDTVPlayParams();
		resolvePlayProgram(tp);
	}

	/*Play a program.*/
	private void resolvePlayProgram(TVPlayParams tp){
		Log.d(TAG, "try to play program");

		if (tp == null)
			return;
		
		/*Translate the channel up/channel down parameters*/
		int playType = tp.getType();

		if((playType == TVPlayParams.PLAY_PROGRAM_UP) || (playType == TVPlayParams.PLAY_PROGRAM_DOWN)){
			int type = getCurProgramType();
			TVProgramNumber num = programNum;
			TVProgram p;

			if(num == null)
				return;

			if(playType == TVPlayParams.PLAY_PROGRAM_UP){
				p = TVProgram.selectUp(this, type, num);
			}else{
				p = TVProgram.selectDown(this, type, num);
			}

			if(p == null)
				return;

			tp = TVPlayParams.playProgramByNumber(p.getNumber());
		}

		/*Get the program*/
		TVProgram prog = playParamsToProgram(tp);
		if(prog == null){

			if (isAtvEnabled && playType == TVPlayParams.PLAY_PROGRAM_NUMBER){
				try{
					TVProgramNumber num = tp.getProgramNumber();
					if (num != null && num.isATSCMode()){
						if (num.getMinor() < 0){
							startChannelAnalyzing(num.getMajor()); 
						}else if (num.getMinor() == 0 && prog == null){
							prog = setChannelToATV(num.getMajor());
						}
					}
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		}
		
		if (prog == null){
			Log.d(TAG, "Cannot get the program to play.");
			return;
		}

		TVChannel chan = prog.getChannel();
		if(chan == null || chan.getParams() == null){
			Log.d(TAG, "Cannot get the channel to play.");
			return;
		}
			
		if((channelParams != null) && !channelParams.equals(chan.getParams())){
			if (recorder.isRecording()){
				/*Channel will change, but currently is recording,
				 *let the client to make the choice*/
				Log.d(TAG, "Switch channel while in recording.");
				sendMessage(TVMessage.recordConflict(
					TVMessage.REC_CFLT_SWITCH_PROGRAM,
					prog.getID()));
				return;
			}
		}
		
		/*Stop playing*/
		if(prog.getID() != programID){
			stopPlaying();
		}

		/*Send program switch message.*/
		sendMessage(TVMessage.programSwitch(prog.getID()));
			
		/*Re-enable the block check if needed*/
		try{
			if(config.getBoolean("tv:always_check_program_block")){
				checkBlock = true;
			}
		}catch(Exception e){
		}
		programBlocked = false;

		/*Check if the input source needed reset.*/
		try{
			if(config.getBoolean("tv:mix_atv_dtv")){
				if(chan.isAnalogMode() && (inputSource == TVConst.SourceInput.SOURCE_DTV)){
					atvPlayParams = tp;
					resolveSetInputSource(TVConst.SourceInput.SOURCE_ATV);
					return;
				}else if(!chan.isAnalogMode() && (inputSource == TVConst.SourceInput.SOURCE_ATV)){
					setDTVPlayParams(tp);
					resolveSetInputSource(TVConst.SourceInput.SOURCE_DTV);
					return;
				}
			}
		}catch(Exception e){
		}

		/*Try to play the program.*/
		if(chan.isAnalogMode() && (inputSource == TVConst.SourceInput.SOURCE_ATV)){
			atvPlayParams = tp;
			playCurrentProgram();
		}else if(!chan.isAnalogMode() && (inputSource == TVConst.SourceInput.SOURCE_DTV)){
			setDTVPlayParams(tp);
			playCurrentProgram();
		}else{
			if(chan.isAnalogMode())
				atvPlayParams = tp;
			else
				setDTVPlayParams(tp);
		}
	}

	/*Stop playing.*/
	private void resolveStopPlaying(){
		if(!isInTVMode())
			return;

		stopPlaying();
	}

	/*Start timeshifting.*/
	private void resolveStartTimeshifting(){
		if(!isInTVMode())
			return;
			
		recordCurrentProgram(0, true);
	}
	
	/*Stop timeshifting.*/
	private void resolveStopTimeshifting(){
		if(!isInTVMode())
			return;
			
		/* Stop record */
		stopRecording();
		/* Stop playback */
		stopPlaying();
	}

	/*Start DVR playback.*/
	private void resolveStartPlayback(String filePath){
		if(!isInTVMode())
			return;

		DTVPlaybackParams dtp = new DTVPlaybackParams(filePath, 0/*read it from file*/);
		/* Stop current play */
		stopPlaying();
		/* Start the playback */
		device.startPlayback(dtp);
		status = TVRunningStatus.STATUS_PLAYBACK;
	}

	/*Stop DVR playback.*/
	private void resolveStopPlayback(){
		if(!isInTVMode())
			return;
		
		stopPlaying();
	}
	
	/*Start channel scanning.*/
	private void resolveStartScan(TVScanParams sp){
		Log.d(TAG, "resolveStartScan");

		/*if(!isInTVMode())
			return;
		*/
	
		/** Configure scan */
		TVScanner.TVScannerParams tsp = new TVScanner.TVScannerParams(sp);
		/** Set params from config */
		try {
		    int vidstd = config.getInt("tv:scan:atv:vidstd");
		    int audstd = config.getInt("tv:scan:atv:audstd");
		    
		    if(vidstd < CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_AUTO.ordinal() || 
		            vidstd > CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_SECAM.ordinal()){
		        Log.e(TAG,"vidstd is error");
		    }
		    if(audstd < CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK.ordinal() || 
                    audstd > CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_AUTO.ordinal()){
                Log.e(TAG,"audstd is error");
            }
		        
			int std = TVChannelParams.getTunerStd(vidstd, audstd);
			Log.v(TAG,"std = "+std);

			int chanID = -1;
			if (sp.getTvMode() == TVScanParams.TV_MODE_ATV &&
				sp.getAtvMode() == TVScanParams.ATV_MODE_MANUAL){
				if (sp.getAtvStartFreq() <= 0){
					TVProgram p = null;
					/** Scan from the current playing channel */
					if(atvPlayParams != null){
						p = playParamsToProgram(atvPlayParams);
						if (p != null){
							chanID = p.getChannel().getID();
						}
					}

					if(chanID < 0){
						Log.d(TAG, "Cannot get current channel for ATV manual scan!");
						return;
					}
					tsp.setAtvStartFreq(p.getChannel().getParams().frequency);
				}
				else
				    tsp.setAtvStartFreq(sp.getAtvStartFreq());
			}
            
		    tsp.setAtvParams(config.getInt("tv:scan:atv:minfreq"), 
	                config.getInt("tv:scan:atv:maxfreq"), std, sp.getAtvChannelID());
	       
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot read atv config !!!");
			return;
		}
		
		TVChannelParams[] channelList = null;
		if (sp.getTvMode() != TVScanParams.TV_MODE_ATV &&
			sp.getDtvMode() == TVScanParams.DTV_MODE_ALLBAND) {

			
			TVRegion rg = getCurrentRegion();

			channelList= sp.getCnannelChooseList();
			if (channelList == null && sp.getTsSourceID() != TVChannelParams.MODE_QPSK){
				channelList = rg.getChannelParams();
			}
		} 

		if(sp.getTsSourceID() == TVChannelParams.MODE_QPSK){
			try{
				boolean ub_switch = config.getBoolean("tv:dtv:unicable_switch");
				int user_band = -1;
				int ub_freq= 0;
				
				if(ub_switch){
					user_band = config.getInt("tv:dtv:unicableuseband");
					ub_freq = config.getInt("tv:dtv:unicableuseband" + user_band + "freq");						
				}

				tsp.setDtvSxUnicableParams(user_band, ub_freq);			
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG, "Cannot read dtv sx unicable !!!");
			}		
		}		

		/* get dtv scan params from config */
		boolean resort, clear, mixTvRadio;
		try{
			resort     = config.getBoolean("tv:scan:dtv:resort_all_programs");
			clear      = config.getBoolean("tv:scan:dtv:clear_source");
			mixTvRadio = config.getBoolean("tv:dtv:mix_tv_radio");
		}catch(Exception e){
			e.printStackTrace();
			Log.d(TAG, "Cannot read DTV scan params config !");
			resort     = false;
			clear      = false;
			mixTvRadio = false;
		}
		
		tsp.setDtvParams(0, channelList, resort, clear, mixTvRadio);

		/** No exceptions, start scan */
		stopPlaying();
		stopRecording();
		/*Stop the epg scanner.*/
		epgScanner.leaveChannel();
		stopScan(false);

		channelParams = null;
		synchronized(this){
			device.freeFrontend();
			isScanning = true;
		}

		scanner.scan(tsp);
		status = TVRunningStatus.STATUS_SCAN;
	}

	/*Stop scanning process.*/
	private void resolveStopScan(boolean store){
		/*if(!isInTVMode())
			return;
			*/

		stopScan(store);

		synchronized(this){
			isScanning = false;
		}
		//temp cancel playCurrentProgram();
		//playCurrentProgram();
	}

	/*Start recording.*/
	private void resolveStartRecording(long duration){
		recordCurrentProgram(duration, false);
	}

	/*Stop recording.*/
	private void resolveStopRecording(){
		stopRecording();
	}
	
	/*Pause.*/
	private void resolvePause(){
		if(!isInFileMode())
			return;

		device.pause();
	}

	/*Resume.*/
	private void resolveResume(){
		if(!isInFileMode())
			return;

		device.resume();
	}

	/*Fast forward.*/
	private void resolveFastForward(int speed){
		if(!isInFileMode())
			return;

		device.fastForward(speed);
	}

	/*Fast backward.*/
	private void resolveFastBackward(int speed){
		if(!isInFileMode())
			return;

		device.fastBackward(speed);
	}

	/*Seek to a new position.*/
	private void resolveSeekTo(int pos){
		if(!isInFileMode())
			return;

		device.seekTo(pos);
	}

	/*Solve the events from the device.*/
	private void resolveDeviceEvent(TVDevice.Event event){
		TVConst.SourceInput source;

		switch(event.type){
			case TVDevice.Event.EVENT_SET_INPUT_SOURCE_OK:
				source = TVConst.SourceInput.values()[event.source];

				Log.d(TAG, "set input source to "+source.name()+" ok");
				if(source == reqInputSource){
					inputSource = reqInputSource;
					if(isInTVMode()){
						if(status == TVRunningStatus.STATUS_SET_INPUT_SOURCE) {
							TVProgram p = null;

							if(inputSource == TVConst.SourceInput.SOURCE_DTV){
								if(dtvTVPlayParams == null && dtvRadioPlayParams == null){
									/*Get a valid program*/
									p = getValidProgram(TVProgram.TYPE_DTV);
									if(p != null){
										if(p.getType() == TVProgram.TYPE_TV)
											dtvTVPlayParams = TVPlayParams.playProgramByNumber(p.getNumber());
										else
											dtvRadioPlayParams = TVPlayParams.playProgramByNumber(p.getNumber());

										if (p.getType() != dtvProgramType)
											dtvProgramType = p.getType();
									}
								}
							}else if(inputSource == TVConst.SourceInput.SOURCE_ATV){
								if(atvPlayParams == null){
									/*Get a valid program*/
									p = getValidProgram(TVProgram.TYPE_ATV);
									if(p != null)
										atvPlayParams = TVPlayParams.playProgramByNumber(p.getNumber());
								}
							}

	                        if(inputSource == TVConst.SourceInput.SOURCE_DTV)
						           playCurrentProgram();  	/*Play program*/
                            if(firstPlayAtv == true && inputSource == TVConst.SourceInput.SOURCE_ATV){
						           playCurrentProgram();  
                                   firstPlayAtv  = false;
                            }
						}
					}
					//else
	              //      device.freeFrontend();

					//
					/*Set to config*/
					setInputSourceToConfig();   
				}else{
				    Log.v(TAG, "source: " + source + " TVConst.SourceInput.SOURCE_MPEG: " + TVConst.SourceInput.SOURCE_MPEG);
				    reqInputSource = source;
                    inputSource = reqInputSource;
				    if(source == TVConst.SourceInput.SOURCE_MPEG){
	                    
	                     device.freeFrontend();
	                 }else
	                 if(source == TVConst.SourceInput.SOURCE_ATV){
	                    /*Get a valid program*/
	                    TVPlayParams playParams = null;
	                    TVProgram p = null;
	                    TVChannelParams fe_params = null;
	                    if(atvPlayParams == null){
	                        p = getValidProgram(TVProgram.TYPE_ATV);
	                        if(p != null)
	                            playParams = TVPlayParams.playProgramByNumber(p.getNumber());
	                    }else
	                        playParams = atvPlayParams;
	                    if(playParams != null){
	                        Log.v(TAG, "*Get a valid program");
	                        p = playParamsToProgram(playParams);
	                        fe_params = p.getChannel().getParams();
	                        device.setFrontend(fe_params);
	                        
	                    }
	                 }
				}		
				/*Send message*/
				sendMessage(TVMessage.inputSourceChanged(event.source));
				break;
			case TVDevice.Event.EVENT_SET_INPUT_SOURCE_FAILED:
				source = TVConst.SourceInput.values()[event.source];
				if(source == reqInputSource){
                    inputSource = reqInputSource;
				}
				Log.e(TAG, "set input source to "+source.name()+" failed");
				break;
			case TVDevice.Event.EVENT_FRONTEND:
				if(isInTVMode()){
					if(channelParams!=null && event.feParams.equals_frontendevt(channelParams)){
						if(inputSource == TVConst.SourceInput.SOURCE_DTV){
							/*Start EPG scanner.*/
							if(channelID !=-1){
								epgScanner.enterChannel(channelID);
							}
						}
						if((status == TVRunningStatus.STATUS_SET_FRONTEND)/* && (event.feStatus & TVChannelParams.FE_HAS_LOCK)!=0*/){
							/*Play AV.*/
							playCurrentProgramAV();
							
							/*if a record waiting for this fend event, start this record now*/
							if (recorder.getStatus() == TVRecorder.ST_WAITING_FEND){
								Log.d(TAG, "fend locked, start the pending record...");
								recorder.fendLocked();
							}
						}
						if(!channelLocked && (event.feStatus & TVChannelParams.FE_HAS_LOCK)!=0){
							Log.d(TAG, "signal resume");
							sendMessage(TVMessage.signalResume(channelID));
							channelLocked = true;
							if(inputSource == TVConst.SourceInput.SOURCE_ATV){
								TVProgram prog = getCurrentProgram();
								if (prog != null && prog.getSkip() == 2){
									prog.setSkipFlag(false);
								}
							}
						}else if(channelLocked && (event.feStatus & TVChannelParams.FE_TIMEDOUT)!=0){
							Log.d(TAG, "signal lost");
							sendMessage(TVMessage.signalLost(channelID));
							channelLocked = false;
						}
					}
				}
				break;
			case TVDevice.Event.EVENT_RECORD_END:
				Log.d(TAG, "Record end with error code " + event.recEndCode);
				sendMessage(TVMessage.recordEnd(event.recEndCode));
				break;
			case TVDevice.Event.EVENT_VGA_ADJUST_STATUS:
				Log.d(TAG, "VGA adjust");
				if(event.vga_adjust_status == TVConst.VGA_ADJUST_STATUS.CC_TV_VGA_ADJUST_FAILED){
					sendMessage(new TVMessage(TVMessage.TYPE_VGA_ADJUST_FAILED));
				}else if(event.vga_adjust_status == TVConst.VGA_ADJUST_STATUS.CC_TV_VGA_ADJUST_SUCCESS){
					sendMessage(new TVMessage(TVMessage.TYPE_VGA_ADJUST_OK));
				}else if(event.vga_adjust_status == TVConst.VGA_ADJUST_STATUS.CC_TV_VGA_ADJUST_DOING){
					sendMessage(new TVMessage(TVMessage.TYPE_VGA_ADJUST_DOING));
				}
				break;
			 case TVDevice.Event.EVENT_SIG_CHANGE:
			     sendMessage(TVMessage.sigChange( event.tvin_info));
			     break;
			case TVDevice.Event.EVENT_PLAYBACK_START:
				sendMessage(TVMessage.playbackStart(event.recParams));
				break;
			case TVDevice.Event.EVENT_PLAYBACK_END:
				sendMessage(TVMessage.playbackStop());
				break;
			case TVDevice.Event.EVENT_DTV_NO_DATA:
				Log.d(TAG, "DTV av data lost.");
				sendMessage(TVMessage.dataLost(programID));
				break;
			case TVDevice.Event.EVENT_DTV_CANNOT_DESCRAMLE:
				Log.d(TAG, "DTV av data scrambled.");
				sendMessage(TVMessage.programScrambled(programID));
				break;
			case TVDevice.Event.EVENT_DTV_DATA_RESUME:
				Log.d(TAG, "DTV av data resumed.");
				sendMessage(TVMessage.dataResume(programID));
				break;
		}
	}

	/*Solve the events from the EPG scanner.*/
	private void resolveEpgEvent(TVEpgScanner.Event event){
		switch(event.type){
			case TVEpgScanner.Event.EVENT_PROGRAM_AV_UPDATE:
				Log.d(TAG, "Detect program "+event.programID+"'s AV changed, try a replay now...");
				playCurrentProgramAV();
				break;
			case TVEpgScanner.Event.EVENT_PROGRAM_NAME_UPDATE:
				Log.d(TAG, "Detect program "+event.programID+"'s name changed, send msg to clients...");
				sendMessage(TVMessage.programUpdate(event.programID));
				break;
			case TVEpgScanner.Event.EVENT_PROGRAM_EVENTS_UPDATE:
				Log.d(TAG, "Detect EPG events have updates, send msg to clients...");
				sendMessage(TVMessage.eventUpdate());
				break;
			case TVEpgScanner.Event.EVENT_CHANNEL_UPDATE:
				Log.d(TAG, "Detect channel "+event.channelID+" update, try a re-scan now...");
				if (event.channelID == channelID){
					if(channelParams.isATSCMode())
						startChannelAnalyzing(getChannelNoByParams(channelParams));
					else
						startChannelAnalyzing(channelParams);
				}
				break;
			default:
				break;
		}
	}

	/*Solve the events from the channel scanner.*/
	private void resolveScanEvent(TVScanner.Event event){
		Log.d(TAG, "Channel scan event: " + event.type);

		if (status == TVRunningStatus.STATUS_SCAN){
			switch (event.type) {
				case TVScanner.Event.EVENT_SCAN_PROGRESS:
					Log.d(TAG, "Progress: " + event.percent + "%" + ", channel no. "+event.channelNumber);
					String name = null;
					
					if (event.programName != null) {
						try{
							String composedName = new String(event.programName, "UTF-8");
							name = TVMultilingualText.getText(this, composedName);
							if (name == null || name.isEmpty()){
								name = TVMultilingualText.getText(this, composedName, "first");
							}
							Log.d(TAG, "New Program : "+ name + ", type "+ event.programType);
						}catch (Exception e){
							e.printStackTrace();
						}
					}
					sendMessage(TVMessage.scanUpdate(event.percent, event.channelNumber, event.totalChannelCount, 
						event.channelParams, event.lockedStatus, name, event.programType));
					break;
				case TVScanner.Event.EVENT_STORE_BEGIN:
					Log.d(TAG, "Store begin...");
					sendMessage(TVMessage.scanStoreBegin());
					break;
				case TVScanner.Event.EVENT_STORE_END:
					Log.d(TAG, "Store end");
					sendMessage(TVMessage.scanStoreEnd());
					break;
				case TVScanner.Event.EVENT_SCAN_END:
					Log.d(TAG, "Scan end");
					sendMessage(TVMessage.scanEnd());
					break;
				case TVScanner.Event.EVENT_BLINDSCAN_PROGRESS:
					Log.d(TAG, "Blind Scan Progress");
					sendMessage(TVMessage.blindScanProgressUpdate(event.percent, event.msg));
					break;
				case TVScanner.Event.EVENT_BLINDSCAN_NEWCHANNEL:
					Log.d(TAG, "Blind Scan New Channel");
					sendMessage(TVMessage.blindScanNewChannelUpdate(event.channelParams));
					break;
				case TVScanner.Event.EVENT_BLINDSCAN_END:
					Log.d(TAG, "Blind Scan end");
					sendMessage(TVMessage.blindScanEnd());
					break;						
				default:
					break;
					
			}
		}else if (status == TVRunningStatus.STATUS_ANALYZE_CHANNEL){
			switch (event.type) {
				case TVScanner.Event.EVENT_SCAN_END:
					stopChannelAnalyzing(true);
					Log.d(TAG, "Channel analyzing end, try replay any program in this channel...");
					replayCurrentChannel();
					break;
				default:
					break;
			}
		}
	}
	
	/*Solve the events from the book manager.*/
	private void resolveBookEvent(TVBookManager.Event event){
		switch(event.type){
			case TVBookManager.Event.EVENT_NEW_BOOKING_CONFIRM:
				Log.d(TAG, "New booking confirm");
				sendMessage(TVMessage.bookingRemind(event.bookingID));
				break;
			case TVBookManager.Event.EVENT_NEW_BOOKING_START:
				Log.d(TAG, "New booking start");
				TVBooking booking = TVBooking.selectByID(this, event.bookingID);
				if (booking != null){
					sendMessage(TVMessage.bookingStart(event.bookingID));
					startBooking(booking);
				} else {
					Log.d(TAG, "Cannot get booking "+event.bookingID);
				}
				
				break;
			default:
				break;
		}
	}

	/*Invoked when configure value changed.*/
	private void resolveConfigChanged(String name){
		try{
			if(name.equals("tv:audio:language")){
				String lang = config.getString("tv:audio:language");

				Log.d(TAG, "tv:audio:language changed -> "+lang);

				/*if(inputSource == TVConst.SourceInput.SOURCE_DTV){
					if(status == TVRunningStatus.STATUS_PLAY_DTV){
						TVProgram p = TVProgram.selectByID(this, programID);
						if(p != null){
							TVProgram.Audio audio;
							int apid, afmt;

							audio = p.getAudio(lang);
							if(audio != null){
								apid = audio.getPID();
								afmt = audio.getFormat();

								if(!checkProgramBlock() && (apid != programAudioPID)){
									device.switchDTVAudio(apid, afmt);
									programAudioPID = apid;
								}
							}
						}
					}
				}*/
			}else if(name.equals("tv:vchip:enable")){
				resolveReplay(true);
			}else if(name.equals("tv:dtv:record_storage_path")){
				String path = config.getString("tv:dtv:record_storage_path");

				Log.d(TAG, "tv:dtv:record_storage_path -> "+path);

				recorder.setStorage(path);
			}else if (name.equals("tv:dtv:mode")){
				String strMode = config.getString("tv:dtv:mode");

				Log.d(TAG, "tv:dtv:mode changed -> "+strMode);
				dtvMode = TVChannelParams.getModeFromString(strMode);
				if(dtvMode >= 0){
					/* restart epg */
					String orderedTextLangs = config.getString("tv:scan:dtv:ordered_text_languages");
					epgScanner.setSource(0, 0, dtvMode, orderedTextLangs);

					/* reset the last played program id */
					config.set("tv:last_program_id", new TVConfigValue(-1));
				}
			}
		}catch(Exception e){
		}
	}

	/*Switch audio*/
	private void resolveSwitchAudio(int id){
		if(inputSource == TVConst.SourceInput.SOURCE_DTV){
			TVProgram p = null;
			if(status == TVRunningStatus.STATUS_PLAY_DTV){
				p = TVProgram.selectByID(this, programID);
			}else if (status == TVRunningStatus.STATUS_TIMESHIFTING){
				try{
					p = TVProgram.selectByID(this, config.getInt("tv:last_program_id"));
				}catch(Exception e){

				}
			}else if (status == TVRunningStatus.STATUS_PLAYBACK){
				TVProgram[] progs = TVProgram.selectByType(this, TVProgram.TYPE_PLAYBACK, false);
				if (progs != null){
					p = progs[0];
				}
			}
			
			if(p != null){
				TVProgram.Audio audio;
				int apid, afmt;

				audio = p.getAudio(id);
				if(audio != null){
					apid = audio.getPID();
					afmt = audio.getFormat();

					p.setCurrentAudio(id);

					if(!checkProgramBlock() && (apid != programAudioPID)){
						device.switchDTVAudio(apid, afmt);
						programAudioPID = apid;
					}
				}
			}
		}
	}

	/*Reset ATV format*/
	private void resolveResetATVFormat(){
		if(inputSource != TVConst.SourceInput.SOURCE_ATV)
			return;

		TVProgram p = TVProgram.selectByID(this, programID);
		if(p == null)
			return;

		TVChannel chan = p.getChannel();
		if(chan == null)
			return;

		TVChannelParams params = chan.getParams();
		if(params == null)
			return;

		device.resetATVFormat(params);
	}

	/*Fine tune*/
	private void resolveFineTune(int freq){

		TVProgram p = TVProgram.selectByID(this, programID);
		if(p == null)
			return;

		TVChannel chan = p.getChannel();
		if(chan == null)
			return;

		if(chan.isAnalogMode()){
			device.ATVChannelFineTune(freq);
		}
	}
    
    	/*cvbs out*/
	private void resolveSetCvbsAmpOut(int amp_out){

		TVProgram p = TVProgram.selectByID(this, programID);
		if(p == null)
			return;

		TVChannel chan = p.getChannel();
		if(chan == null)
			return;

		if(chan.isAnalogMode()){
			device.SetCvbsAmpOut(amp_out);
		}
	}

	/*Restore factory setting*/
	private void resolveRestoreFactorySetting(int flags){
		stopPlaying();
		stopScan(false);
		stopRecording();

		atvPlayParams = null;
		dtvTVPlayParams = null;
		dtvRadioPlayParams = null;

		synchronized(this){
			channelID  = -1;
			programID  = -1;
			programNum = null;
			channelParams = null;
		}
		
		/*restore database*/
		if ((flags & RESTORE_FL_DATABASE) != 0){
			TVDataProvider.restore(this);
		}
		/*restore config*/
		if ((flags & RESTORE_FL_CONFIG) != 0){
			config.restore();
			setInputSourceToConfig();  
		}
		
		/**/
	}
	
	/*Play a program.*/
	private void resolvePlayValid(){
		TVProgram p = getCurrentProgram();

		Log.d(TAG, "Try to play a valid program...");
		if (p == null){
			int id = -1;
			int progType = TVProgram.TYPE_DTV;
			
			Log.d(TAG, "Cannot play last played program, try first valid program.");

			if((inputSource == TVConst.SourceInput.SOURCE_ATV))
				progType = TVProgram.TYPE_ATV;
			TVProgram fvp = getValidProgram(progType);
			if (fvp != null){
				id = fvp.getID();

				/* update the current dtv program type */
				if (fvp.getType() != dtvProgramType)
					dtvProgramType = fvp.getType();
			}else{
				if(dtvMode == TVChannelParams.MODE_ATSC){
					TVRegion curReg = getCurrentRegion();
					if (curReg != null){
						TVChannelParams para = curReg.getChannelParams(3);
						if (para != null){
							/*new add a DTV & ATV program, then try play*/
							Log.d(TAG, "Creating analog program 3-0 in channel 3...");
							TVChannelParams param = TVChannelParams.analogParams(para.getFrequency(), 0, 0, 0); 
							TVChannel newChan = new TVChannel(this, param);
							TVProgram newProg = new TVProgram(this, newChan.getID(), TVProgram.TYPE_ATV,
								new TVProgramNumber(3, 0), 0);							
							Log.d(TAG, "Creating digital program 3-1 in channel 3...");
							try{								
								String strAntenna = config.getString("tv:atsc:antenna:source");
								if (strAntenna.equals("cable")){
									param = TVChannelParams.atscParams(para.getFrequency(), TVChannelParams.MODULATION_QAM_256);
								}else{
									param = TVChannelParams.atscParams(para.getFrequency(), TVChannelParams.MODULATION_VSB_8);
								}
								newChan = new TVChannel(this, param);
								newProg = new TVProgram(this, newChan.getID(), TVProgram.TYPE_TV,
										new TVProgramNumber(3, 1), 0);
								id = newProg.getID();
							}catch (Exception e){
								e.printStackTrace();
							}
						}
					}
				}
			}
			
			if (id != -1){
				TVPlayParams tp = TVPlayParams.playProgramByID(id);
				resolvePlayProgram(tp);
			}
		}else if (status == TVRunningStatus.STATUS_PLAY_ATV || status == TVRunningStatus.STATUS_PLAY_DTV){
			Log.d(TAG, "TV already playing.");
		}else{
			Log.d(TAG, "Play last program");
			playCurrentProgram();
		}
	}

	/*VGA auto adjust.*/
	private void resolveSetVGAAutoAdjust(){
		device.setVGAAutoAdjust();
	}

	/*If the program block status changed, replay current playing program*/
	private void resolveReplay(boolean forceCheckBlock){
		if(status != TVRunningStatus.STATUS_PLAY_ATV && 
		   status != TVRunningStatus.STATUS_PLAY_DTV){
			return;
		}
		
		boolean prevBlock = programBlocked;
		/*when user changed the rating, a force block checking is needed*/
		if (forceCheckBlock){
			checkBlock = true;
		}
		checkProgramBlock();
		if (prevBlock != programBlocked){
			Log.d(TAG, "Program block changed from "+
				(prevBlock      ? "blocked" : "unblocked")+" to "+
				(programBlocked ? "blocked" : "unblocked"));
			if (!programBlocked){
				/*unblocked, perform replay*/
				playCurrentProgramAV();
			}else{
				/*blocked, stop playing*/
				if(status == TVRunningStatus.STATUS_PLAY_ATV){
					device.stopATV();
				} else if(status == TVRunningStatus.STATUS_PLAY_DTV){
					device.stopDTV();
				}
			}
		}
	}
	
	/*Unblock the current blocking program*/
	private void resolveUnblock(){
		if(programBlocked && 
		   (status == TVRunningStatus.STATUS_PLAY_ATV) || 
		   (status == TVRunningStatus.STATUS_PLAY_DTV)){
			checkBlock = false;
			playCurrentProgramAV();
			sendMessage(TVMessage.programUnblock(programID));
		}
	}

	/*lock.*/
	private void resolveLock(TVChannelParams curParams){
		Log.d(TAG, "try to lock");		

		device.setFrontend(curParams);
	}

	/*sec.*/
	private void resolveSec(TVMessage sec_msg){
		Log.d(TAG, "try to sec");

		int sectype = sec_msg.getType();
		TVChannelParams seccurparams = sec_msg.getSecCurChanParams();


		if((sectype == TVMessage.TYPE_SEC_LNBSSWITCHCFGVALID)
			|| (sectype == TVMessage.TYPE_SEC_POSITIONEREAST)
			|| (sectype == TVMessage.TYPE_SEC_POSITIONERWEST)
			|| (sectype == TVMessage.TYPE_SEC_POSITIONERSTORE)
			|| (sectype == TVMessage.TYPE_SEC_POSITIONERGOTO)
			|| (sectype == TVMessage.TYPE_SEC_POSITIONERGOTOX)){
				try{
					boolean ub_switch = config.getBoolean("tv:dtv:unicable_switch");
					int user_band = -1;
					int ub_freq= 0;
					
					if(ub_switch){
						user_band = config.getInt("tv:dtv:unicableuseband");
						ub_freq = config.getInt("tv:dtv:unicableuseband" + user_band + "freq");						
					}
	
					seccurparams.tv_satparams.setUnicableParams(user_band, ub_freq);			
				} catch (Exception e) {
					e.printStackTrace();
					Log.d(TAG, "Cannot read dtv sx unicable !!!");
				}				
		}		

		device.setSecRequest(sectype, seccurparams, sec_msg.getSecPositionerMoveUnit());
	}

	public void dynamicConfigFeAndDmx(){
		String valuefe = null;
		String valuedmx = null;

		Log.d(TAG, "dynamicConfigFeAndDmx !!!");

		try{
			valuefe = config.getString("tv:dtv:configfe");
			Log.d(TAG, "valuefe = " + valuefe);
			
			valuedmx = config.getString("tv:dtv:configdmx");
			Log.d(TAG, "valuedmx = " + valuedmx);
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot read dynamic info !!!");
		}			

		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/dmx/aml_dmx_dynamic_config"));
			try {
				writer.write(valuedmx);
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot config dmx !!!");
		}	

		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/amlfe/aml_fe_dynamic_config"));
			try {
				writer.write(valuefe);
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot config fe !!!");
		}

		int i = 0;
		do{
			SystemClock.sleep(200);

			i++;

			if(i == 10)
				break;
			
			File file = new File("/dev/dvb0.frontend0");

			if(file.exists())
				break;
		}while(true);

	}	


	public void dynamicConfigDemodAndFe(){
		String valuefe = null;
		String valuedmx = null;

		Log.d(TAG, "dynamicConfigFeAndDmx !!!");

		try{
			valuefe = config.getString("tv:dtv:config_demod_fe");
			Log.d(TAG, "valuefe = " + valuefe);
			
			valuedmx = config.getString("tv:dtv:config_dmx");
			Log.d(TAG, "valuedmx = " + valuedmx);
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot read dynamic info !!!");
		}			
		
		String[] valuefe_array =valuefe.split("\\|");

		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/amlfe/setting"));
			try {			
					writer.write("disable 0");
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot config fe !!!");
		}

		for(int i=0;i<valuefe_array.length;i++){
			try{
				BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/amlfe/setting"));
				try {
					
						writer.write(valuefe_array[i]);
					
				} finally {
					writer.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG, "Cannot config fe !!!");
			}
		}

		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/stb/hw_setting"));
			try {
				writer.write(valuedmx);
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot config dmx !!!");
		}	
	}


	public IBinder onBind (Intent intent){
		return mBinder;
	}

	public void onCreate(){
		super.onCreate();	

		config = new TVConfig(this);

		//dynamicConfigFeAndDmx();
		dynamicConfigDemodAndFe();
		
		device = new TVDeviceImpl( this.getMainLooper()){
			/*Device event handler*/
			public void onEvent(TVDevice.Event event){
				/* async processing */
				if (event.type == TVDevice.Event.EVENT_RECORD_END){
					recorder.onRecordEvent(event);
				}

				/* sync processing */
				Message msg = handler.obtainMessage(MSG_DEVICE_EVENT, event);
				handler.sendMessage(msg);
			}
		};
		
		recorder.open(device);

		TVDataProvider.openDatabase(this, config);
	
		bookManager.open(time, config);

		try{
			/* Set the diff time stored in config, to be more accurate when TDT/STT not received */
			time.setDiffTime(Long.parseLong(config.getString("tv:time:diff_value")));
			/* Set the last selected PVR storage path */
			recorder.setStorage(config.getString("tv:dtv:record_storage_path"));
			/* Start EPG scanner */
			String modeStr = config.getString("tv:dtv:mode");
			dtvMode = TVChannelParams.getModeFromString(modeStr);
			if(dtvMode == -1)
				throw new Exception();
			String orderedTextLangs = config.getString("tv:scan:dtv:ordered_text_languages");
			epgScanner.setSource(0, 0, dtvMode, orderedTextLangs);
			
			epgScanner.setDvbTextCoding(config.getString("tv:dtv:dvb_text_coding"));
			
			isAtvEnabled = config.getBoolean("tv:atv:enable");

			/* Update current input source to config */
			inputSource = device.getCurInputSource();
			setInputSourceToConfig();   
			
			config.registerUpdate("tv:audio:language", this);
			config.registerUpdate("tv:dtv:record_storage_path", this);            
            config.registerUpdate("tv:dtv:mode", this);
			config.registerUpdate("setting", device);
			config.registerRead("setting", device);
			boolean lcn = config.getBoolean("tv:dtv:dvbt:lcn");
		}catch(Exception e){
			Log.e(TAG, "intialize config failed");
		}

		/*Start program block check timer*/
		checkBlockHandler.postDelayed(programBlockCheck, 1000);

		/*Start data sync timer*/
		dataSyncHandler.postDelayed(dataSync, 1000);
        registerServiceBroadcast();
	}
	
	public void onUpdate(String name, TVConfigValue value){
		try{
			Message msg = handler.obtainMessage(MSG_CONFIG_CHANGED, name);
			handler.sendMessage(msg);
		}catch(Exception e){
		}
	}

	public void onDestroy(){
		this.unregisterReceiver(myServiceReceiver);
		dataSyncHandler.removeCallbacks(dataSync);
		checkBlockHandler.removeCallbacks(programBlockCheck);
		callbacks.kill();
		epgScanner.destroy();
		bookManager.close();
		recorder.close();
		TVDataProvider.closeDatabase(this);
		try{
			device.dispose();
		}catch (Throwable e){
			Log.d(TAG, "Failed to dispose device");
		}
		super.onDestroy();
	}
    
    ServiceReceiver myServiceReceiver = null;
    private void registerServiceBroadcast()
    {
        myServiceReceiver = new ServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceReceiver.StartPlayer);
		filter.addAction(ServiceReceiver.playProgramUp);
		filter.addAction(ServiceReceiver.playProgramDown);
		filter.addAction(ServiceReceiver.SCREEN_OFF);
		filter.addAction(ServiceReceiver.SCREEN_ON);
		filter.addAction(ServiceReceiver.StartPlayDTV);
        this.registerReceiver(myServiceReceiver, filter);
    }
    
    private boolean firstPlayAtv = false;
    public class ServiceReceiver extends BroadcastReceiver {
	static final String TAG = "TvServiceReceiver";
	public static final String  StartPlayer = "com.amlogic.tvservice.startplayer";
       public static final String  playProgramUp = "com.amlogic.tvservice.playProgramUp";
	public static final String  playProgramDown = "com.amlogic.tvservice.playProgramDown";
	public static final String SCREEN_OFF = "android.intent.action.SCREEN_OFF";
	public static final String SCREEN_ON = "android.intent.action.SCREEN_ON";
	public static final String StartPlayDTV = "com.launcher.play.dtv";
	@Override
	public void onReceive(Context context, Intent intent) {
		if (StartPlayer.equals(intent.getAction())) {
  				Log.d(TAG,"TVService Start ATV*******************************************");
                int val = 0;
                TVConst.SourceInput type = TVConst.SourceInput.values()[val];
                resolveSetInputSource(type);
                firstPlayAtv = true;	
		}else
		if (playProgramUp.equals(intent.getAction())) {
  				Log.d(TAG,"playProgramUp******************************************");		
		        resolvePlayProgram(TVPlayParams.playProgramUp());
		}
		else
		if (playProgramDown.equals(intent.getAction())) {
		  	    Log.d(TAG,"playProgramDown*******************************************");
		        resolvePlayProgram(TVPlayParams.playProgramDown());
		}
		else
		if (StartPlayDTV.equals(intent.getAction())) {
				Log.d(TAG,"StartPlayDTV******************************************");
				
				playCurrentProgram();
		}
		
		if(intent.getAction().equals(SCREEN_OFF)) {
				Log.d(TAG,"SCREEN_OFF+++++++++++++");
				channelParams = null;
				TVMessage msg = new TVMessage(TVMessage.TYPE_SCREEN_OFF);
				
				sendMessage(msg);
				
				return;
			}
			if(intent.getAction().equals(SCREEN_ON)) {
				Log.d(TAG,"SCREEN_ON++++++++++++++++++");
				TVMessage msg = new TVMessage(TVMessage.TYPE_SCREEN_ON);
				
				sendMessage(msg);
				return;
			}
			
		}					
	}
	
}

