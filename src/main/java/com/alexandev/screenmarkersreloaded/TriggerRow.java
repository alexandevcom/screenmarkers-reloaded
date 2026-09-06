package com.alexandev.screenmarkersreloaded;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

public class TriggerRow extends JPanel
{
	private final ScreenMarker marker;
	private final TriggerConfig config;
	private final ScreenMarkersReloadedPlugin plugin;
	private final Runnable onChanged;

	public TriggerRow(ScreenMarker marker, TriggerConfig config,
		ScreenMarkersReloadedPlugin plugin, Runnable onChanged)
	{
		this.marker = marker;
		this.config = config;
		this.plugin = plugin;
		this.onChanged = onChanged;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		add(buildKindRow());
		add(buildParams());
	}

	private JPanel buildKindRow()
	{
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBackground(getBackground());

		javax.swing.JCheckBox notBox = new javax.swing.JCheckBox();
		notBox.setSelected(config.inverted);
		notBox.setBackground(getBackground());
		notBox.setToolTipText("Invert this trigger (NOT)");
		notBox.setPreferredSize(new Dimension(22, 22));
		notBox.addActionListener(e -> {
			config.inverted = notBox.isSelected();
			plugin.notifyMarkerChanged();
		});

		JComboBox<TriggerKind> kind = new JComboBox<>(TriggerKind.values());
		kind.setSelectedItem(config.kind);
		kind.setBackground(ColorScheme.DARK_GRAY_COLOR);
		kind.setForeground(Color.WHITE);
		kind.addActionListener(e -> {
			config.kind = (TriggerKind) kind.getSelectedItem();
			plugin.notifyMarkerChanged();
			onChanged.run();
		});

		JButton del = new JButton("✕");
		del.setToolTipText("Remove trigger");
		del.setForeground(new Color(220, 80, 80));
		del.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		del.setFocusPainted(false);
		del.setPreferredSize(new Dimension(28, 22));
		del.addActionListener(e -> {
			marker.triggers.remove(config);
			if (marker.triggers.isEmpty()) marker.triggers.add(new TriggerConfig());
			plugin.notifyMarkerChanged();
			onChanged.run();
		});

		p.add(notBox, BorderLayout.WEST);
		p.add(kind, BorderLayout.CENTER);
		p.add(del, BorderLayout.EAST);
		return p;
	}

	private JPanel buildParams()
	{
		JPanel wrap = new JPanel(new GridLayout(0, 1, 0, 2));
		wrap.setBackground(getBackground());

		if (config.kind == TriggerKind.TAB_ACTIVE)
		{
			JPanel p = new JPanel(new java.awt.BorderLayout(4, 0));
			p.setBackground(getBackground());
			JLabel lbl = new JLabel("Tab ");
			lbl.setForeground(Color.LIGHT_GRAY);
			JComboBox<SidePanelTab> combo = new JComboBox<>(SidePanelTab.values());
			combo.setSelectedItem(config.tab == null ? SidePanelTab.INVENTORY : config.tab);
			combo.setBackground(ColorScheme.DARK_GRAY_COLOR);
			combo.setForeground(Color.WHITE);
			combo.addActionListener(e -> {
				config.tab = (SidePanelTab) combo.getSelectedItem();
				plugin.notifyMarkerChanged();
			});
			p.add(lbl, java.awt.BorderLayout.WEST);
			p.add(combo, java.awt.BorderLayout.CENTER);
			wrap.add(p);
		}
		else if (config.kind == TriggerKind.ANIMATION_ACTIVE)
		{
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
			p.setBackground(getBackground());
			JLabel lbl = new JLabel("Anim ID");
			lbl.setForeground(Color.LIGHT_GRAY);
			JSpinner spin = new JSpinner(new SpinnerNumberModel(config.animationId, -1, 20000, 1));
			spin.addChangeListener(e -> {
				config.animationId = (int) spin.getValue();
				plugin.notifyMarkerChanged();
			});
			p.add(lbl);
			p.add(spin);
			wrap.add(p);
		}
		else if (config.kind == TriggerKind.INVENTORY_HAS_ITEM)
		{
			JPanel itemRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
			itemRow.setBackground(getBackground());

			JLabel sprite = new JLabel();
			sprite.setPreferredSize(new Dimension(32, 28));
			JLabel name = new JLabel();
			name.setForeground(Color.LIGHT_GRAY);
			updateItemDisplay(sprite, name);

			JButton pickBtn = new JButton(plugin.isPickingItemTrigger(config) ? "Cancel" : "Pick");
			pickBtn.setFocusPainted(false);
			pickBtn.setPreferredSize(new Dimension(54, 22));
			pickBtn.addActionListener(e -> {
				if (plugin.isPickingItemTrigger(config)) plugin.endItemPick();
				else plugin.startItemPickForTrigger(config, onChanged);
			});

			itemRow.add(sprite);
			itemRow.add(name);
			itemRow.add(pickBtn);
			wrap.add(itemRow);

			JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
			countRow.setBackground(getBackground());

			JComboBox<TriggerConfig.CountOp> opCombo = new JComboBox<>(TriggerConfig.CountOp.values());
			opCombo.setSelectedItem(config.itemCountOp == null ? TriggerConfig.CountOp.AT_LEAST : config.itemCountOp);
			opCombo.setBackground(ColorScheme.DARK_GRAY_COLOR);
			opCombo.setForeground(Color.WHITE);
			opCombo.setPreferredSize(new Dimension(54, 22));
			opCombo.addActionListener(e -> {
				config.itemCountOp = (TriggerConfig.CountOp) opCombo.getSelectedItem();
				plugin.notifyMarkerChanged();
			});

			JSpinner countSpin = new JSpinner(new SpinnerNumberModel(Math.max(config.itemCount, 0), 0, 2147483647, 1));
			countSpin.setPreferredSize(new Dimension(70, 22));
			countSpin.addChangeListener(e -> {
				config.itemCount = (int) countSpin.getValue();
				plugin.notifyMarkerChanged();
			});

			countRow.add(new JLabel("Count")).setForeground(Color.LIGHT_GRAY);
			countRow.add(opCombo);
			countRow.add(countSpin);
			wrap.add(countRow);
		}
		return wrap;
	}

	private void updateItemDisplay(JLabel sprite, JLabel name)
	{
		int id = config.itemId;
		if (id <= 0)
		{
			sprite.setIcon(null);
			sprite.setText("—");
			sprite.setForeground(Color.GRAY);
			name.setText("(no item)");
			return;
		}
		sprite.setText(null);
		name.setText("…");
		AsyncBufferedImage img = plugin.getItemManager().getImage(id);
		if (img != null)
		{
			sprite.setIcon(new ImageIcon(img));
			img.onLoaded(() -> sprite.setIcon(new ImageIcon(img)));
		}
		plugin.getClientThread().invokeLater(() -> {
			String n;
			try
			{
				n = plugin.getItemManager().getItemComposition(id).getName();
			}
			catch (Exception ex)
			{
				n = "#" + id;
			}
			final String resolved = n == null ? ("#" + id) : n;
			javax.swing.SwingUtilities.invokeLater(() -> name.setText(resolved));
		});
	}
}
