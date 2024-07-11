package com.HallowedHelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import net.runelite.api.ObjectID;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.util.ColorUtil;

public class HallowedHelperOverlay extends Overlay {
	private static final Duration MAX_TIME = Duration.ofMillis(4800);
	private final HallowedHelperConfig config;
	private final HallowedHelperPlugin plugin;

	@Inject
	private HallowedHelperOverlay(HallowedHelperConfig config, HallowedHelperPlugin plugin)
	{
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getFlames().isEmpty())
		{
			return null;
		}

		Color blueTearsFill = config.getBlueTearsColor();
		Color blueTearsBorder = ColorUtil.colorWithAlpha(blueTearsFill, 255);

		plugin.getFlames().forEach((object, timer) ->
		{
			final Point position = object.getCanvasLocation(100);

			if (position == null)
			{
				return;
			}

			final ProgressPieComponent progressPie = new ProgressPieComponent();
			progressPie.setDiameter(15);

			if (object.getId() == ObjectID.FIRE_38427)
			{
				progressPie.setFill(blueTearsFill);
				progressPie.setBorderColor(blueTearsBorder);
				
			}

			progressPie.setPosition(position);

			final Duration duration = Duration.between(timer, Instant.now());
			progressPie.setProgress(1 - (duration.compareTo(MAX_TIME) < 0
				? (double) duration.toMillis() / MAX_TIME.toMillis()
				: 1));

			progressPie.render(graphics);
		});

		return null;
	}
}