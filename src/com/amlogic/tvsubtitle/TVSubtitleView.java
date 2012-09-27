package com.amlogic.tvsubtitle;

import android.content.Context;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.util.AttributeSet;
import java.lang.Exception;
import android.util.Log;

/**
 * TVSubtitleView提供数字/模拟电视字幕和图文信息支持.
 * 目前支持DVB subtitle, DTV/ATV teletext, ATSC/NTSC closed caption.
 */
public class TVSubtitleView extends View {
	private static final String TAG="TVSubtitleView";

	private static final int BUFFER_W = 720;
	private static final int BUFFER_H = 576;

	private static final int DISP_LEFT=40;
	private static final int DISP_RIGHT=40;
	private static final int DISP_TOP=40;
	private static final int DISP_BOTTOM=40;
	
	private static final int MODE_NONE=0;
	private static final int MODE_DTV_TT=1;
	private static final int MODE_DTV_CC=2;
	private static final int MODE_DVB_SUB=3;
	private static final int MODE_ATV_TT=4;
	private static final int MODE_ATV_CC=5;

	private static final int PLAY_NONE= 0;
	private static final int PLAY_SUB = 1;
	private static final int PLAY_TT  = 2;

	public static final int COLOR_RED=0;
	public static final int COLOR_GREEN=1;
	public static final int COLOR_YELLOW=2;
	public static final int COLOR_BLUE=3;

	private native int native_sub_init();
	private native int native_sub_destroy();
	private native int native_sub_lock();
	private native int native_sub_unlock();
	private native int native_sub_clear();
	private native int native_sub_start_dvb_sub(int dmx_id, int pid, int page_id, int anc_page_id);
	private native int native_sub_start_dtv_tt(int dmx_id, int pid, int page, int sub_page, boolean is_sub);
	private native int native_sub_stop_dvb_sub();
	private native int native_sub_stop_dtv_tt();
	private native int native_sub_tt_goto(int page);
	private native int native_sub_tt_color_link(int color);
	private native int native_sub_tt_home_link();
	private native int native_sub_tt_next(int dir);
	private native int native_sub_tt_set_search_pattern(String pattern, boolean casefold);
	private native int native_sub_tt_search_next(int dir);

	static{
		System.loadLibrary("am_adp");
		System.loadLibrary("am_mw");
		System.loadLibrary("zvbi");
		System.loadLibrary("jnitvsubtitle");
	}

	/**
	 * DVB subtitle 参数
	 */
	public class DVBSubParams{
		private int dmx_id;
		private int pid;
		private int composition_page_id;
		private int ancillary_page_id;

		/**
		 * 创建DVB subtitle参数
		 * @param dmx_id 接收使用demux设备的ID
		 * @param pid subtitle流的PID
		 * @param page_id 字幕的page_id
		 * @param anc_page_id 字幕的ancillary_page_id
		 */
		public DVBSubParams(int dmx_id, int pid, int page_id, int anc_page_id){
			this.dmx_id              = dmx_id;
			this.pid                 = pid;
			this.composition_page_id = page_id;
			this.ancillary_page_id   = anc_page_id;
		}
	}

	/**
	 * 数字电视teletext图文参数
	 */
	public class DTVTTParams{
		private int dmx_id;
		private int pid;
		private int page_no;
		private int sub_page_no;

		/**
		 * 创建数字电视teletext图文参数
		 * @param dmx_id 接收使用demux设备的ID
		 * @param pid 图文信息流的PID
		 * @param page_no 要显示页号
		 * @param sub_page_no 要显示的子页号
		 */
		public DTVTTParams(int dmx_id, int pid, int page_no, int sub_page_no){
			this.dmx_id      = dmx_id;
			this.pid         = pid;
			this.page_no     = page_no;
			this.sub_page_no = sub_page_no;
		}
	}

	public class ATVTTParams{
	}

	public class DTVCCParams{
	}

	public class ATVCCParams{
	}

	private class SubParams{
		int mode;
		DVBSubParams dvb_sub;
		DTVTTParams  dtv_tt;
		ATVTTParams  atv_tt;
		DTVCCParams  dtv_cc;
		ATVCCParams  atv_cc;

		private SubParams(){
			mode = MODE_NONE;
		}
	}

	private class TTParams{
		int mode;
		DTVTTParams dtv_tt;
		ATVTTParams atv_tt;

		private TTParams(){
			mode = MODE_NONE;
		}
	}

	private SubParams sub_params;
	private TTParams  tt_params;
	private int       play_mode;
	private boolean   visible;
	private boolean   destroy;
	private int       native_handle;
	private Bitmap    bitmap;

	private void update() {
		postInvalidate();
	}

	private void stopDecoder(){
		switch(play_mode){
			case PLAY_NONE:
				break;
			case PLAY_TT:
				switch(tt_params.mode){
					case MODE_DTV_TT:
						native_sub_stop_dtv_tt();
						break;
					default:
						break;
				}
				break;
			case PLAY_SUB:
				switch(sub_params.mode){
					case MODE_DTV_TT:
						native_sub_stop_dtv_tt();
						break;
					case MODE_DVB_SUB:
						native_sub_stop_dvb_sub();
						break;
					default:
						break;
				}
				break;
		}

		play_mode = PLAY_NONE;
	}

	private void init(){
		play_mode  = PLAY_NONE;
		visible    = true;
		destroy    = false;
		tt_params  = new TTParams();
		sub_params = new SubParams();
		bitmap     = Bitmap.createBitmap(BUFFER_W, BUFFER_H, Bitmap.Config.ARGB_8888);
		if(native_sub_init()<0){
		}
	}

