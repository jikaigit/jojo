package com.jikai.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.SocketHandler;

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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jikai.jojo.ColorCardSelector;
import com.jikai.jojo.R;

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
	private boolean isInGroup;
	private ServerSocket serverSocket;

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
					wifiP2pManager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
						@Override
						public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
							// 这里可以查看变化后的网络信息
							// 通过传递进来的WifiP2pInfo参数获取变化后的地址信息
							final WifiP2pInfo info = wifiP2pInfo;

							if (info.groupFormed && info.isGroupOwner) {
								if (!isInGroup)
									return;
								// 创建监听端口开始接受其他成员的连接
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											serverSocket = new ServerSocket(NetConstants.ServerPort);
											while (true) {
												Socket peerClient = serverSocket.accept();
												// PeerConnection peerConn = new
												// PeerConnection(peerClient);
												// peerConn.startProcessing();
												debug("建立了一条连接");
											}
										} catch (IOException e) {
											debug("网络错误");
										}
									}
								}).start();

							} else if (info.groupFormed) {
								debug("已经成为组员");
								// 这里执行普通组员的任务
								// 通常是创建一个客户端向组长的服务器发送请求
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											InetAddress addr = info.groupOwnerAddress;
											final Socket socket = new Socket(addr, NetConstants.ServerPort);
											SocketHandler socketHandler = new SocketHandler(addr.getHostAddress(), NetConstants.ServerPort) {
												public void close() {
													try {
														socket.close();
													} catch (IOException e) {

													}
												}
											};
											socket.close();
										} catch (IOException e) {
											debug("客户端连接错误");
										} catch (Exception e) {
											debug("客户端连接未知错误");
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

	class PeerConnection {
		private Socket peerSocket;
		private Thread worker;

		public PeerConnection(Socket peer) {
			this.peerSocket = peer;
			this.worker = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						byte[] buffer = new byte[1024];
						InputStream peerInputs = peerSocket.getInputStream();
						while (true) {
							int readlen = peerInputs.read(buffer);
							if (readlen > 0) {
								// 对内容进行输出
							}
						}
					} catch (IOException e) {
					}
				}
			});
		}

		public void startProcessing() {
			this.worker.start();
		}

		public void stopProcessing() {
			this.worker.stop();
		}

		public String getPeerIp() {
			return peerSocket.getInetAddress().getHostAddress();
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
		this.isInGroup = false;
	}

	public void discoverPeers(final Activity updateActivity, final RelativeLayout groupList) {
		this.peersListListener = new WifiP2pManager.PeerListListener() {
			@Override
			public void onPeersAvailable(WifiP2pDeviceList peerList) {
				groupList.removeAllViews();
				groupOwnerTable.clear();
				final Iterator<WifiP2pDevice> devices = peerList.getDeviceList().iterator();
				// 检查附近所有的Peers中的Group Owner，只有Owner才会被
				// 记录并显示在组界面列表中
				new Thread(new Runnable() {
					public void run() {
						groupList.post(new Runnable() {
							public void run() {
								final LayoutInflater inflater = updateActivity.getLayoutInflater();
								final View theViewToGetGroupCardView = inflater.inflate(R.layout.group_card_layout, null);

								while (devices.hasNext()) {
									WifiP2pDevice device = devices.next();
									if (device.isGroupOwner()) {
										groupOwnerTable.put(device.deviceAddress, device);

										// 这里处理每个小组的信息的UI
										final TextView groupCard = (TextView) theViewToGetGroupCardView.findViewById(R.id.group_card);
										groupCard.setBackgroundResource(ColorCardSelector.selectOneCard());
										groupCard.setText(device.deviceAddress);
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
		// discoverPeers是异步执行的，调用了之后会立刻返回，但是发现的过程一直在进行，
		// 直到发现了某个设备时就会通知你
	}

	public void connectPeer(String peerAddress) {
		WifiP2pDevice device = groupOwnerTable.get(peerAddress);
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;

		wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				isInGroup = true;
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

	// 创建一个P2P小组，每个小组是一个独立的聊天房间，而且小组列表
	// 里列出的也是一个个小组(并不是每个附近P2P网路中的设备被列出)
	public void createGroup() {
		this.wifiP2pManager.createGroup(this.channel, new ActionListener() {
			@Override
			public void onSuccess() {
				isInGroup = true;
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

	// 解散聊天小组
	public void dissolveGroup(final Handler notifySuccessOrFailed) {
		this.wifiP2pManager.removeGroup(this.channel, new ActionListener() {
			@Override
			public void onSuccess() {
				isInGroup = false;
				try {
					serverSocket.close();
				} catch (IOException e) {
					debug("关闭组长服务端口失败");
				}
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

	// 这个类用来做调试向我显示运行信息用的
	public void debug(String content) {
		Toast.makeText(this.context, content, Toast.LENGTH_SHORT).show();
	}

	// 获取本设备的IP地址
	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("WifiPreference IpAddress", ex.toString());
		}
		return null;
	}
}
