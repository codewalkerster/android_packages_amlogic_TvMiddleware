package com.amlogic.tvutil;

import java.util.ArrayList;
import java.util.HashSet;
import android.database.Cursor;
import android.content.Context;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV 区域
 *可以获得每个区域的频率列表等信息
 */
public class TVRegion{
	private Context context;
	private int id;
	private int sourceMode;
	private String name;
	private String country;
	private String source;
	private TVChannelParams[] channels; 

	TVRegion(Context context, Cursor c){
		this.context = context;

		int col;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);
		
		col = c.getColumnIndex("fe_type");
		this.sourceMode = c.getInt(col);

		col = c.getColumnIndex("name");
		this.name = c.getString(col);
		this.country = name.substring(0, name.indexOf(','));
		this.source = name.substring(name.indexOf(',') + 1);

		Cursor cur = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from region_table where name = '" + sqliteEscape(name) + "' and fe_type = " + sourceMode,
				null, null);
		if(cur != null){
			if(cur.moveToFirst()){
				int frequency = 0;
				int bandwidth;
				int modulation;
				int symbolRate;
				int ofdmMode;
				int channelCount = 0;
				
				this.channels = new TVChannelParams[cur.getCount()];
				
				do{
					col = cur.getColumnIndex("frequency");
					frequency = cur.getInt(col);
					col = cur.getColumnIndex("modulation");
					modulation = cur.getInt(col);
					col = cur.getColumnIndex("bandwidth");
					bandwidth = cur.getInt(col);
					col = cur.getColumnIndex("symbol_rate");
					symbolRate = cur.getInt(col);
					col = cur.getColumnIndex("ofdm_mode");
					ofdmMode = cur.getInt(col);

					switch (sourceMode){
						case TVChannelParams.MODE_QAM:
							channels[channelCount++] = TVChannelParams.dvbcParams(frequency, modulation, symbolRate);
							break;
						case TVChannelParams.MODE_OFDM:
							channels[channelCount++] = TVChannelParams.dvbtParams(frequency,bandwidth);
							break;
						case TVChannelParams.MODE_ATSC:
							channels[channelCount++] = TVChannelParams.atscParams(frequency, modulation);
							break;
						case TVChannelParams.MODE_ANALOG:
							channels[channelCount++] = TVChannelParams.analogParams(frequency, 0, 0, 0);
							break;
						case TVChannelParams.MODE_DTMB:
							channels[channelCount++] = TVChannelParams.dtmbParams(frequency,bandwidth);
							break;
						default:
							break;
					}
				}while (cur.moveToNext());
			}
			cur.close();
		}
	}

	/**
	 *根据记录ID取得对应的TVRegion
	 *@param context 当前Context
	 *@param id 记录ID
	 *@return 返回ID对应的TVRegion对象
	 */
	public static TVRegion selectByID(Context context, int id){
		TVRegion e = null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from region_table where region_table.db_id = " + id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				e = new TVRegion(context, c);
			}
			c.close();
		}

		return e;
	}

	public static String sqliteEscape(String keyWord){
		keyWord = keyWord.replace("'", "''");
		return keyWord;
	}	
	
	/**
	 *根据Region name取得对应的TVRegion
	 *@param context 当前Context
	 *@param name Region名称
	 *@return 返回name对应的TVRegion对象
	 */
	public static TVRegion selectByName(Context context, String name){
		TVRegion e = null;
		

		if (name == null)
			return null;
		String cmd;
		cmd = "select * from region_table where name = "+ "\'"+sqliteEscape(name)+"\'";
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				e = new TVRegion(context, c);
			}
			c.close();
		}

		return e;
	}
	
	/**
	 *取得一个国家的所有TVRegion
	 *@param context 当前Context
	 *@param countryName 国家名称
	 *@return 返回countryName对应的TVRegion对象数组
	 */
	public static TVRegion[] selectByCountry(Context context, String countryName){
		TVRegion[] ret = null;

		if (countryName == null)
			return null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null, "select * from region_table", null, null);
		if(c != null){
			if(c.moveToFirst()){
				int col;
				String name;
				HashSet set = new HashSet();
				ArrayList list = new ArrayList();
				do{
					col = c.getColumnIndex("name");
					name = c.getString(col);
					if (name.contains(countryName) && set.add(name)){
						list.add(new TVRegion(context, c));
					}
				}while(c.moveToNext());
				ret = (TVRegion[])list.toArray(new TVRegion[0]);
			}
			c.close();
		}

		return ret;
	}
	
	/**
	 *取得当前支持的所有国家名称
	 *@param context 当前Context
	 *@return 返回国家名称数组
	 */
	public static String[] getAllCountry(Context context){
		String[] ret = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null, "select * from region_table", null, null);
		if(c != null){
			if(c.moveToFirst()){
				int col;
				String name;
				String countryName;
				HashSet set = new HashSet();
				do{
					col = c.getColumnIndex("name");
					name = c.getString(col);
					countryName = name.substring(0, name.indexOf(','));
					set.add(countryName);
				}while(c.moveToNext());
				ret = (String[])set.toArray(new String[0]);
			}
			c.close();
		}

		return ret;
	}

	/**
	 *取得当前支持的ATSC表名称
	 *@param context 当前Context
	 *@return 返回名称数组
	 */
	public static String[] getCountryByATSC(Context context){
		String[] ret = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null, "select * from region_table", null, null);
		if(c != null){
			if(c.moveToFirst()){
				int col;
				String name;
				String countryName;
				HashSet set = new HashSet();
				do{
					col = c.getColumnIndex("name");
					name = c.getString(col);
					//countryName = name.substring(0, name.indexOf(','));

					if (name.contains("ATSC")){
						set.add(name);
					}
					
				}while(c.moveToNext());
				ret = (String[])set.toArray(new String[0]);
			}
			c.close();
		}

		return ret;
	}

	/**
	 *取得当前支持的DVBT国家名称
	 *@param context 当前Context
	 *@return 返回名称数组
	 */
	public static String[] getCountryByDVBT(Context context){
		String[] ret = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null, "select * from region_table", null, null);
		if(c != null){
			if(c.moveToFirst()){
				int col;
				String name;
				String countryName;
				HashSet set = new HashSet();
				do{
					col = c.getColumnIndex("name");
					name = c.getString(col);

					if (name.contains("DVB-T")){
						countryName = name.substring(0, name.indexOf(','));
						set.add(countryName);
					}
					
				}while(c.moveToNext());
				ret = (String[])set.toArray(new String[0]);
			}
			c.close();
		}

		return ret;
	}

	/**
	 *取得Region的ID
	 *@return 返回Region的ID
	 */
	public int getID(){
		return id;
	}

	/**
	 *取得Region名
	 *@return 返回Region名
	 */
	public String getName(){
		return name;
	}

	/**
	 *取得该Region所在国家名称
	 *@return 返回国家名称
	 */
	public String getCountry(){
		return country;
	}

	/**
	 *取得该Region的信号源名称
	 *@return 返回信号源名称
	 */
	public String getSource(){
		return source;
	}

	/**
	 *取得该Region的信号源类型
	 *@return 返回信号源类型
	 */
	public int getSourceMode(){
		return sourceMode;
	}

	/**
	 *取得该Region的信号源频率参数列表
	 *@return 返回信号源频率参数列表
	 */
	public TVChannelParams[] getChannelParams(){
		return channels;
	}

	/**
	 *根据频率序号取得频率参数，序号从1开始
	 *@return 返回索频率参数对象
	 */
	public TVChannelParams getChannelParams(int channelNo){
		TVChannelParams ch = null;

		if (channels != null && channelNo >= 1 && channelNo <= channels.length){
			ch = channels[channelNo-1];
		}

		return ch;
	}
	
	/**
	 *取得一个频率在该Region的频率序号，序号从1开始
	 *@return 返回索引值,<=0 表示不存在该频率
	 */
	public int getChannelNo(int frequency){
		int ret = -1;
		
		if (channels != null){
			for (int i=0; i<channels.length; i++){
				if (channels[i].getFrequency() == frequency){
					ret = i+1;
					break;
				}
			}
		}
		
		return ret;
	}
}
