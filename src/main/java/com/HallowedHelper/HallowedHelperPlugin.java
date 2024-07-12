package com.HallowedHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;

import lombok.Getter;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Preferences;
import net.runelite.api.Renderable;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;


@PluginDescriptor(
	name = "HallowedHelper"
)
public class HallowedHelperPlugin extends Plugin
{
	//FLAME_ID = 38427;
	//FLAME_ANIMATION_IDS = ImmutableSet.of(8664,8661,8662,8663);
	//static final Set<Integer> STATUE_ANIMATION_IDS = ImmutableSet.of(8656,8657,8658,8659);
	private static final Set<Integer> HALLOW_REGIONS = ImmutableSet.of(10075,10187,10189,10188,10186);
	private static final Set<Integer> STATUE_IDS = ImmutableSet.of(38409,38410,38411,38412,38416,38417,38418,38419,38420,38421,38422,38423,38424,38425);
	private static final int TILE_RADIUS = 20;
	private static final int LONG_DURATION_CUTOFF = 10;
	private static final int RACE_STYLE_SOUND_LOW = 3817;
	private static final int RACE_STYLE_SOUND_HIGH = 3818;
	private final Map<GameObject, Integer> statueShortTimers = new HashMap<>();
	private final Map<GameObject, Integer> statueLongTimers = new HashMap<>();
	public int syncTick = -1;

	@Getter
	private final Map<GameObject, WorldPoint[]> statues = new HashMap<>();

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HallowedHelperOverlay overlay;

	@Override
	protected void startUp() throws Exception
	{
		enableOverlay();
	}

	@Override
	protected void shutDown()
	{
		disableOverlay();
	}

	private boolean overlayEnabled = false;

	private void enableOverlay()
	{
		if (overlayEnabled)
		{
			return;
		}

		overlayEnabled = true;
		overlayManager.add(overlay);
	}

	private void disableOverlay()
	{
		if (overlayEnabled)
		{
			overlayManager.remove(overlay);
		}
		overlayEnabled = false;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		Player p = client.getLocalPlayer();
		if (p == null)
		{
			return;
		}

		/*if (!HALLOW_REGIONS.contains(p.getWorldLocation().getRegionID()))
		{
			disableOverlay();
			return;
		}	*/
		
		enableOverlay();
		System.err.println("tick");

		playCountdownSounds(client.getTickCount());
	}

	public boolean isWizardStatue(GameObject statue)
	{
		if (statue == null || !isGameObject(statue))
			return false;
		return STATUE_IDS.contains(statue.getId());
	}

	public boolean isGameObject(GameObject gameObject)
	{
		// 0 = Player
		// 1 = NPC
		// 2 = Object
		// 3 = Item
		return gameObject != null && (gameObject.getHash() >> 14 & 3) == 2;
	}

