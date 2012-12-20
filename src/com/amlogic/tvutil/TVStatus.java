package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 *TV状态
 */
public class TVStatus implements Parcelable{
	public int programType;
	public TVProgramNumber programNo;
	public int programID;

	public static final Parcelable.Creator<TVStatus> CREATOR = new Parcelable.Creator<TVStatus>(){
		public TVStatus createFromParcel(Parcel in) {
			return new TVStatus(in);
		}
		public TVStatus[] newArray(int size) {
			return new TVStatus[size];
		}
	};

	public void readFromParcel(Parcel in){
		int have_no;

		programType = in.readInt();
		programID = in.readInt();

		have_no = in.readInt();
		if(have_no != 0){
			programNo = new TVProgramNumber(in);
		}
	}

	public void writeToParcel(Parcel dest, int flag){
		dest.writeInt(programType);
		dest.writeInt(programID);
		if(programNo == null){
			dest.writeInt(0);
		}else{
			dest.writeInt(1);
			programNo.writeToParcel(dest, flag);
		}
	}

	public TVStatus(Parcel in){
		readFromParcel(in);
	}

	public TVStatus(){
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVStatus> getCreator() {
		return CREATOR;
	}
}

