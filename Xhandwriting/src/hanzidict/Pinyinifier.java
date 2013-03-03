/*
 * Copyright (C) 2004 Jordan Kiang
 * jordan-at-kiang.org
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package hanzidict;

/**
 * Contains a static method for converting pinyin Strings with a numeric tone
 * them into a String using accents to show the tone.
 */
public class Pinyinifier {
	
	/**
	 *  take a string of the form of the phonetic pinyin immediately followed by the
	 *  number of the tone of the pronunciation, 1-5, return a string representing the
	 *  pinyin with properly accented characters.
	 *
	 *  tone 5 for neuter tone
	 *  v for u with umlauts
	 *
	 *  example: wo3 for the chinese for "I"
	 *
	 *  @param pinyinRaw pinyin of the form (pinyin)(tone#), ie: wo3 for chinese for "I"
	 *  @return accented string of the pinyin, null if input doesn't appear to be pinyin
	 */
	public static String pinyinify(String pinyinRaw) {
		
		// return string we append to
		String pinyinified = "";
		
		// no pinyin string should be less than two characters (one for a single letter, 1 for tone)
		if(pinyinRaw.length() < 2)
			return null;
		
		// the number of the of the tone is the last character in the raw string
		char toneChar = pinyinRaw.charAt(pinyinRaw.length() - 1);	
		int tone = -1;
		
		// check the tone number, store the proper tone, 1-5
		switch(toneChar) {
		case '1':
			tone = 1;
			break;
		case '2':
			tone = 2;
			break;
		case '3':
			tone = 3;
			break;
		case '4':
			tone = 4;
			break;
		case '5':
			tone = 5;
			break;
		}
		
		// the last character of the raw string wasn't a tone number
		if(tone < 0)
			return null;
		
		// find the position of the character in the string that should take the tone
		int tonePos = findTonePos(pinyinRaw);	
		
		// cycle through the characters, and append them raw if they don't carry the tone,
		// or append the accented form of the character if they do carry the tone
		// < length - 1 because length - 1 is the tone num
		for(int j = 0; j < pinyinRaw.length() - 1; j++) {
			char nextChar = pinyinRaw.charAt(j);
			
			if(j == tonePos) {
				// this character carries the tone, figure out which character it is, then store a
				// string that represents the unicode form of the accented character
				char accentedChar = 0;
				if(nextChar == 'a') {
					switch(tone) {
					case 1:
						accentedChar = '\u0101';
						break;
					case 2:
						accentedChar = '\u00e1';
						break;
					case 3:
						accentedChar = '\u0103';
						break;
					case 4:
						accentedChar = '\u00e0';
						break;
					case 5:
						accentedChar = '\u0061';
						break;
					}
				} else if(nextChar == 'o') {
					switch(tone) {
					case 1:
						accentedChar = '\u014d';
						break;
					case 2:
						accentedChar = '\u00f3';
						break;
					case 3:
						accentedChar = '\u014f';
						break;
					case 4:
						accentedChar = '\u00f2';
						break;
					case 5:
						accentedChar = '\u006f';
						break;
					}
				} else if(nextChar == 'e') {
					switch(tone) {
					case 1:
						accentedChar = '\u0113';
						break;
					case 2:
						accentedChar = '\u00e9';
						break;
					case 3:
						accentedChar = '\u0115';
						break;
					case 4:
						accentedChar = '\u00e8';
						break;
					case 5:
						accentedChar = '\u0065';
						break;
					}
				} else if(nextChar == 'i') {
					switch(tone) {
					case 1:
						accentedChar = '\u012b';
						break;
					case 2:
						accentedChar = '\u00ed';
						break;
					case 3:
						accentedChar = '\u012d';
						break;
					case 4:
						accentedChar = '\u00ec';
						break;
					case 5:
						accentedChar = '\u0069';
						break;
					}
				} else if(nextChar == 'u') {
					switch(tone) {
					case 1:
						accentedChar = '\u016b';
						break;
					case 2:
						accentedChar = '\u00fa';
						break;
					case 3:
						accentedChar = '\u016d';
						break;
					case 4:
						accentedChar = '\u00f9';
						break;
					case 5:
						accentedChar = '\u0075';
						break;
					}
				} else if(nextChar == 'v') {
					switch(tone) {
					case 1:
						accentedChar = '\u01d6';
						break;
					case 2:
						accentedChar = '\u01d8';
						break;
					case 3:
						accentedChar = '\u01da';
						break;
					case 4:
						accentedChar = '\u01dc';
						break;
					case 5:
						accentedChar = '\u00fc';
						break;
					}
				}
				
				if(accentedChar > 0) {
					pinyinified += accentedChar;
				}
				
			} else {
				// this character doesn't carry the tone, append it raw
				pinyinified += nextChar;
			}
		} 
		
		return pinyinified;
	}
	
	/** 
	 *  return the position of the accent in the given pinyin syllable
	 *  rules: a or e always take accent, o in ou takes accent, otherwise
	 *  its the last vowel
	 *
	 *  @param syllable the pinyin syllable
	 *  @return the position of the syllable, -1 if the pinyin looks bad
	 */
	private static int findTonePos(String syllable) {
		
		boolean tempPosSet = false;
		int tempPos = -1;
		
		// run through the characters in the string one by one
		for(int i = 0; i < syllable.length(); i++) {
			
			char nextChar = syllable.charAt(i);
			switch(nextChar) {		
			case 'a':
			case 'e':
				// a's and e's always take accents
				return i;
				
			case 'i':
			case 'v':
				// v for u with umlauts
				// for an i or a v, the later one should always take the accent
				
				tempPos = i;
				continue;
				
			case 'o':
			case 'u':
				// o's and u's should only take an accent if an accent has not already been set
				
				if(!tempPosSet) {
					if (i + 1 < syllable.length() && syllable.charAt(i) == 'u' && syllable.charAt(i+1) == 'o'){
						tempPos = i + 1;
					}else
						tempPos = i;
					
					tempPosSet = true;
				}
				continue;
			}
		}
		
		return tempPos;
	}
}
