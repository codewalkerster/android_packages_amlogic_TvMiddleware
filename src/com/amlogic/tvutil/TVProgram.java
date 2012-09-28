package com.amlogic.tvutil;

import android.database.Cursor;
import android.content.Context;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV program相关信息.
 *Program对应ATV中的一个频道，DTV中的一个service
 */
public class TVProgram{
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

	private Context context;
	private int id;
	private int dvbServiceID;
	private int type;
	private String name;
	private TVProgramNumber number;
	private int channelID;
	private TVChannel channel;
	private boolean skip;
	private boolean lock;

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

	private TVProgram(Context context, Cursor c){
		int col;
		int num, type;

		this.context = context;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);

		col = c.getColumnIndex("service_id");
		this.dvbServiceID = c.getInt(col);

		col = c.getColumnIndex("db_ts_id");
		this.channelID = c.getInt(col);

		col = c.getColumnIndex("name");
		this.name = c.getString(col);

		col = c.getColumnIndex("chan_num");
		num = c.getInt(col);
		this.number = new TVProgramNumber(num);


		col = c.getColumnIndex("service_type");
		type = c.getInt(col);

		if(type == 1)
			this.type = TYPE_TV;
		else if(type == 2)
			this.type = TYPE_RADIO;
		else
			this.type = TYPE_DATA;

		col = c.getColumnIndex("skip");
		this.skip = (c.getInt(col)!=0);

		col = c.getColumnIndex("lock");
		this.lock = (c.getInt(col)!=0);

		int pid, fmt;

		col = c.getColumnIndex("vid_pid");
		pid = c.getInt(col);

		col = c.getColumnIndex("vid_fmt");
		fmt = c.getInt(col);

		this.video = new Video(pid, fmt);

		String pids[], fmts[], langs[], str;
		int i, count;

		col = c.getColumnIndex("aud_pids");
		str = c.getString(col);
		pids = str.split(" ");

		col = c.getColumnIndex("aud_fmts");
		str = c.getString(col);
		fmts = str.split(" ");

		col = c.getColumnIndex("aud_langs");
		str = c.getString(col);
		langs = str.split(" ");

		count = pids.length;
		this.audioes = new Audio[count];

		for(i=0; i<count; i++){
			String lang;

			pid = Integer.parseInt(pids[i]);
			fmt = Integer.parseInt(fmts[i]);
			lang = langs[i];
			this.audioes[i] = new Audio(pid, lang, fmt);
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
				c.close();
			}
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

		cmd = "select * from srv_table where";
		if(type != TYPE_UNKNOWN)
			cmd += " service_type = "+type+" and";

		cmd += " chan_num = "+num.getNumber();

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				p = new TVProgram(context, c);
				c.close();
			}
		}

		return p;

	}

	/**
	 *列出全部TVProgram
	 *@param context 当前Context
	 *@param no_skip 不列出设为skip的节目
	 *@return 返回TVProgram数组，null表示没有节目
	 */
	public static TVProgram[] selectAll(Context context, boolean no_skip){
		TVProgram p[] = null;
		String cmd = "select * from srv_table";

		if(no_skip){
			cmd += " where skip = 0 ";
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
				c.close();
			}
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
		int tval = TYPE_DATA;

		if(type == TYPE_TV)
			tval = TYPE_TV;
		else if(type == TYPE_RADIO)
			tval = TYPE_RADIO;

		String cmd = "select * from srv_table where type = "+ tval;

		if(no_skip){
			cmd += " and skip = 0 ";
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
				c.close();
			}
		}

		return p;
	}


	/**
	 *取得Program的ID
	 *@return 返回ID值
	 */
	public int getID(){
		return id;
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
	 *取得Program音频相关信息
	 *@return 返回音频相关信息
	 */
	public Audio getAudio(){
		return getAudio(null);
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
	 *取得Program字幕相关信息
	 *@return 返回字幕相关信息
	 */
	public Subtitle getSubtitle(){
		return getSubtitle(null);
	}

	/**
	 *取得Program字幕相关信息
	 *@param lang 字幕语言
	 *@return 返回字幕相关信息
	 */
	public Subtitle getSubtitle(String lang){
		int i;

		if(subtitles==null)
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
	 *取得Program teletext图文相关信息
	 *@return 返回teletext相关信息
	 */
	public Teletext getTeletext(){
		return getTeletext(null);
	}

	/**
	 *取得Program teletext图文相关信息
	 *@param lang Teletext语言
	 *@return 返回teletext相关信息
	 */
	public Teletext getTeletext(String lang){
		int i;

		if(teletexts==null)
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
	 *取得Program所在的Channel
	 *@return 返回service所在的channel
	 */
	public TVChannel getChannel(){
		if(channel == null){
			channel = TVChannel.selectByID(context, channelID);
		}

		return channel;
	}
}

