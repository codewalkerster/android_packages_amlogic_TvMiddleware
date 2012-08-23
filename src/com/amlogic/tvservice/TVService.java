package com.amlogic.tvservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.os.Message;
import android.os.Looper;
import android.os.Handler;
import java.util.Date;
import android.os.RemoteCallbackList;
import com.amlogic.tvutil.TVStatus;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.ITVCallback;

public class TVService extends Service{
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

	private int inputSource;
	private TVTime time = new TVTime();
	private TVConfig config = new TVConfig();
	private TVDevice device = new TVDevice();
	private TVScanner scanner;
	private TVEpgScanner epgScanner;
	private TVVbi vbi;

	final RemoteCallbackList<ITVCallback> callbacks
	                        = new RemoteCallbackList<ITVCallback>();

	private final ITVService.Stub mBinder = new ITVService.Stub(){
		public synchronized void registerCallback(ITVCallback cb){
			if(cb !=null){
				callbacks.register(cb);
			}
		}

		public synchronized void unregisterCallback(ITVCallback cb){
			if(cb != null){
				callbacks.unregister(cb);
			}
		}

		public void setConfig(String name, String value){
			config.set(name, value);
		}

		public String getConfig(String name){
			return config.get(name);
		}

		public void registerConfigCallback(String name, ITVCallback cb){
			config.registerCallback(name, cb);
		}

		public void unregisterConfigCallback(String name, ITVCallback cb){
			config.unregisterCallback(name, cb);
		}

		public long getTime(){
			Date date = time.getTime();
			return date.getTime();
		}

		public void setInputSource(int source){
			Message msg = handler.obtainMessage(MSG_SET_SOURCE, new Integer(source));
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
			Message msg = handler.obtainMessage(MSG_START_SCAN);
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
	};

	private Handler handler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
				case MSG_SET_SOURCE:
					setInputSource((Integer)msg.obj);
					break;
				case MSG_PLAY_PROGRAM:
					playProgram((TVPlayParams)msg.obj);
					break;
				case MSG_STOP_PLAYING:
					stopPlaying();
					break;
				case MSG_START_TIMESHIFTING:
					startTimeshifting();
					break;
				case MSG_START_PLAYBACK:
					startPlayback((Integer)msg.obj);
					break;
				case MSG_START_SCAN:
					startScan((TVScanParams)msg.obj);
					break;
				case MSG_STOP_SCAN:
					stopScan((Boolean)msg.obj);
					break;
				case MSG_START_RECORDING:
					startRecording((Integer)msg.obj);
					break;
				case MSG_STOP_RECORDING:
					stopRecording();
					break;
				case MSG_PAUSE:
					pause();
					break;
				case MSG_RESUME:
					resume();
					break;
				case MSG_FAST_FORWARD:
					fastForward((Integer)msg.obj);
					break;
				case MSG_FAST_BACKWARD:
					fastBackward((Integer)msg.obj);
					break;
				case MSG_SEEK_TO:
					seekTo((Integer)msg.obj);
					break;
			}
		}
	};

	private void setInputSource(int src){
		if(intputSource == src)
			return;

		switch(inputSource){
			case TVStatus.SOURCE_TV:
				break;
		}

		device.setInputSource(src);

		switch(src){
		}

		inputSource = src;
	}

	private void playProgram(TVPlayParams tp){
	}

	private void stopPlaying(){
	}

	private void startTimeshifting(){
	}

	private void startPlayback(int bookingID){
	}

	private void startScan(TVScanParams sp){
	}

	private void stopScan(boolean store){
	}

	private void startRecording(int bookingID){
	}

	private void stopRecording(){
	}

	private void pause(){
	}

	private void resume(){
	}

	private void fastForward(int speed){
	}

	private void fastBackward(int speed){
	}

	private void seekTo(int pos){
	}

	public TVService(Context ctx){
	}

	public IBinder onBind (Intent intent){
		return mBinder;
	}

	public void onDestroy(){
		callbacks.kill();
	}
}

