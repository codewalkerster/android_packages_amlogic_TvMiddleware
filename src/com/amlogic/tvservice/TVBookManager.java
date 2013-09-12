package com.amlogic.tvservice;

import java.lang.Object;
import android.graphics.Canvas;
import android.graphics.Paint.FontMetrics;
import android.graphics.Point;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.IWindowManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.PowerManager;
import android.os.Bundle;
import android.util.Log;
import android.content.ComponentName;
import android.content.Intent;
import java.util.ArrayList;
import android.database.Cursor;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.AlarmManager;
import android.app.PendingIntent;
import com.amlogic.tvutil.TVBooking;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvdataprovider.TVDataProvider;

abstract public class TVBookManager{
	public class Event {
		public static final int EVENT_NEW_BOOKING_CONFIRM      = 1;
		public static final int EVENT_NEW_BOOKING_CONFLICT     = 2;
		public static final int EVENT_NEW_BOOKING_START        = 3;
		
		public int type;
		public int bookingID;
		
		public Event(int type){
			this.type = type;
		}
	}
	
	private static final String TAG = "TVBookManager";
	private static final int CHECK_PERIOD_MS = 2000;
	private static final int PRENOTIFY_SECONDS = 60;
	private static final int PREWAKEUP_SECONDS = 40;
	/* when (currenttime-starttime) > AUTOCANCEL_SECONDS, automatically cancel this booking. */
	private static final int AUTOCANCEL_SECONDS = 10; 
	private TVTime tvTime = null;
	private TVConfig tvConfig = null;
	private Handler handler = new Handler();
	private Context context;
	private ReminderDialog reminderDialog = null;
	private boolean startupChecked = false;
	private static ArrayList<BookingWakeUpAlarm> wakeupAlarms = new ArrayList<BookingWakeUpAlarm>();
	
	private Runnable bookManagerTask = new Runnable() {
		public void run() {
			int delayTime = CHECK_PERIOD_MS;
			boolean expired = false;

			checkWakeupAlarms();
			
			autoAdjustStatus();
			
			TVBooking startedBooking = getStartedBooking();
			if (startedBooking != null && isBookingExpired(startedBooking)){
				startedBooking.updateStatus(TVBooking.ST_END);
				expired = true;
			}
			
			if (reminderDialog == null || reminderDialog.getVisibility() != View.VISIBLE){
				long dialogTimeout = 0;
				int dialogTimeoutType = ReminderDialog.TIMEOUT_TO_START;

				/* Try to get the next active booking */
				TVBooking activeBooking = getNextActiveBooking();
				if (activeBooking != null){					
					Log.d(TAG, "Booking " + activeBooking.getID() +
						" is now active, flag=" + activeBooking.getFlag() + 
						", wait the user's choice...");

					long timeToStart = activeBooking.getStart() - tvTime.getTime();
					long timeToEnd   =  activeBooking.getStart() +  activeBooking.getDuration() - tvTime.getTime();
					if (startedBooking != null){
						dialogTimeoutType = ReminderDialog.TIMEOUT_TO_END;
					}

					if (timeToStart <= 0 ){
						if ((timeToStart + (AUTOCANCEL_SECONDS*1000)) < 0){
							/* Auto cancel */
							activeBooking.updateStatus(TVBooking.ST_CANCELLED);
							Log.d(TAG, "Auto cancel this booking: past start_time " + 
								timeToStart/1000 + " seconds.");
						}else{
							/* Give a short reminding */
							dialogTimeout = 5000;
						}
					}else{
						dialogTimeout = timeToStart;
					}

					if (activeBooking.getStatus() == TVBooking.ST_WAIT_START){
						/* Show dialog for user's choice */
						reminderDialog = new ReminderDialog(context, 
											activeBooking.getID(), 
											dialogTimeout, 
											dialogTimeoutType);
						reminderDialog.show();
					}
					
					delayTime = 100;
				}
			}else{
				/* check the dialog's timeout */
				int checkRet = reminderDialog.checkTime();
				
				if (checkRet == 1){
					if (startedBooking == null || expired){
						/* no booking running now or the running booking expired, just jump to this active one */
						reminderDialog.timeout(true);
					}else{
						/* a booking running now, and the user donot respond to it, cancel it */
						reminderDialog.timeout(false);
					}
					reminderDialog = null;
				}else if (checkRet == 2){
					/* this case can only be true after booking wakeup */
					Log.d(TAG, "Auto cancel the reminded booking: past the start time.");
					reminderDialog.timeout(false);
				}
				
				delayTime = 1000;
			}
			
			handler.postDelayed(this, delayTime);
		}
	};
	
	private boolean isBookingExpired(TVBooking booking){
		boolean ret = false;
		
		if ((booking.getFlag()&TVBooking.FL_RECORD) != 0){
			ret = (booking.getStatus() == TVBooking.ST_END);
		}else{
			ret = (booking.isTimeEnd(tvTime.getTime()));
		}
		
		return ret;
	}
	
