package com.gepriceoverlay;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GEPricePanel extends PluginPanel
{
	private static final int MAX_NAME_LENGTH = 18;

	private final GEPriceOverlayConfig config;
	private final JPanel listPanel;
	private JLabel timeframeLabel;
	private JLabel summaryLabel;

	public GEPricePanel(GEPriceOverlayConfig config)
	{
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("GE Bank Tracker", SwingConstants.CENTER);
		title.setForeground(Color.WHITE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
		title.setBorder(new EmptyBorder(8, 0, 2, 0));

		timeframeLabel = new JLabel("", SwingConstants.CENTER);
		timeframeLabel.setForeground(Color.YELLOW);
		timeframeLabel.setFont(timeframeLabel.getFont().deriveFont(Font.BOLD, 14f));
		timeframeLabel.setBorder(new EmptyBorder(0, 0, 4, 0));

		summaryLabel = new JLabel("", SwingConstants.CENTER);
		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryLabel.setFont(summaryLabel.getFont().deriveFont(11f));
		summaryLabel.setBorder(new EmptyBorder(0, 0, 6, 0));

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.add(title, BorderLayout.NORTH);
		header.add(timeframeLabel, BorderLayout.CENTER);
		header.add(summaryLabel, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);

		listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(listPanel, BorderLayout.CENTER);
	}

	public void update(Map<Integer, PriceData> priceCache, Map<Integer, String> nameCache, Set<Integer> pinnedItems)
	{
		SwingUtilities.invokeLater(() ->
		{
			timeframeLabel.setText(config.timeFrame().name());

			List<PriceData> tracked = priceCache.values().stream()
				.filter(d -> d != null && d.getChange() != 0)
				.collect(Collectors.toList());

			long up = tracked.stream().filter(d -> d.getChange() > 0).count();
			long down = tracked.stream().filter(d -> d.getChange() < 0).count();

			if (!tracked.isEmpty())
			{
				summaryLabel.setText(tracked.size() + " items  •  " + up + "↑  " + down + "↓");
			}
			else
			{
				summaryLabel.setText("");
			}

			listPanel.removeAll();

			boolean moneyMode = config.displayMode() == GEPriceOverlayConfig.DisplayMode.MONEY;

			priceCache.entrySet().stream()
				.filter(e -> e.getValue() != null && (e.getValue().getChange() != 0 || pinnedItems.contains(e.getKey())))
				.sorted(Comparator.comparingDouble((Map.Entry<Integer, PriceData> e) -> moneyMode
					? e.getValue().getChange()
					: e.getValue().getChangePercent()).reversed())
				.forEach(e ->
				{
					int itemId = e.getKey();
					PriceData data = e.getValue();
					boolean pinned = pinnedItems.contains(itemId);

					String rawName = nameCache.getOrDefault(itemId, "Item " + itemId);
					String name = (pinned ? "★ " : "") + truncate(rawName);

					int change = data.getChange();
					Color changeColor = change > 0 ? new Color(0, 200, 0) : change < 0 ? Color.RED : Color.CYAN;
					String changeText = change == 0
						? "stable"
						: moneyMode
							? PriceData.formatGold(change)
							: PriceData.formatPercent(data.getChangePercent());

					JLabel nameLabel = new JLabel(name);
					nameLabel.setForeground(Color.WHITE);
					nameLabel.setFont(nameLabel.getFont().deriveFont(14f));

					JLabel changeLabel = new JLabel(changeText);
					changeLabel.setForeground(changeColor);
					changeLabel.setFont(changeLabel.getFont().deriveFont(Font.BOLD, 14f));

					JPanel row = new JPanel(new BorderLayout(8, 0));
					row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
					row.setBorder(new EmptyBorder(8, 10, 8, 10));
					row.add(nameLabel, BorderLayout.WEST);
					row.add(changeLabel, BorderLayout.EAST);
					row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

					listPanel.add(row);
					listPanel.add(Box.createVerticalStrut(4));
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

	private static String truncate(String name)
	{
		if (name.length() <= MAX_NAME_LENGTH)
		{
			return name;
		}
		return name.substring(0, MAX_NAME_LENGTH - 1) + "…";
	}
}
