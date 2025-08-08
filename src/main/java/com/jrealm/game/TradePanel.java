package com.jrealm.game;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.BorderFactory;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.data.GameSpriteManager;
import com.jrealm.net.entity.NetGameItem;
import com.jrealm.net.entity.NetPlayer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("serial")
public class TradePanel extends JPanel implements ActionListener {
	private BufferStrategy bs;
	private JPanel panel;
	private boolean isSubmitted = false;
	private static final long serialVersionUID = 2394780468624375599L;

	private JFrame frame;

	private JList<ImageIcon> myItems;
	
	public TradePanel(int width, int height, NetPlayer player0, NetPlayer player1, NetGameItem[] p0Inv, NetGameItem[] p1Inv) {
		this.setPreferredSize(new Dimension(width, height));
		this.setSize(new Dimension(width, height));
		this.setFocusable(true);
		this.requestFocus();
		
		this.panel = new JPanel(new GridLayout(2, 2, 16, -64));
		//this.panel.setSize(new Dimension(width/4, height/4));

		this.panel.add(getPlayerNameLabel(player0.getName(), false));
		this.panel.add(getPlayerNameLabel(player1.getName(), true));

		this.myItems = getInventoryIcons(p0Inv, true);
		this.panel.add(this.myItems);
		this.panel.add(getInventoryIcons(p1Inv, false));

		this.add(this.panel);
		this.setVisible(true);
	}
	
	public JLabel getPlayerNameLabel(String name, boolean rightAlign) {
		final JLabel label = new JLabel(name);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		label.setFont(new Font("Serif", Font.PLAIN, 32));
		return label;
	}
	
	public JList<ImageIcon> getInventoryIcons(NetGameItem[] inv, boolean bindEvents) {
		final NetGameItem[] inventoryArr = Arrays.copyOfRange(inv, 4, 12);
		final List<ImageIcon> icons = new ArrayList<>();
		for(int i = 0; i<inventoryArr.length; i++) {
			final int idx = i;
			NetGameItem item = inventoryArr[idx];
			if(item==null) continue;
			final BufferedImage itemImage = GameSpriteManager.ITEM_SPRITES.get(item.getItemId());

			if(itemImage==null) {
				continue;
			}
		    Image newImage = itemImage.getScaledInstance(64, 64, Image.SCALE_DEFAULT);


			final ImageIcon icon =  new ImageIcon(newImage);
			final JButton imageButton = new JButton(icon);
			
			if(bindEvents) {
				imageButton.addActionListener(new ActionListener() {
			        @Override
			        public void actionPerformed(ActionEvent e) {
			            // Your code to execute on button click
			            log.info("Inventory item index {} clicked. Item = {}", idx, item.getName());
			        }
			    });
			}
			icons.add(icon);
		}
		final JList<ImageIcon> imageListComponent = new JList<>(new DefaultListModel<ImageIcon>());
		((DefaultListModel<ImageIcon>) imageListComponent.getModel()).addAll(icons);
		imageListComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		return imageListComponent;

	}

	public void addClassTypes(JComboBox<String> select) {
		for (CharacterClass cls : CharacterClass.values()) {
			if (cls.classId > 0) {
				select.addItem(cls.name() + "[" + cls.classId + "]");
			}
		}
	}

	public JLabel getUsernameLabel() {
		final JLabel label = new JLabel("Username", SwingConstants.RIGHT);
		return label;
	}

	public JTextField getUsernameText() {
		final JTextField username = new JTextField(16);
		username.setText("@jrealm.com");
		return username;
	}

	public JTextField getServerAddrText() {
		final JTextField ip = new JTextField(28);
		ip.setText("127.0.0.1");
		return ip;
	}


	public JLabel getPasswordLabel() {
		final JLabel label = new JLabel("Password", SwingConstants.RIGHT);
		return label;
	}

	public JLabel getServerAddrLabel() {
		final JLabel label = new JLabel("Server Address", SwingConstants.RIGHT);
		return label;
	}

	public JPasswordField getPasswordText() {
		final JPasswordField password = new JPasswordField(16);
		return password;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		

	}

	public JFrame getTradePane() {
		final JFrame frame = new JFrame("Trade");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(this.getWidth(), (int) this.getHeight());
		this.setOpaque(true);
		frame.add(this);
		frame.pack();
		this.frame = frame;
		return frame;
	}

//	public static void main(String[] args) throws Exception {
//		TradePanel p = new TradePanel(800, 300);
//		JFrame frame = p.getTradePane();
//		frame.setVisible(true);
//	}

}