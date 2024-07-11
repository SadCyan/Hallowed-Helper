package com.HallowedHelper;
//TODO: MAKE MAP OF STATUES TO HOLD TIMERS, TRIGGER TIMERS WHEN FIRE IS SHOT
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import lombok.Getter;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;


@PluginDescriptor(
	name = "HallowedHelper"
)
public class HallowedHelperPlugin extends Plugin
{
	private static final Set<Integer> HALLOW_REGIONS = ImmutableSet.of(10075,10187,10189,10188,10186);
	private static final Set<Integer> FLAME_ANIMATION_IDS = ImmutableSet.of(8664,8661,8662,8663);
	private static final Set<Integer> STATUE_IDS = ImmutableSet.of(38409,38410,38411,38412,38413,38414,38415,38416,38417,38418,38419,38420,38421,38422,38423,38424,38425);

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HallowedHelperOverlay overlay;

	@Getter
	private final Map<Animation, Instant> animations = new HashMap<>();

	@Getter
	private final Map<GameObject, Instant> flames = new HashMap<>();

	@Provides
	HallowedHelperConfig providesConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HallowedHelperConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		flames.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();

		if (obj.getId() == 38413)
		{
			flames.put(event.getGameObject(), Instant.now());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (flames.isEmpty())
		{
			return;
		}
		GameObject object = event.getGameObject();
		flames.remove(object);
	}
}
