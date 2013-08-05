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
	private String prefixFileName;
	private String suffixFileName;
	private String programName;
	private int programID;
	private int pmtPID;
	private int pmtProgramNumber;
	private long currRecordSize;
	private long currRecordTime;
	private long recTotalTime;
	private boolean isTimeshift;
	private TVProgram.Video video;
	private TVProgram.Audio[] audios;
	private TVProgram.Subtitle[] subtitles;
	private TVProgram.Teletext[] teletexts;

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
		recTotalTime   = in.readLong();
		programID      = in.readInt();
		programName    = in.readString();
		
		int pid, fmt;
		String lang;
		TVProgram p = new TVProgram();
		/* read video */
		video = null;
		int cnt = in.readInt();
		if (cnt > 0){
			pid = in.readInt();
			fmt = in.readInt();
			video = p.new Video(pid, fmt); 
		}
		/* read audios */
		audios = null;
		cnt = in.readInt();
		if (cnt > 0){
			audios = new TVProgram.Audio[cnt];
			for (int i=0; i<cnt; i++){
				pid  = in.readInt();
				fmt  = in.readInt();
				lang = in.readString();
				audios[i] = p.new Audio(pid, lang, fmt);
			}
		}
		/* read subtitles */
		subtitles = null;
		cnt = in.readInt();
		if (cnt > 0){
			subtitles = new TVProgram.Subtitle[cnt];
			int type, num1, num2;
			for (int i=0; i<cnt; i++){
				pid  = in.readInt();
				type = in.readInt();
				num1 = in.readInt();
				num2 = in.readInt();
				lang = in.readString();
				subtitles[i] = p.new Subtitle(pid, lang, type, num1, num2);
			}
		}
		/* read teletexts */
		teletexts = null;
		cnt = in.readInt();
		if (cnt > 0){
			teletexts = new TVProgram.Teletext[cnt];
			int mag, page;
			for (int i=0; i<cnt; i++){
				pid  = in.readInt();
				mag = in.readInt();
				page = in.readInt();
				lang = in.readString();
				teletexts[i] = p.new Teletext(pid, lang, mag, page);
			}
		}
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeLong(currRecordSize);
		dest.writeLong(currRecordTime);
		dest.writeLong(recTotalTime);
		dest.writeInt(programID);
		dest.writeString(programName);

		/* write video */
		if (video != null){
			dest.writeInt(1);
			dest.writeInt(video.getPID());
			dest.writeInt(video.getFormat());
		}else{
			dest.writeInt(0);
		}
		/* write audios */
		if (audios != null){
			dest.writeInt(audios.length);
			for (int i=0; i<audios.length; i++){
				dest.writeInt(audios[i].getPID());
				dest.writeInt(audios[i].getFormat());
				dest.writeString(audios[i].getLang());
			}
		}else{
			dest.writeInt(0);
		}
		/* write subtitles */
		if (subtitles != null){
			dest.writeInt(subtitles.length);
			for (int i=0; i<subtitles.length; i++){
				dest.writeInt(subtitles[i].getPID());
				dest.writeInt(subtitles[i].getType());
				if (subtitles[i].getType() == TVProgram.Subtitle.TYPE_DVB_SUBTITLE){
					dest.writeInt(subtitles[i].getCompositionPageID());
					dest.writeInt(subtitles[i].getAncillaryPageID());
				}else if (subtitles[i].getType() == TVProgram.Subtitle.TYPE_DTV_TELETEXT){
					dest.writeInt(subtitles[i].getMagazineNumber());
					dest.writeInt(subtitles[i].getPageNumber());
				}else{
					/* Impossible */
					dest.writeInt(0);
					dest.writeInt(0);
				}
				dest.writeString(subtitles[i].getLang());
			}
		}else{
			dest.writeInt(0);
		}
		/* write teletexts */
		if (teletexts != null){
			dest.writeInt(teletexts.length);
			for (int i=0; i<teletexts.length; i++){
				dest.writeInt(teletexts[i].getPID());
				dest.writeInt(teletexts[i].getMagazineNumber());
				dest.writeInt(teletexts[i].getPageNumber());
				dest.writeString(teletexts[i].getLang());
			}
		}else{
			dest.writeInt(0);
		}
	}

	public DTVRecordParams(Parcel in){
		readFromParcel(in);
	}
	
	public DTVRecordParams(){
	}
	
	/**
	 *从一个booking中创建录像启动参数
	 *@param book 需要录制的预约录像
	 *@param storagePath 用户选择的存储器路径
	 *@param prefixName 录像文件前缀名
	 *@param suffixName 录像文件后缀名
	 *@param isTimeshift 是否为时移录像
	 */
	public DTVRecordParams(TVBooking book, String storagePath, String prefixName, String suffixName, boolean isTimeshift){
		this.storagePath = storagePath;

		TVProgram prog = book.getProgram();
		if (prog == null){
			video     = null;
			audios    = null;
			subtitles = null;
			teletexts = null;
			programID = -1;
			pmtPID    = 0x1fff;
			pmtProgramNumber = 0xffff;
			programName      = null;
		}else{
			video     = prog.getVideo();
			audios    = prog.getAllAudio();
			subtitles = prog.getAllSubtitle();
			teletexts = prog.getAllTeletext();
			programID = prog.getID();
			pmtPID    = prog.getPmtPID();
			pmtProgramNumber = prog.getDVBServiceID();
			programName      = prog.getName();
		}

		recTotalTime = book.getDuration();
		this.isTimeshift = isTimeshift;
		currRecordSize = 0;
		currRecordTime = 0;
		prefixFileName = prefixName;
		suffixFileName = suffixName;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<DTVRecordParams> getCreator() {
		return CREATOR;
	}

	/**
	 *获取当前录像时间
	 *@return 返回当前录像时间，ms
	 */
	public long getCurrentRecordTime(){
		return currRecordTime;
	}

	/**
	 *获取总的录像时间
	 *@return 返回总的录像时间，当为即时录像时该值为0
	 */
	public long getTotalRecordTime(){
		return recTotalTime;
	}

	/**
	 *获取当前录像文件长度
	 *@return 返回录像文件长度
	 */
	public long getCurrentRecordSize(){
		return currRecordSize;
	}
	
	/**
	 *获取当前录像文件全路径
	 *@return 返回录像文件全路径
	 */
	public String getRecordFilePath(){
		return recFilePath;
	}
	
	/**
	 *获取当前录像的Program ID
	 *@return 返回当前录像的Program ID
	 */
	public int getProgramID(){
		return programID;
	}

	/**
	 *获取当前录像的Program名称 
	 *@return 返回当前录像的Program名称
	 */
	public String getProgramName(){
		return programName;
	}
	
	/**
	 *获取当前录像的视频
	 *@return 返回Video对象
	 */
	public TVProgram.Video getVideo(){
		return video;
	}

	/**
	 *获取当前录像的所有音频
	 *@return 返回当前录像的所有Audio对象数组
	 */
	public TVProgram.Audio[] getAllAudio(){
		return audios;
	}

	/**
	 *获取当前录像的所有Subtitle
	 *@return 返回当前录像的所有Subtitle对象数组
	 */
	public TVProgram.Subtitle[] getAllSubtitle(){
		return subtitles;
	}

	/**
	 *获取当前录像的所有Teletext
	 *@return 返回当前录像的所有Teletext对象数组
	 */
	public TVProgram.Teletext[] getAllTeletext(){
		return teletexts;
	}
	
	public void setProgramID(int id){
		programID = id;
	}
}

