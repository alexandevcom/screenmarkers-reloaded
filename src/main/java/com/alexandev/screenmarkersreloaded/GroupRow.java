package com.alexandev.screenmarkersreloaded;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;

public class GroupRow extends JPanel
{
	private final MarkerGroup group;
	private final ScreenMarkersReloadedPlugin plugin;
	private final JPanel body;

	public GroupRow(MarkerGroup group, ScreenMarkersReloadedPlugin plugin)
	{
		this.group = group;
		this.plugin = plugin;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		add(buildHeader());
		body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(getBackground());
		body.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
		add(body);
		body.setVisible(group.expanded);

		for (ScreenMarker m : plugin.getMarkers())
		{
			if (group.id.equals(m.groupId))
			{
				body.add(new MarkerRow(m, plugin));
				body.add(Box.createVerticalStrut(6));
			}
		}
	}

	private JPanel buildHeader()
	{
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		left.setBackground(p.getBackground());
		JButton arrow = new JButton(group.expanded ? "▼" : "▶");
		arrow.setFocusPainted(false);
		arrow.setBackground(p.getBackground());
		arrow.setForeground(Color.WHITE);
		arrow.setBorder(BorderFactory.createEmptyBorder());
		arrow.setPreferredSize(new Dimension(22, 22));
		arrow.addActionListener(e -> {
			group.expanded = !group.expanded;
			body.setVisible(group.expanded);
			arrow.setText(group.expanded ? "▼" : "▶");
			plugin.notifyMarkerChanged();
		});

		JCheckBox enabled = new JCheckBox();
		enabled.setSelected(group.enabled);
		enabled.setBackground(p.getBackground());
		enabled.setToolTipText("Enable group");
		enabled.addActionListener(e -> {
			group.enabled = enabled.isSelected();
			plugin.notifyMarkerChanged();
		});

		left.add(arrow);
		left.add(enabled);

		JTextField name = new JTextField(group.name);
		name.setBackground(ColorScheme.DARK_GRAY_COLOR);
		name.setForeground(Color.WHITE);
		name.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
			private void update()
			{
				group.name = name.getText();
				plugin.notifyMarkerChanged();
			}
		});

		JButton menu = new JButton("⋮");
		menu.setFocusPainted(false);
		menu.setBackground(p.getBackground());
		menu.setForeground(Color.WHITE);
		menu.setBorder(BorderFactory.createEmptyBorder());
		menu.setPreferredSize(new Dimension(22, 22));
		menu.addActionListener(e -> {
			JPopupMenu pop = new JPopupMenu();
			JMenuItem export = new JMenuItem("Export (copy to clipboard)");
			export.addActionListener(ev -> {
				String json = plugin.exportGroupJson(group);
				Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new StringSelection(json), null);
				JOptionPane.showMessageDialog(this, "Group copied to clipboard");
			});
			JMenuItem deleteOnly = new JMenuItem("Delete group (keep markers)");
			deleteOnly.addActionListener(ev -> {
				if (confirm("Delete group '" + group.name + "'? Markers will become ungrouped."))
				{
					plugin.deleteGroup(group, false);
				}
			});
			JMenuItem deleteAll = new JMenuItem("Delete group + markers");
			deleteAll.addActionListener(ev -> {
				if (confirm("Delete group '" + group.name + "' and ALL its markers?"))
				{
					plugin.deleteGroup(group, true);
				}
			});
			pop.add(export);
			pop.addSeparator();
			pop.add(deleteOnly);
			pop.add(deleteAll);
			pop.show(menu, 0, menu.getHeight());
		});

		p.add(left, BorderLayout.WEST);
		p.add(name, BorderLayout.CENTER);
		p.add(menu, BorderLayout.EAST);
		return p;
	}

	private boolean confirm(String message)
	{
		int result = JOptionPane.showConfirmDialog(this, message, "Confirm",
			JOptionPane.YES_NO_OPTION);
		return result == JOptionPane.YES_OPTION;
	}

	public JLabel unusedDummy() { return new JLabel(); }
}
