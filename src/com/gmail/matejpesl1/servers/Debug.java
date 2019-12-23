package com.gmail.matejpesl1.servers;

import java.awt.Color;

public class Debug implements Server {
	private static final String IP = "mc.debug.cz";
	private static final String MAIL = "matejpesl1@gmail.com";
	private static final String LOGO = "debug-logo.png";
	private static final String UPDATE_LINK = "https://drive.google.com/uc?export=download&id=1V6VirJm7yLWROPAjjCVT00xNPMdZL-gT";
	private static final Color BORDER_COLOR = new Color(30, 144, 255,  255);
	
	@Override
	public String getLogo() {
		return LOGO;
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
