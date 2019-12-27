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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


import com.gmail.matejpesl1.mc_cheat_detection.Uvod.Rezim;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class Kontrola extends Thread {
	public static final ArrayList<String> DEFAULT_KEYWORDS = new ArrayList<>(Arrays.asList("huzuni", "skillclient", "liquidbounce",
			"wolfram", "impact_", "aristois", "wurst", "jam", "kronos", "jigsaw", "hacked", "hackclient", "hacked-client",
			"hack-client", "pandora", "killaura", "kill-aura", "forgehax", "impact_", "impact-", "_impact", "-impact"));
	public final ArrayList<String> DEFAULT_LOG_KEYWORDS;
	public static final String KORENOVA_SLOZKA = System.getProperty("user.home");
	public static final String VLASTNI_SLOZKA_CESTA = KORENOVA_SLOZKA + "\\vysledky";
	public static final String LOGS_CESTA = System.getProperty("user.home") + "\\AppData\\Roaming\\.minecraft\\logs";
	public static final File SLOZKA_ROAMING = new File(System.getProperty("user.home") + "\\AppData\\Roaming");
	public static final String URL_TO_KEYWORDS_STR = "https://drive.google.com/uc?export=download&id=1h1RyP_m1j29jRES-TWj55Vg47BbppSoF";
	public static final File SLOZKA_LOGS = new File(LOGS_CESTA);
	public static final File HOME_DIR = new File(System.getProperty("user.home"));
	public static final File SLOZKA_PLOCHA = new File(System.getProperty("user.home") + "\\Desktop");
	public static final File SLOZKA_VERSIONS = new File(System.getProperty("user.home") + "\\AppData\\Roaming\\.minecraft\\versions");
	public static final File SLOZKA_MINECRAFT = new File(System.getProperty("user.home") + "\\AppData\\Roaming\\.minecraft");
	public static final File SLOZKA_STAZENE = new File(Paths.get(System.getProperty("user.home"), "Downloads").toString());
	public static final File VLASTNI_SLOZKA = new File(VLASTNI_SLOZKA_CESTA);
	public static final File PREDESLE_KONTROLY_INFO_TXT = new File(VLASTNI_SLOZKA.getPath() + "\\predesleKontrolyInfo.txt");
	public static final String ROZDELOVAC_SLOV = " | ";
	public static final int POCET_RADKU_LOGU_V_NAHLEDU = 500;
	private static final File LATEST_ZIP = new File(VLASTNI_SLOZKA + "\\latest.zip");
	private boolean pravdepodobneNespravneJmeno;
	private boolean pravdepodobnyHacker;
	private List<String> nalezeneJmenaHacku = Collections.synchronizedList(new ArrayList<>());
	private static ArrayList<String> cestyKLogum = new ArrayList<>();
	public ArrayList<String> chyby = new ArrayList<>();
	private ArrayList<String> keywords;
	private ArrayList<String> logKeywords;
	private String datumPosledniKontroly;
	private String duvodyProHackera;
	private String jmenoHrace;
	private int duvodyProHackeraCounter;
	private int celkovyPocetKontrol;
	private static final boolean LATEST_LOG_EXISTS = new File(LOGS_CESTA + "\\latest.log").exists();
	private static final boolean LOGS_FOLDER_EXISTS = SLOZKA_LOGS.exists();
	private static final boolean PLOCHA_EXISTS = SLOZKA_PLOCHA.exists();
	private static final boolean STAZENE_EXISTS = SLOZKA_STAZENE.exists();
	private static final String pocatecniInfo = 0 + "\n" + 0;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
	String stav;
	private Uvod uvod;
	
	public Kontrola(Uvod uvod) {
		super.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
		    public void uncaughtException(Thread t, Throwable e) {
		    	e.printStackTrace();
		       Kontrola.prerusKontrolu("neošetøená vyjímka", true);
		    }
		 });
		
		this.uvod = uvod;
		
		DEFAULT_LOG_KEYWORDS = new ArrayList<>(Arrays.asList("hack", "fly", "antiknockback", "speed",
				"tpaura", "tp-aura", "regen", "blink", "nofall", "no-fall", "autosoup", "velocity", "novelocity", "nuker"));
		DEFAULT_LOG_KEYWORDS.addAll(DEFAULT_KEYWORDS);
	}
	
	public void zacniKontrolu() {
		LATEST_ZIP.deleteOnExit();
		vypisStavKontroly("1");
		if (!VLASTNI_SLOZKA.exists()) {
			vytvorVlastniSlozku();
		}
		nactiKeywordy();
		cestyKLogum.addAll(getPathsToLogs());
		
		String predesleKontrolyInfoStr = prevedObsahSouboruNaString(PREDESLE_KONTROLY_INFO_TXT.getPath());
	
		datumPosledniKontroly = ziskejRadek(predesleKontrolyInfoStr, 2);
		long dobaOdPosledniKontroly = 0;
		if (!datumPosledniKontroly.equals("0")) {
			try {
				dobaOdPosledniKontroly = ziskejRozdilMeziDaty(LocalDateTime.now().format(FORMATTER), datumPosledniKontroly);	
			} catch (DateTimeParseException e) {
				e.printStackTrace();
				napisDoSouboru(PREDESLE_KONTROLY_INFO_TXT, pocatecniInfo);
				prerusKontrolu("Nastala chyba pøi ètení datumu a"
						+ " probìhl pokus o automatickou opravu. Zkuste to, prosím, znovu.", true);
			} catch (NullPointerException e) {
				e.printStackTrace();
				dobaOdPosledniKontroly = -1;
				chyby.add("textový soubor, který zaznamenává pøedešlé kontroly, nebyl nalezen.");
			}
		}
		vypisStavKontroly("2");
		
		if (Uvod.rezim != Rezim.DEBUG) {
			if (dobaOdPosledniKontroly < 3 && dobaOdPosledniKontroly >= 1) {
				prerusKontrolu("Doba od poslední kontroly je moc krátká, zkuste to pozdìji.", true);
			}	
		}
		
		long dobaOdPosledniUpravyVersions =
				ziskejRozdilMeziDaty(LocalDateTime.now().format(FORMATTER), ziskejDatumPosledniUpravy(SLOZKA_VERSIONS));	
		
		if (dobaOdPosledniUpravyVersions < 15 && dobaOdPosledniUpravyVersions >= 0) {
			pridejDuvodProHackera("Složka versions byla upravována pøed ménì než 15 minutami");
		}
		vypisStavKontroly("3");
		
		ArrayList<String> nazvySouboruVeVersionsArr = new HledaniSouboru
				((String)null, null).prohledejSoubory(SLOZKA_VERSIONS, false, true, null);
		
		ArrayList<String> nazvySouboruVMinecraftArr = new HledaniSouboru
				((String)null, null).prohledejSoubory(SLOZKA_MINECRAFT, false, true, null);
		
		vypisStavKontroly("4");
		
		ArrayList<String> nazvySouboruVeStazenychArr = new ArrayList<String>();				
		if (STAZENE_EXISTS) {
			nazvySouboruVeStazenychArr = new HledaniSouboru
					(new ArrayList<String>(Collections.singletonList("jar")), null).prohledejSoubory(SLOZKA_STAZENE, false, true, null);	
		} else {
			String error = "složka stažené nebyla nalezena/neexistuje.";
			nazvySouboruVeStazenychArr.add(error);
			chyby.add(error);
		}
		
		ArrayList<String> nazvySouboruNaPloseArr = new ArrayList<>();
		if (PLOCHA_EXISTS) {
			nazvySouboruNaPloseArr = new HledaniSouboru
					(new ArrayList<String>(Collections.singletonList("jar")), null).prohledejSoubory(SLOZKA_PLOCHA, false, true, null);	
		} else {
			String error = "Plocha nebyla nalezena.";
			nazvySouboruNaPloseArr.add(error);
			chyby.add(error);
		}
		
		vypisStavKontroly("5");
		
		String nazvySouboruVeVersions =
				prevedArrayNaString(nazvySouboruVeVersionsArr, " | ");
		
		String nazvySouboruVMinecraft =
				prevedArrayNaString(nazvySouboruVMinecraftArr, " | ");
		
		vypisStavKontroly("6");
		String nazvySouboruVeStazenych =
				prevedArrayNaString(nazvySouboruVeStazenychArr, " | ");
		
		String nazvySouboruNaPlose =
				prevedArrayNaString(nazvySouboruNaPloseArr, " | ");
		
		vypisStavKontroly("7");
		
		Thread tStazene = najdiKeywordyVeSlozce(SLOZKA_STAZENE, keywords, null);
		Thread tPlocha = najdiKeywordyVeSlozce(SLOZKA_PLOCHA, keywords, null);
		vypisStavKontroly("8");
		if (STAZENE_EXISTS) {
			tStazene.start();	
		}
		if (PLOCHA_EXISTS) {
			tPlocha.start();	
		}
		if (SLOZKA_ROAMING.exists()) {
			ArrayList<String> nalezeneSoubory =
					new HledaniSouboru((String)null, keywords)
					.prohledejSoubory(SLOZKA_ROAMING, true, true, new ArrayList<String>(Collections.singletonList("assets")));
			nalezeneJmenaHacku.addAll(nalezeneSoubory);	
		} else {
			ArrayList<String> nalezeneSoubory =
					new HledaniSouboru((String)null, keywords)
					.prohledejSoubory(SLOZKA_MINECRAFT, false, true, new ArrayList<String>(Collections.singletonList("assets")));
			nalezeneJmenaHacku.addAll(nalezeneSoubory);	
		}		
		
		vypisStavKontroly("9");
		try {
			if (tStazene.isAlive()) {
				tStazene.join();
			}
			
			if (tPlocha.isAlive()) {
				tPlocha.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			prerusKontrolu("Nastala chyba pøi pøipojování vláken", false);
		}
		
		vypisStavKontroly("10");
		String nalezeneJmenaHackuStr = prevedArrayNaString(nalezeneJmenaHacku, ", ");
		
		String logLinesContainingKeyword = null;
		if (LOGS_FOLDER_EXISTS) {
			for (String cestaKLogu : cestyKLogum) {
				if (ziskejRozdilMeziDaty(LocalDateTime.now().format(FORMATTER),
						ziskejDatumPosledniUpravy(new File(cestaKLogu))) < 21600) {
					if (new File(cestaKLogu).getName().contains("debug")) {
						continue;
					}
					
					String logLineWithKeyword = prevedArrayNaString(ziskejObsazeneKeywordyLogu(cestaKLogu, logKeywords, true), "\n");
					if (logLineWithKeyword != null) {
						logLinesContainingKeyword = (logLinesContainingKeyword == null ? "" : logLinesContainingKeyword) + logLineWithKeyword;
					} 
				}
			}	
		} else {
			String error = "složka s logy nebyla nalezena";
			logLinesContainingKeyword = "nebyla nalezena složka s logy";
			chyby.add(error);
		}

		vypisStavKontroly("11");

		boolean nestandardniJmena = false;
		ArrayList<String> nestandardniNazvySlozek = new ArrayList<>();
		for (String nazevSlozky : nazvySouboruVeVersionsArr) {
			if (!nazevSlozky.matches(".*\\d.*")) {
				nestandardniJmena = true;
				nestandardniNazvySlozek.add(nazevSlozky);
			}
		}
		if (nestandardniJmena) {
			pravdepodobnyHacker = true;
			pridejDuvodProHackera("název/názvy Minecraft verze ve složce versions je/jsou nestandardní. (" +
			prevedArrayNaString(nestandardniNazvySlozek, " | ") + ")");
		}

		if (nalezeneJmenaHacku.size() > 0) {
			pravdepodobnyHacker = true;
			pridejDuvodProHackera("Byly nalezeny soubory, které jsou pravdìpodobnì hacky.");
		}
		synchronized (this) {
			while (!Uvod.isInspectionRunning()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					prerusKontrolu("Nastala chyba pøi synchronizování vláken", true);
				}	
			}
		}

		vypisStavKontroly("12");
		celkovyPocetKontrol = Integer.parseInt(ziskejRadek(predesleKontrolyInfoStr, 1).replace("\\D+",""));
		++celkovyPocetKontrol;
		
		String vysledky = "verze programu: " + Uvod.VERZE_PROGRAMU + "<br><br>"
				+ "<b>Jméno hráèe:</b> " + jmenoHrace + "<br><br>"
				+ "<b>ovìøení jména: </b>" + (pravdepodobneNespravneJmeno ? 
						"Za toto jméno pravdìpodobnì nebylo na tomto PC za posledních 15 dní hráno!" : "jméno je pravdìpodobnì správné") + "<br><br>"
				+ "<b>pravdìpodobný hacker: " + (pravdepodobnyHacker ? "ano</b> - dùvody: " + "<br>" + duvodyProHackera : "ne</b>") + "<br><br>"
				+ "<b>celkový poèet kontrol provedených na tomto PC: </b><br>" + celkovyPocetKontrol + "<br><br>"
				+ "<b>Pøedešlá kontrola probìhla pøed: </b><br>" + (celkovyPocetKontrol <= 1 ?
						"žádné pøedešlé kontroly neprobìhly" : ziskejSlovniRozdilMeziDaty(dobaOdPosledniKontroly, datumPosledniKontroly)) + "<br><br>"
				+ "<b>nalezené soubory jež se shodují se jménem hackù: </b>" + "<br>" + (nalezeneJmenaHacku.isEmpty() ?
						"žádné" : nalezeneJmenaHackuStr) + "<br><br>"
				+ "<b>Øádky z logù za posledních 15 dní, ve kterých byly nalezeny klíèové slova:</b>" + "<br>" + (logLinesContainingKeyword == null ?
						"žádné" : logLinesContainingKeyword) + "<br><br>"
				+ "<b>názvy souborù ve složce versions v hloubce 1: </b>" + "<br>" + nazvySouboruVeVersions + "<br><br>"
				+ "<b>Názvy všech \"jar\" souborù ve složce stažené: </b>" + "<br>" + nazvySouboruVeStazenych + "<br><br>"
				+ "<b>Názvy všech \"jar\" souborù na ploše: </b>" + "<br>" + (nazvySouboruNaPlose == null ? "žádné" : nazvySouboruNaPlose) + "<br><br>"
				+ "<b>Názvy souborù ve složce .minecraft ve hloubce 1: </b>" + "<br>" + nazvySouboruVMinecraft + "<br><br>"
				+ "<b>složka versions naposledy upravována pøed: </b>" + "<br>" +
				ziskejSlovniRozdilMeziDaty(dobaOdPosledniUpravyVersions, ziskejDatumPosledniUpravy(SLOZKA_VERSIONS)) + "<br><br>"
				+ "<b>Chyby pøi  kontrole: </b>" + "<br>" + (chyby.isEmpty() ? "žádné" : chyby);

		String obsahLatestLogu = "";
		if (LATEST_LOG_EXISTS) {
			obsahLatestLogu = zkratString(prevedObsahSouboruNaString(LOGS_CESTA + "\\latest.log"), POCET_RADKU_LOGU_V_NAHLEDU);
			try {
				vytvorZip(new File(LOGS_CESTA + "\\latest.log"), LATEST_ZIP.toString(), null);
			} catch (Exception e) {
				chyby.add("Nepodaøilo se zazipovat latest log.");
				e.printStackTrace();
			}
		} else {
			obsahLatestLogu = "latest log nebyl nazelen/neexistuje";
		}
		
		vypisStavKontroly("13");
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				   uvod.changeInspectionState("Odesílání výsledkù");
			}
		});
		
		String title = "Výsledky kontroly PC hráèe " + jmenoHrace;
		String content = "";
		if (LATEST_LOG_EXISTS) {
			content = vysledky + "<br><br>----------------------------------------------------------------------------"
					+ "<br><b>Prvních " + POCET_RADKU_LOGU_V_NAHLEDU + " øádkù Nejnovìjšího logu:"
					+ " </b><br><br>" + obsahLatestLogu;	
		} else {
			content = vysledky + "<br><br>----------------------------------------------------------------------------"
					+ "<br><b>Latest log nebyl nalezen/neexistuje. </b>";
		}
		String chyba = null;
		if (LATEST_LOG_EXISTS) {
			chyba = Email.odesliMail(uvod.getCurrentServer().getMail(), title,
					content, LATEST_ZIP.getPath(), "latest_log.zip");
			if (chyba != null) {
				prerusKontrolu(chyba, true);
			}	
		} else {
			chyba = Email.odesliMail(uvod.getCurrentServer().getMail(), title,
					content);
		}
		
		aktualizujInfoOPredeslychKontrolach(PREDESLE_KONTROLY_INFO_TXT, celkovyPocetKontrol, LocalDateTime.now().format(FORMATTER));
		vypisStavKontroly("FINAL");
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				uvod.showInspectionCompletedScreen(chyby);
			}
		});
	}
	
	public void vytvorVlastniSlozku() {
		boolean slozkaVytvorena = VLASTNI_SLOZKA.mkdir();
		if (!slozkaVytvorena) {
			prerusKontrolu("Nastala chyba pøi vytváøení složky", false);
		}
		napisDoSouboru(PREDESLE_KONTROLY_INFO_TXT, pocatecniInfo);
		zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", true);
		zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", true);
	}
	
	public String zkratString(String text, int maxPocetRadku) {
		String[] radky = text.split("\\r?\\n");
		String vsechnyRadky = null;
		int cisloRadku = 0;
		for (String radek : radky) {
			++cisloRadku;
			if (cisloRadku <= maxPocetRadku) {
				vsechnyRadky = (vsechnyRadky == null ? "" : vsechnyRadky + "\n") + radek;	
			} else {
				break;
			}	
		}
		return vsechnyRadky;
	}
	
	public void vytvorZip(File souborKZazipovani, String destinaceZipu, String heslo) throws Exception {
	        ZipParameters parametry = new ZipParameters();
	        parametry.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
	        parametry.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
	        if (heslo != null) {
	            parametry.setEncryptFiles(true);
	            parametry.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
	            parametry.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
	            parametry.setPassword(heslo);
	        }
	        ZipFile zipFile = new ZipFile(destinaceZipu);
	        if (souborKZazipovani.isFile()) {
	            zipFile.addFile(souborKZazipovani, parametry);
	        } else if (souborKZazipovani.isDirectory()) {
	            zipFile.addFolder(souborKZazipovani, parametry);
	        }
	}
	
	public void dodejUdaje(String jmenoHrace, boolean pravdepodobneNespravneJmeno) {
		this.jmenoHrace = jmenoHrace;
		this.pravdepodobneNespravneJmeno = pravdepodobneNespravneJmeno;
	}
	
	private Thread najdiKeywordyVeSlozce(File slozka, ArrayList<String> keywords, ArrayList<String> dirsToSkip) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				ArrayList<String> nalezeneSoubory = new HledaniSouboru((String)null, keywords).prohledejSoubory(slozka, true, true, dirsToSkip);
				nalezeneJmenaHacku.addAll(nalezeneSoubory);
			}
		};
		return thread;
	}
	
	private void nactiKeywordy() {
		File souborSKeywordama = new File(VLASTNI_SLOZKA_CESTA + "\\keywords.txt");
		ArrayList<String> keywords = new ArrayList<>();
		ArrayList<String> logKeywords = new ArrayList<>();
		
		try {
			ReadableByteChannel rbc = Channels.newChannel(new URL(URL_TO_KEYWORDS_STR).openStream());
			FileOutputStream fos = new FileOutputStream(souborSKeywordama);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		    fos.close();
		    rbc.close();
		    String keywordyZeSouboru = prevedObsahSouboruNaString(souborSKeywordama.getPath());
			odstranSoubor(souborSKeywordama.getPath());
			if (!keywordyZeSouboru.contains("KEYWORDS")) {
				this.keywords = DEFAULT_KEYWORDS;
				this.logKeywords = DEFAULT_LOG_KEYWORDS;
				return;
			}
			
			boolean iterovaniPresKeywordyLogu = false;
			for (String radek : ziskejVsechnyRadky(keywordyZeSouboru)) {
				if (radek.matches("") || radek.contains("//")) {
					continue;
				}
				
				if (!radek.matches("KEYWORDS")) {
					if (radek.matches("LOG_KEYWORDS")) {
						iterovaniPresKeywordyLogu = true;
						continue;
					}
					if (!iterovaniPresKeywordyLogu) {
						keywords.add(radek.toLowerCase().trim());	
					} else {
						logKeywords.add(radek.toLowerCase().trim());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.keywords = DEFAULT_KEYWORDS;
			this.logKeywords = DEFAULT_LOG_KEYWORDS;
			return;
		}
		this.keywords = keywords;
		this.logKeywords = logKeywords;
	}
	
	private void aktualizujInfoOPredeslychKontrolach(File soubor, int celkovyPocetKontrol, String currentDate) {
		String updatedInfo = celkovyPocetKontrol + "\n" + currentDate;
		napisDoSouboru(soubor, updatedInfo);
	}
	
	private void napisDoSouboru(File soubor, String text) {
		boolean isHidden = false;
		boolean isReadOnly = false;
		try {
			if (soubor.isHidden()) {
				isHidden = true;
				zmenAtributSouboru(soubor, "dos:hidden", false);
			}
			if (!soubor.canWrite()) {
				isReadOnly = true;
				soubor.setWritable(true);
			}
			
			 BufferedWriter zapisovatel = new BufferedWriter(new FileWriter(soubor));
			    zapisovatel.write(text);
			    zapisovatel.close();
		} catch (IOException e) {
			e.printStackTrace();
			prerusKontrolu("Nastal problém pøi vytváøení textového souboru", true);
		} finally {
			if (isHidden) {
				zmenAtributSouboru(soubor, "dos:hidden", true);
			}
			if (isReadOnly) {
				soubor.setWritable(false);
			}
		}
	}
	
	private static void zmenAtributSouboru(File soubor, String atributKeZmeneni, boolean stavAtributu) {
		try {
			Files.setAttribute(soubor.toPath(), atributKeZmeneni, stavAtributu);
		} catch (IOException e) {
			e.printStackTrace();
			//chyby.add("Nastal problém pøi mìnìní atributu složek");
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	protected ArrayList<String> ziskejObsazeneKeywordy(ArrayList<String> textKeKontrole, ArrayList<String> keywords) {
		ArrayList<String> obsazeneKeywordy = new ArrayList<>();
		for (String kontrolovanyText : textKeKontrole) {
			for (String keyword : keywords) {
				if (kontrolovanyText.toLowerCase().contains(keyword)) {
					obsazeneKeywordy.add(kontrolovanyText);
				}
			}
		}
		return obsazeneKeywordy;
	}
	
	public void dekompresujGZip(String destinace, File soubor) {
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
		       chyby.add("nelze dekompresovat logy");
		    }
	}
	
	private void pridejDuvodProHackera(String duvod) {
		++duvodyProHackeraCounter;
		duvodyProHackera = (duvodyProHackera == null ? duvodyProHackeraCounter + ". " : duvodyProHackera + "\n" + duvodyProHackeraCounter + ". ") + duvod;
	}
	
	private String prevedArrayNaString(ArrayList<String> array, String rozdeleni) {
		String vyslednyString = null;

		for (String polozka : array) {
			vyslednyString = (vyslednyString == null ? "" : vyslednyString + rozdeleni) + polozka;
		}
		return vyslednyString;
	}
	
	private String prevedArrayNaString(List<String> array, String rozdeleni) {
		ArrayList<String> array2 = new ArrayList<>(array);
		return prevedArrayNaString(array2, rozdeleni);
	}

	public void odstranSoubor(String cestaKSouboru) throws IOException {
	    	Files.deleteIfExists(Paths.get(cestaKSouboru));
	}
	
	private ArrayList<String> ziskejObsazeneKeywordyLogu(String log, ArrayList<String> keywords, boolean wholeLine) {
		ArrayList<String> obsazeneKeywordy = new ArrayList<>();
		if (!new File(log).exists()) {
			return null;
		}
		
		String logText = prevedObsahSouboruNaString(log);
		
        String[] radky = logText.split("\\r?\\n");
        for (String radek : radky) {
            if (!radek.contains("[CHAT]")) {
            	for (String keyword : keywords) {
            		if (radek.contains(keyword)) {
            			obsazeneKeywordy.add(wholeLine ? radek : keyword);
            		}
            	}
            }
        }
        return obsazeneKeywordy;
	}
	
	
	
	private String ziskejRadek(String text, int poradiRadku) {
        String[] radky = text.split("\\r?\\n");
        int i = 0;
        for (String radek : radky) {
        	++i;
        	if (i == poradiRadku) {
        		return radek;
        	}
        }
        return null;
	}
	
	private ArrayList<String> ziskejVsechnyRadky(String text) {
		String[] radky = text.split("\\r?\\n");
		ArrayList<String> vsechnyRadky = new ArrayList<>();
		for (String radek : radky) {
			vsechnyRadky.add(radek);
		}
		return vsechnyRadky;
	}
	
	private String ziskejDatumPosledniUpravy(File slozka) throws NullPointerException {
		LocalDateTime dateAndTime =
			    LocalDateTime.ofInstant(Instant.ofEpochMilli(slozka.lastModified()), ZoneId.systemDefault());
		String dateAndTimeStr = dateAndTime.format(FORMATTER);
		return dateAndTimeStr;
	}
	
	private long ziskejRozdilMeziDaty(String datum1, String datum2) {
		LocalDateTime formatDatum1 = LocalDateTime.parse(datum1, FORMATTER);
		LocalDateTime formatDatum2 = LocalDateTime.parse(datum2, FORMATTER);
		long rozdilVMinutach = ChronoUnit.MINUTES.between(formatDatum2, formatDatum1);
	    return rozdilVMinutach;
	}
	
	public String prevedObsahSouboruNaString(String cestaKSouboru) { 
	    String data = null;
	    boolean compressed = false;
	    boolean fileExists = new File(cestaKSouboru).exists();
	    if (!fileExists) {
	    	return null;
	    }
	    
	    try {
	    	if (cestaKSouboru.endsWith(".gz")) {
	    		compressed = true;
	    		String pathToDecompressedFile = VLASTNI_SLOZKA_CESTA + "\\temp_decompressed_file" + ".log"; 
	    		dekompresujGZip(pathToDecompressedFile, new File(cestaKSouboru));
	    		cestaKSouboru = pathToDecompressedFile;
	    	}
			data = new String(Files.readAllBytes(Paths.get(cestaKSouboru)));
		} catch (IOException e) {
			e.printStackTrace();
			chyby.add("Nastal interní problém pøi ètení obsahu souboru.");
		}
	    
	    if (compressed) {
	    	try {
				odstranSoubor(cestaKSouboru);
			} catch (IOException e) {
				e.printStackTrace();
				chyby.add("Nastal problém pøi odstraòování doèasných dekompresovaných logù.");
			}
	    }
	    return data;
	  }
	
	public String ziskejSlovniRozdilMeziDaty(long rozdil, String folderModifiedDate) {
		String napisDiff = "";
		int oneDayInMins = 1440;
		int oneHourInMins = 60;
		if (rozdil < oneHourInMins) {
			 napisDiff = rozdil + (rozdil == 1 ? " minutou. ": " minutami. ") + "(" + folderModifiedDate + ")";
		}
		else if (rozdil >= oneHourInMins && rozdil < oneDayInMins) {
			long rozdilVMinutach = rozdil/oneHourInMins;
			napisDiff = rozdilVMinutach + (rozdilVMinutach == 1 ? " hodinou ": " hodinami ") + " a "
				+ rozdil % oneHourInMins + (rozdilVMinutach == 1 ? " minutou. ": " minutami. ") + "(" + folderModifiedDate + ")";
		}
		else if (rozdil >= oneDayInMins) {
			long rozdilVeDnech = rozdil/oneHourInMins/24;
			long zbyleHodiny = (rozdil/oneHourInMins) % 24;
			napisDiff = rozdilVeDnech + (rozdilVeDnech == 1 ? " dnem " : " dny ")
				+ " a " + zbyleHodiny + (zbyleHodiny == 1 ? " hodinou. " : " hodinami. ") + "(" + folderModifiedDate + ")";
		}
		return napisDiff;
	}
	
	private ArrayList<String> getPathsToLogs() {
		ArrayList<String> pathsToLogs = new ArrayList<>();
			for (String cestaKLogu : new HledaniSouboru
					(new ArrayList<String>(Arrays.asList("gz", "log")), null).prohledejSoubory
					(SLOZKA_LOGS, false, false, null)) {
				if (!pathsToLogs.contains(cestaKLogu)) {
					pathsToLogs.add(cestaKLogu);
				}
			}
			return pathsToLogs;
	}
	
	public boolean jeJmenoNalezeno(String jmeno) {
		if (cestyKLogum.isEmpty()) {
				return false;
		}
		
		String nameRegex = "\\b" + jmeno + "\\b";
		Pattern namePattern = Pattern.compile(nameRegex, Pattern.CASE_INSENSITIVE);
		
		String aktualniDatum = LocalDateTime.now().format(FORMATTER);
		for (String cestaKLogu : cestyKLogum) {
			String datumPosledniUpravy = ziskejDatumPosledniUpravy(new File(cestaKLogu));
			long dobaOdPosledniUpravyLogu =
					ziskejRozdilMeziDaty(aktualniDatum, datumPosledniUpravy);
			if (dobaOdPosledniUpravyLogu/60/24 > 15) {
				continue;
			}
			
			String obsahLogu = prevedObsahSouboruNaString(cestaKLogu);
			 String[] radky = obsahLogu.split("\\r?\\n");
		        for (String radek : radky) {
		            if (!radek.contains("[CHAT]")) {
		            	Matcher matcher = namePattern.matcher(radek);
		            	if (matcher.find()) {
		            		return true;
		            	}
		            }
		        }
		}
		return false;
	}
	
	public static void prerusKontrolu(String chyba, boolean ukazUzivateli) {
		System.err.println(chyba);
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Chyba");
				alert.setHeaderText("Došlo k chybì, která"
						+ " zabránila normální funkci programu. Žádná data nebudou odeslána.");
				if (ukazUzivateli) {
					alert.setContentText(" Chyba: " + chyba);	
				}				
				alert.showAndWait();
			}
		});
		ukonci();
	}
	
	public static void ukonci() {
		zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", false);
		zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", false);
			try {
				byte[] predesleKontrolyInfoTxtBytes = Files.readAllBytes(PREDESLE_KONTROLY_INFO_TXT.toPath());	
				for(File file : VLASTNI_SLOZKA.listFiles()) {
					if (!Arrays.equals(Files.readAllBytes(file.toPath()), predesleKontrolyInfoTxtBytes)
							&& !file.isDirectory()) {
						file.delete();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (VLASTNI_SLOZKA.exists() && !VLASTNI_SLOZKA.isHidden()) {
				zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", true);
				zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", true);	
			}
			Platform.exit();
			System.exit(0);
	}
	
	public void vypisStavKontroly(String stav) {
		System.out.println(stav);
		this.stav = stav;
	}

	@Override
	public void run() {
		zacniKontrolu();	
	}
}