package com.HallowedHelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class HallowedHelperOverlay extends Overlay {
	private final HallowedHelperPlugin plugin;
	private final Client client;
	private static final Color SAFE = Color.CYAN;
	private static final Color NEUTRAL = Color.LIGHT_GRAY;
	private static final Color WARNING = Color.YELLOW;
	private static final Color DANGER = Color.RED;

	@Inject
	private HallowedHelperOverlay(HallowedHelperPlugin plugin, Client client) {
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		Player p = client.getLocalPlayer();
		if (p == null) {
			return null;
		}
		if (plugin.getCurrentPlane() != p.getWorldLocation().getPlane())
		{
			plugin.cleanOverlay(p.getWorldLocation().getPlane());
		}

		if (plugin.getRelativeTick(client.getTickCount()) % 10 == 0) 
		{
			plugin.filterGameObjects(p.getWorldView());
		}

		if (plugin.getTileTracker().isEmpty()) 
		{
			return null;
		}

		plugin.updateStatueAnimationIds();
		plugin.getTileTracker().forEach((tile, statue) -> {
			if (tile == null || statue == null) {
				return;
			}
			
			if (plugin.getTrackedStatues().get(statue) == -1) {
				return;
			}

			if (plugin.getTrackedStatues().containsKey(statue)) {
				Color status;
				switch (plugin.getTrackedStatues().get(statue)) {
					case 8659:
					case 8655:
						status = SAFE;
						break;
					case 8656:
						status = WARNING;
						break;
					case 8657:
					case 8658:
						status = DANGER;
						break;
					default:
						status = NEUTRAL;
						break;
				}

				WorldPoint[] flameTiles = plugin.getFlamePath(statue);
				if (flameTiles == null)
				{
					return;
				}
				
				for (WorldPoint tiles : flameTiles) {
					if(p.getWorldLocation().distanceTo(tiles) <= 26)
					{
						status = plugin.outOfRange(tiles, status);
						renderTile(graphics, tiles, status);
					}
				}
			}
		});

		return null;
	}

	private void renderTile(Graphics2D graphics, WorldPoint wp, Color color) {
		LocalPoint lp = LocalPoint.fromWorld(client.getLocalPlayer().getWorldView(), wp);
		if (lp != null) {
			Polygon poly = Perspective.getCanvasTilePoly(client, lp);
			if (poly != null) {
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
		}
	}
}