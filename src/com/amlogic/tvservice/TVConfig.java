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
import java.util.Collections;
import java.util.Iterator;

import android.os.RemoteCallbackList;
import android.amlogic.Tv.tvin_info_t;
import android.content.Context;
import android.util.Log;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;

/**
 *TV 閰嶇疆绠＄悊
 */
public class TVConfig{
	private static final String TAG="TVConfig";
	private static final String CFG_FILE_NAME="tv.cfg";
	private static final String CFG_END_FLAG="config:end:flag";
	private static final String CFG_FILE_DEFAULT_NAME="tv_default.cfg";

	/**閰嶇疆椤逛笉瀛樺湪寮傚父*/
	public class NotExistException extends Exception{
	}

	/**閰嶇疆椤圭被鍨嬩笉鍖归厤*/
	public class TypeException extends Exception{
	}

	/**閰嶇疆鏂囦欢閿欒*/
	public class FileException extends Exception{
	}

	public class TVException extends Exception{
	}
	public interface Update{
		public void onUpdate(String name, TVConfigValue value);
	}

	
	public interface Read{
		public TVConfigValue read(String name);
	}

	
	private class TVConfigEntry{
		private TVConfigValue value;
		private Update update;
		private Read read;
		private RemoteCallbackList<ITVCallback> callbacks;
		private HashMap<String, TVConfigEntry> children;
		private TVConfigEntry parent;
	}

