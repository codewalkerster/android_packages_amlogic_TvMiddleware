package com.amlogic.tvutil;

import java.util.Date;
import java.util.ArrayList;
import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import com.amlogic.tvdataprovider.TVDataProvider;
/**
 *预约记录
 */
public class TVBooking{
	private static final String TAG="TVBooking";
	
	/**预约播放*/
	public static final int FL_PLAY   = 0x1; 
	/**预约录像*/
	public static final int FL_RECORD = 0x2;
	
	/**预约状态: 等待开始，尚未临近开始时间*/
	public static final int ST_NOT_START = 0;
	/**预约状态: 临近开始时间，且得到用户确认，待到达开始时间后自动开始*/
	public static final int ST_CONFIRMED = 1;
	/**预约状态: 预约指定的操作已经开始*/
	public static final int ST_STARTED   = 2;
	/**预约状态: 预约指定的操作已经结束*/
	public static final int ST_END       = 3;

	/**预约错误码: 预约参数错误*/
	public static final int ERR_PARAM      = -1;
	/**预约错误码: 预约时间段冲突*/
	public static final int ERR_CONFLICT   = -2;
	
	private int id;
	private int status;
	private int flag;
	private long start;
	private long duration;
	private Context context;
	private TVProgram program;
	private String recFilePath;
	private String recStoragePath;
	private String programName;
	private String eventName;
	private TVProgram.Video video;
	private TVProgram.Audio audios[];
	private TVProgram.Subtitle subtitles[];
	private TVProgram.Teletext teletexts[];
	
	
	public TVBooking(Context context, Cursor c){
		int col;
		int pid, fmt, i;
		String tmpStr;
		TVProgram p;

		this.context = context;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);
		col = c.getColumnIndex("db_srv_id");
		this.program = TVProgram.selectByID(context, c.getInt(col));
		col = c.getColumnIndex("db_evt_id");
		TVEvent event = TVEvent.selectByID(context, c.getInt(col));
		col = c.getColumnIndex("status");
		this.status = c.getInt(col);
		col = c.getColumnIndex("flag");
		this.flag = c.getInt(col);
		col = c.getColumnIndex("start");
		this.start = (long)c.getInt(col)*1000;
		col = c.getColumnIndex("duration");
		this.duration = (long)c.getInt(col)*1000;
		col = c.getColumnIndex("srv_name");
		this.programName = c.getString(col);
		col = c.getColumnIndex("evt_name");
		this.eventName = c.getString(col);
		col = c.getColumnIndex("file_name");
		this.recFilePath = c.getString(col);
		col = c.getColumnIndex("from_storage");
		this.recStoragePath = c.getString(col);
		
		p = program;
		if (p == null) {
			p = new TVProgram();
		}
		
		/**Video*/
		col = c.getColumnIndex("vid_pid");
		pid = c.getInt(col);
		col = c.getColumnIndex("vid_fmt");
		fmt = c.getInt(col);
		this.video = p.new Video(pid, fmt);
		
		/**Audios*/
		col = c.getColumnIndex("aud_pids");
		tmpStr = c.getString(col);
		String apids[] = tmpStr.split(" ");
		col = c.getColumnIndex("aud_fmts");
		tmpStr = c.getString(col);
		String afmts[] = tmpStr.split(" ");
		col = c.getColumnIndex("aud_languages");
		tmpStr = c.getString(col);
		String alangs[] = tmpStr.split(" ");
		if (apids != null && apids.length > 0 && !apids[0].isEmpty()) {
			this.audios = new TVProgram.Audio[apids.length];
			for (i=0; i<apids.length; i++) {
				audios[i] = p.new Audio(
					Integer.parseInt(apids[i]), 
					alangs[i], 
					Integer.parseInt(afmts[i]));
			}
		} else {
			this.audios = null;
		}
		
		/**Subtitles*/
		ArrayList subList = new ArrayList();
		col = c.getColumnIndex("sub_pids");
		tmpStr = c.getString(col);
		String spids[] = tmpStr.split(" ");
		col = c.getColumnIndex("sub_types");
		tmpStr = c.getString(col);
		String stypes[] = tmpStr.split(" ");
		col = c.getColumnIndex("sub_composition_page_ids");
		tmpStr = c.getString(col);
		String scpgids[] = tmpStr.split(" ");
		col = c.getColumnIndex("sub_ancillary_page_ids");
		tmpStr = c.getString(col);
		String sapgids[] = tmpStr.split(" ");
		col = c.getColumnIndex("sub_languages");
		tmpStr = c.getString(col);
		String slangs[] = tmpStr.split(" ");
		if (spids != null && spids.length > 0 && !spids[0].isEmpty()) {
			for (i=0; i<spids.length; i++) {
				subList.add(p.new Subtitle(
					Integer.parseInt(spids[i]),
					slangs[i],
					Integer.parseInt(stypes[i]),
					Integer.parseInt(scpgids[i]),
					Integer.parseInt(sapgids[i])));
			}
		}
		
