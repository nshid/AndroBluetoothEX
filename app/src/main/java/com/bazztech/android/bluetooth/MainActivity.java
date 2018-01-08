package com.bazztech.android.bluetooth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.lang.Exception;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import android.app.Activity;
import android.app.ProgressDialog;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

/**
 * Main activity.
 * 
 * @author Nashid Shabazz <nshid@hotmail.com>
 *
 */
public class MainActivity extends Activity {
	private EditText mConnectEt;
	private TextView mStatusTv;
	private Button mActivateBtn;
	private Button mForceActivateBtn;
	private Button mConnectBtn;
	private Button mPairedBtn;
	private Button mScanBtn;

	private ProgressDialog mWaitDlg;
	private ProgressDialog mProgressDlg;
	
	private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
	
	private BluetoothAdapter mBluetoothAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		mStatusTv 			= (TextView) findViewById(R.id.tv_status);
		mConnectEt 			= (EditText) findViewById(R.id.et_connect);
		mActivateBtn 		= (Button) findViewById(R.id.btn_enable);
		mForceActivateBtn 	= (Button) findViewById(R.id.btn_force_enable);
		mPairedBtn 			= (Button) findViewById(R.id.btn_view_paired);
		mScanBtn 			= (Button) findViewById(R.id.btn_scan);
		mConnectBtn 		= (Button) findViewById(R.id.btn_connect);
		
		mBluetoothAdapter	= BluetoothAdapter.getDefaultAdapter();
		
		mWaitDlg 		= new ProgressDialog(this);
		mProgressDlg 		= new ProgressDialog(this);

		mWaitDlg.setMessage("Please wait...");
		mWaitDlg.setCancelable(false);

		mProgressDlg.setMessage("Scanning...");
		mProgressDlg.setCancelable(false);
		mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.dismiss();
		        
