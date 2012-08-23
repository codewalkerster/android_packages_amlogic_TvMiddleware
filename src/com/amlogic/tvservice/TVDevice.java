package com.amlogic.tvservice;

import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVStatus;

/**
 *TV 底层播放前端设置功能访问接口
 */
public class TVDevice{
	public static final int MODE_DTV = 1;
	public static final int MODE_ATV = 2;

	public void setInputSource(int src){
	}

	public void setFrontend(TVChannelParams params){
	}

	public void playDTV(){
	}

	public void stopDTV(){
	}

	public void playATV(){
	}

	public void stopATV(){
	}
}

