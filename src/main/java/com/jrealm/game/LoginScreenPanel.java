package com.jrealm.game;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferStrategy;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.jrealm.account.dto.CharacterDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.account.service.PopupFrameFactory;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.net.client.SocketClient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Data
@Slf4j
public class LoginScreenPanel extends JPanel implements ActionListener {
    private BufferStrategy bs;

	private JButton loginButton;
	private JLabel usernameLabel;
	private JLabel passwordLabel;
	private JLabel serverAddrLabel;
	private JComboBox<String> chars;
	private JPanel panel;
	private JTextField usernameText, passwordText, serverAddrText;
	private boolean isSubmitted=false;
	private static final long serialVersionUID = 2394780468624375599L;


	public LoginScreenPanel(int width, int height) {
        this.setPreferredSize(new Dimension(width, height));
        this.setFocusable(true);
        this.requestFocus();
		this.usernameLabel = (this.getUsernameLabel());
		this.usernameText = (this.getUsernameText());
		this.passwordLabel = (this.getPasswordLabel());
		this.passwordText = (this.getPasswordText());
		this.serverAddrLabel = this.getServerAddrLabel();
		this.serverAddrText = this.getServerAddrText();
		this.loginButton = new JButton("Login");
		this.loginButton.setBounds(100, 110, 90, 25);
		this.loginButton.setForeground(Color.WHITE);
		this.loginButton.setBackground(Color.BLACK);
		this.panel = new JPanel(new GridLayout(6, 2));
		try {
			JLabel panel = PopupFrameFactory.loadingPanel();
			this.panel.add(panel);
			this.panel.add(new JSeparator(SwingConstants.VERTICAL));
			// this.panel.add(new JSeparator());

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.panel.add(this.usernameLabel); // set username label to panel
		this.panel.add(this.usernameText); // set text field to panel
		this.panel.add(this.passwordLabel); // set password label to panel
		this.panel.add(this.passwordText); // set text field to panel
		this.panel.add(this.serverAddrLabel);
		this.panel.add(this.serverAddrText);

		this.panel.add(this.loginButton);
		this.panel.add(new JSeparator(SwingConstants.VERTICAL));
		this.add(this.panel, BorderLayout.CENTER);
		this.loginButton.addActionListener(this);
        this.setVisible(true);
	}
	
	public void setCharacters(PlayerAccountDto account) {
		this.chars = new JComboBox<>();
		this.chars.addItem("-- Select Character --");

		for(CharacterDto character : account.getCharacters()) {
			
            int lvl = 0;
            if (GameDataManager.EXPERIENCE_LVLS.isMaxLvl(character.getStats().getXp())) {
                lvl = 20;
            } else {
                lvl = GameDataManager.EXPERIENCE_LVLS.getLevel(character.getStats().getXp());
            }
            
			 String characterStr = "{0}, lv {1} {2} {3}/8  [{4}]";
             characterStr = MessageFormat.format(characterStr, account.getAccountName(), lvl, CharacterClass.valueOf(character.getCharacterClass()),
            		 character.numStatsMaxed(), character.getCharacterUuid());
             this.chars.addItem(characterStr);
		}
		this.panel.add(this.chars);
	}

	public JLabel getUsernameLabel() {
		final JLabel label = new JLabel("Username");
		return label;
	}

	public JTextField getUsernameText() {
		final JTextField username = new JTextField(16);
		username.setText("ru@jrealm.com");
		return username;
	}
	
	public JTextField getServerAddrText() {
		final JTextField ip = new JTextField(28);
		ip.setText("127.0.0.1");
		return ip;
	}

	public JLabel getPasswordLabel() {
		final JLabel label = new JLabel("Password");
		return label;
	}
	
	public JLabel getServerAddrLabel() {
		final JLabel label = new JLabel("Server Address");
		return label;
	}

	public JPasswordField getPasswordText() {
		final JPasswordField password = new JPasswordField(16);
		password.setText("password");
		return password;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String username = this.usernameText.getText();
		String password = this.passwordText.getText();
		String addr = this.serverAddrText.getText();
		try {
			SocketClient.PLAYER_EMAIL=username;
			SocketClient.PLAYER_PASSWORD=password;
			SocketClient.SERVER_ADDR=addr;
			this.isSubmitted=true;
			log.info("[CLIENT] Successfully captured user login data Email={}, Server={}", username, addr);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}
	public JFrame start() {
		final JFrame frame = new JFrame("JRealm Login Page");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(this.getWidth(), (int) this.getHeight());
		this.setOpaque(true);
		frame.add(this);
		frame.pack();
		return frame;
	}
	
	
	public static void main(String[] args) throws Exception {
		LoginScreenPanel p = new LoginScreenPanel(800, 300);
		JFrame frame = p.start();
		frame.setVisible(true);
	}

}