		        mBluetoothAdapter.cancelDiscovery();
		    }
		});
		
		if (mBluetoothAdapter == null) {
			showUnsupported();
		} else {
			mPairedBtn.setOnClickListener(new View.OnClickListener() {				
				@Override
				public void onClick(View v) {
					Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
					Set<BluetoothDevice> unpairedDevices = new HashSet<BluetoothDevice>(mDeviceList);
					unpairedDevices.removeAll(pairedDevices);

					if ((unpairedDevices == null || unpairedDevices.size() == 0) && (pairedDevices == null || pairedDevices.size() == 0)) {
						showToast("No Bluetooth Devices Found");
					} else {
						ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();

						list.addAll(unpairedDevices);
						list.addAll(pairedDevices);
						
						Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
						
						intent.putParcelableArrayListExtra("device.list", list);
						
						startActivity(intent);						
					}
				}
			});

			mConnectBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					try {
						if (mConnectBtn.getText() == "Connect" && !mConnectEt.getText().toString().trim().equals("")) {
								BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mConnectEt.getText().toString().toUpperCase().replaceAll("[:-]", "").replaceAll("..(?!$)", "$0:"));
								if (device == null) {
									showToast("Failed to connect device " + device.getAddress());
								}
								mDeviceList.add(device);

								showToast("Added device " + device.getAddress());
								updateControl(mConnectEt, Color.GRAY, false, mConnectEt.getText().toString());
								updateControl(mConnectBtn, Color.CYAN, false, "Disconnect");
						} else {
							try {
								for (int d = 0; d < mDeviceList.size(); d++) {
									BluetoothDevice device = mDeviceList.get(d);
									if (device.getAddress().equals(mConnectEt.getText().toString().toUpperCase().replaceAll("[:-]", "").replaceAll("..(?!$)", "$0:"))) {
										showToast("Removed device " + device.getAddress());

										mDeviceList.remove(device);
										updateControl(mConnectEt, Color.BLACK, true, "");
										updateControl(mConnectBtn, Color.GREEN, true, "Connect");
										break;
									}
								}
							} catch (Exception e) {
								showToast("Unable to disconnect device");
							}
						}
					} catch (Exception e) {
						showToast(e.getMessage());
					}
				}
			});

			mScanBtn.setOnClickListener(new View.OnClickListener() {				
				@Override
				public void onClick(View arg0) {
					mBluetoothAdapter.startDiscovery();
				}
			});
			
			mActivateBtn.setOnClickListener(new View.OnClickListener() {				
				@Override
				public void onClick(View v) {
					if (mBluetoothAdapter.isEnabled()) {
						mBluetoothAdapter.disable();
						
						showDisabled();
					} else {
						Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

					    startActivityForResult(intent, 1000);
					}
				}
			});

			mForceActivateBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!mBluetoothAdapter.isEnabled()) {
						mBluetoothAdapter.enable();
						mWaitDlg.show();
					}
				}
			});

			if (mBluetoothAdapter.isEnabled()) {
				showEnabled();
			} else {
				showDisabled();
			}
		}
		
		IntentFilter filter = new IntentFilter();
		
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		
		registerReceiver(mReceiver, filter);
	}
	
	@Override
	public void onPause() {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
		}
		
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mReceiver);
		
		super.onDestroy();
	}
	
	private void showEnabled() {
		mStatusTv.setText("Bluetooth is On");
		mStatusTv.setTextColor(Color.BLUE);
		mConnectEt.setEnabled(true);
		mConnectEt.setTextColor(Color.BLACK);
		
		mActivateBtn.setText("Disable");		
		mActivateBtn.setEnabled(true);
		mForceActivateBtn.setEnabled(false);
		mForceActivateBtn.setTextColor(Color.LTGRAY);
		
		mPairedBtn.setEnabled(true);
		mScanBtn.setEnabled(true);

		mConnectBtn.setText("Connect");
		mConnectBtn.setEnabled(true);
		mConnectBtn.setBackgroundResource(android.R.drawable.btn_default);
	}
	
	private void showDisabled() {
		mStatusTv.setText("Bluetooth is Off");
		mStatusTv.setTextColor(Color.RED);
		mConnectEt.setEnabled(false);
		mConnectEt.setTextColor(Color.LTGRAY);
		
		mActivateBtn.setText("Enable");
		mActivateBtn.setEnabled(true);
		mForceActivateBtn.setEnabled(true);
		mForceActivateBtn.setTextColor(Color.rgb(255, 140, 0));

		mPairedBtn.setEnabled(false);
		mScanBtn.setEnabled(false);

		mConnectBtn.setText("Connect");
		mConnectBtn.setEnabled(false);
		mConnectBtn.setBackgroundColor(Color.LTGRAY);
	}
	
	private void showUnsupported() {
		mStatusTv.setText("Bluetooth is unsupported by this device");
		mConnectEt.setEnabled(false);
		
		mActivateBtn.setText("Enable");
		mActivateBtn.setEnabled(false);
		mForceActivateBtn.setEnabled(false);
		mForceActivateBtn.setTextColor(Color.LTGRAY);
		
		mPairedBtn.setEnabled(false);
		mScanBtn.setEnabled(false);

		mConnectBtn.setText("Connect");
		mConnectBtn.setEnabled(false);
		mConnectBtn.setBackgroundColor(Color.LTGRAY);
	}

	private void updateControl(Object obj, int color, boolean isEnabled, String text) {
		if (obj instanceof Button) {
			Button btn = (Button) obj;
			btn.setTextColor(color);
			btn.setText(text);
		} else if (obj instanceof EditText) {
			EditText et = (EditText) obj;
			Spannable word2Span = new SpannableString(text);
			word2Span.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			et.setText(word2Span);
			et.setEnabled(isEnabled);
		}
	}

	private void showToast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {	    	
	        String action = intent.getAction();
	        
	        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
	        	final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
	        	 
	        	if (state == BluetoothAdapter.STATE_ON) {
	        		showToast("Enabled");

					if (mWaitDlg.isShowing()) {
						mWaitDlg.dismiss();
					}

	        		showEnabled();
	        	 }
	        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
	        	mDeviceList = new ArrayList<BluetoothDevice>();

				if (mConnectBtn.getText() == "Disconnect" && !mConnectEt.getText().toString().trim().equals("")) {
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mConnectEt.getText().toString().toUpperCase().replaceAll("[:-]", "").replaceAll("..(?!$)", "$0:"));
					if (device != null) {
						mDeviceList.add(device);
						showToast("Found device " + device.getAddress());
					}
				}
				
				mProgressDlg.show();
	        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	        	mProgressDlg.dismiss();
	        	
	        	Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
	        	
	        	newIntent.putParcelableArrayListExtra("device.list", mDeviceList);
				
				startActivity(newIntent);
	        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	        	BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

	        	if (device != null && !mDeviceList.contains(device)) {
					mDeviceList.add(device);

					showToast("Found device " + (device.getName() == null ? device.getAddress() : device.getName()));
				}
	        }
	    }
	};

}