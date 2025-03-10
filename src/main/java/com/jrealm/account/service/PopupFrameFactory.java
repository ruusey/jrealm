package com.jrealm.account.service;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.jrealm.util.Pair;

public class PopupFrameFactory {

	public static Map<String, JFrame> popups = new HashMap<>();

	public PopupFrameFactory() {

	}

	public static void createPopup(String id, Object bean, Boolean disposeOnClose, Integer rows, Integer columns) {
		JFrame frame = new JFrame(id);
		// frame.setSize(300, 600);
		if ((disposeOnClose != null) && (disposeOnClose == true)) {
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent windowEvent) {
					PopupFrameFactory.popups.remove(frame.getTitle());
				}
			});

		} else {
			frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		}
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(rows, columns));
		panel.setOpaque(true);
		for (java.awt.Component comp : PopupFrameFactory.getListComponentAsList(bean)) {
			// String name = comp.getName();
			panel.add(comp);

		}
		// panel.add(container);
		frame.setContentPane(panel);
		// frame.getContentPane().add(BorderLayout.WEST, panel);
		frame.pack();
		// frame.setLocationByPlatform(true);
		frame.setResizable(true);
		PopupFrameFactory.popups.put(id, frame);
	}

	public static void createPopup(String id, List<java.awt.Component> components, Boolean disposeOnClose, Integer rows,
			Integer columns) {
		JFrame frame = new JFrame(id);
		// frame.setSize(300, 600);
		if ((disposeOnClose != null) && (disposeOnClose == true)) {
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent windowEvent) {
					PopupFrameFactory.popups.remove(frame.getTitle());
				}
			});

		} else {
			frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		}
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(rows, columns));
		panel.setOpaque(true);
		for (java.awt.Component comp : components) {
			// String name = comp.getName();
			panel.add(comp);

		}
		JScrollPane scrPane = new JScrollPane(panel);

		frame.setContentPane(scrPane);
		// frame.getContentPane().add(BorderLayout.WEST, panel);
		frame.pack();
		// frame.setLocationByPlatform(true);
		frame.setResizable(true);
		PopupFrameFactory.popups.put(id, frame);
	}

	public static void createPopup(String id, JComponent component, Boolean disposeOnClose, Integer rows,
			Integer columns) {
		JFrame frame = new JFrame(id);
		// frame.setSize(300, 600);
		if ((disposeOnClose != null) && (disposeOnClose == true)) {
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent windowEvent) {
					PopupFrameFactory.popups.remove(frame.getTitle());
				}
			});

		} else {
			frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		}
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(rows, columns));
		panel.setOpaque(true);
		panel.add(component);
		// panel.add(container);
		frame.setContentPane(panel);
		// frame.getContentPane().add(BorderLayout.WEST, panel);
		frame.pack();
		// frame.setLocationByPlatform(true);
		frame.setResizable(true);
		PopupFrameFactory.popups.put(id, frame);
	}

	public static void show(String id) {
		// SwingUtilities.invokeLater(() -> {
		PopupFrameFactory.popups.get(id).setVisible(true);
		PopupFrameFactory.requestFocus();
		// });

	}

	public static void hide(String id) {
		// SwingUtilities.invokeLater(() -> {
		PopupFrameFactory.popups.get(id).setVisible(false);

		// });
	}

	public static void requestFocus() {
		for(Map.Entry<String, JFrame> entry : PopupFrameFactory.popups.entrySet()) {
			if (entry.getValue().isVisible()) {
				entry.getValue().requestFocus();
			}
		}
	}

	public static DefaultListModel<String> getListModel(String... contents) {
		DefaultListModel<String> model = new DefaultListModel<>();
		for (String s : contents) {
			model.addElement(s);

		}

		return model;
	}

	public static JList<String> getListComponent(String... contents) {
		if ((contents == null) || (contents.length == 0))
			return new JList<String>();
		JList<String> l = new JList<>(new DefaultListModel<String>());
		((DefaultListModel<String>) l.getModel()).addAll(Arrays.asList(contents));
		String max = Stream.of(contents).max(Comparator.comparing(String::length)).get();
		// l.setPrototypeCellValue(max);
		if (max.length() > 100) {
			l.setCellRenderer(new MyCellRenderer((max.length() % 100) + 100, 14 + (max.length() % 14)));
		} else if (max.length() < 100) {
			l.setCellRenderer(new MyCellRenderer(max.length() + (100 - max.length()), 14));

		}
		return l;
	}

	public static JList<String> getListComponent(Collection<String> contents) {
		return PopupFrameFactory.getListComponent(contents.toArray(new String[0]));
	}

	public static Pair<JList<String>, JList<String>> getListComponent(Object bean) {
		Map<String, Object> map = PopupFrameFactory.getFieldValueMap(bean);
		JList<String> fields = PopupFrameFactory.getListComponent(map.keySet());
		JList<String> values = PopupFrameFactory
				.getListComponent(map.values().stream().map(Object::toString).collect(Collectors.toList()));

		return new Pair<JList<String>, JList<String>>(fields, values);

	}

	public static List<Component> getListComponentAsList(Object bean) {
		Map<String, Object> map = PopupFrameFactory.getFieldValueMap(bean);
		JList<String> fields = PopupFrameFactory.getListComponent(map.keySet());
		JList<String> values = PopupFrameFactory
				.getListComponent(map.values().stream().map(Object::toString).collect(Collectors.toList()));

		return Arrays.asList(fields, values);

	}

	public static JLabel loadingPanel() throws IOException {

		
		JLabel wIcon = new JLabel("JREALM LOGIN");


		return wIcon;

	}

	public static Map<String, Object> getFieldValueMap(Object bean) {
		Class<?> cls = bean.getClass();
		Map<String, Object> valueMap = new HashMap<String, Object>();
		// Get all fields.
		Field[] fields = cls.getDeclaredFields();
		try {
			for (Field field : fields) {
				field.setAccessible(true);
				Object value = field.get(bean);
				
				valueMap.put(field.getName(), value);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return valueMap;


	}

	public static class MyCellRenderer extends DefaultListCellRenderer {
		/**
		 *
		 */
		private static final long serialVersionUID = 4514094083173856643L;
		public static final String HTML_1 = "<html><body style='width: ";
		public static final String HTML_2 = "px; height: ";
		public static final String HTML_0 = "px'>";
		public static final String HTML_3 = "</html>";
		private int width;
		private int height;

		public MyCellRenderer(int width, int height) {
			this.width = width;
			this.height = height;
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			String text = MyCellRenderer.HTML_1 + String.valueOf(this.width) + MyCellRenderer.HTML_2
					+ String.valueOf(this.height) + MyCellRenderer.HTML_0 + value.toString()
					+ MyCellRenderer.HTML_3;
			return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
		}

	}
}