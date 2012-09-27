package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *TV 配置值
 */
public class TVConfigValue implements Parcelable{
	private static final String TAG="TVConfigValue";

	/**未知类型*/
	public static final int TYPE_UNKNOWN = 0;
	/**字符串类型*/
	public static final int TYPE_STRING = 1;
	/**整数类型*/
	public static final int TYPE_INT    = 2;
	/**布尔型*/
	public static final int TYPE_BOOL   = 3;
	/**整数数组型*/
	public static final int TYPE_INT_ARRAY = 4;

	private int     type;
	private String  strValue;
	private int     intValue;
	private boolean boolValue;
	private int     intArrayValue[];

	/**配置项类型不匹配异常*/
	public class TypeException extends Exception{
	}

	public static final Parcelable.Creator<TVConfigValue> CREATOR = new Parcelable.Creator<TVConfigValue>(){
		public TVConfigValue createFromParcel(Parcel in) {
			return new TVConfigValue(in);
		}
		public TVConfigValue[] newArray(int size) {
			return new TVConfigValue[size];
		}
	};

	public void readFromParcel(Parcel in){
		type      = in.readInt();
		switch(type){
			case TYPE_INT:
				this.intValue = in.readInt();
				break;
			case TYPE_BOOL:
				this.boolValue = (in.readInt()!=0);
				break;
			case TYPE_STRING:
				this.strValue = in.readString();
				break;
			case TYPE_INT_ARRAY:
				int size = in.readInt();
				if(size != 0){
					this.intArrayValue = new int[size];
					in.readIntArray(this.intArrayValue);
				}
				break;

		}
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(type);
		switch(type){
			case TYPE_INT:
				dest.writeInt(intValue);
				break;
			case TYPE_BOOL:
				dest.writeInt(boolValue?1:0);
				break;
			case TYPE_STRING:
				dest.writeString(strValue);
				break;
			case TYPE_INT_ARRAY:
				if(intArrayValue != null){
					dest.writeInt(intArrayValue.length);
					dest.writeIntArray(intArrayValue);
				}else{
					dest.writeInt(0);
				}
				break;

		}
	}

	public TVConfigValue(Parcel in){
		readFromParcel(in);
	}

	public TVConfigValue(int v){
		this.type = TYPE_INT;
		this.intValue = v;
	}

	public TVConfigValue(String v){
		this.type = TYPE_STRING;
		this.strValue = v;
	}

	public TVConfigValue(boolean v){
		this.type = TYPE_BOOL;
		this.boolValue = v;
	}

	public TVConfigValue(int v[]){
		this.type = TYPE_INT_ARRAY;
		this.intArrayValue = v;
	}

	public TVConfigValue(){
		this.type = TYPE_UNKNOWN;
	}

	public TVConfigValue(TVConfigValue v){
		this.type = v.type;

		switch(v.type){
			case TYPE_INT:
				this.intValue = v.intValue;
				break;
			case TYPE_BOOL:
				this.boolValue = v.boolValue;
				break;
			case TYPE_STRING:
				this.strValue = v.strValue;
				break;
			case TYPE_INT_ARRAY:
				if(v.intArrayValue != null)
					this.intArrayValue = (int[])v.intArrayValue.clone();
				break;
		}
	}

	public int getType(){
		return type;
	}

	public int getInt() throws TypeException{
		if(type != TYPE_INT)
			throw new TypeException();
		return intValue;
	}

	public boolean getBoolean() throws TypeException{
		if(type != TYPE_BOOL)
			throw new TypeException();
		return boolValue;
	}

	public String getString() throws TypeException{
		if(type != TYPE_STRING)
			throw new TypeException();
		return strValue;
	}

	public int[] getIntArray() throws TypeException{
		if(type != TYPE_INT_ARRAY)
			throw new TypeException();
		return intArrayValue;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVConfigValue> getCreator() {
		return CREATOR;
	}
}

