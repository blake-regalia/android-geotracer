package net.blurcast.android.gvg;

public class IdentityManager {
	
	private int index;
	private int value;

	public IdentityManager() {
		index = 0;
		value = 0;
	}
	
	public int getValue() {
		return value;
	}
	
	public void setValue(int val) {
		value = val;
	}
	
	public int getNewId() {
		return index++;
	}
}
