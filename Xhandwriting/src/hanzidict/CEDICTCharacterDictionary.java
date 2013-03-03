package hanzidict;

import hanzidict.CharacterDictionary.Entry.Definition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

import com.fhx.app.xhandwriting.Xhandwriting;

//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;

import android.util.Log;

/**
 * @author jkiang
 *
 * A real-time parser for reading character data from a CEDICT formatted UTF-8 file.
 * To keep the memory footprint down, this class doesn't store dictionary data in memory.
 * Instead, it scans the CEDICT file when initialized and indexes the positions where
 * the entries for the various characters.  When a lookup operation is performed,
 * the resource is reread and the parser skips to the relevant position in the resource
 * stream.
 * 
 * Note this would be faster if we had random access to a Resource.  Might be faster
 * if we chose to use a RandomAccessFile instead, but then we couldn't read from a
 * resource.  For now will leave with skipping forward instead of random access.
 */
public class CEDICTCharacterDictionary implements CharacterDictionary {
	private static String TAG = Xhandwriting.class.getName();
	
	// we ignore any characters that are below or above the below thresholds
	static private final char LOW_CHAR = '\u4e00';
	static private final char HIGH_CHAR = '\u9fff';
	
	// the source of the InputStream that we read from.
	private CEDICTStreamProvider streamProvider;
	
	// the indexed character positions.
	// first dimension is char, with each char located at [char] - LOW_CHAR
	// second dimension is an int[] of the character positions in the stream
	// where the dictionary data for that character can be found
	private int[][] characterIndices;

	// the number of character entries storied in this dictionary
	private int characterCount = 0;
	
	
	public CEDICTCharacterDictionary(CEDICTStreamProvider streamProvider) throws IOException {
		this(streamProvider, null);
	}
	
	/**
	 * Instantiate a new CEDICTCharacterDictionary that reads from the stream
	 * provided by the given CEDICTStreamProvider.
	 * 
	 * @param streamProvider source of the CEDICT data stream
	 * @throws IOException
	 */
	public CEDICTCharacterDictionary(CEDICTStreamProvider streamProvider, EventListener changeListener) throws IOException {
		this.streamProvider = streamProvider;
		this.indexCharacters(changeListener);
	}
	
	public int getSize() {
		return this.characterCount;
	}
	
