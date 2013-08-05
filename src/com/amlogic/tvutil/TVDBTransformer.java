package com.amlogic.tvutil;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class TVDBTransformer{
	public static final int DB_TO_XML = 0;
	public static final int XML_TO_DB = 1;

	private static final String DTD_PATH = "/system/etc/tv_default.dtd";

	private static final String feTypes[] = {"dvbs", "dvbc", "dvbt", "atsc", "analog", "dtmb"};
	private static final String srvTypes[] = {"other", "dtv", "radio", "atv", "other"};
	private static final String vidFmts[] = {"mpeg12", "mpeg4", "h264", "mjpeg", "real", "jpeg", "vc1", "avs"};
	private static final String audFmts[] = {"mpeg", "pcm_s16le", "aac", "ac3", "alaw", "mulaw", "dts", "pcm_s16be",
											"flac", "cook", "pcm_u8", "adpcm", "amr", "raac", "wma", "wma_pro", 
											"pcm_bluray", "alac", "vorbis", "aac_latm", "ape", "eac3", "pcm_wifidisplay"};
	private static final String mods[] = {"qpsk", "qam16", "qam32", "qam64", "qam128", "qam256", "qamauto", "vsb8", "vsb16", "psk8", "apsk16", "apsk32", "dqpsk"};
	private static final String bandwidths[] = {"8", "7", "6", "auto", "5", "10", "1_712"};
	private static final String lnbPowers[] = {"13v", "18V", "off", "13/18v"};
	private static final String sig22K[] = {"on", "off", "auto"};
	private static final String tonebursts[] = {"none", "bursta", "burstb"};
	private static final String diseqc10s[] = {"lnb1", "lnb2", "lnb3", "lnb4", "none"};
	private static final String diseqc11s[] = {"lnb1", "lnb2", "lnb3", "lnb4", "lnb5", "lnb6", "lnb7", "lnb8",
											   "lnb9", "lnb10", "lnb11", "lnb12","lnb13", "lnb14", "lnb15", "lnb16","none"};
	private static final String motors[] = {"none", "none", "none", "diseqc1.2", "diseqc1.3"};
	private static final String ofdmModes[] = {"dvbt", "dvbt2"};
	private static final String atvVideoStds[] = {"auto", "pal", "ntsc", "secam"};
	private static final String atvAudioStds[] = {"dk", "i", "bg", "m", "l", "auto"};
	

	private static final String TAG = "TVDBTransformer";

	public static class InvalidFileException extends Exception{
		
	}

	private static class MyErrorHandler implements ErrorHandler {
		public void warning(SAXParseException e) throws SAXException {
			Log.d(TAG, "Warning: "); 
			printInfo(e);
		}
		public void error(SAXParseException e) throws SAXException {
			Log.d(TAG, "Error: "); 
			printInfo(e);
		}
		public void fatalError(SAXParseException e) throws SAXException {
			Log.d(TAG, "Fattal error: "); 
			printInfo(e);
		}
		private void printInfo(SAXParseException e) {
			Log.d(TAG, "   Public ID: "+e.getPublicId());
			Log.d(TAG, "   System ID: "+e.getSystemId());
			Log.d(TAG, "   Line number: "+e.getLineNumber());
			Log.d(TAG, "   Column number: "+e.getColumnNumber());
			Log.d(TAG, "   Message: "+e.getMessage());
		}
	}

	private static int stringToValue(String[] array, String str){
		for (int i=0; i<array.length; i++){
			if (array[i].equals(str)){
				return i;
			}
		}

		return -1;
	}

	private static int getIntValue(Cursor c, String strVal, int default_val){
		int col = c.getColumnIndex(strVal);
		
		if (col >= 0){
			return c.getInt(col);
		}

		return default_val;
	}
	
	private static String getStringValue(Cursor c, String strVal, int default_val){
		int col = c.getColumnIndex(strVal);
		
		if (col >= 0){
			return Integer.toString(c.getInt(col));
		}

		return Integer.toString(default_val);
	}

	private static String getStringValue(Cursor c, String strVal, String default_val){
		int col = c.getColumnIndex(strVal);
		
		if (col >= 0){
			return c.getString(col);
		}

		return default_val;
	}

	private static int getIntAttr(String strAttr, int default_val){
		if (strAttr != null && !strAttr.isEmpty()){
			return Integer.parseInt(strAttr);
		}

		return default_val;
	}

	private static String getStringAttr(String strAttr, String default_val){
		if (strAttr != null && !strAttr.isEmpty()){
			return strAttr;
		}

		return default_val;
	}

	private static void createGroupElements(SQLiteDatabase db, Document document, Element elemParent){
		Cursor curGroup = db.rawQuery("select * from grp_table", null);
		if (curGroup != null){
			if (curGroup.moveToFirst()){
				Element elemGroup;
				int dbGrpId, dbSrvId;
				String strPrograms = "";
				
				do{
					elemGroup = document.createElement("program_group");
					elemParent.appendChild(elemGroup);
					
					elemGroup.setAttribute("name", getStringValue(curGroup, "name", ""));
					dbGrpId = getIntValue(curGroup, "db_id", -1);

					Cursor curGrpMap = db.rawQuery("select * from grp_map_table where db_grp_id="+dbGrpId, null);
					if (curGrpMap != null){
						if (curGrpMap.moveToFirst()){
							strPrograms = "";
							do{
								dbSrvId = getIntValue(curGrpMap, "db_srv_id", -1);
								if (dbSrvId >= 0){
									if (! strPrograms.isEmpty()){
										strPrograms += " ";
									}
									
									strPrograms += Integer.toString(dbSrvId);
								}
								
							}while(curGrpMap.moveToNext());
						}
						curGrpMap.close();
					}

					elemGroup.setAttribute("programs", strPrograms);
					
				}while(curGroup.moveToNext());
			}
			curGroup.close();
		}
	}

	private static void createChannelListElements(SQLiteDatabase db, Document document, Element elemParent){
		Cursor curRegion = db.rawQuery("select distinct name from region_table", null);
		if (curRegion != null){
			if (curRegion.moveToFirst()){
				Element elemChannelEntry, elemChannelList;
				String strName;
				int mode;
				do{
					elemChannelList = document.createElement("channel_list");
					elemParent.appendChild(elemChannelList);
					strName = getStringValue(curRegion, "name", "");
					elemChannelList.setAttribute("name", strName);
					mode = getIntValue(curRegion, "fe_type", 0);
					elemChannelList.setAttribute("fe_type", feTypes[mode]);

					Log.d(TAG, "Loading "+strName+", mode "+mode);
					Cursor curEntry = db.rawQuery("select * from region_table where name='"+strName+"'", null);
					if (curEntry != null){
						if (curEntry.moveToFirst()){
							mode = getIntValue(curEntry, "fe_type", 0);
							elemChannelList.setAttribute("fe_type", feTypes[mode]);
							do{
								elemChannelEntry = document.createElement("channel_entry");
								elemChannelList.appendChild(elemChannelEntry);

								elemChannelEntry.setAttribute("frequency", getStringValue(curEntry, "frequency", 0));
								if (mode == 1){
									elemChannelEntry.setAttribute("modulation", mods[3]);
									elemChannelEntry.setAttribute("symbol_rate", Integer.toString(6875000));
								}else if (mode == 2){
									elemChannelEntry.setAttribute("bandwidth", bandwidths[getIntValue(curEntry, "bandwidth", 0)]);
									elemChannelEntry.setAttribute("ofdm_mode", "dvbt");
								}else if (mode == 3){
									elemChannelEntry.setAttribute("modulation", mods[7]);
								}else if (mode == 5){
									elemChannelEntry.setAttribute("bandwidth", bandwidths[getIntValue(curEntry, "bandwidth", 0)]);
								}else{
								}
							}while(curEntry.moveToNext());
						}
						curEntry.close();
					}
					
				}while(curRegion.moveToNext());
			}
			curRegion.close();
		}
	}

	/*private static void createChannelListElements(Context context, SQLiteDatabase db, Document document, Element elemParent){
		Cursor curRegion = db.rawQuery("select * from region_table", null);
		if (curRegion != null){
			if (curRegion.moveToFirst()){
				Element elemChannelEntry, elemChannelList;
				String strName;
				int mode;
				do{
					elemChannelList = document.createElement("channel_list");
					elemParent.appendChild(elemChannelList);
					strName = getStringValue(curRegion, "name", "");
					elemChannelList.setAttribute("name", strName);
					mode = getIntValue(curRegion, "source", 0);
					elemChannelList.setAttribute("fe_type", feTypes[mode]);

					Log.d(TAG, "Loading "+strName+", mode "+mode);
					Cursor curEntry = db.rawQuery("select * from region_table where name='"+strName+"'", null);
					if (curEntry != null){
						if (curEntry.moveToFirst()){
							do{
								TVChannelParams[] params = TVChannelParams.channelCurAllbandParams(context, strName, mode);
								if (params != null){
									Log.d(TAG, "params length is "+params.length);
									for (int i=0; i<params.length; i++){
										elemChannelEntry = document.createElement("channel_entry");
										elemChannelList.appendChild(elemChannelEntry);


										elemChannelEntry.setAttribute("frequency", Integer.toString(params[i].frequency));
										if (mode == 1){
											elemChannelEntry.setAttribute("modulation", mods[3]);
											elemChannelEntry.setAttribute("symbol_rate", Integer.toString(6875000));
										}else if (mode == 2){
											elemChannelEntry.setAttribute("bandwidth", bandwidths[params[i].bandwidth]);
											elemChannelEntry.setAttribute("ofdm_mode", "dvbt");
										}else if (mode == 3){
											elemChannelEntry.setAttribute("modulation", mods[7]);
										}else{
										}
										
										
									}
								}
							}while(curEntry.moveToNext());
						}
						curEntry.close();
					}
				}while(curRegion.moveToNext());
			}
			curRegion.close();
		}
	}*/
	
	private static void createProgramElements(SQLiteDatabase db, Document document, Element elemTP, int chanID){
		Cursor curProgram = db.rawQuery("select * from srv_table where db_ts_id="+chanID, null);
		if (curProgram != null){
			if (curProgram.moveToFirst()){
				Element elemProg, elemVid, elemAud;
				
				do{
					elemProg = document.createElement("program");
					elemTP.appendChild(elemProg);

					elemProg.setAttribute("name", getStringValue(curProgram, "name", ""));
					elemProg.setAttribute("service_id", getStringValue(curProgram, "service_id", 65535));
					elemProg.setAttribute("channel_number", getStringValue(curProgram, "chan_num", 0));
					elemProg.setAttribute("type", srvTypes[getIntValue(curProgram, "service_type", 0)]);
					elemProg.setAttribute("scrambled", getIntValue(curProgram, "scrambled_flag", 0)==0 ? "false" : "true");
					elemProg.setAttribute("parental_lock", getIntValue(curProgram, "lock", 0)==0 ? "false" : "true");
					elemProg.setAttribute("skip", getIntValue(curProgram, "skip", 0)==0 ? "false" : "true");
					elemProg.setAttribute("id", getStringValue(curProgram, "db_id", -1));

					int vidPid = getIntValue(curProgram, "vid_pid", 0x1fff);
					int vidFmt = getIntValue(curProgram, "vid_fmt", 0);
					if (vidPid < 0x1fff && vidFmt >= 0 && vidFmt < vidFmts.length){
						
						elemVid = document.createElement("video");
						elemProg.appendChild(elemVid);

						elemVid.setAttribute("pid", Integer.toString(vidPid));
						elemVid.setAttribute("format", vidFmts[vidFmt]);
					}

					String apids = getStringValue(curProgram, "aud_pids", "");
					String afmts = getStringValue(curProgram, "aud_fmts", "");
					String alangs = getStringValue(curProgram, "aud_langs", "");
					if (!apids.isEmpty()){
						String[] pids = apids.split(" ");
						String[] fmts = afmts.split(" ");
						String[] langs = alangs.split(" ");
						if (pids.length > 0){
							for (int i=0; i<pids.length; i++){
								elemAud = document.createElement("audio");
								elemProg.appendChild(elemAud);

								elemAud.setAttribute("pid", pids[i]);
								elemAud.setAttribute("format", audFmts[Integer.parseInt(fmts[i])]);
								elemAud.setAttribute("language", langs[i]);
							}
						}
					}

					
				}while(curProgram.moveToNext());
			}
			curProgram.close();
		}
	}
	
	private static void databaseToXml(SQLiteDatabase db, String xmlPath) throws Exception{
		Log.d(TAG, "Creating xml file "+xmlPath);
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		DocumentBuilder builder=factory.newDocumentBuilder();
		Document document=builder.newDocument();
		
		document.setXmlVersion("1.0");
		
		Element root=document.createElement("db");
		document.appendChild(root);
		
		Cursor curSat = db.rawQuery("select * from sat_para_table", null);
		if (curSat != null){
			if (curSat.moveToFirst()){
				int col;
				int dbSatID;
				Element elemSat, elemTP;
				
				do{
					dbSatID = getIntValue(curSat, "db_id", -1);

					elemSat = document.createElement("satellite");
					root.appendChild(elemSat);
					elemSat.setAttribute("name", getStringValue(curSat, "sat_name", ""));
					elemSat.setAttribute("longitude", getStringValue(curSat, "sat_longitude", 0));
					elemSat.setAttribute("lof_hi", getStringValue(curSat, "lof_hi", 10600));
					elemSat.setAttribute("lof_lo", getStringValue(curSat, "lof_lo", 9750));
					elemSat.setAttribute("lof_threshold", getStringValue(curSat, "lof_threshold", 11700));
					elemSat.setAttribute("lnb_power", lnbPowers[getIntValue(curSat, "voltage", 3)]);
					elemSat.setAttribute("signal_22khz", sig22K[getIntValue(curSat, "signal_22khz", 2)]);
					elemSat.setAttribute("toneburst", tonebursts[getIntValue(curSat, "tone_burst", 0)]);
					elemSat.setAttribute("diseqc1_0", diseqc10s[getIntValue(curSat, "committed_cmd", 4)]);
					int diseqc11Val = getIntValue(curSat, "uncommitted_cmd", 4);
					if (diseqc11Val < 0xF0 || diseqc11Val > 0xFF)
						diseqc11Val = 16;
					else
						diseqc11Val -= 0xF0;

					elemSat.setAttribute("diseqc1_1", diseqc11s[diseqc11Val]);
					elemSat.setAttribute("motor", motors[getIntValue(curSat, "diseqc_mode", 0)]);
					
					Cursor curTP = db.rawQuery("select * from ts_table where db_sat_para_id="+dbSatID, null);
					if (curTP != null && curTP.moveToFirst()){
						do{
							elemTP = document.createElement("transponder");
							elemSat.appendChild(elemTP);

							String networkID = "65535";
							Cursor curNet = db.rawQuery("select * from net_table where db_id="+getIntValue(curTP, "db_net_id", -1), null);
							if (curNet != null){
								if (curNet.moveToFirst()){
									networkID = getStringValue(curNet, "network_id", 65535);
								}
								curNet.close();
							}
							elemTP.setAttribute("original_network_id", networkID);
							elemTP.setAttribute("ts_id", getStringValue(curTP, "ts_id", 65535));
							elemTP.setAttribute("frequency", getStringValue(curTP, "freq", 0));
							elemTP.setAttribute("symbol_rate",getStringValue(curTP, "symb", 0));
							elemTP.setAttribute("polarisation", getIntValue(curTP, "polar", 1)==0 ? "V" : "H");

							createProgramElements(db, document, elemTP, getIntValue(curTP, "db_id", -1));
						}while(curTP.moveToNext());
					}
				}while(curSat.moveToNext());
			}
			curSat.close();
		}
		
		
		Cursor curChan = db.rawQuery("select * from ts_table where db_sat_para_id < 0", null);
		if (curChan != null){
			if (curChan.moveToFirst()){
				Element elemChan;
				do{
					elemChan = document.createElement("channel");
					root.appendChild(elemChan);

					int src = getIntValue(curChan, "src", 0);

					elemChan.setAttribute("fe_type", getStringValue(curChan, "src", "dvbc"));
					elemChan.setAttribute("frequency", getStringValue(curChan, "freq", 0));

					String networkID = "65535";
					Cursor curNet = db.rawQuery("select * from net_table where db_id="+getIntValue(curChan, "db_net_id", -1), null);
					if (curNet != null){
						if (curNet.moveToFirst()){
							networkID = getStringValue(curNet, "network_id", 65535);
						}
						curNet.close();
					}
					elemChan.setAttribute("original_network_id", networkID);
					elemChan.setAttribute("ts_id", getStringValue(curChan, "ts_id", 65535));

					if (src == 1){
						elemChan.setAttribute("symbol_rate", getStringValue(curChan, "symb", 0));
						elemChan.setAttribute("modulation", mods[getIntValue(curChan, "mod", 0)]);
					}else if (src == 2){
						elemChan.setAttribute("bandwidth", bandwidths[getIntValue(curChan, "bw", 0)]);
						//FIXME: support dvbt2
						elemChan.setAttribute("ofdm_mode", "dvbt");
					}else if (src == 3){
						elemChan.setAttribute("modulation", mods[getIntValue(curChan, "mod", 0)]);
					}else if (src == 4){
						int std = getIntValue(curChan, "std", 0);
						int vstd = TVChannelParams.VideoStd2Enum(std).toInt();
						int astd = TVChannelParams.AudioStd2Enum(std).toInt();
						
						elemChan.setAttribute("video_standard", atvVideoStds[vstd]);
						elemChan.setAttribute("audio_standard", atvAudioStds[astd]);
						elemChan.setAttribute("sound_sys", "a2");
					}else if (src == 5){
						elemChan.setAttribute("bandwidth", bandwidths[getIntValue(curChan, "bw", 0)]);
					}

					createProgramElements(db, document, elemChan, getIntValue(curChan, "db_id", -1));
				}while(curChan.moveToNext());
			}
			curChan.close();
		}

		createGroupElements(db, document, root);
		
		createChannelListElements(db, document, root);


		TransformerFactory transFactory=TransformerFactory.newInstance();

	    Transformer transformer=transFactory.newTransformer();

	    transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); 
	    transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8"); 
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); 

	    DOMSource domSource=new DOMSource(document);
		
	    File file = new File(xmlPath);
	    FileOutputStream fos = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		Writer out = new BufferedWriter(osw);
		
	    StreamResult xmlResult=new StreamResult(out);
		
	    transformer.transform(domSource,xmlResult);
	}

	private static int insertNet(SQLiteDatabase db, int fe_type, int networkID){
		ContentValues cv = new ContentValues();
		int ret = -1;

		Cursor c = db.rawQuery("select * from net_table where src="+fe_type+" and network_id="+networkID, null);
		if (c != null){
			if (c.moveToFirst()){
				ret = getIntValue(c, "db_id", -1);
			}
			c.close();
		}

		if (ret < 0){
			cv.clear();
			cv.put("src", fe_type);
			cv.put("network_id", networkID);
			cv.put("name", "");
		
			db.insert("net_table", "", cv);

			c = db.rawQuery("select * from net_table where src="+fe_type+" and network_id="+networkID, null);
			if (c != null){
				if (c.moveToFirst()){
					ret = getIntValue(c, "db_id", -1);
				}
				c.close();
			}
		}

		return ret;
	}

	private static void insertSatellitePara(Element elemSat, SQLiteDatabase db){
		ContentValues cvSat = new ContentValues();

		cvSat.clear();
		cvSat.put("sat_name", getStringAttr(elemSat.getAttribute("name"), ""));
		cvSat.put("lnb_num", 0);
		cvSat.put("lof_hi", getIntAttr(elemSat.getAttribute("lof_hi"), 9750));
		cvSat.put("lof_lo", getIntAttr(elemSat.getAttribute("lof_lo"), 10600));
		cvSat.put("lof_threshold", getIntAttr(elemSat.getAttribute("lof_threshold"), 11700));
		cvSat.put("signal_22khz", stringToValue(sig22K, getStringAttr(elemSat.getAttribute("signal_22khz"), "auto")));
		cvSat.put("voltage", stringToValue(lnbPowers, getStringAttr(elemSat.getAttribute("lnb_power"), "13/18v")));
		cvSat.put("motor_num", 0);
		cvSat.put("pos_num", 0);
		cvSat.put("lo_direction", 0);
		cvSat.put("la_direction", 0);
		cvSat.put("longitude", 0.0);
		cvSat.put("latitude", 0.0);
		cvSat.put("sat_longitude", getIntAttr(elemSat.getAttribute("longitude"), 0));
		int diseqcMode = stringToValue(motors, getStringAttr(elemSat.getAttribute("motor"), "none"));
		//To ensure at least 1.1
		if (diseqcMode <= 2)
			diseqcMode = 2;
		cvSat.put("diseqc_mode", diseqcMode);
		cvSat.put("tone_burst", stringToValue(tonebursts, getStringAttr(elemSat.getAttribute("touneburst"), "none")));
		cvSat.put("committed_cmd", stringToValue(diseqc10s, getStringAttr(elemSat.getAttribute("diseqc1_0"), "none")));
		int diseqc11Val = stringToValue(diseqc11s, getStringAttr(elemSat.getAttribute("diseqc1_1"), "none"));
		if (diseqc11Val < 16)
			diseqc11Val += 0xF0;
		else
			diseqc11Val = 4;
		cvSat.put("uncommitted_cmd", diseqc11Val);
		cvSat.put("repeat_count", 0);
		cvSat.put("sequence_repeat", 0);
		cvSat.put("fast_diseqc", 0);
		cvSat.put("cmd_order", 0);

		db.insert("sat_para_table", "", cvSat);
	}

	private static void insertChannelList(Element elemChanList, SQLiteDatabase db){
		ContentValues cv = new ContentValues();
		String strName = getStringAttr(elemChanList.getAttribute("name"), "");
		int feType = stringToValue(feTypes, getStringAttr(elemChanList.getAttribute("fe_type"), "dvbc"));

		Log.d(TAG, "Inserting channel list "+strName);
		Element elemEntry;
		NodeList entries = elemChanList.getElementsByTagName("channel_entry");
		if (entries != null && entries.getLength() > 0){
			for (int j=0; j<entries.getLength(); j++){
				/* Insert a new record */
				elemEntry = (Element)entries.item(j);

				Log.d(TAG, "Inserting channel entry "+getIntAttr(elemEntry.getAttribute("frequency"), 0));
				cv.clear();
				cv.put("name", strName);
				cv.put("fe_type", feType);
				cv.put("frequency", getIntAttr(elemEntry.getAttribute("frequency"), 0));
				cv.put("symbol_rate", getIntAttr(elemEntry.getAttribute("symbol_rate"), 0));
				cv.put("modulation", stringToValue(mods, getStringAttr(elemEntry.getAttribute("modulation"), "qpsk")));
				cv.put("bandwidth", stringToValue(bandwidths, getStringAttr(elemEntry.getAttribute("bandwidth"), "8")));
				cv.put("ofdm_mode", stringToValue(ofdmModes, getStringAttr(elemEntry.getAttribute("ofdm_mode"), "dvbt")));
				db.insert("region_table", "", cv);
			}
		}
	}

	private static void insertGroup(Document doc, Element elemGroup, SQLiteDatabase db){
		ContentValues cv = new ContentValues();
		String strName = getStringAttr(elemGroup.getAttribute("name"), "");
		String strProgramIds = getStringAttr(elemGroup.getAttribute("programs"), "");

		cv.clear();
		cv.put("name", strName);
		db.insert("grp_table", "", cv);

		int dbGrpId = getNewInsertedRecordID(db, "grp_table");
		
		if (! strProgramIds.isEmpty() && dbGrpId >= 0){
			String[] strIds = strProgramIds.split(" ");
			if (strIds != null){
				Element elem;
				
				for (int i=0; i<strIds.length; i++){
					elem = doc.getElementById(strIds[i]);	
					if (elem == null){
						Log.d(TAG, "** Cannot find program id="+strIds[i]+" in group "+strName+" **");
						continue;
					}
					cv.clear();
					int dbSrvId = getIntAttr(elem.getAttribute("db_id"), -1);
					cv.put("db_srv_id", dbSrvId);
					cv.put("db_grp_id", dbGrpId);
					db.insert("grp_map_table", "", cv);
				}
			}
		}
	}

	private static void insertTP(Element elemTP, SQLiteDatabase db, int dbNetID, int dbSatID){
		ContentValues cv = new ContentValues();

		cv.clear();
		cv.put("src", 0);
		cv.put("db_net_id", dbNetID);
		cv.put("ts_id", 0xffff);
		cv.put("freq", getIntAttr(elemTP.getAttribute("frequency"), 0));
		cv.put("symb", getIntAttr(elemTP.getAttribute("symbol_rate"), 0));
		cv.put("mod", 0);
		cv.put("bw", 0);
		cv.put("snr", 0);
		cv.put("ber", 0);
		cv.put("strength", 0);
		cv.put("db_sat_para_id", dbSatID);
		cv.put("polar", getStringAttr(elemTP.getAttribute("polarisation"), "H").equals("H") ? 1 : 0);
		cv.put("std", 0);
		cv.put("aud_mode", 0);
		cv.put("flags", 0);

		db.insert("ts_table", "", cv);
	}

	private static void insertTS(Element elemTS, SQLiteDatabase db, int dbNetID){
		ContentValues cv = new ContentValues();
		int fe_type = stringToValue(feTypes, elemTS.getAttribute("fe_type"));

		cv.clear();
		cv.put("src", fe_type);
		cv.put("db_net_id", insertNet(db, fe_type, getIntAttr(elemTS.getAttribute("network_id"), 65535)));
		cv.put("ts_id", getIntAttr(elemTS.getAttribute("ts_id"), 65535));
		cv.put("freq", getIntAttr(elemTS.getAttribute("frequency"), 0));
		cv.put("symb", getIntAttr(elemTS.getAttribute("symbol_rate"), 0));
		cv.put("mod", stringToValue(mods, getStringAttr(elemTS.getAttribute("modulation"), "qpsk")));
		cv.put("bw", stringToValue(bandwidths, getStringAttr(elemTS.getAttribute("bandwidth"), "8")));
		cv.put("snr", 0);
		cv.put("ber", 0);
		cv.put("strength", 0);
		cv.put("db_sat_para_id", -1);
		cv.put("polar", -1);
		int vstd = stringToValue(atvVideoStds, getStringAttr(elemTS.getAttribute("video_standard"), "auto"));
		int astd = stringToValue(atvAudioStds, getStringAttr(elemTS.getAttribute("audio_standard"), "auto"));
		cv.put("std", TVChannelParams.getTunerStd(vstd, astd));
		cv.put("aud_mode", TVChannelParams.AUDIO_MONO);
		cv.put("flags", 0);

		db.insert("ts_table", "", cv);
	}

	private static void insertProgram(Element elemProg, SQLiteDatabase db, int fe_type, int dbNetID, int dbTSID, int dbSatID){
		ContentValues cv = new ContentValues();
				
		cv.clear();
		cv.put("src", fe_type);
		cv.put("db_net_id", dbNetID);
		cv.put("db_ts_id", dbTSID);
		cv.put("name", getStringAttr(elemProg.getAttribute("name"), "No Name"));
		cv.put("service_id", getIntAttr(elemProg.getAttribute("service_id"), 65535));
		cv.put("service_type", stringToValue(srvTypes, getStringAttr(elemProg.getAttribute("type"), "other")));
		cv.put("eit_schedule_flag", 0);
		cv.put("eit_pf_flag", 0);
		cv.put("running_status", 1);
		cv.put("free_ca_mode", 1);
		cv.put("volume", 50);
		cv.put("aud_track", 0);
		NodeList video = elemProg.getElementsByTagName("video");
		if (video != null && video.getLength() > 0){
			Element vElem = (Element)video.item(0);
			cv.put("vid_pid", getIntAttr(vElem.getAttribute("pid"), 0x1fff));
			cv.put("vid_fmt", stringToValue(vidFmts, getStringAttr(vElem.getAttribute("format"), "mpeg12")));
		}else{
			cv.put("vid_pid", 0x1fff);
			cv.put("vid_fmt", -1);
		}
		
		cv.put("scrambled_flag", getStringAttr(elemProg.getAttribute("scrambled"), "false").equals("true") ? 1 : 0);
		cv.put("current_aud", -1);

		String apids = "";
		String afmts = "";
		String alangs = "";
		NodeList audios = elemProg.getElementsByTagName("audio");
		if (audios != null && audios.getLength() > 0){
			Element aElem;
			
			for (int i=0; i<audios.getLength(); i++){
				aElem = (Element)audios.item(i);
				if (i != 0){
					apids += " ";
					afmts += " ";
					alangs += " ";
				}
				apids += getStringAttr(aElem.getAttribute("pid"), "0x1fff");
				afmts += stringToValue(audFmts, getStringAttr(aElem.getAttribute("format"), "mpeg"));
				alangs += getStringAttr(aElem.getAttribute("language"), "Audio"+(i+1));				
			}
		}
		cv.put("aud_pids", apids);
		cv.put("aud_fmts", afmts);
		cv.put("aud_langs", alangs);

	 	cv.put("current_sub", -1);
		cv.put("sub_pids", "");
		cv.put("sub_types", "");
		cv.put("sub_composition_page_ids", "");
		cv.put("sub_ancillary_page_ids", "");
		cv.put("sub_langs", "");
		cv.put("current_ttx", -1);
		cv.put("ttx_pids", "");
		cv.put("ttx_types", "");
		cv.put("ttx_magazine_nos", "");
		cv.put("ttx_page_nos", "");
		cv.put("ttx_langs", "");
		
		cv.put("chan_num", getIntAttr(elemProg.getAttribute("channel_number"), 0));
		cv.put("skip", getStringAttr(elemProg.getAttribute("skip"), "false").equals("true") ? 1 : 0);
		cv.put("lock", getStringAttr(elemProg.getAttribute("parental_lock"), "false").equals("true") ? 1 : 0);
		cv.put("favor", 0);
		cv.put("lcn", 0);
		cv.put("sd_lcn", 0);
		cv.put("hd_lcn", 0);
		cv.put("default_chan_num", getIntAttr(elemProg.getAttribute("channel_number"), 0));
		cv.put("chan_order", getIntAttr(elemProg.getAttribute("channel_number"), 0));
		cv.put("lcn_order", getIntAttr(elemProg.getAttribute("channel_number"), 0));
		cv.put("service_id_order", getIntAttr(elemProg.getAttribute("channel_number"), 0));
		cv.put("hd_sd_order", getIntAttr(elemProg.getAttribute("channel_number"), 0));
		cv.put("db_sat_para_id", dbSatID);
		cv.put("major_chan_num", 0);
		cv.put("minor_chan_num", 0);
		cv.put("access_controlled", 0);
		cv.put("hidden", 0);
		cv.put("hide_guide", 0);
		cv.put("source_id", 0);

		db.insert("srv_table", "", cv);

		elemProg.setAttribute("db_id", Integer.toString(getNewInsertedRecordID(db, "srv_table")));
	}

	private static int getNewInsertedRecordID(SQLiteDatabase db, String table){
		int id = -1;
		
		Cursor c = db.rawQuery("select * from "+table+" order by db_id desc limit 1", null);
		if (c != null){
			if (c.moveToFirst()){
				id = getIntValue(c, "db_id", -1);
			}
			c.close();
		}

		return id;
	}

	public static class MyEntityResolver implements EntityResolver{
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException{
			Log.d(TAG, "resolveEntity, systemId is "+systemId);
			if (systemId.equals(DTD_PATH))
				return new InputSource(DTD_PATH);   
			else
				return null;
		}
	}

	private static void xmlToDatabase(SQLiteDatabase db, String xmlPath) throws Exception{
		Log.d(TAG, "Clearing database ...");
		db.execSQL("delete from net_table");
		db.execSQL("delete from ts_table");
		db.execSQL("delete from srv_table");
		db.execSQL("delete from evt_table");
		db.execSQL("delete from booking_table");
		db.execSQL("delete from grp_table");
		db.execSQL("delete from grp_map_table");
		db.execSQL("delete from dimension_table");
		db.execSQL("delete from sat_para_table");
		db.execSQL("delete from region_table");
		

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		factory.setValidating(true);
		factory.setNamespaceAware(true);

		Log.d(TAG, "Set entity resolver");
		builder.setEntityResolver(new MyEntityResolver());
		
		ErrorHandler h = new MyErrorHandler();
		builder.setErrorHandler(h);

		Document dom;
		try{
			InputStream is = new FileInputStream(xmlPath);
			dom = builder.parse(is);
		} catch (Exception e) {
			Log.d(TAG, e.toString());    
			throw new InvalidFileException();
		} 
	
		Element root = dom.getDocumentElement();
		NodeList satellites = root.getElementsByTagName("satellite");
		if (satellites != null && satellites.getLength() > 0){
			Element elemSat, elemTP;
			int dbSatID, dbTSID, dbNetID;
			NodeList tps, progs;
			
			for (int i=0; i<satellites.getLength(); i++){
				elemSat = (Element)satellites.item(i);
				/* Insert a new satellite record */
				insertSatellitePara(elemSat, db);
				dbSatID = getNewInsertedRecordID(db, "sat_para_table");

				tps = elemSat.getElementsByTagName("transponder");
				if (tps != null && tps.getLength() > 0){
					for (int j=0; j<tps.getLength(); j++){
						/* Insert a new transponder record */
						elemTP = (Element)tps.item(j);
						dbNetID = insertNet(db, 0, getIntAttr(elemTP.getAttribute("original_network_id"), 65535));
						insertTP(elemTP, db, dbNetID, dbSatID);
						dbTSID = getNewInsertedRecordID(db, "ts_table");

						progs = elemTP.getElementsByTagName("program");
						if (progs != null && progs.getLength() > 0){
							for (int k=0; k<progs.getLength(); k++){
								/* Insert a new program record */
								insertProgram((Element)progs.item(k), db, 0, dbNetID, dbTSID, dbSatID);
							}
						}
					}
				}
			}
		}

		NodeList chans = root.getElementsByTagName("channel");
		if (chans != null && chans.getLength() > 0){
			NodeList progs;
			int dbTSID, dbNetID;
			Element elemTS;
				
			for (int j=0; j<chans.getLength(); j++){
				/* Insert a new channel record */
				elemTS = (Element)chans.item(j);

				String strFe = getStringAttr(elemTS.getAttribute("fe_type"), "dvbt");
				
				dbNetID = insertNet(db, 
					stringToValue(feTypes, strFe),
					getIntAttr(elemTS.getAttribute("original_network_id"), 65535));
				
				insertTS(elemTS, db, dbNetID);
				dbTSID = getNewInsertedRecordID(db, "ts_table");

				progs = elemTS.getElementsByTagName("program");
				if (progs != null && progs.getLength() > 0){
					for (int k=0; k<progs.getLength(); k++){
						/* Insert a new program record */
						insertProgram((Element)progs.item(k), db, 
							stringToValue(feTypes, strFe),
							dbNetID, dbTSID, -1);
					}
				}
			}
		}

		NodeList chanLists = root.getElementsByTagName("channel_list");
		for (int i=0; i<chanLists.getLength(); i++){
			insertChannelList((Element)chanLists.item(i), db);
		}

		NodeList groups = root.getElementsByTagName("program_group");
		for (int i=0; i<groups.getLength(); i++){
			insertGroup(dom, (Element)groups.item(i), db);
		}
			
	}
	
	public static void transform(int transDirection, SQLiteDatabase db, String xmlPath) throws Exception{
		if (transDirection == XML_TO_DB){
			xmlToDatabase(db, xmlPath);
		}else if (transDirection == DB_TO_XML){
			databaseToXml(db, xmlPath);
		}
	}

}

