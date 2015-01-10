package com.jikai.jojo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jikai.network.NetCore;

public class MainActivity extends Activity {
	private NetCore netcore;
	private LinearLayout groupList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		netcore = new NetCore(MainActivity.this);
		groupList = (LinearLayout) findViewById(R.id.group_list);
	}

	// 用来调试或向用户输出一段信息
	public void showMessage(String content) {
		Toast.makeText(this, content, Toast.LENGTH_LONG).show();
	}

	// 创建一个P2P通讯小组
	public void createGroup(View view) {
		netcore.createGroup();
	}

	// 扫描附近的P2P小组并把它们显示在组列表界面
	public void refreshGroups(View view) {
		netcore.discoverPeers(groupList);
	}
}
