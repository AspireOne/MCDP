package com.gmail.matejpesl1.mcdp.servers;

import java.awt.Color;

public class Debug implements Server {
	private static final String IP = "mc.debug.cz";
	private static final String MAIL = "matejpesl1@gmail.com";
	private static final String LOGO = "debug_logo.png";
	private static final String UPDATE_LINK =
			"https://github.com/Aspire0ne/mcdp-downloads/releases/latest/download/mcdp-Debug.jar";
	private static final Color BORDER_COLOR = new Color(30, 144, 255,  255);
	private static final String NAME = "Debug";
	
	@Override
	public String getLogo() {
		return LOGO;
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public Color getBorderColor() {
		return BORDER_COLOR;
	}

	@Override
	public String getIP() {
		return IP;
	}
	
	@Override
	public String getMail() {
		return MAIL;
	}
	
	@Override
	public String getUpdateLink() {
		return UPDATE_LINK;
	}
}
