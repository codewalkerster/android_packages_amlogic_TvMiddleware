package com.amlogic.tvutil;

import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV program相关信息.
 *Program对应ATV中的一个频道，DTV中的一个service
 */
public class TVProgram{
	private static final String TAG="TVProgram";

	/**未定义类型*/
	public static final int TYPE_UNKNOWN = 0;
	/**电视节目*/
	public static final int TYPE_TV    = 1;
	/**广播节目*/
	public static final int TYPE_RADIO = 2;
	/**模拟节目*/
	public static final int TYPE_ATV   = 3;
	/**数据节目*/
	public static final int TYPE_DATA  = 4;
	/**数字节目*/
	public static final int TYPE_DTV   = 5;
	/** PVR/Timeshifting playback program*/
	public static final int TYPE_PLAYBACK = 6;

	private Context context;
	private int id;
	private int dvbServiceID;
	private int type;
	private String name;
	private TVProgramNumber number;
	private int channelID;
	private TVChannel channel;
	private int skip;
	private boolean lock;
	private boolean scrambled;
	private boolean favorite;
	private int volume;
	private int src;
	private int sourceID;
	private int audioTrack;
	private int pmtPID;
	
	/**
	 *Service中的基础元素信息
	 */
	public class Element{
		private int pid;

		public Element(int pid){
			this.pid = pid;
		}

		/**
		 *取得基础元素的PID
		 *@return 返回PID
		 */
		public int getPID(){
			return pid;
		}
	}

	/**
	 *多语言基础元素信息
	 */
	public class MultiLangElement extends Element{
		private String lang;

		public MultiLangElement(int pid, String lang){
			super(pid);
			this.lang = lang;
		}

		/**
		 *取得元素对应的语言
		 *@return 返回3字符语言字符串
		 */
		public String getLang(){
			return lang;
		}
	}

	/**
	 *视频信息
	 */
	public class Video extends Element{
		/**MPEG1/2*/
		public static final int FORMAT_MPEG12 = 0;
		/**MPEG4*/
		public static final int FORMAT_MPEG4  = 1;
		/**H.264*/
		public static final int FORMAT_H264   = 2;
		/**MJPEG*/
		public static final int FORMAT_MJPEG  = 3;
		/**Real video*/
		public static final int FORMAT_REAL   = 4;
		/**JPEG*/
		public static final int FORMAT_JPEG   = 5;
		/**Microsoft VC1*/
		public static final int FORMAT_VC1    = 6;
		/**AVS*/
		public static final int FORMAT_AVS    = 7;
		/**YUV*/
		public static final int FORMAT_YUV    = 8;
		/**H.264 MVC*/
		public static final int FORMAT_H264MVC= 9;
		/**QJPEG*/
		public static final int FORMAT_QJPEG  = 10;

		private int format;

		public Video(int pid, int fmt){
			super(pid);
			this.format = fmt;
		}

		/**
		 *取得视频编码格式
		 *@return 返回视频编码格式
		 */
		public int getFormat(){
			return format;
		}
	}

	/**
	 *音频信息
	 */
	public class Audio extends MultiLangElement{
		/**MPEG*/
		public static final int FORMAT_MPEG      = 0;
		/**PCM 16位小端*/
		public static final int FORMAT_PCM_S16LE = 1;
		/**AAC*/
		public static final int FORMAT_AAC       = 2;
		/**AC3*/
		public static final int FORMAT_AC3       = 3;
		/**ALAW*/
		public static final int FORMAT_ALAW      = 4;
		/**MULAW*/
		public static final int FORMAT_MULAW     = 5;
		/**DTS*/
		public static final int FORMAT_DTS       = 6;
		/**PCM 16位大端*/
		public static final int FORMAT_PCM_S16BE = 7;
		/**FLAC*/
		public static final int FORMAT_FLAC      = 8;
		/**COOK*/
		public static final int FORMAT_COOK      = 9;
		/**PCM 8位*/
		public static final int FORMAT_PCM_U8    = 10;
		/**ADPCM*/
		public static final int FORMAT_ADPCM     = 11;
		/**AMR*/
		public static final int FORMAT_AMR       = 12;
		/**RAAC*/
		public static final int FORMAT_RAAC      = 13;
		/**WMA*/
		public static final int FORMAT_WMA       = 14;
		/**WMA Pro*/
		public static final int FORMAT_WMAPRO    = 15;
		/**蓝光PCM*/
		public static final int FORMAT_PCM_BLURAY= 16;
		/**ALAC*/
		public static final int FORMAT_ALAC      = 17;
		/**Vorbis*/
		public static final int FORMAT_VORBIS    = 18;
		/**AAC latm格式*/
		public static final int FORMAT_AAC_LATM  = 19;
		/**APE*/
		public static final int FORMAT_APE       = 20;

		private int format;

		public Audio(int pid, String lang, int fmt){
			super(pid, lang);
			this.format = fmt;
		}

		/**
		 *取得音频编码格式
		 *@return 返回音频编码格式
		 */
		public int getFormat(){
			return format;
		}
	}

	/**
	 *字幕信息
	 */
	public class Subtitle extends MultiLangElement{
		/**DVB subtitle*/
		public static final int TYPE_DVB_SUBTITLE = 1;
		/**数字电视Teletext*/
		public static final int TYPE_DTV_TELETEXT = 2;
		/**模拟电视Teletext*/
		public static final int TYPE_ATV_TELETEXT = 3;
		/**数字电视Closed caption*/
		public static final int TYPE_DTV_CC = 4;
		/**模拟电视Closed caption*/
		public static final int TYPE_ATV_CC = 5;

		private int compositionPage;
		private int ancillaryPage;
		private int magazineNo;
		private int pageNo;
		private int type;

		public Subtitle(int pid, String lang, int type, int num1, int num2){
			super(pid, lang);

			this.type = type;
			if(type == TYPE_DVB_SUBTITLE){
				compositionPage = num1;
				ancillaryPage   = num2;
			}else if(type == TYPE_DTV_TELETEXT){
				magazineNo = num1;
				pageNo = num2;
			}
		}

		/**
		 *取得字幕类型
		 *@return 返回字幕类型
		 */
		public int getType(){
			return type;
		}

		/**
		 *取得DVB subtitle的composition page ID
		 *@return 返回composition page ID
		 */
		public int getCompositionPageID(){
			return compositionPage;
		}

		/**
		 *取得DVB subtitle的ancillary page ID
		 *@return 返回ancillary page ID
		 */
		public int getAncillaryPageID(){
			return ancillaryPage;
		}

		/**
		 *取得teletext的magazine number
		 *@return 返回magazine number
		 */
		public int getMagazineNumber(){
			return magazineNo;
		}

		/**
		 *取得teletext的page number
		 *@return 返回page number
		 */
		public int getPageNumber(){
			return pageNo;
		}
	}

	/**
	 *Teletext信息
	 */
	public class Teletext extends MultiLangElement{
		private int magazineNo;
		private int pageNo;

		public Teletext(int pid, String lang, int mag, int page){
			super(pid, lang);
			magazineNo = mag;
			pageNo = page;
		}

		/**
		 *取得teletext的magazine number
		 *@return 返回magazine number
		 */
		public int getMagazineNumber(){
			return magazineNo;
		}

