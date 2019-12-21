package com.gmail.matejpesl1.mc_cheat_detection;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.Font;

public class Uvod {
	private static final Rezim rezim = Rezim.DEBUG;
	public static String nazevServeru;
	public static String emailRecipientAddress;
	private static final String NAZEV_SERVERU_BASICLAND = "mc.basicland.cz";
	private static final String EMAIL_RECIPIENT_ADDRESS_BASICLAND = "report@basicland.cz";
	private static final String NAZEV_SERVERU_DEBUG = "mc.DEBUG.cz";
	private static final String EMAIL_RECIPIENT_ADDRESS_DEBUG = "matejpesl1@gmail.com";
	public static final Pattern NEPOVOLENE_ZNAKY_VE_JMENE = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*() %!-]");
	public static final float VERZE_PROGRAMU = 2.4f;
	public static final int MAX_DELKA_JMENA = 16;
	public static final int MIN_DELKA_JMENA = 3;
	private static JFrame frame;
	private static JLabel lblHeader;
	private static JLabel lblStatus;
	private static JLabel lblNazevServeru;
	private static JPanel controlPanel;
	private static JButton btnZmenaOdpovedi;
	private static JTextField txtfJmeno;
	public static enum Podminka{PRIPOJENI_K_INTERNETU, SLOZKA_MINECRAFT};
	public static enum Rezim {DEBUG, BASICLAND};
	private static Kontrola kontrola;
	private static boolean kontrolaSpustena;
	   
	public static void main(String[] args) {
		kontrola = new Kontrola();
		kontrola.start();
		setServerDependantInfo(rezim);
		prepareGUI();
		if (rezim != Rezim.DEBUG) {
			ukazAktualizaceObrazovku();	
		}
		ukazAgreementObrazovku();
	}
	
	private static void setServerDependantInfo(Rezim rezim) {
		switch (rezim) {
			case DEBUG: {
				nazevServeru = NAZEV_SERVERU_DEBUG;
				emailRecipientAddress = EMAIL_RECIPIENT_ADDRESS_DEBUG;
			} break;
			case BASICLAND: {
				nazevServeru = NAZEV_SERVERU_BASICLAND;
				emailRecipientAddress = EMAIL_RECIPIENT_ADDRESS_BASICLAND;
			} break;
		}
	}
	
	private static void ukazAktualizaceObrazovku() {
		lblHeader.setText("<html>Prob�h� kontrola aktualizac�...</html>");
		Aktualizace aktualizace = null;
		try {
			aktualizace = new Aktualizace();	
		} catch (URISyntaxException e) {
			ukazChybu("Program nedok�zal naj�t cestu s�m k sob�.");
			e.printStackTrace();
			ukazAgreementObrazovku();
		} catch (IOException e) {
			ukazChybu("Nepoda�ila se ov��it dostupnost nov�j�� verze.");
			e.printStackTrace();
			ukazAgreementObrazovku();
		}
		
		if (aktualizace.isUpdateAvailable()) {
			lblHeader.setText("<html>Nov� verze nalezena! Prob�h� stahov�n�, po dokon�en� bude program restartov�n...</html>");
			try {
				aktualizace.downloadUpdate();	
			} catch (IOException e) {
				e.printStackTrace();
				ukazChybu("Nepoda�ilo se st�hnout novou verzi, pokra�uji se starou...");
				ukazAgreementObrazovku();
			}
			
			try {
				aktualizace.update();	
			} catch (IOException e) {
				e.printStackTrace();
				ukazChybu("Nepoda�ilo se prov�st aktualizaci, pokra�uji se starou verz�...");
				ukazAgreementObrazovku();
			}
			
		}
	}
	
	private static void ukazChybu(String chyba) {
		JOptionPane.showMessageDialog(null, "Nastala chyba: " + chyba, "Chyba", JOptionPane.ERROR_MESSAGE);	
	}
	
	private static void prepareGUI() {
		frame = new JFrame("Kontrola - " + nazevServeru + " | by matejpesl1@gmail.com " +  "[v" + VERZE_PROGRAMU + "]");
		frame.setSize(520, 130);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	      
		lblHeader = new JLabel("", JLabel.CENTER);
		lblStatus = new JLabel("", JLabel.CENTER);

		lblNazevServeru = new JLabel("<html><b>" + nazevServeru + "</b></html>", JLabel.CENTER);
		lblNazevServeru.setVisible(false);
		
		controlPanel = new JPanel();
		controlPanel.add(lblHeader);
		controlPanel.add(lblHeader);
		controlPanel.add(lblStatus);
		controlPanel.add(lblNazevServeru);
		
		lblHeader.setBounds(22, 15, 470, 29);
		lblStatus.setBounds(395, 15, 0, 0);
		controlPanel.setLayout(null);
		frame.getContentPane().add(controlPanel);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				kontrola.vynuceneUkonci();
				System.exit(0);
			}
	    });
		frame.setVisible(true);
	}
	
	private static void ukazAgreementObrazovku() {
		lblHeader.setText("<html>Souhlas�te s poskytnut�m n�zvu, typu a obsahu n�kter�ch sv�ch soubor� serveru?</html>");
		JButton btnNe = new JButton("<html>ne</html>");
		btnNe.setBounds(260, 48, 57, 29);
		controlPanel.add(btnNe);
		JButton btnAno = new JButton("<html>ano</html>");
		btnAno.setBounds(196, 48, 57, 29);
		controlPanel.add(btnAno);
		frame.revalidate();
		frame.repaint();
		btnAno.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnAno.setVisible(false);
				btnNe.setVisible(false);
				ukazZadejJmenoObrazovku();
			}
		});
	      
		btnNe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnAno.setVisible(false);
				btnNe.setVisible(false);
				ukazUzivatelNesouhlasilObrazovku();
			}
		});
	}
	
	private static void ukazZadejJmenoObrazovku() {
		lblHeader.setText("<html>Zadejte sv� jm�no ve h�e</html>");
		txtfJmeno = new JTextField();
		txtfJmeno.setBounds(179, 48, 117, 20);
		controlPanel.add(txtfJmeno);
		txtfJmeno.setColumns(13);
			     
		JButton btnPotvrdit = new JButton("<html>potvrdit</html>");
		btnPotvrdit.setBounds(301, 45, 85, 26);
		controlPanel.add(btnPotvrdit);
			      
		JLabel lblJmeno = new JLabel("<html>Jm�no:</html>");
		lblJmeno.setBounds(129, 48, 45, 20);
		controlPanel.add(lblJmeno);
			      
		JLabel lblNeplatneJmeno = new JLabel("<html>neplatn� jm�no</html>");
		lblNeplatneJmeno.setBounds(192, 70, 97, 20);
		controlPanel.add(lblNeplatneJmeno);
		lblNeplatneJmeno.setVisible(false);
		    	  
		btnPotvrdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String jmeno = txtfJmeno.getText(); 
				if (jmeno.length() >= MIN_DELKA_JMENA && jmeno.length() <= MAX_DELKA_JMENA &&
						!NEPOVOLENE_ZNAKY_VE_JMENE.matcher(jmeno).find() && Normalizer.isNormalized(jmeno, Normalizer.Form.NFD)) {
					lblHeader.setText("<html>prob�ha kontrola podm�nek p�ed kontrolou...</html>");
					lblHeader.paintImmediately(lblHeader.getVisibleRect());
					Podminka nesplnenaPodminka = ziskejNesplnenouPodminku();
					btnPotvrdit.setVisible(false);
					lblJmeno.setVisible(false);
					txtfJmeno.setVisible(false);
					lblNeplatneJmeno.setVisible(false);
					if (nesplnenaPodminka == null) {
						if (!new Kontrola().jeJmenoNalezeno(jmeno)) {
							String[] moznosti = {"I p�esto pokra�ovat", "zadat spr�vn� jm�no"};
							int vybranaMoznost = JOptionPane.showOptionDialog(null,
								"<html>Jm�no nebylo v PC nalezeno. V p��pad� pokra�ov�n� bude informace o z�d�n� potenci�ln� ciz�ho jm�na odesl�na. "
										+ "Opravdu chcete pokra�ovat?",
								"Jm�no nenalazeno!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, moznosti, moznosti[0]);
							
							if (vybranaMoznost == 0) {
								zacniKontrolu(jmeno, true);
								} else {
									ukazZadejJmenoObrazovku();
								}
							} else {
							  zacniKontrolu(jmeno, false);	 
							}
						} else {
							ukazNesplnenaPodminkaObrazovku(nesplnenaPodminka);
						}
					} else {
						lblNeplatneJmeno.setVisible(true);
					}
			}
		});
	}
	   
	private static Podminka ziskejNesplnenouPodminku() {
		try {
			final URL googleURL = new URL("https://www.google.com");
			final URLConnection connection1 = googleURL.openConnection();
			connection1.connect();
			connection1.setConnectTimeout(6000);
			connection1.setReadTimeout(4000);
			connection1.getInputStream().close();
			} catch (Exception e) {
				e.printStackTrace();
				return Podminka.PRIPOJENI_K_INTERNETU;
				}
		
		if (!Kontrola.SLOZKA_MINECRAFT.exists()) {
			return Podminka.SLOZKA_MINECRAFT;
		}
		
		return null;
	}
	   
	private static void zacniKontrolu(String jmeno, boolean pravdepodobneNespravneJmeno) {
		kontrolaSpustena = true;
		zmenStav("prob�h� kontrola");
		lblNazevServeru.setVisible(true);
		lblNazevServeru.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblNazevServeru.setBounds(54, 48, 405, 42);
		controlPanel.add(lblNazevServeru);
		kontrola.dodejUdaje(jmeno, pravdepodobneNespravneJmeno);
		synchronized (kontrola)  {
			kontrola.notify();   
		}
	}
	   
	public static void ukazDokoncenouKontrolu(ArrayList<String> chyby) {
		boolean supported = frame.isAlwaysOnTopSupported();
		if (supported) {
			frame.setAlwaysOnTop(true);
		}
		frame.toFront();
		frame.requestFocus();
		if (supported) {
			frame.setAlwaysOnTop(false);
	     }
		lblHeader.setText(chyby.size() > 0 ? "<html>Kontrola byla dokon�ena s chybama. V�sledky byly odesl�ny.</html>"
				: "<html>Kontrola byla dokon�ena bez chyb a v�sledky byly odesl�ny.</html>");
		   
		JButton btnOdejit = new JButton("<html>Odej�t</html>");
		btnOdejit.setBounds(403, 61, 89, 29);
		controlPanel.add(btnOdejit);
		frame.repaint();
		   
		btnOdejit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
	}
	   
	public static void zmenStav(String stav) {
		lblHeader.setText(stav + "...");
	}
	   
	public static void ukazUzivatelNesouhlasilObrazovku() {
		lblHeader.setText("<html>Kontrola nebude spu�t�na.</html>");			   
		JButton btnOdejit = new JButton("Odej�t");
		btnOdejit.setBounds(292, 48, 68, 29);
		controlPanel.add(btnOdejit);
			   
		btnZmenaOdpovedi = new JButton("<html>zm�nit odpov��</html>");
		btnZmenaOdpovedi.setBounds(155, 48, 133, 29);
		controlPanel.add(btnZmenaOdpovedi);
		controlPanel.add(lblHeader);
		btnOdejit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
			   
		btnZmenaOdpovedi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnZmenaOdpovedi.setVisible(false);
				btnOdejit.setVisible(false);
				ukazAgreementObrazovku();
			}
		});
	}
		
	public static void ukazNesplnenaPodminkaObrazovku(Podminka nesplnenaPodminka) {
		String duvodProZastaveniKontroly = null;
		switch (nesplnenaPodminka) {
		case PRIPOJENI_K_INTERNETU: duvodProZastaveniKontroly = "Chyb� p�ipojen� k internetu"; break;
		case SLOZKA_MINECRAFT: duvodProZastaveniKontroly = "Minecraft nebyl nalezen"; break;
		}
		
		lblHeader.setText("<html>Kontrola nem��e b�t spu�t�na. D�vod: <strong>" + duvodProZastaveniKontroly + "</strong></html>");
		
		JButton btnZkusitZnovu = new JButton("<html>Zkusit znovu</html>");
		btnZkusitZnovu.setBounds(148, 55, 105, 27);
		JButton btnOdejit = new JButton("<html>odej�t</html>");
		btnOdejit.setBounds(263, 55, 105, 27);
		
		controlPanel.add(btnZkusitZnovu);
		controlPanel.add(btnOdejit);
		controlPanel.repaint();
			
		btnOdejit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
			   
		btnZkusitZnovu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnZkusitZnovu.setVisible(false);
				btnOdejit.setVisible(false);
				ukazZadejJmenoObrazovku();
			}
		});
	}
		
	public static boolean kontrolaJeSpustena() {
		return kontrolaSpustena;
	}
	
	public static Rezim ziskejRezim() {
		return rezim;
	}
}