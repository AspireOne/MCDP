package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


import com.gmail.matejpesl1.mc_cheat_detection.Main.Mode;
import com.gmail.matejpesl1.servers.Debug;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

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
	
	public static final String URL_TO_KEYWORDS_STR =
			"https://drive.google.com/uc?export=download&id=1h1RyP_m1j29jRES-TWj55Vg47BbppSoF";
	
	public static final String PATH_ROOT = System.getProperty("user.home");
	public static final File ROOT_DIR = new File(PATH_ROOT);
	public static final File OWN_DIR = new File(PATH_ROOT +  "\\vysledky");
	public static final File LOGS_DIR = new File(PATH_ROOT +  "\\AppData\\Roaming\\.minecraft\\logs");
	public static final File ROAMING_DIR = new File(PATH_ROOT +"\\AppData\\Roaming");
	public static final File DESKTOP_DIR = new File(PATH_ROOT + "\\Desktop");
	public static final File VERSIONS_DIR = new File(PATH_ROOT + "\\AppData\\Roaming\\.minecraft\\versions");
	public static final File MINECRAFT_DIR = new File(PATH_ROOT + "\\AppData\\Roaming\\.minecraft");
	public static final File DOWNLOADS_DIR = new File(Paths.get(PATH_ROOT, "Downloads").toString());
	public static final File LATEST_ZIP = new File(OWN_DIR + "\\latest.zip");
	public static final File LATEST_LOG = new File(LOGS_DIR.getPath() + "\\latest.log");
	public static final File TXT_PREVIOUS_INSPECTIONS_INFO = 
			new File(OWN_DIR.getPath() + "\\predesleKontrolyInfo.txt");
	
	public static final boolean LATEST_LOG_EXISTS = LATEST_LOG.exists();
	public static final boolean LOGS_DIR_EXISTS = LOGS_DIR.exists();
	public static final boolean DESKTOP_DIR_EXISTS = DESKTOP_DIR.exists();
	public static final boolean DOWNLOADS_DIR_EXISTS = DOWNLOADS_DIR.exists();
	public static final boolean ROAMING_DIR_EXISTS = ROAMING_DIR.exists();
	
	public ArrayList<String> errors;
	private ArrayList<String> keywords;
	private ArrayList<String> logKeywords;
	private final ArrayList<String> pathsToLogs;
	private List<String> foundHacksName;
	private boolean probablyWrongName;
	private boolean probableHacker;
	private String lastInspectionDate;
	private String hackerIndicators;
	private String playerName;
	private int hackerIndicatorsCounter;
	private int totalInspectionsNumber;
	private Main main;
	public String progress;
	
	public static final String WORD_SEPARATOR = " | ";
	public static final String initialInfo = 0 + "\n" + 0;
	public static final int MAIL_LATEST_LOG_LINES = 500;
	public static final int MAIL_LOGS_KEYWORDS_LINES = 100;
	public static final int MAX_AGE_OF_LOGS_TO_INSPECT_DAYS = 25;
	public static final DateTimeFormatter FORMATTER = 
			DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
	
	public Inspection(Main main) {
		printInspectionProgress("0.5");
		this.main = main;
		errors = new ArrayList<>();
		foundHacksName = Collections.synchronizedList(new ArrayList<>());
		pathsToLogs = getPathsToLogs();
	}
	
	public void doInspection() {
		printInspectionProgress("1");
		LATEST_ZIP.deleteOnExit();
		
		if (!OWN_DIR.exists()) {
			createOwnDir();
		}
		
		loadKeywords();
		pathsToLogs.addAll(getPathsToLogs());
		
		String predesleKontrolyInfoStr = 
				convertLogContentToString(TXT_PREVIOUS_INSPECTIONS_INFO.getPath());
		
		lastInspectionDate = getLine(predesleKontrolyInfoStr, 2);
		long timeSinceLastInspectionMins = 0;
		//progress
		printInspectionProgress("2");
		
		if (!lastInspectionDate.equals("0")) {
			try {
				timeSinceLastInspectionMins =
						getDateDiff(LocalDateTime.now()
								.format(FORMATTER), lastInspectionDate, TimeUnit.MINUTES);	
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				writeToFile(TXT_PREVIOUS_INSPECTIONS_INFO, initialInfo);
			} catch (NullPointerException e) {
				e.printStackTrace();
				timeSinceLastInspectionMins = -1;
				errors.add("textový soubor, který zaznamenává pøedešlé kontroly, nebyl nalezen.");
				createOwnDir();
			}
		}
		//progress
		printInspectionProgress("3");
		
		if (Main.mode != Mode.DEBUG && Main.mode != Mode.DEMO) {
			if (timeSinceLastInspectionMins < 3 && timeSinceLastInspectionMins >= 1) {
				interruptInspection("Doba od poslední kontroly je moc krátká, zkuste to pozdìji.", true, null, false);
			}	
		}
		
		long lastVersionsModifInMins =
				getDateDiff(LocalDateTime.now().format(FORMATTER),
						getLastModificationDate(VERSIONS_DIR), TimeUnit.MINUTES);
		
		if (lastVersionsModifInMins < 30 && lastVersionsModifInMins >= 0) {
			addHackerIndicator("Složka versions byla upravována pøed ménì než 30 minutami");
		}
		printInspectionProgress("4");
		
		ArrayList<String> namesOfFilesOnDesktopArr = new ArrayList<>();
		ArrayList<String> namesOfFilesInVersionsArr = new ArrayList<>();
		ArrayList<String> namesOfFilesInMinecraftArr =  new ArrayList<>();
		ArrayList<String> namesOfJarFilesInDownloadsArr = new ArrayList<>();
		ArrayList<String> namesOfExeFilesInDownloadsArr = new ArrayList<>();
		
		namesOfFilesInVersionsArr = new FileSearch
				((String)null, null).searchFiles(VERSIONS_DIR, false, true, null);
		
		namesOfFilesInMinecraftArr = new FileSearch
				((String)null, null).searchFiles(MINECRAFT_DIR, false, true, null);
		
		printInspectionProgress("5");
		
		if (DOWNLOADS_DIR_EXISTS) {
			namesOfJarFilesInDownloadsArr = new FileSearch
					(new ArrayList<String>(Collections.singletonList("jar")), null)
					.searchFiles(DOWNLOADS_DIR, false, true, null);
			
			namesOfExeFilesInDownloadsArr = new FileSearch
					(new ArrayList<String>(Collections.singletonList("exe")), null)
					.searchFiles(DOWNLOADS_DIR, false, true, null);	
		} else {
			String error = "složka stažené nebyla nalezena/neexistuje.";
			namesOfJarFilesInDownloadsArr.add(error);
			errors.add(error);
		}
		
		if (DESKTOP_DIR_EXISTS) {
			namesOfFilesOnDesktopArr = new FileSearch
					(new ArrayList<String>(Collections.singletonList("jar")), null)
					.searchFiles(DESKTOP_DIR, false, true, null);	
		} else {
			String error = "Plocha nebyla nalezena.";
			namesOfFilesOnDesktopArr.add(error);
			errors.add(error);
		}
		
		printInspectionProgress("6");
		
		String namesOfMinecraftFiles =
				convertArrayToString(namesOfFilesInVersionsArr, " | ");
		
		String foundKeywordsInMinecraft =
				convertArrayToString(namesOfFilesInMinecraftArr, " | ");

		String namesOfDownloadsJarFiles =
				convertArrayToString(namesOfJarFilesInDownloadsArr, " | ");
		
		String namesOfDownloadsExeFiles =
				convertArrayToString(namesOfExeFilesInDownloadsArr, " | ");
		
		String namesOfDesktopJarFiles =
				convertArrayToString(namesOfFilesOnDesktopArr, " | ");
		
		printInspectionProgress("7");
		
		
		
		//now find keywords
		Thread tDownloads = findKeywordsInDir(DOWNLOADS_DIR, keywords, null);
		Thread tDesktop = findKeywordsInDir(DESKTOP_DIR, keywords, null);
		
		printInspectionProgress("8");
		
		if (DOWNLOADS_DIR_EXISTS) {
			tDownloads.start();	
		}
		if (DESKTOP_DIR_EXISTS) {
			tDesktop.start();	
		}
		
		File dirToSearch = /*ROAMING_DIR_EXISTS ? ROAMING_DIR : */MINECRAFT_DIR;
		ArrayList<String> foundKeywords =
				new FileSearch((String)null, keywords)
				.searchFiles(dirToSearch, true, true,
						new ArrayList<String>(Collections.singletonList("assets")));
		foundHacksName.addAll(foundKeywords);	
		
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
			interruptInspection("Nastala chyba pøi pøipojování vláken.", false, e, true);
		}
		
		printInspectionProgress("10");
		String foundHackKeywordsStr = convertArrayToString(foundHacksName, " | ");
		
		String logLinesContainingKeyword = null;
		int logLinesContainingKeywordCounter = 0;
		if (LOGS_DIR_EXISTS) {
			for (String pathToLog : pathsToLogs) {
				if (getDateDiff(LocalDateTime.now().format(FORMATTER),
						getLastModificationDate
						(new File(pathToLog)), TimeUnit.DAYS) < MAX_AGE_OF_LOGS_TO_INSPECT_DAYS) {
					if (new File(pathToLog).getName().contains("debug")) {
						continue;
					}
					
					String logLineWithKeyword =
							convertArrayToString
							(getKeywordsContainedInLog
									(pathToLog, logKeywords, true), "\n");
					if (logLineWithKeyword != null) {
						++logLinesContainingKeywordCounter;
						if (logLinesContainingKeywordCounter >= MAIL_LOGS_KEYWORDS_LINES) {
							break;
						}
						logLinesContainingKeyword =
								(logLinesContainingKeyword == null ? "" :
									logLinesContainingKeyword) + logLineWithKeyword;
					}
				}
			}	
		} else {
			String error = "složka s logy nebyla nalezena";
			logLinesContainingKeyword = "nebyla nalezena složka s logy";
			errors.add(error);
		}

		printInspectionProgress("11 - waiting");

		boolean versionNamesAreNotStandard = false;
		ArrayList<String> namesOfNotStandardDirs = new ArrayList<>();
		for (String dirName : namesOfFilesInVersionsArr) {
			if (!dirName.matches(".*\\d.*")) {
				versionNamesAreNotStandard = true;
				namesOfNotStandardDirs.add(dirName);
			}
		}
		
		if (versionNamesAreNotStandard) {
			probableHacker = true;
			addHackerIndicator("název/názvy Minecraft verze ve složce versions je/jsou nestandardní. (" +
			convertArrayToString(namesOfNotStandardDirs, " | ") + ")");
		}

		if (foundHacksName.size() > 0) {
			probableHacker = true;
			addHackerIndicator("Byly nalezeny soubory, které jsou pravdìpodobnì hacky.");
		}
		
		if (logLinesContainingKeyword != null) {
			probableHacker = true;
			addHackerIndicator("Byla nalezena podezøelá klíèová slova v log souborech.");
		}
		
		synchronized (this) {
			while (!Main.isInspectionRunning()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					interruptInspection("Nastala chyba pøi synchronizování vláken.", true, e, true);
				}	
			}
		}

		printInspectionProgress("12");
		
		totalInspectionsNumber = Integer.parseInt(getLine(predesleKontrolyInfoStr, 1).replace("\\D+",""));
		++totalInspectionsNumber;
		
		String results = "verze programu: "
					+ Main.PROGRAM_VERSION + "<br><br>"
				
				+ "<b>Jméno hráèe: </b> "
					+ playerName + "<br><br>"
				
				+ "<b>ovìøení jména: </b>"
					+ (probablyWrongName ? "Za toto jméno pravdìpodobnì nebylo na tomto PC za posledních "
					+ MAX_AGE_OF_LOGS_TO_INSPECT_DAYS + " dní hráno!" : "jméno je pravdìpodobnì správné") + "<br><br>"
				
				+ "<b>pravdìpodobný hacker: "
					+ (probableHacker ? "ano</b> - dùvody: " + "<br>" + hackerIndicators : "ne</b>") + "<br><br>"
				
				+ "<b>celkový poèet kontrol provedených na tomto PC: </b><br>"
					+ totalInspectionsNumber + "<br><br>"
				
				+ "<b>Pøedešlá kontrola probìhla pøed: </b><br>"
					+ (totalInspectionsNumber <= 1 ? "žádné pøedešlé kontroly neprobìhly" :
					convertDateDiffToWords(timeSinceLastInspectionMins) + " (" + lastInspectionDate + ")") + "<br><br>"
				
				+ "<b>nalezené soubory jež se shodují se jménem hackù: </b><br>"
					+ (foundHacksName.isEmpty() ? "žádné" : foundHackKeywordsStr) + "<br><br>"
				
				+ "<b>Øádky z logù za posledních " + MAX_AGE_OF_LOGS_TO_INSPECT_DAYS + " dní, ve kterých byly"
					+ " nalezeny klíèové slova (max. " + MAIL_LOGS_KEYWORDS_LINES + "):</b><br>"
					+ (logLinesContainingKeyword == null ? "žádné" : logLinesContainingKeyword) + "<br><br>"
				
				+ "<b>názvy souborù ve složce versions v hloubce 1: </b><br>"
					+ (namesOfMinecraftFiles == null ? "žádné" : namesOfMinecraftFiles) + "<br><br>"
				
				+ "<b>Názvy všech \"jar\" souborù ve složce stažené: </b><br>"
					+ (namesOfDownloadsJarFiles == null ? "žádné" : namesOfDownloadsJarFiles) + "<br><br>"
				
				+ "<b>Názvy všech \"exe\" souborù ve složce stažené: </b><br>"
					+ (namesOfDownloadsExeFiles == null ? "žádné" : namesOfDownloadsExeFiles) + "<br><br>"
				
				+ "<b>Názvy všech \"jar\" souborù na ploše: </b><br>"
					+ (namesOfDesktopJarFiles == null ? "žádné" : namesOfDesktopJarFiles) + "<br><br>"
				
				+ "<b>Názvy souborù ve složce .minecraft ve hloubce 1: </b><br>"
					+ foundKeywordsInMinecraft + "<br><br>"
				
				+ "<b>složka versions naposledy upravována pøed: </b><br>" 
					+ convertDateDiffToWords(lastVersionsModifInMins)
					+ " (" + getLastModificationDate(VERSIONS_DIR) + ")" + "<br><br>"
				
				+ "<b>Chyby pøi  kontrole: </b><br>"
					+ (errors.isEmpty() ? "žádné" : errors) + "<br><br>"
					
				+ "<b>Ostatní chyby: </b><br>"
					+ (Main.globalErrors.isEmpty() ? "žádné" : errors);

		
		
		String latestLogContent = "latest log nebyl nazelen/neexistuje";
		if (LATEST_LOG_EXISTS) {
			latestLogContent =
					cutString(convertLogContentToString(LATEST_LOG.getPath()), MAIL_LATEST_LOG_LINES);
			try {
				createZip(LATEST_LOG, LATEST_ZIP.toString(), null);
			} catch (Exception e) {
				errors.add("Nepodaøilo se zazipovat latest log.");
				e.printStackTrace();
			}
		}
		
		printInspectionProgress("13");
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				   main.changeInspectionState("Odesílání výsledkù");
			}
		});
		
		String title = "Kontrola hráèe " + playerName;
		String content = "";
		if (LATEST_LOG_EXISTS) {
			content = results + "<br><br>----------------------------------------------------------------------------"
					+ "<br><b>Prvních " + MAIL_LATEST_LOG_LINES + " øádkù Nejnovìjšího logu:"
					+ " </b><br><br>" + latestLogContent;	
		} else {
			content = results + "<br><br>----------------------------------------------------------------------------"
					+ "<br><b>Latest log nebyl nalezen/neexistuje. </b>";
		}
		
		String chyba = null;
		
		if (LATEST_LOG_EXISTS) {
			chyba = Email.sendMail(main.getCurrentServer().getMail(), title,
					content, LATEST_ZIP.getPath(), "latest_log.zip");
		} else {
			chyba = Email.sendMail(main.getCurrentServer().getMail(), title,
					content);
		}
		
		if (chyba != null) {
			interruptInspection(chyba, true, null, false);
		}
		
		updatePreviousInspectionsInfo(TXT_PREVIOUS_INSPECTIONS_INFO, totalInspectionsNumber,
				LocalDateTime.now().format(FORMATTER));
		
		printInspectionProgress("14 - FINAL");
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				main.showInspectionCompletedScreen(errors);
			}
		});
		System.out.println("Inspection done");
	}
	
	public void createOwnDir() {
		boolean dirCreated = OWN_DIR.mkdir();
		if (!dirCreated) {
			interruptInspection("Nastala chyba pøi vytváøení složky.", false, null, true);
		}
		
		writeToFile(TXT_PREVIOUS_INSPECTIONS_INFO, initialInfo);
		changeFileAttribute(OWN_DIR, "dos:hidden", true);
		changeFileAttribute(TXT_PREVIOUS_INSPECTIONS_INFO, "dos:hidden", true);
	}
	
	public String cutString(String text, int maxNumberOfLines) {
		String[] lines = text.split("\\r?\\n");
		String allLines = null;
		int currentLinePos = 0;
		for (String line : lines) {
			++currentLinePos;
			if (currentLinePos <= maxNumberOfLines) {
				allLines = (allLines == null ? "" : allLines + "\n") + line;
			} else {
				break;
			}	
		}
		return allLines;
	}
	
	public void createZip(File fileToZip, String zipDestination, String pass) throws Exception {
	        ZipParameters params = new ZipParameters();
	        ZipFile zipFile = new ZipFile(zipDestination);
	        params.setCompressionMethod(CompressionMethod.DEFLATE);
	        params.setCompressionLevel(CompressionLevel.ULTRA);
	        if (pass != null) {
	            params.setEncryptFiles(true);
	            params.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
	            zipFile.setPassword(pass.toCharArray());
	        }
	       
	        if (fileToZip.isFile()) {
	            zipFile.addFile(fileToZip, params);
	        } else if (fileToZip.isDirectory()) {
	            zipFile.addFolder(fileToZip, params);
	        }
	}
	
	public void supplyData(String playerName, boolean probablyWrongName) {
		this.playerName = playerName;
		this.probablyWrongName = probablyWrongName;
	}
	
	private Thread findKeywordsInDir(File dir, ArrayList<String> keywords, ArrayList<String> subDirsToSkip) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				ArrayList<String> foundFiles = new FileSearch((String)null, keywords)
						.searchFiles(dir, true, true, subDirsToSkip);
				foundHacksName.addAll(foundFiles);
			}
		};
		return thread;
	}
	
	private void loadKeywords() {
		//TODO: delete this
		if (Main.mode != Mode.DEBUG) {
			this.keywords = BACKUP_KEYWORDS;
			this.logKeywords = BACKUP_LOG_KEYWORDS;
			return;
		};
		File fileWithKeywords = new File(OWN_DIR.getPath() + "\\keywords.txt");
		ArrayList<String> keywords = new ArrayList<>();
		ArrayList<String> logKeywords = new ArrayList<>();
		
		try {
			ReadableByteChannel rbc = Channels.newChannel(new URL(URL_TO_KEYWORDS_STR).openStream()); //TODO CHANGE LINK TO NORMAL
			FileOutputStream fos = new FileOutputStream(fileWithKeywords);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		    fos.close();
		    rbc.close();
		    String downloadedKeywords = convertLogContentToString(fileWithKeywords.getPath());
			deleteFile(fileWithKeywords.getPath());
			//a check if the website from where the keywords are downloaded is up and that the downloaded file is really
			//a text file with the keywords.
			if (!downloadedKeywords.contains("KEYWORDS")) {
				this.keywords = BACKUP_KEYWORDS;
				this.logKeywords = BACKUP_LOG_KEYWORDS;
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
			System.out.println("Chyba pøi stahování klíèových slov");
			e.printStackTrace();
			this.keywords = BACKUP_KEYWORDS;
			this.logKeywords = BACKUP_LOG_KEYWORDS;
			return;
		}
		this.keywords = keywords;
		this.logKeywords = logKeywords;
	}
	
	private void updatePreviousInspectionsInfo(File file, int totalInspectionsNumber, String currentDate) {
		String updatedInfo = totalInspectionsNumber + "\n" + currentDate;
		writeToFile(file, updatedInfo);
	}
	
	private void writeToFile(File file, String text) {
		boolean isHidden = false;
		boolean isReadOnly = false;
		try {
			if (file.isHidden()) {
				isHidden = true;
				changeFileAttribute(file, "dos:hidden", false);
			}
			if (!file.canWrite()) {
				isReadOnly = true;
				file.setWritable(true);
			}
			
			 BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			    writer.write(text);
			    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			interruptInspection("Nastal problém pøi vytváøení textového souboru.", true, e, true);
		} finally {
			if (isHidden) {
				changeFileAttribute(file, "dos:hidden", true);
			}
			if (isReadOnly) {
				file.setWritable(false);
			}
		}
	}
	
	private static void changeFileAttribute(File file, String attributeToChange, boolean value) {
		try {
			Files.setAttribute(file.toPath(), attributeToChange, value);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	protected ArrayList<String> getContainedKeywords(ArrayList<String> textToInspect, ArrayList<String> keywords) {
		ArrayList<String> obsazeneKeywordy = new ArrayList<>();
		for (String kontrolovanyText : textToInspect) {
			for (String keyword : keywords) {
				if (kontrolovanyText.toLowerCase().contains(keyword)) {
					obsazeneKeywordy.add(kontrolovanyText);
				}
			}
		}
		return obsazeneKeywordy;
	}
	
	public void decompressGZip(String destinace, File soubor) {
		  byte[] buffer = new byte[1024];
		     try {
		    	 GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(soubor));
		    	 FileOutputStream out = new FileOutputStream(destinace);
		        int len;
		        while ((len = gzis.read(buffer)) > 0) {
		        	out.write(buffer, 0, len);
		        }
		        gzis.close();
		    	out.close();
		    } catch(IOException e) {
		       e.printStackTrace(); 
		       errors.add("nelze dekompresovat logy");
		    }
	}
	
	private void addHackerIndicator(String reason) {
		++hackerIndicatorsCounter;
		hackerIndicators =
				(hackerIndicators == null ? hackerIndicatorsCounter + ". " :
					hackerIndicators + "\n" + hackerIndicatorsCounter + ". ") + reason;
	}
	
	private String convertArrayToString(ArrayList<String> array, String separator) {
		String finalString = null;

		for (String item : array) {
			finalString = (finalString == null ? "" : finalString + separator) + item;
		}
		return finalString;
	}
	
	private String convertArrayToString(List<String> array, String separator) {
		ArrayList<String> array2 = new ArrayList<>(array);
		return convertArrayToString(array2, separator);
	}

	public void deleteFile(String filePath) throws IOException {
	    	Files.deleteIfExists(Paths.get(filePath));
	}
	
	private ArrayList<String> getKeywordsContainedInLog(String log, ArrayList<String> keywords, boolean wholeLine) {
		ArrayList<String> containedKeywords = new ArrayList<>();
		if (!new File(log).exists()) {
			return null;
		}
		
		String logText = convertLogContentToString(log);
		
        String[] lines = logText.split("\\r?\\n");
        for (String line : lines) {
            if (!line.contains("[CHAT]")) {
            	for (String keyword : keywords) {
            		if (line.contains(keyword)) {
            			containedKeywords.add(wholeLine ? line : keyword);
            		}
            	}
            }
        }
        return containedKeywords;
	}
	
	
	
	private String getLine(String text, int linePosition) {
        String[] lines = text.split("\\r?\\n");
        int i = 0;
        for (String line : lines) {
        	++i;
        	if (i == linePosition) {
        		return line;
        	}
        }
        return null;
	}
	
	private ArrayList<String> getLines(String text) {
		String[] lines = text.split("\\r?\\n");
		ArrayList<String> allLines = new ArrayList<>();
		for (String line : lines) {
			allLines.add(line);
		}
		return allLines;
	}
	
	private String getLastModificationDate(File slozka) throws NullPointerException {
		LocalDateTime dateAndTime =
			    LocalDateTime.ofInstant(Instant.ofEpochMilli(slozka.lastModified()), ZoneId.systemDefault());
		String dateAndTimeStr = dateAndTime.format(FORMATTER);
		return dateAndTimeStr;
	}
	
	private long getDateDiff(String date1, String date2, TimeUnit timeUnit) {
		LocalDateTime formattedDate1 = LocalDateTime.parse(date1, FORMATTER);
		LocalDateTime formattedDate2 = LocalDateTime.parse(date2, FORMATTER);
		long diffInMins = ChronoUnit.MINUTES.between(formattedDate2, formattedDate1);
		switch (timeUnit) {
		case NANOSECONDS: return TimeUnit.MINUTES.toNanos(diffInMins);
		case MICROSECONDS: return TimeUnit.MINUTES.toMicros(diffInMins);
		case MILLISECONDS: return TimeUnit.MINUTES.toMillis(diffInMins);
		case SECONDS: return TimeUnit.MINUTES.toSeconds(diffInMins);
		case MINUTES: return diffInMins;
		case HOURS: return TimeUnit.MINUTES.toHours(diffInMins);
		case DAYS: return TimeUnit.MINUTES.toDays(diffInMins);
		}
		return -1;
	}
	
	public String convertLogContentToString(String pathToFile) { 
	    String data = null;
	    boolean compressed = false;
	    boolean fileExists = new File(pathToFile).exists();
	    if (!fileExists) {
	    	return null;
	    }
	    
	    try {
	    	if (pathToFile.endsWith(".gz")) {
	    		compressed = true;
	    		String pathToDecompressedFile = OWN_DIR.getPath() + "\\temp_decompressed_file" + ".log"; 
	    		decompressGZip(pathToDecompressedFile, new File(pathToFile));
	    		pathToFile = pathToDecompressedFile;
	    	}
			data = new String(Files.readAllBytes(Paths.get(pathToFile)));
		} catch (IOException e) {
			e.printStackTrace();
			errors.add("Nastal interní problém pøi ètení obsahu souboru. " + progress);
		}
	    
	    if (compressed) {
	    	try {
				deleteFile(pathToFile);
			} catch (IOException e) {
				e.printStackTrace();
				errors.add("Nastal problém pøi odstraòování doèasných dekompresovaných logù." + progress);
			}
	    }
	    return data;
	  }
	
	public String convertDateDiffToWords(long diff) {
		String diffByWords = "";
		int oneDayInMins = 1440;
		
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
			long remainingHours = (diff/60) % 24;
			diffByWords = diffInDays + (diffInDays == 1 ? " dnem " : " dny ")
				+ " a " + remainingHours + (remainingHours == 1 ? " hodinou." : " hodinami.");
		}
		return diffByWords;
	}
	
	private ArrayList<String> getPathsToLogs() {
		ArrayList<String> pathsToLogs = new ArrayList<>();
			for (String pathToLog : new FileSearch
					(new ArrayList<String>(Arrays.asList("gz", "log")), null)
					.searchFiles(LOGS_DIR, true, false, null)) {
				pathsToLogs.add(pathToLog);
			}
			return pathsToLogs;
	}
	
	public boolean isNameInLogs(String name) {
		String nameRegex = "\\b" + name + "\\b";
		Pattern namePattern = Pattern.compile(nameRegex, Pattern.CASE_INSENSITIVE);
		
		String currentDate = LocalDateTime.now().format(FORMATTER);
		for (String pathToLog : pathsToLogs) {
			String lastModificationDate = getLastModificationDate(new File(pathToLog));
			long timeFromLastModification =
					getDateDiff(currentDate, lastModificationDate, TimeUnit.DAYS);
			if (timeFromLastModification > MAX_AGE_OF_LOGS_TO_INSPECT_DAYS) {
				continue;
			}
			
			String logContent = convertLogContentToString(pathToLog);
			 String[] lines = logContent.split("\\r?\\n");
		        for (String line : lines) {
		            if (!line.contains("[CHAT]")) {
		            	Matcher matcher = namePattern.matcher(line);
		            	if (matcher.find()) {
		            		return true;
		            	}
		            }
		        }
		}
		return false;
	}
	
	public static void interruptInspection(String error, boolean showToUser, Exception e, boolean send) {
		try {
			Platform.runLater(new Runnable(){
				@Override
				public void run() {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Chyba");
					alert.setHeaderText("Došlo k chybì, která"
							+ " zabránila normální funkci programu. Žádná data nebudou odeslána.");
					
					if (showToUser) {
						alert.setContentText("kód: " + Main.inspection.progress + " | Chyba: " + error);
					}				
					
					alert.showAndWait();
					Main.stage.hide();
					
					//send the error
					if (send) {
				    	try {
				    		System.out.println("sending informations about error.");
				    		
					    	Email.sendMail(new Debug().getMail(), "chyba " + " (v" + Main.PROGRAM_VERSION + ")",
					    			error + "<br>"
					    	+ "progress: " + Main.inspection.progress + "<br>"
					    	+ "global errors: " + Main.globalErrors + "<br>"
					    	+ "errors: " + Main.inspection.errors + "<br><br>"
					    	+ "Stack trace: " + e);
				    	} catch (Exception e) {
				    		System.out.println("Nepodaøilo se odeslat informace o chybì.");
				    		e.printStackTrace();
				    	}
					}
					Inspection.endProgram();
				}
			});	
		} catch (Exception ex) {
			System.out.println("something went wrong in interruptInspection()");
			e.printStackTrace();
			Inspection.endProgram();
		}
	}
	
	public static void endProgram() {
		try {
			Main.stage.hide();
			changeFileAttribute(OWN_DIR, "dos:hidden", false);
			changeFileAttribute(TXT_PREVIOUS_INSPECTIONS_INFO, "dos:hidden", false);
				try {
					byte[] previousInspectionsInfoTxtBytes = Files.readAllBytes(TXT_PREVIOUS_INSPECTIONS_INFO.toPath());	
					for(File file : OWN_DIR.listFiles()) {
						if (!Arrays.equals(Files.readAllBytes(file.toPath()), previousInspectionsInfoTxtBytes)
								&& !file.isDirectory()) {
							file.delete();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (OWN_DIR.exists() && !OWN_DIR.isHidden()) {
					changeFileAttribute(OWN_DIR, "dos:hidden", true);
					changeFileAttribute(TXT_PREVIOUS_INSPECTIONS_INFO, "dos:hidden", true);	
				}
				Platform.exit();
				System.exit(0);	
		} catch (Exception e) {
			System.out.println("something went wrong in endProgram()");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void printInspectionProgress(String progress) {
		this.progress = progress + "-I";
		System.out.println(progress + "-I");
	}

	@Override
	public void run() {
		doInspection();	
	}
}