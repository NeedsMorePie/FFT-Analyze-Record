package com.example.davis;

public class NoteFinder {
	
	String[][] freqArray = {
			{"261", "C"},
			{"277", "C#/Db"},
			{"293", "D"},
			{"311", "D#/Eb"},
			{"329", "E"},
			{"349", "F"},
			{"370", "F#/Gb"},
			{"392", "G"},
			{"415", "G#/Ab"},
			{"440", "A"},
			{"466", "A#/Bb"},
			{"494", "B"},
			{"523", "C"},
			{"554", "C#/Db"},
			{"587", "D"},
			{"622", "D#/Eb"},
			{"659", "E"},
			{"698", "F"},
			{"740", "F#/Gb"},
			{"784", "G"},
			{"831", "G#/Ab"},
			{"880", "A"},
			{"932", "A#/Bb"},
			{"988", "B"},
			{"1046", "C"},
			{"1109", "C#/Db"},
			{"1175", "D"},
			{"1245", "D#/Eb"},
			{"1319", "E"},
			{"1390", "F"},
			{"1480", "F#/Gb"},
			{"1568", "G"},
			{"1661", "G#/Ab"},
			{"1760", "A"},
			{"1865", "A#/Bb"},
			{"1976", "B"},
			{"2093", "C"},
			{"2217", "C#/Db"},
			{"2349", "D"},
			{"2489", "D#/Eb"},
			{"2637", "E"},
			{"2794", "F"},
			{"2960", "F#/Gb"},
			{"3136", "G"},
			{"3322", "G#/Ab"},
			{"3520", "A"},
			{"3729", "A#/Bb"},
			{"3951", "B"},
			{"4186", "C"},
			{"4435", "C#/Db"},
			{"4699", "D"},
			{"4978", "D#/Eb"},
			{"5274", "E"},
			{"5588", "F"},
			{"5920", "F#/Gb"},
			{"6272", "G"},
			{"6645", "G#/Ab"},
			{"7040", "A"},
			{"7459", "A#/Bb"},
			{"7902", "B"},
	};
	
	public String getNote(int freq) {
		int freqDifference = 100000;
		int freqArrayIndex = 0;
		for (int i=0; i<freqArray.length; i++){
			if (Math.abs(freq-(Integer.parseInt(freqArray[i][0]))) < freqDifference) {
				freqDifference = Math.abs(freq-(Integer.parseInt(freqArray[i][0])));
				freqArrayIndex = i;
			}
		}
		return freqArray[freqArrayIndex][1];
	}
}
