package com.gmail.matejpesl1.mcdp.program;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Player {
	private boolean nameWrong;
	private boolean isHacker;
	private byte hackerIndicationsCounter;
	private StringBuilder hackerIndications = new StringBuilder();
	private String name;
	private long timeSinceLastInspectionMin;
	private LocalDateTime lastInspectionDate;
	private final List<String> foundHacks = Collections.synchronizedList(new ArrayList<>());
	private short totalInspectionsNumber;
	private ArrayList<String> logLinesContainingKeyword = new ArrayList<>();
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setNameWrong(boolean nameWrong) {
		this.nameWrong = nameWrong;
	}
	
	public boolean isNameWrong() {
		return nameWrong;
	}
	
	public void setHacker(boolean isHacker) {
		this.isHacker = isHacker;
	}
	
	public boolean isHacker() {
		return isHacker;
	}
	
	public void addHackerIndication(String indication) {
		setHacker(true);
		hackerIndications.append(++hackerIndicationsCounter).append(". ").append(indication).append("\n");
	}
	
	public void addHackerIndications(ArrayList<String> indications) {
		setHacker(true);
		for (String indication : indications) {
			addHackerIndication(indication);
		}
		
	}
	
	public StringBuilder getHackerIndications() {
		return hackerIndications;
	}
	
	public void setLastInspectionDate(LocalDateTime date) {
		this.lastInspectionDate = date;
	}
	
	public LocalDateTime getLastInspectionDate() {
		return lastInspectionDate;
	}
	
	public void addFoundHack(String hack) {
		foundHacks.add(hack);
	}
	
	public void addFoundHacks(ArrayList<String> hacks) {
		foundHacks.addAll(hacks);
	}
	
	public List<String> getFoundHacks() {
		return foundHacks;
	}
	
	public void setTotalInspectionsNumber(short number) {
		totalInspectionsNumber = number;
	}
	
	public short getTotalInspectionsNumber() {
		return totalInspectionsNumber;
	}
	
	public void addLogLineContainingKeyword(String line) {
		logLinesContainingKeyword.add(line);
	}
	
	public void addLogLinesContainingKeyword(ArrayList<String> lines) {
		logLinesContainingKeyword.addAll(lines);
	}
	
	public ArrayList<String> getLogLinesContainingKeyword() {
		return logLinesContainingKeyword;
	}
	
	public void setTimeSinceLastInspectionMins(long time) {
		this.timeSinceLastInspectionMin = time;
	}
	
	public long getTimeSinceLastInspection(TimeUnit unit) {
		switch (unit) {
		case NANOSECONDS: return TimeUnit.MINUTES.toNanos(timeSinceLastInspectionMin);
		case MICROSECONDS: return TimeUnit.MINUTES.toMicros(timeSinceLastInspectionMin);
		case MILLISECONDS: return TimeUnit.MINUTES.toMillis(timeSinceLastInspectionMin);
		case SECONDS: return TimeUnit.MINUTES.toSeconds(timeSinceLastInspectionMin);
		case MINUTES: return timeSinceLastInspectionMin;
		case HOURS: return TimeUnit.MINUTES.toHours(timeSinceLastInspectionMin);
		case DAYS: return TimeUnit.MINUTES.toDays(timeSinceLastInspectionMin);
		}
		return -1;
	}
}
