package hanzidict;

public interface CharacterSelectionListener {
    /**
     * A character has been selected.
     * The event contains the selected character.
     * @param e the event
     */
    public void characterSelected(CharacterSelectionEvent e);
}
