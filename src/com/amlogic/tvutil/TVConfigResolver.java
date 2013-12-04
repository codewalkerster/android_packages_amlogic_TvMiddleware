package com.amlogic.tvutil;

import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import com.amlogic.tvdataprovider.TVDataProvider;


/**
 *TV config resolver
 *Read/Write config through TVDataprovider
 */
public class TVConfigResolver{
	private static final String TAG="TVConfigResolver";

	public static int getConfig(Context context, String name, int defaultValue){
		int ret = defaultValue;
		
		Cursor c = context.getContentResolver().query(
				TVDataProvider.RD_CONFIG_URL,
				null, name, new String[]{"Int"}, null);
		if(c != null){
			if(c.moveToFirst()){
				int col = c.getColumnIndex("value");
				ret = c.getInt(col);
			}
			c.close();
		}

		return ret;
	}

	public static String getConfig(Context context, String name, String defaultValue){
		String ret = defaultValue;
		
		Cursor c = context.getContentResolver().query(
				TVDataProvider.RD_CONFIG_URL,
				null, name, new String[]{"String"}, null);
		if(c != null){
			if(c.moveToFirst()){
				int col = c.getColumnIndex("value");
				ret = c.getString(col);
			}
			c.close();
		}

		return ret;
	}

	public static boolean getConfig(Context context, String name, boolean defaultValue){
		boolean ret = defaultValue;
		
		Cursor c = context.getContentResolver().query(
				TVDataProvider.RD_CONFIG_URL,
				null, name, new String[]{"Boolean"}, null);
		if(c != null){
			if(c.moveToFirst()){
				int col = c.getColumnIndex("value");
				ret = (c.getInt(col)==0) ? false : true;
			}
			c.close();
		}

		return ret;
	}

	//TODO: Add setConfig methods
}



