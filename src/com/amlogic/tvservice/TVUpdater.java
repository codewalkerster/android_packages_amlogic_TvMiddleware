package com.amlogic.tvservice;

import java.util.ArrayList;
import android.util.Log;
import android.os.SystemClock;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Color;

import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

abstract public class TVUpdater{
	private static final String TAG = "TVUpdater";

	public class Event{

		public static final int EVENT_UPDATE_NOTFOUND     = 0;
		public static final int EVENT_UPDATE_TIMEOUT      = 1;
		public static final int EVENT_UPDATE_FOUND        = 2;
		public static final int EVENT_UPDATE_DL_NOTFOUND  = 3;
		public static final int EVENT_UPDATE_DL_TIMEOUT   = 4;
		public static final int EVENT_UPDATE_DL_PROGRESS  = 5;
		public static final int EVENT_UPDATE_DL_DONE      = 6;

		public int type;
		public int param1;
		public int param2;
		public String msg;

		public String sw_ver;
		public String sw_ver_new;

		public int	 dl_frequency;						  /*!< frequency */
		public int	 dl_fec_outer;						  /*!< FEC outer */
		public int	 dl_modulation_type;				  /*!< modulation type */
		public int	 dl_symbol_rate;					  /*!< symbol rate */
		public int	 dl_fec_inner;						  /*!< FEC inner */

		public int  download_pid;
		public int  download_tableid;

		public int control;

		public Event(int type){
			this.type = type;
		}
		public Event(Event evt){
			type = evt.type;
			param1 = evt.param1;
			param2 = evt.param2;
			msg = evt.msg;

			sw_ver = evt.sw_ver;
			sw_ver_new = evt.sw_ver_new;

			dl_frequency = evt.dl_frequency;
			dl_fec_outer = evt.dl_fec_outer;
			dl_modulation_type =  evt.dl_modulation_type;
			dl_symbol_rate = evt.dl_symbol_rate;
			dl_fec_inner = evt.dl_fec_inner;

			download_pid = evt.download_pid;
			download_tableid = evt.download_tableid;

			control = evt.control;
		}
	}

	private long native_mon_handle;
	private long native_dl_handle;

	private native int native_tvupd_start_monitor(int dmx, String sw_ver);
	private native int native_tvupd_stop_monitor();
	private native int native_tvupd_start_downloader(int dmx, int pid, int tableid, String store, int timeout);
	private native int native_tvupd_stop_downloader();

	private Context context;
	private String version;

	/** Load native library*/
	static{
		System.loadLibrary("jnitvupdater");
	}

	public TVUpdater(Context context, String version){
		this.context = context;
		this.version = version;
		native_mon_handle = 0;
		native_dl_handle = 0;
	}

	public void startMonitor(){
		native_tvupd_start_monitor(0, version);
	}

	public void stopMonitor(){
		native_tvupd_stop_monitor();
	}

	public void startDownloader(int pid, int tableid, String store, int timeout){
		native_tvupd_start_downloader(0, pid, tableid, store, timeout);
	}

	public void stopDownloader(){
		native_tvupd_stop_downloader();
	}

	abstract public void onEvent(Event evt);


	public static boolean isForce(Event evt) {return ((evt.control&1)==1);}


	public class NotifyDialog extends View{
		private int w;
		private int h;
		private ArrayList<NotifyView> subViews;
		private Context ctx;
		private NotifyView viewTitle;
		private NotifyView viewSubTitle;
		private NotifyView viewEvent1;
		private NotifyView viewEvent2;
		private NotifyView viewEvent3;
		private NotifyView viewOk;
		private NotifyView viewCancel;
		private int flags;
		public String title;
		public String subtitle;
		public String event1;
		public String event2;
		public String event3;
		public String ok_text;
		public String cancel_text;
		private String name;

		public String txtOK = "OK";
		public String txtCancel = "Cancel";

		public static final int FLAG_OK = 0x1;
		public static final int FLAG_CANCEL = 0x2;
		public static final int FLAG_OKCANCEL = 0x3;

