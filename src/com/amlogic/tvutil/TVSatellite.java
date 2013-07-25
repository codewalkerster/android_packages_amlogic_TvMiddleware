package com.amlogic.tvutil;

import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV Satellite相关信息.
 */
public class TVSatellite{
	private static final String TAG="TVSatellite";

	public Context context;
	public int sat_id;
	public String sat_name; 
	public TVSatelliteParams tv_satparams;

	public TVSatellite(){
	}

	public TVSatellite(Context context){
		this.context = context;
	}
	
	private void constructFromCursor(Context context, Cursor cur){
		int numColumn = 0;

		this.context = context;

		this.tv_satparams = new TVSatelliteParams();	

		numColumn= cur.getColumnIndex("db_id");
		int  sat_id = cur.getInt(numColumn);  				
		//Log.d(TAG, "sat_id " + sat_id);			
		//this.setSatelliteId(sat_id);
		this.sat_id = sat_id;

		numColumn = cur.getColumnIndex("sat_name");    		
		String sat_name = cur.getString(numColumn);  		
		//Log.d(TAG, "sat_name " + sat_name);				
		//this.setSatelliteName(sat_name);
		this.sat_name = sat_name;

		numColumn = cur.getColumnIndex("sat_longitude");    
		int  sat_longitude = cur.getInt(numColumn);  		
		//Log.d(TAG, "sat_longitude " + sat_longitude);			
		this.tv_satparams.setSatelliteLongitude(sat_longitude);	

		numColumn = cur.getColumnIndex("lnb_num");
		int lnb_no = cur.getInt(numColumn);
		//Log.d(TAG, "lnb_no " + lnb_no);

		numColumn = cur.getColumnIndex("lof_hi");    
		int  lof_hi= cur.getInt(numColumn);  		
		//Log.d(TAG, "lof_hi" + lof_hi);

		numColumn = cur.getColumnIndex("lof_lo");    
		int  lof_lo = cur.getInt(numColumn);  		
		//Log.d(TAG, "lof_lo " + lof_lo);

		numColumn = cur.getColumnIndex("lof_threshold");    
		int  lof_threshold= cur.getInt(numColumn);  		
		//Log.d(TAG, "lof_threshold " + lof_threshold);			

		this.tv_satparams.setSatelliteLnb(lnb_no, lof_hi, lof_lo, lof_threshold);			

		numColumn = cur.getColumnIndex("voltage");    
		int  voltage = cur.getInt(numColumn);  		
		//Log.d(TAG, "voltage " + voltage);			
		this.tv_satparams.setSecVoltage(voltage);	

		numColumn = cur.getColumnIndex("signal_22khz");    
		int  signal_22khz = cur.getInt(numColumn);  		
		//Log.d(TAG, "signal_22khz" + signal_22khz);			
		this.tv_satparams.setSec22k(signal_22khz);	

		numColumn = cur.getColumnIndex("tone_burst");    
		int  tone_burst = cur.getInt(numColumn);  		
		//Log.d(TAG, "tone_burst " + tone_burst);			
		this.tv_satparams.setSecToneBurst(tone_burst);	
		
		numColumn = cur.getColumnIndex("diseqc_mode");    
		int  diseqc_mode = cur.getInt(numColumn);  		
		//Log.d(TAG, "diseqc_mode " + diseqc_mode);				
		this.tv_satparams.setDiseqcMode(diseqc_mode);

		numColumn = cur.getColumnIndex("committed_cmd");    
		int  lnb_diseqc_mode_config10 = cur.getInt(numColumn);  		
		//Log.d(TAG, "lnb_diseqc_mode_config1.0 " + lnb_diseqc_mode_config10);			
		this.tv_satparams.setDiseqcCommitted(lnb_diseqc_mode_config10);	

		numColumn = cur.getColumnIndex("uncommitted_cmd");    
		int  lnb_diseqc_mode_config11 = cur.getInt(numColumn);  		
		//Log.d(TAG, "lnb_diseqc_mode_config1.1 " + lnb_diseqc_mode_config11);			
		this.tv_satparams.setDiseqcUncommitted(lnb_diseqc_mode_config11);	

		numColumn = cur.getColumnIndex("fast_diseqc");    
		int  fast_diseqc = cur.getInt(numColumn);  		
		//Log.d(TAG, "fast_diseqc " + fast_diseqc);			
		this.tv_satparams.setDiseqcFast(fast_diseqc);

		numColumn = cur.getColumnIndex("repeat_count");    
		int  repeat_count = cur.getInt(numColumn);  		
		//Log.d(TAG, "repeat_count " + repeat_count);			
		this.tv_satparams.setDiseqcRepeatCount(repeat_count);

		numColumn = cur.getColumnIndex("sequence_repeat");    
		int  sequence_repeat = cur.getInt(numColumn);  		
		//Log.d(TAG, "sequence_repeat " + sequence_repeat);			
		this.tv_satparams.setDiseqcSequenceRepeat(sequence_repeat);				

		numColumn = cur.getColumnIndex("cmd_order");    
		int  cmd_order = cur.getInt(numColumn);  		
		//Log.d(TAG, "cmd_order " + cmd_order);
		this.tv_satparams.setDiseqcOrder(cmd_order);
	
		numColumn = cur.getColumnIndex("motor_num");    
		int  motor_num = cur.getInt(numColumn);  		
		//Log.d(TAG, "motor_num " + motor_num);			
		this.tv_satparams.setMotorNum(motor_num);

		numColumn = cur.getColumnIndex("pos_num");    
		int  pos_num = cur.getInt(numColumn);  		
		//Log.d(TAG, "pos_num " + pos_num);			
		this.tv_satparams.setMotorPositionNum(pos_num);

		numColumn = cur.getColumnIndex("longitude");    
		double  longitude = cur.getDouble(numColumn);  		
		//Log.d(TAG, "longitude " + longitude);			

		numColumn = cur.getColumnIndex("latitude");    
		double  latitude = cur.getDouble(numColumn);  		
		//Log.d(TAG, "latitude " + latitude);			
		this.tv_satparams.setSatelliteRecLocal(longitude, latitude);

		this.tv_satparams.setUnicableParams(-1, 0);
		
	}

