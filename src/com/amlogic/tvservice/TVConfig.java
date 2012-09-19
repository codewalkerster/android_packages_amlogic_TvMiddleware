package com.amlogic.tvservice;

import java.util.HashMap;
import java.lang.StringBuilder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;

/**
 *TV 配置管理
 */
public class TVConfig{
	private static final String TAG="TVConfig";

	/**配置项不存在异常*/
	public class NotExistException extends Exception{
	}

	/**配置项类型不匹配*/
	public class TypeException extends Exception{
	}

	/**配置项目值更新接口*/
	public interface Update{
		public void onUpdate(String name, TVConfigValue value);
	}

	/**配置项目值读取接口*/
	public interface Read{
		public TVConfigValue read(String name);
	}

	/**配置项*/
	private class TVConfigEntry{
		private TVConfigValue value;
		private Update update;
		private Read read;
		private RemoteCallbackList<ITVCallback> callbacks;
		private HashMap<String, TVConfigEntry> children;
		private TVConfigEntry parent;
	}

	/**根配置项*/
	private TVConfigEntry root;

	/**
	 *构造函数
	 */
	public TVConfig(){
		root = new TVConfigEntry();
	}

	private TVConfigEntry getEntry(String name) throws Exception{
		String names[] = name.split(":");
		TVConfigEntry ent, curr = root, parent;
		TVConfigValue value = null;
		int i;

		for(i = 0; i < names.length; i++){
			if(curr.children != null)
				curr.children = new HashMap<String, TVConfigEntry>();

			ent = curr.children.get(names[i]);
			if(ent != null)
				curr = ent;
			else
				break;
		}

		if(i >= names.length)
			return curr;

		parent = curr;
		while(parent != null){
			if(parent.read != null){
				value = parent.read.read(name);
				if(value != null)
					break;
			}
			parent = parent.parent;
		}

		for(; i < names.length; i++){
			ent = new TVConfigEntry();
			if(i == names.length - 1){
				ent.value = value;
				Log.d(TAG, "create new config entry "+name);
			}
			
			if(curr.children == null){
				curr.children = new HashMap<String, TVConfigEntry>();
			}
			curr.children.put(name, ent);
			curr = ent;
		}

		return curr;
	}

	/**
	 *获取配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public synchronized TVConfigValue get(String name) throws Exception{
		TVConfigEntry ent = getEntry(name);

		return new TVConfigValue(ent.value);
	}

	/**
	 *获取boolean型配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public boolean getBoolean(String name) throws Exception{
		TVConfigValue v = get(name);

		if(v.getType() != TVConfigValue.TYPE_BOOL)
			throw new TypeException();

		return v.getBoolean();
	}

	/**
	 *获取int型配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public int getInt(String name) throws Exception{
		TVConfigValue v = get(name);

		if(v.getType() != TVConfigValue.TYPE_INT)
			throw new TypeException();

		return v.getInt();
	}

	/**
	 *获取String型配置项的值
	 *@param name 配置项的名字
	 *@return 返回配置项的值
	 */
	public String getString(String name) throws Exception{
		TVConfigValue v = get(name);

		if(v.getType() != TVConfigValue.TYPE_STRING)
			throw new TypeException();

		return v.getString();
	}


	/**
	 *设定配置项的值
	 *@param name 配置项的名字
	 *@param value 新设定值
	 */
	public synchronized void set(String name, TVConfigValue value) throws Exception{
		TVConfigEntry ent = getEntry(name);

		if((ent.children == null) ||
				((ent.value != null) && (ent.value.getType() != TVConfigValue.TYPE_UNKNOWN) && (ent.value.getType() != value.getType())))
			throw new TypeException();

		ent.value = value;

		do{
			if(ent.update != null){
				ent.update.onUpdate(name, value);
			}
			if(ent.callbacks != null){
				final int N = ent.callbacks.beginBroadcast();
				TVMessage msg = TVMessage.configChanged(name, value);
				for (int i = 0; i < N; i++){
					ent.callbacks.getBroadcastItem(i).onMessage(msg);
				}
				ent.callbacks.finishBroadcast();
			}
			ent = ent.parent;
		}while(ent !=null);
	}


	/**
	 *注册远程配置项回调
	 *@param name 配置项名称
	 *@param cb 回调
	 */
	public synchronized void registerRemoteCallback(String name, ITVCallback cb) throws Exception{
		TVConfigEntry ent = getEntry(name);

		if(ent.callbacks == null)
			ent.callbacks = new RemoteCallbackList<ITVCallback>();

		ent.callbacks.register(cb);
	}

	/**
	 *释放远程配置项回调
	 *@param name 配置项名称
	 *@param cb 回调
	 */
	public synchronized void unregisterRemoteCallback(String name, ITVCallback cb) throws Exception{
		if(cb == null)
			return;

		TVConfigEntry ent = getEntry(name);

		if(ent.callbacks == null)
			return;

		ent.callbacks.unregister(cb);
	}

	synchronized void registerUpdate(String name, Update update) throws Exception{
		TVConfigEntry ent = getEntry(name);

		ent.update = update;
	}

	synchronized void registerRead(String name, Read read) throws Exception{
		TVConfigEntry ent = getEntry(name);

		ent.read = read;
	}
}

