package com.jikai.jojo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.jikai.network.NetCore;

public class MainActivity extends Activity {
	private NetCore netcore;
	private RelativeLayout groupList;
	private Handler asyncIsOkHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		asyncIsOkHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				switch (message.what) {
				case 11:
					Intent intent = new Intent(MainActivity.this, GroupChatActivity.class);
					startActivity(intent);
					break;
				case 12:
					showMessage("����С��ʧ�ܣ��볢�����´���С��");
					break;
				case 21:
					showMessage("С���ѽ�ɢ");
					break;
				case 22:
					showMessage("��ɢС��ʱ��������");
					break;
				case 31:
					showMessage("���ӳɹ�");
					break;
				case 32:
					showMessage("����ʧ��");
					break;
				default:
					showMessage("δ֪����");
				}
			}
		};
		netcore = new NetCore(MainActivity.this, asyncIsOkHandler);
		groupList = (RelativeLayout) findViewById(R.id.group_list);
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
