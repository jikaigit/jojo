package com.jikai.network;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NetCore {
	private Context context;
	private WifiP2pManager wifiP2pManager;
	private IntentFilter intentFilter;
	private Channel channel;
	private NetchatBdcastReceiver broadcastReceiver;
	private ArrayList<WifiP2pDevice> groupOwnerList;
	private PeerListListener peersListListener;

	/*
	 * This class is used to wrap the BroadcastReceiver to receive the intent
	 * broadcast.
	 */
	class NetchatBdcastReceiver extends BroadcastReceiver {
		private WifiP2pManager wifiP2pManager;
		private Channel channel;
		private Activity activity;

		public NetchatBdcastReceiver(WifiP2pManager wifiP2pManager, Channel channel, Activity activity) {
			this.wifiP2pManager = wifiP2pManager;
			this.channel = channel;
			this.activity = activity;
		}

		// In this onReceive we will process different intents.
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				// 这里处理Wifi的设备状态
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					Toast.makeText(this.activity, "Wifi已处于开启状态", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this.activity, "Wifi已被关闭，请开启Wifi以正常使用jojo", Toast.LENGTH_SHORT).show();
				}
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				// 处理peers列表变化的action
				if (wifiP2pManager != null) {
					wifiP2pManager.requestPeers(channel, peersListListener);
				}
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				// Process the situations when the connections changes.
				if (wifiP2pManager == null)
					return;

				NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
				if (networkInfo.isConnected()) {
					// We are connected with the other device, request
					// connection info to find group owner IP
					wifiP2pManager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
						@Override
						public void onConnectionInfoAvailable(WifiP2pInfo info) {
							// 这里可以查看变化后的网络信息

							// 通过传递进来的WifiP2pInfo参数获取变化后的地址信息
							InetAddress groupOwnerAddress = info.groupOwnerAddress;
							// 通过协商，决定一个小组的组长
							if (info.groupFormed && info.isGroupOwner) {
								// 这里执行P2P小组组长的任务。
								// 通常是创建一个服务线程来监听客户端的请求
							} else if (info.groupFormed) {
								// 这里执行普通组员的任务
								// 通常是创建一个客户端向组长的服务器发送请求
							}
						}
					});
				}
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
			}
		}
	}

	public NetCore(Context context) {
		this.context = context;
		this.wifiP2pManager = (WifiP2pManager) this.context.getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);

		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		this.channel = this.wifiP2pManager.initialize(this.context.getApplicationContext(), this.context.getMainLooper(), null);
		this.broadcastReceiver = new NetchatBdcastReceiver(this.wifiP2pManager, this.channel, (Activity) this.context);

		this.context.registerReceiver(this.broadcastReceiver, this.intentFilter);
		this.groupOwnerList = new ArrayList<WifiP2pDevice>();
	}

	public void discoverPeers(final LinearLayout groupList) {
		this.peersListListener = new WifiP2pManager.PeerListListener() {
			@Override
			public void onPeersAvailable(WifiP2pDeviceList peerList) {
				groupOwnerList.clear();
				final Iterator<WifiP2pDevice> devices = peerList.getDeviceList().iterator();
				// 检查附近所有的Peers中的Group Owner，只有Owner才会被
				// 记录并显示在组界面列表中
				new Thread(new Runnable() {
					public void run() {
						groupList.post(new Runnable() {
							public void run() {
								while (devices.hasNext()) {
									WifiP2pDevice device = devices.next();
									if (device.isGroupOwner()) {
										groupOwnerList.add(device);

										// 这里处理每个小组的信息的UI
										TextView textView = new TextView(context);
										textView.setText(device.deviceName);
										textView.append("\r\n");
										textView.append(device.deviceAddress);
										groupList.addView(textView);
									}
								}
							}
						});
					}
				}).start();
			}
		};

		this.wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
			}

			@Override
			public void onFailure(int arg0) {
			}
		});
		// discoverPeers是异步执行的，调用了之后会立刻返回，但是发现的过程一直在进行，
		// 直到发现了某个设备时就会通知你
	}

	// Connect to the peers.
	public void connectPeer() {
		final WifiP2pDevice device = new WifiP2pDevice();
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;

		wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				// 连接成功
				Toast.makeText(context.getApplicationContext(), "与设备" + device.deviceName + "连接成功", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onFailure(int arg0) {
				// 连接失败
				Toast.makeText(context.getApplicationContext(), "与设备" + device.deviceName + "连接失败", Toast.LENGTH_LONG).show();
			}
		});
	}

	// 创建一个P2P小组，每个小组是一个独立的聊天房间，而且小组列表
	// 里列出的也是一个个小组(并不是每个附近P2P网路中的设备被列出)
	public void createGroup() {
		this.wifiP2pManager.createGroup(this.channel, new ActionListener() {
			@Override
			public void onSuccess() {
				debug("小组创建成功");
			}

			@Override
			public void onFailure(int reason) {
				debug("小组创建失败");
			}
		});
	}

	// 这个类用来做调试向我显示运行信息用的
	public void debug(String content) {
		Toast.makeText(this.context, content, Toast.LENGTH_LONG).show();
	}
}
