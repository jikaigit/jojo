package com.jikai.jojo;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

public class Effects {
	public static void makeShadow(View view, int x, int y, int size) {
		Paint paint = new Paint();
		BlurMaskFilter blurMaskFilter = new BlurMaskFilter(size, BlurMaskFilter.Blur.INNER);
		paint.setColor(Color.GRAY);
		paint.setMaskFilter(blurMaskFilter);

		Drawable background = view.getBackground();
		int shadowWidth = view.getWidth() + 2;
		int shadowHeight = view.getHeight() + 2;
		Bitmap bitmap = Bitmap.createBitmap(shadowWidth, shadowHeight, background.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		background.setBounds(0, 0, shadowWidth, shadowHeight);
		background.draw(canvas);
		canvas.drawBitmap(bitmap.extractAlpha(paint, null), x, y, paint);
	}
}