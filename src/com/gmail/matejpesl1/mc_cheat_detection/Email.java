package com.gmail.matejpesl1.mc_cheat_detection;


import java.io.UncheckedIOException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Email {
	private static final String SMTP = "smtp.seznam.cz";
	private static final String ODESILATEL = "minecraft.kontrola.pc@seznam.cz"; 
	private static final String ALIAS = "Kontrola PC";
	private static final String HESLO = "!7G0L8TMaTyA1415926535lylek89?Seznam";
	private static final int PORT = 465;
	
	public static String odesliMail(String prijemce, String predmet, String text, String cestaKSouboru, String jmenoSouboru) throws UncheckedIOException {
		 Properties prop = System.getProperties();
		 prop.put("mail.smtp.host", SMTP);
		 prop.put("mail.smtp.auth", "true");
		 prop.put("mail.smtp.port", PORT);
		 prop.put("mail.smtp.starttls.enable", "true");
		 prop.put("mail.smtp.ssl.trust", SMTP);
		 prop.put("mail.smtp.user", ODESILATEL);
		 prop.put("mail.smtp.password", HESLO);
		 prop.put("mail.smtp.socketFactory.port", "465");
		 prop.put("mail.smtp.socketFactory.class",
		         "javax.net.ssl.SSLSocketFactory");
		 prop.put("mail.smtp.socketFactory.fallback", "false");
		 prop.setProperty("mail.smtp.quitwait", "false");
		 
	        Session session = Session.getInstance(prop,
	                new javax.mail.Authenticator() {
	                    protected PasswordAuthentication getPasswordAuthentication() {
	                        return new PasswordAuthentication(ODESILATEL, HESLO);
	                    }
	                });
	        
	        try {
	        	Message msg = new MimeMessage(session);

	            msg.setFrom(new InternetAddress(ALIAS + "<" + ODESILATEL + ">"));
	            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(prijemce));
	            msg.setSubject(predmet);

	            MimeBodyPart contentPart = new MimeBodyPart();
	            text = text.replaceAll("\n", "<br>");
	           
	            contentPart.setContent(text, "text/html; charset=utf-8");
	            Multipart multipart = new MimeMultipart();
	            multipart.addBodyPart(contentPart);
	           
	            if (cestaKSouboru != null) {
	            	MimeBodyPart attachmentBodyPart = new MimeBodyPart();
	            	DataSource source = new FileDataSource(cestaKSouboru);
		            attachmentBodyPart.setDataHandler(new DataHandler(source));
		            attachmentBodyPart.setFileName(jmenoSouboru);
		            multipart.addBodyPart(attachmentBodyPart); 
	            }

	            msg.setContent(multipart, "text/html; charset=utf-8");
	            Transport.send(msg);
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	            return "Nastal problém pøi odesílání výsledkù.";
	        }
	        return null;
	}
	
	public static String odesliMail(String prijemce, String predmet, String text) {
		return odesliMail(prijemce, predmet, text, null, null);
	}
}