		public NotifyDialog(String name, Context ctx,
							String title, String subtitle,
							String evt1, String evt2, String evt3,
							int flags){
			super(ctx);

			this.name = name;
			this.ctx       = ctx;
			subViews       = new ArrayList<NotifyView>();
			this.flags = flags;
			this.title = title;
			this.subtitle = subtitle;
			this.event1 = evt1;
			this.event2 = evt2;
			this.event3 = evt3;
			txtOK = ctx.getString(R.string.ok);
			txtCancel = ctx.getString(R.string.cancel);
			this.ok_text = txtOK;
			this.cancel_text = txtCancel;
		}

		public void onBtnOK(){
			cancel();
		}
		public void onBtnCancel(){
			cancel();
		}

		private NotifyView findPrevFocus(){
			int viewCount = subViews.size();
			int prevIndex = -1;

			for (int i=0; i<viewCount; i++){
				if (subViews.get(i).isFocusable()){
					if (subViews.get(i).isFocused()){
						break;
					}else{
						prevIndex = i;
					}
				}
			}
			if (prevIndex != -1){
				return subViews.get(prevIndex);
			}
			
			return null;
		}

		private NotifyView findNextFocus(){
			int viewCount = subViews.size();
			int nextIndex = -1;
			boolean bFoundFocus = false;

			for (int i=0; i<viewCount; i++){
				if (subViews.get(i).isFocusable()){
					if (subViews.get(i).isFocused()){
						bFoundFocus = true;
					}else if (bFoundFocus){
						nextIndex = i;
						break;
					}
				}
			}

			if (nextIndex != -1){
				return subViews.get(nextIndex);
			}

			return null;
		}
		
		private NotifyView findCurrentFocus(){
			int viewCount = subViews.size();

			for (int i=0; i<viewCount; i++){
				if (subViews.get(i).isFocused()){
					return subViews.get(i);
				}
			}

			return null;
		}

		public void show(){
			/* add views */
			Bitmap imgFocus, imgUnfocus;

			NotifyView viewBackground = new NotifyView();

			subViews.clear();

			imgUnfocus = BitmapFactory.decodeStream(ctx.getResources()
				.openRawResource(R.drawable.reminder_bg));
			viewBackground.setUnfocusImage(imgUnfocus);

			w = imgUnfocus.getWidth();
			h = imgUnfocus.getHeight();
			viewBackground.setViewPosition(0, 0, w, h);
			subViews.add(viewBackground);

			imgFocus = BitmapFactory.decodeStream(ctx.getResources()
					.openRawResource(R.drawable.btn_focus));
			imgUnfocus = BitmapFactory.decodeStream(ctx.getResources()
					.openRawResource(R.drawable.btn_unfocus));

			int line = 0;
			int marginLeft = 100;
			int marginTop = 30;
			int marginBottom = 60;
			int buttonH = imgFocus.getHeight();
			int clipW = w - 2*marginLeft; //Right==Left
			int clipH = h - marginTop - buttonH - marginBottom;
			int lineH = clipH/7;

			viewTitle = new NotifyView();
			viewTitle.setViewPosition(marginLeft, marginTop, clipW, lineH);
			viewTitle.setTextSize(30);
			viewTitle.setTextColor(Color.YELLOW);
			viewTitle.setText(title);
			subViews.add(viewTitle);
			line++;

			line++; //gap

			viewSubTitle = new NotifyView();
			viewSubTitle.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
			viewSubTitle.setText(subtitle);
			subViews.add(viewSubTitle);
			line++;

			viewEvent1 = new NotifyView();
			viewEvent1.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
			viewEvent1.setText(event1);
			subViews.add(viewEvent1);
			line++;

			viewEvent2 = new NotifyView();
			viewEvent2.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
			viewEvent2.setTextSize(20);
			viewEvent2.setText(event2);
			subViews.add(viewEvent2);
			line++;

			viewEvent3 = new NotifyView();
			viewEvent3.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
			viewEvent3.setTextSize(20);
			viewEvent3.setTextColor(Color.RED);
			viewEvent3.setText(event3);
			subViews.add(viewEvent3);
			line++;

			line++; //gap

			int okW = imgFocus.getWidth();
			int cancelW = imgFocus.getWidth();

			if((flags&FLAG_OK)==FLAG_OK) {
				Log.d(TAG, "BTN_OK");
				viewOk = new NotifyView(){
					@Override
					public void onClick(){
						onBtnOK();
					}
				};
				viewOk.setFocusImage(imgFocus);
				viewOk.setUnfocusImage(imgUnfocus);
				if(flags==FLAG_OK)
					viewOk.setViewPosition((w-okW)/2, marginTop+line*lineH, okW, buttonH);
				else
					viewOk.setViewPosition((w/2-okW)/2, marginTop+line*lineH, okW, buttonH);
				viewOk.setText((ok_text!=null)?  ok_text : txtOK);
				viewOk.setFocusable(true);
				subViews.add(viewOk);
			}

			if((flags&FLAG_CANCEL)==FLAG_CANCEL) {
				Log.d(TAG, "BTN_CANCEL");
				viewCancel = new NotifyView(){
					@Override
					public void onClick(){
						onBtnCancel();
					}
				};

				viewCancel.setFocusImage(imgFocus);
				viewCancel.setUnfocusImage(imgUnfocus);
				if(flags==FLAG_CANCEL)
					viewCancel.setViewPosition((w-cancelW)/2, marginTop+line*lineH, cancelW, buttonH);
				else
					viewCancel.setViewPosition((w/2-cancelW)/2 + w/2, marginTop+line*lineH, cancelW, buttonH);
				viewCancel.setText((cancel_text!=null)? cancel_text : txtCancel);
				viewCancel.setFocusable(true);
				subViews.add(viewCancel);
			}

			if((flags&FLAG_OK)==FLAG_OK)
				viewOk.setFocus();
			else if((flags&FLAG_CANCEL)==FLAG_CANCEL)
				viewCancel.setFocus();

			WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
			params.x = 0;
			params.y = 0;
			params.setTitle("Updater");

			WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
			try {
				wm.removeView(NotifyDialog.this);
			}catch(Exception e) {
			}
			wm.addView(NotifyDialog.this, params);
			setVisibility(View.VISIBLE);
			setFocusable(true);
			setFocusableInTouchMode(true);
			requestFocus();
		}

