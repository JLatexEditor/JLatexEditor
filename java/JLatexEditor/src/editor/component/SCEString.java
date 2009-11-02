
/**
 * @author Jörg Endrullis
 */

package editor.component;

public class SCEString{
  private SCEDocumentChar characters[] = null;

  public SCEString(SCEDocumentChar characters[]){
    this.characters = characters;
  }

  public SCEString(SCEDocumentChar characters[], int offset, int length){
    this.characters = new SCEDocumentChar[length];
    System.arraycopy(characters, offset, this.characters, 0, length);
  }

  public SCEDocumentChar[] getCharacters(){
    return characters;
  }

  public int length(){
    return characters.length;
  }

  public SCEDocumentChar charAt(int i){
    return characters[i];
  }

  public String toString(){
    char buffer[] = new char[characters.length];
    for(int char_nr = 0; char_nr < characters.length; char_nr++){
      buffer[char_nr] = characters[char_nr].character;
    }
    return new String(buffer);
  }
}
