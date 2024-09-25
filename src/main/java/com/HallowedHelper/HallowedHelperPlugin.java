package com.HallowedHelper;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;

import lombok.Getter;
import lombok.Setter;
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
	//private static final Set<Integer> HALLOW_REGIONS = ImmutableSet.of(10075,10187,10189,10188,10186);
	private static final Set<Integer> STATUE_ANIMATION_IDS = ImmutableSet.of(8656,8657,8658,8659);
	private static final Set<Integer> STATUE_IDS = ImmutableSet.of(38409,38410,38411,38412,38416,38417,38418,38419,38420,38421,38422,38423,38424,38425);
	private static final int TILE_RADIUS = 20;
	private static final int RACE_STYLE_SOUND_LOW = 3817;
	private static final int RACE_STYLE_SOUND_HIGH = 3818;
	public int syncTick = -1;
	public int loginTick = -1;
	
	@Getter
	private final Map<GameObject, Integer> trackedStatues = new HashMap<>();

	@Getter
	private final Map<Tile, GameObject> tileTracker = new HashMap<>();

	@Getter @Setter
	private int currentPlane = -1001;

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
		loginTick = client.getTickCount();
		currentPlane = client.getLocalPlayer().getWorldLocation().getPlane();
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
			tileTracker.clear();
			trackedStatues.clear();
		}
		overlayEnabled = false;
	}

	public void cleanOverlay(int newPlane)
	{
		disableOverlay();
		currentPlane = newPlane;
		enableOverlay();
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		Player p = client.getLocalPlayer();
		if (p == null)
		{
			return;
		}
		
		enableOverlay();
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

	public synchronized void filterGameObjects(WorldView view)
	{
		Tile[][] tiles = view.getScene().getTiles()[view.getPlane()];
		final WorldPoint location = client.getLocalPlayer().getWorldLocation();
		final int width = tiles.length;
		final int height = tiles[0].length;

		for (int i = 0; i < width-1; i++)
		{
			for (int j = 0; j < height-1; j++)
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
				if (tileTracker.containsKey(tile))
				{
					continue;
				}	
				
				GameObject statueFound = getStatueFromTile(tile);
				if(statueFound == null)
				{
					continue;
				}

				if(!tileTracker.containsKey(tile) && !trackedStatues.containsKey(statueFound))
				{
					tileTracker.put(tile, statueFound);
					tiles[i][j+1] = null;
					tiles[i+1][j] = null;
					tiles[i+1][j+1] = null;
					trackedStatues.put(statueFound, -2);
				}	

			}
		}
	}

	public synchronized void updateStatueAnimationIds()
	{
		if (tileTracker == null || tileTracker.isEmpty())
		{
			return;
		}

		tileTracker.forEach((tile, statue) -> {
			if (tile == null || statue == null)
			{
				return;
			}

			GameObject checkedStatue = getStatueFromTile(tile);
			if (checkedStatue == null)
			{
				tileTracker.remove(tile);
				return;
			}
			int checkedAnimationID = checkStatuesAnimation(checkedStatue);
			if (checkedAnimationID == -1001)
			{
				trackedStatues.remove(statue);
				tileTracker.remove(tile);
			}
			if (checkedAnimationID == trackedStatues.get(statue))
			{
				return;
			}

			if (STATUE_ANIMATION_IDS.contains(checkedAnimationID))
			{
				tileTracker.put(tile, checkedStatue);
				trackedStatues.remove(statue);
				trackedStatues.put(statue, checkedAnimationID);
			}
		});
	}

	public GameObject getStatueFromTile(Tile tile)
	{
		if (tile == null)
		{
			return null;
		}

		GameObject[] gameObjects = tile.getGameObjects();
		if(gameObjects == null)
		{
			return null;
		}

		for (GameObject gameObject : gameObjects) 
		{
			if (isWizardStatue(gameObject))
			{
				return gameObject;
			}
		}

		return null;
	}

	public int checkStatuesAnimation(GameObject statue)
	{
		if (statue != null)
		{
			int id = getAnimationId(statue.getRenderable());
			//System.err.println(getRelativeTick(client.getTickCount()) + " Animation Checked for Statue at x:" + statue.getX() + ",y:" + statue.getY() + " [AnimationID]:" + id);
			return id;
		}
		return -1001;
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
		//this will always be the SW tile of the statue
		WorldPoint statueWP;

		if (facingDirection(statue) == 0)
		{
			//hand is in the SW tile -> S
			statueWP = statue.getWorldLocation();
			WorldPoint[] tiles = {statueWP.dy(-1),statueWP.dy(-2),statueWP.dy(-3)};
			return tiles;
		}
		if (facingDirection(statue) == 1)
		{
			//hand is in the NW tile -> W
			statueWP = statue.getWorldLocation().dy(1);
			WorldPoint[] tiles = {statueWP.dx(-1),statueWP.dx(-2),statueWP.dx(-3)};
			return tiles;
		}
		if (facingDirection(statue) == 2)
		{
			//hand is in the NE tile -> N
			statueWP = statue.getWorldLocation().dx(1).dy(1);
			WorldPoint[] tiles = {statueWP.dy(1),statueWP.dy(2),statueWP.dy(3)};
			return tiles;
		}
		if (facingDirection(statue) == 3)
		{
			//hand is in the SE tile -> E
			statueWP = statue.getWorldLocation().dx(1);
			WorldPoint[] tiles = {statueWP.dx(1),statueWP.dx(2),statueWP.dx(3)};
			return tiles;
		}
		
		return null;
	}

	public int getRelativeTick(int tick)
	{
		return tick - loginTick;
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

	public Color outOfRange(WorldPoint tiles, Color status) 
	{
		if (client.getLocalPlayer().getWorldLocation().distanceTo(tiles) <= 8)
			return status;	
		return Color.DARK_GRAY;
	}
}