	private void onBookingEvent(int type, int bookingID){
		Event evt = new Event(type);
		evt.bookingID = bookingID;
		onEvent(evt);
	}
	
	private TVBooking getStartedBooking(){
		TVBooking ret = null;
		
		TVBooking startedBookings[] = TVBooking.selectByStatus(context, TVBooking.ST_STARTED);
		if (startedBookings != null){
			if (startedBookings.length > 1){
				Log.d(TAG, "Warning: more than one bookings are started, this should not happen!");
			}
			ret = startedBookings[0];
		}
		
		return ret;
	}

	private TVBooking getNextActiveBooking(){
		TVBooking bookings[] = TVBooking.selectByStatus(context, TVBooking.ST_WAIT_START);
		if (bookings == null){
			return null;
		}

		long currentTime = tvTime.getTime();
		int preNotifyTime = (getStartedBooking() == null) ? PRENOTIFY_SECONDS : PRENOTIFY_SECONDS;

		for (int i=0; i<bookings.length; i++){
			if (bookings[i].isTimeStart(currentTime+preNotifyTime*1000) && 
				!bookings[i].isTimeEnd(currentTime)){
				if (bookings[i].getProgram() == null){
					/* this program may be deleted by user, skip this booking */
					Log.d(TAG, "Cannot get program, has been deleted? Auto skip this booking.");
					bookings[i].updateStatus(TVBooking.ST_END);
					continue;
				}
				
				return bookings[i];
			}
		}

		return null;
	}
	
