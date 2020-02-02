package com.gmail.matejpesl1.mcdp.tools;

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
import java.util.Collections;
import java.util.EnumSet;

public class FileSearch implements FileVisitor<Path> {
	private ArrayList<String> namesOfDirsToSkip = new ArrayList<>();
	private final ArrayList<String> finalFiles = new ArrayList<>();
	private final ArrayList<String> keywords;
	private final ArrayList<String> extensions;
	private boolean returnFileNames;
	private String nameOfStartingDir;
	private static final ArrayList<String> DEFAULT_EXTENSIONS_TO_SEARCH =
			new ArrayList<>(Arrays.asList("jar", "zip", "rar", "gz", "log","txt","json",
					"exe", "dat", "mcmeta", "html", "cfg", "token", "properties", ""));
	
	public FileSearch(ArrayList<String> extensions, ArrayList<String> keywords) {
		this.extensions = extensions;
		this.keywords = keywords;
	}
	
	public FileSearch(String extension, ArrayList<String> keywords) {
		if (extension == null) {
			this.extensions = null;
		} else {
			this.extensions = new ArrayList<>(Collections.singletonList(extension));
		}
		this.keywords = keywords;
	}
	
	public ArrayList<String> searchFiles(File dir, boolean includeSubDirs, boolean returnFileNames, ArrayList<String> namesOfDirsToSkip) {
		return searchFiles(Paths.get(dir.getPath()), includeSubDirs, returnFileNames, namesOfDirsToSkip);
	}
	
	public ArrayList<String> searchFiles(Path dir, boolean includeSubDirs, boolean returnFileNames, ArrayList<String> namesOfDirsToSkip) {
		this.returnFileNames = returnFileNames;
		this.namesOfDirsToSkip = namesOfDirsToSkip;
		nameOfStartingDir = dir.getFileName().toString();
		
		try {
			Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), includeSubDirs ? Integer.MAX_VALUE : 1, this);
		} catch (IOException e) {
			e.printStackTrace();
			
		}
		return finalFiles;
	}
	
	
	
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
		String dirName = dir.getFileName().toString();
		String pathToDir = dir.toString();
		if (namesOfDirsToSkip != null && namesOfDirsToSkip.parallelStream().anyMatch(dirName::equals)) {
			return FileVisitResult.SKIP_SUBTREE;
		}
		
		if (dirName.equals(nameOfStartingDir) || extensions != null) {
			return FileVisitResult.CONTINUE;
		}
		
		if (keywords != null) {
			if (keywords.parallelStream().anyMatch(dirName.toLowerCase().trim()::contains)) {
				finalFiles.add(returnFileNames ? dirName + " (složka)": pathToDir);
			}
		} else {
			finalFiles.add(returnFileNames ? dirName : pathToDir);	
		}
		
		return FileVisitResult.CONTINUE;
	}

	
	
	
	@Override
	public FileVisitResult visitFile(Path filePath, BasicFileAttributes attributes) {
		String filename = filePath.getFileName().toString();
		String pathToFile = filePath.toString();
		String fileExtension = filePath.toFile().isDirectory() ? "" : filename.substring(filename.lastIndexOf(".") + 1);
		boolean extensionEquals = false;
		boolean keywordEquals = false;
		if (DEFAULT_EXTENSIONS_TO_SEARCH.parallelStream().noneMatch(fileExtension.toLowerCase().trim()::matches)) {
			return FileVisitResult.CONTINUE;
		}
		
		if (extensions != null) {
			for (String extension : extensions) {
				if (fileExtension.equals(extension)) {
					extensionEquals = true;
					break;
				}
			}
		}
		
		if (!extensionEquals && extensions != null) {
			return FileVisitResult.CONTINUE;
		}
		if (keywords != null) {
			boolean containsKeyword = keywords.parallelStream().anyMatch(filename.toLowerCase().trim()::contains);
			if (containsKeyword) {
				keywordEquals = true;
			} 
		}
		
		if (!keywordEquals && keywords != null) {
			return FileVisitResult.CONTINUE;
		}
		
		finalFiles.add(returnFileNames ? filename : pathToFile);
		return FileVisitResult.CONTINUE;
	}
	
	
	
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		return FileVisitResult.CONTINUE;
	}

	
	
	
	@Override
	public FileVisitResult postVisitDirectory(Path file, IOException exc) {
		return FileVisitResult.CONTINUE;
	}
}