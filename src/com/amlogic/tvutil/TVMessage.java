package com.amlogic.tvutil;

import java.lang.UnsupportedOperationException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 *TV 消息
 */
public class TVMessage implements Parcelable{
	/**正在播放的节目因收看等级不够被停止*/
	public static final int TYPE_PROGRAM_BLOCK     = 1;
	/**正在播放的节目从BLOCK状态恢复*/
	public static final int TYPE_PROGRAM_UNBLOCK   = 2;
	/**没有信号*/
	public static final int TYPE_SIGNAL_LOST       = 3;
	/**信号恢复*/
	public static final int TYPE_SIGNAL_RESUME     = 4;
	/**节目数据停止*/
	public static final int TYPE_DATA_LOST         = 5;
	/**节目数据恢复*/
	public static final int TYPE_DATA_RESUME       = 6;
	/**预约提醒*/
	public static final int TYPE_BOOKING_REMIND    = 7;
	/**预约开始*/
	public static final int TYPE_BOOKING_START     = 8;
	/**配置项被修改*/
	public static final int TYPE_CONFIG_CHANGED    = 9;
	/**TV Scan progress message*/
	public static final int TYPE_SCAN_PROGRESS     = 10;
	/**TV scan end, start storing */
	public static final int TYPE_SCAN_STORE_BEGIN  = 11;
	/**Store end*/
	public static final int TYPE_SCAN_STORE_END    = 12;
	/**Scan End*/
	public static final int TYPE_SCAN_END          = 13;
	/**正在播放节目相关信息更新*/
	public static final int TYPE_PROGRAM_UPDATE    = 14;
	/**节目开始播放*/
	public static final int TYPE_PROGRAM_START     = 15;
	/**节目停止播放*/
	public static final int TYPE_PROGRAM_STOP      = 16;
	/**TV系统时间更新*/
	public static final int TYPE_TIME_UPDATE       = 17;
	/**事件信息更新*/
	public static final int TYPE_EVENT_UPDATE      = 18;
	/**输入源切换*/
	public static final int TYPE_INPUT_SOURCE_CHANGED = 19;

	private static final String TAG="TVMessage";
	private int type;
	private int programID;
	private int channelID;
	private int bookingID;
	private String cfgName;
	private TVConfigValue cfgValue;
	private int scanProgress; // 0-100
	private int scanTotalChanCount;
	private int scanCurChanNo;
	private TVChannelParams scanCurChanParams;
	private int scanCurChanLocked;	// 0 unlocked, 1 locked
	private String scanProgramName; // Maybe null to indicate that no new program in this update
	private int scanProgramType;
	private int inputSource;

	private int flags;
	private static final int FLAG_PROGRAM_ID = 1;
	private static final int FLAG_CHANNEL_ID = 2;
	private static final int FLAG_BOOKING_ID = 4;
	private static final int FLAG_CONFIG     = 8;
	private static final int FLAG_SCAN       = 16;
	private static final int FLAG_INPUT_SOURCE = 32;

	public static final Parcelable.Creator<TVMessage> CREATOR = new Parcelable.Creator<TVMessage>(){
		public TVMessage createFromParcel(Parcel in) {
			return new TVMessage(in);
		}
		public TVMessage[] newArray(int size) {
			return new TVMessage[size];
		}
	};

	public void readFromParcel(Parcel in){
		type      = in.readInt();
		flags     = in.readInt();

		if((flags & FLAG_PROGRAM_ID) != 0)
			programID = in.readInt();
		if((flags & FLAG_CHANNEL_ID) != 0)
			channelID = in.readInt();
		if((flags & FLAG_BOOKING_ID) != 0)
			bookingID = in.readInt();
		if((flags & FLAG_CONFIG) != 0){
			cfgName  = in.readString();
			cfgValue = new TVConfigValue(in);
		}
		if((flags & FLAG_INPUT_SOURCE) != 0){
			inputSource = in.readInt();
		}
		if ((flags & FLAG_SCAN) != 0 && type == TYPE_SCAN_PROGRESS) {
			scanProgress = in.readInt();
			scanTotalChanCount = in.readInt();
			scanCurChanNo = in.readInt();
			scanCurChanParams = new TVChannelParams(in);
			scanCurChanLocked = in.readInt();
			scanProgramName = in.readString();
			scanProgramType = in.readInt();
		}
	}