		/**
		 *取得teletext的page number
		 *@return 返回page number
		 */
		public int getPageNumber(){
			return pageNo;
		}
	}

	private Video video;
	private Audio audioes[];
	private Subtitle subtitles[];
	private Teletext teletexts[];
	
	private void constructFromCursor(Context context, Cursor c){
		int col;
		int num, type;
		int major, minor;

		this.context = context;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);
		
		col = c.getColumnIndex("source_id");
		this.sourceID = c.getInt(col);

		col = c.getColumnIndex("src");
		src = c.getInt(col);

		col = c.getColumnIndex("service_id");
		this.dvbServiceID = c.getInt(col);

		col = c.getColumnIndex("db_ts_id");
		this.channelID = c.getInt(col);

		col = c.getColumnIndex("name");
		this.name = c.getString(col);

		col = c.getColumnIndex("chan_num");
		num = c.getInt(col);
			
		col   = c.getColumnIndex("major_chan_num");
		major = c.getInt(col);
		
		col   = c.getColumnIndex("minor_chan_num");
		minor = c.getInt(col);
		
		col   = c.getColumnIndex("aud_track");
		this.audioTrack = c.getInt(col);
		
		if (src == TVChannelParams.MODE_ATSC || (src == TVChannelParams.MODE_ANALOG && major > 0)){
			this.number = new TVProgramNumber(major, minor);
		}else{
			this.number = new TVProgramNumber(num);
		}

		col = c.getColumnIndex("service_type");
		type = c.getInt(col);

		col = c.getColumnIndex("pmt_pid");
		pmtPID = c.getInt(col);

		if(type == 1)
			this.type = TYPE_TV;
		else if(type == 2)
			this.type = TYPE_RADIO;
		else if(type == 3)
			this.type = TYPE_ATV;
		else if (type == 6)
			this.type = TYPE_PLAYBACK;
		else
			this.type = TYPE_DATA;

		col = c.getColumnIndex("skip");
		this.skip = c.getInt(col);

		col = c.getColumnIndex("lock");
		this.lock = (c.getInt(col)!=0);

		col = c.getColumnIndex("scrambled_flag");
		this.scrambled = (c.getInt(col)!=0);

		col = c.getColumnIndex("favor");
		this.favorite = (c.getInt(col)!=0);

		col = c.getColumnIndex("volume");  
		this.volume =  c.getInt(col);
		int pid, fmt;

		col = c.getColumnIndex("vid_pid");
		pid = c.getInt(col);

		col = c.getColumnIndex("vid_fmt");
		fmt = c.getInt(col);

		this.video = new Video(pid, fmt);

		String pids[]=null, fmts[]=null, langs[]=null, str;
		int i, count;

		col = c.getColumnIndex("aud_pids");
		str = c.getString(col);
		if(str.length() != 0)
			pids = str.split(" ");

		col = c.getColumnIndex("aud_fmts");
		str = c.getString(col);
		if(str.length() != 0)
			fmts = str.split(" ");

		col = c.getColumnIndex("aud_langs");
		str = c.getString(col);
		if(str.length() != 0)
			langs = str.split(" ");

		if(pids != null){
			count = pids.length;
			this.audioes = new Audio[count];
		}else{
			count = 0;
			this.audioes = null;
		}

		for(i=0; i<count; i++){
			String lang;

			pid = Integer.parseInt(pids[i]);
			fmt = Integer.parseInt(fmts[i]);
			lang = langs[i];
			this.audioes[i] = new Audio(pid, lang, fmt);
		}

		/* parse subtitles */
		String cids[]=null, aids[]=null;
		pids = null;
		langs = null;
		col = c.getColumnIndex("sub_pids");
		str = c.getString(col);
		if(str.length() != 0)
			pids = str.split(" ");

		col = c.getColumnIndex("sub_composition_page_ids");
		str = c.getString(col);
		if(str.length() != 0)
			cids = str.split(" ");

		col = c.getColumnIndex("sub_ancillary_page_ids");
		str = c.getString(col);
		if(str.length() != 0)
			aids = str.split(" ");

		col = c.getColumnIndex("sub_langs");
		str = c.getString(col);
		if(str.length() != 0)
			langs = str.split(" ");

		if(pids != null){
			count = pids.length;
		}else{
			count = 0;
			this.subtitles = null;
		}

		/* parse teletexts */
		int ttx_count = 0, ttx_sub_count = 0;
		String ttx_pids[]=null, ttx_types[]=null, mag_nos[]=null, page_nos[]=null, ttx_langs[]=null;
		col = c.getColumnIndex("ttx_pids");
		str = c.getString(col);
		if(str.length() != 0)
			ttx_pids = str.split(" ");

		col = c.getColumnIndex("ttx_types");
		str = c.getString(col);
		if(str.length() != 0)
			ttx_types = str.split(" ");

		col = c.getColumnIndex("ttx_magazine_nos");
		str = c.getString(col);
		if(str.length() != 0)
			mag_nos = str.split(" ");

		col = c.getColumnIndex("ttx_page_nos");
		str = c.getString(col);
		if(str.length() != 0)
			page_nos = str.split(" ");

		col = c.getColumnIndex("ttx_langs");
		str = c.getString(col);
		if(str.length() != 0)
			ttx_langs = str.split(" ");

		if(ttx_pids != null){
			for(i=0; i<ttx_pids.length; i++){
				int ttype = Integer.parseInt(ttx_types[i]);
				if (ttype == 0x2 || ttype == 0x5){
					ttx_sub_count++;
				}else{
					ttx_count++;
				}
			}

			if (ttx_count > 0){
				this.teletexts = new Teletext[ttx_count];
			}else{
				this.teletexts = null;
			}
		}else{
			ttx_count = 0;
			this.teletexts = null;
		}

		if ((count+ttx_sub_count) > 0){
			this.subtitles = new Subtitle[count + ttx_sub_count];
		}else{
			this.subtitles = null;
		}
		for(i=0; i<(count); i++){
			this.subtitles[i] = new Subtitle(
				Integer.parseInt(pids[i]), 
				langs[i], Subtitle.TYPE_DVB_SUBTITLE, 
				Integer.parseInt(cids[i]), 
				Integer.parseInt(aids[i]));
		}

