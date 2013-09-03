package com.amlogic.tvservice;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Handler;
import android.util.Log;
import android.content.ContentValues;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVBooking;
import com.amlogic.tvutil.DTVRecordParams;
import com.amlogic.tvdataprovider.TVDataProvider;


abstract public class TVRecorder{	
	/** Record status */
	public static final int ST_IDLE         = 0;
	public static final int ST_RECORDING    = 1;
	public static final int ST_WAITING_FEND = 2;

	private static final String TAG = "TVRecorder";
	private static final String SUFFIX_NAME = "ts";	
	private static final String STORE_DIR = "TVRecordFiles";
	private int native_handle;
	private int status = ST_IDLE;
	private String storePath = "/mnt/sda1";
	private TVDevice tvDevice = null;
	private TVRecorderParams recordParams = null;
	private Context context;
	
	public class Event{
		public static final int EVENT_RECORDS_UPDATE = 0;
		
		public int type;

		public Event(int type){
			this.type = type;
		}
	};
	
	public class TVRecorderParams{
		public TVBooking booking;
		public boolean isTimeshift;
		public boolean fendLocked;
	};
	
	private int startDevice(){
		if (recordParams == null) {
			Log.d(TAG, "Recorder params not set, cannot start record!");
			return -1;
		}
		TVBooking book = recordParams.booking;
		String progName = book.getProgram().getName();
		if (progName.length() > 16){
			progName = progName.substring(0, 15);
		}else if (progName.isEmpty()){
			progName = "Program";
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("-MMddyyyy-HHmmss");
  		String prefixName = progName + sdf.format(new Date());

		DTVRecordParams recStartPara = new DTVRecordParams(book,storePath+"/"+STORE_DIR, 
			prefixName, SUFFIX_NAME, recordParams.isTimeshift);

		status = ST_RECORDING;
		
		tvDevice.startRecording(recStartPara);
				
		return 0;
	}

	public TVRecorder(Context context){	
		this.context = context;
		
	}
	
	public void open(TVDevice device){
		this.tvDevice = device;
	}
	
	public void close(){
		if (status == ST_RECORDING){
			tvDevice.stopRecording();
		}
		tvDevice = null;
	}
	
	public synchronized void onRecordEvent(TVDevice.Event event){
		switch(event.type){
			case TVDevice.Event.EVENT_RECORD_END:
				Log.d(TAG, "Recorder get record end from device");

				if (recordParams != null && 
					recordParams.booking != null &&
					recordParams.booking.getID() >= 0 && 
					recordParams.booking.getStatus() != TVBooking.ST_END){
					
					recordParams.booking.updateStatus(TVBooking.ST_END);
				}
													
				status = ST_IDLE;
				recordParams = null;

				break;
		}
	}
	
	public void onEvent(TVRecorder.Event event){
	
	}

	public void setStorage(String path){
		storePath = path;
	}
	
	public String getStorage(){
		return storePath;
	}
	
	public void startRecord(TVRecorderParams param){
		if (tvDevice == null) {
			Log.d(TAG, "No TV device specified, cannot start record!");
			return;
		}
		if (param.booking == null) {
			Log.d(TAG, "Invalid booking, cannot start record!");
			return;
		}
		if (status != ST_IDLE) {
			Log.d(TAG, "Already recording now, stop it first for new one!");
			return;
		}
		
		recordParams = param;
		if (param.fendLocked) {
			if (startDevice() < 0) {
				recordParams = null;
			}
		} else {
			status = ST_WAITING_FEND;
		}
	}
	
	public void fendLocked(){
		if (status == ST_WAITING_FEND) {
			if (startDevice() < 0) {
				status = ST_IDLE;
				recordParams = null;
			}
		}
	}
	
	public void stopRecord(){
		if (tvDevice == null) {
			Log.d(TAG, "No TV device specified, cannot stop record!");
			return;
		}
		if (status == ST_IDLE) {
			Log.d(TAG, "Recording already stopped!");
			return;
		}
		
		tvDevice.stopRecording();
		
		if (recordParams != null && 
			recordParams.booking != null &&
			recordParams.booking.getID() >= 0){
			recordParams.booking.updateStatus(TVBooking.ST_END);	
		}
		
		status = ST_IDLE;
		recordParams = null;
	}
	
	public DTVRecordParams getRecordingParams(){
		if (tvDevice != null && status != ST_IDLE) {
			DTVRecordParams para = tvDevice.getRecordingParams();
			TVProgram p = getRecordingProgram();
			if (para != null) {
				if (p != null) {
					para.setProgramID(p.getID());
				} else {
					para.setProgramID(-1);
				}
				return para;
			}
		}
		
		return null;
	}
	
	public TVProgram getRecordingProgram(){
		return (recordParams != null) ? recordParams.booking.getProgram() : null;
	}
	
	public int getStatus(){
		return status;
	}

	public boolean isRecording(){
		return (status != ST_IDLE);
	}
	
	public boolean isTimeshifting(){
		if (isRecording() && recordParams != null){
			return recordParams.isTimeshift;
		}
		
		return false;
	}

	public String getTimeshiftingFilePath(){
		return storePath + "/" + STORE_DIR + "/TimeShifting0." + SUFFIX_NAME;
	}
	
};
