package com.alexandev.screenmarkersreloaded;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class ScreenMarkersReloadedPanel extends PluginPanel
{
	private final ScreenMarkersReloadedPlugin plugin;
	private final JPanel listHolder;
	private final JButton screenBtn;
	private final JButton tileBtn;
	private final JButton objectBtn;

	public ScreenMarkersReloadedPanel(ScreenMarkersReloadedPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(getBackground());

		JLabel title = new JLabel("Screen Markers: Reloaded");
		title.setForeground(Color.WHITE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
		title.setAlignmentX(LEFT_ALIGNMENT);
		top.add(title);
		top.add(Box.createVerticalStrut(8));

		screenBtn = mkCreateBtn("+ Screen", MarkerKind.SCREEN_RECT);
		tileBtn = mkCreateBtn("+ Tile", MarkerKind.TILE);
		objectBtn = mkCreateBtn("+ Object", MarkerKind.OBJECT_TYPE);

		JButton newMarkerBtn = new JButton("+ New marker ▾");
		styleButton(newMarkerBtn, new Color(40, 110, 50));
		newMarkerBtn.setPreferredSize(new Dimension(200, 28));
		newMarkerBtn.addActionListener(e -> {
			javax.swing.JPopupMenu pop = new javax.swing.JPopupMenu();
			javax.swing.JMenuItem screen = new javax.swing.JMenuItem("Screen rectangle");
			screen.addActionListener(ev -> onCreateClick(MarkerKind.SCREEN_RECT));
			javax.swing.JMenuItem tile = new javax.swing.JMenuItem("Tile");
			tile.addActionListener(ev -> onCreateClick(MarkerKind.TILE));
			javax.swing.JMenuItem object = new javax.swing.JMenuItem("Object type");
			object.addActionListener(ev -> onCreateClick(MarkerKind.OBJECT_TYPE));
			javax.swing.JMenuItem item = new javax.swing.JMenuItem("Inventory item");
			item.addActionListener(ev -> onCreateClick(MarkerKind.INVENTORY_ITEM));
			javax.swing.JMenuItem widget = new javax.swing.JMenuItem("Widget / spell");
			widget.addActionListener(ev -> onCreateClick(MarkerKind.WIDGET));
			javax.swing.JMenuItem ground = new javax.swing.JMenuItem("Ground item");
			ground.addActionListener(ev -> onCreateClick(MarkerKind.GROUND_ITEM));
			pop.add(screen);
			pop.add(tile);
			pop.add(object);
			pop.add(item);
			pop.add(widget);
			pop.add(ground);
			pop.show(newMarkerBtn, 0, newMarkerBtn.getHeight());
		});
		JPanel newMarkerRow = new JPanel(new BorderLayout());
		newMarkerRow.setBackground(getBackground());
		newMarkerRow.setAlignmentX(LEFT_ALIGNMENT);
		newMarkerRow.add(newMarkerBtn, BorderLayout.CENTER);
		top.add(newMarkerRow);

		top.add(Box.createVerticalStrut(6));

		JPanel groupBtns = new JPanel(new GridLayout(1, 2, 4, 0));
		groupBtns.setBackground(getBackground());
		groupBtns.setAlignmentX(LEFT_ALIGNMENT);
		JButton newGroupBtn = new JButton("+ Group");
		styleButton(newGroupBtn, new Color(50, 80, 120));
		newGroupBtn.addActionListener(e -> plugin.addGroup());
		JButton importBtn = new JButton("Import");
		styleButton(importBtn, new Color(80, 60, 110));
		importBtn.addActionListener(e -> onImport());
		groupBtns.add(newGroupBtn);
		groupBtns.add(importBtn);
		top.add(groupBtns);

		listHolder = new JPanel();
		listHolder.setLayout(new BoxLayout(listHolder, BoxLayout.Y_AXIS));
		listHolder.setBackground(getBackground());
		listHolder.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

		add(top, BorderLayout.NORTH);
		add(listHolder, BorderLayout.CENTER);

		rebuild();
	}

	private JButton mkCreateBtn(String text, MarkerKind kind)
	{
		JButton b = new JButton(text);
		b.setFocusPainted(false);
		b.setPreferredSize(new Dimension(60, 28));
		b.setForeground(Color.WHITE);
		b.setBackground(new Color(40, 110, 50));
		b.addActionListener(e -> onCreateClick(kind));
		return b;
	}

	private void styleButton(JButton b, Color bg)
	{
		b.setBackground(bg);
		b.setForeground(Color.WHITE);
		b.setFocusPainted(false);
		b.setPreferredSize(new Dimension(100, 24));
	}

	private void onCreateClick(MarkerKind kind)
	{
		if (plugin.getCreationKind() == kind) plugin.endCreationMode();
		else plugin.startCreationMode(kind);
	}

	private void onImport()
	{
		try
		{
			String json = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
				.getData(DataFlavor.stringFlavor);
			if (json == null || json.isBlank())
			{
				JOptionPane.showMessageDialog(this, "Clipboard is empty");
				return;
			}
			boolean ok = plugin.importGroupJson(json);
			JOptionPane.showMessageDialog(this, ok ? "Imported" : "Invalid group JSON");
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Could not read clipboard: " + ex.getMessage());
		}
	}

	public void rebuild()
	{
		listHolder.removeAll();

		// Groups
		for (MarkerGroup g : plugin.getGroups())
		{
			GroupRow row = new GroupRow(g, plugin);
			row.setAlignmentX(LEFT_ALIGNMENT);
			listHolder.add(row);
			listHolder.add(Box.createVerticalStrut(6));
		}

		// Ungrouped markers
		boolean hasUngrouped = false;
		for (ScreenMarker m : plugin.getMarkers())
		{
			if (m.groupId == null || plugin.getGroup(m.groupId) == null)
			{
				if (!hasUngrouped)
				{
					JLabel header = new JLabel("Ungrouped");
					header.setForeground(Color.LIGHT_GRAY);
					header.setAlignmentX(LEFT_ALIGNMENT);
					header.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
					listHolder.add(header);
					hasUngrouped = true;
				}
				MarkerRow mr = new MarkerRow(m, plugin);
				mr.setAlignmentX(LEFT_ALIGNMENT);
				listHolder.add(mr);
				listHolder.add(Box.createVerticalStrut(6));
			}
		}

		if (plugin.getGroups().isEmpty() && plugin.getMarkers().isEmpty())
		{
			JLabel empty = new JLabel("<html><div style='text-align:center;color:#888;padding:20px 0;'>No markers yet.<br>Use <b>Screen</b>, <b>Tile</b>, or <b>Object</b> to create one.</div></html>");
			empty.setAlignmentX(LEFT_ALIGNMENT);
			listHolder.add(empty);
		}

		updateCreateButtons();
		listHolder.revalidate();
		listHolder.repaint();
	}

	private void updateCreateButtons()
	{
		// Top button becomes "Cancel" when in creation mode — reuse screenBtn reference
	}

	private void updateCreateBtn(JButton b, String defaultText, MarkerKind kind, MarkerKind active)
	{
		if (active == kind)
		{
			b.setText("Cancel");
			b.setBackground(new Color(140, 60, 60));
		}
		else
		{
			b.setText(defaultText);
			b.setBackground(new Color(40, 110, 50));
		}
	}
}
