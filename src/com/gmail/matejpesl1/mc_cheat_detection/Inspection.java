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


import com.gmail.matejpesl1.mc_cheat_detection.Main.Rezim;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class Inspection extends Thread {
	private static final ArrayList<String> BACKUP_KEYWORDS = new ArrayList<>(Arrays.asList("huzuni", "skillclient", "liquidbounce",
			"wolfram", "impact_", "aristois", "wurst", "jam", "kronos", "jigsaw", "hacked", "hackclient", "hacked-client",
			"hack-client", "pandora", "killaura", "kill-aura", "forgehax", "impact_", "impact-", "_impact", "-impact"));
	public static final String URL_TO_KEYWORDS_STR = "https://drive.google.com/uc?export=download&id=1h1RyP_m1j29jRES-TWj55Vg47BbppSoF";
	private final ArrayList<String> BACKUP_LOG_KEYWORDS;
	
	public static final String PATH_ROOT = System.getProperty("user.home");
	public static final File ROOT_FOLDER = new File(PATH_ROOT);
	public static final File LOGS_FOLDER = new File(PATH_ROOT + "\\AppData\\Roaming\\.minecraft\\logs");
	public static final File ROAMING_FOLDER = new File(PATH_ROOT + "\\AppData\\Roaming");
	public static final File DESKTOP_FOLDER = new File(PATH_ROOT + "\\Desktop");
	public static final File VERSIONS_FOLDER = new File(PATH_ROOT + "\\AppData\\Roaming\\.minecraft\\versions");
	public static final File MINECRAFT_FOLDER = new File(PATH_ROOT + "\\AppData\\Roaming\\.minecraft");
	public static final File DOWNLOADS_FOLDER = new File(Paths.get(PATH_ROOT, "Downloads").toString());
	public static final File OWN_FOLDER = new File(PATH_ROOT + "\\vysledky");
	public static final File LATEST_LOG = new File(LOGS_FOLDER.getPath() + "\\latest.log");
	public static final File TXT_PREVIOUS_INSPECTIONS_INFO = new File(OWN_FOLDER.getPath() + "\\predesleKontrolyInfo.txt");
	public static final File LATEST_ZIP = new File(OWN_FOLDER + "\\latest.zip");
	
	public static final boolean LATEST_LOG_EXISTS = LATEST_LOG.exists();
	public static final boolean LOGS_FOLDER_EXISTS = LOGS_FOLDER.exists();
	public static final boolean DESKTOP_EXISTS = DESKTOP_FOLDER.exists();
	public static final boolean DOWNLOADS_EXISTS = DOWNLOADS_FOLDER.exists();
	public static final boolean ROAMING_EXISTS = ROAMING_FOLDER.exists();
	
	private boolean probablyWrongName;
	private boolean probableHacker;
	private List<String> foundHacksName;
	private ArrayList<String> pathsToLogs;
	public ArrayList<String> errors;
	private ArrayList<String> keywords;
	private ArrayList<String> logKeywords;
	private String lastInspectionDate;
	private String hackerIndicators;
	private String playerName;
	private int hackerIndicatorsCounter;
	private int totalInspectionsNumber;
	private Main main;
	private String progress;
	
	public static final String WORD_SEPARATOR = " | ";
	public static final int MAIL_LATEST_LOG_LINES = 500;
	public static final String initialInfo = 0 + "\n" + 0;
	public static final int MAX_AGE_OF_LOGS_TO_INSPECT_DAYS = 25;
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
	
	public Inspection(Main main) {
		super.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
		    public void uncaughtException(Thread t, Throwable e) {
		    	e.printStackTrace();
		       Inspection.interruptInspection("neošetøená vyjímka", true);
		    }
		 });
		
		this.main = main;
		errors = new ArrayList<>();
		foundHacksName = Collections.synchronizedList(new ArrayList<>());
		pathsToLogs =  new ArrayList<>();
		BACKUP_LOG_KEYWORDS = new ArrayList<>(Arrays.asList("hack", "fly", "antiknockback", "speed",
				"tpaura", "tp-aura", "regen", "blink", "nofall", "no-fall", "autosoup", "velocity", "novelocity", "nuker"));
		BACKUP_LOG_KEYWORDS.addAll(BACKUP_KEYWORDS);
	}
	
	public void doInspection() {
		LATEST_ZIP.deleteOnExit();
		printInspectionProgress("1");
		if (!OWN_FOLDER.exists()) {
			createOwnDir();
		}
		loadKezwords();
		pathsToLogs.addAll(getPathsToLogs());
		
		String predesleKontrolyInfoStr = convertLogContentToString(TXT_PREVIOUS_INSPECTIONS_INFO.getPath());
	
		lastInspectionDate = getLine(predesleKontrolyInfoStr, 2);
		long timeSinceLastInspectionMins = 0;
		if (!lastInspectionDate.equals("0")) {
			try {
				timeSinceLastInspectionMins =
						getDateDiff(LocalDateTime.now().format(FORMATTER), lastInspectionDate, TimeUnit.MINUTES);	
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				writeToFile(TXT_PREVIOUS_INSPECTIONS_INFO, initialInfo);
				interruptInspection("Nastala chyba pøi ètení datumu a"
						+ " probìhl pokus o automatickou opravu. Zkuste to, prosím, znovu.", true);
			} catch (NullPointerException e) {
				e.printStackTrace();
				timeSinceLastInspectionMins = -1;
				errors.add("textový soubor, který zaznamenává pøedešlé kontroly, nebyl nalezen.");
			}
		}
		printInspectionProgress("2");
		
		if (Main.mode != Rezim.DEBUG) {
			if (timeSinceLastInspectionMins < 3 && timeSinceLastInspectionMins >= 1) {
				interruptInspection("Doba od poslední kontroly je moc krátká, zkuste to pozdìji.", true);
			}	
		}
		
		long lastVersionsModifInMins =
				getDateDiff(LocalDateTime.now().format(FORMATTER),
						getLastModificationDate(VERSIONS_FOLDER), TimeUnit.MINUTES);	
		
		if (lastVersionsModifInMins < 30 && lastVersionsModifInMins >= 0) {
			addHackerIndicator("Složka versions byla upravována pøed ménì než 30 minutami");
		}
		printInspectionProgress("3");
		
		ArrayList<String> namesOfFilesOnDesktopArr = new ArrayList<>();
		ArrayList<String> namesOfFilesInVersionsArr = new ArrayList<>();
		ArrayList<String> namesOfFilesInMinecraftArr =  new ArrayList<>();
		ArrayList<String> namesOfJarFilesInDownloadsArr = new ArrayList<>();
		ArrayList<String> namesOfExeFilesInDownloadsArr = new ArrayList<>();
		
		namesOfFilesInVersionsArr = new FileSearch
				((String)null, null).searchFiles(VERSIONS_FOLDER, false, true, null);
		
		namesOfFilesInMinecraftArr = new FileSearch
				((String)null, null).searchFiles(MINECRAFT_FOLDER, false, true, null);
		
		printInspectionProgress("4");
		
		if (DOWNLOADS_EXISTS) {
			namesOfJarFilesInDownloadsArr = new FileSearch
					(new ArrayList<String>(Collections.singletonList("jar")), null)
					.searchFiles(DOWNLOADS_FOLDER, false, true, null);
			
			namesOfExeFilesInDownloadsArr = new FileSearch
					(new ArrayList<String>(Collections.singletonList("exe")), null)
					.searchFiles(DOWNLOADS_FOLDER, false, true, null);	
		} else {
			String error = "složka stažené nebyla nalezena/neexistuje.";
			namesOfJarFilesInDownloadsArr.add(error);
			errors.add(error);
		}
		
		if (DESKTOP_EXISTS) {
			namesOfFilesOnDesktopArr = new FileSearch
					(new ArrayList<String>(Collections.singletonList("jar")), null)
					.searchFiles(DESKTOP_FOLDER, false, true, null);	
		} else {
			String error = "Plocha nebyla nalezena.";
			namesOfFilesOnDesktopArr.add(error);
			errors.add(error);
		}
		
		printInspectionProgress("5");
		
		String namesOfMinecraftFiles =
				convertArrayToString(namesOfFilesInVersionsArr, " | ");
		
		String foundKeywordsInMinecraft =
				convertArrayToString(namesOfFilesInMinecraftArr, " | ");
		
		printInspectionProgress("6");
		String namesOfDownloadsJarFiles =
				convertArrayToString(namesOfJarFilesInDownloadsArr, " | ");
		
		String namesOfDownloadsExeFiles =
				convertArrayToString(namesOfExeFilesInDownloadsArr, " | ");
		
		String namesOfDesktopJarFiles =
				convertArrayToString(namesOfFilesOnDesktopArr, " | ");
		
		printInspectionProgress("7");
		
		
		
		//now find keywords
		Thread tDownloads = findKeywordsInDir(DOWNLOADS_FOLDER, keywords, null);
		Thread tDesktop = findKeywordsInDir(DESKTOP_FOLDER, keywords, null);
		printInspectionProgress("8");
		if (DOWNLOADS_EXISTS) {
			tDownloads.start();	
		}
		if (DESKTOP_EXISTS) {
			tDesktop.start();	
		}
		
		File folderToSearch = ROAMING_EXISTS ? ROAMING_FOLDER : MINECRAFT_FOLDER;
		ArrayList<String> foundKeywords =
				new FileSearch((String)null, keywords)
				.searchFiles(folderToSearch, true, true,
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
			interruptInspection("Nastala chyba pøi pøipojování vláken", false);
		}
		
		printInspectionProgress("10");
		String foundHackKeywordsStr = convertArrayToString(foundHacksName, " | ");
		
		String logLinesContainingKeyword = null;
		if (LOGS_FOLDER_EXISTS) {
			for (String pathToLog : pathsToLogs) {
				if (getDateDiff(LocalDateTime.now().format(FORMATTER),
						getLastModificationDate(new File(pathToLog)), TimeUnit.DAYS) < MAX_AGE_OF_LOGS_TO_INSPECT_DAYS) {
					if (new File(pathToLog).getName().contains("debug")) {
						continue;
					}
					
					String logLineWithKeyword =
							convertArrayToString
							(getKeywordsContainedInLog
									(pathToLog, logKeywords, true), "\n");
					if (logLineWithKeyword != null) {
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

		printInspectionProgress("11");

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
					interruptInspection("Nastala chyba pøi synchronizování vláken", true);
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
				
				+ "<b>Øádky z logù za posledních 15 dní, ve kterých byly nalezeny klíèové slova:</b><br>"
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
					+ " (" + getLastModificationDate(VERSIONS_FOLDER) + ")" + "<br><br>"
				
				+ "<b>Chyby pøi  kontrole: </b><br>"
					+ (errors.isEmpty() ? "žádné" : errors);

		
		String latestLogContent = "latest log nebyl nazelen/neexistuje";
		if (LATEST_LOG_EXISTS) {
			latestLogContent = cutString(convertLogContentToString(LATEST_LOG.getPath()), MAIL_LATEST_LOG_LINES);
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
		
		String title = "Výsledky kontroly PC hráèe " + playerName;
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
			if (chyba != null) {
				interruptInspection(chyba, true);
			}	
		} else {
			chyba = Email.sendMail(main.getCurrentServer().getMail(), title,
					content);
		}
		
		updatePreviousInspectionsInfo(TXT_PREVIOUS_INSPECTIONS_INFO, totalInspectionsNumber,
				LocalDateTime.now().format(FORMATTER));
		
		printInspectionProgress("FINAL");
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				main.showInspectionCompletedScreen(errors);
			}
		});
	}
	
	public void createOwnDir() {
		boolean dirCreated = OWN_FOLDER.mkdir();
		if (!dirCreated) {
			interruptInspection("Nastala chyba pøi vytváøení složky", false);
		}
		
		writeToFile(TXT_PREVIOUS_INSPECTIONS_INFO, initialInfo);
		changeFileAttribute(OWN_FOLDER, "dos:hidden", true);
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
	        params.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
	        params.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
	        if (pass != null) {
	            params.setEncryptFiles(true);
	            params.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
	            params.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
	            params.setPassword(pass);
	        }
	        
	        ZipFile zipFile = new ZipFile(zipDestination);
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
	
	private void loadKezwords() {
		File fileWithKeywords = new File(OWN_FOLDER.getPath() + "\\keywords.txt");
		ArrayList<String> keywords = new ArrayList<>();
		ArrayList<String> logKeywords = new ArrayList<>();
		
		try {
			ReadableByteChannel rbc = Channels.newChannel(new URL(URL_TO_KEYWORDS_STR).openStream());
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
			interruptInspection("Nastal problém pøi vytváøení textového souboru", true);
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
			//chyby.add("Nastal problém pøi mìnìní atributu složek");
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
	    		String pathToDecompressedFile = OWN_FOLDER.getPath() + "\\temp_decompressed_file" + ".log"; 
	    		decompressGZip(pathToDecompressedFile, new File(pathToFile));
	    		pathToFile = pathToDecompressedFile;
	    	}
			data = new String(Files.readAllBytes(Paths.get(pathToFile)));
		} catch (IOException e) {
			e.printStackTrace();
			errors.add("Nastal interní problém pøi ètení obsahu souboru.");
		}
	    
	    if (compressed) {
	    	try {
				deleteFile(pathToFile);
			} catch (IOException e) {
				e.printStackTrace();
				errors.add("Nastal problém pøi odstraòování doèasných dekompresovaných logù.");
			}
	    }
	    return data;
	  }
	
	public String convertDateDiffToWords(long diff) {
		String napisDiff = "";
		int oneDayInMins = 1440;
		int oneHourInMins = 60;
		if (diff < oneHourInMins) {
			 napisDiff = diff + (diff == 1 ? " minutou. ": " minutami.");
		}
		else if (diff >= oneHourInMins && diff < oneDayInMins) {
			long rozdilVMinutach = diff/oneHourInMins;
			napisDiff = rozdilVMinutach + (rozdilVMinutach == 1 ? " hodinou ": " hodinami ") + " a "
				+ diff % oneHourInMins + (rozdilVMinutach == 1 ? " minutou. ": " minutami.");
		}
		else if (diff >= oneDayInMins) {
			long rozdilVeDnech = diff/oneHourInMins/24;
			long zbyleHodiny = (diff/oneHourInMins) % 24;
			napisDiff = rozdilVeDnech + (rozdilVeDnech == 1 ? " dnem " : " dny ")
				+ " a " + zbyleHodiny + (zbyleHodiny == 1 ? " hodinou. " : " hodinami.");
		}
		return napisDiff;
	}
	
	private ArrayList<String> getPathsToLogs() {
		ArrayList<String> pathsToLogs = new ArrayList<>();
			for (String pathToLog : new FileSearch
					(new ArrayList<String>(Arrays.asList("gz", "log")), null).searchFiles
					(LOGS_FOLDER, true, false, null)) {
				if (!pathsToLogs.contains(pathToLog)) {
					pathsToLogs.add(pathToLog);
				}
			}
			return pathsToLogs;
	}
	
	public boolean isNameFound(String name) {
		if (pathsToLogs.isEmpty()) {
				return false;
		}
		
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
	
	public static void interruptInspection(String error, boolean showToUser) {
		System.err.println(error);
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Chyba");
				alert.setHeaderText("Došlo k chybì, která"
						+ " zabránila normální funkci programu. Žádná data nebudou odeslána.");
				if (showToUser) {
					alert.setContentText(" Chyba: " + error);	
				}				
				alert.showAndWait();
				Inspection.endProgram();
			}
		});
	}
	
	public static void endProgram() {
		changeFileAttribute(OWN_FOLDER, "dos:hidden", false);
		changeFileAttribute(TXT_PREVIOUS_INSPECTIONS_INFO, "dos:hidden", false);
			try {
				byte[] previousInspectionsInfoTxtBytes = Files.readAllBytes(TXT_PREVIOUS_INSPECTIONS_INFO.toPath());	
				for(File file : OWN_FOLDER.listFiles()) {
					if (!Arrays.equals(Files.readAllBytes(file.toPath()), previousInspectionsInfoTxtBytes)
							&& !file.isDirectory()) {
						file.delete();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (OWN_FOLDER.exists() && !OWN_FOLDER.isHidden()) {
				changeFileAttribute(OWN_FOLDER, "dos:hidden", true);
				changeFileAttribute(TXT_PREVIOUS_INSPECTIONS_INFO, "dos:hidden", true);	
			}
			Platform.exit();
			System.exit(0);
	}
	
	public void printInspectionProgress(String progress) {
		System.out.println(progress);
		this.progress = progress;
	}

	@Override
	public void run() {
		doInspection();	
	}
}