package hanzidict;

import java.util.EventObject;

public class CharacterSelectionEvent extends EventObject {
    /**
	 * 
	 */
	private static final long serialVersionUID = 9006462102949195725L;
	
	private char character;
    
    private CharacterSelectionEvent(Object source, char character) {
        super(source);
        this.character = character;
    }
    
    /**
     * @return the selected character
     */
    public char getSelectedCharacter() {
        return this.character;
    }
}
