package com.amlogic.tvservice;

import java.util.HashMap;
import java.lang.StringBuilder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVMessage;

/**
 *TV 配置管理
 */
public class TVConfig{
	private class TVConfigEntry{
		private String value;
		private RemoteCallbackList<ITVCallback> callbacks;
	}

	private HashMap<String, TVConfigEntry> entries = new HashMap<String, TVConfigEntry>();

	/**
	 *增加一个配置项
	 *@param name 配置项的名字
	 */
	public synchronized void add(String name){
		if(entries.get(name) != null)
			return;

		TVConfigEntry ent = new TVConfigEntry();

		entries.put(name, ent);
	}

	/**
	 *获取配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public synchronized String get(String name){
		TVConfigEntry ent = entries.get(name);

		if(ent == null)
			return null;

		return ent.value;
	}

	/**
	 *设定配置项的值
	 *@param name 配置项的名字
	 *@param value 新设定值
	 */
	public synchronized void set(String name, String value){
		TVConfigEntry ent = entries.get(name);

		if(ent != null){
			if(!ent.value.equals(value) && (ent.callbacks != null)){
				final int n = ent.callbacks.beginBroadcast();
				int i;

				for(i=0; i<n; i++){
					try{
						ent.callbacks.getBroadcastItem(i).onMessage(
								TVMessage.configChanged(name, value));
					}catch(RemoteException e){
					}
				}

				ent.callbacks.finishBroadcast();
			}
			ent.value = value;
		}
	}

	/**
	 *设定配置项的值(boolean类型)
	 *@param name 配置项的名字
	 *@param b 新设定值
	 */
	public void set(String name, boolean b){
		set(name, new Boolean(b).toString());
	}

	/**
	 *设定配置项的值(整形值)
	 *@param name 配置项的名字
	 *@param i 新设定值
	 */
	public void set(String name, int i){
		set(name, new Integer(i).toString());
	}

	/**
	 *设定配置项的值(整形数组值)
	 *@param name 配置项的名字
	 *@param i 新设定值
	 */
	public void set(String name, int i[]){
		String value;
		StringBuilder sb;

		if(i.length == 0){
			value = "";
		}else{
			sb = new StringBuilder();

			int c;
			for(c=0; c<i.length; c++){
				sb.append(i[c]);
				if((c != 0) && (c != i.length-1))
					sb.append(",");
			}

			value = sb.toString();
		}

		set(name, value);
	}

	/**
	 *获取布尔型配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public boolean getBoolean(String name){
		String value = get(name);
		if(value == null)
			return false;

		return Boolean.valueOf(value);
	}

	/**
	 *获取整形配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public int getInt(String name){
		String value = get(name);
		if(value == null)
			return 0;

		return Integer.valueOf(value);
	}

	/**
	 *获取整形数组配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public int[] getIntArrary(String name){
		String value = get(name);
		String strs[];
		int array[];
		int i;

		if(value == null)
			return null;

		strs = value.split(",");
		array = new int[strs.length];
		for(i=0; i<strs.length; i++){
			array[i] = Integer.valueOf(strs[i]);
		}

		return array;
	}

	/**
	 *注册配置项回调，当配置项的值被修改时被调用
	 *@param name 配置项名称
	 *@param cb 回调
	 */
	public synchronized void registerCallback(String name, ITVCallback cb){
		if(cb == null)
			return;

		TVConfigEntry ent = entries.get(name);

		if(ent == null)
			return;

		if(ent.callbacks == null)
			ent.callbacks = new RemoteCallbackList<ITVCallback>();

		ent.callbacks.register(cb);
	}

	/**
	 *释放配置项回调
	 *@param name 配置项名称
	 *@param cb 回调
	 */
	public synchronized void unregisterCallback(String name, ITVCallback cb){
		if(cb == null)
			return;

		TVConfigEntry ent = entries.get(name);

		if((ent == null) || (ent.callbacks == null))
			return;

		ent.callbacks.unregister(cb);
	}
}

