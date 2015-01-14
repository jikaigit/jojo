package com.jikai.jojo;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class BubbleView extends View {
	private Context context;
	private TextView groupCard;
	private float centerX;
	private float centerY;

	public BubbleView(Context context) {
		super(context);
		this.groupCard = new TextView(this.context);
		this.centerX = 0;
		this.centerY = 0;
	}
}