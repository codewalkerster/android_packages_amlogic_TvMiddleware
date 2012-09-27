package com.amlogic.tvutil;

import com.amlogic.tvutil.TVMessage;

oneway interface ITVCallback{
	void onMessage(in TVMessage msg);
}

