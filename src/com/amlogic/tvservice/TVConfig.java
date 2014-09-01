package com.amlogic.tvservice;

import java.util.HashMap;
import java.util.Map;
import java.lang.StringBuilder;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.content.Context;
import android.util.Log;
import android.os.Handler;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;

import android.content.res.AssetManager; 

/**
 *TV 配置管理
 */
public class TVConfig{
	private static final String TAG="TVConfig";
	private static final String CFG_FILE_NAME="tv.cfg";
	private static final String CFG_END_FLAG="config:end:flag";
	private static final String CFG_FILE_DEFAULT_NAME="tv_default.cfg";

	private Context context;

	/**配置项不存在异常*/
	public class NotExistException extends Exception{
	}

	/**配置项类型不匹配*/
	public class TypeException extends Exception{
	}

	/**配置文件错误*/
	public class FileException extends Exception{
	}

	/**配置项目值更新接口*/
	public interface Update{
		public void onUpdate(String name, TVConfigValue value);
	}

	/**配置项目值读取接口*/
	public interface Read{
		public TVConfigValue read(String name, TVConfigEntry entry);
	}

	/**配置项*/
	class TVConfigEntry{
		private TVConfigValue value;
		private Update update;
		private Read read;
		private RemoteCallbackList<ITVCallback> callbacks;
		private HashMap<String, TVConfigEntry> children;
		private TVConfigEntry parent;
		private boolean cacheable;

		TVConfigEntry(){
			cacheable = true;
		}

		void setCacheable(boolean v){
			cacheable = v;
		}
	}

	/**根配置项*/
	private TVConfigEntry root;

	private void loadConfigFile(InputStream is) throws Exception{
		InputStreamReader isr = null;
		BufferedReader br = null;
		String line;
		boolean flag = false;

		isr = new InputStreamReader(is);
		br  = new BufferedReader(isr);

		while((line = br.readLine()) != null){
			if(line.matches("[ \\t\\n]*"))
				continue;

			String sub[] = line.split("=");
			String name  = sub[0];
			String vstr  = sub[1];
			TVConfigValue value;

			name = name.replaceAll("[ \\t\\n]*([\\w:]+)[ \\t\\n]*", "$1");

			if(vstr.matches("[ \\t\\n]*\".*\"[ \\t\\n]*")){
				String sv = vstr.replaceAll("[ \\t\\n]*\"(.*)\"[ \\t\\n]*", "$1");

				value = new TVConfigValue(sv);

				Log.d(TAG, name+"=\""+sv+"\"");
			}else if(vstr.matches("[ \\t\\n]*true[ \\t\\n]*")){
				value = new TVConfigValue(true);

				Log.d(TAG, name+"=true");
			}else if(vstr.matches("[ \\t\\n]*false[ \\t\\n]*")){
				value = new TVConfigValue(false);

				Log.d(TAG, name+"=false");
			}else{
				String istrs[] = vstr.split(",");

				if(istrs.length == 1){
					String istr = istrs[0].replaceAll("[ \\t\\n]*([+-]?\\d*)", "$1");

					value = new TVConfigValue(Integer.parseInt(istr));

					Log.d(TAG, name+"="+Integer.parseInt(istr));
				}else{
					int v[] = new int[istrs.length];
					int i;

					for(i = 0; i < istrs.length; i++){
						String istr = istrs[i].replaceAll("[ \\t\\n]*([+-]?\\d*)", "$1");

						v[i] = Integer.parseInt(istr);
					}

					value = new TVConfigValue(v);

					Log.d(TAG, name+"="+vstr);
				}
			}

			if(name.equals(CFG_END_FLAG)){
				flag = true;
			}else{
				set(name, value);
			}
		}

		if(!flag){
			Log.e(TAG, "cannot get config end flag");
			throw new FileException();
		}
	}

	private class ConfigString{
		private String name;
		private String value;

		private ConfigString(String name, String value){
			this.name  = name;
			this.value = value;
		}
	}

	private class ConfigStringComparator implements Comparator{
		public int compare(Object lhs, Object rhs){
			ConfigString l = (ConfigString)lhs;
			ConfigString r = (ConfigString)rhs;
			return l.name.compareTo(r.name);
		}
	}

	private void getConfigStrings(ArrayList<ConfigString> list, String pname, TVConfigEntry ent){
		if(ent.value != null){
			String val = "";

			try{
				if(ent.read==null){
					switch(ent.value.getType()){
						case TVConfigValue.TYPE_STRING:
							val = "\""+ent.value.getString()+"\"";
							break;
						case TVConfigValue.TYPE_BOOL:
							val = ent.value.getBoolean()?"true":"false";
							break;
						case TVConfigValue.TYPE_INT:
							val = new Integer(ent.value.getInt()).toString();
							break;
						case TVConfigValue.TYPE_INT_ARRAY:
							StringBuilder sb = new StringBuilder();
							int v[] = ent.value.getIntArray();
							int i;
							for(i = 0; i < v.length; i++){
								if(i != 0)
									sb.append(",");
								sb.append(new Integer(v[i]).toString());
							}
							val = sb.toString();
							break;
					}

					ConfigString cstr = new ConfigString(pname, val);
					list.add(cstr);
				}
			}catch(Exception e){
			}
		}

		if(ent.children != null && ent.read == null){
			Iterator iter = ent.children.entrySet().iterator();
			while(iter.hasNext()){
				Map.Entry map_entry = (Map.Entry) iter.next();

				String name = (String)map_entry.getKey();
				TVConfigEntry child = (TVConfigEntry)map_entry.getValue();

				String cname;

				if(pname == null)
					cname = name;
				else
					cname = pname+":"+name;

				getConfigStrings(list, cname, child);
			}
		}
	}

