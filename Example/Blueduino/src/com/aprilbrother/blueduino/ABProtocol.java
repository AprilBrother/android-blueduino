package com.aprilbrother.blueduino;

import java.io.UnsupportedEncodingException;

import android.util.Log;

import com.aprilbrother.blueduino.UartService;
import com.aprilbrother.blueduino.bean.PinInfo;
import com.aprilbrother.blueduino.contants.Contants;
import com.aprilbrother.blueduino.globalvariables.GlobalVariables;
import com.aprilbrother.blueduino.utils.ByteUtil;

public class ABProtocol {

	public static byte[] mValues = new byte[0];
	private static PinInfo pinInfo = new PinInfo();

	/**
	 * 查询引脚个数 query total pin count
	 */
	public static void queryTotalPinCount(UartService mService) {
		byte[] value;
		try {
			value = "C".getBytes("UTF-8");
			mService.writeRXCharacteristic(value);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 查询所有引脚信息 query all pins info
	 */
	public static void queryPinAll(UartService mService) {

		byte[] value;
		try {
			value = "A".getBytes("UTF-8");
			mService.writeRXCharacteristic(value);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 设置引脚模式 set pin mode
	 */
	public static void setPinMode(UartService mService, int pin, int mode) {
		byte[] value = { 'S', (byte) (pin & 0xff), (byte) (mode & 0xff) };
		mService.writeRXCharacteristic(value);
	}

	/**
	 * 数字输出 digital write
	 */
	public static void digitalWrite(UartService mService, int pin, int value) {
		byte[] mValue = { 'T', (byte) (pin & 0xff), (byte) (value & 0xff) };
		mService.writeRXCharacteristic(mValue);
	}

	/**
	 * 设置方波输出 set pin PWM
	 */
	public static void setPinPWM(UartService mService, int pin, int value) {
		byte[] pwmValue = { 'N', (byte) (pin & 0xff), (byte) (value & 0xff) };
		mService.writeRXCharacteristic(pwmValue);
	}

	/**
	 * 解析数据 parse data
	 */
	public static void parseData(byte[] value) {
		if (value.length < 512) {
			mValues = ByteUtil.byteMerger(mValues, value);
		}
		if (value.length != 20) {
			int i = 0;
			while (i < mValues.length) {
				byte b = mValues[i++];
				int tag = b & 0xff;
				switch (tag) {
				case 'C':
					if (i < mValues.length) {
						int totalCount = mValues[i++];
						GlobalVariables.pinSize = totalCount;
					}
					break;
				case 'P':
					if (i < mValues.length - 2) {
						int pin = mValues[i++];
						int capability = mValues[i++];
						if (GlobalVariables.pinInfos.size() > 0) {
							pinInfo = new PinInfo();
							pinInfo.setPin(pin);
							pinInfo.setCapability(capability);
							if (GlobalVariables.pinInfos.size()
									% GlobalVariables.pinSize == 0) {
								GlobalVariables.pinInfos.clear();
							}
//							GlobalVariables.pinInfos.add(pin, pinInfo);
							GlobalVariables.pinInfos.add(pinInfo);
						} else {
							pinInfo = new PinInfo();
							pinInfo.setPin(pin);
							pinInfo.setCapability(capability);
//							GlobalVariables.pinInfos.add(pin, pinInfo);
							GlobalVariables.pinInfos.add(pinInfo);
						}
					}
					break;
				case 'G':
					if (i < mValues.length - 2) {
						int pin = mValues[i++];
						int mode = mValues[i++];
						int receiveValue = mValues[i++];
						int _mode = mode & 0x0F;
						
						Log.i("Test", "pin = "+pin);
						Log.i("Test", "mode = "+mode);
						
						if ((mode == Contants.PIN_MODE_INPUT)
								|| (mode == Contants.PIN_MODE_OUTPUT)) {

							for(PinInfo info : GlobalVariables.pinInfos){
								if(info.getPin() == pin){
									info.setMode(mode);
									info.setValue(receiveValue);
								}
							}
							
//							GlobalVariables.pinInfos.get(pin).setMode(mode);
//							GlobalVariables.pinInfos.get(pin).setValue(
//									receiveValue);

						} else if (_mode == Contants.PIN_MODE_ANALOG) {
							int mValue = 0;
							if(receiveValue>0){
								mValue = receiveValue;
							}else if(receiveValue<0){
								mValue = 255 + receiveValue;
							}
							
							Log.i("Test", "pin = "+pin);
							Log.i("Test", "mode = "+mode);
							
							for(PinInfo info : GlobalVariables.pinInfos){
								if(info.getPin() == pin){
									info.setMode(mode);
									info.setValue(mValue);
								}
							}
//							GlobalVariables.pinInfos.get(pin).setMode(_mode);
//							GlobalVariables.pinInfos.get(pin).setValue(
//									mValue);
							
						} else if (mode == Contants.PIN_MODE_PWM) {
							Log.i("Test", "pin = "+pin);
							Log.i("Test", "mode = "+mode);
							for(PinInfo info : GlobalVariables.pinInfos){
								if(info.getPin() == pin){
									info.setMode(mode);
									info.setValue(receiveValue);
								}
							}
							
							
//							GlobalVariables.pinInfos.get(pin).setMode(mode);
//							GlobalVariables.pinInfos.get(pin).setValue(
//									receiveValue);
						}
					}
					break;
				default:
					break;
				}
			}
		}
	}
}
