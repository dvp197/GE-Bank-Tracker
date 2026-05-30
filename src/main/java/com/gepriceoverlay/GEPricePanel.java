package com.gepriceoverlay;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;

public class GEPricePanel extends PluginPanel
{
	private final ItemManager itemManager;
	private final GEPriceOverlayConfig config;
	private final JPanel listPanel;

	public GEPricePanel(ItemManager itemManager, GEPriceOverlayConfig config)
	{
		this.itemManager = itemManager;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("GE Price Changes", SwingConstants.CENTER);
		title.setForeground(Color.WHITE);
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		title.setBorder(new EmptyBorder(8, 0, 8, 0));
		add(title, BorderLayout.NORTH);

		listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(listPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void update(Map<Integer, PriceData> priceCache)
	{
		SwingUtilities.invokeLater(() ->
		{
			listPanel.removeAll();

			priceCache.entrySet().stream()
				.filter(e -> e.getValue() != null && e.getValue().getChange() != 0)
				.sorted(Comparator.comparingInt((Map.Entry<Integer, PriceData> e) ->
					e.getValue().getChangePercent()).reversed())
				.forEach(e ->
				{
					int itemId = e.getKey();
					PriceData data = e.getValue();
					String name = itemManager.getItemComposition(itemId).getName();

					int change = data.getChange();
					Color changeColor = change > 0 ? new Color(0, 200, 0) : Color.RED;
					String changeText = config.displayMode() == GEPriceOverlayConfig.DisplayMode.MONEY
						? formatGold(change)
						: String.format("%s%d%%", change > 0 ? "+" : "", data.getChangePercent());

					JPanel row = new JPanel(new BorderLayout());
					row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
					row.setBorder(new EmptyBorder(4, 8, 4, 8));
					row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

					JLabel nameLabel = new JLabel(name);
					nameLabel.setForeground(Color.WHITE);
					nameLabel.setFont(nameLabel.getFont().deriveFont(12f));

					JLabel changeLabel = new JLabel(changeText);
					changeLabel.setForeground(changeColor);
					changeLabel.setFont(changeLabel.getFont().deriveFont(Font.BOLD, 12f));

					row.add(nameLabel, BorderLayout.WEST);
					row.add(changeLabel, BorderLayout.EAST);

					listPanel.add(row);
					listPanel.add(Box.createVerticalStrut(2));
				});

			if (listPanel.getComponentCount() == 0)
			{
				JLabel empty = new JLabel("Open your bank to load prices", SwingConstants.CENTER);
				empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				empty.setBorder(new EmptyBorder(20, 0, 0, 0));
				listPanel.add(empty);
			}

			listPanel.revalidate();
			listPanel.repaint();
		});
	}

	private static String formatGold(int change)
	{
		String sign = change > 0 ? "+" : "-";
		long abs = Math.abs(change);
		if (abs >= 1_000_000_000)
		{
			return sign + String.format("%.1fB", abs / 1_000_000_000.0);
		}
		if (abs >= 1_000_000)
		{
			return sign + String.format("%.1fM", abs / 1_000_000.0);
		}
		if (abs >= 1_000)
		{
			return sign + String.format("%.1fk", abs / 1_000.0);
		}
		return sign + abs;
	}
}
