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
import com.gmail.matejpesl1.servers.*;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.Font;

public class Uvod {
	public static final Rezim rezim = Rezim.DEBUG;
	public static final Pattern NEPOVOLENE_ZNAKY_VE_JMENE = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*() %!-]");
	public static final float VERZE_PROGRAMU = 2.4f;
	public static final int MAX_DELKA_JMENA = 16;
	public static final int MIN_DELKA_JMENA = 3;
	private static final JFrame frame = new JFrame();
	private static final JLabel lblHeader = new JLabel("", JLabel.CENTER);
	private static final JLabel lblStatus = new JLabel("", JLabel.CENTER);
	private static final JLabel lblNazevServeru = new JLabel("", JLabel.CENTER);
	private static final JPanel panel = new JPanel();
	private static final JTextField txtfJmeno = new JTextField();
	private static boolean kontrolaSpustena = false;
	public static enum Podminka{PRIPOJENI_K_INTERNETU, SLOZKA_MINECRAFT};
	public static enum Rezim {DEBUG, BASICLAND};
	private Kontrola kontrola;
	private Server currentServer;
	   
	public static void main(String[] args) {
		Uvod uvod = new Uvod();
		uvod.beginning();
	}
	
	private void beginning() {
		kontrola = new Kontrola(this);
		kontrola.start();
		currentServer = determineServer(rezim);
		try {
			SplashScreen splash =
					new SplashScreen(currentServer.getBorderColor(), currentServer.getLogo(), 4000);
			splash.run();	
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		prepareGUI();
		
		if (rezim != Rezim.DEBUG) {
			ukazAktualizaceObrazovku();	
		}
		
		ukazAgreementObrazovku();
	}
	
	private Server determineServer(Rezim rezim) {
		Server currentServer = null;
		switch (rezim) {
			case DEBUG: {
				currentServer = new Debug();
			} break;
			case BASICLAND: {
				currentServer = new Basicland();
			} break;
		}
		return currentServer;
	}
	
	private void ukazAktualizaceObrazovku() {
		lblHeader.setText("<html>Probíhá kontrola aktualizací...</html>");
		Aktualizace aktualizace = null;
		try {
			aktualizace = new Aktualizace();
		} catch (URISyntaxException e) {
			String chyba = "Program nedokázal najít cestu sám k sobì.";
			ukazChybu(chyba);
			kontrola.chyby.add(chyba);
			e.printStackTrace();
			ukazAgreementObrazovku();
		} catch (IOException e) {
			String chyba = "Nepodaøila se ovìøit dostupnost novìjší verze.";
			ukazChybu(chyba);
			kontrola.chyby.add(chyba);
			e.printStackTrace();
			ukazAgreementObrazovku();
		}
		
		if (aktualizace.isUpdateAvailable()) {
			lblHeader.setText("<html>Nová verze nalezena! Probíhá stahování, po dokonèení bude program restartován...</html>");
			try {
				aktualizace.downloadUpdate();	
			} catch (IOException e) {
				e.printStackTrace();
				String chyba = "Nepodaøilo se stáhnout novou verzi, pokraèuji se starou...";
				ukazChybu(chyba);
				kontrola.chyby.add(chyba);
				ukazAgreementObrazovku();
			}
			
			try {
				aktualizace.update();	
			} catch (IOException e) {
				e.printStackTrace();
				String chyba = "Aktualizace se nezdaøila, pokraèuji se starou verzí...";
				ukazChybu(chyba);
				kontrola.chyby.add(chyba);
				ukazAgreementObrazovku();
			}
			
		}
	}
	
	private void ukazChybu(String chyba) {
		JOptionPane.showMessageDialog(null, "Nastala chyba: " + chyba, "Chyba", JOptionPane.ERROR_MESSAGE);	
	}
	
	private void prepareGUI() {
		frame.setTitle("Kontrola - " + currentServer.getIP() + " | by matejpesl1@gmail.com " +  "[v" + VERZE_PROGRAMU + "]");
		frame.setSize(520, 130);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		lblNazevServeru.setText("<html><b>" + currentServer.getIP() + "</b></html>");
		lblNazevServeru.setVisible(false);
		
		panel.add(lblHeader);
		panel.add(lblHeader);
		panel.add(lblStatus);
		panel.add(lblNazevServeru);
		
		lblHeader.setBounds(22, 15, 470, 29);
		lblStatus.setBounds(395, 15, 0, 0);
		panel.setLayout(null);
		frame.getContentPane().add(panel);
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
	
	private void ukazAgreementObrazovku() {
		lblHeader.setText("<html>Souhlasíte s poskytnutím názvu, typu a obsahu nìkterých svých souborù serveru?</html>");
		JButton btnNe = new JButton("<html>ne</html>");
		btnNe.setBounds(260, 48, 57, 29);
		panel.add(btnNe);
		JButton btnAno = new JButton("<html>ano</html>");
		btnAno.setBounds(196, 48, 57, 29);
		panel.add(btnAno);
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
	
	private void ukazZadejJmenoObrazovku() {
		lblHeader.setText("<html>Zadejte své jméno ve høe</html>");
		txtfJmeno.setBounds(179, 48, 117, 20);
		panel.add(txtfJmeno);
		txtfJmeno.setColumns(13);
			     
		JButton btnPotvrdit = new JButton("<html>potvrdit</html>");
		btnPotvrdit.setBounds(301, 45, 85, 26);
		panel.add(btnPotvrdit);
			      
		JLabel lblJmeno = new JLabel("<html>Jméno:</html>");
		lblJmeno.setBounds(129, 48, 45, 20);
		panel.add(lblJmeno);
			      
		JLabel lblNeplatneJmeno = new JLabel("<html>neplatné jméno</html>");
		lblNeplatneJmeno.setBounds(192, 70, 97, 20);
		panel.add(lblNeplatneJmeno);
		lblNeplatneJmeno.setVisible(false);
		    	  
		btnPotvrdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String jmeno = txtfJmeno.getText(); 
				if (jmeno.length() >= MIN_DELKA_JMENA && jmeno.length() <= MAX_DELKA_JMENA &&
						!NEPOVOLENE_ZNAKY_VE_JMENE.matcher(jmeno).find() && Normalizer.isNormalized(jmeno, Normalizer.Form.NFD)) {
					lblHeader.setText("<html>probíha kontrola podmínek pøed kontrolou...</html>");
					lblHeader.paintImmediately(lblHeader.getVisibleRect());
					Podminka nesplnenaPodminka = ziskejNesplnenouPodminku();
					btnPotvrdit.setVisible(false);
					lblJmeno.setVisible(false);
					txtfJmeno.setVisible(false);
					lblNeplatneJmeno.setVisible(false);
					if (nesplnenaPodminka == null) {
						if (!new Kontrola(null).jeJmenoNalezeno(jmeno)) {
							String[] moznosti = {"I pøesto pokraèovat", "zadat správné jméno"};
							int vybranaMoznost = JOptionPane.showOptionDialog(null,
								"<html>Jméno nebylo v PC nalezeno. V pøípadì pokraèování bude informace o zádání potenciálnì cizího jména odeslána. "
										+ "Opravdu chcete pokraèovat?",
								"Jméno nenalazeno!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, moznosti, moznosti[0]);
							
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
	   
	private Podminka ziskejNesplnenouPodminku() {
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
	   
	private void zacniKontrolu(String jmeno, boolean pravdepodobneNespravneJmeno) {
		kontrolaSpustena = true;
		zmenStav("probíhá kontrola");
		lblNazevServeru.setVisible(true);
		lblNazevServeru.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblNazevServeru.setBounds(54, 48, 405, 42);
		panel.add(lblNazevServeru);
		kontrola.dodejUdaje(jmeno, pravdepodobneNespravneJmeno);
		synchronized (kontrola)  {
			kontrola.notify();   
		}
	}
	   
	public void ukazDokoncenouKontrolu(ArrayList<String> chyby) {
		boolean supported = frame.isAlwaysOnTopSupported();
		if (supported) {
			frame.setAlwaysOnTop(true);
		}
		frame.toFront();
		frame.requestFocus();
		if (supported) {
			frame.setAlwaysOnTop(false);
	     }
		lblHeader.setText(chyby.size() > 0 ? "<html>Kontrola byla dokonèena s chybama. Výsledky byly odeslány.</html>"
				: "<html>Kontrola byla dokonèena bez chyb a výsledky byly odeslány.</html>");
		   
		JButton btnOdejit = new JButton("<html>Odejít</html>");
		btnOdejit.setBounds(403, 61, 89, 29);
		panel.add(btnOdejit);
		frame.repaint();
		   
		btnOdejit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
	}
	   
	public void zmenStav(String stav) {
		lblHeader.setText(stav + "...");
	}
	   
	public void ukazUzivatelNesouhlasilObrazovku() {
		lblHeader.setText("<html>Kontrola nebude spuštìna.</html>");
		
		JButton btnOdejit = new JButton("Odejít");
		JButton btnZmenaOdpovedi = new JButton("zmìnit odpovìï");
		
		btnOdejit.setBounds(292, 48, 68, 29);
		btnZmenaOdpovedi.setBounds(155, 48, 133, 29);
		
		panel.add(lblHeader);
		panel.add(btnZmenaOdpovedi);
		panel.add(btnOdejit);
		
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
		
	public void ukazNesplnenaPodminkaObrazovku(Podminka nesplnenaPodminka) {
		String duvodProZastaveniKontroly = null;
		switch (nesplnenaPodminka) {
		case PRIPOJENI_K_INTERNETU: duvodProZastaveniKontroly = "Chybý pøipojení k internetu"; break;
		case SLOZKA_MINECRAFT: duvodProZastaveniKontroly = "Minecraft nebyl nalezen"; break;
		}
		
		lblHeader.setText("<html>Kontrola nemùže být spuštìna. Dùvod: <strong>" + duvodProZastaveniKontroly + "</strong></html>");
		
		JButton btnZkusitZnovu = new JButton("<html>Zkusit znovu</html>");
		btnZkusitZnovu.setBounds(148, 55, 105, 27);
		JButton btnOdejit = new JButton("<html>odejít</html>");
		btnOdejit.setBounds(263, 55, 105, 27);
		
		panel.add(btnZkusitZnovu);
		panel.add(btnOdejit);
		panel.repaint();
		
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
	
	public Server getCurrentServer() {
		return currentServer;
	}
}