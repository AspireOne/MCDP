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
	public static final File newestVerNumFile = new File(Kontrola.VLASTNI_SLOZKA_CESTA + "\\newestVerNum.txt");
	public static final File newestVer = new File(Kontrola.VLASTNI_SLOZKA_CESTA + "\\newestVer.jar");
	
	public Aktualizace() {
		 loadUrls();
		 thisProgramPath = getThisProgramPath();
		 newestVerNum = getNewestVerNum();
		 System.out.println("this program path: " + thisProgramPath);
	}
	
	private float getNewestVerNum() {
		try {
			download(newestVerNumUrl, newestVerNumFile.getPath());	
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		float newestVerNum = Float.parseFloat(new Kontrola().prevedObsahSouboruNaString(newestVerNumFile.getPath()));
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
				System.exit(0);
			}
	}
	
	private String getThisProgramPath() {
		String thisProgramLoc = null;
		try {
			thisProgramLoc = new File(Aktualizace.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI()).getPath();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		return thisProgramLoc;
	}
	
	public boolean isUpdateAvailable() {
		if (Uvod.VERZE_PROGRAMU < newestVerNum) {
			return true;
		} else {
			return false;
		}
	}
	
	public void downloadUpdate() {
		try {
			download(newestVerUrl, newestVer.getPath());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void update() {
		if (newestVer == null) {
			System.out.println("stahujuuuu");
			downloadUpdate();
		}
		System.out.println("doing it");
		
		try {
			new ProcessBuilder().command("cmd.exe", "/c", "PING -n 2 127.0.0.1>nul && " +
					"move /Y " + newestVer.getPath() + " " + thisProgramPath + " && " + thisProgramPath).start();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} 
		
		System.exit(0);
	}
}
