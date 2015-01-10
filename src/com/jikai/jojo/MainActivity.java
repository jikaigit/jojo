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
	private Handler ifOkGoToGroupInterface; // ������ⴴ��С���Ƿ�ɹ������յ��ɹ���Ϣʱ���л���С���������

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
					showMessage("����С��ʧ�ܣ��볢�����´���С��");
					break;
				case 2:
					showMessage("С���ѽ�ɢ");
					break;
				case -2:
					showMessage("��ɢС��ʱ��������");
					break;
				default:
					showMessage("δ֪����");
				}
			}
		};
	}

	// �������Ի����û����һ����Ϣ
	public void showMessage(String content) {
		Toast.makeText(this, content, Toast.LENGTH_LONG).show();
	}

	// ����һ��P2PͨѶС��
	public void createGroup(View view) {
		netcore.createGroup(ifOkGoToGroupInterface);
	}

	// ɨ�踽����P2PС�鲢��������ʾ�����б����
	public void refreshGroups(View view) {
		netcore.discoverPeers(groupList);
	}
}
