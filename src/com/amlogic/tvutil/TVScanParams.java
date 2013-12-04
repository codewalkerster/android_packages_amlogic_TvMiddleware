package com.amlogic.tvutil;

import android.util.Log;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
import java.util.ArrayList;
/**
 *搜索参数
 */
public class TVScanParams implements Parcelable {
	/** General TV Scan Mode */
	public static final int TV_MODE_ATV = 0;	// Only search ATV
	public static final int TV_MODE_DTV = 1;	// Only search DTV
	public static final int TV_MODE_ADTV = 2;	// A/DTV will share a same frequency list, like ATSC
	/** DTV scan mode */
	public static final int DTV_MODE_AUTO   = 1;
	public static final int DTV_MODE_MANUAL = 2;
	public static final int DTV_MODE_ALLBAND= 3;
	public static final int DTV_MODE_BLIND  = 4;

	/** DTV scan options, DONOT channge */
	public static final int DTV_OPTION_UNICABLE = 0x10;      //Satellite unicable mode
	public static final int DTV_OPTION_FTA      = 0x20;      //Only store free programs
	public static final int DTV_OPTION_NO_TV    = 0x40;      //Only store tv programs
	public static final int DTV_OPTION_NO_RADIO = 0x80;      //Only store radio programs
	public static final int DTV_OPTION_ISDBT_ONESEG  = 0x100; //Only scan ISDBT layer A
	public static final int DTV_OPTION_ISDBT_FULLSEG = 0x200; //Scan ISDBT full-seg
	
	/** ATV scan mode */
	public static final int ATV_MODE_AUTO   = 1;
	public static final int ATV_MODE_MANUAL = 2;

	private int mode;
	private int fendID;
	/** DTV parameters */
	private int dtvMode;
	private int dtvOptions;
	private int sat_id;
	private TVSatelliteParams tv_satparams;
	private int tsSourceID;
	private TVChannelParams startParams;
	private TVChannelParams chooseListParams[];
	/** ATV parameters */
	private int atvMode;
	private int startFreq;
	private int direction;
    
    private int channelID;

	public static final Parcelable.Creator<TVScanParams> CREATOR = new Parcelable.Creator<TVScanParams>(){
		public TVScanParams createFromParcel(Parcel in) {
			return new TVScanParams(in);
		}
		public TVScanParams[] newArray(int size) {
			return new TVScanParams[size];
		}
	};

	public void readFromParcel(Parcel in){
		mode   = in.readInt();
		fendID = in.readInt();
		if (mode == TV_MODE_DTV || mode == TV_MODE_ADTV) {
			dtvMode = in.readInt();
			dtvOptions = in.readInt();
			tsSourceID = in.readInt();
			if(dtvMode == DTV_MODE_MANUAL)
				startParams = new TVChannelParams(in);
			if((dtvMode == DTV_MODE_BLIND) || (dtvMode == DTV_MODE_ALLBAND)){
				sat_id = in.readInt();
				int satparams_notnull = in.readInt();
				if(satparams_notnull == 1)				
					tv_satparams = new TVSatelliteParams(in);	
			}
			if(dtvMode == DTV_MODE_ALLBAND){
				int length = in.readInt();

				if(length > 0){
					chooseListParams=new TVChannelParams[length];
					in.readTypedArray(chooseListParams, TVChannelParams.CREATOR);
				}
				
			}
		}  else if (mode == TV_MODE_ATV) {
			atvMode = in.readInt();
			startFreq = in.readInt();
			direction = in.readInt();
            channelID = in.readInt();
		}
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(mode);
		dest.writeInt(fendID);
		if (mode == TV_MODE_DTV || mode == TV_MODE_ADTV) {
			dest.writeInt(dtvMode);
			dest.writeInt(dtvOptions);
			dest.writeInt(tsSourceID);
			if(dtvMode == DTV_MODE_MANUAL)
				startParams.writeToParcel(dest, flags);
			if((dtvMode == DTV_MODE_BLIND) || (dtvMode == DTV_MODE_ALLBAND)){
				dest.writeInt(sat_id);
				int satparams_notnull =0;
				if(tv_satparams != null){
					satparams_notnull = 1;
				}else{
					satparams_notnull = 0;
				}					
				dest.writeInt(satparams_notnull);
				if(satparams_notnull == 1)				
					tv_satparams.writeToParcel(dest, flags);
			}			
			if(dtvMode == DTV_MODE_ALLBAND){

				int length = 0;
				if(chooseListParams != null)
					length = chooseListParams.length;
				
				dest.writeInt(length);
				if(length > 0)
					dest.writeTypedArray(chooseListParams, flags);
			}
		} else if (mode == TV_MODE_ATV) {
			dest.writeInt(atvMode);
			dest.writeInt(startFreq);
			dest.writeInt(direction);
            dest.writeInt(channelID);
		}
		
	}