	public void filterGameObjects(WorldView view)
	{
		Tile[][] tiles = view.getScene().getTiles()[view.getPlane()];
		System.err.println("Tiles Loaded");
		final WorldPoint location = client.getLocalPlayer().getWorldLocation();
		/*if (!HALLOW_REGIONS.contains(location.getRegionID()))
		{
			return;
		}*/
		final int width = tiles.length;
		final int height = tiles[0].length;

		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				Tile tile = tiles[i][j];
				if (tile == null)
				{
					continue;
				}
				if (location.distanceTo(tile.getWorldLocation()) > TILE_RADIUS)
				{
					continue;
				}
				GameObject[] gameObjects = tile.getGameObjects();

				for (GameObject gameObject : gameObjects) 
				{
					if (isWizardStatue(gameObject)) //TODO: MAKE THIS IF STATEMENT ALSO CHECK FOR WIZARD STATUES THAT ARE ACCOUNTED FOR BY TRAVERSING THE HASHMAP AND CROSSREFERENCING TILE COORDS
					{
						System.err.println("Wizard Found!");
						statues.put(gameObject, getFlamePath(gameObject));
					}
				}
			}
		}
	}

	public void storeStatueTimer(GameObject statue, int cycle, int start)
	{
		if(cycle < LONG_DURATION_CUTOFF)
			statueShortTimers.put(statue, start);
		else
			statueLongTimers.put(statue, start);
	}

	public boolean isTracked(GameObject statue)
	{
		return statue != null && (statueShortTimers.get(statue) != null || statueLongTimers.get(statue) != null);
	}

	public Integer getStatueStart(GameObject statue)
	{
		if(statueLongTimers != null && statue != null && statueLongTimers.get(statue) != null)
			return statueLongTimers.get(statue);
		else if(statueShortTimers != null && statue != null && statueShortTimers.get(statue) != null)
			return statueShortTimers.get(statue);
		
		return null;
	}

	public int checkStatuesAnimation(GameObject statue)
	{
		if (statue != null)
		{
			return getAnimationId(statue.getRenderable());
		}
		return -1;
	}

	public int getAnimationId(Renderable renderable)
	{
		if (renderable instanceof DynamicObject)
		{
			Animation animation = ((DynamicObject) renderable).getAnimation();
			if (animation != null)
			{
				return animation.getId();
			}
		}
		return -1;
	}
	
	public int facingDirection(GameObject statue)
	{
		// 0 = South
		// 512 = West
		// 1024 = North
		// 1536 = East
		if (statue.getOrientation() < 256 || statue.getOrientation() >= 1792)
			return 0; // SE - SW
		else if (statue.getOrientation() >= 256 && statue.getOrientation() < 768)
			return 1; // SW - NW
		else if (statue.getOrientation() >= 768 && statue.getOrientation() < 1280)
			return 2; // NW - NE
		else if (statue.getOrientation() >= 1280 && statue.getOrientation() < 1792)
			return 3; // NE - SE
		return -1;
	}

	public WorldPoint[] getFlamePath(GameObject statue)
	{
		if(statue == null || facingDirection(statue) == -1)
		{
			return null;
		}

		WorldPoint statueWP = statue.getWorldLocation();

		if (facingDirection(statue) == 0)
		{
			WorldPoint[] tiles = {statueWP.dy(-1),statueWP.dy(-2),statueWP.dy(-3)};
			return tiles;
		}
		if (facingDirection(statue) == 1)
		{
			WorldPoint[] tiles = {statueWP.dx(-1),statueWP.dx(-2),statueWP.dx(-3)};
			return tiles;
		}
		if (facingDirection(statue) == 2)
		{
			WorldPoint[] tiles = {statueWP.dy(1),statueWP.dy(2),statueWP.dy(3)};
			return tiles;
		}
		if (facingDirection(statue) == 3)
		{
			WorldPoint[] tiles = {statueWP.dx(1),statueWP.dx(2),statueWP.dx(3)};
			return tiles;
		}
		
		return null;
	}

	private void playCountdownSounds(int startTick)
	{
			// As playSoundEffect only uses the volume argument when the in-game volume isn't muted, sound effect volume
			// needs to be set to the value desired for race sounds and afterwards reset to the previous value.
			Preferences preferences = client.getPreferences();
			int previousVolume = preferences.getSoundEffectVolume();
			if ((client.getTickCount() - startTick) % 16 == syncTick + 0 || (client.getTickCount() - startTick) % 16 == syncTick + 7 || (client.getTickCount() - startTick) % 16 == syncTick + 6 || (client.getTickCount() - startTick) % 16 == syncTick + 5)
			{
				// high sound for countdown 0
				client.playSoundEffect(RACE_STYLE_SOUND_HIGH);
			}
			else if((client.getTickCount() - startTick) % 16 == syncTick + 15 || (client.getTickCount() - startTick) % 16 == syncTick + 14 || (client.getTickCount() - startTick) % 16 == syncTick + 13 || (client.getTickCount() - startTick) % 16 == syncTick + 8)
			{
				// low sound for countdown 3,2,1
				client.playSoundEffect(RACE_STYLE_SOUND_LOW);
			}
			preferences.setSoundEffectVolume(previousVolume);
	}

	public void synchroTicks(int tickCount) 
	{
		syncTick = tickCount % 16;
	}
}
