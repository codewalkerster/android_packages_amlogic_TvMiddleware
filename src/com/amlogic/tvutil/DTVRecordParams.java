package com.amlogic.tvutil;

import android.util.Log;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *TV录像参数
 */
public class DTVRecordParams implements Parcelable {
	private static final String TAG = "DTVRecordParams";
	
	private String recFilePath;
	private String storagePath;
	private int programID;
	private int vidPid;
	private int audPids[];
	private int otherPids[];
	private long currRecordSize;
	private long currRecordTime;
	private long recTotalTime;
	private boolean isTimeshift;

	public static final Parcelable.Creator<DTVRecordParams> CREATOR = new Parcelable.Creator<DTVRecordParams>(){
		public DTVRecordParams createFromParcel(Parcel in) {
			return new DTVRecordParams(in);
		}
		public DTVRecordParams[] newArray(int size) {
			return new DTVRecordParams[size];
		}
	};

	public void readFromParcel(Parcel in){
		currRecordSize = in.readLong();
		currRecordTime = in.readLong();
		recTotalTime = in.readLong();
		programID = in.readInt();
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeLong(currRecordSize);
		dest.writeLong(currRecordTime);
		dest.writeLong(recTotalTime);
		dest.writeInt(programID);
	}

	public DTVRecordParams(Parcel in){
		readFromParcel(in);
	}
	
	public DTVRecordParams(){
	}
	
	public DTVRecordParams(TVBooking book, String storagePath, boolean isTimeshift){
		this.storagePath = storagePath;
		TVProgram.Audio audios[] = book.getAllAudio();
		TVProgram.Subtitle subtitles[] = book.getAllSubtitle();
		TVProgram.Teletext teletexts[] = book.getAllTeletext();
		vidPid = (book.getVideo() != null) ? book.getVideo().getPID() : 0x1fff;
		Log.d(TAG, "Video pid: "+vidPid);

		int pidCount = (audios != null) ? audios.length : 0;
		audPids = new int[pidCount];
		for (int i=0; i<pidCount; i++){
			Log.d(TAG, "Audio pid: "+audios[i].getPID());
			audPids[i] = audios[i].getPID();
		}

		pidCount = (subtitles != null) ? subtitles.length : 0;
		pidCount += (teletexts != null) ? teletexts.length : 0;
		otherPids = new int[pidCount];
		
		pidCount = 0;
		for (int i=0; subtitles!=null && i<subtitles.length; i++){
			Log.d(TAG, "Subtitle pid: "+subtitles[i].getPID());
			otherPids[pidCount++] = subtitles[i].getPID();
		}
		for (int i=0; teletexts!=null && i<teletexts.length; i++){
			Log.d(TAG, "Teletext pid: "+teletexts[i].getPID());
			otherPids[pidCount++] = teletexts[i].getPID();
		}
		recTotalTime = book.getDuration();
		this.isTimeshift = isTimeshift;
		
		if (book.getProgram() != null){
			programID = book.getProgram().getID();
		}else{
			programID = -1;
		}
		
		currRecordSize = 0;
		currRecordTime = 0;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<DTVRecordParams> getCreator() {
		return CREATOR;
	}

	public long getCurrentRecordTime(){
		return currRecordTime;
	}

	public long getTotalRecordTime(){
		return recTotalTime;
	}

	public long getCurrentRecordSize(){
		return currRecordSize;
	}
	
	public String getRecordFilePath(){
		return recFilePath;
	}
	
	public int getProgramID(){
		return programID;
	}
	
	public void setProgramID(int id){
		programID = id;
	}
}

