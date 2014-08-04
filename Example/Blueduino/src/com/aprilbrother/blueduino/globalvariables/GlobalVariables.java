package com.aprilbrother.blueduino.globalvariables;

import java.util.ArrayList;

import com.aprilbrother.blueduino.bean.PinInfo;

public class GlobalVariables {
	
	//pin count
	public static int pinSize;
	
	//pins Info
	public static ArrayList<PinInfo> pinInfos = new ArrayList<PinInfo>();
	
	public static boolean isQueryPinAll = false;

}
