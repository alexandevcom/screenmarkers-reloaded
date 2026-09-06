package com.alexandev.screenmarkersreloaded;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.VarClientInt;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class ScreenMarkersReloadedOverlay extends Overlay
{
	private final Client client;
	private final ScreenMarkersReloadedPlugin plugin;

	@Inject
	ScreenMarkersReloadedOverlay(Client client, ScreenMarkersReloadedPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		for (ScreenMarker m : plugin.getMarkers())
		{
			if (!plugin.isMarkerActive(m)) continue;
			if (!combineActive(m)) continue;
			drawMarker(g, m);
		}
		return null;
	}

	private boolean combineActive(ScreenMarker m)
	{
		if (m.triggers == null || m.triggers.isEmpty()) return true;
		boolean all = m.combine != ScreenMarker.Combine.ANY;
		for (TriggerConfig t : m.triggers)
		{
			boolean active = isTriggerActive(t);
			if (t.inverted) active = !active;
			if (all && !active) return false;
			if (!all && active) return true;
		}
		return all;
	}

	private void drawMarker(Graphics2D g, ScreenMarker m)
	{
		Color border = new Color(m.borderColor, true);
		Color fill = new Color(m.fillColor, true);
		g.setStroke(new BasicStroke(Math.max(1, m.borderThickness)));

		switch (m.kind)
		{
			case SCREEN_RECT:
				drawScreenRect(g, m, border, fill);
				break;
			case TILE:
				drawTile(g, m, border, fill);
				break;
			case OBJECT_TYPE:
				drawObjectType(g, m, border, fill);
				break;
			case INVENTORY_ITEM:
				drawInventoryItem(g, m, border, fill);
				break;
			case WIDGET:
				drawWidget(g, m, border, fill);
				break;
			case GROUND_ITEM:
				drawGroundItem(g, m, border, fill);
				break;
		}
	}

	private void drawGroundItem(Graphics2D g, ScreenMarker m, Color border, Color fill)
	{
		if (m.itemId <= 0) return;
		for (Tile tile : plugin.getGroundTilesForMarker(m))
		{
			if (tile == null) continue;
			Polygon poly = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());
			if (poly == null) continue;
			g.setColor(fill);
			g.fillPolygon(poly);
			g.setColor(border);
			g.drawPolygon(poly);
			if (m.labelVisible && m.name != null && !m.name.isEmpty())
			{
				Rectangle b = poly.getBounds();
				drawLabel(g, m, b.x + b.width / 2, b.y + b.height / 2);
			}
		}
	}

	private void drawWidget(Graphics2D g, ScreenMarker m, Color border, Color fill)
	{
		if (m.widgetPackedId < 0) return;
		Widget w = client.getWidget(m.widgetPackedId);
		if (w == null || w.isHidden()) return;
		java.awt.Rectangle b = w.getBounds();
		if (b == null || b.width <= 0 || b.height <= 0) return;
		g.setColor(fill);
		g.fillRect(b.x, b.y, b.width, b.height);
		g.setColor(border);
		g.drawRect(b.x, b.y, b.width, b.height);
		if (m.labelVisible && m.name != null && !m.name.isEmpty())
		{
			drawLabel(g, m, b.x + b.width / 2, b.y + b.height / 2);
		}
	}

	private void drawScreenRect(Graphics2D g, ScreenMarker m, Color border, Color fill)
	{
		g.setColor(fill);
		g.fillRect(m.x + 1, m.y + 1, Math.max(0, m.width - 1), Math.max(0, m.height - 1));
		g.setColor(border);
		g.drawRect(m.x, m.y, m.width, m.height);
		drawLabel(g, m, m.x + m.width / 2, m.y + m.height / 2);
	}

	private void drawTile(Graphics2D g, ScreenMarker m, Color border, Color fill)
	{
		WorldPoint wp = new WorldPoint(m.tileX, m.tileY, m.tilePlane);
		LocalPoint lp = LocalPoint.fromWorld(client, wp);
		if (lp == null) return;
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null) return;
		g.setColor(fill);
		g.fillPolygon(poly);
		g.setColor(border);
		g.drawPolygon(poly);
		if (m.labelVisible && m.name != null && !m.name.isEmpty())
		{
			Rectangle b = poly.getBounds();
			drawLabel(g, m, b.x + b.width / 2, b.y + b.height / 2);
		}
	}

	private void drawObjectType(Graphics2D g, ScreenMarker m, Color border, Color fill)
	{
		for (GameObject obj : plugin.getObjectsForMarker(m))
		{
			Shape hull = obj.getConvexHull();
			if (hull == null) continue;
			g.setColor(border);
			g.draw(hull);
			g.setColor(fill);
			g.fill(hull);
			if (m.labelVisible && m.name != null && !m.name.isEmpty())
			{
				Rectangle b = hull.getBounds();
				drawLabel(g, m, b.x + b.width / 2, b.y + b.height / 2);
			}
		}
	}

	private void drawInventoryItem(Graphics2D g, ScreenMarker m, Color border, Color fill)
	{
		if (m.itemId <= 0) return;
		drawItemInWidget(g, m, border, fill, client.getWidget(InterfaceID.INVENTORY, 0));
		Widget bank = client.getWidget(InterfaceID.BANKMAIN, 0);
		if (bank != null && !bank.isHidden())
		{
			drawItemInTree(g, m, border, fill, bank);
		}
	}

	private void drawItemInWidget(Graphics2D g, ScreenMarker m, Color border, Color fill, Widget w)
	{
		if (w == null || w.isHidden()) return;
		Widget[] children = w.getDynamicChildren();
		if (children == null) return;
		for (Widget slot : children)
		{
			drawIfMatch(g, m, border, fill, slot);
		}
	}

	private void drawItemInTree(Graphics2D g, ScreenMarker m, Color border, Color fill, Widget root)
	{
		if (root == null || root.isHidden()) return;
		drawIfMatch(g, m, border, fill, root);
		Widget[] dyn = root.getDynamicChildren();
		if (dyn != null) for (Widget c : dyn) drawItemInTree(g, m, border, fill, c);
		Widget[] stat = root.getStaticChildren();
		if (stat != null) for (Widget c : stat) drawItemInTree(g, m, border, fill, c);
		Widget[] nest = root.getNestedChildren();
		if (nest != null) for (Widget c : nest) drawItemInTree(g, m, border, fill, c);
	}

	private void drawIfMatch(Graphics2D g, ScreenMarker m, Color border, Color fill, Widget slot)
	{
		if (slot == null || slot.getItemId() != m.itemId) return;
		java.awt.Rectangle b = slot.getBounds();
		if (b == null || b.width <= 0 || b.height <= 0) return;
		g.setColor(fill);
		g.fillRect(b.x, b.y, b.width, b.height);
		g.setColor(border);
		g.drawRect(b.x, b.y, b.width, b.height);
		if (m.labelVisible && m.name != null && !m.name.isEmpty())
		{
			drawLabel(g, m, b.x + b.width / 2, b.y + b.height / 2);
		}
	}

	private void drawLabel(Graphics2D g, ScreenMarker m, int cx, int cy)
	{
		if (!m.labelVisible || m.name == null || m.name.isEmpty()) return;
		FontMetrics fm = g.getFontMetrics();
		int tx = cx - fm.stringWidth(m.name) / 2;
		int ty = cy + fm.getAscent() / 2 - 1;
		g.setColor(new Color(0, 0, 0, 180));
		g.drawString(m.name, tx + 1, ty + 1);
		g.setColor(Color.WHITE);
		g.drawString(m.name, tx, ty);
	}

	private boolean isTriggerActive(TriggerConfig t)
	{
		if (t == null) return false;
		switch (t.kind)
		{
			case ALWAYS:
				return true;
			case BANK_OPEN:
				Widget bank = client.getWidget(InterfaceID.BANKMAIN, 0);
				return bank != null && !bank.isHidden();
			case ANIMATION_ACTIVE:
				if (client.getLocalPlayer() == null) return false;
				int anim = client.getLocalPlayer().getAnimation();
				if (t.animationId < 0) return anim != -1;
				return anim == t.animationId;
			case INVENTORY_FULL:
				return countInventory(true) >= 28;
			case INVENTORY_EMPTY:
				return countInventory(true) == 0;
			case INVENTORY_HAS_ITEM:
				if (t.itemId <= 0) return false;
				int have = itemCount(t.itemId);
				int need = Math.max(0, t.itemCount);
				TriggerConfig.CountOp op = t.itemCountOp == null ? TriggerConfig.CountOp.AT_LEAST : t.itemCountOp;
				switch (op)
				{
					case AT_MOST: return have <= need;
					case EQUAL: return have == need;
					case AT_LEAST:
					default: return have >= need;
				}
			case TAB_ACTIVE:
				if (t.tab == null) return false;
				int current = client.getVarcIntValue(VarClientInt.INVENTORY_TAB);
				return current == t.tab.index;
			default:
				return false;
		}
	}

	private int countInventory(boolean usedSlots)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null) return 0;
		int used = 0;
		for (Item it : inv.getItems())
		{
			if (it.getId() > 0) used++;
		}
		return usedSlots ? used : 28 - used;
	}

	private int itemCount(int id)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null) return 0;
		int total = 0;
		for (Item it : inv.getItems())
		{
			if (it.getId() == id) total += it.getQuantity();
		}
		return total;
	}
}