	public void writeToParcel(Parcel dest, int flag){
		dest.writeInt(type);
		dest.writeInt(flags);

		if((flags & FLAG_PROGRAM_ID) != 0)
			dest.writeInt(programID);
		if((flags & FLAG_CHANNEL_ID) != 0)
			dest.writeInt(channelID);
		if((flags & FLAG_BOOKING_ID) != 0)
			dest.writeInt(bookingID);
		if((flags & FLAG_CONFIG) != 0){
			dest.writeString(cfgName);
			cfgValue.writeToParcel(dest, flag);
		}
		if((flags & FLAG_INPUT_SOURCE) != 0){
			dest.writeInt(inputSource);
		}

		if((flags & FLAG_SCAN) != 0 && type == TYPE_SCAN_PROGRESS){
			dest.writeInt(scanProgress);
			dest.writeInt(scanTotalChanCount);
			dest.writeInt(scanCurChanNo);
			scanCurChanParams.writeToParcel(dest, flag);
			dest.writeInt(scanCurChanLocked);
			dest.writeString(scanProgramName);
			dest.writeInt(scanProgramType);
		}
	}

	public TVMessage(Parcel in){
		readFromParcel(in);
	}

	public TVMessage(){
	}

	/**
	 *取得消息类型
	 *@return 返回消息类型
	 */
	public int getType(){
		return type;
	}

	/**
	 *取得消息对应服务记录ID
	 *@return 返回服务记录ID
	 */
	public int getProgramID(){
		if((flags & FLAG_PROGRAM_ID) != FLAG_PROGRAM_ID)
			throw new UnsupportedOperationException();

		return programID;
	}

	/**
	 *取得消息对应通道记录ID
	 *@return 返回通道记录ID
	 */
	public int getChannelID(){
		if((flags & FLAG_CHANNEL_ID) != FLAG_CHANNEL_ID)
			throw new UnsupportedOperationException();

		return channelID;
	}

	/**
	 *取得消息对应预约记录ID
	 *@return 返回预约记录ID
	 */
	public int getBookingID(){
		if((flags & FLAG_BOOKING_ID) != FLAG_BOOKING_ID)
			throw new UnsupportedOperationException();

		return bookingID;
	}

	/**
	 *取得搜索进度
	 *@return 返回搜索进度百分比
	 */
	public int getScanProgress() {
		if((flags & FLAG_SCAN) != FLAG_SCAN)
			throw new UnsupportedOperationException();

		return scanProgress;
	}

	/**
	 *搜索中取得搜索到的频道数目
	 *@return 返回搜索到的频道数目
	 */
	public int getScanTotalChanCount() {
		if((flags & FLAG_SCAN) != FLAG_SCAN)
			throw new UnsupportedOperationException();

		return scanTotalChanCount;
	}

	/**
	 *搜索中取得当前频道号
	 *@return 返回当前频道号
	 */
	public int getScanCurChanNo() {
		if((flags & FLAG_SCAN) != FLAG_SCAN)
			throw new UnsupportedOperationException();

		return scanCurChanNo;
	}

	/**
	 *搜索中取得当前频道参数
	 @return 返回当前频道参数
	 */
	public TVChannelParams getScanCurChanParams() {
		if((flags & FLAG_SCAN) != FLAG_SCAN)
			throw new UnsupportedOperationException();

		return scanCurChanParams;
	}

	/**
	 *搜索中取得当前频道锁定状态
	 *@return 返回当前频道锁定状态
	 */
	public int getScanCurChanLockStatus() {
		if((flags & FLAG_SCAN) != FLAG_SCAN)
			throw new UnsupportedOperationException();

		return scanCurChanLocked;
	}

	/**
	 *搜索中取得搜到的节目名称
	 *@return 返回搜到的节目名称
	 */
	public String getScanProgramName() {
		if((flags & FLAG_SCAN) != FLAG_SCAN)
			throw new UnsupportedOperationException();

		return scanProgramName;
	}

	/**
	 *搜索中取得搜到的节目类型
	 *@return 返回搜到的节目类型
	 */
	public int getScanProgramType() {
		if((flags & FLAG_SCAN) != FLAG_SCAN)
			throw new UnsupportedOperationException();

		return scanProgramType;
	}

	/**
	 *取得更改的配置项目名称
	 *@return 返回配置项目名称
	 */
	public String getConfigName(){
		if((flags & FLAG_CONFIG) != FLAG_CONFIG)
			throw new UnsupportedOperationException();

		return cfgName;
	}

	/**
	 *取得更改的配置项目值
	 *@return 返回配置项目值
	 */
	public TVConfigValue getConfigValue(){
		if((flags & FLAG_CONFIG) != FLAG_CONFIG)
			throw new UnsupportedOperationException();

		return cfgValue;
	}

