package com.amlogic.tvutil;

import java.lang.UnsupportedOperationException;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *TV 消息
 */
public class TVMessage implements Parcelable{
	/**正在播放的节目因收看等级不够被停止*/
	public static final int TYPE_SERVICE_BLOCK     = 1;
	/**正在播放的节目从BLOCK状态恢复*/
	public static final int TYPE_SERVICE_UNBLOCK   = 2;
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

	private int type;
	private int serviceID;
	private int channelID;
	private int bookingID;
	private String cfgName;
	private TVConfigValue cfgValue;

	private int flags;
	private static final int FLAG_SERVICE_ID = 1;
	private static final int FLAG_CHANNEL_ID = 2;
	private static final int FLAG_BOOKING_ID = 4;
	private static final int FLAG_CONFIG     = 8;

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

		if((flags & FLAG_SERVICE_ID) != 0)
			serviceID = in.readInt();
		if((flags & FLAG_CHANNEL_ID) != 0)
			channelID = in.readInt();
		if((flags & FLAG_BOOKING_ID) != 0)
			bookingID = in.readInt();
		if((flags & FLAG_CONFIG) != 0){
			cfgName  = in.readString();
			cfgValue = new TVConfigValue(in);
		}
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(type);
		dest.writeInt(flags);

		if((flags & FLAG_SERVICE_ID) != 0)
			dest.writeInt(serviceID);
		if((flags & FLAG_CHANNEL_ID) != 0)
			dest.writeInt(channelID);
		if((flags & FLAG_BOOKING_ID) != 0)
			dest.writeInt(bookingID);
		if((flags & FLAG_CONFIG) != 0){
			dest.writeString(cfgName);
			cfgValue.writeToParcel(dest, flags);
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
	public int getServiceID(){
		if((flags & FLAG_SERVICE_ID) != FLAG_SERVICE_ID)
			throw new UnsupportedOperationException();

		return serviceID;
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
	 *创建一个ServiceBlock消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage serviceBlock(int serviceID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_SERVICE_ID;
		msg.type = TYPE_SERVICE_BLOCK;
		msg.serviceID = serviceID;

		return msg;
	}

	/**
	 *创建一个ServiceUnblock消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage serviceUnblock(int serviceID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_SERVICE_ID;
		msg.type = TYPE_SERVICE_UNBLOCK;
		msg.serviceID = serviceID;

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
	public static TVMessage dataLost(int serviceID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_SERVICE_ID;
		msg.type = TYPE_DATA_LOST;
		msg.serviceID = serviceID;

		return msg;
	}

	/**
	 *创建一个DataResume消息
	 *@return 返回创建的新消息
	 */
	public static TVMessage dataResume(int serviceID){
		TVMessage msg = new TVMessage();

		msg.flags = FLAG_SERVICE_ID;
		msg.type = TYPE_DATA_RESUME;
		msg.serviceID = serviceID;

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

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVMessage> getCreator() {
		return CREATOR;
	}
}