		/**Teletexts*/
		ArrayList ttxList = new ArrayList();
		col = c.getColumnIndex("ttx_pids");
		tmpStr = c.getString(col);
		String tpids[] = tmpStr.split(" ");
		col = c.getColumnIndex("ttx_magazine_numbers");
		tmpStr = c.getString(col);
		String tmagnums[] = tmpStr.split(" ");
		col = c.getColumnIndex("ttx_page_numbers");
		tmpStr = c.getString(col);
		String tpgnums[] = tmpStr.split(" ");
		col = c.getColumnIndex("ttx_languages");
		tmpStr = c.getString(col);
		String tlangs[] = tmpStr.split(" ");
		if (tpids != null && tpids.length > 0 && !tpids[0].isEmpty()) {
			for (i=0; i<tpids.length; i++) {
				ttxList.add(p.new Teletext(
					Integer.parseInt(tpids[i]),
					tlangs[i],
					Integer.parseInt(tmagnums[i]),
					Integer.parseInt(tpgnums[i])));
			}
		}
		this.subtitles = (TVProgram.Subtitle[])subList.toArray(new TVProgram.Subtitle[0]);
		this.teletexts = (TVProgram.Teletext[])ttxList.toArray(new TVProgram.Teletext[0]);
	}
	
	public TVBooking(TVProgram program, long start, long duration){
		this.id = -1;
		this.program = program;
		this.start = start;
		this.duration = duration;
		this.programName = program.getName();
		this.eventName = "";
		this.video = program.getVideo();
		this.audios = program.getAllAudio();
		this.subtitles = program.getAllSubtitle();
		this.teletexts = program.getAllTeletext();
	}
	
	public static String sqliteEscape(String keyWord){
		keyWord = keyWord.replace("'", "''");
		return keyWord;
	}
	
	/**
	 *根据记录ID查找指定TVBooking
	 *@param context 当前Context
	 *@param id 记录ID
	 *@return 返回对应的TVBooking对象，null表示不存在该id所对应的对象
	 */
	public static TVBooking selectByID(Context context, int id){
		TVBooking p = null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where db_id = " + id, null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVBooking(context, c);
			}
			c.close();
		}

		return p;
	}
	
	public static TVBooking[] selectAllPlayBookings(Context context){
		TVBooking bookings[] = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where (flag & "+FL_PLAY+") != 0" , null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				bookings = new TVBooking[c.getCount()];
				do{
					bookings[id++] = new TVBooking(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}
		
		return bookings;
	}
	
	public static TVBooking[] selectAllRecordBookings(Context context){
		TVBooking bookings[] = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where (flag & "+FL_RECORD+") != 0" , null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				bookings = new TVBooking[c.getCount()];
				do{
					bookings[id++] = new TVBooking(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}
		
		return bookings;
	}
	
	public static int bookProgram(Context context, TVProgram program, int flag, long start, long duration){
		if (program == null || start < 0 ||
			((flag&FL_PLAY)==0 && (flag&FL_RECORD)==0)) {
			Log.d(TAG, "Invalid param for booking program");
			return ERR_PARAM;	
		}
		
		int status;
		if (duration <= 0) {
			/* can only be used for real-time record or timeshifting record */
			status = ST_STARTED;
		} else {
			/*check conflict*/
			status = ST_NOT_START;
		}
		
		/*book this program*/
		String cmd = "insert into booking_table(db_srv_id, db_evt_id, srv_name, evt_name,";
		cmd += "start,duration,flag,status,file_name,vid_pid,vid_fmt,aud_pids,aud_fmts,aud_languages,";
		cmd += "sub_pids,sub_types,sub_composition_page_ids,sub_ancillary_page_ids,sub_languages,";
		cmd += "ttx_pids,ttx_types,ttx_magazine_numbers,ttx_page_numbers,ttx_languages, other_pids,from_storage)";
		cmd += "values("+program.getID()+",-1,'"+sqliteEscape(program.getName());
		cmd += "','',"+start/1000+","+duration/1000+","+flag;
		cmd += ","+status+",'',"+program.getVideo().getPID()+","+program.getVideo().getFormat();

		TVProgram.Audio auds[] = program.getAllAudio();
		String apids="", afmts="", alangs="";
		if (auds != null && auds.length > 0) {
			apids  += auds[0].getPID();
			afmts  += auds[0].getFormat();
			alangs += auds[0].getLang();
			for (int i=1; i<auds.length; i++){
				apids  += " "+auds[i].getPID();
				afmts  += " "+auds[i].getFormat();
				alangs += " "+auds[i].getLang();
			}
		}
		cmd += ",'"+apids+"','"+afmts+"','"+alangs+"'";
		
		TVProgram.Subtitle subs[] = program.getAllSubtitle();
		String spids="", stypes="", scpgids="", sapgids="", slangs="";
		if (subs != null && subs.length > 0) {
			spids   += subs[0].getPID();
			stypes  += subs[0].getType();
			scpgids += subs[0].getCompositionPageID();
			sapgids += subs[0].getAncillaryPageID();
			slangs  += subs[0].getLang();
			for (int i=1; i<subs.length; i++){
				spids   += " "+subs[i].getPID();
				stypes  += " "+subs[i].getType();
				scpgids += " "+subs[i].getCompositionPageID();
				sapgids += " "+subs[i].getAncillaryPageID();
				slangs  += " "+subs[i].getLang();
			}
		}
		cmd += ",'"+spids+"','"+stypes+"','"+scpgids+"','"+sapgids+"','"+slangs+"'";
		
		TVProgram.Teletext ttxs[] = program.getAllTeletext();
		String tpids="", ttypes="", tmagnums="", tpgnums="", tlangs="";
		if (ttxs != null && ttxs.length > 0) {
			tpids    += ttxs[0].getPID();
			tmagnums += ttxs[0].getMagazineNumber();
			tpgnums  += ttxs[0].getPageNumber();
			tlangs   += ttxs[0].getLang();
			for (int i=1; i<ttxs.length; i++){
				tpids    += " "+ttxs[i].getPID();
				tmagnums += " "+ttxs[i].getMagazineNumber();
				tpgnums  += " "+ttxs[i].getPageNumber();
				tlangs   += " "+ttxs[i].getLang();
			}
		}
		cmd += ",'"+tpids+"','"+ttypes+"','"+tmagnums+"','"+tpgnums+"','"+tlangs+"'";
		cmd += ",'','')";
                
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
		
		/* Query the id */
		int retid = ERR_PARAM;
		cmd = "select * from booking_table where db_srv_id="+program.getID();
		cmd += " and start="+start/1000+" and status="+status;
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, cmd, null, null);
		if(c != null){
			if(c.moveToFirst()){
				int col = c.getColumnIndex("db_id");
				retid = c.getInt(col);
			}
			c.close();
		}
		
		return retid;
	}
	
	public static int bookEvent(Context context, TVEvent event, int flag){
		if (event == null) {
			Log.d(TAG, "Invalid param for booking event");
			return ERR_PARAM;
		}
		int ret = bookProgram(context, event.getProgram(), 
			flag, event.getStartTime(), 
			event.getEndTime()-event.getStartTime());
		if (ret >= 0){
			String cmd = "update booking_table set evt_name='"+sqliteEscape(event.getName());
			cmd += "' where db_id=" + ret;
			context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
		}
		
		return ret;
	}
	
	public int getFlag(){
		return this.flag;
	}
	
	public int getID(){
		return this.id;
	}
	
	public String getRecordFilePath(){
		return this.recFilePath;
	}
	
	public String getRecordStoragePath(){
		return this.recStoragePath;
	}
	
	public long getStart(){
		return this.start;
	}
	
	public long getDuration(){
		return this.duration;
	}
	
	public TVProgram getProgram(){
		return this.program;
	}
	
	public String getProgramName(){
		return this.programName;
	}
	
	public String getEventName(){
		return this.eventName;
	}
	
	public TVProgram.Video getVideo(){
		return this.video;
	}
	
	public TVProgram.Audio[] getAllAudio(){
		return this.audios;
	}
	
	public TVProgram.Subtitle[] getAllSubtitle(){
		return this.subtitles;
	}
	
	public TVProgram.Teletext[] getAllTeletext(){
		return this.teletexts;
	}
	
	public void updateStatus(int status){
		if (status < ST_NOT_START || status > ST_END) {
			Log.d(TAG, "Invalid booking status "+status);
			return;
		}
		this.status = status;
		Log.d(TAG, "Booking "+id+"' status updated to "+status);
		String cmd = "update booking_table set status="+status+" where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	public void updateDuration(long duration){
		this.duration = duration;
		Log.d(TAG, "Booking "+id+"' duration updated to "+duration/1000);
		String cmd = "update booking_table set duration="+duration/1000+" where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	/** This file path is not include the storage path which may be dynamic */
	public void updateRecordFilePath(String path){
		this.recFilePath = new String(path);
		Log.d(TAG, "Booking "+id+"' file path updated to "+path);
		String cmd = "update booking_table set file_name='"+sqliteEscape(path)+"' where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	public void updateRecordStoragePath(String path){
		this.recStoragePath = new String(path);
		Log.d(TAG, "Booking "+id+"' storage path updated to "+path);
		String cmd = "update booking_table set from_storage='"+sqliteEscape(path)+"' where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
}