	/**
	 * 创建TVSubtitle控件
	 */
	public TVSubtitleView(Context context){
		super(context);
		init();
	}

	/**
	 * 创建TVSubtitle控件
	 */
	public TVSubtitleView(Context context, AttributeSet attrs){
		super(context, attrs);
		init();
	}

	/**
	 * 创建TVSubtitle控件
	 */
	public TVSubtitleView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * 设定字幕参数
	 * @param params 字幕参数
	 */
	public void setSubParams(DVBSubParams params){
		sub_params.mode = MODE_DVB_SUB;
		sub_params.dvb_sub = params;

		if(play_mode==PLAY_SUB)
			startSub();
	}

	/**
	 * 设定字幕参数
	 * @param params 字幕参数
	 */
	public void setSubParams(DTVTTParams params){
		sub_params.mode = MODE_DTV_TT;
		sub_params.dtv_tt = params;

		if(play_mode==PLAY_SUB)
			startSub();
	}

	/**
	 * 设定图文参数
	 * @param params 字幕参数
	 */
	public void setTTParams(DTVTTParams params){
		tt_params.mode = MODE_DTV_TT;
		tt_params.dtv_tt = params;

		if(play_mode==PLAY_TT)
			startTT();
	}

	/**
	 * 显示字幕/图文信息
	 */
	public void show(){
		if(visible)
			return;

		visible = true;
		update();
	}

	/**
	 * 隐藏字幕/图文信息
	 */
	public void hide(){
		if(!visible)
			return;

		visible = false;
		update();
	}

	/**
	 * 开始图文信息解析
	 */
	public void startTT(){
		stopDecoder();

		if(tt_params.mode==MODE_NONE)
			return;

		int ret = 0;
		switch(tt_params.mode){
			case MODE_DTV_TT:
				ret = native_sub_start_dtv_tt(tt_params.dtv_tt.dmx_id,
						tt_params.dtv_tt.pid,
						tt_params.dtv_tt.page_no,
						tt_params.dtv_tt.sub_page_no,
						false);
				break;
			default:
				break;
		}

		if(ret >= 0)
			play_mode = PLAY_TT;
	}

	/**
	 * 开始字幕信息解析
	 */
	public void startSub(){
		stopDecoder();

		if(sub_params.mode==MODE_NONE)
			return;

		int ret = 0;
		switch(sub_params.mode){
			case MODE_DVB_SUB:
				ret = native_sub_start_dvb_sub(sub_params.dvb_sub.dmx_id,
						sub_params.dvb_sub.pid,
						sub_params.dvb_sub.composition_page_id,
						sub_params.dvb_sub.ancillary_page_id);
				break;
			case MODE_DTV_TT:
				ret = native_sub_start_dtv_tt(sub_params.dtv_tt.dmx_id,
						sub_params.dtv_tt.pid,
						sub_params.dtv_tt.page_no,
						sub_params.dtv_tt.sub_page_no,
						true);
				break;
			default:
				break;
		}

		if(ret >= 0)
			play_mode = PLAY_SUB;
	}

	/**
	 * 停止图文/字幕信息解析
	 */
	public void stop(){
		stopDecoder();
	}

	/**
	 * 停止图文/字幕信息解析并清除缓存数据
	 */
	public void clear(){
		stopDecoder();
		native_sub_clear();
		tt_params.mode  = MODE_NONE;
		sub_params.mode = MODE_NONE;
	}

	/**
	 * 在图文模式下进入下一页
	 */
	public void nextPage(){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_next(1);
	}

	/**
	 * 在图文模式下进入上一页
	 */
	public void previousPage(){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_next(-1);
	}

	/**
	 * 在图文模式下跳转到指定页
	 * @param page 要跳转到的页号
	 */
	public void gotoPage(int page){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_goto(page);
	}

	/**
	 * 在图文模式下跳转到home页
	 */
	public void goHome(){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_home_link();
	}

	/**
	 * 在图文模式下根据颜色跳转到指定链接
	 * @param color 颜色，COLOR_RED/COLOR_GREEN/COLOR_YELLOW/COLOR_BLUE
	 */
	public void colorLink(int color){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_color_link(color);
	}

	/**
	 * 在图文模式下设定搜索字符串
	 * @param pattern 搜索匹配字符串
	 * @param casefold 是否区分大小写
	 */
	public void setSearchPattern(String pattern, boolean casefold){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_set_search_pattern(pattern, casefold);
	}

	/**
	 * 搜索下一页
	 */
	public void searchNext(){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_search_next(1);
	}

	/**
	 * 搜索上一页
	 */
	public void searchPrevious(){
		if(play_mode!=PLAY_TT)
			return;

		native_sub_tt_search_next(-1);
	}

	@Override
	public void onDraw(Canvas canvas){
		Rect sr;

		if(!visible || (play_mode==PLAY_NONE))
			return;

		if(play_mode==PLAY_TT || tt_params.mode==MODE_DTV_TT || tt_params.mode==MODE_ATV_TT){
			sr = new Rect(0, 0, 12*41, 10*25);
		}else{
			sr = new Rect(0, 0, BUFFER_W, BUFFER_H);
		}

		native_sub_lock();
		canvas.drawBitmap(bitmap,
				sr,
				new Rect(DISP_LEFT, DISP_TOP, getWidth()-DISP_LEFT-DISP_RIGHT, getHeight()-DISP_TOP-DISP_BOTTOM),
				new Paint());

		native_sub_unlock();
	}

	protected void finalize() throws Throwable {
		if(!destroy){
			clear();
			destroy = true;
			native_sub_destroy();
		}
		super.finalize();
	}  
}