	/**鏍归厤缃」*/
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
			}catch(Exception e){
			}
		}

		if(ent.children != null){
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
	 *灏嗛厤缃繚瀛樺埌鏂囦欢
	 */
	public synchronized void save(Context context){
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

	/**
	 *鏋勯�鍑芥暟
	 */
	public TVConfig(Context context){
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

			is = null;
			try{
				File file = new File("/system/etc/"+CFG_FILE_DEFAULT_NAME);

				Log.d(TAG, "try to load "+file.toString());
				is = new FileInputStream(file);
				loadConfigFile(is);
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

		if(i >= names.length)
			return curr;

		boolean new_ent = false;
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
					value = parent.read.read(name);
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
	 * 
	 * @param name
	 * @param intArray
	 * @return
	 * @throws Exception
	 */
//	public synchronized TVConfigValue get(String name,TVConfigValue intArrayValue) throws Exception{
//		TVConfigEntry ent = getEntry(name);
//		
//		TVConfigValue myIntArray = null;
//		if (intArrayValue != null) {
//			int intArrayCustomValue[] = intArrayValue.getIntArray();
//			switch (intArrayCustomValue.length) {
//			case 0:
//				Log.e("TVConfig", "########No definition method#######");
//				break;
//			case 1:
//				myIntArray = new TVConfigValue(TVDeviceImpl.tv.TvITFExecute(name,intArrayCustomValue[0]));
//				break;
//			case 2:
//				myIntArray = new TVConfigValue(TVDeviceImpl.tv.TvITFExecute(name,intArrayCustomValue[0],intArrayCustomValue[1]));
//				break;
//			case 3:
//				Log.e("TVConfig", "########No definition method#######");
//				break;
//			case 4:
//				Log.e("TVConfig", "########No definition method#######");
//				break;
//			case 5:
//				Log.e("TVConfig", "########No definition method#######");
//				break;
//			case 6:
//				Log.e("TVConfig", "########No definition method#######");
//				break;
//
//			default:
//				break;
//			}
//		}
//		return myIntArray;
//	}
	/**
	 *鑾峰彇閰嶇疆椤圭殑鍊�	 *@param name 閰嶇疆椤圭殑鍚嶅瓧
	 *@return 杩斿洖閰嶇疆椤圭殑鍊�	 */
	public synchronized TVConfigValue get(String name) throws Exception{
		TVConfigEntry ent = getEntry(name);
		TVConfigValue myvalue = null;
		
		/*****************tv setting***********************/
		if(ent.value == null){
			if(TVDeviceImpl.tv == null)
				throw new TVException();
//			int val = TVDeviceImpl.tv.GetCurrentSourceInput();
			int val = 0;
			if (name.equals("GetAudioBalance")
					||name.equals("GetAudioSoundMode")
					||name.equals("GetAudioTrebleVolume")
					||name.equals("GetAudioBassVolume")
					||name.equals("GetAudioSupperBassVolume")
					||name.equals("GetAudioSRSSurround")
					||name.equals("GetAudioSrsDialogClarity")
					||name.equals("GetAudioSrsTruBass")) {
				myvalue = new TVConfigValue(TVDeviceImpl.tv.TvITFExecute(name));
				return myvalue;
			}else {
				myvalue = new TVConfigValue(TVDeviceImpl.tv.TvITFExecute(name,val));
				return myvalue;
			}
		}
		
		
		/**************************************************/
		return new TVConfigValue(ent.value);
	}

	/**
	 *鑾峰彇boolean鍨嬮厤缃」鐨勫�
	 *@param name 閰嶇疆椤圭殑鍚嶅瓧
	 *@return 杩斿洖閰嶇疆椤圭殑鍊�	 */
	public boolean getBoolean(String name) throws Exception{
		TVConfigValue v = get(name);

		if(v.getType() != TVConfigValue.TYPE_BOOL)
			throw new TypeException();

		return v.getBoolean();
	}

	/**
	 *鑾峰彇int鍨嬮厤缃」鐨勫�
	 *@param name 閰嶇疆椤圭殑鍚嶅瓧
	 *@return 杩斿洖閰嶇疆椤圭殑鍊�	 */
	public int getInt(String name) throws Exception{
		TVConfigValue v = get(name);

		if(v.getType() != TVConfigValue.TYPE_INT)
			throw new TypeException();

		return v.getInt();
	}

	/**
	 *鑾峰彇String鍨嬮厤缃」鐨勫�
	 *@param name 閰嶇疆椤圭殑鍚嶅瓧
	 *@return 杩斿洖閰嶇疆椤圭殑鍊�	 */
	public String getString(String name) throws Exception{
		TVConfigValue v = get(name);

		if(v.getType() != TVConfigValue.TYPE_STRING)
			throw new TypeException();

		return v.getString();
	}


	/**
	 *璁惧畾閰嶇疆椤圭殑鍊�	 *@param name 閰嶇疆椤圭殑鍚嶅瓧
	 *@param value 鏂拌瀹氬�
	 */
	public synchronized void set(String name, TVConfigValue value) throws Exception{
		TVConfigEntry ent = getEntry(name);
		if((ent.value != null) && (ent.value.getType() != TVConfigValue.TYPE_UNKNOWN) && (ent.value.getType() != value.getType()))
			throw new TypeException();

		ent.value = value;
		/*****************tv setting***********************/
//		if(ent.value == null){
			if(TVDeviceImpl.tv == null)
				throw new TVException();
			
			int type = value.getType();
			switch(type){
				case TVConfigValue.TYPE_INT:
					int userValue = value.getInt();
//					int val = TVDeviceImpl.tv.GetCurrentSourceInput();
					int val = 0;
					int status3D = TVDeviceImpl.tv.Get3DMode();
					tvin_info_t sig_fmt = TVDeviceImpl.tv.GetCurrentSignalInfo();
					
					int fmt = sig_fmt.fmt.ordinal();
					int ON = 1;
					if (name.equals("SetSharpness")) {
						TVDeviceImpl.tv.TvITFExecute(name,userValue,val,ON,status3D);
					}else if (name.equals("SetSaturation")||name.equals("SetDisplayMode")||name.equals("SetHue")) {
						TVDeviceImpl.tv.TvITFExecute(name,userValue,val,fmt);
					}else if (name.equals("SetAudioSoundMode")
							||name.equals("SetAudioBalance")
							||name.equals("SetAudioTrebleVolume")
							||name.equals("SetAudioBassVolume")
							||name.equals("SetAudioSupperBassVolume")
							||name.equals("SetAudioSRSSurround")
							||name.equals("SetAudioSrsDialogClarity")
							||name.equals("SetAudioSrsTruBass")) {
						TVDeviceImpl.tv.TvITFExecute(name,userValue);
					}else {
						TVDeviceImpl.tv.TvITFExecute(name,userValue,val);
					}
					
					break;
				case TVConfigValue.TYPE_BOOL:
					boolean boolValue = value.getBoolean();
					break;
				case TVConfigValue.TYPE_STRING:
					String  strValue =  value.getString();
					break;
				case TVConfigValue.TYPE_INT_ARRAY:
					if(value.getIntArray() != null){
						int intArrayValue[] = value.getIntArray();
					}
					break;
	//			case TVConfigValue.TYPE_CUSTOM_INT_ARRAY:
	//				if (value.getIntArray() != null) {
	//					int intArrayCustomValue[] = value.getIntArray();
	//					switch (intArrayCustomValue.length) {
	//					case 0:
	//						Log.e("TVConfig", "########No definition method#######");
	//						break;
	//					case 1:
	//						Log.e("TVConfig", "########No definition method#######");
	//						break;
	//					case 2:
	//						TVDeviceImpl.tv.TvITFExecute(name,intArrayCustomValue[0],intArrayCustomValue[1]);
	//						break;
	//					case 3:
	//						Log.e("TVConfig", "########No definition method#######");
	//						break;
	//					case 4:
	//						Log.e("TVConfig", "########No definition method#######");
	//						break;
	//					case 5:
	//						Log.e("TVConfig", "########No definition method#######");
	//						break;
	//					case 6:
	//						Log.e("TVConfig", "########No definition method#######");
	//						break;
	//					default:
	//						break;
	//					}
	//				}
			}
//			return;
//		}
		/**************************************************/
		
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
	 *娉ㄥ唽杩滅▼閰嶇疆椤瑰洖璋�	 *@param name 閰嶇疆椤瑰悕绉�	 *@param cb 鍥炶皟
	 */
	public synchronized void registerRemoteCallback(String name, ITVCallback cb) throws Exception{
		TVConfigEntry ent = getEntry(name);

		if(ent.callbacks == null)
			ent.callbacks = new RemoteCallbackList<ITVCallback>();

		ent.callbacks.register(cb);
	}

	/**
	 *閲婃斁杩滅▼閰嶇疆椤瑰洖璋�	 *@param name 閰嶇疆椤瑰悕绉�	 *@param cb 鍥炶皟
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

