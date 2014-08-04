package com.aprilbrother.blueduino;

import java.util.ArrayList;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aprilbrother.blueduino.bean.PinInfo;
import com.aprilbrother.blueduino.contants.Contants;
import com.aprilbrother.blueduino.globalvariables.GlobalVariables;

@SuppressLint("NewApi")
public class OperateActivity extends Activity {

	private static final int UART_PROFILE_DISCONNECTED = 21;

	private UartService mService = null;
	private int mState = UART_PROFILE_DISCONNECTED;
	private static final int UART_PROFILE_CONNECTED = 20;

	private ProgressDialog progressDialog;
	private BluetoothDevice device;
	private MyAdapter adapter;

	private ArrayList<PinInfo> pinsInfo = new ArrayList<PinInfo>();

	byte[] lastValue;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			adapter.notifyDataSetChanged();
			super.handleMessage(msg);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_operate);
		super.onCreate(savedInstanceState);

		init();

		service_init();

		setViewData();

	}

	private void setViewData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// 等待成功绑定服务
					// waiting for connect service
					Thread.sleep(500);

					// connect device
					connectService(device);

					// 等待建立连接成功
					// waiting for device connect
					Thread.sleep(3000);

					// 获取引脚个数
					// get totol pin count
					ABProtocol.queryTotalPinCount(mService);

					// 等待写入C返回值
					// waiting for the return when wirte C
					Thread.sleep(500);

					// 获取引脚信息
					// get all pins info
					ABProtocol.queryPinAll(mService);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void init() {
		Bundle bundle = getIntent().getExtras();
		device = bundle.getParcelable("device");
		ListView lv = (ListView) findViewById(R.id.lv_blueduino_infos);
		adapter = new MyAdapter();
		lv.setAdapter(adapter);
		setItemClick(lv);
		progressDialog = ProgressDialog.show(OperateActivity.this,
				"Loading...", "Please wait...", true, false);
		progressDialog.setCancelable(true);

	}

	private void setItemClick(ListView lv) {
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				setChangeModle(position, view);
			}

			private void setChangeModle(final int position, View view) {
				// change the mode
				int capability = GlobalVariables.pinInfos.get(position)
						.getCapability();

				switch (capability) {
				case 0:
					// nothing
					String[] strings = new String[] {};
					showListDialog(strings, position);
					break;
				case 1:
					// PIN_CAPABILITY_DIGITAL
					String[] digital = new String[] { "Input", "Output" };
					showListDialog(digital, position);
					break;
				case 2:
					// PIN_CAPABILITY_ANALOG
					String[] analog = new String[] { "Analog" };
					showListDialog(analog, position);
					break;
				case 3:
					// PIN_CAPABILITY_DIGITAL+PIN_CAPABILITY_ANALOG
					String[] digital_analog = new String[] { "Input", "Output",
							"Analog" };
					showListDialog(digital_analog, position);
					break;
				case 4:
					// PIN_CAPABILITY_PWM
					String[] pwm = new String[] { "PWM" };
					showListDialog(pwm, position);
					break;
				case 5:
					// PIN_CAPABILITY_DIGITAL+PIN_CAPABILITY_PWM
					String[] digital_pwm = new String[] { "Input", "Output",
							"PWM" };
					showListDialog(digital_pwm, position);
					break;
				case 6:
					// PIN_CAPABILITY_ANALOG+PIN_CAPABILITY_PWM
					String[] analog_pwm = new String[] { "Analog", "PWM" };
					showListDialog(analog_pwm, position);
					break;
				case 7:
					// PIN_CAPABILITY_DIGITAL+PIN_CAPABILITY_ANALOG+PIN_CAPABILITY_PWM
					String[] digital_analog_pin = new String[] { "Input",
							"Output", "Analog", "PWM" };
					showListDialog(digital_analog_pin, position);

					break;
				default:
					break;
				}
			}

			private void showListDialog(final String[] strings,
					final int positon) {
				Builder dialog = new AlertDialog.Builder(OperateActivity.this)
						.setTitle("Modle").setItems(strings,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {

										// set the mode

										byte pin = (byte) (positon & 0xFF);

										if (strings[which].equals("Input")) {
											ABProtocol.setPinMode(mService,
													pin,
													Contants.PIN_MODE_INPUT);
										} else if (strings[which]
												.equals("Output")) {
											ABProtocol.setPinMode(mService,
													pin,
													Contants.PIN_MODE_OUTPUT);
										} else if (strings[which]
												.equals("Analog")) {
											ABProtocol.setPinMode(mService,
													pin,
													Contants.PIN_MODE_ANALOG);
										} else if (strings[which].equals("PWM")) {
											ABProtocol.setPinMode(mService,
													pin, Contants.PIN_MODE_PWM);
										}
									}
								});
				dialog.show();
			}

		});
	}

	/**
	 * 连接服务
	 * connect the service to operate bluetooth
	 * @param device 连接服务的蓝牙设备
	 */
	private void connectService(BluetoothDevice device) {
		mService.connect(device.getAddress());
	}

	private void service_init() {
		Intent bindIntent = new Intent(this, UartService.class);
		bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		LocalBroadcastManager.getInstance(this).registerReceiver(
				UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
	}

	// UART service connected/disconnected
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder rawBinder) {
			mService = ((UartService.LocalBinder) rawBinder).getService();
			if (!mService.initialize()) {
				finish();
			}
		}

		public void onServiceDisconnected(ComponentName classname) {
			// // mService.disconnect(mDevice);
			mService = null;
		}
	};

	private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// *********************//
			if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
				runOnUiThread(new Runnable() {
					public void run() {
						mState = UART_PROFILE_CONNECTED;
					}
				});
			}

			// *********************//
			if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
				runOnUiThread(new Runnable() {
					public void run() {
						mState = UART_PROFILE_DISCONNECTED;
						mService.close();
						// setUiState();
					}
				});
			}

			// *********************//
			if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
				mService.enableTXNotification();
			}
			// *********************//
			if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
				
				final byte[] txValue = intent
						.getByteArrayExtra(UartService.EXTRA_DATA);
				try {
					// 返回的数据通上一个一样则不解析
					// if the value == last value don't parar
					if (lastValue != null) {
						if (!Arrays.equals(lastValue, txValue)) {
							lastValue = txValue;
							ABProtocol.parseData(txValue);
							if (txValue.length != 2 && txValue.length != 20) {
								pinsInfo = GlobalVariables.pinInfos;
								handler.sendEmptyMessage(0);
								progressDialog.dismiss();
							}
						}
					} else {
						lastValue = txValue;
						ABProtocol.parseData(txValue);
						if (txValue.length != 2 && txValue.length != 20) {
							pinsInfo = GlobalVariables.pinInfos;
							handler.sendEmptyMessage(0);
							progressDialog.dismiss();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// *********************//
			if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
				showMessage("Device doesn't support UART. Disconnecting");
				mService.disconnect();
			}
		}
	};

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
		return intentFilter;
	}

	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return GlobalVariables.pinSize;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View view = setView(position, convertView);
			return view;
		}

		private View setView(int position, View convertView) {
			View view;
			ViewHolder holder;
			if (convertView == null) {
				view = View.inflate(OperateActivity.this,
						R.layout.pin_info_list_item, null);
				holder = new ViewHolder();
				holder.tv_operate_info_pin = (TextView) view
						.findViewById(R.id.tv_operate_info_pin);
				holder.tv_operate_info_modle = (TextView) view
						.findViewById(R.id.tv_operate_info_modle);
				holder.iv_operate_info_input_output = (ImageView) view
						.findViewById(R.id.iv_operate_info_input_output);
				holder.tv_operate_info_analog = (TextView) view
						.findViewById(R.id.tv_operate_info_analog);
				holder.seekBar_operate_info_pmw = (SeekBar) view
						.findViewById(R.id.seekBar_operate_info_pmw);
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
			holder.tv_operate_info_pin
					.setText(Contants.pinSerial[GlobalVariables.pinInfos.get(
							position).getPin()]
							+ "");
			setModle(position, holder);
			setChangeModle(position, holder);
			setChangeValue(position, holder);
			return view;
		}

		/**
		 * 输出值
		 * Output
		 */
		private void setChangeValue(final int position, ViewHolder holder) {
			switch (GlobalVariables.pinInfos.get(position).getMode()) {
			case Contants.PIN_MODE_OUTPUT:// 输出 对外输出高低电频
				// write out PIN_STATE_HIGH or PIN_STATE_LOW
				holder.iv_operate_info_input_output
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								// 判断当前是高电频还是低电频 然后写入与当前值相反的值
								// when the value is Contants.PIN_STATE_HIGH
								// write Contants.PIN_STATE_LOW
								// when the value is Contants.PIN_STATE_LOW
								// write Contants.PIN_STATE_HIGH
								switch (GlobalVariables.pinInfos.get(position)
										.getValue()) {
								case Contants.PIN_STATE_HIGH:
									ABProtocol.digitalWrite(mService, position,
											Contants.PIN_STATE_LOW);
									GlobalVariables.pinInfos.get(position)
											.setValue(Contants.PIN_STATE_LOW);
									adapter.notifyDataSetChanged();
									break;
								case Contants.PIN_STATE_LOW:
									ABProtocol.digitalWrite(mService, position,
											Contants.PIN_STATE_HIGH);
									GlobalVariables.pinInfos.get(position)
											.setValue(Contants.PIN_STATE_HIGH);
									adapter.notifyDataSetChanged();
									break;
								default:
									break;
								}
							}
						});
				break;

			case Contants.PIN_MODE_PWM:// 输出 对外输出方波值
				// write out pwm
				holder.seekBar_operate_info_pmw.setMax(255);
				holder.seekBar_operate_info_pmw
						.setOnSeekBarChangeListener(new SeekBarListener(
								position));
				break;

			default:
				break;
			}
		}

		private class SeekBarListener implements
				SeekBar.OnSeekBarChangeListener {
			private int position;

			public SeekBarListener(int position) {
				this.position = position;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				ABProtocol.setPinPWM(mService, position, progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

		}

		private void setModle(int position, ViewHolder holder) {
			switch (GlobalVariables.pinInfos.get(position).getMode()) {
			case Contants.PIN_MODE_INPUT:
				holder.tv_operate_info_modle.setText("Input");

				holder.iv_operate_info_input_output.setVisibility(View.VISIBLE);

				if (GlobalVariables.pinInfos.get(position).getValue() == Contants.PIN_STATE_HIGH) {// 高电频
					holder.iv_operate_info_input_output
							.setBackground(getResources().getDrawable(
									R.drawable.but));

				} else {// 低电频
					holder.iv_operate_info_input_output
							.setBackground(getResources().getDrawable(
									R.drawable.but_close));
				}
				holder.iv_operate_info_input_output.setClickable(false);
				holder.tv_operate_info_analog.setVisibility(View.GONE);
				holder.seekBar_operate_info_pmw.setVisibility(View.GONE);
				break;
			case Contants.PIN_MODE_OUTPUT:
				holder.tv_operate_info_modle.setText("Output");

				holder.iv_operate_info_input_output.setVisibility(View.VISIBLE);
				if (GlobalVariables.pinInfos.get(position).getValue() == Contants.PIN_STATE_HIGH) {// 高电频
					holder.iv_operate_info_input_output
							.setBackground(getResources().getDrawable(
									R.drawable.but));
				} else {// 低电频
					holder.iv_operate_info_input_output
							.setBackground(getResources().getDrawable(
									R.drawable.but_close));
				}
				holder.iv_operate_info_input_output.setClickable(true);
				holder.tv_operate_info_analog.setVisibility(View.GONE);
				holder.seekBar_operate_info_pmw.setVisibility(View.GONE);
				break;
			case Contants.PIN_MODE_ANALOG:
				holder.tv_operate_info_modle.setText("Analog");

				holder.iv_operate_info_input_output.setVisibility(View.GONE);
				holder.tv_operate_info_analog.setVisibility(View.VISIBLE);
				holder.tv_operate_info_analog.setText(GlobalVariables.pinInfos
						.get(position).getValue() + "");
				holder.seekBar_operate_info_pmw.setVisibility(View.GONE);
				break;
			case Contants.PIN_MODE_PWM:
				holder.tv_operate_info_modle.setText("PWM");

				holder.iv_operate_info_input_output.setVisibility(View.GONE);
				holder.tv_operate_info_analog.setVisibility(View.GONE);
				holder.seekBar_operate_info_pmw.setVisibility(View.VISIBLE);
				break;
			default:
				break;
			}
		}

		private void setChangeModle(final int position, final ViewHolder holder) {
			holder.tv_operate_info_modle
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							// change the mode
							int capability = GlobalVariables.pinInfos.get(
									position).getCapability();

							switch (capability) {
							case 0:
								// nothing
								String[] strings = new String[] {};
								showListDialog(strings, position);
								break;
							case 1:
								// PIN_CAPABILITY_DIGITAL
								String[] digital = new String[] { "Input",
										"Output" };
								showListDialog(digital, position);
								break;
							case 2:
								// PIN_CAPABILITY_ANALOG
								String[] analog = new String[] { "Analog" };
								showListDialog(analog, position);
								break;
							case 3:
								// PIN_CAPABILITY_DIGITAL+PIN_CAPABILITY_ANALOG
								String[] digital_analog = new String[] {
										"Input", "Output", "Analog" };
								showListDialog(digital_analog, position);
								break;
							case 4:
								// PIN_CAPABILITY_PWM
								String[] pwm = new String[] { "PWM" };
								showListDialog(pwm, position);
								break;
							case 5:
								// PIN_CAPABILITY_DIGITAL+PIN_CAPABILITY_PWM
								String[] digital_pwm = new String[] { "Input",
										"Output", "PWM" };
								showListDialog(digital_pwm, position);
								break;
							case 6:
								// PIN_CAPABILITY_ANALOG+PIN_CAPABILITY_PWM
								String[] analog_pwm = new String[] { "Analog",
										"PWM" };
								showListDialog(analog_pwm, position);
								break;
							case 7:
								// PIN_CAPABILITY_DIGITAL+PIN_CAPABILITY_ANALOG+PIN_CAPABILITY_PWM
								String[] digital_analog_pin = new String[] {
										"Input", "Output", "Analog", "PWM" };
								showListDialog(digital_analog_pin, position);

								break;
							default:
								break;
							}
						}

						private void showListDialog(final String[] strings,
								final int positon) {
							Builder dialog = new AlertDialog.Builder(
									OperateActivity.this)
									.setTitle("Modle")
									.setItems(
											strings,
											new DialogInterface.OnClickListener() {

												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {

													// set the mode

													byte pin = (byte) (positon & 0xFF);

													if (strings[which]
															.equals("Input")) {
														ABProtocol
																.setPinMode(
																		mService,
																		pin,
																		Contants.PIN_MODE_INPUT);
														holder.iv_operate_info_input_output.setClickable(false);	
													} else if (strings[which]
															.equals("Output")) {
														ABProtocol
																.setPinMode(
																		mService,
																		pin,
																		Contants.PIN_MODE_OUTPUT);
														holder.iv_operate_info_input_output.setClickable(true);
													} else if (strings[which]
															.equals("Analog")) {
														ABProtocol
																.setPinMode(
																		mService,
																		pin,
																		Contants.PIN_MODE_ANALOG);
													} else if (strings[which]
															.equals("PWM")) {
														ABProtocol
																.setPinMode(
																		mService,
																		pin,
																		Contants.PIN_MODE_PWM);
													}
												}
											});
							dialog.show();
						}
					});
		}
	}

	class ViewHolder {
		TextView tv_operate_info_pin;
		TextView tv_operate_info_modle;
		ImageView iv_operate_info_input_output;
		TextView tv_operate_info_analog;
		SeekBar seekBar_operate_info_pmw;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		GlobalVariables.pinSize = 0;
		pinsInfo.clear();
		GlobalVariables.pinInfos.clear();
		try {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					UARTStatusChangeReceiver);
			unbindService(mServiceConnection);
			mService.stopSelf();
			mService = null;
		} catch (Exception ignore) {
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
}
