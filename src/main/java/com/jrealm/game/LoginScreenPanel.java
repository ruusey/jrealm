package com.jrealm.game;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.jrealm.account.service.PopupFrameFactory;
import com.jrealm.net.client.SocketClient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Data
@Slf4j
public class LoginScreenPanel extends JFrame implements ActionListener {
	private JButton loginButton;
	private JLabel usernameLabel;
	private JLabel passwordLabel;
	private JLabel serverAddrLabel;

	private JPanel panel;
	private JTextField usernameText, passwordText, serverAddrText;
	private boolean isSubmitted=false;
	private static final long serialVersionUID = 2394780468624375599L;


	public LoginScreenPanel() {
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
		this.panel = new JPanel(new GridLayout(5, 2));
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
		this.setTitle("JRealm Login");

	}


	public JLabel getUsernameLabel() {
		JLabel label = new JLabel("Username");
		return label;
	}

	public JTextField getUsernameText() {
		JTextField username = new JTextField(16);
		//username.setBounds(100, 27, 193, 28);
		return username;
	}
	
	public JTextField getServerAddrText() {
		JTextField ip = new JTextField(28);
		//username.setBounds(100, 27, 193, 28);
		ip.setText("127.0.0.1");
		return ip;
	}

	public JLabel getPasswordLabel() {
		JLabel label = new JLabel("Password");
		return label;
	}
	
	public JLabel getServerAddrLabel() {
		JLabel label = new JLabel("Server Address");
		return label;
	}

	public JPasswordField getPasswordText() {
		JPasswordField password = new JPasswordField(16);
		// username.setBounds(100, 27, 193, 28);
		return password;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String username = this.usernameText.getText();
		String password = this.passwordText.getText();
		String addr = this.serverAddrText.getText();
		try {
	

			this.isSubmitted=true;

			SocketClient.PLAYER_EMAIL=username;
			SocketClient.PLAYER_PASSWORD=password;
			SocketClient.SERVER_ADDR=addr;

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

}