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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

public class Kontrola extends Thread {
	public static final ArrayList<String> DEFAULT_KEYWORDS = new ArrayList<>(Arrays.asList("huzuni", "skillclient", "liquidbounce",
			"wolfram", "impact", "aristois", "wurst", "jam", "kronos", "jigsaw", "hacked", "hackclient", "hacked-client",
			"hack-client", "pandora", "killaura", "kill-aura", "forgehax"));
	private final ArrayList<String> DEFAULT_LOG_KEYWORDS;
	public static final float VERZE_PROGRAMU = 2.0f;
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
	public static final SimpleDateFormat FORMAT_DATA = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
	public static final File VLASTNI_SLOZKA = new File(VLASTNI_SLOZKA_CESTA);
	public static final File PREDESLE_KONTROLY_INFO_TXT = new File(VLASTNI_SLOZKA.getPath() + "\\predesleKontrolyInfo.txt");
	public static final String ROZDELOVAC_SLOV = " | ";
	private boolean pravdepodobneNespravneJmeno;
	private boolean pravdepodobnyHacker;
	private List<String> nalezeneJmenaHacku = Collections.synchronizedList(new ArrayList<>());
	private static ArrayList<String> cestyKLogum = new ArrayList<>();
	private ArrayList<String> chyby = new ArrayList<>();
	private ArrayList<String> keywords;
	private ArrayList<String> logKeywords;
	private String datumPosledniKontroly;
	private String duvodyProHackera;
	private String jmenoHrace;
	private int duvodyProHackeraCounter;
	private int celkovyPocetKontrol;
	
	public Kontrola() {
		DEFAULT_LOG_KEYWORDS = new ArrayList<>(Arrays.asList("hack", "fly", "antiknockback", "speed",
				"tpaura", "tp-aura", "regen", "blink", "nofall", "no-fall", "autosoup", "velocity", "novelocity", "nuker"));
		DEFAULT_LOG_KEYWORDS.addAll(DEFAULT_KEYWORDS);
	}
	
