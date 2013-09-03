package com.amlogic.tvutil;

import java.io.File;
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
	public static final int ST_WAIT_START = 0;
	/**预约状态: 开始时间已到，但被用户取消*/
	public static final int ST_CANCELLED = 1;
	/**预约状态: 预约指定的操作已经开始*/
	public static final int ST_STARTED   = 2;
	/**预约状态: 预约指定的操作已经结束*/
	public static final int ST_END       = 3;
	
	/**预约重复类型：不重复，仅此一次*/
	public static final int RP_NONE   = 0;
	/**预约重复类型：每天同一时间重复一次*/
	public static final int RP_DAILY  = 1;
	/**预约重复类型：每星期同一时间重复一次*/
	public static final int RP_WEEKLY = 2;

	/**预约错误码: 预约参数错误*/
	public static final int ERR_PARAM      = -1;
	/**预约错误码: 预约时间段冲突*/
	public static final int ERR_CONFLICT   = -2;
	
	private int id;
	private int status;
	private int flag;
	private int repeat;
	private long start;
	private long duration;
	private Context context;
	private TVProgram program;
	private TVEvent event;
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
		event = TVEvent.selectByID(context, c.getInt(col));
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
		col = c.getColumnIndex("repeat");
		this.repeat = c.getInt(col);
		
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
		this.flag = FL_PLAY | FL_RECORD;
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
	 *预约时间段冲突异常 
	 */
	public static class TVBookingConflictException extends Exception{
		
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
	
	/**
	 *根据预约状态查找指定的预约记录
	 *@param context 当前Context
	 *@param st 预约状态
	 *@return 返回对应的TVBooking对象，null表示不存在对应的对象
	 */
	public static TVBooking[] selectByStatus(Context context, int st){
		TVBooking bookings[] = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where status="+st+" order by start", 
			null, null);
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

	/**
	 *根据预约状态查找指定的录像TVBooking
	 *@param context 当前Context
	 *@param st 预约状态
	 *@return 返回对应的TVBooking对象，null表示不存在对应的对象
	 */
	public static TVBooking[] selectRecordBookingsByStatus(Context context, int st){
		TVBooking bookings[] = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where (flag & "+FL_RECORD+") != 0 and status="+st+" order by start", 
			null, null);
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
	
	/**
	 *根据预约状态查找指定的预约播放
	 *@param context 当前Context
	 *@param st 预约状态
	 *@return 返回对应的TVBooking对象，null表示不存在对应的对象
	 */
	public static TVBooking[] selectPlayBookingsByStatus(Context context, int st){
		TVBooking bookings[] = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where (flag & "+FL_PLAY+") != 0 and status="+st+" order by start", 
			null, null);
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
	
	/**
	 *查找所有预约播放
	 *@param context 当前Context
	 *@return 返回对应的TVBooking对象，null表示不存在该id所对应的对象
	 */
	public static TVBooking[] selectAllPlayBookings(Context context){
		TVBooking bookings[] = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where (flag & "+FL_PLAY+") != 0 order by start" , null, null);
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
	
	/**
	 *查找所有预约录像
	 *@param context 当前Context
	 *@return 返回对应的TVBooking对象，null表示不存在该id所对应的对象
	 */
	public static TVBooking[] selectAllRecordBookings(Context context){
		TVBooking bookings[] = null;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, 
			"select * from booking_table where (flag & "+FL_RECORD+") != 0 order by start" , null, null);
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

	/**
	 *预约一个Program
	 *@param context 当前Context
	 *@param program 需要预约的频道
	 *@param flag 预约标志，可以为FL_RECORD|FL_PLAY
	 *@param start 起始时间 ms
	 *@param duration 持续时间 ms
	 *@param repeat 重复类型，可以为 RP_NONE, RP_DAILY, RP_WEEKLY
	 *@param allowConflict 是否允许预约时间段冲突
	 *@throws TVBookingConflictException
	 */
	public static void bookProgram(Context context, TVProgram program, int flag, long start, long duration, int repeat, boolean allowConflict) throws TVBookingConflictException {
		if (program == null || start < 0 ||
			(flag & (FL_PLAY|FL_RECORD)) == 0) {
			Log.d(TAG, "Invalid param for booking program");
			return;	
		}
		
		int status = ST_WAIT_START;
		if (! allowConflict){
			/*check conflict*/
			long end = start + duration;
			int s = (int)(start/1000);
			int e = (int)(end/1000);
			String sql = "select * from booking_table where status<="+ST_STARTED;
			sql += " and ((start<="+s+" and (start+duration)>="+s+")";
			sql += " or (start<="+e+" and (start+duration)>="+e+")) limit 1";
			Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, sql, null, null);
			if(c != null){
				if(c.moveToFirst()){
					int col = c.getColumnIndex("db_id");
					int cid = c.getInt(col);
					Log.d(TAG, "Conflict with booking "+cid+", cannot book.");
					c.close();
					throw new TVBookingConflictException();
				}
				c.close();
			}
		}
		
		/*book this program*/
		String cmd = "insert into booking_table(db_srv_id, db_evt_id, srv_name, evt_name,";
		cmd += "start,duration,flag,status,file_name,vid_pid,vid_fmt,aud_pids,aud_fmts,aud_languages,";
		cmd += "sub_pids,sub_types,sub_composition_page_ids,sub_ancillary_page_ids,sub_languages,";
		cmd += "ttx_pids,ttx_types,ttx_magazine_numbers,ttx_page_numbers,ttx_languages, other_pids,from_storage,repeat)";
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
		cmd += ",'',''," + repeat + ")";
                
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	/**
	 *预约一个Event
	 *@param context 当前Context
	 *@param event 需要预约的节目
	 *@param flag 预约标志，可以为FL_RECORD|FL_PLAY
	 *@param repeat 重复类型，可以为 RP_NONE, RP_DAILY, RP_WEEKLY
	 *@param allowConflict 是否允许预约时间段冲突
	 *@throws TVBookingConflictException
	 */
	public static void bookEvent(Context context, TVEvent event, int flag, int repeat, boolean allowConflict) throws TVBookingConflictException{
		if (event == null) {
			Log.d(TAG, "Invalid param for booking event");
			return;
		}
		
		/* book its program */
		bookProgram(context, 
			event.getProgram(), 
			flag, event.getStartTime(), 
			event.getEndTime()-event.getStartTime(),
			repeat, allowConflict);
		
		/* update the event info */
		int start = (int)(event.getStartTime()/1000);
		int duration = (int)((event.getEndTime()-event.getStartTime())/1000);
		String cmd = "update booking_table set evt_name='"+sqliteEscape(event.getName());
		cmd += "',db_evt_id="+event.getID()+" where db_srv_id="+event.getProgram().getID();
		cmd += " and start="+start+" and duration="+duration;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
		
		cmd = "update evt_table set sub_flag="+flag+" where db_id="+event.getID();
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	
	/**
	 *判断某个给定时间上该预约是否已经开始
	 *@param timeInMs 
	 *@return true为已经开始，false为未开始
	 */
	public boolean isTimeStart(long timeInMs){
		long tmpStart=start, tmpTime=timeInMs;
		final long MS_PER_DAY = (24 * 3600 * 1000);
		final long MS_PER_WEEK = MS_PER_DAY * 7;
		
		if (repeat == RP_DAILY){
			tmpStart %= MS_PER_DAY;
			tmpTime  %= MS_PER_DAY;
		}else if (repeat == RP_WEEKLY){
			tmpStart %= MS_PER_WEEK;
			tmpTime  %= MS_PER_WEEK;
		}
		
		long ret = tmpTime - tmpStart;
		if (ret <= 0){
			return false;
		}
		
		return true;
	}
	
	/**
	 *判断某个给定时间上该预约是否已经结束
	 *@param timeInMs 
	 *@return true为已经结束，false为未结束
	 */
	public boolean isTimeEnd(long timeInMs){
		if (duration <= 0){
			/* infinite end time */
			return false;
		}
		
		return isTimeStart(timeInMs - duration);
	}
	
	/**
	 *获取预约标志
	 *@return 预约标志 可以为FL_RECORD|FL_PLAY
	 */
	public int getFlag(){
		return this.flag;
	}
	
	/**
	 *获取唯一ID
	 *@return ID
	 */
	public int getID(){
		return this.id;
	}
	
	/**
	 *获取录像文件路径，该路径不包含存储器路径
	 *@return 录像文件路径
	 */
	public String getRecordFilePath(){
		return this.recFilePath;
	}
	
	/**
	 *获取录像存储器路径
	 *@return 录像存储器路径
	 */
	public String getRecordStoragePath(){
		return this.recStoragePath;
	}
	
	/**
	 *获取预约开始时间
	 *@return 预约开始时间, ms
	 */
	public long getStart(){
		return this.start;
	}
	
	/**
	 *获取预约持续时间
	 *@return 预约持续时间, ms
	 */
	public long getDuration(){
		return this.duration;
	}
	
	/**
	 *获取预约状态
	 *@return 预约状态
	 */
	public int getStatus(){
		return this.status;
	}
	
	/**
	 *获取预约重复类型
	 *@return 预约重复类型
	 */
	public int getRepeat(){
		return this.repeat;
	}
	
	/**
	 *获取预约的Program
	 *@return Program对象
	 */
	public TVProgram getProgram(){
		return this.program;
	}
	
	/**
	 *获取预约Program名称
	 *@return 预约Program名称
	 */
	public String getProgramName(){
		return this.programName;
	}
	
	/**
	 *获取预约的Event
	 *@return Event对象
	 */
	public TVEvent getEvent(){
		return this.event;
	}

	/**
	 *获取预约Event名称
	 *@return 预约Event名称
	 */
	public String getEventName(){
		return this.eventName;
	}
	
	/**
	 *获取预约频道的视频
	 *@return 视频对象
	 */
	public TVProgram.Video getVideo(){
		return this.video;
	}
	
	/**
	 *获取预约频道的所有音频
	 *@return 音频对象
	 */
	public TVProgram.Audio[] getAllAudio(){
		return this.audios;
	}
	
	/**
	 *获取预约频道的所有字幕
	 *@return 字幕对象
	 */
	public TVProgram.Subtitle[] getAllSubtitle(){
		return this.subtitles;
	}
	
	/**
	 *获取预约频道的所有图文
	 *@return 图文对象
	 */
	public TVProgram.Teletext[] getAllTeletext(){
		return this.teletexts;
	}
	
	/**
	 *更新预约状态
	 *@param status 预约状态
	 */
	public void updateStatus(int status){
		if (status < ST_WAIT_START || status > ST_END) {
			Log.d(TAG, "Invalid booking status "+status);
			return;
		}
		this.status = status;
		Log.d(TAG, "Booking "+id+"' status updated to "+status);
		String cmd = "update booking_table set status="+status+" where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	/**
	 *更新预约标志
	 *@param flag 预约标志
	 */
	public void updateFlag(int flag){
		this.flag = flag;
		Log.d(TAG, "Booking "+id+"' flag updated to "+flag);
		String cmd = "update booking_table set flag="+flag+" where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}

	/**
	 *更新预约重复性
	 *@param repeat 预约重复性
	 */
	public void updateRepeat(int repeat){
		this.repeat = repeat;
		Log.d(TAG, "Booking "+id+"' repeat updated to "+repeat);
		String cmd = "update booking_table set repeat="+repeat+" where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}

	/**
	 *更新预约持续时间，例如录像总时间
	 *@param duration 持续时间
	 */
	public void updateDuration(long duration){
		this.duration = duration;
		Log.d(TAG, "Booking "+id+"' duration updated to "+duration/1000);
		String cmd = "update booking_table set duration="+duration/1000+" where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	/**
	 *更新预约开始时间
	 *@param start 开始时间
	 */
	public void updateStartTime(long start){
		this.start = start;
		Log.d(TAG, "Booking "+id+"' start updated to "+start/1000);
		String cmd = "update booking_table set start="+start/1000+" where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	/**
	 *更新录像文件路径，不包含存储器路径
	 *@param path 录像文件路径
	 */
	public void updateRecordFilePath(String path){
		this.recFilePath = new String(path);
		Log.d(TAG, "Booking "+id+"' file path updated to "+path);
		String cmd = "update booking_table set file_name='"+sqliteEscape(path)+"' where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	/**
	 *更新录像存储器路径,用于动态从存储器中读取录像记录
	 *@param path 录像存储器路径
	 */
	public void updateRecordStoragePath(String path){
		this.recStoragePath = new String(path);
		Log.d(TAG, "Booking "+id+"' storage path updated to "+path);
		String cmd = "update booking_table set from_storage='"+sqliteEscape(path)+"' where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
	}
	
	/**
	 *删除该预约记录
	 */
	public void delete(){
		String cmd = "delete from booking_table where db_id="+this.id;
		context.getContentResolver().query(TVDataProvider.WR_URL, null, cmd , null, null);
		if ((flag & FL_RECORD) != 0){
			Log.d(TAG, "Delete the record files for this booking...");
			try{
				File rfile = new File(recStoragePath + "/" + recFilePath);
				rfile.delete();
				File rifile = new File(recStoragePath + "/" + recFilePath.replace("amrec", "amri"));
				rifile.delete();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		Log.d(TAG, "Booking "+id+" deleted");
		this.id = -1;
	}
}