		int ittx = 0, isubttx = 0;
		for(i=0; i<(ttx_sub_count + ttx_count); i++){
			int ttype = Integer.parseInt(ttx_types[i]);
			if (ttype == 0x2 || ttype == 0x5){
				this.subtitles[isubttx+count] = new Subtitle(
					Integer.parseInt(ttx_pids[i]), 
					ttx_langs[i], Subtitle.TYPE_DTV_TELETEXT, 
					Integer.parseInt(mag_nos[i]), 
					Integer.parseInt(page_nos[i]));
				isubttx++;
			}else{
				this.teletexts[ittx++] = new Teletext(
					Integer.parseInt(ttx_pids[i]), 
					ttx_langs[i],  
					Integer.parseInt(mag_nos[i]), 
					Integer.parseInt(page_nos[i]));
			}
		}
	}
	
	
	private Cursor selectProgramInChannelByNumber(Context context, int channelID, TVProgramNumber num){
		String cmd = "select * from srv_table where db_ts_id = " + channelID + " and ";
		if (num.isATSCMode()){
			cmd += "major_chan_num = "+num.getMajor()+" and minor_chan_num = "+num.getMinor();
		}else{
			cmd += "chan_num = "+num.getNumber();
		}
		return context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);

	}
	
	private TVProgram(Context context, Cursor c){
		constructFromCursor(context, c);
	}

	/**
	 *向数据库添加一个Program
	 */
	public TVProgram(Context context, int channelID, int type, TVProgramNumber num, int skipFlag){
		TVChannel channel = TVChannel.selectByID(context, channelID);
		if (channel == null){
			Log.d(TAG, "Cannot add new program, invalid channel id "+channelID);
			this.id = -1;
		}else{
			TVChannelParams params = channel.getParams();
			Cursor c = selectProgramInChannelByNumber(context, channelID, num);
			if(c != null){
				if(c.moveToFirst()){
					/*Construct*/
					constructFromCursor(context, c);
				}else{
					/*add a new atv program to database*/
					String cmd = "insert into srv_table(db_net_id,db_ts_id,service_id,src,name,service_type,";
					cmd += "eit_schedule_flag,eit_pf_flag,running_status,free_ca_mode,volume,aud_track,vid_pid,";
					cmd += "vid_fmt,aud_pids,aud_fmts,aud_langs,skip,lock,chan_num,major_chan_num,";
					cmd += "minor_chan_num,access_controlled,hidden,hide_guide,source_id,favor,current_aud,";
					cmd += "db_sat_para_id,scrambled_flag,lcn,hd_lcn,sd_lcn,default_chan_num,chan_order) ";
					cmd += "values(-1,"+ channelID + ",65535,"+ params.getMode() + ",'',"+type+",";
					cmd += "0,0,0,0,0,0,8191,";
					int chanNum = num.isATSCMode() ? (num.getMajor()<<16)|num.getMinor() : num.getNumber();
					int majorNum = num.isATSCMode() ? num.getMajor() : 0;
					cmd += "-1,'','',''," + skipFlag + ",0,"+ chanNum +","+ majorNum + ",";
					cmd += "" + num.getMinor() + ",0,0,0,-1,0,-1,";
					cmd += "-1,0,-1,-1,-1,-1,0)";
					context.getContentResolver().query(TVDataProvider.WR_URL,
							null, cmd, null, null);
						
					Cursor cr = selectProgramInChannelByNumber(context, channelID, num);
					if(cr != null){
						if(cr.moveToFirst()){
							/*Construct*/
							constructFromCursor(context, cr);
						}else{
							/*A critical error*/
							Log.d(TAG, "Cannot add new program, sqlite error");
							this.id = -1;
						}
						cr.close();
					}
				}
				c.close();
			}
		}
	}
	
	public TVProgram(){
	
	}

	/**
	 *This method is mainly designed for adding a Playback program.
	 */
	public TVProgram(Context context, String name, int type, Video vid, Audio[] auds, Subtitle[] subs, Teletext[] ttxs){
		int vpid = (vid != null) ? vid.getPID() : 0x1fff;
		int vfmt = (vid != null) ? vid.getFormat() : -1;
		
		/*add a new program to database*/
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

		ArrayList dvbSubsList = new ArrayList();
		ArrayList ttxSubsList = new ArrayList();
		Subtitle[] dvbSubs = null;
		Subtitle[] ttxSubs = null;
		
		
		for (int i=0; subs != null && i<subs.length; i++){
			if (subs[i].getType() == Subtitle.TYPE_DTV_TELETEXT){
				ttxSubsList.add(subs[i]);
			}else{
				dvbSubsList.add(subs[i]);
			}
		}

		dvbSubs = (Subtitle[])dvbSubsList.toArray(new Subtitle[0]);
		ttxSubs = (Subtitle[])ttxSubsList.toArray(new Subtitle[0]);
		String spids="", stypes="", scpgids="", sapgids="", slangs="";
		if (dvbSubsList.size() > 0) {
			spids   += dvbSubs[0].getPID();
			stypes  += dvbSubs[0].getType();
			scpgids += dvbSubs[0].getCompositionPageID();
			sapgids += dvbSubs[0].getAncillaryPageID();
			slangs  += dvbSubs[0].getLang();
			for (int i=1; i<dvbSubs.length; i++){
				spids   += " "+dvbSubs[i].getPID();
				stypes  += " "+dvbSubs[i].getType();
				scpgids += " "+dvbSubs[i].getCompositionPageID();
				sapgids += " "+dvbSubs[i].getAncillaryPageID();
				slangs  += " "+dvbSubs[i].getLang();
			}
		}
		
		String tpids="", ttypes="", tmagnums="", tpgnums="", tlangs="";
		if (ttxs != null && ttxs.length > 0) {
			tpids    += ttxs[0].getPID();
			tmagnums += ttxs[0].getMagazineNumber();
			tpgnums  += ttxs[0].getPageNumber();
			tlangs   += ttxs[0].getLang();
			ttypes   += 0x1/*not used*/;
			for (int i=1; i<ttxs.length; i++){
				tpids    += " "+ttxs[i].getPID();
				tmagnums += " "+ttxs[i].getMagazineNumber();
				tpgnums  += " "+ttxs[i].getPageNumber();
				tlangs   += " "+ttxs[i].getLang();
				ttypes   += " "+0x1/*not used*/;
			}
			/*add subtitle ttx*/
			for (int i=0; i<ttxSubsList.size(); i++){
				tpids    += " "+ttxSubs[i].getPID();
				tmagnums += " "+ttxSubs[i].getCompositionPageID();
				tpgnums  += " "+ttxSubs[i].getAncillaryPageID();
				tlangs   += " "+ttxSubs[i].getLang();
				ttypes   += " "+0x2;
			}
		}else if (ttxSubsList.size() > 0) {
			tpids    += ttxSubs[0].getPID();
			tmagnums += ttxSubs[0].getCompositionPageID();
			tpgnums  += ttxSubs[0].getAncillaryPageID();
			tlangs   += ttxSubs[0].getLang();
			ttypes   += 0x2; //maybe 0x5
			for (int i=1; i<ttxSubsList.size(); i++){
				tpids    += " "+ttxSubs[i].getPID();
				tmagnums += " "+ttxSubs[i].getCompositionPageID();
				tpgnums  += " "+ttxSubs[i].getAncillaryPageID();
				tlangs   += " "+ttxSubs[i].getLang();
				ttypes   += " "+0x2;
			}
		}

		boolean newInsert = true;
		String cmd;
		if (type == TYPE_PLAYBACK){
			cmd = "select * from srv_table where service_type = "+type;
			Cursor cr = context.getContentResolver().query(TVDataProvider.RD_URL,
					null,
					cmd,
					null, null);
			if(cr != null){
				if(cr.moveToFirst()){
					/*Just update*/
					int col = cr.getColumnIndex("db_id");
					int db_id = cr.getInt(col);
					cmd = "update srv_table set ";
					cmd += "name='" + sqliteEscape(name) + "',";
					cmd += "vid_pid="+vpid+",vid_fmt="+vfmt+",";
					cmd += "current_aud=-1,aud_pids='"+apids+"',aud_fmts='"+afmts+"',aud_langs='"+alangs+"',";
					cmd += "current_sub=-1,sub_pids='"+spids+"',sub_types='"+stypes+"',sub_composition_page_ids='"+scpgids+
						"',sub_ancillary_page_ids='"+sapgids+"',sub_langs='"+slangs+"',";
					cmd += "current_ttx=-1,ttx_pids='"+tpids+"',ttx_types='"+ttypes+"',ttx_magazine_nos='"+tmagnums+
						"',ttx_page_nos='"+tpgnums+"',ttx_langs='"+tlangs+"' ";
					cmd += "where db_id=" + db_id;

					context.getContentResolver().query(TVDataProvider.WR_URL,
						null,
						cmd,
						null, null);

					newInsert = false;
				}
				cr.close();
			}
		}

		if (newInsert){
			cmd = "insert into srv_table(db_net_id,db_ts_id,service_id,src,name,service_type,";
			cmd += "eit_schedule_flag,eit_pf_flag,running_status,free_ca_mode,volume,aud_track,vid_pid,";
			cmd += "vid_fmt,aud_pids,aud_fmts,aud_langs,skip,lock,chan_num,major_chan_num,";
			cmd += "minor_chan_num,access_controlled,hidden,hide_guide,source_id,favor,current_aud,";
			cmd += "current_sub,sub_pids,sub_types,sub_composition_page_ids,sub_ancillary_page_ids,sub_langs,";
		 	cmd += "current_ttx,ttx_pids,ttx_types,ttx_magazine_nos,ttx_page_nos,ttx_langs,";
			cmd += "db_sat_para_id,scrambled_flag,lcn,hd_lcn,sd_lcn,default_chan_num,chan_order) ";
			cmd += "values(-1,-1,65535,-1,'"+sqliteEscape(name)+"',"+type+",";
			cmd += "0,0,0,0,0,0," + vpid + ",";
			cmd += ""+vfmt+",'"+apids+"','"+afmts+"','"+alangs+"',0,0,0,0,";
			cmd += "0,0,0,0,-1,0,-1,";
			cmd += "-1,'"+spids+"','"+stypes+"','"+scpgids+"','"+sapgids+"','"+slangs+"',";
			cmd += "-1,'"+tpids+"','"+ttypes+"','"+tmagnums+"','"+tpgnums+"','"+tlangs+"',";
			cmd += "-1,0,-1,-1,-1,-1,0)";
			context.getContentResolver().query(TVDataProvider.WR_URL,
					null, cmd, null, null);
		}
		

		/**FIXME: here we have a bug, for non-playback programs,
		 * the program we selected may be not the one we inserted.*/
		cmd = "select * from srv_table where vid_pid="+vpid+" and vid_fmt="+vfmt+" and service_type = "+type;
		Cursor cr = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(cr != null){
			if(cr.moveToFirst()){
				/*Construct*/
				constructFromCursor(context, cr);
			}else{
				/*A critical error*/
				Log.d(TAG, "Cannot add new program, sqlite error");
				this.id = -1;
			}
			cr.close();
		}
	}
	

	/**
	 *根据记录ID查找指定TVProgram
	 *@param context 当前Context
	 *@param id 记录ID
	 *@return 返回对应的TVProgram对象，null表示不存在该id所对应的对象
	 */
	public static TVProgram selectByID(Context context, int id){
		TVProgram p = null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from srv_table where srv_table.db_id = " + id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
			}
			c.close();
		}

		return p;
	}

	/**
	 *根据节目类型和节目号查找指定TVProgram
	 *@param context 当前Context
	 *@param type 节目类型
	 *@param num 节目号
	 *@return 返回对应的TVProgram对象，null表示不存在该id所对应的对象
	 */
	public static TVProgram selectByNumber(Context context, int type, TVProgramNumber num){
		TVProgram p = null;
		String cmd;

		cmd = "select * from srv_table where ";
		if(type != TYPE_UNKNOWN){
			if(type == TYPE_DTV){
				cmd += "(service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") and ";
			}else{
				cmd += "service_type = "+type+" and ";
			}
		}

		if(num.isATSCMode()){
			if (num.getMinor() < 0){
				/*recursive call*/
				/*select dtv program first*/
				p = selectByNumber(context,TYPE_DTV,new TVProgramNumber(num.getMajor(), 1, TVProgramNumber.MINOR_CHECK_NEAREST_UP));
				if (p == null){
					/*then try atv program*/
					p = selectByNumber(context,TYPE_ATV,new TVProgramNumber(num.getMajor(), 0, TVProgramNumber.MINOR_CHECK_NONE));
				}
				return p;
			}else if (num.getMinor() >= 1){
				int minorCheck = num.getMinorCheck();
				if (minorCheck == TVProgramNumber.MINOR_CHECK_UP){
					cmd += "major_chan_num = "+num.getMajor()+" and minor_chan_num >= "+num.getMinor()+" ";
					cmd += "order by minor_chan_num DESC limit 1";
				}else if (minorCheck == TVProgramNumber.MINOR_CHECK_DOWN){
					cmd += "major_chan_num = "+num.getMajor()+" and minor_chan_num <= "+num.getMinor()+" ";
					cmd += "order by minor_chan_num limit 1";
				}else if (minorCheck == TVProgramNumber.MINOR_CHECK_NEAREST_UP){
					cmd += "major_chan_num = "+num.getMajor()+" and minor_chan_num >= "+num.getMinor()+" ";
					cmd += "order by minor_chan_num limit 1";
				}else if (minorCheck == TVProgramNumber.MINOR_CHECK_NEAREST_DOWN){
					cmd += "major_chan_num = "+num.getMajor()+" and minor_chan_num <= "+num.getMinor()+" ";
					cmd += "order by minor_chan_num DESC limit 1";
				}else{
					cmd += "major_chan_num = "+num.getMajor()+" and minor_chan_num = "+num.getMinor();
				}
			}else{
				cmd += "major_chan_num = "+num.getMajor()+" and minor_chan_num = "+num.getMinor();
			}
		}else{
			cmd += "chan_num = "+num.getNumber();
		}

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
			}
			c.close();
		}

		return p;
	}

	/**
	 *根据节目号选择下一节目
	 *@param context 当前Context
	 *@param type 节目类型
	 *@param num 节目号
	 *@return 返回对应的TVProgram对象，null表示不存在该id所对应的对象
	 */
	public static TVProgram selectUp(Context context, int type, TVProgramNumber num){
		String cmd;
		Cursor c;
		TVProgram p = null;

		cmd = "select * from srv_table where skip=0 and ";

		if(type != TYPE_UNKNOWN){
			if(type == TYPE_DTV){
				cmd += "(service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") and ";
			}else{
				cmd += "service_type = "+type+" and ";
			}
		}

		if(num.isATSCMode()){
			cmd += "(major_chan_num > "+num.getMajor()+" or (major_chan_num = "+num.getMajor()+" and minor_chan_num > "+num.getMinor()+")) ";
		}else{
			cmd += "chan_num > "+num.getNumber()+" ";
		}

		cmd += "order by chan_num";

		c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
			}
			c.close();
		}

		if(p != null) return p;

		cmd = "select * from srv_table where skip=0 and ";

		if(type != TYPE_UNKNOWN){
			if(type == TYPE_DTV){
				cmd += "(service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") and ";
			}else{
				cmd += "service_type = "+type+" and ";
			}
		}

		if(num.isATSCMode()){
			cmd += "(major_chan_num < "+num.getMajor()+" or (major_chan_num = "+num.getMajor()+" and minor_chan_num < "+num.getMinor()+")) ";
		}else{
			cmd += "chan_num < "+num.getNumber()+" ";
		}

		cmd += "order by chan_num";

		c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
				Log.d(TAG, "selectUp "+p.getNumber().getMinor());
			}
			c.close();
		}

		return p;
	}

	/**
	 *根据节目号选择上一节目
	 *@param context 当前Context
	 *@param type 节目类型
	 *@param num 节目号
	 *@return 返回对应的TVProgram对象，null表示不存在该id所对应的对象
	 */
	public static TVProgram selectDown(Context context, int type, TVProgramNumber num){
		String cmd;
		Cursor c;
		TVProgram p = null;

		cmd = "select * from srv_table where skip=0 and ";

		if(type != TYPE_UNKNOWN){
			if(type == TYPE_DTV){
				cmd += "(service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") and ";
			}else{
				cmd += "service_type = "+type+" and ";
			}
		}

		if(num.isATSCMode()){
			cmd += "(major_chan_num < "+num.getMajor()+" or (major_chan_num = "+num.getMajor()+" and minor_chan_num < "+num.getMinor()+")) ";
		}else{
			cmd += "chan_num < "+num.getNumber()+" ";
		}

		cmd += "order by chan_num desc";

		c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
			}
			c.close();
		}

		if(p != null) return p;

		cmd = "select * from srv_table where skip=0 and ";

		if(type != TYPE_UNKNOWN){
			if(type == TYPE_DTV){
				cmd += "(service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") and ";
			}else{
				cmd += "service_type = "+type+" and ";
			}
		}

		if(num.isATSCMode()){
			cmd += "(major_chan_num > "+num.getMajor()+" or (major_chan_num = "+num.getMajor()+" and minor_chan_num > "+num.getMinor()+")) ";
		}else{
			cmd += "chan_num > "+num.getNumber()+" ";
		}

		cmd += "order by chan_num desc";

		c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
			}
			c.close();
		}

		return p;

	}

	/**
	 *选择一个有效的节目，先查找频道号最小的电视节目，如果没有电视，再查找频道号最小的广播节目
	 **@param context 当前Context
	 */
	public static TVProgram selectFirstValid(Context context, int type){
		TVProgram p = null;
		String cmd;
		Cursor c;

		if((type == TYPE_TV) || (type == TYPE_DTV) || (type == TYPE_UNKNOWN)){
			cmd = "select * from srv_table where skip = 0 and service_type = "+TYPE_TV+" order by chan_num";
			c = context.getContentResolver().query(TVDataProvider.RD_URL,
					null,
					cmd,
					null, null);
			if(c != null){
				if(c.moveToFirst()){
					p = new TVProgram(context, c);
				}
				c.close();
			}

			if(p != null) return p;
		}

		if((type == TYPE_RADIO) || (type == TYPE_DTV) || (type == TYPE_UNKNOWN)){
			cmd = "select * from srv_table where skip = 0 and service_type = "+TYPE_RADIO+" order by chan_num";
			c = context.getContentResolver().query(TVDataProvider.RD_URL,
					null,
					cmd,
					null, null);
			if(c != null){
				if(c.moveToFirst()){
					p = new TVProgram(context, c);
				}
				c.close();
			}

			if(p != null) return p;
		}

		if((type == TYPE_ATV) || (type == TYPE_UNKNOWN)){
			cmd = "select * from srv_table where skip = 0 and service_type = "+TYPE_ATV+" order by chan_num";
			c = context.getContentResolver().query(TVDataProvider.RD_URL,
					null,
					cmd,
					null, null);
			if(c != null){
				if(c.moveToFirst()){
					p = new TVProgram(context, c);
				}
				c.close();
			}

			if(p != null) return p;
		}

		return null;
	}

	/**
	 *列出全部TVProgram
	 *@param context 当前Context
	 *@param no_skip 不列出设为skip的节目
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectAll(Context context, boolean no_skip){
		return selectByType(context, TYPE_UNKNOWN, no_skip);
	}

	/**
	 *列出全部TVProgram
	 *@param context 当前Context
	 *@param type 节目类型
	 *@param skip skip值
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectByType(Context context, int type, int skip){
		TVProgram p[] = null;
		boolean where = false;
		String cmd = "select * from srv_table ";

		if(type == TYPE_DTV){
			cmd += "where (service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") ";
			where = true;
		}else if(type != TYPE_UNKNOWN){
			cmd += "where service_type = "+type+" ";
			where = true;
		}

		if(where){
			cmd += " and skip = " + skip + " ";
		}else{
			cmd += " where skip = " + skip + " ";;
		}

		cmd += " order by chan_order";

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}
	
	/**
	 *列出全部TVProgram
	 *@param context 当前Context
	 *@param type 节目类型
	 *@param no_skip 不列出设为skip的节目
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectByType(Context context, int type, boolean no_skip){
		TVProgram p[] = null;
		boolean where = false;
		String cmd = "select * from srv_table ";

		if(type == TYPE_DTV){
			cmd += "where (service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") ";
			where = true;
		}else if(type != TYPE_UNKNOWN){
			cmd += "where service_type = "+type+" ";
			where = true;
		}

		if(no_skip){			
			if(where){				
				cmd += " and skip = 0 ";			
			}else{				
				cmd += " where skip = 0 ";			
			}		
		}else{
			if(where){				
				cmd += " and skip <= 1 ";			
			}else{				
				cmd += " where skip <= 1 ";			
			}
		}

		cmd += " order by chan_order";

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}
	
	/**
	 *列出一个channel的全部TVProgram
	 *@param context 当前Context
	 *@param channelID channel id
	 *@param type 节目类型
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectByChannel(Context context, int channelID, int type){
		TVProgram p[] = null;
		boolean where = false;
		String cmd = "select * from srv_table ";

		if(type == TYPE_DTV){
			cmd += "where (service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") ";
			where = true;
		}else if(type != TYPE_UNKNOWN){
			cmd += "where service_type = "+type+" ";
			where = true;
		}

		cmd += " and db_ts_id = " + channelID + " order by chan_order";

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}

	/**
	 *根据记录ID查找指定TVProgram
	 *@param context 当前Context
	 *@param sat_id 卫星ID
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectBySatID(Context context, int sat_id){
		TVProgram p[] = null;
		String cmd;
		cmd = "select * from srv_table where db_sat_para_id = " + sat_id + " and (service_type = "+TYPE_TV+" or service_type = "+TYPE_RADIO+") ";

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}

	/**
	 *根据记录ID查找指定TVProgram
	 *@param context 当前Context
	 *@param sat_id 卫星ID
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectBySatIDAndType(Context context, int sat_id,int type){
		TVProgram p[] = null;
		String cmd;

		cmd = "select * from srv_table where db_sat_para_id = " + sat_id + " and service_type = "+type;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}
	
	/**
	 *根据节目名称中的关键字查找指定TVProgram
	 *@param context 当前Context
	 *@param key 名称关键字
	 *@return 返回TVProgram数组，null表示没有节目
	 */

	public static TVProgram[] selectByName(Context context, String key){
		TVProgram p[] = null;
		String cmd;

		cmd = "select * from srv_table where name like '%" + key + "%'";

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}

	/**
	 *根据节目名称中的关键字查找指定TVProgram
	 *@param context 当前Context
	 *@param key 名称关键字
	 *@return 返回TVProgram数组，null表示没有节目
	 */

	public static TVProgram[] selectByNameAndType(Context context, String key,int type){
		TVProgram p[] = null;
		String cmd;

		cmd = "select * from srv_table where name like '%" + key + "%'" + " and service_type = "+type;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}

	/**
	 *根据记录ID删除指定TVProgram
	 *@param context 当前Context
	 *@param sat_id 卫星ID
	 */
	public static void tvProgramDelByChannelID(Context context, int channel_id){
		TVProgram p[] = null;
		String cmd;
		int idx = 0;
		int count = 0;

		Log.d(TAG, "tvProgramDelByChannelID:" + channel_id);
		
		cmd = "select * from srv_table where db_ts_id = " + channel_id;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		count = c.getCount();
		if(c != null){
			if(c.moveToFirst()){
				
				p = new TVProgram[count];
				do{
					p[idx++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		for(idx = 0; idx < count; idx++){
			TVEvent.tvEventDelBySrvID(context, p[idx].getID());
			p[idx].deleteSubtitle();
			p[idx].deleteTeletext();
			p[idx].deleteFromGroupBySrvID();
		}
	
		Cursor cur = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from srv_table where db_ts_id = "+ channel_id,
				null, null);
		if(cur != null){
			cur.close();
		}

		return;
	}
	
	/**
	 *根据记录ID删除指定TVProgram
	 *@param context 当前Context
	 *@param sat_id 卫星ID
	 */
	public static void tvProgramDelBySatID(Context context, int sat_id){
		TVProgram p[] = null;
		String cmd;
		int idx = 0;
		int count = 0;

		Log.d(TAG, "tvProgramDelBySatID:" + sat_id);
		
		cmd = "select * from srv_table where db_sat_para_id = " + sat_id;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		count = c.getCount();
		if(c != null){
			if(c.moveToFirst()){
				
				p = new TVProgram[count];
				do{
					p[idx++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		for(idx = 0; idx < count; idx++){
			TVEvent.tvEventDelBySrvID(context, p[idx].getID());
			p[idx].deleteSubtitle();
			p[idx].deleteTeletext();
			p[idx].deleteFromGroupBySrvID();
		}
	
		Cursor cur = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from srv_table where db_sat_para_id = "+ sat_id,
				null, null);
		if(cur != null){
			cur.close();
		}

		return;
	}


	/**
	 *取得Program的ID
	 *@return 返回ID值
	 */
	public int getID(){
		return id;
	}

	/**
	 *取得Program的audio track
	 *@return 返回audio track值
	 */
	public int getAudTrack(){
		return this.audioTrack;	
	}

	/**
	 *修改Program的audio track
	 *@param aud audio track值
	 */
	public void setAudTrack(int aud){
		
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update srv_table set aud_track = "+aud+" where srv_table.db_id = " + id,
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	 *取得Program的DVB service ID(DVB)
	 *@return 返回service ID
	 */
	public int getDVBServiceID(){
		return dvbServiceID;
	}

	/**
	 *取得Program类型
	 *@return 返回类型
	 */
	public int getType(){
		return type;
	}

	/**
	 *取得Program的名字
	 *@return 返回名字
	 */
	public String getName(){
		return name;
	}

	/**
	 *取得Program的编号
	 *@return 返回编号
	 */
	public TVProgramNumber getNumber(){
		return number;
	}

	/**
	 *取得Program视频相关信息
	 *@return 返回视频相关信息
	 */
	public Video getVideo(){
		return video;
	}

	/**
	 *取得音频总数
	 *@return 返回音频总数
	 */
	public int getAudioCount(){
		if(audioes == null)
			return 0;

		return audioes.length;
	}

	/**
	 *取得Program音频相关信息
	 *@return 返回音频相关信息
	 */
	public Audio getAudio(){
		return getAudio(null);
	}

	/**
	 *取得Program音频相关信息
	 *@param id 音频ID
	 *@return 返回音频相关信息
	 */
	public Audio getAudio(int id){
		if(audioes==null || audioes.length==0)
			return null;

		if(id >= audioes.length)
			id = 0;

		return audioes[id];
	}

	/**
	 *取得Program音频相关信息
	 *@param lang 音频语言
	 *@return 返回音频相关信息
	 */
	public Audio getAudio(String lang){
		int i;

		if(audioes==null)
			return null;

		if(lang!=null){
			for(i=0; i<audioes.length; i++){
				if(audioes[i].getLang().equals(lang))
					return audioes[i];
			}
		}

		return audioes[0];
	}
	
	/**
	 *取得Program所有音频相关信息
	 *@return 返回所有音频相关信息
	 */
	public Audio[] getAllAudio(){
		return audioes;
	}

	/**
	 *记录当前的audio
	 *@param id audio索引
	 *@return 
	 */
	public void setCurrentAudio(int id){
		if (audioes == null || id < 0 || id >= audioes.length){
			Log.d(TAG, "Invalid audio id " + id);
			return;
		}

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update srv_table set current_aud=" + id + " where db_id = " + this.id,
			null, null);
	}
	
	/**
	 *取得当前的audio索引
	 *@param defaultLang 用户未选择语言时，默认的全局语言
	 *@return 当前的Audio索引
	 */
	public int getCurrentAudio(String defaultLang){
		int id = -1;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select current_aud from srv_table where db_id = " + this.id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				id = c.getInt(0);
				if (id < 0 && audioes != null){
					if(defaultLang!=null){
						for(int i=0; i<audioes.length; i++){
							if(audioes[i].getLang().equals(defaultLang)){
								id = i;
								break;
							}
						}
					}

					if (id < 0){
						/* still not found, using the first */
						id = 0;
					}
				}
			}
			c.close();
		}

		return id;
	}

	private void selectSubtitle(){
	}

	/**
	 *删除当前节目Subtitle
	 */
	private void deleteSubtitle(){
		Log.d(TAG, "deleteSubtitle NOT IMPLEMENT YET!");
	}	

	/**
	 *取得字幕总数
	 *@return 返回字幕总数
	 */
	public int getSubtitleCount(){
		selectSubtitle();

		if(subtitles == null)
			return 0;

		return subtitles.length;
	}

	/**
	 *取得Program字幕相关信息
	 *@return 返回字幕相关信息
	 */
	public Subtitle getSubtitle(){
		return getSubtitle(null);
	}

	/**
	 *取得Program字幕相关信息
	 *@param id 字幕ID
	 *@return 返回字幕相关信息
	 */
	public Subtitle getSubtitle(int id){
		selectSubtitle();

		if(subtitles==null)
			return null;

		if(id >= subtitles.length)
			id = 0;

		return subtitles[id];
	}

	/**
	 *取得Program字幕相关信息
	 *@param lang 字幕语言
	 *@return 返回字幕相关信息
	 */
	public Subtitle getSubtitle(String lang){
		int i;

		selectSubtitle();

		if(subtitles==null || subtitles.length==0)
			return null;

		if(lang!=null){
			for(i=0; i<subtitles.length; i++){
				if(subtitles[i].getLang().equals(lang))
					return subtitles[i];
			}
		}

		return subtitles[0];
	}
	
	/**
	 *取得Program所有字幕相关信息
	 *@return 返回所有字幕相关信息
	 */
	public Subtitle[] getAllSubtitle(){
		selectSubtitle();
		return subtitles;
	}

	/**
	 *记录当前的subtitle
	 *@param id subtitle索引
	 *@return 
	 */
	public void setCurrentSubtitle(int id){
		if (subtitles == null || id < 0 || id >= subtitles.length){
			Log.d(TAG, "Invalid subtitle id " + id);
			return;
		}

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update srv_table set current_sub=" + id + " where db_id = " + this.id,
			null, null);
	}
	
	/**
	 *取得当前的subtitle索引
	 *@param defaultLang 用户未选择语言时，默认的全局语言
	 *@return 当前的Subtitle 索引
	 */
	public int getCurrentSubtitle(String defaultLang){
		int id = -1;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select current_sub from srv_table where db_id = " + this.id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				id = c.getInt(0);
				if (id < 0 && subtitles != null){
					if(defaultLang!=null){
						for(int i=0; i<subtitles.length; i++){
							if(subtitles[i].getLang().equals(defaultLang)){
								id = i;
								break;
							}
						}
					}

					if (id < 0){
						/* still not found, using the first */
						id = 0;
					}
				}
			}
			c.close();
		}

		return id;
	}

	private void selectTeletext(){
	}

	/**
	 *删除当前节目Teletext
	 */
	private void deleteTeletext(){
		Log.d(TAG, "deleteTeletext NOT IMPLEMENT YET!");
	}

	/**
	 *取得图文信息总数
	 *@return 返回图文信息总数
	 */
	public int getTeletextCount(){
		selectTeletext();

		if(teletexts == null)
			return 0;

		return teletexts.length;
	}

	/**
	 *取得Program teletext图文相关信息
	 *@return 返回teletext相关信息
	 */
	public Teletext getTeletext(){
		return getTeletext(null);
	}

	/**
	 *取得Program teletext图文相关信息
	 *@param id 图文ID
	 *@return 返回teletext相关信息
	 */
	public Teletext getTeletext(int id){
		selectTeletext();

		if(teletexts == null)
			return null;

		if(id >= teletexts.length)
			id = 0;

		return teletexts[id];
	}

	/**
	 *取得Program teletext图文相关信息
	 *@param lang Teletext语言
	 *@return 返回teletext相关信息
	 */
	public Teletext getTeletext(String lang){
		int i;

		selectTeletext();

		if(teletexts==null || teletexts.length==0)
			return null;

		if(lang!=null){
			for(i=0; i<teletexts.length; i++){
				if(teletexts[i].getLang().equals(lang))
					return teletexts[i];
			}
		}

		return teletexts[0];
	}
	
	/**
	 *取得Program 所有teletext图文相关信息
	 *@return 返回所有teletext相关信息
	 */
	public Teletext[] getAllTeletext(){
		selectTeletext();
		return teletexts;
	}

	/**
	 *记录当前的teletext
	 *@param id teletext索引
	 *@return 
	 */
	public void setCurrentTeletext(int id){
		if (teletexts == null || id < 0 || id >= teletexts.length){
			Log.d(TAG, "Invalid teletext id " + id);
			return;
		}

		context.getContentResolver().query(TVDataProvider.WR_URL,
			null,
			"update srv_table set current_ttx=" + id + " where db_id = " + this.id,
			null, null);
	}
	
	/**
	 *取得当前的teletext索引
	 *@param defaultLang 用户未选择语言时，默认的全局语言
	 *@return 当前的Teletext索引
	 */
	public int getCurrentTeletext(String defaultLang){
		int id = -1;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select current_ttx from srv_table where db_id = " + this.id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				id = c.getInt(0);
				if (id < 0 && teletexts != null){
					if(defaultLang!=null){
						for(int i=0; i<teletexts.length; i++){
							if(teletexts[i].getLang().equals(defaultLang)){
								id = i;
								break;
							}
						}
					}

					if (id < 0){
						/* still not found, using the first */
						id = 0;
					}
				}
			}
			c.close();
		}

		return id;
	}

	/**
	 *取得Program所在的Channel
	 *@return 返回service所在的channel
	 */
	public TVChannel getChannel(){
		if(channel == null){
			channel = TVChannel.selectByID(context, channelID);
		}

		return channel;
	}

	/**
	 *取得节目加锁标志
	 *@return true表示节目加锁，false表示节目未加锁
	 */
	public boolean getLockFlag(){
		return lock;
	}

	/**
	 *取得节目跳过标志
	 *@return true 表示节目设置了跳过标志，false表示节目未设置跳过标志
	 */
	public boolean getSkipFlag(){
		return (skip != 0);
	}
	
	/**
	 *取得节目跳过标志数值
	 *@return true 跳过标志数值
	 */
	public int getSkip(){
		return skip;
	}

	/**
	 *取得加扰标志
	 *@return true 表示节目加扰，false表示节目为清流
	 */
	public boolean getScrambledFlag(){
		return scrambled;
	}

	/**
	 *取得节目喜爱标志
	 *@return true 表示节目设置了喜爱标志，false表示节目未设置喜爱标志
	 */
	public boolean getFavoriteFlag(){
		return favorite;
	}
	
	/**
     *取得节目喜爱标志
     *@return true 表示节目设置了喜爱标志，false表示节目未设置喜爱标志
     */
    public int getVolume(){
        return volume;
    }

	public int getPmtPID(){
		return pmtPID;
	}

	/**
	 *修改节目加锁标志
	 *@param f 加锁标志
	 */
	public void setLockFlag(boolean f){
		lock = f;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update srv_table set lock = "+(f?1:0)+" where srv_table.db_id = " + id,
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	 *修改节目加锁标志
	 *@param f 跳过标志
	 */
	public void setSkipFlag(boolean f){
		skip = f ? 1 : 0;;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update srv_table set skip = "+(f?1:0)+" where srv_table.db_id = " + id,
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	 *修改节目喜爱标志
	 *@param f 跳过标志
	 */
	public void setFavoriteFlag(boolean f){
		favorite = f;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update srv_table set favor = "+(f?1:0)+" where srv_table.db_id = " + id,
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	 *取得节目正在播放事件
	 *@param context 当前Context
	 *@param now 当前时间(UTC时间)
	 *@return 返回正在播放事件，null 表示无正在播放事件信息
	 */
	public TVEvent getPresentEvent(Context context, long now){
		String cmd;
		int time = (int)(now / 1000);
		TVEvent evt = null;
		Cursor c;

		cmd = "select * from evt_table where evt_table.";
		if (src == TVChannelParams.MODE_ATSC){
			cmd += "source_id = "+sourceID;
		}else{
			cmd += "db_srv_id = "+getID();
		}
		cmd += " and evt_table.start <= "+time+" and evt_table.end > "+time;
		c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				evt = new TVEvent(context, c);
			}
			c.close();
		}

		return evt;
	}

	/**
	 *取得节目即将播放事件
	 *@param context 当前Context
	 *@param now 当前时间(UTC时间)
	 *@return 返回即将播放事件，null 表示无即将播放事件信息
	 */
	public TVEvent getFollowingEvent(Context context, long now){
		String cmd;
		int time = (int)(now / 1000);
		TVEvent evt = null;
		Cursor c;

		cmd = "select * from evt_table where evt_table.";
		if (src == TVChannelParams.MODE_ATSC){
			cmd += "source_id = "+sourceID;
		}else{
			cmd += "db_srv_id = "+getID();
		}
		cmd += " and evt_table.start > "+time+" order by evt_table.start";
		c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				evt = new TVEvent(context, c);
			}
			c.close();
		}

		return evt;
	}

	/**
	 *取得节目在一个时间段内的事件
	 *@param context 当前Context
	 *@param start 时间段的开始时间(UTC时间)
	 *@param duration  时间段长度
	 *@return 返回时间段内的事件数组，null表示无相关事件信息
	 */
	public TVEvent[] getScheduleEvents(Context context, long start, long duration){
		String cmd;
		int begin = (int)(start / 1000);
		int end   = (int)((start+duration)/1000);
		TVEvent evts[] = null;
		Cursor c;

		cmd = "select * from evt_table where evt_table.";
		if (src == TVChannelParams.MODE_ATSC){
			cmd += "source_id = "+sourceID;
		}else{
			cmd += "db_srv_id = "+getID();
		}
		cmd += " and ";
		cmd += " ((start < "+begin+" and end > "+begin+") ||";
		cmd += " (start >= "+begin+" and start < "+end+"))";
		cmd += " order by evt_table.start";

		c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				evts = new TVEvent[c.getCount()];
				int id = 0;
				do{
					evts[id++] = new TVEvent(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return evts;
	}

	/**
	 *列出喜爱节目组TVProgram
	 *@param context 当前Context
	 *@param no_skip 不列出设为skip的节目
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectByFavorite(Context context, boolean no_skip){
		TVProgram p[] = null;
		boolean where = false;
		String cmd = "select * from srv_table ";
	
		cmd += "where favor = 1 ";
		where = true;

		if(no_skip){
			if(where){
				cmd += " and skip = 0 ";
			}else{
				cmd += " where skip = 0 ";
			}
		}

		cmd += " order by chan_order";

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}

	/**
	 *列出喜爱节目组TVProgram
	 *@param context 当前Context
	 *@param group_id 节目分组ID
	 *@param no_skip 不列出设为skip的节目
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectByGroupMap(Context context, int group_id, boolean no_skip){
		TVProgram p[] = null;
		String cmd = "select * from srv_table left join grp_map_table on srv_table.db_id = grp_map_table.db_srv_id where grp_map_table.db_grp_id="+group_id ;
	
		cmd += " and srv_table.skip = " + (no_skip ? 0 : 1) + " ";
		cmd += " order by chan_order";

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVProgram[c.getCount()];
				do{
					p[id++] = new TVProgram(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}
  
	public static String sqliteEscape(String keyWord){
		keyWord = keyWord.replace("'", "''");
		return keyWord;
	}	
  
	/**
	 *通过节目名称，service id和节目类型找到对应节目
	 *@param context 当前Context
	 *@param name 节目名称
	 *@param service_id 节目service id
	 *@param type 节目类型
	 *@return 返回TVProgram，null表示没有节目
	 */

	public static TVProgram selectByNameAndServiceId(Context context, String name, int service_id,int type){
		TVProgram p = null;
		String cmd;

		cmd = "select * from srv_table where name = "+ "\'"+sqliteEscape(name)+"\'" + " and service_type = "+type;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
			}
			c.close();
		}

		if(p != null) 
			return p;
		return null;
	}

	/**
	 *添加节目到指定节目分组
	 *@param id 节目分组ID
	 */
	public void addProgramToGroup(int id){
		int group_id = id;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"insert into grp_map_table  (db_srv_id, db_grp_id) values ("+ this.id +" ,"+group_id+")",
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	 *从指定节目分组删除当前节目
	 *@param id 节目分组ID
	 */
	public void deleteFromGroup(int id){
		int group_id = id;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from grp_map_table where db_srv_id = "+ this.id +" and db_grp_id = " + group_id,
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	 *删除当前节目分组
	 */
	private void deleteFromGroupBySrvID(){
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from grp_map_table where db_srv_id = "+ this.id,
				null, null);
		if(c != null){
			c.close();
		}
	}	

	/**
	 *检测当前节目是否属于分组
	 *@param id 节目分组ID
	 *@return 返回布尔类型 true表示该节目属于分组 false表示不属于该分组
	 */
	public boolean checkGroup(int id){
		boolean b=false;
		String cmd = "select * from grp_map_table where db_srv_id = "+ this.id +" and db_grp_id = " +id ;
	
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.getCount()>0){
				b = true;
			}
			c.close();
		}

		return b;
	}

	/**
	 *删除节目
	 */	
	public void deleteFromDb(){
		int group_id = id;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from grp_map_table where db_srv_id = "+ this.id ,
				null, null);

		c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from srv_table where db_id = "+ this.id ,
				null, null);
	
		if(c != null){
			c.close();
		}
	}

	/**
	 *修改节目排序
	 *@param pos 节目排序
	 */
	public void modifyChanOrder(int pos){
		int group_id = id;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update srv_table set chan_order = "+ pos+ " where db_id = "+this.id,
				null, null);

		if(c != null){
			c.close();
		}
	}

	/**
	 *修改节目名称
	 *@param name 节目名称
	 */
	public void setProgramName(String name){
		this.name = name;
		
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update srv_table set name = "+"\'"+name+"\'"+" where srv_table.db_id = " + id,
				null, null);
		if(c != null){
			c.close();
		}
	}
	
	/**
     *修改节目skip状态
     *@param myskip 新的skip状态
     */
    public void setProgramSkip(boolean myskip){
    	setSkipFlag(myskip);
    }
	
    /**
     *修改节目number
     *@param number 新节目号
     */
    public void setProgramNumber(int number){
        if( this.number.getNumber() != number){
            
            this.number = new TVProgramNumber(number);
            Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
                    null,
                    "update srv_table set chan_num = " + number + " where srv_table.db_id = " + id,
                    null, null);
            if(c != null){
                c.close();
            }
        }
    }
    
    /**
     *修改volume
     *@param mvolume 新设定音量
     */
    public void setProgramVolume(int mvolume){
        if( this.volume != mvolume){
            
            this.volume = mvolume;
            Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
                    null,
                    "update srv_table set volume = " + mvolume + " where srv_table.db_id = " + id,
                    null, null);
            if(c != null){
                c.close();
            }
        }
    }
    
    
    
}