	public void zacniKontrolu() {
		vypisStavKontroly("1");
		nactiKeywordy();
		if (!VLASTNI_SLOZKA.exists()) {
			boolean slozkaVytvorena = VLASTNI_SLOZKA.mkdir();
			if (!slozkaVytvorena) {
				prerusKontrolu("Nastala chyba pøi vytváøení složky", false);
			}
			
			String pocatecniInfo = 0 + "\n" + 0;
			napisDoSouboru(PREDESLE_KONTROLY_INFO_TXT, pocatecniInfo);
			zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", true);
			zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", true);
		}
		
		String predesleKontrolyInfoStr = prevedObsahSouboruNaString(PREDESLE_KONTROLY_INFO_TXT.getPath());
		
		datumPosledniKontroly = ziskejRadek(predesleKontrolyInfoStr, 2);
		long dobaOdPosledniKontroly = 0;
		if (!datumPosledniKontroly.equals("0")) {
			dobaOdPosledniKontroly = ziskejRozdilMeziDaty(FORMAT_DATA.format(new Date()), datumPosledniKontroly);	
		}
		vypisStavKontroly("2");
		// TODO: PØI DISTRIBUCI ODKOMENTOVAT
		if (dobaOdPosledniKontroly < 30 && dobaOdPosledniKontroly > 1) {
			prerusKontrolu("Doba od poslední kontroly je moc krátká, zkuste to pozdìji.", true);
		}
		
		long dobaOdPosledniUpravyVersions = ziskejRozdilMeziDaty(FORMAT_DATA.format(new Date()), ziskejDatumPosledniUpravy(SLOZKA_VERSIONS)); 
		if (dobaOdPosledniUpravyVersions < 15) {
			pridejDuvodProHackera("Složka versions byla upravována pøed ménì než 15 minutami");
		}
		vypisStavKontroly("3");
		
		ArrayList<String> nazvySouboruVeVersionsArr = new HledaniSouboru((String)null, null).prohledejSoubory(SLOZKA_VERSIONS, false, true, null);
		ArrayList<String> nazvySouboruVMinecraftArr = new HledaniSouboru((String)null, null).prohledejSoubory(SLOZKA_MINECRAFT, false, true, null);
		vypisStavKontroly("4");
		ArrayList<String> nazvySouboruVeStazenychArr = new HledaniSouboru(new ArrayList<String>(Collections.singletonList("jar")), null).prohledejSoubory(SLOZKA_STAZENE, false, true, null);
		ArrayList<String> nazvySouboruNaPloseArr = new HledaniSouboru(new ArrayList<String>(Collections.singletonList("jar")), null).prohledejSoubory(SLOZKA_PLOCHA, false, true, null);
		vypisStavKontroly("5");
		
		String nazvySouboruVeVersions = prevedArrayNaString(nazvySouboruVeVersionsArr, " | ");
		String nazvySouboruVMinecraft = prevedArrayNaString(nazvySouboruVMinecraftArr, " | ");
		vypisStavKontroly("6");
		String nazvySouboruVeStazenych = prevedArrayNaString(nazvySouboruVeStazenychArr, " | ");
		String nazvySouboruNaPlose = prevedArrayNaString(nazvySouboruNaPloseArr, " | ");
		String keywordyVLatestLogu = prevedArrayNaString(ziskejObsazeneKeywordyLogu(LOGS_CESTA + "\\latest.log", logKeywords), " | ");
		vypisStavKontroly("7");
		Thread t1 = najdiKeywordyVeSlozce(SLOZKA_MINECRAFT, keywords, new ArrayList<String>(Collections.singletonList("assets")));
		Thread t2 = najdiKeywordyVeSlozce(SLOZKA_STAZENE, keywords, null);
		Thread t3 = najdiKeywordyVeSlozce(SLOZKA_PLOCHA, keywords, null);
		vypisStavKontroly("8");
		t1.start();
		t2.start();
		t3.start();
		ArrayList<String> nalezeneSoubory = new HledaniSouboru((String)null, keywords).prohledejSoubory(SLOZKA_ROAMING, false, true, null);
		nalezeneJmenaHacku.addAll(nalezeneSoubory);
		vypisStavKontroly("9");
		try {
			if (t1.isAlive()) {
				t1.join();
			}
			if (t2.isAlive()) {
				t2.join();
			}
			
			if (t3.isAlive()) {
				t3.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			prerusKontrolu("Nastala chyba pøi pøipojování vláken", false);
		}
		vypisStavKontroly("10");
		String nalezeneJmenaHackuStr = prevedArrayNaString(nalezeneJmenaHacku, ", ");

		for (String cestaKLogu : cestyKLogum) {
			if (ziskejRozdilMeziDaty(FORMAT_DATA.format(new Date()), ziskejDatumPosledniUpravy(new File(cestaKLogu))) < 21600) {
				String obsazeneKeywordyLogu = prevedArrayNaString(ziskejObsazeneKeywordyLogu(cestaKLogu, logKeywords), " | ");
				if (obsazeneKeywordyLogu != null) {
					keywordyVLatestLogu = keywordyVLatestLogu + obsazeneKeywordyLogu;
				} 
			}
		}
		vypisStavKontroly("11");

		boolean nestandardniJmena = false;
		for (String nazevSlozky : nazvySouboruVeVersionsArr) {
			if (nazevSlozky.contains("[0-9]+")) {
				nestandardniJmena = true;
			}
		}
		if (nestandardniJmena) {
			pridejDuvodProHackera("název/názvy Minecraft verze ve složce versions je/jsou nestandardní.");
		}

		if (nalezeneJmenaHacku.size() > 0) {
			pravdepodobnyHacker = true;
			pridejDuvodProHackera("Byly nalezeny soubory, které jsou pravdìpodobnì hacky.");
		}
		synchronized (this) {
			while (!Uvod.kontrolaJeSpustena()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}	
			}
		}

		vypisStavKontroly("12");
		zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", false);
		zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", false);
		celkovyPocetKontrol = Integer.parseInt(ziskejRadek(predesleKontrolyInfoStr, 1).replace("\\D+",""));
		++celkovyPocetKontrol;
		aktualizujInfoOPredeslychKontrolach(PREDESLE_KONTROLY_INFO_TXT, celkovyPocetKontrol, FORMAT_DATA.format(new Date()));
		zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", true);
		zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", true);
		
		vypisStavKontroly("13");
		
		String vysledky = "verze programu: " + VERZE_PROGRAMU + "<br><br>"
				+ "<b>Jméno hráèe:</b> " + jmenoHrace + "<br><br>"
				+ "<b>ovìøení jména: </b>" + (pravdepodobneNespravneJmeno ? "Za toto jméno pravdìpodobnì nebylo na tomto PC za posledních 15 dní hráno!" : "jméno je pravdìpodobnì správné") + "<br><br>"
				+ "<b>pravdìpodobný hacker: " + (pravdepodobnyHacker ? "ano</b> - dùvody: " + "<br>" + duvodyProHackera : "ne</b>") + "<br><br>"
				+ "<b>celkový poèet kontrol provedených na tomto PC: </b><br>" + celkovyPocetKontrol + "<br><br>"
				+ "<b>Uplynulá doba od pøedešlé kontroly: </b><br>" + (celkovyPocetKontrol <= 1 ?
						"žádné pøedešlé kontroly neprobìhly" : ziskejSlovniRozdilMeziDaty(dobaOdPosledniKontroly, datumPosledniKontroly)) + "<br><br>"
				+ "<b>nalezené soubory jež se shodují se jménem hackù: </b>" + "<br>" + (nalezeneJmenaHacku.isEmpty() ?
						"žádné" : nalezeneJmenaHackuStr) + "<br><br>"
				+ "<b>nalezené klíèové slova v log souborech za posledních 15 dní, které se shodují se jménem hackù:</b>" + "<br>" + (keywordyVLatestLogu == null ?
						"žádné" : keywordyVLatestLogu) + "<br><br>"
				+ "<b>názvy souborù ve složce versions v hloubce 1: </b>" + "<br>" + nazvySouboruVeVersions + "<br><br>"
				+ "<b>Názvy všech \"jar\" souborù ve složce stažené: </b>" + "<br>" + nazvySouboruVeStazenych + "<br><br>"
				+ "<b>Názvy všech \"jar\" souborù na ploše: </b>" + "<br>" + (nazvySouboruNaPlose == null ? "žádné" : nazvySouboruNaPlose) + "<br><br>"
				+ "<b>Názvy souborù ve složce .minecraft ve hloubce 1: </b>" + "<br>" + nazvySouboruVMinecraft + "<br><br>"
				+ "<b>složka versions naposledy upravována pøed: </b>" + "<br>" +
				ziskejSlovniRozdilMeziDaty(dobaOdPosledniUpravyVersions, ziskejDatumPosledniUpravy(SLOZKA_VERSIONS)) + "<br><br>"
				+ "<b>Chyby pøi  kontrole: </b>" + "<br>" + (chyby.isEmpty() ? "žádné" : chyby);

		Uvod.zmenStav("odesílání výsledkù");
			String chyba = Email.odesliMail(Uvod.EMAIL_RECIPIENT_ADDRESS, "Výsledky kontroly PC hráèe " + jmenoHrace,
					vysledky + "<br><br>----------------------------------------------------------------------------\n<b>Nejnovìjší log: </b><br><br>"+
							prevedObsahSouboruNaString(LOGS_CESTA + "\\latest.log"));	
		
		if (chyba != null) {
			prerusKontrolu(chyba, true);
		}
		vypisStavKontroly("FINAL");
		Uvod.ukazDokoncenouKontrolu(chyby);
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
		try {
			 BufferedWriter zapisovatel = new BufferedWriter(new FileWriter(soubor));
			    zapisovatel.write(text);
			    zapisovatel.close();
		} catch (IOException e) {
			e.printStackTrace();
			prerusKontrolu("Nastal problém pøi vytváøení textového souboru", true);
		}
	}
	
