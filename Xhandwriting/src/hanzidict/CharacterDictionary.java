package hanzidict;

/**
 * @author jkiang
 *
 * Interface for a CharacterDictionary usable by HanziDict.
 */
public interface CharacterDictionary {

	/**
	 * @param character
	 * @return the definition data for the given character
	 */
	public Entry lookup(char character);
	
	/**
	 * @return the number of entries in the dictionary
	 */
	public int getSize();
	
	/**
	 * An entry that defines the definition(s) for a particular character.
	 */
	static public interface Entry {

		/**
		 * @return the traditional form of the character this entry corresponds to
		 */
		public char getTraditional();
		
		/**
		 * @return the simplified form of the character this entry corresponds to
		 */
		public char getSimplified();
		
		/**
		 * @return the definitions for this entry
		 */
		public Definition[] getDefinitions();
		
		/**
		 * A definition for a character.
		 */
		static public interface Definition {
			
			/**
			 * @return the Pinyin
			 */
			public String getPinyin();
			
			/**
			 * @return the translations
			 */
			public String[] getTranslations();
			
		}
	}
}
