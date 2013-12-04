package com.amlogic.tvutil;

import java.lang.UnsupportedOperationException;
import android.database.Cursor;
import android.content.Context;
import com.amlogic.tvdataprovider.TVDataProvider;
import android.util.Log;

/**
 *TV Channel对应模拟电视中的一个频点，数字电视中的一个频点调制的TS流
 */
public class TVChannel{
	private static final String TAG="TVChannel";
	private Context context;
	private int id;
	private int dvbTSID;
	private int dvbOrigNetID;
	private int fendID;
	private int tsSourceID;
	public TVChannelParams params;

	private void constructFromCursor(Context context, Cursor c){
		int col;
		int src, freq, mod, symb, bw, satid, satpolar,ofdm_mode, layer;

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

			col = c.getColumnIndex("dvbt_flag");
			ofdm_mode = c.getInt(col);

			if(ofdm_mode==TVChannelParams.OFDM_MODE_DVBT)
				this.params = TVChannelParams.dvbtParams(freq, bw);
			else
				this.params = TVChannelParams.dvbt2Params(freq, bw);
		}else if(src == TVChannelParams.MODE_ATSC){
			col = c.getColumnIndex("mod");
			mod = c.getInt(col);
			this.params = TVChannelParams.atscParams(freq, mod);
		}		
		else if(src == TVChannelParams.MODE_ANALOG){
			col = c.getColumnIndex("std");
			int std = c.getInt(col);
			col = c.getColumnIndex("aud_mode");
			int aud_mode = c.getInt(col);
			col = c.getColumnIndex("flags");
			int afc_flag = c.getInt(col);
			this.params = TVChannelParams.analogParams(freq,std, aud_mode,afc_flag);
		}
		else if(src == TVChannelParams.MODE_QPSK){
			col = c.getColumnIndex("symb");
			symb = c.getInt(col);

			col = c.getColumnIndex("db_sat_para_id");
			satid = c.getInt(col);

			col = c.getColumnIndex("polar");
			satpolar = c.getInt(col);			
			
			this.params = TVChannelParams.dvbsParams(context, freq, symb, satid, satpolar);
		}else if(src == TVChannelParams.MODE_DTMB){
			col = c.getColumnIndex("bw");
			bw = c.getInt(col);

			this.params = TVChannelParams.dtmbParams(freq, bw);
		}else if(src == TVChannelParams.MODE_ISDBT){
			col = c.getColumnIndex("bw");
			bw = c.getInt(col);
			/** use the dvbt_flag to store the isdbt layer */
			col = c.getColumnIndex("dvbt_flag");
			layer = c.getInt(col);

			this.params = TVChannelParams.isdbtParams(freq, bw);
			this.params.setISDBTLayer(layer);
		}