	/**
	 *将配置保存到文件
	 */
	public synchronized void save(){
		ArrayList<ConfigString> list = new ArrayList<ConfigString>();

		getConfigStrings(list, null, root);

		Collections.sort(list, new ConfigStringComparator());

		FileOutputStream fos = null;
		OutputStreamWriter osw = null;

		try{
			fos = context.openFileOutput(CFG_FILE_NAME, context.MODE_PRIVATE);
			osw = new OutputStreamWriter(fos);

			int i;
			for(i = 0; i < list.size(); i++){

				ConfigString cstr = list.get(i);

				osw.write(cstr.name);
				osw.write('=');
				osw.write(cstr.value);
				osw.write('\n');

				Log.d(TAG, "save "+cstr.name+"="+cstr.value);
			}

			osw.write(CFG_END_FLAG+"=true");
		}catch(Exception e){
			Log.d(TAG, "write config file failed "+e.getMessage());
		}finally{
			try{
				if(osw != null)
					osw.close();
				if(fos != null)
					fos.close();
			}catch(Exception e){
			}
		}
	}

	private void init(){
		root = new TVConfigEntry();

		InputStream is = null;
		boolean loaded = false;
		try{
			Log.d(TAG, "try to load "+CFG_FILE_NAME);
			is = context.openFileInput(CFG_FILE_NAME);
			loadConfigFile(is);
			loaded = true;
		}catch(Exception e){
			Log.d(TAG, "load config failed");
		}finally{
			try{
				if(is != null)
					is.close();
			}catch(Exception e){
			}
		}

		if(!loaded){
			root = new TVConfigEntry();

			AssetManager assetManager = this.context.getAssets(); 	

			is = null;
			try{

				/*	
				File file = new File("/system/etc/"+CFG_FILE_DEFAULT_NAME);

				Log.d(TAG, "try to load "+file.toString());
				is = new FileInputStream(file);
				loadConfigFile(is);
				*/
			
				is = assetManager.open(CFG_FILE_DEFAULT_NAME);

				
			}catch(Exception e){
				Log.d(TAG, "load config failed");
			}finally{
				try{
					if(is != null)
						is.close();
				}catch(Exception e){
				}
			}
		}
	}

	/**
	 *构造函数
	 */
	public TVConfig(Context context){
		this.context = context;
		init();
	}

	private TVConfigEntry getEntry(String name) throws Exception{
		String names[] = name.split(":");
		TVConfigEntry ent, curr = root, parent;
		TVConfigValue value = null;
		int i;

		for(i = 0; i < names.length; i++){
			if(curr.children == null)
				curr.children = new HashMap<String, TVConfigEntry>();

			ent = curr.children.get(names[i]);
			if(ent != null)
				curr = ent;
			else
				break;
		}

		boolean new_ent = false;

		if(i >= names.length){
			if(curr.cacheable)
				return curr;
			else
				new_ent = true;
		}

		for(; i < names.length; i++){
			ent = new TVConfigEntry();
			ent.parent = curr;
			if(curr.children == null){
				curr.children = new HashMap<String, TVConfigEntry>();
			}
			curr.children.put(names[i], ent);
			curr = ent;
			new_ent = true;
		}

		if(new_ent){
			parent = curr;
			while(parent != null){
				if(parent.read != null){
					value = parent.read.read(name, curr);
					if(value != null)
						break;
				}
				parent = parent.parent;
			}

			if(value != null){
				curr.value = value;
			}
		}

		return curr;
	}

	/**
	 *设定配置项的值是否缓存
	 *@param v true 表示缓存，false表示不缓存
	 */
	public synchronized void setCacheable(String name, boolean v) throws Exception{
		TVConfigEntry ent = getEntry(name);

		ent.setCacheable(v);
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

	/*Save the configure data to file.*/
	private boolean need_save = false;
	private Handler save_handler = new Handler();
	Runnable save_runnable = new Runnable(){
		@Override
		public void run() {
			synchronized(TVConfig.this){
				if(need_save){
					need_save = false;
					save();
				}
			}
		}
	};

	/**
	 *设定配置项的值
	 *@param name 配置项的名字
	 *@param value 新设定值
	 */
	public synchronized void set(String name, TVConfigValue value) throws Exception{
		TVConfigEntry ent = getEntry(name);
		TVConfigEntry org_ent = ent;

		if((ent.value != null) && (ent.value.getType() != TVConfigValue.TYPE_UNKNOWN) && (ent.value.getType() != value.getType()))
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
					Log.d(TAG, "config "+name+" callback "+i);
					ent.callbacks.getBroadcastItem(i).onMessage(msg);
				}
				ent.callbacks.finishBroadcast();
			}
			ent = ent.parent;
		}while(ent !=null);

		/*Need to save the data*/
		if(org_ent.read == null){
			synchronized(this){
				if(!need_save){
					need_save = true;
					save_handler.postDelayed(save_runnable, 200);
				}
			}
		}
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

		Log.d(TAG, "registerRemoteCallback "+name);
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

	/**
	 *恢复缺省设置
	 */
	public synchronized void restore(){
		File file = new File(context.getFilesDir(), CFG_FILE_NAME);
		if(file.exists())
			file.delete();

		init();
	}
}

