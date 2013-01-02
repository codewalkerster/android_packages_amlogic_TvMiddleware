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
	/** Record error codes */
	public static final int REC_ERR_NONE        = 0; // Success, no error
	public static final int REC_ERR_OPEN_FILE   = 1; // Cannot open output record file
	public static final int REC_ERR_WRITE_FILE  = 2; // Cannot write data to record file
	public static final int REC_ERR_ACCESS_FILE = 3; // Cannot access record file
	public static final int REC_ERR_SYSTEM      = 4; // For other system reasons
	
	/** Record status */
	public static final int ST_IDLE         = 0;
	public static final int ST_RECORDING    = 1;
	public static final int ST_WAITING_FEND = 2;
	
	/** Timeshifting record file path */
	public static final String TIMESHIFTING_FILE = "DVBRecordFiles/REC_TimeShifting0.amrec";
	
	private static final String TAG = "TVRecorder";
	private int native_handle;
	private int fendID = -1;
	private int dvrID  = -1;
	private int asyncFifoID = -1;
	private int status = ST_IDLE;
	private String storePath = "/mnt/sda1";
	private Handler recordHandler = new Handler();
	private TVRecordInfoFile riFile = null;
	private TVDevice tvDevice = null;
	private TVRecorderParams recordParams = null;
	private Context context;
	private MountEventReceiver mntEvtRecver = null;
	
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
	
	class TVRecordInfoFile{
		private String fileName;
		private String storagePath;
		private String[] lines = new String[21];
				
		private void flush(){
			try {
				File file = new File(fileName);
				if (!file.exists()){
					file.createNewFile();
				}
				FileOutputStream fos=new FileOutputStream(file, false);
				OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
				BufferedWriter bw = new BufferedWriter(osw);
				for (int i=0; i<lines.length; i++){
					bw.write(lines[i], 0, lines[i].length());
					bw.newLine();
				}
				bw.flush();
				bw.close();
				osw.close();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public TVRecordInfoFile(String storage, String fname){
			fileName = fname;
			storagePath = storage;
		}
		
		/** Update the record time to file */
		public void update(long duration){
			lines[1] = Integer.toString((int)(duration/1000));
			flush();
		}
		
		/** Construct from a TVBooking */
		public void construct(TVBooking book){
			lines[0] = Integer.toString((int)(book.getStart()/1000));
			lines[1] = Integer.toString(0);
			lines[2] = (book.getProgramName()!=null) ? book.getProgramName() : "";
			lines[3] = (book.getEventName()!=null) ? book.getEventName() : "";
			lines[4] = fileName.replace(storagePath+"/", "");
			lines[4] = lines[4].replace("amri", "amrec");
			lines[5] = (book.getVideo()!=null) ? Integer.toString(book.getVideo().getPID()) : "0x1fff";
			lines[6] = (book.getVideo()!=null) ? Integer.toString(book.getVideo().getFormat()) : "-1";
			
			TVProgram.Audio auds[] = book.getAllAudio();
			String apids="", afmts="", alangs="";
			if (auds != null && auds.length > 0) {
				apids  += auds[0].getPID();
				afmts  += auds[0].getFormat();
				alangs += auds[0].getLang();
				for (int i=1; i<auds.length; i++){
					apids  += " "+auds[i].getPID();
					afmts  += " "+auds[i].getFormat();
					alangs += " "+auds[i].getLang();
				}
			}
			lines[7] = apids;
			lines[8] = afmts;
			lines[9] = alangs;
			
			TVProgram.Subtitle subs[] = book.getAllSubtitle();
			String spids="", stypes="", scpgids="", sapgids="", slangs="";
			if (subs != null && subs.length > 0) {
				spids   += subs[0].getPID();
				stypes  += subs[0].getType();
				scpgids += subs[0].getCompositionPageID();
				sapgids += subs[0].getAncillaryPageID();
				slangs  += subs[0].getLang();
				for (int i=0; i<subs.length; i++){
					spids   += " "+subs[i].getPID();
					stypes  += " "+subs[i].getType();
					scpgids += " "+subs[i].getCompositionPageID();
					sapgids += " "+subs[i].getAncillaryPageID();
					slangs  += " "+subs[i].getLang();
				}
			}
			lines[10] = spids;
			lines[11] = stypes;
			lines[12] = scpgids;
			lines[13] = sapgids;
			lines[14] = slangs;
		
			TVProgram.Teletext ttxs[] = book.getAllTeletext();
			String tpids="", ttypes="", tmagnums="", tpgnums="", tlangs="";
			if (ttxs != null && ttxs.length > 0) {
				tpids    += ttxs[0].getPID();
				tmagnums += ttxs[0].getMagazineNumber();
				tpgnums  += ttxs[0].getPageNumber();
				tlangs   += ttxs[0].getLang();
				for (int i=0; i<ttxs.length; i++){
					tpids    += " "+ttxs[i].getPID();
					tmagnums += " "+ttxs[i].getMagazineNumber();
					tpgnums  += " "+ttxs[i].getPageNumber();
					tlangs   += " "+ttxs[i].getLang();
				}
			}
			
			lines[15] = tpids;
			lines[16] = "";
			lines[17] = tmagnums;
			lines[18] = tpgnums;
			lines[19] = tlangs;
			lines[20] = storagePath;
		}
		
		/** load this ri file to a database booking_table record */
		public void toDatabaseRecord(){
			try {
				ContentValues values = new ContentValues();
				FileInputStream fis=new FileInputStream(fileName);
				InputStreamReader isr=new InputStreamReader(fis, "UTF-8");
				BufferedReader br = new BufferedReader(isr);      
				String line;        
				int lineCount = 0;
				String fields[] = {"start","duration","srv_name","evt_name",
					"file_name","vid_pid","vid_fmt","aud_pids","aud_fmts",
					"aud_languages","sub_pids","sub_types","sub_composition_page_ids",
					"sub_ancillary_page_ids","sub_languages","ttx_pids","ttx_types",
					"ttx_magazine_numbers","ttx_page_numbers","ttx_languages"};
					
				String sql = "insert into booking_table(db_srv_id,db_evt_id,status,";
				for (int i=0; i<fields.length; i++) {
					sql += fields[i];
					sql += ",";
				}
				sql += "other_pids, from_storage) values(-1,-1,3,";
				while ((line=br.readLine())!=null && lineCount < fields.length) {
					if (fields[lineCount].equals("start") || 
						fields[lineCount].equals("duration") ||
						fields[lineCount].equals("vid_pid") ||
						fields[lineCount].equals("vid_fmt")) {
						sql += line;
					} else {
						sql += "'";
						line = TVBooking.sqliteEscape(line);
						sql += line;
						sql += "'";
					}
					sql += ",";
					/* ensure */
					if (fields[lineCount].equals("file_name") && !line.endsWith("amrec")) {
						Log.d(TAG, "Invalid file_name "+line+", delete this ri file");
						File ri_file = new File(fileName);
						if (ri_file != null && ri_file.exists()) {
							ri_file.delete();
						}
						break;
					}
					lineCount++;
				}
				if (lineCount < fields.length) {
					Log.d(TAG, "Invalid ri file, lines "+lineCount);
				} else {
					Log.d(TAG, "Insert a rec record to database");
					sql += "'', '" + storagePath + "')";
					context.getContentResolver().query(TVDataProvider.WR_URL, null, sql, null, null);
				}
			
				br.close();
				isr.close();
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	class MountEventReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        Uri uri = intent.getData();
	
	        if (uri.getScheme().equals("file")) {
	        	String path = uri.getPath();
	        	
	        	if (action.equals(Intent.ACTION_MEDIA_MOUNTED))
    			{
	        		Log.d(TAG, path + " mounted");
					scanRecordsToDatabase(path);
					/* tell service */
					Event evt = new Event(Event.EVENT_RECORDS_UPDATE);
					onEvent(evt);
    			}
	        	else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED))
    			{
					Log.d(TAG, path + " unmounted");
					clearDatabaseRecords(path);
					/* tell service */
					Event evt = new Event(Event.EVENT_RECORDS_UPDATE);
					onEvent(evt);
    			}
	        }
		}
    }
    
    private void clearDatabaseRecords(String storage) {
		Log.d(TAG, "delete records for storage "+storage);
		String sql = "delete from booking_table where from_storage='"+storage+"'";
		context.getContentResolver().query(TVDataProvider.WR_URL, null, sql, null, null);
	}

	private void scanRecordsToDatabase(String storage) {
		List<String> fileList =  new ArrayList<String>();

		/*re-scan*/
		clearDatabaseRecords(storage);
		File[] files = new File(storage+"/DVBRecordFiles").listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.getName().endsWith("amri")) {
					Log.d(TAG, "Adding "+file.getName()+" to database ...");
					TVRecordInfoFile rf = new TVRecordInfoFile(storage, file.getPath());
					rf.toDatabaseRecord();
				}
			}	
		}
	}
	
	private Runnable recorderTimerTask = new Runnable() {
		public void run() {
			DTVRecordParams recPara = tvDevice.getRecordingParams();
			if (recPara != null && recPara.getCurrentRecordTime() > 0) {
				if (riFile == null && recordParams != null) {
					/** Recording now, construct record info file from the TVBooking */
					String riFilePath = recPara.getRecordFilePath().replace("amrec","amri");
					Log.d(TAG, "Creating a new ri file: " + riFilePath);
					riFile = new TVRecordInfoFile(storePath, riFilePath);
					riFile.construct(recordParams.booking);
					
					/* update the file path allocated by device to database */
					recordParams.booking.updateRecordStoragePath(storePath);
					recordParams.booking.updateRecordFilePath(recPara.getRecordFilePath().replace(storePath+"/", ""));
				}
				if (riFile != null) {
					long currentRecTime = recPara.getCurrentRecordTime();
					Log.d(TAG, "Upadting record time to storage ("+currentRecTime/1000+"s)");
					riFile.update(currentRecTime);
				}
			}
			recordHandler.postDelayed(this, 1000);
		}
	};
	
	private int startDevice(){
		if (recordParams == null) {
			Log.d(TAG, "Recorder params not set, cannot start record!");
			return -1;
		}
		TVBooking book = recordParams.booking;
		DTVRecordParams recStartPara = new DTVRecordParams(book, storePath, recordParams.isTimeshift);
		tvDevice.startRecording(recStartPara);
		
		status = ST_RECORDING;
		if (! recordParams.isTimeshift) {
			recordHandler.postDelayed(recorderTimerTask, 1000);
		}
		
		return 0;
	}
	
	public TVRecorder(Context context){	
		this.context = context;
		
	}
	
	public void open(TVDevice device){
		this.tvDevice = device;
		mntEvtRecver = new MountEventReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addDataScheme("file");
		Log.d(TAG,"mntRecver "+mntEvtRecver);
		context.registerReceiver(mntEvtRecver, filter);
	}
	
	public void close(){
		if(mntEvtRecver != null) {
			context.unregisterReceiver(mntEvtRecver);
			mntEvtRecver = null;
		}
		if (status == ST_RECORDING){
			tvDevice.stopRecording();
		}
		tvDevice = null;
	}
	
	public void onRecordEvent(TVDevice.Event event){
		switch(event.type){
			case TVDevice.Event.EVENT_RECORD_END:
				Log.d(TAG, "Recorder get record end from device");
				recordHandler.removeCallbacks(recorderTimerTask);
				if (riFile != null) {
					long totalRecTime = event.recParams.getTotalRecordTime();
					/* update to file */
					riFile.update(totalRecTime);
					/* update to database */
					recordParams.booking.updateDuration(totalRecTime);
					recordParams.booking.updateStatus(TVBooking.ST_END);
				}
				status = ST_IDLE;
				recordParams = null;
				riFile = null;
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
	}
	
	public DTVRecordParams getRecordingParams(){
		if (tvDevice != null) {
			return tvDevice.getRecordingParams();
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
	
	
	public void scanRecordsFromStorage() {
		File[] files = new File("/mnt").listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.getPath().startsWith("/mnt/sd") && 
					!(file.getPath().equals("/mnt/sdcard"))) {
					File myfile = file;
					if(myfile.canRead()){
						scanRecordsToDatabase(file.getPath());
					}
				}
			}	
		}
		/*SDCARD*/
		Runtime runtime = Runtime.getRuntime();  

		String cmd = "mount";
		String sdcard_path = "/mnt/sdcard/external_sdcard";

		try {
			Process proc = runtime.exec(cmd);
			InputStream input = proc.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			String strLine;
			while(null != (strLine = br.readLine())){
				for(int i=0;i<strLine.length();i++){
					if(strLine.regionMatches(i,sdcard_path,0,sdcard_path.length())) {
						scanRecordsToDatabase(sdcard_path);
					}
				}
			}	
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	
};
