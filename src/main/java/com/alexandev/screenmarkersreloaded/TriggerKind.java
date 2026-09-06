package com.alexandev.screenmarkersreloaded;

public enum TriggerKind
{
	ALWAYS("Always"),
	BANK_OPEN("Bank open"),
	ANIMATION_ACTIVE("Animation"),
	INVENTORY_FULL("Inv full"),
	INVENTORY_EMPTY("Inv empty"),
	INVENTORY_HAS_ITEM("Has item"),
	TAB_ACTIVE("Tab active");

	public final String label;

	TriggerKind(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
