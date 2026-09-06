package com.alexandev.screenmarkersreloaded;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;

public class MarkerRow extends JPanel
{
	private final ScreenMarker marker;
	private final ScreenMarkersReloadedPlugin plugin;
	private final JPanel triggersContainer;
	private final JPanel body;

	public MarkerRow(ScreenMarker marker, ScreenMarkersReloadedPlugin plugin)
	{
		this.marker = marker;
		this.plugin = plugin;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		triggersContainer = new JPanel();
		triggersContainer.setLayout(new BoxLayout(triggersContainer, BoxLayout.Y_AXIS));
		triggersContainer.setBackground(getBackground());

		body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(getBackground());
		body.add(Box.createVerticalStrut(4));
		body.add(buildGroupRow());
		body.add(Box.createVerticalStrut(6));
		body.add(buildTriggersHeader());
		body.add(triggersContainer);
		body.add(buildAddTriggerRow());
		body.add(Box.createVerticalStrut(6));
		body.add(buildColorRow());

		add(buildTopRow());
		add(body);
		attachContextMenu(this);
		body.setVisible(marker.expanded);
		rebuildTriggers();
	}

	private JPanel buildTopRow()
	{
		JPanel p = new JPanel(new BorderLayout(6, 0));
		p.setBackground(getBackground());

		JPanel left = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
		left.setBackground(getBackground());

		JButton arrow = new JButton(marker.expanded ? "▼" : "▶");
		arrow.setFocusPainted(false);
		arrow.setBackground(getBackground());
		arrow.setForeground(Color.WHITE);
		arrow.setBorder(BorderFactory.createEmptyBorder());
		arrow.setPreferredSize(new Dimension(22, 22));
		arrow.setToolTipText("Expand / collapse");
		Runnable toggle = () -> {
			marker.expanded = !marker.expanded;
			body.setVisible(marker.expanded);
			arrow.setText(marker.expanded ? "▼" : "▶");
			plugin.notifyMarkerChanged();
			revalidate();
			repaint();
		};
		arrow.addActionListener(e -> toggle.run());

		JCheckBox enabled = new JCheckBox();
		enabled.setSelected(marker.enabled);
		enabled.setBackground(getBackground());
		enabled.setToolTipText("Enabled");
		enabled.addActionListener(e -> {
			marker.enabled = enabled.isSelected();
			plugin.notifyMarkerChanged();
		});

		left.add(arrow);
		left.add(enabled);

		JTextField name = new JTextField(marker.name);
		name.setBackground(ColorScheme.DARK_GRAY_COLOR);
		name.setForeground(Color.WHITE);
		name.setToolTipText("Double-click to expand / collapse");
		name.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override public void mouseClicked(java.awt.event.MouseEvent e)
			{
				if (e.getClickCount() >= 2) toggle.run();
			}
		});
		name.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
			private void update()
			{
				marker.name = name.getText();
				plugin.notifyMarkerChanged();
			}
		});

		JButton delete = new JButton("✕");
		delete.setToolTipText("Delete marker");
		delete.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		delete.setForeground(new Color(220, 80, 80));
		delete.setFocusPainted(false);
		delete.setPreferredSize(new Dimension(28, 24));
		delete.addActionListener(e -> plugin.deleteMarker(marker));

		p.add(left, BorderLayout.WEST);
		p.add(name, BorderLayout.CENTER);
		p.add(delete, BorderLayout.EAST);
		return p;
	}

	private void attachContextMenu(java.awt.Component c)
	{
		c.addMouseListener(new MouseAdapter()
		{
			@Override public void mousePressed(MouseEvent e) { maybe(e); }
			@Override public void mouseReleased(MouseEvent e) { maybe(e); }
			private void maybe(MouseEvent e)
			{
				if (!e.isPopupTrigger()) return;
				JPopupMenu pop = new JPopupMenu();

				JMenuItem none = new JMenuItem("Move to (none)");
				none.addActionListener(ev -> {
					marker.groupId = null;
					plugin.notifyMarkerChanged();
					plugin.refreshPanel();
				});
				pop.add(none);

				java.util.List<MarkerGroup> groups = plugin.getGroups();
				if (!groups.isEmpty())
				{
					pop.addSeparator();
					for (MarkerGroup g : groups)
					{
						JMenuItem item = new JMenuItem("Move to: " + g.name);
						item.addActionListener(ev -> {
							marker.groupId = g.id;
							plugin.notifyMarkerChanged();
							plugin.refreshPanel();
						});
						pop.add(item);
					}
				}

				pop.addSeparator();
				String editLabel;
				switch (marker.kind)
				{
					case TILE: editLabel = "Re-pick tile"; break;
					case OBJECT_TYPE: editLabel = "Re-pick object"; break;
					case SCREEN_RECT:
					default: editLabel = "Redraw rectangle"; break;
				}
				JMenuItem edit = new JMenuItem(editLabel);
				edit.addActionListener(ev -> plugin.startEditMode(marker));
				pop.add(edit);

				pop.addSeparator();
				JMenuItem del = new JMenuItem("Delete marker");
				del.addActionListener(ev -> plugin.deleteMarker(marker));
				pop.add(del);

				pop.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}

	private JPanel buildGroupRow()
	{
		JPanel p = new JPanel(new BorderLayout(6, 0));
		p.setBackground(getBackground());
		JLabel lbl = new JLabel("Group");
		lbl.setForeground(Color.LIGHT_GRAY);

		java.util.List<MarkerGroup> groups = plugin.getGroups();
		String[] opts = new String[groups.size() + 1];
		opts[0] = "(none)";
		for (int i = 0; i < groups.size(); i++) opts[i + 1] = groups.get(i).name;

		JComboBox<String> combo = new JComboBox<>(opts);
		combo.setBackground(ColorScheme.DARK_GRAY_COLOR);
		combo.setForeground(Color.WHITE);
		int sel = 0;
		if (marker.groupId != null)
		{
			for (int i = 0; i < groups.size(); i++)
			{
				if (groups.get(i).id.equals(marker.groupId)) { sel = i + 1; break; }
			}
		}
		combo.setSelectedIndex(sel);
		combo.addActionListener(e -> {
			int idx = combo.getSelectedIndex();
			marker.groupId = idx == 0 ? null : groups.get(idx - 1).id;
			plugin.notifyMarkerChanged();
			plugin.refreshPanel();
		});

		p.add(lbl, BorderLayout.WEST);
		p.add(combo, BorderLayout.CENTER);
		return p;
	}

	private JPanel buildTriggersHeader()
	{
		JPanel p = new JPanel(new BorderLayout(6, 0));
		p.setBackground(getBackground());
		JLabel lbl = new JLabel("Triggers");
		lbl.setForeground(Color.LIGHT_GRAY);

		JComboBox<ScreenMarker.Combine> combine = new JComboBox<>(ScreenMarker.Combine.values());
		combine.setSelectedItem(marker.combine == null ? ScreenMarker.Combine.ALL : marker.combine);
		combine.setBackground(ColorScheme.DARK_GRAY_COLOR);
		combine.setForeground(Color.WHITE);
		combine.setPreferredSize(new Dimension(70, 22));
		combine.setToolTipText("ALL = every trigger must match. ANY = at least one.");
		combine.addActionListener(e -> {
			marker.combine = (ScreenMarker.Combine) combine.getSelectedItem();
			plugin.notifyMarkerChanged();
		});

		p.add(lbl, BorderLayout.WEST);
		p.add(combine, BorderLayout.EAST);
		return p;
	}

	private JPanel buildAddTriggerRow()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.setBackground(getBackground());
		JButton add = new JButton("+ Add trigger");
		add.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add.setForeground(Color.WHITE);
		add.setFocusPainted(false);
		add.setPreferredSize(new Dimension(120, 22));
		add.addActionListener(e -> {
			marker.triggers.add(new TriggerConfig());
			plugin.notifyMarkerChanged();
			rebuildTriggers();
		});
		p.add(add);
		return p;
	}

	private void rebuildTriggers()
	{
		triggersContainer.removeAll();
		for (TriggerConfig tc : marker.triggers)
		{
			TriggerRow row = new TriggerRow(marker, tc, plugin, this::rebuildTriggers);
			triggersContainer.add(row);
			triggersContainer.add(Box.createVerticalStrut(4));
		}
		triggersContainer.revalidate();
		triggersContainer.repaint();
	}

	private JPanel buildColorRow()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		p.setBackground(getBackground());

		JLabel borderLbl = new JLabel("Border");
		borderLbl.setForeground(Color.LIGHT_GRAY);
		p.add(borderLbl);
		p.add(colorSwatch(new Color(marker.borderColor, true), c -> {
			marker.borderColor = c.getRGB();
			plugin.notifyMarkerChanged();
		}));

		JLabel fillLbl = new JLabel("Fill");
		fillLbl.setForeground(Color.LIGHT_GRAY);
		p.add(fillLbl);
		p.add(colorSwatch(new Color(marker.fillColor, true), c -> {
			marker.fillColor = c.getRGB();
			plugin.notifyMarkerChanged();
		}));

		JCheckBox showLabel = new JCheckBox("Label");
		showLabel.setSelected(marker.labelVisible);
		showLabel.setBackground(getBackground());
		showLabel.setForeground(Color.LIGHT_GRAY);
		showLabel.addActionListener(e -> {
			marker.labelVisible = showLabel.isSelected();
			plugin.notifyMarkerChanged();
		});
		p.add(showLabel);

		return p;
	}

	private JButton colorSwatch(Color initial, java.util.function.Consumer<Color> onChange)
	{
		JButton b = new JButton();
		b.setBackground(initial);
		b.setFocusPainted(false);
		b.setPreferredSize(new Dimension(22, 22));
		b.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		b.setToolTipText("Click to change color");
		b.addActionListener(e -> {
			Color picked = JColorChooser.showDialog(b, "Pick color", b.getBackground());
			if (picked != null)
			{
				Color withAlpha = new Color(picked.getRed(), picked.getGreen(), picked.getBlue(), initial.getAlpha());
				b.setBackground(withAlpha);
				onChange.accept(withAlpha);
			}
		});
		return b;
	}
}