		this.fendID = 0;
	}
	
	private TVChannel(Context context, Cursor c){
		constructFromCursor(context, c);
	}
	
	/**
	 *向数据库添加一个Channel
	 */
	public TVChannel(Context context, TVChannelParams p){
		String cmd_select = "select * from ts_table where ts_table.src = " + p.getMode() + " and ts_table.freq = " + p.getFrequency();

		if(p.isDVBSMode()){
			cmd_select += " and ts_table.db_sat_para_id = " + p.getSatId() + " and ts_table.polar = " + p.getPolarisation();
		}
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd_select,
				null, null);
		if(c != null){
			if(c.moveToFirst()&& p.getMode() != TVChannelParams.MODE_ANALOG){
				Log.d("TVChannel","&&&&&&update............................p.getFrequency()="+p.getFrequency());
				/*Using the new params*/
				String cmd = "update ts_table set ";
				int chanID = c.getInt(c.getColumnIndex("db_id"));
				if (p.isDVBCMode()){
					cmd += "symb=" + p.getSymbolRate();
					cmd += ", mod=" + p.getModulation();
				}else if (p.isDVBTMode()){
					cmd += "bw=" + p.getBandwidth();
				}else if (p.isDVBSMode()){
					cmd += "symb=" + p.getSymbolRate();
				}else if (p.isAnalogMode()){
					cmd += "std=" + p.getStandard();
					cmd += ", aud_mode=" + p.getAudioMode();
				}else{
					/*stub*/
					cmd += "freq =" + p.getFrequency();
				}
				

				cmd += " where db_id = " + chanID;
				
				context.getContentResolver().query(TVDataProvider.WR_URL,
					null, cmd, null, null);
				
				/*re-query*/
				c.close();
				c = context.getContentResolver().query(TVDataProvider.RD_URL,
					null,
					"select * from ts_table where db_id=" + chanID,
					null, null);
				if (c != null && c.moveToFirst()){
					/*Construct*/
					constructFromCursor(context, c);
				}else{
					/*Impossible*/
				}
			}else{
				Log.d("TVChannel","....insert............................p.getFrequency()="+p.getFrequency());
				String cmd = "insert into ts_table(src,freq,db_sat_para_id,polar,db_net_id,";
				cmd += "ts_id,symb,mod,bw,snr,ber,strength,std,aud_mode,flags) ";
				cmd += "values("+p.getMode()+","+p.getFrequency()+",";
	
				if (p.isDVBCMode()){
					cmd += "-1,-1,-1,65535,"+p.getSymbolRate()+","+p.getModulation()+",0,0,0,0,0,0,0)";
				}else if (p.isDVBTMode()){
					cmd += "-1,-1,-1,65535,0,0,"+p.getBandwidth()+",0,0,0,0,0,0)";
				}else if (p.isDVBSMode()){
					cmd += p.getSatId() + "," + p.getPolarisation() + ",-1,65535," + p.getSymbolRate() + ",0,0,0,0,0,0,0,0)";
				}else if (p.isAnalogMode()){
					cmd += "-1,-1,-1,65535,0,0,0,0,0,0,"+p.getStandard()+","+p.getAudioMode()+",0)";
				}else if (p.isATSCMode()){
					cmd += "-1,-1,-1,65535,0,0,0,0,0,0,0,0,0)";
				}else{
					cmd += "-1,-1,-1,65535,0,0,0,0,0,0,0,0,0)";
				}
				context.getContentResolver().query(TVDataProvider.WR_URL,
					null, cmd, null, null);
				
				Cursor cr = context.getContentResolver().query(TVDataProvider.RD_URL,
						null,
						"select * from ts_table where ts_table.src = " + p.getMode() +
						" and ts_table.freq = " + p.getFrequency()+
					" order by db_id desc limit 1",						
						null, null);
				if(cr != null){
					if(cr.moveToFirst()){
						/*Construct*/
						constructFromCursor(context, cr);
					}else{
						this.id = -1;
					}
					cr.close();
				}
			}
			c.close();
		}
	}

	/**
	 *创建当前卫星所有通道 
	 *@param sat_id 卫星id
	 *@return 返回创建的通道
	 */
	public static TVChannel[] tvChannelList(Context context, int sat_id){
		TVChannel[] channelList = null;
		int tschannel_count = 0;
		int tschannel_index = 0;

		String cmd = "select * from ts_table where db_sat_para_id = " + sat_id;
		Cursor cur = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		
		tschannel_count = cur.getCount();
		if(tschannel_count > 0){

			if(cur != null){

				if(cur.moveToFirst()){
					channelList = new TVChannel[tschannel_count];
					
					while (!cur.isAfterLast()) {				
						channelList[tschannel_index] = new TVChannel(context, cur);
				
						cur.moveToNext();		

						tschannel_index++;
					}	
				}
				
				cur.close();
			}		     
		}

		return channelList;
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
	 *@param context 当前Context
	 *@param params 频率参数
	 *@return 返回对应的TVChannel对像，null表示不存在该id所对应的对象
	 */
	public static TVChannel selectByParams(Context context, TVChannelParams params){
		TVChannel chan = null;
		String cmd = "select * from ts_table where ts_table.src = " + params.getMode() + " and ts_table.freq = " + params.getFrequency();

		if(params.isDVBSMode()){
			cmd += " and ts_table.db_sat_para_id = " + params.getSatId() + " and ts_table.polar = " + params.getPolarisation();
		}
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				chan = new TVChannel(context, c);
			}
			c.close();
		}

		return chan;
	}

	/*
	 *根据通道id删除通道相关信息 
	 */
	public void tvChannelDel(Context context){
		int ts_db_id = this.id;

		TVProgram.tvProgramDelByChannelID(context, ts_db_id);
	
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from ts_table where db_id = "+ ts_db_id,
				null, null);
		if(c != null){
			c.close();
		}
	}	

	/*
	 *根据卫星id删除通道相关信息 
	 *@param sat_id 卫星id	 	 
	 */
	public static void tvChannelDelBySatID(Context context, int sat_id){
		Log.d(TAG, "tvChannelDelBySatID:" + sat_id);
	
		TVProgram.tvProgramDelBySatID(context, sat_id);
	
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from ts_table where db_sat_para_id = "+ sat_id,
				null, null);
		if(c != null){
			c.close();
		}

		return;
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
	 *设置频率(单位Hz)
	 @param frequency频率
	 */
	public void setFrequency(int frequency){
		params.setFrequency(frequency);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update ts_table set freq=" + frequency + " where db_id = " + id,
			null, null);		
	}

	/**
	 *设置符号率(QPSK/QAM模式)
	 *@param symbolRate 符号率
	 */
	public void setSymbolRate(int symbolRate){
		params.setSymbolRate(symbolRate);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update ts_table set symb=" + symbolRate + " where db_id = " + id,
			null, null);		
	}	

	/**
	 *设置极性(QPSK模式)
	 *@param sat_polarisation 极性
	 */
	public void setPolarisation(int sat_polarisation){
		params.setPolarisation(sat_polarisation);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update ts_table set polar=" + sat_polarisation + " where db_id = " + id,
			null, null);			
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
     *@param freq 是频点
     *@return true 表示修改成功,false表示没有进行修改
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
                ret = true;
            }
        }
        
        return ret;
    }
    
    
    /**
     *修改ATV的afc状态
     *@param data AFC 设定数据
	 *@return true 表示修改成功,false表示没有进行修改
     */
    public boolean setATVAfcData(int data){
        boolean ret = false;
        if(params!=null){
            if(params.getMode() == TVChannelParams.MODE_ANALOG){
                if( params.afc_data != data){
                    params.afc_data = data;
                    context.getContentResolver().query(TVDataProvider.WR_URL,
                            null,
                            "update ts_table set flags=" + params.afc_data + " where db_id = " + id,
                            null, null);
                    ret = true;
                }
            }
        }
        
        return ret;
    }
	
}

