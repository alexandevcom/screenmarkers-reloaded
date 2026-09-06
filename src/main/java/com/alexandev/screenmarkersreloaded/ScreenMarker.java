package com.alexandev.screenmarkersreloaded;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScreenMarker
{
	public enum Combine { ALL, ANY }

	public String id = UUID.randomUUID().toString();
	public String name = "Marker";
	public boolean enabled = true;
	public boolean expanded = false;
	public String groupId; // null = ungrouped

	public MarkerKind kind = MarkerKind.SCREEN_RECT;

	// SCREEN_RECT
	public int x;
	public int y;
	public int width;
	public int height;

	// TILE
	public int tileX;
	public int tileY;
	public int tilePlane;

	// OBJECT_TYPE
	public int objectId = -1;

	// INVENTORY_ITEM
	public int itemId = -1;

	// WIDGET (spell, prayer, button, etc.)
	public int widgetPackedId = -1;

	// Common visuals
	public int borderColor = 0xC800FF78;
	public int fillColor = 0x2800FF78;
	public int borderThickness = 2;
	public boolean labelVisible = true;

	public List<TriggerConfig> triggers = defaultTriggers();
	public Combine combine = Combine.ALL;

	// Legacy single-trigger field (old saves). Migrated to triggers on load.
	public TriggerConfig trigger;

	private static List<TriggerConfig> defaultTriggers()
	{
		List<TriggerConfig> list = new ArrayList<>();
		list.add(new TriggerConfig());
		return list;
	}
}
