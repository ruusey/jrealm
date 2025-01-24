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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jrealm.account.dto.CharacterDto;
import com.jrealm.account.dto.PlayerAccountDto;
import com.jrealm.account.service.PopupFrameFactory;
import com.jrealm.game.contants.CharacterClass;
import com.jrealm.game.data.GameDataManager;
import com.jrealm.game.util.WorkerThread;
import com.jrealm.net.client.ClientGameLogic;
import com.jrealm.net.client.SocketClient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class LoginScreenPanel extends JPanel implements ActionListener {
	private BufferStrategy bs;

	private JButton loginButton;
	private JButton registerButton;

	private JLabel usernameLabel;
	private JLabel passwordLabel;
	private JLabel serverAddrLabel;
	private JComboBox<String> chars;
	private JPanel panel;
	private JTextField usernameText, passwordText, serverAddrText;
	private boolean isSubmitted = false;
	private static final long serialVersionUID = 2394780468624375599L;
	private JComboBox<String> classTypes;
	private JFrame frame;
	public LoginScreenPanel(int width, int height) {
		this.setPreferredSize(new Dimension(width, height));
		this.setSize(new Dimension(width, height));
		this.setFocusable(true);
		this.requestFocus();
		this.usernameLabel = (this.getUsernameLabel());
		
		this.usernameText = (this.getUsernameText());
		this.passwordLabel = (this.getPasswordLabel());
		this.passwordText = (this.getPasswordText());
		this.serverAddrLabel = this.getServerAddrLabel();
		this.serverAddrText = this.getServerAddrText();
		this.loginButton = new JButton("Login");
		//this.loginButton.setBounds(100, 110, 90, 25);
		this.loginButton.setForeground(Color.WHITE);
		this.loginButton.setBackground(Color.BLACK);
		
		this.registerButton = new JButton("Register");
		//this.registerButton.setBounds(100, 110, 90, 25);
		this.registerButton.setForeground(Color.WHITE);
		this.registerButton.setBackground(Color.BLACK);
		this.panel = new JPanel(new GridLayout(8, 2, 16,0));
		try {
			JLabel panel = PopupFrameFactory.loadingPanel();
			this.panel.add(panel);
			this.panel.add(new JComponent() {});

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.panel.add(this.usernameLabel);
		this.panel.add(this.usernameText); 

		this.panel.add(this.passwordLabel);
		this.panel.add(this.passwordText); 

		this.panel.add(this.serverAddrLabel);
		this.panel.add(this.serverAddrText);
		
		this.panel.add(new JComponent() {});
		this.panel.add(this.loginButton);
		
		this.panel.add(new JComponent() {});
		this.panel.add(this.registerButton);

		//this.panel.add(new JSeparator(SwingConstants.VERTICAL));
		this.add(this.panel, BorderLayout.CENTER);
		this.loginButton.addActionListener(this);
		this.registerButton.addActionListener(this.getRegisterListner());
		this.setVisible(true);
	}
	
	
	private ActionListener getRegisterListner() {
		return e -> {
			try {
				final String accountName = JOptionPane.showInputDialog(this.frame, "Choose Account Name", "Itani");
				final String username = this.usernameText.getText();
				final String password = this.passwordText.getText();
				final ObjectNode jsonReq = new ObjectNode(JsonNodeFactory.instance);
				jsonReq.put("password", password);
				jsonReq.put("email", username);

				jsonReq.put("accountName", accountName);
				jsonReq.putArray("accountProvisions").add("JREALM");
				jsonReq.putArray("accountSubscriptions").add("TRIAL");
				jsonReq.put("admin", false);
				log.info("Register click with username={}, password={}", username, password);
				final ObjectNode registerResult = ClientGameLogic.DATA_SERVICE.executePost("admin/account/register", jsonReq, ObjectNode.class);
				log.info("Registration successful for account {}", registerResult.get("email"));
			} catch (Exception e1) {
				log.error("Failed registering account. Reason: {}", e1.getMessage());
			}
		};
	}

	public void setCharacters(PlayerAccountDto account) {
		if(this.chars!=null) {
			this.panel.remove(this.chars);
		}
		this.chars = new JComboBox<>();
		this.chars.addItem("-- Select Character --");

		for (CharacterDto character : account.getCharacters()) {
			int lvl = 0;
			if (GameDataManager.EXPERIENCE_LVLS.isMaxLvl(character.getStats().getXp())) {
				lvl = 20;
			} else {
				lvl = GameDataManager.EXPERIENCE_LVLS.getLevel(character.getStats().getXp());
			}
			String characterStr = "{0}, lv {1} {2} {3}/8  [{4}]";
			characterStr = MessageFormat.format(characterStr, account.getAccountName(), lvl,
					CharacterClass.valueOf(character.getCharacterClass()), character.numStatsMaxed(),
					character.getCharacterUuid());
			this.chars.addItem(characterStr);
		}
		
		this.panel.add(this.chars);
		JButton newCharacterButton = new JButton("New Character");
		this.panel.remove(newCharacterButton);
		newCharacterButton.addActionListener(e -> {
			log.info("[CLIENT] Login - new character button press. Source={}", e.getSource());
			if(this.classTypes!=null) {
				this.panel.remove(this.classTypes);
			}
			this.classTypes = new JComboBox<>();
			this.classTypes.addItem("-- Character Type --");
			this.addClassTypes(this.classTypes);
			this.panel.add(this.classTypes);
			final Dimension currSize = this.frame.getSize();
			currSize.setSize(currSize.getWidth() + 20, currSize.getHeight() + 20);
			this.frame.setSize(currSize);

			final Runnable charSelectRun = ()->{
				while (this.classTypes.getSelectedItem().equals("-- Character Type --")) {
					try {
						Thread.sleep(50);
					}catch(Exception e1) {}
				}
				final String chosenClass = this.classTypes.getSelectedItem().toString();
				final int idx = chosenClass.indexOf("[");
				final String classId = chosenClass.substring(idx + 1, chosenClass.lastIndexOf("]"));
				try {
					final PlayerAccountDto result = ClientGameLogic.DATA_SERVICE.executePost("data/account/"+account.getAccountUuid()+"/character?classId="+classId, null, PlayerAccountDto.class);
					this.setCharacters(result);
				} catch (Exception e1) {
					log.error("[CLIENT] Failed to create character for account {}. Reason: {}",account.getAccountEmail(), e1.getMessage());
				}
			};
			WorkerThread.submitAndForkRun(charSelectRun);
		});
		
		this.panel.add(newCharacterButton);
		this.panel.add(new JComponent() {});

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
	public JTextField getUsernameTextField() {
		return this.usernameText;
	}

	public JTextField getServerAddrText() {
		final JTextField ip = new JTextField(28);
		ip.setText("127.0.0.1");
		return ip;
	}
	
	public JTextField getServerAddrTextField() {
		return this.serverAddrText;
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
	
	public JTextField getPasswordTextField() {
		return this.passwordText;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final String username = this.usernameText.getText();
		final String password = this.passwordText.getText();
		final String addr = this.serverAddrText.getText();
		try {
			SocketClient.PLAYER_EMAIL = username;
			SocketClient.PLAYER_PASSWORD = password;
			SocketClient.SERVER_ADDR = addr;
			this.isSubmitted = true;
			log.info("[CLIENT] Successfully captured user login data Email={}, Server={}", username, addr);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}

	public JFrame getLoginFrame() {
		final JFrame frame = new JFrame("JRealm Login Page");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(this.getWidth(), (int) this.getHeight());
		this.setOpaque(true);
		frame.add(this);
		frame.pack();
		this.frame = frame;
		return frame;
	}

	public static void main(String[] args) throws Exception {
		LoginScreenPanel p = new LoginScreenPanel(800, 300);
		JFrame frame = p.getLoginFrame();
		frame.setVisible(true);
	}

}