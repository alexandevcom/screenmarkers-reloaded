package com.alexandev.screenmarkersreloaded;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ScreenMarkersReloadedPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ScreenMarkersReloadedPlugin.class);
		RuneLite.main(args);
	}
}
