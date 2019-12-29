package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Update {
	private URL updateUrl;
	private URL newestVerNumUrl;
	private String thisProgramPath;
	public final float newestVerNum;
	private boolean downloaded;
	public static final File newestVerNumFile = new File(Inspection.OWN_DIR.getPath() + "\\newestVerNum.txt");
	public static final File update = new File(Inspection.OWN_DIR.getPath() + "\\update.jar");
	private Main uvod;
	
	public Update(Main uvod) throws URISyntaxException, IOException {
		this.uvod = uvod;
		 loadUrls();
		 thisProgramPath = getThisProgramPath();
		 newestVerNum = getNewestVerNum();
		 if (!Inspection.OWN_DIR.exists()) {
			 new Inspection(null).createOwnDir();
		 }
	}
	
	private float getNewestVerNum() throws IOException {
		download(newestVerNumUrl, newestVerNumFile.getPath());
		float newestVerNum = Float.parseFloat(new Inspection(null).convertLogContentToString(newestVerNumFile.getPath()));
		newestVerNumFile.delete();
		return newestVerNum;
	}
	
	private void download(URL url, String destination) throws IOException {
			ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			FileOutputStream fos = new FileOutputStream(destination);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		    fos.close();
		    rbc.close();
	}
	
	private void loadUrls() {
		 try {
			 updateUrl = new URL(uvod.getCurrentServer().getUpdateLink());
			 newestVerNumUrl = new URL("https://drive.google.com/uc?export=download&id=1pc7fn0_f5PCYeYsLjE60j1eTEZ_7QmRp");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
	}
	
	private String getThisProgramPath() throws URISyntaxException {
		String thisProgramPath = null;
		/*
			thisProgramPath = new File(Uvod.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI()).getPath();
			*/
		String thisProgramName = new File(System.getProperty("java.class.path")).getName();
		thisProgramPath = System.getProperty("user.dir") + "\\" + thisProgramName;
		return thisProgramPath;
	}
	
	public boolean isUpdateAvailable() {
		if (Main.PROGRAM_VERSION < newestVerNum) {
			return true;
		} else {
			return false;
		}
	}
	
	public void downloadUpdate() throws IOException {
			download(updateUrl, update.getPath());
			downloaded = true;
	}
	
	public void update() throws IOException {
		if (!downloaded) {
			downloadUpdate();
		}
		new ProcessBuilder().command("cmd.exe", "/c", "PING -n 2 127.0.0.1>nul && " +
				"move /Y \"" + update.getPath() + "\"  \"" + thisProgramPath + "\" && \"" + thisProgramPath + "\"").start();
		System.exit(0);
	}
}
