package com.amlogic.tvservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.os.Message;
import android.os.Looper;
import android.os.Handler;
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
import com.amlogic.tvdataprovider.TVDataProvider;
import android.os.Looper;

public class TVService extends Service implements TVConfig.Update{
	private static final String TAG = "TVService";

	/*Message types*/
	private static final int MSG_SET_SOURCE    = 1949;
	private static final int MSG_PLAY_PROGRAM  = 1950;
	private static final int MSG_STOP_PLAYING  = 1951;
	private static final int MSG_START_TIMESHIFTING = 1952;
	private static final int MSG_START_PLAYBACK  = 1953;
	private static final int MSG_START_SCAN      = 1954;
	private static final int MSG_STOP_SCAN       = 1955;
	private static final int MSG_START_RECORDING = 1956;
	private static final int MSG_STOP_RECORDING  = 1957;
	private static final int MSG_PAUSE           = 1958;
	private static final int MSG_RESUME          = 1959;
	private static final int MSG_FAST_FORWARD    = 1960;
	private static final int MSG_FAST_BACKWARD   = 1961;
	private static final int MSG_SEEK_TO         = 1962;
	private static final int MSG_DEVICE_EVENT    = 1963;
	private static final int MSG_EPG_EVENT       = 1964;
	private static final int MSG_SCAN_EVENT      = 1965;
	private static final int MSG_SET_PROGRAM_TYPE = 1966;
	private static final int MSG_CONFIG_CHANGED   = 1967;
	private static final int MSG_SWITCH_AUDIO     = 1968;
	private static final int MSG_RESET_ATV_FORMAT = 1969;
	private static final int MSG_FINE_TUNE        = 1970;

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

		public void setInputSource(int source){
			Message msg = handler.obtainMessage(MSG_SET_SOURCE, new Integer(source));
			handler.sendMessage(msg);
		}

