package com.gmail.matejpesl1.mcdp.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.zip.GZIPInputStream;

import com.gmail.matejpesl1.mcdp.frontend.Main;
import com.gmail.matejpesl1.mcdp.program.Inspection;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

public class FileUtils {
	private FileUtils() {
		
	}
	
	public static void changeFileAttribute(File file, String attributeToChange, boolean value) throws Exception {
		if (file.exists()) {
			Files.setAttribute(file.toPath(), attributeToChange, value);
		}
	}
	
	public static void writeToFile(File file, String text) throws Exception {
		boolean isHidden = false;
		boolean canWrite = true;
		
		if (!file.canWrite()) {
			canWrite = false;
			file.setWritable(true);
		}
		
		if (file.isHidden()) {
			isHidden = true;
			changeFileAttribute(file, "dos:hidden", false);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(text);
		writer.close();
		
		if (isHidden) {
			FileUtils.changeFileAttribute(file, "dos:hidden", true);
		}
		if (canWrite) {
			file.setWritable(true);
		}
	}
	
	public static String convertFileContentToString(File file) {
	    boolean fileExists = file.exists();
	    if (!fileExists) {
	    	return null;
	    }

		StringBuilder data = new StringBuilder();
	    try {
	    	FileInputStream stream = new FileInputStream(file);
			BufferedReader br;
			
			if (file.getName().endsWith(".gz")) {
				GZIPInputStream gzipInputStream = new GZIPInputStream(stream);
				Reader reader = new InputStreamReader(gzipInputStream, "windows-1250");
				br = new BufferedReader(reader);
			} else {
				Reader reader = new InputStreamReader(stream, "windows-1250");
				br = new BufferedReader(reader);
			}
			
			String line;
			while ((line = br.readLine()) != null) {
				data.append(line).append("\n");
			}
		br.close();
		} catch (Exception e) {
			e.printStackTrace();
			Main.globalErrors.add("Nastal interní problém pøi ètení obsahu souboru. ");
		}
	    return data.toString();
	  }
	
	public static void deleteFile(File file) throws IOException {
    	Files.deleteIfExists(file.toPath());
}
	
	public static LocalDateTime getLastModificationDate(File file) throws NullPointerException {
		long fileModifEpoch = file.lastModified();
		Instant instant = Instant.ofEpochMilli(fileModifEpoch);
		ZoneId zoneId = ZoneId.systemDefault();
		return instant.atZone(zoneId).toLocalDateTime();
	}
	
	public static void createOwnDir() {
		if (!Constants.OWN_DIR.exists()) {
		}
		boolean dirCreated = Constants.OWN_DIR.mkdir();
		if (!dirCreated) {
			Inspection.interruptInspection("Nastala chyba pøi vytváøení složky.", false, null, true);
		}
		Inspection.writeInitialInfo();
	}
	
	
	public static void createZip(File fileToZip, String zipDestination, String pass) throws Exception {
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
}
