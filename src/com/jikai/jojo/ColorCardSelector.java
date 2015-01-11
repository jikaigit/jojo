package com.jikai.jojo;

import java.util.LinkedList;
import java.util.Random;

public class ColorCardSelector {
	private int colorNumbers = 6;
	private Integer[] colorCard = { R.drawable.group_card_blue, R.drawable.group_card_cyan, R.drawable.group_card_green, R.drawable.group_card_orige, R.drawable.group_card_purple, R.drawable.group_card_yellow };
	private LinkedList<Integer> selectHistory = new LinkedList<Integer>();
	private int selectHistoryCount = 0;

	public static int selectOneCard() {
		while (true) {
			int randnum = ((new Random()).nextInt()) % 6;
		}
	}
}