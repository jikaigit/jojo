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

	// �������Ի����û����һ����Ϣ
	public void showMessage(String content) {
		Toast.makeText(this, content, Toast.LENGTH_LONG).show();
	}

	// ����һ��P2PͨѶС��
	public void createGroup(View view) {
		netcore.createGroup();
	}

	// ɨ�踽����P2PС�鲢��������ʾ�����б����
	public void refreshGroups(View view) {
		netcore.discoverPeers(groupList);
	}
}
