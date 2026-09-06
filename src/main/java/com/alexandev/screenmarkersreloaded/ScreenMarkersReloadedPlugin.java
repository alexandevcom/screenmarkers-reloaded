package com.alexandev.screenmarkersreloaded;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.TileItem;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Screen Markers Reloaded",
	description = "Dynamic screen and world markers with trigger-based visibility",
	tags = {"screen", "marker", "overlay", "trigger", "tile", "object", "qol"}
)
public class ScreenMarkersReloadedPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private OverlayManager overlayManager;
	@Inject private ScreenMarkersReloadedOverlay overlay;
	@Inject private CreationOverlay creationOverlay;
	@Inject private MouseManager mouseManager;
	@Inject private KeyManager keyManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ConfigManager configManager;
	@Inject @lombok.Getter private ItemManager itemManager;
	@Inject private Gson gson;

	public ClientThread getClientThread() { return clientThread; }

	private final List<ScreenMarker> markers = new ArrayList<>();
	private final List<MarkerGroup> groups = new ArrayList<>();
	// Objects currently on scene keyed by object ID
	private final Map<Integer, Set<GameObject>> objectsById = new HashMap<>();
	// Ground items: itemId -> tiles where that item lies
	private final Map<Integer, Set<Tile>> groundItemsById = new HashMap<>();
	private final Map<TileItem, Tile> tileForGroundItem = new HashMap<>();

	private NavigationButton navButton;
	private ScreenMarkersReloadedPanel panel;

	private volatile MarkerKind creationKind; // null = no creation in progress
	private volatile ScreenMarker editingMarker; // non-null = updating existing marker's bounds
	private volatile Point dragStart;
	private volatile Point dragEnd;
	private volatile TriggerConfig pickingItemForTrigger; // null = not in item-pick mode
	private volatile Runnable pickingCallback;

	private final MouseAdapter mouseAdapter = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent e)
		{
			if (creationKind != MarkerKind.SCREEN_RECT) return e;
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				dragStart = e.getPoint();
				dragEnd = e.getPoint();
				e.consume();
			}
			return e;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent e)
		{
			if (creationKind != MarkerKind.SCREEN_RECT || dragStart == null) return e;
			dragEnd = e.getPoint();
			e.consume();
			return e;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent e)
		{
			if (creationKind != MarkerKind.SCREEN_RECT || dragStart == null) return e;
			dragEnd = e.getPoint();
			Rectangle r = CreationOverlay.rect(dragStart, dragEnd);
			ScreenMarker editing = editingMarker;
			endCreationMode();
			if (r.width >= 4 && r.height >= 4)
			{
				if (editing != null)
				{
					editing.x = r.x;
					editing.y = r.y;
					editing.width = r.width;
					editing.height = r.height;
					notifyMarkerChanged();
					refreshPanel();
				}
				else
				{
					ScreenMarker m = new ScreenMarker();
					m.kind = MarkerKind.SCREEN_RECT;
					m.name = "Marker " + (markers.size() + 1);
					m.x = r.x;
					m.y = r.y;
					m.width = r.width;
					m.height = r.height;
					addMarker(m);
				}
			}
			e.consume();
			return e;
		}
	};

	private final KeyListener keyListener = new KeyListener()
	{
		@Override public void keyTyped(KeyEvent e) {}
		@Override public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				if (creationKind != null)
				{
					endCreationMode();
					e.consume();
				}
				else if (pickingItemForTrigger != null)
				{
					endItemPick();
					e.consume();
				}
			}
		}
		@Override public void keyReleased(KeyEvent e) {}
	};

	@Override
	protected void startUp()
	{
		load();
		overlayManager.add(overlay);
		overlayManager.add(creationOverlay);
		mouseManager.registerMouseListener(mouseAdapter);
		keyManager.registerKeyListener(keyListener);

		panel = new ScreenMarkersReloadedPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Screen Markers Reloaded")
			.priority(7)
			.icon(ImageUtil.loadImageResource(getClass(), "icon.png"))
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(creationOverlay);
		mouseManager.unregisterMouseListener(mouseAdapter);
		keyManager.unregisterKeyListener(keyListener);
		clientToolbar.removeNavigation(navButton);
		objectsById.clear();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		MenuEntry entry = e.getMenuEntry();
		if (pickingItemForTrigger != null)
		{
			int itemId = entry.getItemId();
			if (itemId > 0)
			{
				pickingItemForTrigger.itemId = itemId;
				endItemPick();
				notifyMarkerChanged();
				refreshPanel();
				e.consume();
			}
			return;
		}
		if (creationKind == null) return;
		if (creationKind == MarkerKind.TILE)
		{
			WorldPoint wp = null;
			Tile selected = client.getSelectedSceneTile();
			if (selected != null) wp = selected.getWorldLocation();
			ScreenMarker editing = editingMarker;
			if (wp != null)
			{
				if (editing != null)
				{
					editing.tileX = wp.getX();
					editing.tileY = wp.getY();
					editing.tilePlane = wp.getPlane();
					notifyMarkerChanged();
					refreshPanel();
				}
				else
				{
					ScreenMarker m = new ScreenMarker();
					m.kind = MarkerKind.TILE;
					m.name = "Tile " + (markers.size() + 1);
					m.tileX = wp.getX();
					m.tileY = wp.getY();
					m.tilePlane = wp.getPlane();
					addMarker(m);
				}
			}
			endCreationMode();
			e.consume();
		}
		else if (creationKind == MarkerKind.INVENTORY_ITEM)
		{
			int itemId = entry.getItemId();
			if (itemId > 0)
			{
				ScreenMarker editing = editingMarker;
				String nameFromItem = null;
				try
				{
					nameFromItem = itemManager.getItemComposition(itemId).getName();
				}
				catch (Exception ignored) {}
				if (editing != null)
				{
					editing.itemId = itemId;
					if (nameFromItem != null && !nameFromItem.isEmpty()) editing.name = nameFromItem;
					notifyMarkerChanged();
					refreshPanel();
				}
				else
				{
					ScreenMarker m = new ScreenMarker();
					m.kind = MarkerKind.INVENTORY_ITEM;
					m.itemId = itemId;
					m.name = (nameFromItem != null && !nameFromItem.isEmpty()) ? nameFromItem : ("Item " + itemId);
					addMarker(m);
				}
				endCreationMode();
				e.consume();
			}
		}
		else if (creationKind == MarkerKind.GROUND_ITEM)
		{
			int itemId = -1;
			MenuAction action = entry.getType();
			if (action == MenuAction.GROUND_ITEM_FIRST_OPTION
				|| action == MenuAction.GROUND_ITEM_SECOND_OPTION
				|| action == MenuAction.GROUND_ITEM_THIRD_OPTION
				|| action == MenuAction.GROUND_ITEM_FOURTH_OPTION
				|| action == MenuAction.GROUND_ITEM_FIFTH_OPTION
				|| action == MenuAction.EXAMINE_ITEM_GROUND)
			{
				itemId = entry.getIdentifier();
			}
			else if (entry.getItemId() > 0)
			{
				itemId = entry.getItemId();
			}
			if (itemId > 0)
			{
				String nameFromItem = null;
				try { nameFromItem = itemManager.getItemComposition(itemId).getName(); } catch (Exception ignored) {}
				ScreenMarker editing = editingMarker;
				if (editing != null)
				{
					editing.itemId = itemId;
					if (nameFromItem != null && !nameFromItem.isEmpty()) editing.name = nameFromItem;
					notifyMarkerChanged();
					refreshPanel();
				}
				else
				{
					ScreenMarker m = new ScreenMarker();
					m.kind = MarkerKind.GROUND_ITEM;
					m.itemId = itemId;
					m.name = (nameFromItem != null && !nameFromItem.isEmpty()) ? nameFromItem : ("Item " + itemId);
					addMarker(m);
				}
				endCreationMode();
				e.consume();
			}
		}
		else if (creationKind == MarkerKind.WIDGET)
		{
			net.runelite.api.widgets.Widget w = entry.getWidget();
			int packed = -1;
			if (w != null) packed = w.getId();
			else if (entry.getParam1() > 0) packed = entry.getParam1();
			if (packed >= 0)
			{
				String target = entry.getTarget();
				String clean = target == null ? null : net.runelite.client.util.Text.removeTags(target);
				ScreenMarker editing = editingMarker;
				if (editing != null)
				{
					editing.widgetPackedId = packed;
					if (clean != null && !clean.isEmpty()) editing.name = clean;
					notifyMarkerChanged();
					refreshPanel();
				}
				else
				{
					ScreenMarker m = new ScreenMarker();
					m.kind = MarkerKind.WIDGET;
					m.widgetPackedId = packed;
					m.name = (clean != null && !clean.isEmpty()) ? clean : "Widget";
					addMarker(m);
				}
				endCreationMode();
				e.consume();
			}
		}
		else if (creationKind == MarkerKind.OBJECT_TYPE)
		{
			MenuAction action = entry.getType();
			int objId = -1;
			String objName = null;
			if (action == MenuAction.GAME_OBJECT_FIRST_OPTION
				|| action == MenuAction.GAME_OBJECT_SECOND_OPTION
				|| action == MenuAction.GAME_OBJECT_THIRD_OPTION
				|| action == MenuAction.GAME_OBJECT_FOURTH_OPTION
				|| action == MenuAction.GAME_OBJECT_FIFTH_OPTION
				|| action == MenuAction.EXAMINE_OBJECT)
			{
				objId = entry.getIdentifier();
				ObjectComposition comp = client.getObjectDefinition(objId);
				if (comp != null)
				{
					if (comp.getImpostorIds() != null && comp.getImpostor() != null)
					{
						comp = comp.getImpostor();
					}
					objName = comp.getName();
				}
			}
			if (objId >= 0)
			{
				ScreenMarker editing = editingMarker;
				if (editing != null)
				{
					editing.objectId = objId;
					if (objName != null && !objName.isEmpty()) editing.name = objName;
					notifyMarkerChanged();
					refreshPanel();
				}
				else
				{
					ScreenMarker m = new ScreenMarker();
					m.kind = MarkerKind.OBJECT_TYPE;
					m.name = objName != null && !objName.isEmpty() ? objName : ("Object " + objId);
					m.objectId = objId;
					addMarker(m);
					rescanObjects();
				}
				endCreationMode();
				e.consume();
			}
			else
			{
				// Not an object — inform user via toast? For now just ignore.
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject go = e.getGameObject();
		objectsById.computeIfAbsent(go.getId(), k -> new HashSet<>()).add(go);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		GameObject go = e.getGameObject();
		Set<GameObject> set = objectsById.get(go.getId());
		if (set != null) set.remove(go);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		GameState s = e.getGameState();
		if (s == GameState.LOADING || s == GameState.HOPPING || s == GameState.LOGIN_SCREEN)
		{
			objectsById.clear();
			groundItemsById.clear();
			tileForGroundItem.clear();
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned e)
	{
		TileItem it = e.getItem();
		Tile tile = e.getTile();
		tileForGroundItem.put(it, tile);
		groundItemsById.computeIfAbsent(it.getId(), k -> new HashSet<>()).add(tile);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned e)
	{
		TileItem it = e.getItem();
		Tile tile = tileForGroundItem.remove(it);
		Set<Tile> tiles = groundItemsById.get(it.getId());
		if (tiles != null && tile != null) tiles.remove(tile);
	}

	public Set<Tile> getGroundTilesForMarker(ScreenMarker m)
	{
		if (m.kind != MarkerKind.GROUND_ITEM) return Collections.emptySet();
		Set<Tile> s = groundItemsById.get(m.itemId);
		return s == null ? Collections.emptySet() : s;
	}

	public Set<GameObject> getObjectsForMarker(ScreenMarker m)
	{
		if (m.kind != MarkerKind.OBJECT_TYPE) return Collections.emptySet();
		Set<GameObject> s = objectsById.get(m.objectId);
		return s == null ? Collections.emptySet() : s;
	}

	private void rescanObjects()
	{
		// No public API to iterate all scene objects easily; rely on future spawn events.
	}

	public List<ScreenMarker> getMarkers() { return markers; }
	public List<MarkerGroup> getGroups() { return groups; }

	public MarkerGroup getGroup(String id)
	{
		if (id == null) return null;
		for (MarkerGroup g : groups) if (id.equals(g.id)) return g;
		return null;
	}

	public boolean isMarkerActive(ScreenMarker m)
	{
		if (!m.enabled) return false;
		MarkerGroup g = getGroup(m.groupId);
		if (g != null && !g.enabled) return false;
		return true;
	}

	public void addGroup()
	{
		MarkerGroup g = new MarkerGroup();
		g.name = "Group " + (groups.size() + 1);
		groups.add(g);
		save();
		refreshPanel();
	}

	public void deleteGroup(MarkerGroup g, boolean alsoDeleteMarkers)
	{
		if (alsoDeleteMarkers)
		{
			markers.removeIf(m -> g.id.equals(m.groupId));
		}
		else
		{
			for (ScreenMarker m : markers)
			{
				if (g.id.equals(m.groupId)) m.groupId = null;
			}
		}
		groups.remove(g);
		save();
		refreshPanel();
	}

	public String exportGroupJson(MarkerGroup g)
	{
		java.util.Map<String, Object> out = new java.util.HashMap<>();
		out.put("group", g);
		java.util.List<ScreenMarker> gm = new ArrayList<>();
		for (ScreenMarker m : markers) if (g.id.equals(m.groupId)) gm.add(m);
		out.put("markers", gm);
		return gson.toJson(out);
	}

	public boolean importGroupJson(String json)
	{
		try
		{
			com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
			MarkerGroup g = gson.fromJson(obj.get("group"), MarkerGroup.class);
			Type t = new TypeToken<List<ScreenMarker>>() {}.getType();
			List<ScreenMarker> importedMarkers = gson.fromJson(obj.get("markers"), t);
			if (g == null || importedMarkers == null) return false;
			// Give fresh IDs to avoid collisions
			g.id = java.util.UUID.randomUUID().toString();
			for (ScreenMarker m : importedMarkers)
			{
				m.id = java.util.UUID.randomUUID().toString();
				m.groupId = g.id;
				if ((m.triggers == null || m.triggers.isEmpty()) && m.trigger != null)
				{
					m.triggers = new ArrayList<>();
					m.triggers.add(m.trigger);
				}
				if (m.triggers == null) m.triggers = new ArrayList<>();
				if (m.triggers.isEmpty()) m.triggers.add(new TriggerConfig());
				if (m.combine == null) m.combine = ScreenMarker.Combine.ALL;
				m.trigger = null;
			}
			groups.add(g);
			markers.addAll(importedMarkers);
			save();
			refreshPanel();
			return true;
		}
		catch (Exception e)
		{
			log.warn("Import failed", e);
			return false;
		}
	}
	public MarkerKind getCreationKind() { return creationKind; }
	public ScreenMarker getEditingMarker() { return editingMarker; }
	public boolean isCreationMode() { return creationKind != null; }
	public Point getDragStart() { return dragStart; }
	public Point getDragEnd() { return dragEnd; }

	public void startCreationMode(MarkerKind kind)
	{
		creationKind = kind;
		dragStart = null;
		dragEnd = null;
		refreshPanel();
	}

	public void endCreationMode()
	{
		creationKind = null;
		editingMarker = null;
		dragStart = null;
		dragEnd = null;
		refreshPanel();
	}

	public void startEditMode(ScreenMarker marker)
	{
		editingMarker = marker;
		creationKind = marker.kind;
		dragStart = null;
		dragEnd = null;
		refreshPanel();
	}

	public void startItemPickForTrigger(TriggerConfig trig, Runnable cb)
	{
		pickingItemForTrigger = trig;
		pickingCallback = cb;
		refreshPanel();
	}

	public void endItemPick()
	{
		pickingItemForTrigger = null;
		Runnable cb = pickingCallback;
		pickingCallback = null;
		if (cb != null) cb.run();
		refreshPanel();
	}

	public boolean isPickingItemTrigger(TriggerConfig trig)
	{
		return pickingItemForTrigger == trig;
	}

	public void addMarker(ScreenMarker m)
	{
		m.expanded = true; // auto-expand new markers for configuration
		markers.add(m);
		save();
		refreshPanel();
	}

	public void deleteMarker(ScreenMarker m)
	{
		markers.remove(m);
		save();
		refreshPanel();
	}

	public void notifyMarkerChanged()
	{
		save();
	}

	public void refreshPanel()
	{
		if (panel != null) panel.rebuild();
	}

	private void load()
	{
		String groupsJson = configManager.getConfiguration(ScreenMarkersReloadedConfig.GROUP, ScreenMarkersReloadedConfig.GROUPS_KEY);
		groups.clear();
		if (groupsJson != null && !groupsJson.isEmpty())
		{
			try
			{
				Type gt = new TypeToken<List<MarkerGroup>>() {}.getType();
				List<MarkerGroup> loaded = gson.fromJson(groupsJson, gt);
				if (loaded != null) groups.addAll(loaded);
			}
			catch (Exception e)
			{
				log.warn("Failed to load groups", e);
			}
		}

		String json = configManager.getConfiguration(ScreenMarkersReloadedConfig.GROUP, ScreenMarkersReloadedConfig.MARKERS_KEY);
		markers.clear();
		if (json == null || json.isEmpty()) return;
		try
		{
			Type t = new TypeToken<List<ScreenMarker>>() {}.getType();
			List<ScreenMarker> loaded = gson.fromJson(json, t);
			if (loaded != null)
			{
				for (ScreenMarker m : loaded)
				{
					// Migrate legacy single-trigger saves
					if ((m.triggers == null || m.triggers.isEmpty()) && m.trigger != null)
					{
						m.triggers = new ArrayList<>();
						m.triggers.add(m.trigger);
					}
					if (m.triggers == null) m.triggers = new ArrayList<>();
					if (m.triggers.isEmpty()) m.triggers.add(new TriggerConfig());
					if (m.combine == null) m.combine = ScreenMarker.Combine.ALL;
					m.trigger = null; // drop legacy reference
				}
				markers.addAll(loaded);
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to load markers", e);
		}
	}

	private void save()
	{
		configManager.setConfiguration(
			ScreenMarkersReloadedConfig.GROUP,
			ScreenMarkersReloadedConfig.MARKERS_KEY,
			gson.toJson(markers));
		configManager.setConfiguration(
			ScreenMarkersReloadedConfig.GROUP,
			ScreenMarkersReloadedConfig.GROUPS_KEY,
			gson.toJson(groups));
	}

	@Provides
	ScreenMarkersReloadedConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(ScreenMarkersReloadedConfig.class);
	}
}
