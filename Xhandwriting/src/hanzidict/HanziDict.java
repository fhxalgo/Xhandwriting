package hanzidict;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EventListener;
import java.util.MissingResourceException;

import android.content.res.AssetManager;

import hanzidict.CEDICTCharacterDictionary.CEDICTStreamProvider;
import hanzidict.CharacterDictionary.Entry.Definition;


public class HanziDict {

	public AssetManager am;
	final private String DEFAULT_RESOURCE_DICTIONARY_PATH = "cedict_ts.u8";
	//static private final String STROKE_DATA	= "/strokes.dat";
	//static private final String STROKE_DATA	= "/strokes-extended.dat";

	private CharacterDictionary dictionary;
	
//	private String resourceDictionaryPath = DEFAULT_RESOURCE_DICTIONARY_PATH;
//	private String fileDictionaryPath = "";
	private boolean usingResourceDictionary = true;

	public HanziDict(AssetManager am) {
		this.am = am;
	}
	
	public void init() {
		try {
			this.loadFileDictionary(DEFAULT_RESOURCE_DICTIONARY_PATH, null);
		} catch (IOException ioe) {

		}
	}
	
    /**
     * Load dictionary data from the given resource path.
     * The given ChangeListener (if non-null) will have stateChanged fired with
     * every new character loaded with a source of the CharacterDictionary.
     * The listener can then check the dictionary's current size.
     * This is useful for progress bars when loading.
     * 
     * @param resourcePath
     * @param progressListener
     * @throws IOException
     */
//    private void loadResourceDictionary(String resourcePath, EventListener progressListener) throws IOException {
//    	final URL resourceURL = this.getClass().getResource(resourcePath);
//		if(null == resourceURL) {
//			throw new MissingResourceException("Can't find resource: " + resourcePath, this.getClass().getName(), resourcePath);
//		} else {
//			this.loadDictionary(new CEDICTStreamProvider() {
//				public InputStream getCEDICTStream() throws IOException {
//					return resourceURL.openStream();
//				}
//			}, progressListener);
//			
//			HanziDict.this.usingResourceDictionary = true;
//		}
//    }
    
    
    /**
     * Load the dictionary data from the given file on disk.
     * @param filePath
     * @param progressListener
     * @throws IOException
     */
    private void loadFileDictionary(final String filePath, EventListener progressListener) throws IOException {
    	//final File file = new File(filePath);
		//if(!file.canRead()) {
		//	throw new IOException("Can't read from the specified file: " + filePath);
		//} else {
			this.loadDictionary(new CEDICTStreamProvider() {
				public InputStream getCEDICTStream() throws IOException {
					//return new FileInputStream(file);
					return am.open(filePath);
				}
			}, progressListener);
			
			//HanziDict.this.usingResourceDictionary = false;
		//}
    }
    
    private void loadDictionary(CEDICTStreamProvider streamProvider, EventListener progressListener) throws IOException {
    	this.dictionary = new CEDICTCharacterDictionary(streamProvider, progressListener);
    }
    
    
//	public void characterSelected(CharacterSelectionEvent e) {
//		char selectedChar = e.getSelectedCharacter();
//	    CharacterDictionary.Entry entry = this.dictionary.lookup(selectedChar);
//	    
//	    if(null != entry) {
//	        this.loadDefinitionData(selectedChar, entry);
//	    } else {
//	        this.loadEmptyDefinition(selectedChar);
//	    }        
//	}
    