		public void cancel(){
			//NotifyDialog.this.setVisibility(View.GONE);
			Log.d(TAG, "Notify cancel");
			WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
			wm.removeView(NotifyDialog.this);
		}

		@Override
		public void onDraw(Canvas canvas) {
			int viewCount = subViews.size();

			for (int i=0; i<viewCount; i++){
				subViews.get(i).drawView(canvas);
			}
		}

		@Override
		public boolean onKeyDown (int keyCode, KeyEvent event){
			boolean ret = true;

			switch (keyCode){
			case KeyEvent.KEYCODE_DPAD_LEFT:
				NotifyView prevFocus = findPrevFocus();
				NotifyView curFocus = findCurrentFocus();
				if (prevFocus != null){
					if (curFocus != null){
						curFocus.unFocus();
					}
					prevFocus.setFocus();
					invalidate();
				}
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				NotifyView nextFocus = findNextFocus();
				NotifyView currFocus = findCurrentFocus();
				if (nextFocus != null){
					if (currFocus != null){
						currFocus.unFocus();
					}
					nextFocus.setFocus();
					invalidate();
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				NotifyView focus = findCurrentFocus();
				if (focus != null){
					focus.onClick();
					invalidate();
				}
				break;
			case KeyEvent.KEYCODE_BACK:
				//cancel();
				break;
			default:
				ret = false;//super.onKeyDown(keyCode, event);
				break;
			}

			return ret;
		}

		public NotifyDialog setTitle(String text)
		{
			title = text;
			viewTitle.setText(text);
			invalidate();
			return this;
		}
		public NotifyDialog setSubTitle(String text)
		{
			subtitle = text;
			viewSubTitle.setText(text);
			invalidate();
			return this;
		}
		public NotifyDialog setEvent1(String text)
		{
			event1 = text;
			viewEvent1.setText(text);
			invalidate();
			return this;
		}
		public NotifyDialog setEvent2(String text)
		{
			event2 = text;
			viewEvent2.setText(text);
			invalidate();
			return this;
		}
		public NotifyDialog setEvent3(String text)
		{
			event3 = text;
			viewEvent3.setText(text);
			invalidate();
			return this;
		}
		public NotifyDialog setOKText(String text)
		{
			ok_text = (text!=null)? text : txtOK;
			if(viewCancel!=null) {
				viewOk.setText(ok_text);
				invalidate();
			}
			return this;
		}
		public NotifyDialog setCancelText(String text)
		{
			cancel_text=(text!=null)? text : txtCancel;;
			if(viewCancel!=null) {
				viewCancel.setText(cancel_text);
				invalidate();
			}
			return this;
		}
		public NotifyDialog setFlags(int flags)
		{
			this.flags = flags;
			return this;
		}
		public String getName()
		{
			return name;
		}

		public class NotifyView{
			public static final int ST_FOCUS   = 0;
			public static final int ST_UNFOCUS = 1;

			private int status;
			private int viewWidth;
			private int viewHeight;
			private Point viewPosition;
			private Bitmap[] images = new Bitmap[2]; //focus, unfocus
			private boolean focusable;
			private String textString;
			private int textColor;
			private int textSize;
			private Paint.Align textAlign;

			public NotifyView(){
				viewWidth          = 0;
				viewHeight         = 0;
				viewPosition       = null;
				images[ST_FOCUS]   = null;
				images[ST_UNFOCUS] = null;
				focusable          = false;
				textString         = null;
				textSize           = 24;
				textColor          = Color.WHITE;
				textAlign          = Paint.Align.CENTER;
				status             = ST_UNFOCUS;
			}

			public void setViewPosition(int x, int y, int w, int h){
				viewPosition = new Point(x, y);
				viewWidth = w;
				viewHeight = h;
			}

			public void setFocusImage(Bitmap bmp){
				images[ST_FOCUS] = bmp;
			}

			public void setUnfocusImage(Bitmap bmp){
				images[ST_UNFOCUS] = bmp;
			}

			public void setFocusable(boolean bFocus){
				focusable = bFocus;
			}

			public void setText(String text){
				textString = text;
			}

			public void setTextColor(int color){
				textColor = color;
			}

			public void setTextSize(int size){
				textSize = size;
			}

			public void setTextAlign(Paint.Align align){
				textAlign = align;
			}

			public void setFocus(){
				if (focusable){
					status = ST_FOCUS;
				}
			}

			public void unFocus(){
				if (isFocused()){
					status = ST_UNFOCUS;
				}
			}

			public boolean isFocusable(){
				return focusable;
			}

			public boolean isFocused(){
				return (status == ST_FOCUS);
			}

			private Bitmap getImage(){
				if (status == ST_FOCUS && focusable){
					return images[ST_FOCUS];
				}

				return images[ST_UNFOCUS];
			}


			public void drawView(Canvas canvas){
				Paint paint = new Paint();
				paint.setAntiAlias(true);

				int vX = viewPosition.x + (canvas.getWidth() - w)/2;
				int vY = viewPosition.y + (canvas.getHeight() - h)/2;
				/* draw the background image */
				Bitmap img = getImage();
				if (img != null) {
					canvas.drawBitmap(img, vX, vY, paint);
				}

				/* draw text */
				if (textString != null) {
					paint.setColor(textColor);
					paint.setTextSize(textSize);
					paint.setTextAlign(textAlign);

					canvas.save();
					int textX = vX;
					int textY = vY;
					canvas.clipRect(textX, textY, textX+viewWidth, textY+viewHeight);
					FontMetrics fontMetrics = paint.getFontMetrics(); 
					if (textAlign == Paint.Align.CENTER){
						int fontBase = (int)((fontMetrics.bottom - fontMetrics.top)/2 - fontMetrics.bottom); 
						textX += viewWidth/2;
						textY += (viewHeight/2 + fontBase);
					}else{
						textY += (int)(fontMetrics.bottom - fontMetrics.top);
					}
					canvas.drawText(textString, textX, textY, paint);
					canvas.restore();
				}
			}

			public void onClick(){

			}
		}
	}
}

