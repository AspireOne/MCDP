package com.gmail.matejpesl1.mcdp.servers;

import java.awt.Color;

public class Basicland implements Server {
	private static final String IP = "mc.basicland.cz";
	private static final String MAIL = "report@basicland.cz";
	private static final String LOGO = "basicland-logo.png";
	private static final String UPDATE_LINK =
			"https://github.com/Aspire0ne/MCDP-downloads/releases/latest/download/MCDP-Basicland.jar";
	private static final String NAME = "BasicLand";
	private static final Color BORDER_COLOR = new Color(30, 144, 255,  255);
	
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