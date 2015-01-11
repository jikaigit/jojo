package com.jikai.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SlidingPaneLayout.LayoutParams;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

class NetConstants {
	public static int ServerPort = 15280;
}

public class NetCore {
	private Context context;
	private WifiP2pManager wifiP2pManager;
	private IntentFilter intentFilter;
	private Channel channel;
	private NetchatBdcastReceiver broadcastReceiver;
	private HashMap<String, WifiP2pDevice> groupOwnerTable;
	private PeerListListener peersListListener;
	private Handler asyncIsOkHandler;

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
					wifiP2pManager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
						@Override
						public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
							// ������Բ鿴�仯���������Ϣ
							// ͨ�����ݽ�����WifiP2pInfo������ȡ�仯��ĵ�ַ��Ϣ
							final WifiP2pInfo info = wifiP2pInfo;

							if (info.groupFormed && info.isGroupOwner) {
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											ServerSocket listener = new ServerSocket(NetConstants.ServerPort, 20);
											while (true) {
												// Ϊÿ��Peer���Ӵ���һ���������߳���ά�ֲ�����
												// �������
												Socket acceptedSocket = listener.accept();
												final Socket clientPeer = acceptedSocket;
												new Thread(new Runnable() {
													@Override
													public void run() {
														byte[] buffer = new byte[512];
														try {
															InputStream peerInput = clientPeer.getInputStream();
															while (true) {
																int readlen = peerInput.read(buffer);
																// �����������ʾPeer���͵�����
															}
														} catch (IOException e) {

														}
													}
												});
											}
										} catch (IOException e) {
											debug("�鳤��������ʧ��");
										} catch (Exception e) {
											debug("δ֪���Ӵ���");
										}
									}
								}).start();
							} else if (info.groupFormed) {
								// ����ִ����ͨ��Ա������
								// ͨ���Ǵ���һ���ͻ������鳤�ķ�������������
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											InetAddress addr = info.groupOwnerAddress;
											Socket socket = new Socket(addr, NetConstants.ServerPort);
											debug("�ͻ��������ˣ�" + addr.getHostAddress());
											socket.close();
										} catch (IOException e) {
											debug("�ͻ������Ӵ���");
										} catch (Exception e) {
											debug("�ͻ�������δ֪����");
										}
									}
								}).start();
							}
						}
					});
				}
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
			}
		}
	}

	public NetCore(Context context, Handler asyncIsOkHandler) {
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
		this.groupOwnerTable = new HashMap<String, WifiP2pDevice>();
		this.asyncIsOkHandler = asyncIsOkHandler;
	}

	public void discoverPeers(final RelativeLayout groupList) {
		this.peersListListener = new WifiP2pManager.PeerListListener() {
			@Override
			public void onPeersAvailable(WifiP2pDeviceList peerList) {
				groupList.removeAllViews();
				groupOwnerTable.clear();
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
										groupOwnerTable.put(device.deviceAddress, device);

										// ���ﴦ��ÿ��С�����Ϣ��UI
										final TextView groupCard = new TextView(context);
										groupCard.setPadding(30, 30, 30, 30);
										groupCard.setTextColor(Color.rgb(250, 250, 250));
										groupCard.setTextSize(20);
										groupCard.setBackgroundColor(Color.rgb(11, 33, 250));
										groupCard.setText(device.deviceAddress);
										groupCard.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
										groupCard.setOnClickListener(new OnClickListener() {
											@Override
											public void onClick(View v) {
												connectPeer(groupCard.getText().toString());
											}
										});
										groupList.addView(groupCard);
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

	public void connectPeer(String peerAddress) {
		WifiP2pDevice device = groupOwnerTable.get(peerAddress);
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;

		wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Message message = new Message();
				message.what = 31;
				asyncIsOkHandler.sendMessage(message);
			}

			@Override
			public void onFailure(int arg0) {
				Message message = new Message();
				message.what = 32;
				asyncIsOkHandler.sendMessage(message);
			}
		});
	}

	// ����һ��P2PС�飬ÿ��С����һ�����������췿�䣬����С���б�
	// ���г���Ҳ��һ����С��(������ÿ������P2P��·�е��豸���г�)
	public void createGroup() {
		this.wifiP2pManager.createGroup(this.channel, new ActionListener() {
			@Override
			public void onSuccess() {
				Message message = new Message();
				message.what = 11;
				asyncIsOkHandler.sendMessage(message);
			}

			@Override
			public void onFailure(int reason) {
				Message message = new Message();
				message.what = 12;
				asyncIsOkHandler.sendMessage(message);
			}
		});
	}

	// ��ɢ����С��
	public void dissolveGroup(final Handler notifySuccessOrFailed) {
		this.wifiP2pManager.removeGroup(this.channel, new ActionListener() {
			@Override
			public void onSuccess() {
				Message message = new Message();
				message.what = 21;
				notifySuccessOrFailed.sendMessage(message);
			}

			@Override
			public void onFailure(int reason) {
				Message message = new Message();
				message.what = 22;
				notifySuccessOrFailed.sendMessage(message);
			}
		});
	}

	// ���������������������ʾ������Ϣ�õ�
	public void debug(String content) {
		Toast.makeText(this.context, content, Toast.LENGTH_SHORT).show();
	}
}