	public TVScanParams() {

	}
	
	public TVScanParams(Parcel in){
		readFromParcel(in);
	}


	public TVScanParams(int mode){
		this.mode = mode;
	}

	
	public int getTvMode() {
		return mode;
	}

	public int getDtvMode() {
		return dtvMode;
	}

	public int getAtvMode() {
		return atvMode;
	}

	public int getTsSourceID() {
		return tsSourceID;
	}

	public int getAtvStartFreq() {
		return startFreq;
	}

	public void setAtvStartFreq(int sf) {
		startFreq = sf;
	}

	public int getAtvChannelID() {
		return channelID;
	}

	public void setAtvChannelID(int chanID) {
		channelID = chanID;
	}
      
	public int getSatID() {
		return sat_id;
	}	

	public TVChannelParams[] getCnannelChooseList() {
		return chooseListParams;
	}

	public void setDtvOptions(int options) {
		dtvOptions = options;
	}

	public int getDtvOptions() {
		return dtvOptions;
	}

	/**
	 *创建新的搜索参数
	 *@param sp 原始参数
	 */
	public TVScanParams(TVScanParams sp){
		mode = sp.mode;
		dtvMode = sp.dtvMode;
		dtvOptions = sp.dtvOptions;
		sat_id = sp.sat_id;
		tv_satparams = sp.tv_satparams;
		fendID = sp.fendID;
		tsSourceID = sp.tsSourceID;
		startParams = sp.startParams;
		chooseListParams = sp.chooseListParams;
		atvMode = sp.atvMode;
		startFreq = sp.startFreq;
		direction = sp.direction;
        channelID = sp.channelID;
	}

	/**
	 *创建手动搜索参数
	 *@param fendID 前端设备参数
	 *@param params 要搜索的频点参数
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams dtvManualScanParams(int fendID, TVChannelParams params){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_MANUAL;
		sp.fendID = fendID;
		sp.startParams = params;

		return sp;
	}

	/**
	 *创建自动搜索参数
	 *@param fendID 前端设备参数
	 *@param mainParams main frequency contains NIT
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams dtvAutoScanParams(int fendID, TVChannelParams mainParams){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_AUTO;
		sp.fendID = fendID;
		sp.startParams = mainParams;

		return sp;
	}

	/**
	 *创建盲搜参数
	 *@param fendID 前端设备参数
	 *@param sat_id Tp属于的卫星id	 
	 *@param tsSourceID TS输入源ID
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams dtvBlindScanParams(int fendID, int sat_id, int tsSourceID){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_BLIND;
		sp.fendID = fendID;
		sp.sat_id = sat_id;
		sp.tsSourceID  = tsSourceID;

		return sp;
	}

	/**
	 *创建盲搜参数
	 *@param fendID 前端设备参数
	 *@param tv_satparams 卫星参数	 
	 *@param tsSourceID TS输入源ID
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams dtvBlindScanParams(int fendID, TVSatelliteParams tv_satparams, int tsSourceID){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_BLIND;
		sp.fendID = fendID;
		sp.tv_satparams = tv_satparams;
		sp.tsSourceID  = tsSourceID;	

		return sp;
	}	

	/**
	 * Get DTV allband scan mode params
	 *@param fendID frontend device number
	 *@param tsSourceID frontend type
	 *@return the new TVScanParams object
	 */
	public static TVScanParams dtvAllbandScanParams(int fendID, int tsSourceID){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_ALLBAND;
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;
        sp.chooseListParams = null;

		return sp;
	}
	
