package com.gmail.matejpesl1.servers;

import java.awt.Color;

public class Debug implements Server {
	private static final String IP = "mc.debug.cz";
	private static final String MAIL = "matejpesl1@gmail.com";
	private static final String LOGO = "debug-logo.png";
	private static final Color BORDER_COLOR = new Color(30, 144, 255,  255);

	public String getLogo() {
		return LOGO;
	}
	
	public Color getBorderColor() {
		return BORDER_COLOR;
	}

	public String getIP() {
		return IP;
	}
	
	public String getMail() {
		return MAIL;
	}
}