    /**
     * Load the given dictionary entry data.
     * @param selectedChar the character
     * @param dictEntry the definition data
     */
    private void loadDefinitionData(char selectedChar, CharacterDictionary.Entry dictEntry) {
		char tradChar = dictEntry.getTraditional();
		char simpChar = dictEntry.getSimplified();
		
		char primaryChar;
		char secondaryChar;
		
		if(selectedChar == tradChar) {
			primaryChar = tradChar;
			secondaryChar = simpChar;
		} else {
			primaryChar = simpChar;
			secondaryChar = tradChar;
		}
    	
		StringBuffer paneText = new StringBuffer();
		
		paneText.append("<html>\n");
		paneText.append("\t<head>\n");
		paneText.append("\t\t<style type=\"text/css\">\n");
		
//		Font font = this.getFont();
//		if(null != font) {
//			paneText.append("\t\tbody {font-family: ").append(this.getFont().getFamily()).append("; font-size: ").append(this.getFont().getSize()).append("}\n");
//		}
		
		paneText.append("\t\t.characters {font-size: 150%}\n");
		paneText.append("\t\t</style>\n");
    	paneText.append("\t</head>\n");
    	paneText.append("\t<body>\n");
		
		paneText.append("<h1 class=\"characters\">").append(primaryChar);
		if(secondaryChar != primaryChar) {
			paneText.append("(").append(secondaryChar).append(")");
		}
		paneText.append("</h1>\n");
		paneText.append("<br>\n\n");
		
		Definition[] defs = dictEntry.getDefinitions();
		
		// display the data in an html list
		paneText.append("<ol>\n");
		// cycle through pronunciations
		for(int i = 0; i < defs.length; i++) {
			String pinyinString = Pinyinifier.pinyinify(defs[i].getPinyin());
			
			/* canDisplayUpTo not reliable, apparently
			if(this.getFont().canDisplayUpTo(pinyinString) > -1) {
				// preferably show pinyin tones with accented chars,
				// but if the font can't do that, then revert to tone digits appended.
				pinyinString = defs[i].getPinyin();
			}
			*/
			
			paneText.append("<li><b>").append(pinyinString).append("</b><br>\n");
			
			String[] translations = defs[i].getTranslations();
			
			// cycle through the definitions for this pronunciation
			for(int j = 0; j < translations.length; j++) {
				paneText.append(translations[j]);
				if(j < translations.length - 1) {
					paneText.append("; ");
				}
			}
			paneText.append("\n");
		}
		
		paneText.append("</ol>\n");
		paneText.append("\t</body>\n");
		paneText.append("</html>");
	
//		this.definitionTextPane.setText(paneText.toString());	
//		
//		// make sure scroll centered at the top
//		this.definitionTextPane.setCaretPosition(0);
		
		
    }
 
	private void loadEmptyDefinition(char character) {
		//this.definitionTextPane.setText("<html>\n<body>\nNo definition found.\n</body>\n</html>");
		
	}
	
    public String getDefinitionData(char selectedChar) {
    	CharacterDictionary.Entry dictEntry = this.dictionary.lookup(selectedChar);
    	
	    if(null == dictEntry) {
	    	return "No definition found.";
    	} 

		char tradChar = dictEntry.getTraditional();
		char simpChar = dictEntry.getSimplified();
		
		char primaryChar;
		char secondaryChar;
		
		if(selectedChar == tradChar) {
			primaryChar = tradChar;
			secondaryChar = simpChar;
		} else {
			primaryChar = simpChar;
			secondaryChar = tradChar;
		}
    	
		StringBuffer paneText = new StringBuffer();
				
		paneText.append(primaryChar);
		paneText.append(":");
		if(secondaryChar != primaryChar) {
			paneText.append("(").append(secondaryChar).append(")");
		}
		
		Definition[] defs = dictEntry.getDefinitions();
		
		// display the data in an html list
		// cycle through pronunciations
		for(int i = 0; i < defs.length; i++) {
			paneText.append(i+1).append(". ");
			String pinyinString = Pinyinifier.pinyinify(defs[i].getPinyin());
			
			/* canDisplayUpTo not reliable, apparently
			if(this.getFont().canDisplayUpTo(pinyinString) > -1) {
				// preferably show pinyin tones with accented chars,
				// but if the font can't do that, then revert to tone digits appended.
				pinyinString = defs[i].getPinyin();
			}
			*/
			
			paneText.append(pinyinString).append(" ");
			
			String[] translations = defs[i].getTranslations();
			
			// cycle through the definitions for this pronunciation
			for(int j = 0; j < translations.length; j++) {
				paneText.append(translations[j]);
				if(j < translations.length - 1) {
					paneText.append("; ");
				}
			}
			paneText.append("| ");
		}
				
		return paneText.toString();
	}
}
