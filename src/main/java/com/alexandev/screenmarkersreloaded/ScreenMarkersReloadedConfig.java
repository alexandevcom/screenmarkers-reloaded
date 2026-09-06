package com.alexandev.screenmarkersreloaded;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

@ConfigGroup(ScreenMarkersReloadedConfig.GROUP)
public interface ScreenMarkersReloadedConfig extends Config
{
	String GROUP = "smreloaded";
	String MARKERS_KEY = "markers";
	String GROUPS_KEY = "groups";
}
