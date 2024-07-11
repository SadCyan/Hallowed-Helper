package com.HallowedHelper;

import java.awt.Color;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.util.ColorUtil;

@ConfigGroup("hallowedhelper")
public interface HallowedHelperConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Greeting",
		description = "The message to show to the user when they login",
		position = 1
	)
	default boolean showGreenTearsTimer()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "blueTearsColor",
		name = "Blue Tears Color",
		description = "Color of Blue Tears timer",
		position = 2
	)
	default Color getBlueTearsColor()
	{
		return ColorUtil.colorWithAlpha(Color.CYAN, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "greenTearsColor",
		name = "Green Tears Color",
		description = "Color of Green Tears timer",
		position = 3
	)
	default Color getGreenTearsColor()
	{
		return ColorUtil.colorWithAlpha(Color.GREEN, 100);
	}
}
