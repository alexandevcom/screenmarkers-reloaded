package com.alexandev.screenmarkersreloaded;

public enum SidePanelTab
{
	COMBAT("Combat options", 0),
	SKILLS("Skills", 1),
	QUESTS("Character summary / Quests", 2),
	INVENTORY("Inventory", 3),
	EQUIPMENT("Worn equipment", 4),
	PRAYER("Prayer", 5),
	MAGIC("Magic", 6),
	FRIENDS("Friends list", 9),
	ACCOUNT("Account management", 10),
	LOGOUT("Logout", 10),
	SETTINGS("Settings", 11),
	EMOTES("Emotes", 12),
	MUSIC("Music", 13);

	public final String label;
	public final int index;

	SidePanelTab(String label, int index)
	{
		this.label = label;
		this.index = index;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
