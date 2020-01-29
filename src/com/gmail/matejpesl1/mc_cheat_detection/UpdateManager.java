package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class UpdateManager {
	private URL updateUrl;
	private URL newestVerNumUrl;
	private final String thisProgramPath;
	private float newestVerNum;
	private boolean downloaded;
	public static final File NEWEST_VER_NUM_FILE = 
			new File(Inspection.OWN_DIR.getPath() + "\\newestVerNum.txt");
	public static final File UPDATE_FILE = 
			new File(Inspection.OWN_DIR.getPath() + "\\update.jar");
	public static final String UPDATE_FILE_URL_STR =
			"https://drive.google.com/uc?export=download&id=1pc7fn0_f5PCYeYsLjE60j1eTEZ_7QmRp";
	private Main uvod;
	
	public UpdateManager(Main uvod) throws URISyntaxException, MalformedURLException {
		this.uvod = uvod;
		thisProgramPath = getThisProgramPath();
		loadUrls();
		if (!Inspection.OWN_DIR.exists()) {
			new Inspection(null).createOwnDir();
		}
	}
	
	private float getNewestVerNum() throws IOException {
		download(newestVerNumUrl, NEWEST_VER_NUM_FILE.getPath());
		float newestVerNum = 
				Float.parseFloat
				(new Inspection(null)
						.convertLogContentToString
						(NEWEST_VER_NUM_FILE.getPath()));
		NEWEST_VER_NUM_FILE.delete();
		return newestVerNum;
	}
	
	private void download(URL url, String destination) throws IOException {
			ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			FileOutputStream fos = new FileOutputStream(destination);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		    fos.close();
		    rbc.close();
	}
	
	private void loadUrls() throws MalformedURLException {
		updateUrl = 
				new URL(uvod.getCurrentServer().getUpdateLink());
		newestVerNumUrl = 
				new URL(UPDATE_FILE_URL_STR);
	}
	
	private String getThisProgramPath() throws URISyntaxException {
		String thisProgramPath = null;
		String thisProgramName = new File(System.getProperty("java.class.path")).getName();
		thisProgramPath = System.getProperty("user.dir") + "\\" + thisProgramName;

		System.out.println("path: " + thisProgramPath);
		return thisProgramPath;
	}
	
	public static boolean isProgramInIDE() {
	    boolean isEclipse = System.getProperty("java.class.path").toLowerCase().contains("eclipse");
	    boolean isIntellij = 
	    		System.getProperty("java.class.path")
	    		.toLowerCase()
	    		.contains("idea.test.cyclic.buffer.size");
	    if (!isEclipse && !isIntellij) {
	    	return false;
	    } else {
	    	System.out.println("is ide");
	    	return true;
	    }
	}
	
	public boolean checkUpdateAvailability() throws IOException {
		if (newestVerNum == 0) {
			newestVerNum = getNewestVerNum();
		}
		
		if (Main.PROGRAM_VERSION < newestVerNum) {
			return true;
		} else {
			return false;
		}
	}
	
	public void downloadUpdate() throws IOException {
			download(updateUrl, UPDATE_FILE.getPath());
			downloaded = true;
	}
	
	public void update() throws IOException {
		if (!downloaded) {
			downloadUpdate();
		}
		new ProcessBuilder().command("cmd.exe", "/c", "PING -n 2 127.0.0.1>nul && " +
				"move /Y \"" + UPDATE_FILE.getPath() + "\"  \"" + thisProgramPath + "\" && \"" + thisProgramPath + "\"").start();
		System.exit(0);
	}
}