	private void startDtvActivity(int bookingID){
		try{
			String strClass = tvConfig.getString("tv:dtv:player_class");
			Log.d(TAG, "Starting dtv player("+strClass+") ...");

			int lastDotPos = strClass.lastIndexOf('.');
			String strPkg = strClass.substring(0, lastDotPos);

			Bundle bookingBundle = new Bundle();
			bookingBundle.putInt("booking_id", bookingID);

			Log.d(TAG, "package: " + strPkg);
			Intent dtvPlayerIntent = new Intent();
			ComponentName dtvPlayerComponent = new ComponentName(strPkg, strClass);
			dtvPlayerIntent.setComponent(dtvPlayerComponent);
			dtvPlayerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			dtvPlayerIntent.putExtras(bookingBundle);
			context.startActivity(dtvPlayerIntent);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private void autoAdjustStatus(){
		if (! startupChecked){
			/* cancel the started bookings while startup */
			TVBooking[] startedBookings = TVBooking.selectByStatus(context, TVBooking.ST_STARTED);
			if (startedBookings != null){
				for (int i=0; i<startedBookings.length; i++){
					Log.d(TAG, "Startup: auto cancel the last started booking "+startedBookings[i].getID());
					startedBookings[i].updateStatus(TVBooking.ST_CANCELLED);
				}
			}
			
			startupChecked = true;
		}
		
		TVBooking[] cancelledBookings = TVBooking.selectByStatus(context, TVBooking.ST_CANCELLED);
		if (cancelledBookings != null){
			long now = tvTime.getTime();
			for (int i=0; i<cancelledBookings.length; i++){
				if (cancelledBookings[i].getDuration() > 0 && !cancelledBookings[i].isTimeEnd(now))
					continue;
				
				if (cancelledBookings[i].getRepeat() != TVBooking.RP_NONE){
					Log.d(TAG, "Auto reset the cancelled booking "+cancelledBookings[i].getID());
					cancelledBookings[i].updateStatus(TVBooking.ST_WAIT_START);
				}else{
					Log.d(TAG, "Auto delete the cancelled booking "+cancelledBookings[i].getID());
					cancelledBookings[i].delete();
				}
			}
		}
	}

	private void checkWakeupAlarms(){
		TVBooking waitingBookings[] = TVBooking.selectByStatus(context, TVBooking.ST_WAIT_START);
		if (waitingBookings == null)
			return;	

		int i, j;

		/*Calculate the new added bookings*/
		ArrayList<TVBooking> newBookings = new ArrayList<TVBooking>();
		for (i=0; i<waitingBookings.length; i++){
			for (j=0; j<wakeupAlarms.size(); j++){
				if (waitingBookings[i].getID() == wakeupAlarms.get(j).bookingID){
					break;
				}
			}
			
			if (j >= wakeupAlarms.size()){
				newBookings.add(waitingBookings[i]);
			}
		}

		/*Calculate the deleted bookings*/
		ArrayList<BookingWakeUpAlarm> deletedAlarms = new ArrayList<BookingWakeUpAlarm>();
		for (i=0; i<wakeupAlarms.size(); i++){
			for (j=0; j<waitingBookings.length; j++){
				if (waitingBookings[j].getID() == wakeupAlarms.get(i).bookingID){
					break;
				}
			
				if (j >= waitingBookings.length){
					wakeupAlarms.get(i).release();
				}
			}
		}

		/*Remove the deleted bookings' alarm*/
		for (i=0; i<deletedAlarms.size(); i++){
			for (j=0; j<wakeupAlarms.size(); j++){
				if (deletedAlarms.get(i) == wakeupAlarms.get(j)){
					wakeupAlarms.remove(j);
					break;
				}
			}
		}

		/*Add the new bookings' alarm*/
		for (i=0; i<newBookings.size(); i++){
			wakeupAlarms.add(new BookingWakeUpAlarm(newBookings.get(i)));
		}
	}
	
	public TVBookManager(Context context){
		this.context  = context;
	}
	
	public void onEvent(TVBookManager.Event evt){
	
	}

	public void open(TVTime time, TVConfig config){
		this.tvTime   = time;
		this.tvConfig = config;

		handler.postDelayed(bookManagerTask, 1000);
	}

	public void close(){
		handler.removeCallbacks(bookManagerTask);
		this.tvTime   = null;
		this.tvConfig = null;
	}

	public class ReminderDialog extends View{
		public static final int TIMEOUT_TO_START = 0;
		public static final int TIMEOUT_TO_END   = 1;
		
		private TVBooking booking;
		private int w;
		private int h;
		private long timeout;
		private long baseTime;
		private ArrayList<ReminderView> subViews;
		private Context ctx;
		private ReminderView viewTimeout;
		private String strTimeout;

		public ReminderDialog(Context ctx, int bookingID, long timeout, int timeoutType){
			super(ctx);

			this.ctx       = ctx;
			this.booking   = TVBooking.selectByID(ctx, bookingID);
			this.timeout   = timeout;
			subViews       = new ArrayList<ReminderView>();
			baseTime       = SystemClock.elapsedRealtime();
			if (timeoutType == TIMEOUT_TO_END){
				strTimeout = context.getString(R.string.auto_discard);
			}else{
				strTimeout = context.getString(R.string.auto_start);
			}
			
		}

		private ReminderView findPrevFocus(){
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
		
		private ReminderView findNextFocus(){
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
		
		private ReminderView findCurrentFocus(){
			int viewCount = subViews.size();
			
			for (int i=0; i<viewCount; i++){
				if (subViews.get(i).isFocused()){
					return subViews.get(i);
				}
			}
			
			return null;
		}
		
		public int checkTime(){
			if (viewTimeout == null)
				return 0;
			
			if (timeout <= 0){
				/* There will be no timeout */
			}else{
				long currTime = SystemClock.elapsedRealtime();
				long remainTime = timeout - (currTime - baseTime);

				Log.d(TAG, "Dialog remaining time check: " + remainTime);
				
				remainTime /= 1000; //To seconds
				
				if (remainTime > 0){
					String strText = strTimeout + "    " + remainTime/3600 + 
						":" + (remainTime%3600)/60 + ":" + remainTime%60;
					
					viewTimeout.setText(strText);
					invalidate();
				}else if ((remainTime + AUTOCANCEL_SECONDS) < 0){
					return 2;
				}else{
					return 1;
				}
				
			}
			
			return 0;
		}
		
		public void show(){
			/* add views */
			Bitmap imgFocus, imgUnfocus;
			
			ReminderView viewBackground = new ReminderView();
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
			
			ReminderView viewTitle = new ReminderView();
			viewTitle.setViewPosition(marginLeft, marginTop, clipW, lineH);
			viewTitle.setTextSize(30);
			viewTitle.setTextColor(Color.YELLOW);
			viewTitle.setText(context.getString(R.string.booking_title));
			subViews.add(viewTitle);
			line++;
			
			line++; //gap

			ReminderView viewProgram = new ReminderView();
			if (booking != null){
				viewProgram.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
				viewProgram.setText(booking.getProgramName());
			}
			subViews.add(viewProgram);
			line++;

			ReminderView viewEvent = new ReminderView();
			if (booking != null){
				viewEvent.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
				viewEvent.setText(booking.getEventName());
			}
			subViews.add(viewEvent);
			line++;
			
			ReminderView viewType = new ReminderView();
			if (booking != null){
				viewType.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
				String strType = "";
				if ((booking.getFlag()&TVBooking.FL_PLAY) != 0){
					strType += context.getString(R.string.view);
				}
				if ((booking.getFlag()&TVBooking.FL_RECORD) != 0){
					if (! strType.isEmpty()){
						strType += " + ";
					}
					strType += context.getString(R.string.pvr);
				}
				viewType.setTextSize(20);
				viewType.setText(strType);
			}
			subViews.add(viewType);
			line++;

			viewTimeout = new ReminderView();
			if (booking != null){
				viewTimeout.setViewPosition(marginLeft, marginTop+line*lineH, clipW, lineH);
				viewTimeout.setTextSize(20);
				viewTimeout.setTextColor(Color.RED);
				viewTimeout.setText("");
			}
			subViews.add(viewTimeout);
			line++;
			
			line++; //gap

			int okW = imgFocus.getWidth();
			int cancelW = imgFocus.getWidth();

			ReminderView viewOk = new ReminderView(){
				@Override
				public void onClick(){
					jump();
				}
			};
			viewOk.setFocusImage(imgFocus);
			viewOk.setUnfocusImage(imgUnfocus);
			viewOk.setViewPosition((w/2-okW)/2, marginTop+line*lineH, okW, buttonH);
			viewOk.setText(context.getString(R.string.ok));
			viewOk.setFocusable(true);
			subViews.add(viewOk);

			ReminderView viewCancel = new ReminderView(){
				@Override
				public void onClick(){
					cancel();
				}
			};
			viewCancel.setFocusImage(imgFocus);
			viewCancel.setUnfocusImage(imgUnfocus);
			viewCancel.setViewPosition((w/2-cancelW)/2 + w/2, marginTop+line*lineH, okW, buttonH);
			viewCancel.setText(context.getString(R.string.cancel));
			viewCancel.setFocusable(true);
			subViews.add(viewCancel);
			
			viewOk.setFocus();
			
			WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
			params.x = 0;
			params.y = 0;
			params.setTitle("TV Reminder");
			
			WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
			wm.addView(ReminderDialog.this, params);
			setVisibility(View.VISIBLE);
			setFocusable(true);
			setFocusableInTouchMode(true);
			requestFocus();
		}
		
		private void jump(){
			TVBooking startedBooking = getStartedBooking();
			if (startedBooking != null){
				startedBooking.updateStatus(TVBooking.ST_END);
			}
			
			if (booking != null){
				/*start the dtv player*/
				startDtvActivity(booking.getID());

				/*update status*/
				if (booking.getFlag() == booking.FL_PLAY){
					booking.updateStatus(TVBooking.ST_END);
				}else{
					booking.updateStatus(TVBooking.ST_STARTED);
				}
			}
			ReminderDialog.this.setVisibility(View.GONE);
		}
		
		private void cancel(){
			if (booking != null){
				booking.updateStatus(TVBooking.ST_CANCELLED);
			}
			ReminderDialog.this.setVisibility(View.GONE);
		}
		
		public void timeout(boolean choice){
			if (choice){
				jump();
			}else{
				cancel();
			}
			
			WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
			wm.removeView(this);	
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
				ReminderView prevFocus = findPrevFocus();
				ReminderView curFocus = findCurrentFocus();
				if (prevFocus != null){
					if (curFocus != null){
						curFocus.unFocus();
					}
					prevFocus.setFocus();
					invalidate();
				}
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				ReminderView nextFocus = findNextFocus();
				ReminderView currFocus = findCurrentFocus();
				if (nextFocus != null){
					if (currFocus != null){
						currFocus.unFocus();
					}
					nextFocus.setFocus();
					invalidate();
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				ReminderView focus = findCurrentFocus();
				if (focus != null){
					focus.onClick();
					invalidate();
				}
				break;
			case KeyEvent.KEYCODE_BACK:
				cancel();
				break;
			default:
				ret = false;//super.onKeyDown(keyCode, event);
				break;
			}

			return ret;
		}
		
		public class ReminderView{
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

			public ReminderView(){
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
	
	class BookingWakeUpAlarm {
		public int bookingID = -1;
		private PendingIntent operation = null;

		public BookingWakeUpAlarm(TVBooking booking){
			if (booking == null)
				return;
			this.bookingID = booking.getID();
			
			Intent intent = new Intent(context, TVServiceReceiver.class);
			intent.setAction("com.amlogic.tvservice.booking_wakeup");
			this.operation = PendingIntent.getBroadcast(context, 0, intent, 0);

			long curSysTime = System.currentTimeMillis();
			long curTvTime = tvTime.getTime();
			long alarmTime = curSysTime + (booking.getStart()-curTvTime) - PREWAKEUP_SECONDS*1000;
			
			Log.d(TAG, "Add wakeup alarm for booking " + this.bookingID +
				": system_time " + curSysTime + ", tv_time " + curTvTime +
				", booking_start " + booking.getStart() + ", alarmTime " + alarmTime);

			AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			am.set(AlarmManager.RTC_WAKEUP, alarmTime, this.operation);
		}

		public void release(){
			if (operation != null){
				Log.d(TAG, "Release wakeup alarm for booking " + this.bookingID);
				AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				am.cancel(operation);

				operation = null;
			}
			
			bookingID = -1;
		}
	};
}
