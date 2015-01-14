package com.jikai.jojo;

import java.util.LinkedList;
import java.util.Random;

public class ColorCardSelector {
	private static int colorNumbers = 6;
	private static Integer[] colorCard = { R.drawable.group_card_blue, R.drawable.group_card_cyan, R.drawable.group_card_green, R.drawable.group_card_orige, R.drawable.group_card_purple, R.drawable.group_card_yellow };
	private static LinkedList<Integer> selectHistory = new LinkedList<Integer>();
	private static int selectHistoryCount = 0;

	public static int selectOneCard() {
		while (true) {
			int randnum = Math.abs(((new Random()).nextInt()) % colorNumbers);
			for (int i = 0; i < selectHistoryCount; i++) {
				if (selectHistory.get(i) == colorCard[randnum])
					continue;
			}
			if (selectHistoryCount == 3) {
				selectHistory.removeFirst();
				selectHistory.add(colorCard[randnum]);
			} else {
				selectHistory.add(colorCard[randnum]);
				selectHistoryCount++;
			}
			return colorCard[randnum];
		}
	}
}