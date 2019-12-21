package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Aktualizace {
	private URL newestVerUrl;
	private URL newestVerNumUrl;
	private String thisProgramPath;
	public final float newestVerNum;
	private boolean downloaded;
	public static final File newestVerNumFile = new File(Kontrola.VLASTNI_SLOZKA_CESTA + "\\newestVerNum.txt");
	public static final File newestVer = new File(Kontrola.VLASTNI_SLOZKA_CESTA + "\\newestVer.jar");
	
	public Aktualizace() throws URISyntaxException, IOException {
		 loadUrls();
		 thisProgramPath = getThisProgramPath();
		 newestVerNum = getNewestVerNum();
	}
	
	private float getNewestVerNum() throws IOException {
		download(newestVerNumUrl, newestVerNumFile.getPath());
		float newestVerNum = Float.parseFloat(new Kontrola().prevedObsahSouboruNaString(newestVerNumFile.getPath()));
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
			 newestVerUrl = new URL("https://drive.google.com/uc?export=download&id=13UJKideTMyW6U19PYA3WhKOV0QgqISlC");
			 newestVerNumUrl = new URL("https://drive.google.com/uc?export=download&id=1pc7fn0_f5PCYeYsLjE60j1eTEZ_7QmRp");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
	}
	
	private String getThisProgramPath() throws URISyntaxException {
		String thisProgramPath = null;
			thisProgramPath = new File(Aktualizace.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI()).getPath();
		return thisProgramPath;
	}
	
	public boolean isUpdateAvailable() {
		if (Uvod.VERZE_PROGRAMU < newestVerNum) {
			return true;
		} else {
			return false;
		}
	}
	
	public void downloadUpdate() throws IOException {
			download(newestVerUrl, newestVer.getPath());
			downloaded = true;
	}
	
	public void update() throws IOException {
		if (!downloaded) {
			downloadUpdate();
		}
		new ProcessBuilder().command("cmd.exe", "/c", "PING -n 2 127.0.0.1>nul && " +
				"move /Y " + newestVer.getPath() + " " + thisProgramPath + " && " + thisProgramPath).start();
		System.exit(0);
	}
}
