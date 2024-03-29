package com.gmail.matejpesl1.mcdp.program;

import com.gmail.matejpesl1.mcdp.frontend.Main;
import com.gmail.matejpesl1.mcdp.servers.Debug;
import com.gmail.matejpesl1.mcdp.tools.Constants;
import com.gmail.matejpesl1.mcdp.tools.Constants.Mode;
import com.gmail.matejpesl1.mcdp.tools.Email;
import com.gmail.matejpesl1.mcdp.tools.FileSearch;
import com.gmail.matejpesl1.mcdp.tools.FileUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Inspection extends Thread {
	private static final ArrayList<String> BACKUP_KEYWORDS =
			new ArrayList<>(Arrays.asList("sigma", "huzuni", "skillclient", "liquidbounce",
					"wolfram", "impact_", "impact-", "impactinstaller", "-impact", "_impact",
					"aristois", "wurst", "jam", "ghostclient", "ghost-client", "ghost_client",
					"fluxclient", "flux-client", "flux_client", "flux client", "kronos", "jigsaw",
					"hacked", "hackclient", "hacked-client", "hack-client", "pandora", "killaura",
					"kill-aura", "forgehax", "kami", "xray", "x-ray"));
	
	private static final ArrayList<String> BACKUP_LOG_KEYWORDS =
			new ArrayList<>(Arrays.asList("ghost", "sigma", "huzuni", "skillclient", "liquidbounce",
			"wolfram", "impact", "aristois", "wurst", "jam", "kronos", "jigsaw", "hacked",
			"hackclient", "hacked-client", "hack-client", "pandora", "killaura", "kill-aura",
			"forgehax", "hack", "autosoup", "antiknockback", "tpaura", "tp-aura", "regen", "blink", 
			"nofall", "no-fall", "autosoup", "velocity", "novelocity", "nuker", "xray", "x-ray"));
	
	//both are initialized at the start of inspection, in the doInspection() method
	private ArrayList<String> fileKeywords;
	private ArrayList<String> logKeywords;
	private final ArrayList<String> errors = new ArrayList<>();
	private static final ArrayList<File> logs = new ArrayList<>();
	
	public String progress;
	public static final String WORD_SEPARATOR = " | ";
	public static final String INITIAL_INFO = 0 + "\n" + 0;
	private static final String URL_TO_KEYWORDS_STR =
			"https://drive.google.com/uc?export=download&id=1h1RyP_m1j29jRES-TWj55Vg47BbppSoF";
	
	
	private static final int MAX_INSPECTED_LOG_LINES = 70000;
	private static final short MAX_INSPECTED_LOG_LINES_NAME = 300;
	
	public static final File LOGS_DIR = new File(Constants.PATH_ROOT +  "\\AppData\\Roaming\\.minecraft\\logs");
	public static final File ROAMING_DIR = new File(Constants.PATH_ROOT +"\\AppData\\Roaming");
	public static final File DESKTOP_DIR = new File(Constants.PATH_ROOT + "\\Desktop");
	public static final File VERSIONS_DIR = new File(Constants.PATH_ROOT + "\\AppData\\Roaming\\.minecraft\\versions");
	public static final File MINECRAFT_DIR = new File(Constants.PATH_ROOT + "\\AppData\\Roaming\\.minecraft");
	public static final File DOWNLOADS_DIR = new File(Paths.get(Constants.PATH_ROOT, "Downloads").toString());
	
	public static final File LATEST_ZIP = new File(Constants.OWN_DIR + "\\latest.zip");
	public static final File LATEST_LOG = new File(LOGS_DIR.getPath() + "\\latest.log");
	public static final File INFO_TXT = new File(Constants.OWN_DIR.getPath() + "\\predesleKontrolyInfo.txt");
	
	public static final boolean DOWNLOAD_KEYWORDS = false;
	public static final boolean LOGS_DIR_EXISTS = LOGS_DIR.exists();
	public static final boolean DESKTOP_DIR_EXISTS = DESKTOP_DIR.exists();
	public static final boolean DOWNLOADS_DIR_EXISTS = DOWNLOADS_DIR.exists();
	public static final boolean MINECRAFT_DIR_EXISTS = MINECRAFT_DIR.exists();
	public static final boolean VERSIONS_DIR_EXISTS = VERSIONS_DIR.exists();
	public static final boolean LATEST_LOG_EXISTS = LATEST_LOG.exists();
	
	public static final short MAIL_LATEST_LOG_LINES = 500;
	public static final short MAIL_LOGS_KEYWORDS_LINES = 100;
	public static final short MAX_AGE_OF_LOGS_TO_INSPECT_DAYS = 25;
	
	public static final DateTimeFormatter FORMATTER = 
	DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
	
	private final Main main;
	private final Player player = new Player();
	
	public Inspection(Main main) {
		printInspectionProgress("0.5");
		this.main = main;
	}
	
	public void doInspection() {
		printInspectionProgress("1");
		LATEST_ZIP.deleteOnExit();
		
		if (!Constants.OWN_DIR.exists()) {
			FileUtils.createOwnDir();
		} else {
			if (Constants.OWN_DIR.isHidden()) {
				try {
					FileUtils.changeFileAttribute(Constants.OWN_DIR, "dos:hidden", false);
					FileUtils.changeFileAttribute(INFO_TXT, "dos:hidden", false);	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		loadKeywords();
		
		final String txtInfoStr = 
				FileUtils.convertFileContentToString(INFO_TXT);
		
		String lastInspectionDateStr = getLine(txtInfoStr, 2);
		
		if (!lastInspectionDateStr.equals("0")) {
			try {
				player.setLastInspectionDate(LocalDateTime.parse(lastInspectionDateStr, FORMATTER));
			} catch (Exception e) {
				e.printStackTrace();
				writeInitialInfo();
				errors.add("�patn� zapsan� datum, pravd�podobn� zapsan� pomoc�"
						+ " star�� verze programu. Prob�hla oprava.");
			}
		}
		

		printInspectionProgress("2");
		
		long timeSinceLastInspectionMins = 0;
		if (player.getLastInspectionDate() != null) {
			try {
				long currentEpochMillis = Instant.now().toEpochMilli();
				Instant instant = player.getLastInspectionDate().atZone(ZoneId.systemDefault()).toInstant();
				long lastInspectionEpochMillis = instant.toEpochMilli();
				timeSinceLastInspectionMins =
						getMillisDiff(currentEpochMillis, lastInspectionEpochMillis,
							TimeUnit.MINUTES);
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				writeInitialInfo();
			} catch (NullPointerException e) {
				e.printStackTrace();
				timeSinceLastInspectionMins = -1;
				errors.add("textov� soubor, kter� zaznamen�v� p�ede�l� kontroly, nebyl nalezen.");
				FileUtils.createOwnDir();
			}
		}
		player.setTimeSinceLastInspectionMins(timeSinceLastInspectionMins);
		
		//progress
		printInspectionProgress("3");
		
		if (Constants.mode != Mode.DEBUG) {
			if (player.getTimeSinceLastInspection(TimeUnit.MINUTES) < 3 && player.getTimeSinceLastInspection(TimeUnit.MINUTES) >= 1) {
				interruptInspection("Doba od posledn� kontroly je moc kr�tk�, zkuste to pozd�ji.",
						true, null, false);
			}	
		}
		
		long currentEpochMillis = Instant.now().toEpochMilli();
		long lastVersionsModifInMins =
				getMillisDiff(currentEpochMillis, VERSIONS_DIR.lastModified(), TimeUnit.MINUTES);
		
		if (lastVersionsModifInMins < 30 && lastVersionsModifInMins >= 0) {
			player.addHackerIndication("Slo�ka versions byla upravov�na p�ed m�n� ne� 30 minutami.");
		}
		printInspectionProgress("4");
		
		//START
		ArrayList<String> namesOfFilesOnDesktopArr;
		ArrayList<String> namesOfJarFilesInDownloadsArr;
		ArrayList<String> namesOfFilesInVersionsArr;
		ArrayList<String> namesOfFilesInMinecraftArr;
		ArrayList<String> namesOfExeFilesInDownloadsArr;
		
		namesOfFilesInVersionsArr = new FileSearch
				((String)null, null)
				.searchFiles(VERSIONS_DIR, false, true, null);
		
		namesOfFilesInMinecraftArr = new FileSearch
				((String)null, null)
				.searchFiles(MINECRAFT_DIR, false, true, null);
		
		printInspectionProgress("5");
		
		if (DOWNLOADS_DIR_EXISTS) {
			namesOfJarFilesInDownloadsArr = new FileSearch
					(new ArrayList<>(Collections.singletonList("jar")), null)
					.searchFiles(DOWNLOADS_DIR, false, true, null);
			
			namesOfExeFilesInDownloadsArr = new FileSearch
					(new ArrayList<>(Collections.singletonList("exe")), null)
					.searchFiles(DOWNLOADS_DIR, false, true, null);	
		} else {
			String error = "slo�ka sta�en� nebyla nalezena/neexistuje.";
			namesOfJarFilesInDownloadsArr = new ArrayList<>(Collections.singletonList(error));
			namesOfExeFilesInDownloadsArr = new ArrayList<>(Collections.singletonList(error));
			errors.add(error);
		}
		
		if (DESKTOP_DIR_EXISTS) {
			namesOfFilesOnDesktopArr = new FileSearch
					(new ArrayList<>(Collections.singletonList("jar")), null)
					.searchFiles(DESKTOP_DIR, false, true, null);	
		} else {
			String error = "Plocha nebyla nalezena.";
			namesOfFilesOnDesktopArr = new ArrayList<>(Collections.singletonList(error));
			errors.add(error);
		}
		//END
		
		
		printInspectionProgress("6");
		
		String namesOfVersionsDirFiles =
				convertArrayToString(namesOfFilesInVersionsArr, WORD_SEPARATOR);
		
		String namesOfMinecraftDirFiles =
				convertArrayToString(namesOfFilesInMinecraftArr, WORD_SEPARATOR);

		String namesOfDownloadsJarFiles =
				convertArrayToString(namesOfJarFilesInDownloadsArr, WORD_SEPARATOR);
		
		String namesOfDownloadsExeFiles =
				convertArrayToString(namesOfExeFilesInDownloadsArr, WORD_SEPARATOR);
		
		String namesOfDesktopJarFiles =
				convertArrayToString(namesOfFilesOnDesktopArr, WORD_SEPARATOR);
		
		printInspectionProgress("7");
		
		
		
		//now find keywords
		Thread tDownloads = findKeywordsInDir(DOWNLOADS_DIR, fileKeywords, null);
		Thread tDesktop = findKeywordsInDir(DESKTOP_DIR, fileKeywords, null);
		
		printInspectionProgress("8");
		
		if (DOWNLOADS_DIR_EXISTS) {
			tDownloads.start();	
		}
		if (DESKTOP_DIR_EXISTS) {
			tDesktop.start();	
		}
		
		File dirToSearch = /*ROAMING_DIR_EXISTS ? ROAMING_DIR : */MINECRAFT_DIR;
		player.addFoundHacks(
				new FileSearch((String)null, fileKeywords)
				.searchFiles(dirToSearch,
							true,
							true,
							new ArrayList<>(Collections.singletonList("assets")))
		);
		
		printInspectionProgress("9");
		try {
			if (tDownloads.isAlive()) {
				tDownloads.join();
			}
			
			if (tDesktop.isAlive()) {
				tDesktop.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			interruptInspection("Nastala chyba p�i p�ipojov�n� vl�ken.", false, e, true);
		}
		
		printInspectionProgress("10");
		String foundHacksStr = convertArrayToString(player.getFoundHacks(), WORD_SEPARATOR);
		
		currentEpochMillis = Instant.now().toEpochMilli();

		if (LOGS_DIR_EXISTS) {
			int logLinesContainingKeywordCounter = 0;
			
			for (File log : getLogs()) {
				long lastModifEpochMillis = log.lastModified();
				if (getMillisDiff(currentEpochMillis, lastModifEpochMillis, TimeUnit.DAYS)
						< MAX_AGE_OF_LOGS_TO_INSPECT_DAYS) {
					
					if (log.getName().contains("debug")) {
						continue;
					}
					
					ArrayList<String> logLinesContainingKeyword =
							getKeywordsContainedInLog(log, logKeywords, true);
					
					if (!logLinesContainingKeyword.isEmpty()) {
						logLinesContainingKeywordCounter += logLinesContainingKeyword.size();
						if (logLinesContainingKeywordCounter >= MAIL_LOGS_KEYWORDS_LINES) {
							break;
						}
						
						player.addLogLinesContainingKeyword(logLinesContainingKeyword);
					}
				}
			}
			
		} else {
			String error = "slo�ka s logy nebyla nalezena";
			errors.add(error);
		}

		
		StringBuilder latestLogContent = new StringBuilder();
		if (LATEST_LOG_EXISTS) {
			latestLogContent.append(cutString(FileUtils.convertFileContentToString(LATEST_LOG), MAIL_LATEST_LOG_LINES));
			try {
				FileUtils.createZip(LATEST_LOG, LATEST_ZIP.toString(), null);
			} catch (Exception e) {
				errors.add("Nepoda�ilo se zazipovat latest log.");
				e.printStackTrace();
			}
		} else {
			latestLogContent.append("latest log nebyl nazelen/neexistuje");
		}
		
		 
		player.setTotalInspectionsNumber(
				Short.parseShort(getLine(txtInfoStr, 1)
					.replace("\\D+",""))
			);
		
		printInspectionProgress("11W");
		
		synchronized (this) {
			while (!main.isInspectionRunning()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					interruptInspection("Nastala chyba p�i synchronizov�n� vl�ken.",
							true, e, true);
				}	
			}
		}
		
		player.addHackerIndications(analyseResults(namesOfFilesInVersionsArr));
		
		short totalInspectionsNumber = player.getTotalInspectionsNumber();
		player.setTotalInspectionsNumber(++totalInspectionsNumber);
		
		printInspectionProgress("12");
		String results = "verze programu: "
					+ Constants.PROGRAM_VERSION + "<br><br>"
				
				+ "<b>Jm�no hr��e: </b> "
					+ player.getName() + "<br><br>"
				
				+ "<b>ov��en� jm�na: </b>"
					+ (player.isNameWrong() ? "Za toto jm�no pravd�podobn� nebylo na tomto PC za posledn�ch "
					+ MAX_AGE_OF_LOGS_TO_INSPECT_DAYS + " dn� hr�no!" : "jm�no je pravd�podobn� spr�vn�") + "<br><br>"
				
				+ "<b>pravd�podobn� hacker: "
					+ (player.isHacker() ? "ano</b> - d�vody: " + "<br>" + player.getHackerIndications() : "ne</b>") + "<br><br>"
				
				+ "<b>celkov� po�et kontrol proveden�ch na tomto PC: </b><br>"
					+ player.getTotalInspectionsNumber() + "<br><br>"
				
				+ "<b>P�ede�l� kontrola prob�hla p�ed: </b><br>"
					+ (player.getTotalInspectionsNumber() <= 1 ? "��dn� p�ede�l� kontroly neprob�hly" :
					convertMinutesDiffToWords(timeSinceLastInspectionMins)
							+ " (" + (player.getLastInspectionDate() == null ? "datum bylo po�kozen�,"
									+ " nejsp�e zaps�no star�� verz� programu. Prob�hla oprava." :
								player.getLastInspectionDate().format(FORMATTER)) + ")") + "<br><br>"
				
				+ "<b>nalezen� soubory je� se shoduj� se jm�nem hack�: </b><br>"
					+ (player.getFoundHacks().isEmpty() ? "��dn�" : foundHacksStr) + "<br><br>"
				
				+ "<b>��dky z log� za posledn�ch " + MAX_AGE_OF_LOGS_TO_INSPECT_DAYS + " dn�, ve kter�ch byly"
					+ " nalezeny kl��ov� slova (max. " + MAIL_LOGS_KEYWORDS_LINES + "):</b><br>"
					+ (player.getLogLinesContainingKeyword().isEmpty() ? "��dn�" : convertArrayToString(player.getLogLinesContainingKeyword(), "\n")) + "<br><br>"
				
				+ "<b>n�zvy soubor� ve slo�ce versions v hloubce 1: </b><br>"
					+ (namesOfVersionsDirFiles.isEmpty() ? "��dn�" : namesOfVersionsDirFiles) + "<br><br>"
				
				+ "<b>N�zvy v�ech \"jar\" soubor� ve slo�ce sta�en�: </b><br>"
					+ (namesOfDownloadsJarFiles.isEmpty() ? "��dn�" : namesOfDownloadsJarFiles) + "<br><br>"
				
				+ "<b>N�zvy v�ech \"exe\" soubor� ve slo�ce sta�en�: </b><br>"
					+ (namesOfDownloadsExeFiles.isEmpty() ? "��dn�" : namesOfDownloadsExeFiles) + "<br><br>"
				
				+ "<b>N�zvy v�ech \"jar\" soubor� na plo�e: </b><br>"
					+ (namesOfDesktopJarFiles.isEmpty() ? "��dn�" : namesOfDesktopJarFiles) + "<br><br>"
				
				+ "<b>N�zvy soubor� ve slo�ce .minecraft ve hloubce 1: </b><br>"
					+ namesOfMinecraftDirFiles + "<br><br>"
				
				+ "<b>slo�ka versions naposledy upravov�na p�ed: </b><br>" 
					+ convertMinutesDiffToWords(lastVersionsModifInMins)
					+ " (" + FileUtils.getLastModificationDate(VERSIONS_DIR).format(FORMATTER) + ")" + "<br><br>"
				
				+ "<b>Chyby p�i  kontrole: </b><br>"
					+ (errors.isEmpty() ? "��dn�" : errors) + "<br><br>"
					
				+ "<b>Ostatn� chyby: </b><br>"
					+ (Main.globalErrors.isEmpty() ? "��dn�" : errors);
		
		printInspectionProgress("13");
		
		Platform.runLater(() -> main.changeInspectionState("Odes�l�n� v�sledk�"));
		
		String title = "Kontrola hr��e " + player.getName();
		String content;
		if (LATEST_LOG_EXISTS) {
			content = results + "<br><br>----------------------------------------------------------------------------"
					+ "<br><b>Prvn�ch " + MAIL_LATEST_LOG_LINES + " ��dk� Nejnov�j��ho logu:"
					+ " </b><br><br>" + latestLogContent;	
		} else {
			content = results + "<br><br>----------------------------------------------------------------------------"
					+ "<br><b>Latest log nebyl nalezen/neexistuje. </b>";
		}
		
		String error = null;
		
		if (Constants.mode == Mode.BASICLAND && UpdateManager.isProgramInIDE()) {
			System.out.println("title: " + title);
			System.out.println("content: " + content);
		} else {
			if (LATEST_LOG_EXISTS) {
				error = Email.sendMail(main.getCurrentServer().getMail(), title,
						content, LATEST_ZIP.getPath(), "latest_log.zip");
			} else {
				error = Email.sendMail(main.getCurrentServer().getMail(), title,
						content);
			}
		}
		
		if (error != null) {
			interruptInspection(error, true, null, false);
		}
		
		updatePreviousInspectionsInfo(INFO_TXT, player.getTotalInspectionsNumber(),
				LocalDateTime.now().format(FORMATTER));
		
		printInspectionProgress("14F");
		
		Platform.runLater(() -> main.showInspectionCompletedScreen(errors));
		System.out.println("Inspection done");
	}
	
	public static void writeInitialInfo() {
		try {
			FileUtils.writeToFile(INFO_TXT, INITIAL_INFO);
		} catch (Exception e) {
			e.printStackTrace();
			Inspection.interruptInspection("nelze zapsat informace do textov�ho souboru",
					true, e, true);
		}
	}
	
	private ArrayList<String> analyseResults(ArrayList<String> namesOfFilesInVersionsArr) {
		ArrayList<String> hackerIndications = new ArrayList<>();
		
		boolean versionNamesAreNotStandard = false;
		ArrayList<String> namesOfNotStandardDirs = new ArrayList<>();
		for (String dirName : namesOfFilesInVersionsArr) {
			if (!dirName.matches(".*\\d.*")) {
				versionNamesAreNotStandard = true;
				namesOfNotStandardDirs.add(dirName);
			}
		}
		
		if (versionNamesAreNotStandard) {
			hackerIndications.add("n�zev/n�zvy Minecraft verze ve slo�ce versions je/jsou nestandardn�. (" +
			convertArrayToString(namesOfNotStandardDirs, WORD_SEPARATOR) + ")");
		}

		if (!player.getFoundHacks().isEmpty()) {
			hackerIndications.add("Byly nalezeny soubory, kter� jsou pravd�podobn� hacky.");
		}
		
		if (!player.getLogLinesContainingKeyword().isEmpty()) {
			hackerIndications.add("Byla nalezena podez�el� kl��ov� slova v log souborech.");
		}
		
		if (player.getTotalInspectionsNumber() > 2) {
			hackerIndications.add(player.getName() + " byl celkov� pro�et�ov�n >2x.");
		}
		
		if (player.getTimeSinceLastInspection(TimeUnit.DAYS) < 5) {
			hackerIndications.add(player.getName() + " byl za posledn�ch 5 dn� pro�et�ov�n >=2x.");
		}
		
		return hackerIndications;
	}
	
	public StringBuilder cutString(String text, int maxNumberOfLines) {
		String[] lines = text.split("\\r?\\n");
		StringBuilder allLines = new StringBuilder();

		for (int i = 0; i < maxNumberOfLines && i < lines.length; ++i) {
			allLines.append(lines[i]).append("\n");
		}
		
		return allLines;
	}
	
	public void supplyData(String playerName, boolean probablyWrongName) {
		player.setName(playerName);
		player.setNameWrong(probablyWrongName);
	}
	
	private Thread findKeywordsInDir(File dir, ArrayList<String> keywords, ArrayList<String> subDirsToSkip) {
		return new Thread(() -> player.addFoundHacks(
				new FileSearch((String)null, keywords)
				.searchFiles(dir, true, true, subDirsToSkip)));
	}
	
	private void loadKeywords() {
		if (!DOWNLOAD_KEYWORDS) {
			setBackupKeywords();
			return;
		}

		System.out.println("downloading keywords");
		File fileWithKeywords = new File(Constants.OWN_DIR.getPath() + "\\keywords.txt");
		ArrayList<String> keywords = new ArrayList<>();
		ArrayList<String> logKeywords = new ArrayList<>();
		
		try {
		    FileUtils.download(new URL(URL_TO_KEYWORDS_STR), fileWithKeywords.getPath());
		    
		    String downloadedKeywords = FileUtils.convertFileContentToString(fileWithKeywords);
			FileUtils.deleteFile(fileWithKeywords);
			//a check if the website from where the keywords are downloaded is up and that the downloaded file is really
			//a text file with the keywords.
			if (!downloadedKeywords.contains("KEYWORDS")) {
				setBackupKeywords();
				return;
			}
			
			boolean iteratingOverLogKeywords = false;
			for (String radek : getLines(downloadedKeywords)) {
				if (radek.matches("") || radek.contains("//")) {
					continue;
				}
				
				if (!radek.matches("KEYWORDS")) {
					if (radek.matches("LOG_KEYWORDS")) {
						iteratingOverLogKeywords = true;
						continue;
					}
					if (!iteratingOverLogKeywords) {
						keywords.add(radek.toLowerCase().trim());	
					} else {
						logKeywords.add(radek.toLowerCase().trim());
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Chyba p�i stahov�n� kl��ov�ch slov, pou�ity z�lo�n�");
			e.printStackTrace();
			setBackupKeywords();
			return;
		}
		this.fileKeywords = keywords;
		this.logKeywords = logKeywords;
	}
	
	private void setBackupKeywords() {
		this.fileKeywords = BACKUP_KEYWORDS;
		this.logKeywords = BACKUP_LOG_KEYWORDS;
	}
	
	private void updatePreviousInspectionsInfo(File file, short totalInspectionsNumber, String currentDate) {
		String updatedInfo = totalInspectionsNumber + "\n" + currentDate;
		try {
			FileUtils.writeToFile(file, updatedInfo);
		} catch (Exception e) {
			e.printStackTrace();
			errors.add("nelze zapsat datum a celkov� po�et kontrol do textov�ho souboru.");
		}
	}
	
	private String convertArrayToString(ArrayList<String> array, String separator) {
		StringBuilder finalString = new StringBuilder();

		for (String item : array) {
			finalString.append(item).append(separator);
		}
		
		if (finalString.length() != 0) {
			if (finalString.substring((finalString.length() - 1) -(separator.length() - 1)).equals(separator)) {
				finalString.delete((finalString.length() - 1) - (separator.length() - 1), (finalString.length() - 1));
			}	
		}
		return finalString.toString();
	}
	
	private String convertArrayToString(List<String> array, String separator) {
		return convertArrayToString(new ArrayList<>(array), separator);
	}


	
	private ArrayList<String> getKeywordsContainedInLog(File log, ArrayList<String> keywords, boolean returnWholeLine) {
		ArrayList<String> containedKeywords = new ArrayList<>();
		if (!log.exists()) {
			return containedKeywords;
		}
		
		try {
			FileInputStream stream = new FileInputStream(log);
			Reader reader;
			
			if (log.getName().endsWith(".gz")) {
				GZIPInputStream gzipInputStream = new GZIPInputStream(stream);
				reader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8);
			} else {
				reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			}
			BufferedReader br = new BufferedReader(reader);
			
			String line;
			short lineCounter = 0;
			while ((line = br.readLine()) != null && (++lineCounter <= MAX_INSPECTED_LOG_LINES)) {
	            if (!line.contains("[CHAT]")) {
	            	for (String keyword : keywords) {
	            		if (line.contains(keyword)) {
	            			containedKeywords.add(returnWholeLine ? line : keyword);
	            		}
	            	}
	            }
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return containedKeywords;
	}
	
	
	
	private String getLine(String text, int linePosition) {
        String[] lines = text.split("\\r?\\n");
        return lines[linePosition - 1];
	}
	
	private ArrayList<String> getLines(String text) {
		return new ArrayList<>(Arrays.asList(text.split("\\r?\\n")));
	}
	
	public String convertMinutesDiffToWords(long diff) {
		String diffByWords = "";
		short oneDayInMins = 1440;
		
		if (diff < 60) {
			 diffByWords = diff + (diff == 1 ? " minutou.": " minutami.");
		}
		
		else if (diff >= 60 && diff < oneDayInMins) {
			long diffInHours = diff/60;
			long remainingMins = diff%60;
			diffByWords = diffInHours + (diffInHours == 1 ? " hodinou ": " hodinami ") + " a "
				+ remainingMins + (remainingMins == 1 ? " minutou.": " minutami.");
		}
		
		else if (diff >= oneDayInMins) {
			long diffInDays = (diff/60)/24;
			long remainingHours = (diff/60)%24;
			diffByWords = diffInDays + (diffInDays == 1 ? " dnem " : " dny ")
				+ " a " + remainingHours + (remainingHours == 1 ? " hodinou." : " hodinami.");
		}
		return diffByWords;
	}
	
	public static ArrayList<File> getLogs() {
		if (logs.isEmpty()) {
			ArrayList<String> pathsToLogs = getPathsToLogs();
			for (String pathToLog : pathsToLogs) {
				logs.add(new File(pathToLog));
			}
		}
		return logs;
	}
	
	public static ArrayList<String> getPathsToLogs() {
		return new FileSearch
				(new ArrayList<>(Arrays.asList("gz", "log")), null)
				.searchFiles(LOGS_DIR, true, false, null);
	}
	
	public boolean isNameValid(String name) {
		String nameRegex = "\\b" + name + "\\b";
		Pattern namePattern = Pattern.compile(nameRegex);
		long currentEpochMillis = Instant.now().toEpochMilli();
		
		for (File log : Inspection.getLogs()) {
			long lastModifEpochMillis = log.lastModified();
			long timeFromLastModificationDays =
					getMillisDiff(currentEpochMillis, lastModifEpochMillis, TimeUnit.DAYS);
			if (timeFromLastModificationDays > MAX_AGE_OF_LOGS_TO_INSPECT_DAYS) {
				continue;
			}
			
			try {
				FileInputStream stream = new FileInputStream(log);
				BufferedReader br;
				Reader reader;
				
				if (log.getName().endsWith(".gz")) {
					GZIPInputStream gzipInputStream = new GZIPInputStream(stream);
					reader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8);
				} else {
					reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
				}
				br = new BufferedReader(reader);
				
				String line;
				int lineCounter = 0;
				while ((line = br.readLine()) != null && (++lineCounter < MAX_INSPECTED_LOG_LINES_NAME)) {
		            if (!line.contains("[CHAT]")) {
		            	if (namePattern.matcher(line).find()) {
		            		return true;
		            	}
		            }
				}
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	private long getMillisDiff(long time1, long time2, TimeUnit unit) {
		long diffMillis = time1 - time2;
		switch (unit) {
		case NANOSECONDS: return TimeUnit.MILLISECONDS.toNanos(diffMillis);
		case MICROSECONDS: return TimeUnit.MILLISECONDS.toMicros(diffMillis);
		case MILLISECONDS: return diffMillis;
		case SECONDS: return TimeUnit.MILLISECONDS.toSeconds(diffMillis);
		case MINUTES: return TimeUnit.MILLISECONDS.toMinutes(diffMillis);
		case HOURS: return TimeUnit.MILLISECONDS.toHours(diffMillis);
		case DAYS: return TimeUnit.MILLISECONDS.toDays(diffMillis);
		}
		return -1;
	}
	
	public static void interruptInspection(String error, boolean showToUser, Exception e, boolean send) {
		System.out.println("inspection interrupted");
		Platform.runLater(() -> {
			try {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Chyba");
				alert.setHeaderText("Do�lo k chyb�, kter�"
						+ " zabr�nila norm�ln� funkci programu. ��dn� data nebudou odesl�na.");

				if (showToUser) {
					alert.setContentText("k�d: " + Main.inspection.progress + " | Chyba: " + error);
				}

				alert.showAndWait();
				Main.stage.hide();
				
				if (send) {
					Inspection.sendInfoAboutError(error, e);	
				}
			} catch (Exception ex) {
				System.err.println("something went wrong in interruptInspection()");
				e.printStackTrace();
			} finally {
				Main.endProgram();
			}
		});
	}
	
	private static void sendInfoAboutError(String error, Exception e) {
		try {
			System.out.println("sending informations about error.");

			Email.sendMail(new Debug().getMail(), "chyba " + " (v" + Constants.PROGRAM_VERSION + ")",
					error + "<br>"
			+ "progress: " + Main.inspection.progress + "<br>"
			+ "global errors: " + Main.globalErrors + "<br>"
			+ "errors: " + Main.inspection.errors + "<br><br>"
			+ "Stack trace: " + e);
		} catch (Exception e1) {
			System.err.println("Nepoda�ilo se odeslat informace o chyb�.");
			e1.printStackTrace();
		}
	}
	
	private void printInspectionProgress(String progress) {
		this.progress = progress + "-I";
		System.out.println(progress + "-I");
	}

	@Override
	public void run() {
		doInspection();	
	}
}