	private void zmenAtributSouboru(File soubor, String atributKeZmeneni, boolean stavAtributu) {
		try {
			Files.setAttribute(soubor.toPath(), atributKeZmeneni, stavAtributu);
		} catch (IOException e) {
			e.printStackTrace();
			chyby.add("Nastal problém pøi mìnìní atributu složek");
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
	
	private ArrayList<String> ziskejObsazeneKeywordyLogu(String log, ArrayList<String> keywords) {
		ArrayList<String> obsazeneKeywordy = new ArrayList<>();
		String logText = prevedObsahSouboruNaString(log);
		
        String[] radky = logText.split("\\r?\\n");
        for (String radek : radky) {
            if (!radek.contains("[CHAT]")) {
            	for (String keyword : keywords) {
            		if (radek.contains(keyword)) {
            			obsazeneKeywordy.add(keyword);
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
	
	private String ziskejDatumPosledniUpravy(File slozka) {
		return FORMAT_DATA.format(slozka.lastModified());
	}
	
	private long ziskejRozdilMeziDaty(String datum1, String datum2) {
		long rozdilVMinutach = 0;
	    	 try {
	 			long diff = FORMAT_DATA.parse(datum1).getTime() - FORMAT_DATA.parse(datum2).getTime();
	 			rozdilVMinutach = TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS);
	 		} catch (Exception e) {
	 			e.printStackTrace();
	 			chyby.add("nastal interní problém pøi porovnávání rozdílu mezi daty");
	 		}
	    	 return rozdilVMinutach;
	}
	
	public String prevedObsahSouboruNaString(String cestaKSouboru) { 
	    String data = null;
	    boolean compressed = false;
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
			 napisDiff = rozdil + " min. (" + folderModifiedDate + ")";
		}
		else if (rozdil >= oneHourInMins && rozdil < oneDayInMins) {
			napisDiff = rozdil/oneHourInMins + "h a " + rozdil % oneHourInMins + " min. (" + folderModifiedDate + ")";
		}
		else if (rozdil >= oneDayInMins) {
			napisDiff = rozdil/oneHourInMins/24 + " dnama a " + (rozdil/oneHourInMins) % 24 + " hodinama. (" + folderModifiedDate + ")";
		}
		return napisDiff;
	}
	
	public boolean jeJmenoNalezeno(String jmeno) {
		if (cestyKLogum.isEmpty()) {
			for (String cestaKLogu : new HledaniSouboru(new ArrayList<String>(Arrays.asList("gz")), null).prohledejSoubory(SLOZKA_LOGS, false, false, null)) {
				cestyKLogum.add(cestaKLogu);
			}
		}
		for (String cestaKLogu : cestyKLogum) {
			if (ziskejRozdilMeziDaty(FORMAT_DATA.format(new Date()), ziskejDatumPosledniUpravy(new File(cestaKLogu))) < 21600) {
				String obsahLogu = prevedObsahSouboruNaString(cestaKLogu);
				String regex = "\\b" + jmeno + "\\b";
				Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(obsahLogu);
				if (matcher.find()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void prerusKontrolu(String chyba, boolean ukazUzivateli) {
		System.err.println(chyba);
		chyba = ukazUzivateli ? chyba : "";
		JOptionPane.showMessageDialog(null, "Pøi kontrole došlo k chybì, která zabránila normální funkci programu. Žádná data nebudou odeslána. Chyba: " + chyba, "Chyba", JOptionPane.ERROR_MESSAGE);
			zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", true);
			zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", true);
		System.exit(0);
	}
	
	public void vynuceneUkonci() {
			zmenAtributSouboru(VLASTNI_SLOZKA, "dos:hidden", true);
			zmenAtributSouboru(PREDESLE_KONTROLY_INFO_TXT, "dos:hidden", true);
	}
	
	public void vypisStavKontroly(String stav) {
		System.out.println(stav);
	}

	@Override
	public void run() {
		zacniKontrolu();
	}
}