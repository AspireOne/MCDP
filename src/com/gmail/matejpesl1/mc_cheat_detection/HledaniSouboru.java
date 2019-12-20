package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

public class HledaniSouboru implements FileVisitor<Path> {
	private ArrayList<String> jmenaSlozekProPreskoceni = new ArrayList<>();
	private ArrayList<String> vysledneSoubory = new ArrayList<>();
	private ArrayList<String> keywords = new ArrayList<>();
	private ArrayList<String> pripony = new ArrayList<>();
	private boolean vratNazvySouboru;
	private String jmenoPocatecniSlozky;
	public static final ArrayList<String> DEFAULTNI_PRIPONY_K_PRESKOCENI = new ArrayList<>(Arrays.asList("ogg", "wav", "mp3", "ps", "snt", "nbt"));
	
	public HledaniSouboru(ArrayList<String> pripony, ArrayList<String> keywords) {
		this.pripony = pripony;
		this.keywords = keywords;
	}
	
	public HledaniSouboru(String pripona, ArrayList<String> keywords) {
		if (pripona == null) {
			this.pripony = null;
		} else {
			ArrayList<String> priponaArr = new ArrayList<>(Arrays.asList(pripona));
			this.pripony = priponaArr;
		}
		this.keywords = keywords;
	}

	public ArrayList<String> prohledejSoubory(String slozka, boolean zahrnoutPodslozky, boolean vratNazvySouboru, ArrayList<String> jmenaSlozekProPreskoceni) {
		return prohledejSoubory(Paths.get(slozka), zahrnoutPodslozky, vratNazvySouboru, jmenaSlozekProPreskoceni);
	}
	
	public ArrayList<String> prohledejSoubory(File slozka, boolean zahrnoutPodslozky, boolean vratNazvySouboru, ArrayList<String> jmenaSlozekProPreskoceni) {
		return prohledejSoubory(Paths.get(slozka.getPath()), zahrnoutPodslozky, vratNazvySouboru, jmenaSlozekProPreskoceni);
	}
	
	public ArrayList<String> prohledejSoubory(Path slozka, boolean zahrnoutPodslozky, boolean vratNazvySouboru, ArrayList<String> jmenaSlozekProPreskoceni) {
		this.vratNazvySouboru = vratNazvySouboru;
		this.jmenaSlozekProPreskoceni = jmenaSlozekProPreskoceni;
		jmenoPocatecniSlozky = slozka.getFileName().toString();
		
		try {
			Files.walkFileTree(slozka, EnumSet.noneOf(FileVisitOption.class), zahrnoutPodslozky ? Integer.MAX_VALUE : 1, this);
		} catch (IOException e) {
			e.printStackTrace();
			
		}
		return vysledneSoubory;
	}
	
	
	
	
	@Override
	public FileVisitResult preVisitDirectory(Path slozka, BasicFileAttributes atributy) throws IOException {
		String nazevSlozky = slozka.getFileName().toString();
		String cestaKeSlozceStr = slozka.toString();
		if (jmenaSlozekProPreskoceni != null && jmenaSlozekProPreskoceni.parallelStream().anyMatch(nazevSlozky::equals)) {
			return FileVisitResult.SKIP_SUBTREE;
		}
		
		if (nazevSlozky.equals(jmenoPocatecniSlozky) || pripony != null) {
			return FileVisitResult.CONTINUE;
		}
		
		if (keywords != null) {
			if (keywords.parallelStream().anyMatch(nazevSlozky.toLowerCase().trim()::contains)) {
				vysledneSoubory.add(vratNazvySouboru ? nazevSlozky + " (složka)": cestaKeSlozceStr);
			}
		} else {
			vysledneSoubory.add(vratNazvySouboru ? nazevSlozky : cestaKeSlozceStr);	
		}
		
		return FileVisitResult.CONTINUE;
	}

	
	
	
	@Override
	public FileVisitResult visitFile(Path soubor, BasicFileAttributes atributy) throws IOException {
		String nazevSouboru = soubor.getFileName().toString();
		String cestaKSouboruStr = soubor.toString();
		String priponaSouboru = nazevSouboru.substring(nazevSouboru.lastIndexOf(".") + 1);
		boolean priponaEquals = false;
		boolean keywordEquals = false;
		if (DEFAULTNI_PRIPONY_K_PRESKOCENI.parallelStream().anyMatch(priponaSouboru.toLowerCase().trim()::matches)) {
			return FileVisitResult.CONTINUE;
		}
		
		if (pripony != null) {
			for (String pripona : pripony) {
				if (priponaSouboru.equals(pripona)) {
					priponaEquals = true;
				}
			}
		}
		
		if (priponaEquals == false && pripony != null) {
			return FileVisitResult.CONTINUE;
		}
		if (keywords != null) {
			boolean obsahujeKeyword = keywords.parallelStream().anyMatch(nazevSouboru.toLowerCase().trim()::contains);
			if (obsahujeKeyword) {
				keywordEquals = true;
			} 
		}
		
		if (keywordEquals == false && keywords != null) {
			return FileVisitResult.CONTINUE;
		}
		
		vysledneSoubory.add(vratNazvySouboru ? nazevSouboru : cestaKSouboruStr);
		return FileVisitResult.CONTINUE;
	}
	
	
	
	
	@Override
	public FileVisitResult visitFileFailed(Path soubor, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	
	
	
	@Override
	public FileVisitResult postVisitDirectory(Path slozka, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}
}