	/**
	 * Get DTV all band scan mode params, user put chanel list
	 *@param fendID frontend device number	 
	 *@param tsSourceID frontend type 
	 *@param channelList channel list	 
	 *@return the new TVScanParams object
	 */
	public static TVScanParams dtvAllbandScanParams(int fendID, int tsSourceID, TVChannelParams[] channelList){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_ALLBAND;
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;
		sp.chooseListParams = channelList;

		if(tsSourceID == TVChannelParams.MODE_QPSK){
			sp.sat_id = channelList[0].sat_id;
			sp.tv_satparams = channelList[0].tv_satparams;	
		}

		return sp;
	}	

	/**
	 * Get DTV all band scan mode params, user put chanel list
	 *@param fendID frontend device number	 
	 *@param tsSourceID frontend type 
	 *@param channelList channel list	 
	 *@return the new TVScanParams object
	 */
	public static TVScanParams dtvAllbandScanParams(int fendID, int tsSourceID, ArrayList<TVChannelParams> channelList){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);
		
		sp.dtvMode = DTV_MODE_ALLBAND;
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;

		TVChannelParams[] channelParaList = null; 
		if(channelList.size()>0){
			channelParaList = new TVChannelParams[channelList.size()];
			for (int i=0; i<channelList.size(); i++) {
				channelParaList[i] = new TVChannelParams(TVChannelParams.MODE_QPSK);
				channelParaList[i].frequency= channelList.get(i).frequency;
				channelParaList[i].symbolRate= channelList.get(i).symbolRate;	
				channelParaList[i].sat_id= channelList.get(i).sat_id;
				channelParaList[i].sat_polarisation= channelList.get(i).sat_polarisation;	
				channelParaList[i].tv_satparams = channelList.get(i).tv_satparams;
			}
			
			sp.chooseListParams = channelParaList;

			if(tsSourceID == TVChannelParams.MODE_QPSK){
				sp.sat_id = channelParaList[0].sat_id;
				sp.tv_satparams = channelParaList[0].tv_satparams;	
			}
		}	

		return sp;
	}	

	/**
	 * Get ATV manual scan mode params
	 *@param fendID frontend device number
	 *@param startFreq the start frequency 
	 *@param direction direction to search
     *@param channelID the start scan chanID
	 *@return the new TVScanParams object
	 */
	public static TVScanParams atvManualScanParams(int fendID, int startFreq, int direction,int channelID){
		TVScanParams sp = new TVScanParams(TV_MODE_ATV);

		sp.atvMode = ATV_MODE_MANUAL;
		sp.fendID = fendID;
		sp.startFreq= startFreq;
		sp.direction = direction;
        sp.channelID = channelID;
		return sp;
	}

	/**
	 * Get ATV manual scan mode params, search from the playing channel
	 *@param fendID frontend device number
	 *@param direction direction to search
	 *@return the new TVScanParams object
	 */
	public static TVScanParams atvManualScanParams(int fendID, int direction){
		TVScanParams sp = new TVScanParams(TV_MODE_ATV);

		sp.atvMode = ATV_MODE_MANUAL;
		sp.fendID = fendID;
		sp.startFreq= 0;
		sp.direction = direction;
        sp.channelID = -1;
		return sp;
	}

	/**
	 * Get ATV auto scan mode params
	 *@param fendID frontend device number
	 *@return the new TVScanParams object
	 */
	public static TVScanParams atvAutoScanParams(int fendID){
		TVScanParams sp = new TVScanParams(TV_MODE_ATV);

		sp.atvMode = ATV_MODE_AUTO;
		sp.fendID = fendID;

		return sp;
	}
	
	/**
	 * 创建ATV/DTV一起搜索模式参数
	 *@param fendID 前端设备号
	 *@param dtvTsSourceID DTV TS输入源ID
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams adtvScanParams(int fendID, int dtvTsSourceID){
		TVScanParams sp = new TVScanParams(TV_MODE_ADTV);

		sp.dtvMode = DTV_MODE_ALLBAND;
		sp.fendID = fendID;
		sp.tsSourceID  = dtvTsSourceID;

		return sp;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVScanParams> getCreator() {
		return CREATOR;
	}
}