		public int getCurInputSource(){
			return device.getCurInputSource().ordinal();
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

		public void startRecording(int bookingID){
			Message msg = handler.obtainMessage(MSG_START_RECORDING, new Integer(bookingID));
                        handler.sendMessage(msg);
		}

		public void stopRecording(){
			Message msg = handler.obtainMessage(MSG_STOP_RECORDING);
                        handler.sendMessage(msg);
		}

		public void startPlayback(int bookingID){
			Message msg = handler.obtainMessage(MSG_START_PLAYBACK);
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
				case MSG_START_PLAYBACK:
					resolveStartPlayback((Integer)msg.obj);
					break;
				case MSG_START_SCAN:
					resolveStartScan((TVScanParams)msg.obj);
					break;
				case MSG_STOP_SCAN:
					resolveStopScan((Boolean)msg.obj);
					break;
				case MSG_START_RECORDING:
					resolveStartRecording((Integer)msg.obj);
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
					time.setTime(event.time*1000);
					return;
			}

			Message msg = handler.obtainMessage(MSG_EPG_EVENT, event);
			handler.sendMessage(msg);
		}
	};

	private enum TVRunningStatus{
		STATUS_SET_INPUT_SOURCE,
		STATUS_SET_FRONTEND,
		STATUS_PLAY_ATV,
		STATUS_PLAY_DTV,
		STATUS_TIMESHIFTING,
		STATUS_PLAYBACK,
		STATUS_STOPPED,
		STATUS_SCAN
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
	private TVProgramNumber programNum;
	private boolean programBlocked = false;
	private boolean channelLocked = false;
	private boolean recording = false;
	private boolean isScanning = false;

	private void setDTVPlayParams(TVPlayParams params){
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
		}

		synchronized(this){
			programID = -1;
		}
	}

	private void stopScan(boolean store){
		if(status == TVRunningStatus.STATUS_SCAN){
			scanner.stop(store);
			status = TVRunningStatus.STATUS_STOPPED;

			if(store){
				atvPlayParams = null;
				dtvTVPlayParams = null;
				dtvRadioPlayParams = null;
			}

			synchronized(this){
				channelID  = -1;
				programID  = -1;
				programNum = null;
				channelParams = null;
			}
		}
	}

	private void stopRecording(){
		if(recording){
			TVDevice.DTVRecordParams params;

			params = device.stopRecording();
			recording = false;
		}
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
					int type = getCurProgramType();

					p = TVProgram.selectByNumber(this, type, params.getProgramNumber());
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

		if(inputSource == TVConst.SourceInput.SOURCE_ATV){
		}else if(inputSource == TVConst.SourceInput.SOURCE_DTV){
		}

		if(ret != programBlocked){
			programBlocked = ret;

			if(ret){
				Log.d(TAG, "block the program");
				sendMessage(TVMessage.programBlock(programID));
			}else{
				Log.d(TAG, "unblock the program");
				sendMessage(TVMessage.programUnblock(programID));
			}
		}

		return programBlocked;
	}

	private void playCurrentProgramAV(){
		if(inputSource == TVConst.SourceInput.SOURCE_ATV){
			TVProgram p;
		
			p = playParamsToProgram(atvPlayParams);
			if(p != null){
				synchronized(this){
					programID = p.getID();
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
			int vpid = 0x1fff, apid = 0x1fff, vfmt = -1, afmt = -1;

			p = playParamsToProgram(getDTVPlayParams());
			if(p != null){

				synchronized(this){
					programID = p.getID();
				}

				/*Scan the program's EPG*/
				epgScanner.enterProgram(programID);

				/*Play the DTV program's AV*/
				video = p.getVideo();

				try{
					String lang = config.getString("tv:audio:language");
					audio = p.getAudio(lang);
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

				if(!checkProgramBlock()){
					Log.d(TAG, "play dtv "+programID+" video "+vpid+" format "+vfmt+" audio "+apid+" format "+vfmt);
					device.playDTV(vpid, vfmt, apid, vfmt);
				}

				sendMessage(TVMessage.programStart(programID));

				status = TVRunningStatus.STATUS_PLAY_DTV;
			}
		}
	}

	private void playCurrentProgram(){
		TVProgram p = null;
		TVChannelParams fe_params;

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

		synchronized(this){
			programNum = p.getNumber();
			programType = p.getType();
			channelID = p.getChannel().getID();
		}

		/*Send program number message.*/
		sendMessage(TVMessage.programNumber(getCurProgramType(), programNum));

		fe_params = p.getChannel().getParams();

		if((channelParams == null) || !channelParams.equals(fe_params)){
			channelParams = fe_params;
			channelLocked = false;

			/*Stop the epg scanner.*/
			epgScanner.leaveChannel();

			/*Reset the frontend.*/
			device.setFrontend(fe_params);
			status = TVRunningStatus.STATUS_SET_FRONTEND;
			return;
		}

		playCurrentProgramAV();
	}

	/*Reset the input source.*/
	private void resolveSetInputSource(TVConst.SourceInput src){
		Log.d(TAG, "try to set input source to "+src.name());

		if(src == reqInputSource)
			return;

		reqInputSource = src;

		if((src == TVConst.SourceInput.SOURCE_ATV) || (src == TVConst.SourceInput.SOURCE_DTV)){
			lastTVSource = src;
		}

		if(isInTVMode()){
			stopPlaying();
			stopRecording();
			stopScan(false);
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
		if(prog == null)
			return;

		/*Stop playing*/
		if(prog.getID() != programID){
			stopPlaying();
		}

		TVChannel chan = prog.getChannel();
		if(chan == null)
			return;

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
	}

	/*Start DVR playback.*/
	private void resolveStartPlayback(int bookingID){
		if(!isInTVMode())
			return;
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
		    if(vidstd < CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK.ordinal() || 
                    vidstd > CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_AUTO.ordinal()){
                Log.e(TAG,"audstd is error");
            }
		        

			int std = TVChannelParams.getTunerStd(vidstd, audstd);
            Log.v(TAG,"std = "+std);
             tsp.setAtvParams(config.getInt("tv:scan:atv:minfreq") , config.getInt("tv:scan:atv:maxfreq"), std);


		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Cannot read atv config !!!");
			return;
		}
		
		TVChannelParams[] channelList = null;
		if (sp.getTvMode() != TVScanParams.TV_MODE_ATV &&
			sp.getDtvMode() == TVScanParams.DTV_MODE_ALLBAND) {
			String region;
			/** load the frequency list */
			try {
				region = config.getString("tv:scan:dtv:region");
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG, "Cannot read dtv region !!!");
				return;
			}
			
			Cursor c = this.getContentResolver().query(TVDataProvider.RD_URL, null,
					"select * from region_table where name='" + region + "' and source=" + sp.getTsSourceID(),
					null, null);
			if(c != null){
				if(c.moveToFirst()){
					int col = c.getColumnIndex("frequencies");
					String freqs = c.getString(col);
					
					if (freqs != null && freqs.length() > 0) {
						String[] flist = freqs.split(" ");
						
						if (flist !=null && flist.length > 0) {
							int frequency = 0;
							int bandwidth = 0;

							if(sp.getTsSourceID() == TVChannelParams.MODE_QPSK){
								channelList = new TVChannelParams[flist.length];
								/** get each frequency */
								for (int i=0; i<channelList.length; i++) {
									frequency = Integer.parseInt(flist[i]);
									channelList[i] = TVChannelParams.dvbsParams(frequency, 0);
								}
							}
							else if(sp.getTsSourceID() == TVChannelParams.MODE_QAM){
								channelList = new TVChannelParams[flist.length];
								/** get each frequency */
								for (int i=0; i<channelList.length; i++) {
									frequency = Integer.parseInt(flist[i]);
									channelList[i] = TVChannelParams.dvbcParams(frequency, 0, 0);
								}
							}
							else if(sp.getTsSourceID() == TVChannelParams.MODE_OFDM){
								channelList = new TVChannelParams[flist.length/2];
								
								/** get each frequency and bandwidth */
								for (int i=0; i<flist.length; i++) {
									
									if(i%2 == 0){
										frequency = Integer.parseInt(flist[i]);
									}else{
										bandwidth = Integer.parseInt(flist[i]);
										channelList[i/2] = TVChannelParams.dvbtParams(frequency, bandwidth);
									}
								}								
							}
							else if(sp.getTsSourceID() == TVChannelParams.MODE_ATSC){
								channelList = new TVChannelParams[flist.length];
								/** get each frequency */
								for (int i=0; i<channelList.length; i++) {
									frequency = Integer.parseInt(flist[i]);
									channelList[i] = TVChannelParams.atcsParams(frequency);
								}
							}
							else if(sp.getTsSourceID() == TVChannelParams.MODE_ANALOG){
								channelList = new TVChannelParams[flist.length];
								/** get each frequency */
								for (int i=0; i<channelList.length; i++) {
									frequency = Integer.parseInt(flist[i]);
									channelList[i] = TVChannelParams.analogParams(frequency, 0, 0);
								}
							}
							
						}
					}
				}
				c.close();
			}
		} 
		
		tsp.setDtvParams(0, channelList);

		/** No exceptions, start scan */
		stopPlaying();
		stopRecording();
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
	private void resolveStartRecording(int bookingID){
		stopRecording();
	}

	/*Stop recording.*/
	private void resolveStopRecording(){
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

				Log.e(TAG, "set input source to "+source.name()+" ok");
				if(source == reqInputSource){
					inputSource = reqInputSource;
					if(isInTVMode()){
						if(status == TVRunningStatus.STATUS_SET_INPUT_SOURCE) {
							TVProgram p = null;

							if(inputSource == TVConst.SourceInput.SOURCE_DTV){
								if(dtvTVPlayParams == null && dtvRadioPlayParams == null){
									/*Get a valid program*/
									p = TVProgram.selectFirstValid(this, TVProgram.TYPE_DTV);
									if(p != null){
										if(p.getType() == TVProgram.TYPE_TV)
											dtvTVPlayParams = TVPlayParams.playProgramByNumber(p.getNumber());
										else
											dtvRadioPlayParams = TVPlayParams.playProgramByNumber(p.getNumber());
									}
								}
							}else if(inputSource == TVConst.SourceInput.SOURCE_ATV){
								if(atvPlayParams == null){
									/*Get a valid program*/
									p = TVProgram.selectFirstValid(this, TVProgram.TYPE_ATV);
									if(p != null)
										atvPlayParams = TVPlayParams.playProgramByNumber(p.getNumber());
								}
							}

							/*Play program*/
							playCurrentProgram();
						}
					}
				}
				/*Send message*/
				sendMessage(TVMessage.inputSourceChanged(event.source));
				break;
			case TVDevice.Event.EVENT_SET_INPUT_SOURCE_FAILED:
				source = TVConst.SourceInput.values()[event.source];

				Log.e(TAG, "set input source to "+source.name()+" failed");
				break;
			case TVDevice.Event.EVENT_FRONTEND:
				if(isInTVMode()){
					if(channelParams!=null && event.feParams.equals(channelParams)){
						if(inputSource == TVConst.SourceInput.SOURCE_DTV){
							/*Start EPG scanner.*/
							if(channelID !=-1){
								epgScanner.enterChannel(channelID);
							}
						}
						if((status == TVRunningStatus.STATUS_SET_FRONTEND)/* && (event.feStatus & TVChannelParams.FE_HAS_LOCK)!=0*/){
							/*Play AV.*/
							playCurrentProgramAV();
						}
						if(!channelLocked && (event.feStatus & TVChannelParams.FE_HAS_LOCK)!=0){
							Log.d(TAG, "signal resume");
							sendMessage(TVMessage.signalResume(channelID));
							channelLocked = true;
						}else if(channelLocked && (event.feStatus & TVChannelParams.FE_TIMEDOUT)!=0){
							Log.d(TAG, "signal lost");
							sendMessage(TVMessage.signalLost(channelID));
							channelLocked = false;
						}
					}
				}
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
			default:
				break;
		}
	}

	/*Solve the events from the channel scanner.*/
	private void resolveScanEvent(TVScanner.Event event){
		Log.d(TAG, "Channel scan event: " + event.type);
		switch (event.type) {
			case TVScanner.Event.EVENT_SCAN_PROGRESS:
				Log.d(TAG, "Progress: " + event.percent + "%" + ", channel no. "+event.channelNumber);
				if (event.programName != null) {
					Log.d(TAG, "New Program : "+ event.programName + ", type "+ event.programType);
				}
				sendMessage(TVMessage.scanUpdate(event.percent, event.channelNumber, event.totalChannelCount, 
					event.channelParams, event.lockedStatus, event.programName, event.programType));
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
			}
		}catch(Exception e){
		}
	}

	/*Switch audio*/
	private void resolveSwitchAudio(int id){
		if(inputSource == TVConst.SourceInput.SOURCE_DTV){
			if(status == TVRunningStatus.STATUS_PLAY_DTV){
				TVProgram p = TVProgram.selectByID(this, programID);
				if(p != null){
					TVProgram.Audio audio;
					int apid, afmt;

					audio = p.getAudio(id);
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

	public IBinder onBind (Intent intent){
		return mBinder;
	}

	public void onCreate(){
		super.onCreate();	
		device = new TVDeviceImpl( this.getMainLooper()){
			/*Device event handler*/
			public void onEvent(TVDevice.Event event){
				Message msg = handler.obtainMessage(MSG_DEVICE_EVENT, event);
				handler.sendMessage(msg);
			}
		};

		TVDataProvider.openDatabase(this);
		config = new TVConfig(this);

		try{
			String modeStr = config.getString("tv:dtv:mode");
			int mode = TVChannelParams.getModeFromString(modeStr);
			if(mode == -1)
				throw new Exception();
			epgScanner.setSource(0, 0, mode);

			config.registerUpdate("tv:audio:language", this);
			config.registerUpdate("setting", device);
			config.registerRead("setting", device);
		}catch(Exception e){
			Log.e(TAG, "intialize config failed");
		}

	}
	
	public void onUpdate(String name, TVConfigValue value){
		try{
			Message msg = handler.obtainMessage(MSG_CONFIG_CHANGED, name);
			handler.sendMessage(msg);
		}catch(Exception e){
		}
	}

	public void onDestroy(){
		callbacks.kill();
		TVDataProvider.closeDatabase(this);
		super.onDestroy();
	}
}

