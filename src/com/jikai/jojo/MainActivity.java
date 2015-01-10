package com.jikai.jojo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jikai.network.NetCore;

public class MainActivity extends Activity {
	private NetCore netcore;
	private LinearLayout groupList;
	private Handler ifOkGoToGroupInterface; // 用来检测创建小组是否成功，接收到成功消息时就切换到小组聊天界面

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		netcore = new NetCore(MainActivity.this);
		groupList = (LinearLayout) findViewById(R.id.group_list);
		ifOkGoToGroupInterface = new Handler() {
			@Override
			public void handleMessage(Message message) {
				switch (message.what) {
				case 1:
					Intent intent = new Intent(MainActivity.this, GroupChatActivity.class);
					startActivity(intent);
					break;
				case -1:
					showMessage("创建小组失败，请尝试重新创建小组");
					break;
				case 2:
					showMessage("小组已解散");
					break;
				case -2:
					showMessage("解散小组时发生错误");
					break;
				default:
					showMessage("未知错误");
				}
			}
		};
	}

	// 用来调试或向用户输出一段信息
	public void showMessage(String content) {
		Toast.makeText(this, content, Toast.LENGTH_LONG).show();
	}

	// 创建一个P2P通讯小组
	public void createGroup(View view) {
		netcore.createGroup(ifOkGoToGroupInterface);
	}

	// 扫描附近的P2P小组并把它们显示在组列表界面
	public void refreshGroups(View view) {
		netcore.discoverPeers(groupList);
	}
}
