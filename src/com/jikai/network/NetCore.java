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
				// ���ﴦ��Wifi���豸״̬
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					Toast.makeText(this.activity, "Wifi�Ѵ��ڿ���״̬", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this.activity, "Wifi�ѱ��رգ��뿪��Wifi������ʹ��jojo", Toast.LENGTH_SHORT).show();
				}
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				// ����peers�б�仯��action
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
							// ������Բ鿴�仯���������Ϣ

							// ͨ�����ݽ�����WifiP2pInfo������ȡ�仯��ĵ�ַ��Ϣ
							InetAddress groupOwnerAddress = info.groupOwnerAddress;
							// ͨ��Э�̣�����һ��С����鳤
							if (info.groupFormed && info.isGroupOwner) {
								// ����ִ��P2PС���鳤������
								// ͨ���Ǵ���һ�������߳��������ͻ��˵�����
							} else if (info.groupFormed) {
								// ����ִ����ͨ��Ա������
								// ͨ���Ǵ���һ���ͻ������鳤�ķ�������������
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
				// ��鸽�����е�Peers�е�Group Owner��ֻ��Owner�Żᱻ
				// ��¼����ʾ��������б���
				new Thread(new Runnable() {
					public void run() {
						groupList.post(new Runnable() {
							public void run() {
								while (devices.hasNext()) {
									WifiP2pDevice device = devices.next();
									if (device.isGroupOwner()) {
										groupOwnerList.add(device);

										// ���ﴦ��ÿ��С�����Ϣ��UI
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
		// discoverPeers���첽ִ�еģ�������֮������̷��أ����Ƿ��ֵĹ���һֱ�ڽ��У�
		// ֱ��������ĳ���豸ʱ�ͻ�֪ͨ��
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
				// ���ӳɹ�
				Toast.makeText(context.getApplicationContext(), "���豸" + device.deviceName + "���ӳɹ�", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onFailure(int arg0) {
				// ����ʧ��
				Toast.makeText(context.getApplicationContext(), "���豸" + device.deviceName + "����ʧ��", Toast.LENGTH_LONG).show();
			}
		});
	}

	// ����һ��P2PС�飬ÿ��С����һ�����������췿�䣬����С���б�
	// ���г���Ҳ��һ����С��(������ÿ������P2P��·�е��豸���г�)
	public void createGroup() {
		this.wifiP2pManager.createGroup(this.channel, new ActionListener() {
			@Override
			public void onSuccess() {
				debug("С�鴴���ɹ�");
			}

			@Override
			public void onFailure(int reason) {
				debug("С�鴴��ʧ��");
			}
		});
	}

	// ���������������������ʾ������Ϣ�õ�
	public void debug(String content) {
		Toast.makeText(this.context, content, Toast.LENGTH_LONG).show();
	}
}
