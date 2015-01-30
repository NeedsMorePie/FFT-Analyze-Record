package com.example.davis;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class VariableNoteFinder {
	
	int freqArrayLength = 100;
	
	String[][] freqArray = new String[freqArrayLength+1][2];
	
	String[] notes = {
			"G#/Ab",
			"A",
			"A#/Bb",
			"B",
			"C",
			"C#/Db",
			"D",
			"D#/Eb",
			"E",
			"F",
			"F#/Gb",
			"G"
	};
	
	public VariableNoteFinder(int calibrationValue) {
		populateArray(calibrationValue);
	}
	
	/**
	 * input: an index that's simplified to a number between 0-11 
	 * output: note
	 */
	public String findNote(int index) {
		String note = "N/A";
		int simplifiedIndex = index%12;
		
		for (int k=0; k<notes.length; k++){
			if (simplifiedIndex == k){
				note = notes[k];
			}
		}
		
		return note;
	}
	
	/**
	 * Populates an array filled with notes
	 * Called at the start of VariableNoteFinder
	 * @param calibration
	 */
	public void populateArray(double calibration) {
		for (int i=0; i<=freqArrayLength; i++){
			freqArray[i][0] = Double.toString((Math.pow(2, (double)(i-49)/12))*calibration); // default calibration is 440
			freqArray[i][1] = findNote(i);
		}
	}
	
	/**
	 * inputs a frequency and returns the note
	 * @param freq
	 * @return
	 */
	public String getNote(int freq) {
		double freqDifference = 100000;
		int freqArrayIndex = 1;
		
		for (int i=1; i<=freqArrayLength; i++){
			if (Math.abs(freq-(Double.parseDouble(freqArray[i][0]))) < freqDifference) { // compares differences
				freqDifference = Math.abs(freq-(Double.parseDouble(freqArray[i][0]))); 
				freqArrayIndex = i;
			}
		}
		
		if (freq == 0){
			return "N/A";
		} else {
			return freqArray[freqArrayIndex][1];
		}
	}
	
	/**
	 * inputs a frequency and returns the simplified note index only (a value from 0-11)
	 * @param freq
	 * @return
	 */
	public int getIndex(int freq) {
		double freqDifference = 100000;
		int freqArrayIndex = 1;
		int simplifiedIndex;
		
		for (int i=1; i<=freqArrayLength; i++){
			if (Math.abs(freq-(Double.parseDouble(freqArray[i][0]))) < freqDifference) {
				freqDifference = Math.abs(freq-(Double.parseDouble(freqArray[i][0])));
				freqArrayIndex = i;
			}
		}
		simplifiedIndex = freqArrayIndex%12;
		return simplifiedIndex;
	}
}
