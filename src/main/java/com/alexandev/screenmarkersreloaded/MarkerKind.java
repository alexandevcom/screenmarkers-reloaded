package com.alexandev.screenmarkersreloaded;

public enum MarkerKind
{
	SCREEN_RECT("Screen"),
	TILE("Tile"),
	OBJECT_TYPE("Object type"),
	INVENTORY_ITEM("Inventory item"),
	WIDGET("Widget / spell"),
	GROUND_ITEM("Ground item");

	public final String label;

	MarkerKind(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