	/**
	 * Look up the data for a particular character.
	 * @param character the character whose data we want to look up
	 */
	public Entry lookup(char character) {
		int charIndex = character - LOW_CHAR;
		if(charIndex >= 0 && charIndex < this.characterIndices.length) {
			// ensure that the char is in the proper range, if so find the positions
			
			int[] positions = this.characterIndices[character - LOW_CHAR];
			
			if(null != positions) {
				try {
					Reader reader = this.getCEDICTReader();
					
					int readerPosition = 0;
					int read = 0;
					
					char tradChar = 0;
					char simpChar = 0;
					
					// We compile a list of each of the definitions at
					// the indicated positions.
					List definitionList = new ArrayList();
					
					for(int i = 0; i < positions.length; i++) {
						// for each definition, skip ahead in the character stream to its position
						for(; readerPosition < positions[i]; readerPosition += reader.skip(positions[i] - readerPosition));
						
						tradChar = (char)reader.read();
						reader.read();	// the space in between
						simpChar = (char)reader.read();
						readerPosition += 3;
						
						// advance to the pinyin past the opening '['
						for(; read > -1 && '[' != (char)read; readerPosition++) {
							read = reader.read();
						}
						
						// collect everything up to the closing ']' as pinyin
						StringBuffer pinyin = new StringBuffer();
						read = reader.read();
						readerPosition++;
						for(; read > -1 && ']' != (char)read; readerPosition++) {
							pinyin.append((char)read);
							read = reader.read();
						}
						
						// advance to the start of first definition past the first opening '/'
						for(; read > -1 && '/' != (char)read; readerPosition++) {
							read = reader.read();
						}
						
						// read in definitions until the end of the line
						List translationList = new ArrayList();
						read = reader.read();
						readerPosition++;
						for(; read > -1 && read != '\n' && read != '\r'; readerPosition++) {
							// keep reading each definition until the end of the line
							
							StringBuffer definition = new StringBuffer();
							for(; read > -1 && '/' != (char)read; readerPosition++) {
								// keep reading to the end of this definition, delimited by a /
								
								definition.append((char)read);
								read = reader.read();
							}
							
							translationList.add(definition.toString());
							read = reader.read();
						}
						
						String[] translations = new String[translationList.size()];
						translations = (String[])translationList.toArray(translations);
						
						definitionList.add(new CEDICTEntry.CEDICTDefinition(pinyin.toString(), translations));
					}
					
					Definition[] definitions = new Definition[definitionList.size()];
					definitions = (Definition[])definitionList.toArray(definitions);
					
					return new CEDICTEntry(tradChar, simpChar, definitions);
					
				} catch(IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
		
		// character out of range or Exception...
		return null;
	}
	
	/**
	 * Index the positions of each of the definitions for each character.
	 * Indexing is done in two scans.
	 * The first scan counts the number of definitions for each character.
	 * We use the counts to dimension an array for each character of positions.
	 * The second scan writes the positions.
	 * 
	 * @throws IOException
	 */
	private void indexCharacters(final EventListener changeListener) throws IOException {
		// first scan loads the definition count of each character
		// into the characterCounts array
		final int[] characterCounts = new int[HIGH_CHAR - LOW_CHAR];
		
		final EventObject ce = new EventObject(this);
		this.scanPositions(new ScanAction() {
			public void indexPosition(char character, int position) {
				if(character >= LOW_CHAR && character <= HIGH_CHAR) {
					characterCounts[character - LOW_CHAR]++;
					
					CEDICTCharacterDictionary.this.characterCount++;
					if(null != changeListener) {
						//changeListener.stateChanged(ce);
						Log.i(TAG, "update XXX");
					}
				}
			}
		});
		
		// now that we know how many definitions there are for each character
		// we go back through and dimension an array to store the positions
		final int[][] characterIndices = new int[characterCounts.length][];
		for(int i = 0; i < characterIndices.length; i++) {
			if(characterCounts[i] > 0) {
				characterIndices[i] = new int[characterCounts[i]];
			}	
		}
		
		// second pass loads the positions into each character's position array.
		this.scanPositions(new ScanAction() {
			public void indexPosition(char character, int position) {
				// find the first unitialized index in the position array and fill it with the given position
				if(character >= LOW_CHAR && character <= HIGH_CHAR) {
					for(int i = 0; i < characterIndices[character - LOW_CHAR].length; i++) {
						if(characterIndices[character - LOW_CHAR][i] == 0) {
							characterIndices[character - LOW_CHAR][i] = position;
							break;
						}
					}
				}
			}
		});
		
		this.characterIndices = characterIndices;
	}
	
	private Reader getCEDICTReader() throws IOException {
		return new BufferedReader(new InputStreamReader(this.streamProvider.getCEDICTStream(), "UTF-8"));
	}
	
	/**
	 * The two scans use the same scanning code, but perform
	 * a different operation when the definition is discovered.
	 * Use this interface to unify the two scanning operations
	 * rather than a copy and paste.
	 */
	static private interface ScanAction {
		public void indexPosition(char character, int position);
	}
	
	/**
	 *  Scan through the CEDICT character stream and perform
	 *  the ScanAction when a new character is detected.
	 */
	private void scanPositions(ScanAction action) throws IOException {
		Reader reader = this.getCEDICTReader();
		
		int read;
		boolean newLine = true;
		
		for(int readerPosition = 0; (read = reader.read()) > -1; readerPosition++) {	
			char readChar = (char)read;
			
			if('\n' == readChar || '\r' == readChar) {
				// new entries begin after line breaks, so mark the end of line
				// while iterating over the loop
				newLine = true;
		
			} else if(newLine){
				// we're at the beginning of a line, could be an entry
				
				newLine = false;
				
				if('#' != readChar) {
					// ignore comment lines that begin with #
					
					try {
						char tradChar = readChar;
						
						read = reader.read();
						readChar = (char)read;
						
						read = reader.read();
						if(' ' == readChar && read > -1) {
							// if the second character on a line is a space,
							// then this is an entry for a single character
							// (rather than for a multiple character word if it
							// was another non-space char.
							// we're only interested in single character defs.
							
							// index this line as the position for the traditional character.
							action.indexPosition(tradChar, readerPosition);

							char simpChar = (char)read;
							if(tradChar != simpChar) {
								// if the simplified form is different, then mark this as a position
								// for the simplified char.
								
								action.indexPosition(simpChar, readerPosition);
							}
						}
						
						readerPosition += 2;	
						
					} catch(ArrayIndexOutOfBoundsException e) {
						// Character must have fallen out of the defined range of lowChar to highChar.
						// We just ignore the character.
						e.printStackTrace();
					}
				}
			}
		}
		
		reader.close();
		
		return;
	}
	
	/**
	 * An abstraction of a source for a CEDICT InputStream.
	 * Will work with any InputStream, but is intended for use
	 * with a resource, otherwise it would probably be more efficient
	 * to rewrite this to take advantage of random access. 
	 */
	static public interface CEDICTStreamProvider {
		/**
		 * The provider needs to serve up a new InputStream positioned
		 * at the start of the stream each time this method is invoked.
		 * It should NOT return the same instance with each call.
		 * 
		 * @return InputStream positioned at the start of a CEDICT resource/file
		 */
		public InputStream getCEDICTStream() throws IOException;
	}
	
	// CharacterDictionary entry implementations.

	static private class CEDICTEntry implements Entry {
		
		private char traditional;
		private char simplified;
		
		private Definition[] definitions;
		
		public CEDICTEntry(char traditional, char simplified, Definition[] definitions) {
			this.traditional = traditional;
			this.simplified = simplified;
			
			this.definitions = definitions;
		}
		
		public char getTraditional() {
			return this.traditional;
		}
		
		public char getSimplified() {
			return this.simplified;
		}
		
		public Definition[] getDefinitions() {
			return this.definitions;
		}
		
		public String toString() {
			StringBuffer sbuf = new StringBuffer();
			
			for(int i = 0;;) {
				sbuf.append(this.traditional).append(' ').append(this.simplified);
				sbuf.append(this.definitions[i].toString());
			
				if(++i < this.definitions.length) {
					sbuf.append('\n');
				} else {
					break;
				}
			}
			
			return sbuf.toString();
		}
		
		static private class CEDICTDefinition implements Definition {
			private String pinyin;
			private String[] translations;
			
			public CEDICTDefinition(String pinyin, String[] translations) {
				this.pinyin = pinyin;
				this.translations = translations;
			}
			
			public String getPinyin() {
				return this.pinyin;
			}
			
			public String[] getTranslations() {
				return this.translations;
			}
			
			public String toString() {
				StringBuffer sbuf = new StringBuffer();
				sbuf.append('[').append(this.pinyin).append(']').append(' ').append('/');
				
				for(int j = 0; j < this.translations.length; j++) {
					sbuf.append(this.translations[j]).append('/');
				}
				
				return sbuf.toString();
			}
		}
	}
}
