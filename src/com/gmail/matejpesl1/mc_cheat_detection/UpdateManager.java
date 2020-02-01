package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

public class UpdateManager {
	private URL updateUrl;
	private URL latestVerNumURL;
	private final String thisProgramPath;
	private float newestVerNum;
	private boolean downloaded;
	public static final File LATEST_VER_NUM_FILE = 
			new File(Inspection.OWN_DIR.getPath() + "\\newestVerNum.txt");
	public static final File UPDATE_FILE = 
			new File(Inspection.OWN_DIR.getPath() + "\\update.jar");
	public static final String LATEST_VER_NUM_URL_STR =
			"https://api.github.com/repos/Aspire0ne/MCDP-downloads/tags";
	private Main uvod;
	
	public UpdateManager(Main uvod) throws URISyntaxException, MalformedURLException {
		this.uvod = uvod;
		thisProgramPath = getThisProgramPath();
		loadUrls();
		if (!Inspection.OWN_DIR.exists()) {
			new Inspection(null).createOwnDir();
		}
	}
	
	private float getNewestVerNum() throws Exception {
		Scanner sc = new Scanner(latestVerNumURL.openStream(), "UTF-8");
		String releaseInfo = sc.useDelimiter("\\A").next();
		sc.close();
		int numBeginChar = releaseInfo.indexOf("\"v") + 2;
		int numEndChar = releaseInfo.indexOf("\"", numBeginChar);
		String tag = releaseInfo.substring(numBeginChar, numEndChar);
		System.out.println(tag);
		return Float.parseFloat(tag);
	}
	
	private void download(URL url, String destination) throws Exception {
		ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
		FileOutputStream fos = new FileOutputStream(destination);
		FileChannel fch = fos.getChannel();
		
		fch.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		fos.close();
		fch.close();
	}
	
	private void loadUrls() throws MalformedURLException {
		updateUrl = 
				new URL(uvod.getCurrentServer().getUpdateLink());
		latestVerNumURL = 
				new URL(LATEST_VER_NUM_URL_STR);
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
	
	public boolean checkUpdateAvailability() throws Exception {
		if (newestVerNum == 0) {
			newestVerNum = getNewestVerNum();
		}
		
		if (Main.PROGRAM_VERSION < newestVerNum) {
			return true;
		} else {
			return false;
		}
	}
	
	public void downloadUpdate() throws Exception {
			download(updateUrl, UPDATE_FILE.getPath());
			downloaded = true;
	}
	
	public void update() throws Exception {
		if (!downloaded) {
			downloadUpdate();
		}
		new ProcessBuilder().command("cmd.exe", "/c", "PING -n 2 127.0.0.1>nul && " +
				"move /Y \"" + UPDATE_FILE.getPath() + "\"  \"" + thisProgramPath + "\" && \"" + thisProgramPath + "\"").start();
		System.exit(0);
	}
}
