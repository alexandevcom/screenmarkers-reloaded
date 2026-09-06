package com.alexandev.screenmarkersreloaded;

public class TriggerConfig
{
	public TriggerKind kind = TriggerKind.ALWAYS;
	public boolean inverted = false;
	public int animationId = -1;
	public int itemId = -1;
	public int itemCount = 1;
	public CountOp itemCountOp = CountOp.AT_LEAST;
	public SidePanelTab tab = SidePanelTab.INVENTORY;

	public enum CountOp
	{
		AT_LEAST("≥"),
		AT_MOST("≤"),
		EQUAL("=");

		public final String symbol;
		CountOp(String s) { this.symbol = s; }
		@Override public String toString() { return symbol; }
	}
}
