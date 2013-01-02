package com.amlogic.tvservice;

import android.os.Handler;
import android.util.Log;
import android.database.Cursor;
import android.content.Context;
import com.amlogic.tvutil.TVBooking;
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
	private static final int START_CONFIRM_TIME = 60;
	private static final int CONFLICT_CONFIRM_TIME = 15;
	private TVTime tvTime = null;
	private Handler handler;
	private Context context;
	
	private Runnable bookManagerTask = new Runnable() {
		/*private long getNextDelayMs(){
			long delay = -1;
			long currTime = tvTime.getTime();
			TVBooking latestNotStart = null;
			TVBooking latestConfirmed = null;
			
			String sql = "select * from booking_table where status="+TVBooking.ST_NOT_START;
			sql += " and ("+currTime/1000+"-start)>"+START_CONFIRM_TIME;
			sql += " order by start limit 1";
			Cursor c = getContentResolver().query(TVDataProvider.RD_URL, null, sql, null, null);
			if (c != null) {
				latestNotStart = new TVBooking(context, c);
			}
			
			sql = "select * from booking_table where status<="+TVBooking.ST_CONFIRMED;
			sql += " and ("+currTime/1000+"-start)<0";
			sql += " order by start limit 1";
			c = getContentResolver().query(TVDataProvider.RD_URL, null, sql, null, null);
			if (c != null) {
				latestConfirmed = new TVBooking(context, c);
			}
			
			if (latestNotStart != null && latestConfirmed != null) {
				delay = (latestNotStart.start < latestConfirmed.start) ? latestNotStart.start : latestConfirmed.start;
				delay -= currTime;
			} else if (latestNotStart != null) {
				delay = latestNotStart.start - currTime;
			} else if (latestConfirmed != null) {
				delay = latestConfirmed.start - currTime;
			}
			
			if (delay != -1) {
				Log.d(TAG, "Next record check at "+delay+"ms later");
			}
			return delay;
		}*/
		
		public void run() {
			/* Checking not start bookings */
			TVBooking notStartBooking[] = checkNotStartBookings();
			if (notStartBooking != null){
				for (int i=0; i<notStartBooking.length; i++) {
					Log.d(TAG, "New booking "+notStartBooking[i].getID()+" will start, flag="+
						notStartBooking[i].getFlag() + ", wait the user's confirm...");
					onBookingEvent(Event.EVENT_NEW_BOOKING_CONFIRM, notStartBooking[i].getID());
				}
			}
			
			/* Checking conflict bookings */
			TVBooking conflictBooking[] = checkConflictBookings();
			if (conflictBooking != null){
				for (int i=0; i<conflictBooking.length; i++) {
					Log.d(TAG, "New booking "+conflictBooking[i].getID()+" got conflict, wait the user's confirm...");
					onBookingEvent(Event.EVENT_NEW_BOOKING_CONFLICT, conflictBooking[i].getID());
				}
			}
	
			/* Checking confirmed bookings */
			TVBooking confirmedBooking[] = checkConfirmedBookings();
			if (confirmedBooking != null){
				for (int i=0; i<confirmedBooking.length; i++) {
					Log.d(TAG, "New booking "+confirmedBooking[i].getID()+" starting now, flag="+
						confirmedBooking[i].getFlag());
					onBookingEvent(Event.EVENT_NEW_BOOKING_START, confirmedBooking[i].getID());
				}
			}

			handler.postDelayed(this, 5000);
			
		}
	};
	
	private void onBookingEvent(int type, int bookingID){
		Event evt = new Event(type);
		evt.bookingID = bookingID;
		onEvent(evt);
	}
	
	private TVBooking[] checkNotStartBookings(){
		TVBooking bookings[] = null;
		String sql = "select * from booking_table where status="+TVBooking.ST_NOT_START;
		sql += " and (start-"+tvTime.getTime()/1000+")<="+START_CONFIRM_TIME;
		sql += " order by start";
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, sql, null, null);
		if (c != null) {
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
	
	private TVBooking[] checkConfirmedBookings(){
		TVBooking bookings[] = null;
		String sql = "select * from booking_table where status<="+TVBooking.ST_CONFIRMED;
		sql += " and (start-"+tvTime.getTime()/1000+")<=0";
		sql += " order by start";
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, sql, null, null);
		if (c != null) {
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
	
	private TVBooking[] checkConflictBookings(){
		TVBooking b[] = null;
		/*String sql = "select * from booking_table where status<="+TVBooking.ST_CONFIRMED;
		sql += " and (start-"+tvTime.getTime()/1000+")<="+CONFLICT_CONFIRM_TIME;
		sql += " order by start";
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL, null, sql, null, null);
		if (c != null) {
			if(c.moveToFirst()){
				int id = 0;
				bookings = new TVBooking[c.getCount()];
				do{
					bookings[id++] = new TVBooking(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}*/
		
		return b;
	}
	
	public TVBookManager(Context context, TVTime time){
		this.context = context;
		tvTime = time;
	}
	
	public void onEvent(TVBookManager.Event evt){
	
	}
}
