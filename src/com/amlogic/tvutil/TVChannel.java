package com.amlogic.tvutil;

import java.lang.UnsupportedOperationException;
import android.database.Cursor;
import android.content.Context;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV Channel对应模拟电视中的一个频点，数字电视中的一个频点调制的TS流
 */
public class TVChannel{
	private Context context;
	private int id;
	private int dvbTSID;
	private int dvbOrigNetID;
	private int fendID;
	private int tsSourceID;
	private TVChannelParams params;

	private TVChannel(Context context, Cursor c){
		int col;
		int src, freq, mod, symb, bw;

		this.context = context;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);

		col = c.getColumnIndex("ts_id");
		this.dvbTSID = c.getInt(col);

		col = c.getColumnIndex("src");
		src = c.getInt(col);

		col = c.getColumnIndex("freq");
		freq = c.getInt(col);

		if(src == TVChannelParams.MODE_QAM){
			col = c.getColumnIndex("mod");
			mod = c.getInt(col);

			col = c.getColumnIndex("symb");
			symb = c.getInt(col);

			this.params = TVChannelParams.dvbcParams(freq, mod, symb);
		}else if(src == TVChannelParams.MODE_OFDM){
			col = c.getColumnIndex("bw");
			bw = c.getInt(col);

			this.params = TVChannelParams.dvbtParams(freq, bw);
		}
		else if(src == TVChannelParams.MODE_ANALOG){
			col = c.getColumnIndex("std");
			int std = c.getInt(col);
			col = c.getColumnIndex("aud_mode");
			int aud_mode = c.getInt(col);
			this.params = TVChannelParams.analogParams(freq,std, aud_mode);
		}

		this.fendID = 0;
	}

	/**
	 *根据记录ID查找对应的TVChannel
	 *@param context 当前Context
	 *@param id 记录ID
	 *@return 返回对应的TVChannel对像，null表示不存在该id所对应的对象
	 */
	public static TVChannel selectByID(Context context, int id){
		TVChannel chan = null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from ts_table where ts_table.db_id = " + id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				chan = new TVChannel(context, c);
			}
			c.close();
		}

		return chan;
	}

	/**
	 *取得Channel ID
	 *@return 返回Channel ID
	 */
	public int getID(){
		return id;
	}

	/**
	 *取得DVB 传输流ID
	 *@return 返回传输流ID
	 */
	public int getDVBTSID(){
		if(params!=null && !params.isDVBMode())
			throw new UnsupportedOperationException();

		return dvbTSID;
	}

	/**
	 *取得DVB原始网络ID
	 *@return 返回原始网络ID
	 */
	public int getDVBOrigNetID(){
		if(params!=null && !params.isDVBMode())
			throw new UnsupportedOperationException();

		return dvbOrigNetID;
	}

	/**
	 *取得前端设备ID
	 *@return 返回前端设备ID.
	 */
	public int getFrontendID(){
		return fendID;
	}

	/**
	 *取得TS输入源ID
	 *@return 返回前端输入源ID
	 */
	public int getTSSourceID(){
		return tsSourceID;
	}

	/**
	 *取得频道参数
	 *@return 返回频道参数
	 */
	public TVChannelParams getParams(){
		return params;
	}

	/**
	 *判断是否为DVBC模式
	 *@return true表示是DVBC模式，false表示不是DVBC模式
	 */
	public boolean isDVBCMode(){
		if((params!=null) && params.isDVBCMode())
			return true;

		return false;
	}

	/**
	 *判断是否为DVBT模式
	 *@return true表示是DVBT模式，false表示不是DVBT模式
	 */
	public boolean isDVBTMode(){
		if((params!=null) && params.isDVBTMode())
			return true;

		return false;
	}

	/**
	 *判断是否为DVBS模式
	 *@return true表示是DVBS模式，false表示不是DVBS模式
	 */
	public boolean isDVBSMode(){
		if((params!=null) && params.isDVBSMode())
			return true;

		return false;
	}

	/**
	 *判断是否为DVB模式
	 *@return true表示是DVB模式，false表示不是DVB模式
	 */
	public boolean isDVBMode(){
		if((params!=null) && params.isDVBMode())
			return true;

		return false;
	}

	/**
	 *判断是否为ATSC模式
	 *@return true表示是ATSC模式，false表示不是ATSC模式
	 */
	public boolean isATSCMode(){
		if((params!=null) && params.isATSCMode())
			return true;

		return false;
	}

	/**
	 *判断是否为模拟模式
	 *@return true表示是模拟模式，false表示不是模拟模式
	 */
	public boolean isAnalogMode(){
		if((params!=null) && params.isAnalogMode())
			return true;

		return false;
	}
	
	/**
	 *修改模拟音频
	 *@return true 表示已经修改,false表示制式已经设置无需修改
	 */
	public boolean setATVAudio(int audio){
		boolean ret = false;
		if(params!=null){
			ret = params.setATVAudio(audio);
		}
		if (ret){
			context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update ts_table set aud_mode=" + audio + " where db_id = " + id,
				null, null);
		}
		
		return ret;
	}

	/**
	 *修改模拟视频制式
	 *@param fmt 视频制式
	 *@return true 表示已经修改制式,false表示制式已经设置无需修改
	 */
	public boolean setATVVideoFormat(TVConst.CC_ATV_VIDEO_STANDARD fmt){
		boolean ret = false;
		if(params!=null){
			ret = params.setATVVideoFormat(fmt);
		}
		if (ret){
			context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update ts_table set std=" + params.getStandard() + " where db_id = " + id,
				null, null);
		}
		
		return ret;
	}

	/**
	 *修改模拟音频制式
	 *@param fmt 音频制式
	 *@return true 表示已经修改制式,false表示制式已经设置无需修改
	 */
	public boolean setATVAudioFormat(TVConst.CC_ATV_AUDIO_STANDARD fmt){
		boolean ret = false;
		if(params!=null){
			ret = params.setATVAudioFormat(fmt);
		}
		if (ret){
			context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update ts_table set std=" + params.getStandard() + " where db_id = " + id,
				null, null);
		}
		
		return ret;
	}
	
	
	/**
     *修改ATV的频点
     *@param fre是频点
     *@return true 表示已经修改制式,false表示制式已经设置无需修改
     */
    public boolean setATVFreq(int freq){
        boolean ret = false;
        if(params!=null){
            if(params.getMode() == TVChannelParams.MODE_ANALOG){
                params.frequency = freq;
                context.getContentResolver().query(TVDataProvider.WR_URL,
                    null,
                    "update ts_table set freq=" + params.frequency + " where db_id = " + id,
                    null, null);
            }
        }
        
        return ret;
    }
	
}