	private TVSatellite(Context context, Cursor c){
		constructFromCursor(context, c);
	}

	/**
	 *向数据库添加一个TVSatellite
	 */
	public TVSatellite(Context context, String sat_name, double sat_longitude){

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from sat_para_table where sat_longitude = " + sat_longitude,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				/*Construct*/
				constructFromCursor(context, c);
			}else{
				/*add a new satellite to database*/
				String cmd_i = "insert into sat_para_table(sat_name,lnb_num,lof_hi,lof_lo,lof_threshold,signal_22khz,";
				cmd_i += "voltage,motor_num,pos_num,lo_direction,la_direction,longitude,latitude,";
				cmd_i += "sat_longitude,diseqc_mode,tone_burst,committed_cmd,uncommitted_cmd,repeat_count,sequence_repeat,fast_diseqc,cmd_order) ";
				cmd_i += "values('"+ sat_name + "',0"+ ",10600000" + ",9750000" + ",11700000" + "," + TVSatelliteParams.SEC_22k_AUTO;
				cmd_i += "," + TVSatelliteParams.SEC_VOLTAGE_AUTO + ",0,0,0,0,0,0";
				cmd_i += "," + sat_longitude + "," + TVSatelliteParams.DISEQC_MODE_NONE + "," + TVSatelliteParams.SEC_TONE_BURST_NONE;
				cmd_i += "," + TVSatelliteParams.DISEQC_NONE + "," + TVSatelliteParams.DISEQC_NONE + ",0,0,0,0)";
				context.getContentResolver().query(TVDataProvider.WR_URL,
						null, cmd_i, null, null);
				
				String cmd_s = "select * from sat_para_table where sat_longitude = " + sat_longitude;
				Cursor cr = context.getContentResolver().query(TVDataProvider.RD_URL,
						null,
						cmd_s,
						null, null);
				if(cr != null){
					if(cr.moveToFirst()){
						/*Construct*/
						constructFromCursor(context, cr);
					}else{
						this.sat_id = -1;
					}
					cr.close();
				}
			}
			c.close();
		}
		
	}

	/**
	 *创建当前所有TV Satellite相关信息
	 *@return 返回TV Satellite相关信息列表
	 */
	public static TVSatellite[] tvSatelliteList(Context context){
		TVSatellite[] satList = null;
		int sat_count = 0;
		int sat_index = 0;

		String cmd = "select * from sat_para_table";
		Cursor cur = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		
		sat_count = cur.getCount();
		if(sat_count > 0){

			if(cur != null){

				if(cur.moveToFirst()){
					satList = new TVSatellite[sat_count];
					
					while (!cur.isAfterLast()) {				
						satList[sat_index] = new TVSatellite(context, cur);
				
						cur.moveToNext();		

						sat_index++;
					}	
				}
				
				cur.close();
			}		     
		}		

		return satList;
	}

	/**
	 *根据卫星id获取TV Satellite相关信息 
	 *@param sat_id 卫星id	 
	 *@return 返回TV Satellite相关信息
	 */
	public static TVSatellite tvSatelliteSelect(Context context, int sat_id){
		TVSatellite sat = null;

		String cmd = "select * from sat_para_table where db_id = " + sat_id;
		Cursor cur = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		
		if(cur != null){
			if(cur.moveToFirst()){
				sat = new TVSatellite(context, cur);
			}
			cur.close();
		}		

		return sat;
	}	

	/**
	 *根据卫星id删除TV Satellite相关信息 
	 */
	public void tvSatelliteDel(Context context){
		int sat_id = this.sat_id;

		Log.d(TAG, "tvSatelliteDel:" + sat_id);

		TVChannel.tvChannelDelBySatID(context, sat_id);
	
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from sat_para_table where db_id = "+ sat_id,
				null, null);
		if(c != null){
			c.close();
		}	
	}	

	/**
	 *根据卫星id删除TV Satellite相关信息 
	 *@param sat_id 卫星id	 
	 */
	public static void tvSatelliteDel(Context context, int sat_id){
		Log.d(TAG, "tvSatelliteDel:" + sat_id);

		TVChannel.tvChannelDelBySatID(context, sat_id);
	
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from sat_para_table where db_id = "+ sat_id,
				null, null);
		if(c != null){
			c.close();
		}	
	}	

	/**
	 *获取卫星id
	 *@param sat_id 卫星id
	 */
	private void setSatelliteId(int sat_id){
		this.sat_id = sat_id;	
	}

	/**
	 *获取卫星id
	 *@return 返回卫星id
	 */
	public int getSatelliteId(){
		return this.sat_id;
	}
	

	/**
	 *设置卫星名字
	 *@param sat_name 卫星名字
	 */
	public void setSatelliteName(String sat_name){
		this.sat_name = sat_name;

		
		this.context.getContentResolver().query(TVDataProvider.WR_URL,
			null,

			"update sat_para_table set sat_name = '" + sqliteEscape(sat_name) + "' where db_id = " + sat_id,

			null, null);
	} 
	
	/**
	 *设置卫星名字
	 *@return 卫星名字
	 */
	public String getSatelliteName(){
		return this.sat_name;
	}

	/**
	 *取得卫星参数
	 *@return 返回卫星参数
	 */
	public TVSatelliteParams getParams(){
		return this.tv_satparams;
	}	


	/**
	 *设置卫星接收设备本地经纬度
	 *@param local_longitude 本地经度 
	 *@param local_longitude 本地纬度	 
	 */
	public void setSatelliteRecLocal(double local_longitude, double local_latitude){
		tv_satparams.setSatelliteRecLocal(local_longitude, local_latitude);
		
		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set longitude = " + local_longitude + ", latitude = " + local_latitude,
			null, null);
	}	

	/**
	 *设置卫星接收LNB信息
	 *@param lnb_num lnb号 
	 *@param lnb_lof_hi lnb高本振频率	
	 *@param lnb_lof_lo lnb低本振频率 
	 *@param lnb_lof_threadhold lnb转折频率		 
	 */
	public void setSatelliteLnb(int lnb_num, int lnb_lof_hi, int lnb_lof_lo, int lnb_lof_threadhold){
		tv_satparams.setSatelliteLnb(lnb_num, lnb_lof_hi, lnb_lof_lo, lnb_lof_threadhold);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set lnb_num = " + lnb_num + ", lof_hi = " + lnb_lof_hi + ", lof_lo = " + lnb_lof_lo + ", lof_threshold = " + lnb_lof_threadhold + " where db_id = " + sat_id,
			null, null);		
	}

	/**
	 *设置卫星设备控制22k状态
	 *@param sec_22k_status 22k状态
	 */
	public void setSec22k(int sec_22k_status){
		tv_satparams.setSec22k(sec_22k_status);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set signal_22khz = " + sec_22k_status + " where db_id = " + sat_id,
			null, null);		
	}	

	/**
	 *设置卫星设备控制电压状态
	 *@param sec_voltage_status 电压状态
	 */
	public void setSecVoltage(int sec_voltage_status){
		tv_satparams.setSecVoltage(sec_voltage_status);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set voltage = " + sec_voltage_status + " where db_id = " + sat_id,
			null, null);	
	}			

	/**
	 *设置卫星设备控制tone burst
	 *@param sec_tone_burst tone burst 状态
	 */
	public void setSecToneBurst(int sec_tone_burst){
		tv_satparams.setSecToneBurst(sec_tone_burst);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set tone_burst = " + sec_tone_burst + " where db_id = " + sat_id,
			null, null);		
	}	
	
	/**
	 *设置diseqc模式
	 *@param diseqc_mode diseqc模式
	 */
	public void setDiseqcMode(int diseqc_mode){
		tv_satparams.setDiseqcMode(diseqc_mode);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set diseqc_mode = " + diseqc_mode + " where db_id = " + sat_id,
			null, null);		
	}	

	/**
	 *设置diseqc committed	 
	 *@param diseqc_committed diseqc committed
	 */
	public void setDiseqcCommitted(int diseqc_committed){
		tv_satparams.setDiseqcCommitted(diseqc_committed);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set committed_cmd = " + diseqc_committed + " where db_id = " + sat_id,
			null, null);		
	}	

	/**
	 *设置diseqc uncommitted	 
	 *@param diseqc_uncommitted diseqc uncommitted
	 */
	public void setDiseqcUncommitted(int diseqc_uncommitted){
		tv_satparams.setDiseqcUncommitted(diseqc_uncommitted);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set uncommitted_cmd = " + diseqc_uncommitted + " where db_id = " + sat_id,
			null, null);		
	}	

	/**
	 *设置diseqc重复发送次数，用于级联	 
	 *@param diseqc_repeat_count 重复发送次数
	 */
	public void setDiseqcRepeatCount(int diseqc_repeat_count){
		tv_satparams.setDiseqcRepeatCount(diseqc_repeat_count);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set repeat_count = " + diseqc_repeat_count + " where db_id = " + sat_id,
			null, null);			
	}

	/**
	 *设置diseqc发送2次	 
	 *@param diseqc_sequence_repeat 是否发送
	 */
	public void setDiseqcSequenceRepeat(int diseqc_sequence_repeat){
		tv_satparams.setDiseqcSequenceRepeat(diseqc_sequence_repeat);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set sequence_repeat = " + diseqc_sequence_repeat + " where db_id = " + sat_id,
			null, null);		
	}	

	/**
	 *设置快速diseqc 
	 *@param diseqc_fast 是否快速diseqc
	 */
	public void setDiseqcFast(int diseqc_fast){
		tv_satparams.setDiseqcFast(diseqc_fast);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set fast_diseqc = " + diseqc_fast + " where db_id = " + sat_id,
			null, null);		
	}			

	/**
	 *设置diseqc序列 
	 *@param diseqc_order diseqc序列
	 */
	public void setDiseqcOrder(int diseqc_order){
		tv_satparams.setDiseqcOrder(diseqc_order);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set cmd_order = " + diseqc_order + " where db_id = " + sat_id,
			null, null);		
	}	

	/**
	 *设置马达号
	 *@param motor_num motor号 		 
	 */
	public void setMotorNum(int motor_num){
		tv_satparams.setMotorNum(motor_num);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set motor_num = " + motor_num + " where db_id = " + sat_id,
			null, null);			
	}	

	/**
	 *设置马达存储位置号
	 *@param motor_position_num motor存储位置号 		 
	 */
	public void setMotorPositionNum(int motor_position_num){
		tv_satparams.setMotorPositionNum(motor_position_num);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set pos_num = " + motor_position_num + " where db_id = " + sat_id,
			null, null);			
	}

	/**
	 *设置卫星经度	 
	 *@param sat_longitude 卫星经度
	 */
	public void setSatelliteLongitude(double sat_longitude){
		tv_satparams.setSatelliteLongitude(sat_longitude);

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update sat_para_table set sat_longitude = " + sat_longitude + " where db_id = " + sat_id,
			null, null);			
	}

	/**
	 *设置Unicable参数	 
	 *@param user_band Unicable通道 
	 *@param ub_freq Unicable通道频率	 
	 */
	public void setUnicableParams(int user_band, int ub_freq) {
		tv_satparams.setUnicableParams(user_band, ub_freq);
	}	

	/**
	 *获取可用的马达存储位置号 
	 *@param motor_num 马达号，暂不支持 
	 *@param cur_motor_position_num 马达存储位置号	 
	 */
	public int getValidMotorPositionNum(int motor_num, int cur_motor_position_num){
		Cursor cur;
		int posCount = 0;	
		int posIndex = 0;
		int[] posList = null;
		int numColumn = 0;
		int pos = 1;		
		
		Log.d(TAG, "motor_num:" + motor_num + "cur_motor_position_num:" + cur_motor_position_num);

		if(cur_motor_position_num == 0 || cur_motor_position_num == -1){
			cur = context.getContentResolver().query(TVDataProvider.RD_URL,
												null,
												"select * from sat_para_table where motor_num = " + motor_num + " order by pos_num ASC",
												null, null);

			posCount = cur.getCount();
			if(posCount > 0){

				if(cur != null){

					if(cur.moveToFirst()){
						posList = new int[posCount];
						
						while (!cur.isAfterLast()) {	
							numColumn = cur.getColumnIndex("pos_num"); 
							posList[posIndex] = cur.getInt(numColumn);
					
							cur.moveToNext();		

							posIndex++;
						}	
					}
					
					cur.close();
				}		     

				int i, j;
				int conflict = 0;
				for(i = 1; i <= 255; i++){
					conflict = 0;
					
					for(j = 0; j < posCount; j++){
						if(i == posList[j]){
							conflict = 1;
							break;
						}
					}

					if(conflict == 1){
						Log.d(TAG, "conflict " + i);
					}else{
						Log.d(TAG, "new pos " + i);
						pos = i;
						break;
					}
				}
			}	
		}
		else{
			/*
			cur = context.getContentResolver().query(TVDataProvider.RD_URL,
												null,
												"select * from sat_para_table where motor_num = " + motor_num +" and pos_num = "+cur_motor_position_num+ " order by pos_num ASC",
												null, null);
			*/
			
			/*think about motor_num change, we will handle in furture if customer request*/
			pos = cur_motor_position_num;
		}
			    
		return pos;
	}

	public static String sqliteEscape(String keyWord){
		keyWord = keyWord.replace("'", "''");
		return keyWord;
	}	
}

