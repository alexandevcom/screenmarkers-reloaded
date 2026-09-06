package com.alexandev.screenmarkersreloaded;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class CreationOverlay extends Overlay
{
	private final ScreenMarkersReloadedPlugin plugin;

	@Inject
	CreationOverlay(ScreenMarkersReloadedPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		MarkerKind kind = plugin.getCreationKind();
		if (kind == null) return null;

		boolean editing = plugin.getEditingMarker() != null;
		String hint;
		switch (kind)
		{
			case SCREEN_RECT:
				hint = (editing ? "Drag to redraw rectangle." : "Drag to draw a rectangle.") + " ESC to cancel.";
				break;
			case TILE:
				hint = (editing ? "Click a tile to move marker." : "Click a tile to mark it.") + " ESC to cancel.";
				break;
			case OBJECT_TYPE:
				hint = (editing ? "Click an object to re-pick." : "Click an object to mark all of that type.") + " ESC to cancel.";
				break;
			case INVENTORY_ITEM:
				hint = (editing ? "Click an inventory item to re-pick." : "Click an inventory item to mark it.") + " ESC to cancel.";
				break;
			case WIDGET:
				hint = (editing ? "Click a widget (spell/prayer/button) to re-pick." : "Click a widget (spell/prayer/button) to mark it.") + " ESC to cancel.";
				break;
			case GROUND_ITEM:
				hint = (editing ? "Click a ground item (or inventory item) to re-pick." : "Click a ground item (or inventory item) to mark it.") + " ESC to cancel.";
				break;
			default:
				hint = "ESC to cancel";
		}
		g.setColor(new Color(0, 0, 0, 200));
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(hint) + 16;
		g.fillRect(10, 10, w, 22);
		g.setColor(Color.WHITE);
		g.drawString(hint, 18, 26);

		if (kind == MarkerKind.SCREEN_RECT)
		{
			Point start = plugin.getDragStart();
			Point end = plugin.getDragEnd();
			if (start != null && end != null)
			{
				Rectangle r = rect(start, end);
				g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
					1f, new float[]{5f, 4f}, 0f));
				g.setColor(new Color(0, 255, 120, 240));
				g.drawRect(r.x, r.y, r.width, r.height);
				g.setColor(new Color(0, 255, 120, 60));
				g.fillRect(r.x, r.y, r.width, r.height);

				String size = r.width + "×" + r.height;
				g.setColor(new Color(0, 0, 0, 200));
				g.fillRect(r.x, r.y - 16, fm.stringWidth(size) + 8, 16);
				g.setColor(Color.WHITE);
				g.drawString(size, r.x + 4, r.y - 3);
			}
		}
		return null;
	}

	static Rectangle rect(Point a, Point b)
	{
		int x = Math.min(a.x, b.x);
		int y = Math.min(a.y, b.y);
		int w = Math.abs(a.x - b.x);
		int h = Math.abs(a.y - b.y);
		return new Rectangle(x, y, w, h);
	}
}
