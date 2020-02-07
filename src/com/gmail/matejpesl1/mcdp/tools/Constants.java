package com.gmail.matejpesl1.mcdp.tools;

import java.io.File;

//a class for globally accessed constants
public class Constants {
	public enum Mode {DEBUG, BASICLAND}

	public static final Mode mode = Mode.DEBUG;
	public static final float PROGRAM_VERSION = 4.2f;

	public static final String PATH_ROOT = System.getProperty("user.home");
	public static final File OWN_DIR = new File(PATH_ROOT +  "\\vysledky");

	private Constants() {}
}