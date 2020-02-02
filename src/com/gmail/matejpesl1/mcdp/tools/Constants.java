package com.gmail.matejpesl1.mcdp.tools;

import java.io.File;

public class Constants {
	public static enum Mode {DEBUG, BASICLAND};
	public static final Mode mode = Mode.DEBUG;
	public static final float PROGRAM_VERSION = 4.0f;
	
	public static final String PATH_ROOT = System.getProperty("user.home");
	public static final File OWN_DIR = new File(PATH_ROOT +  "\\vysledky");
}
