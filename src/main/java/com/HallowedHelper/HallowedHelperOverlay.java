package com.HallowedHelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.util.ColorUtil;

public class HallowedHelperOverlay extends Overlay {
	private static final int MAX_TIME = 16;
	private final HallowedHelperPlugin plugin;
	private final Client client;
	private static final Color SAFE = Color.CYAN;
	private static final Color NEUTRAL = Color.LIGHT_GRAY;
	private static final Color WARNING = Color.YELLOW;
	private static final Color DANGER = Color.RED;



	@Inject
	private HallowedHelperOverlay(HallowedHelperPlugin plugin, Client client)
	{
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getStatues().isEmpty())
		{
			return null;
		}

		WorldView view = client.getLocalPlayer().getWorldView();

		plugin.getStatues().forEach((statue,flameTiles) -> {
			if (statue == null || flameTiles == null)
			{
				return;
			}

			if(plugin.isTracked(statue))
			{
				Color status;
                switch (plugin.checkStatuesAnimation(statue)) {
                    case 8659:
						status = SAFE;
						break;
                    case 8655:
						plugin.synchroTicks(client.getTickCount());
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

				final ProgressPieComponent progressPie = new ProgressPieComponent();
				progressPie.setDiameter(15);
				progressPie.setFill(status);
				progressPie.setBorderColor(ColorUtil.colorWithAlpha(status, 255));
				final int duration = client.getTickCount() - plugin.getStatueStart(statue);
				progressPie.setProgress(1 - (duration % MAX_TIME));
				progressPie.render(graphics);

				for (WorldPoint tile : flameTiles) {
					renderTile(graphics, view, tile, status);
				}
			}
		});
	
		return null;
	}

	private void renderTile(Graphics2D graphics, WorldView wv, WorldPoint wp, Color color)
	{
		LocalPoint lp = LocalPoint.fromWorld(wv, wp);
		if (lp != null)
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, lp);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
		}
	}
}