package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *TV播放参数
 */
public class TVPlayParams implements Parcelable {
	public static final int PLAY_PROGRAM_NUMBER = 0;
	public static final int PLAY_PROGRAM_ID     = 1;
	public static final int PLAY_PROGRAM_UP     = 2;
	public static final int PLAY_PROGRAM_DOWN   = 3;

	private int type;
	private TVProgramNumber number;
	private int id;

	public static final Parcelable.Creator<TVPlayParams> CREATOR = new Parcelable.Creator<TVPlayParams>(){
		public TVPlayParams createFromParcel(Parcel in) {
			return new TVPlayParams(in);
		}
		public TVPlayParams[] newArray(int size) {
			return new TVPlayParams[size];
		}
	};

	public void readFromParcel(Parcel in){
		type = in.readInt();
		if(type == PLAY_PROGRAM_ID)
			id = in.readInt();
		else if(type == PLAY_PROGRAM_NUMBER)
			number = new TVProgramNumber(in);
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(type);
		if(type == PLAY_PROGRAM_ID)
			dest.writeInt(id);
		else if(type == PLAY_PROGRAM_NUMBER)
			number.writeToParcel(dest, flags);
	}

	public TVPlayParams(Parcel in){
		readFromParcel(in);
	}

	/**
	 *创建一个按节目号播放的播放参数
	 *@param no 节目号
	 *@return 返回新的播放参数
	 */
	public static TVPlayParams playProgramByNumber(TVProgramNumber no){
		TVPlayParams tp = new TVPlayParams(PLAY_PROGRAM_NUMBER);

		tp.number = no;
		return tp;
	}

	/**
	 *创建一个按节目ID播放的播放参数
	 *@param id 节目ID
	 *@return 返回新的播放参数
	 */
	public static TVPlayParams playProgramByID(int id){
		TVPlayParams tp = new TVPlayParams(PLAY_PROGRAM_ID);

		tp.id = id;
		return tp;
	}

	/**
	 *播放下一节目(频道号比当前节目大)
	 *@return 返回新的播放参数
	 */
	public static TVPlayParams playProgramUp(){
		TVPlayParams tp = new TVPlayParams(PLAY_PROGRAM_UP);
		return tp;
	}

	/**
	 *播放上一节目(频道号比当前节目小)
	 *@return 返回新的播放参数
	 */
	public static TVPlayParams playProgramDown(){
		TVPlayParams tp = new TVPlayParams(PLAY_PROGRAM_DOWN);
		return tp;
	}

	/**
	 *创建播放参数
	 *@param type 播放参数类型
	 */
	public TVPlayParams(int type){
		this.type = type;
	}

	/**
	 *创建播放参数
	 *@param tp 复制此参数到新建对象
	 */
	public TVPlayParams(TVPlayParams tp){
		this.type   = tp.type;
		this.number = new TVProgramNumber(tp.number);
		this.id     = tp.id;
	}

	/**
	 *判断播放参数是否相等
	 *@param tp 与之比较的比较播放参数
	 *@return true表示相等,false表示不相等
	 */
	public boolean equals(TVPlayParams tp){
		if(tp.type != type)
			return false;
		if(tp.type == PLAY_PROGRAM_NUMBER)
			return tp.number.equals(number);
		else if(tp.type == PLAY_PROGRAM_ID)
			return tp.id == id;

		return false;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVPlayParams> getCreator() {
		return CREATOR;
	}

	/**
	 *取得播放类型
	 *@return 返回播放类型
	 */
	public int getType(){
		return type;
	}

	/**
	 *对按ID播放的参数，取得节目ID
	 *@return 返回节目ID
	 */
	public int getProgramID() throws Exception {
		if(type != PLAY_PROGRAM_ID)
			throw new Exception();
		return id;
	}

	/**
	 *对按节目号播放的参数，取得节目号
	 *@return 返回节目号
	 */
	public TVProgramNumber getProgramNumber() throws Exception{
		if(type != PLAY_PROGRAM_NUMBER)
			throw new Exception();

		return number;
	}
}