	/**
	 *创建一个ProgramBlock消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage programBlock(int programID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_PROGRAM_ID;
		msg.type = TYPE_PROGRAM_BLOCK;
		msg.programID = programID;

		return msg;
	}

	/**
	 *创建一个ProgramUnblock消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage programUnblock(int programID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_PROGRAM_ID;
		msg.type = TYPE_PROGRAM_UNBLOCK;
		msg.programID = programID;

		return msg;
	}

	/**
	 *创建一个ProgramUpdate消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage programUpdate(int programID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_PROGRAM_ID;
		msg.type = TYPE_PROGRAM_UPDATE;
		msg.programID = programID;

		return msg;
	}

	/**
	 *创建一个ProgramStart消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage programStart(int programID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_PROGRAM_ID;
		msg.type = TYPE_PROGRAM_START;
		msg.programID = programID;

		return msg;
	}

	/**
	 *创建一个ProgramStop消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage programStop(int programID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_PROGRAM_ID;
		msg.type = TYPE_PROGRAM_STOP;
		msg.programID = programID;

		return msg;
	}

	/**
	 *创建一个SignalLost消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage signalLost(int channelID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_CHANNEL_ID;
		msg.type = TYPE_SIGNAL_LOST;
		msg.channelID = channelID;

		return msg;
	}

	/**
	 *创建一个SignalResume消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage signalResume(int channelID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_CHANNEL_ID;
		msg.type = TYPE_SIGNAL_RESUME;
		msg.channelID = channelID;

		return msg;
	}

	/**
	 *创建一个DataLost消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage dataLost(int programID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_PROGRAM_ID;
		msg.type = TYPE_DATA_LOST;
		msg.programID = programID;

		return msg;
	}

	/**
	 *创建一个DataResume消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage dataResume(int programID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_PROGRAM_ID;
		msg.type = TYPE_DATA_RESUME;
		msg.programID = programID;

		return msg;
	}

	/**
	 *创建一个BookingRemind消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage bookingRemind(int bookingID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_BOOKING_ID;
		msg.type = TYPE_BOOKING_REMIND;
		msg.bookingID = bookingID;

		return msg;
	}

	/**
	 *创建一个BookingStart消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage bookingStart(int bookingID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_BOOKING_ID;
		msg.type = TYPE_BOOKING_START;
		msg.bookingID = bookingID;

		return msg;
	}

	public static TVMessage configChanged(String name, TVConfigValue value){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_CONFIG;
		msg.type  = TYPE_CONFIG_CHANGED;
		msg.cfgName  = name;
		msg.cfgValue = value;

		return msg;
	}

	public static TVMessage scanUpdate(int progressVal, int curChan, int totalChan, TVChannelParams curChanParam, 
		int lockStatus, String programName, int programType) {
		TVMessage msg = new TVMessage();
	
		msg.flags = FLAG_SCAN;
		msg.type = TYPE_SCAN_PROGRESS;
		msg.scanProgress = progressVal;
		msg.scanCurChanNo = curChan;
		msg.scanTotalChanCount = totalChan;
		msg.scanCurChanParams = curChanParam;
		msg.scanCurChanLocked = lockStatus;
		msg.scanProgramName = programName;
		msg.scanProgramType = programType;
		
		return msg;
	}

	public static TVMessage scanStoreBegin() {
		TVMessage msg = new TVMessage();
	
		msg.flags = FLAG_SCAN;
		msg.type = TYPE_SCAN_STORE_BEGIN;

		return msg;
	}

	public static TVMessage scanStoreEnd() {
		TVMessage msg = new TVMessage();
	
		msg.flags = FLAG_SCAN;
		msg.type = TYPE_SCAN_STORE_END;

		return msg;
	}

	public static TVMessage scanEnd() {
		TVMessage msg = new TVMessage();
	
		msg.flags = FLAG_SCAN;
		msg.type = TYPE_SCAN_END;

		return msg;
	}

	public static TVMessage timeUpdate(){
		TVMessage msg = new TVMessage();
		msg.type = TYPE_TIME_UPDATE;

		return msg;
	}

	public static TVMessage eventUpdate(){
		TVMessage msg = new TVMessage();
		msg.type = TYPE_EVENT_UPDATE;

		return msg;
	}

	public static TVMessage inputSourceChanged(int src){
		TVMessage msg = new TVMessage();

		msg.type = TYPE_INPUT_SOURCE_CHANGED;
		msg.flags = FLAG_INPUT_SOURCE;
		msg.inputSource = src;

		return msg;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVMessage> getCreator() {
		return CREATOR;
	}
}

