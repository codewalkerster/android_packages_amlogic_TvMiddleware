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
	private TVTime tvTime = null;
	private Handler handler = new Handler();
	private Context context;
	
	private Runnable bookManagerTask = new Runnable() {
		public void run() {
			/* Checking not start bookings */
			TVBooking notStartBooking[] = checkNotStartBookings();
			if (notStartBooking != null){
				for (int i=0; i<notStartBooking.length; i++) {
					Log.d(TAG, "New booking "+notStartBooking[i].getID()+" will start, flag="+
						notStartBooking[i].getFlag() + ", wait the user's confirm...");
					onBookingEvent(Event.EVENT_NEW_BOOKING_CONFIRM, notStartBooking[i].getID());
					notStartBooking[i].updateStatus(TVBooking.ST_CONFIRMED);
				}
			}
			
			/* Checking confirmed bookings */
			TVBooking confirmedBooking[] = checkConfirmedBookings();
			if (confirmedBooking != null){
				for (int i=0; i<confirmedBooking.length; i++) {
					if (i == 0) {
						/* Start the first one */
						Log.d(TAG, "New booking "+confirmedBooking[i].getID()+" starting now, flag="+
							confirmedBooking[i].getFlag());
						onBookingEvent(Event.EVENT_NEW_BOOKING_START, confirmedBooking[i].getID());
						if ((confirmedBooking[i].getFlag()&TVBooking.FL_RECORD) != 0) {
							confirmedBooking[i].updateStatus(TVBooking.ST_STARTED);
						} else {
							confirmedBooking[i].updateStatus(TVBooking.ST_END);
						}
					} else {
						/* Auto delete the elses  */
						confirmedBooking[i].delete();
					}
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
	
	public TVBookManager(Context context, TVTime time){
		this.context = context;
		tvTime = time;
		
		handler.postDelayed(bookManagerTask, 1000);
	}
	
	public void onEvent(TVBookManager.Event evt){
	
	}
}
