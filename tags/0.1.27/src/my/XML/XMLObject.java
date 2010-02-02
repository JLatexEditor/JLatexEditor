
package my.XML;

/**
 * Interface für Klassen, die ihre Daten in einem XMLElement zurück liefern können.
 */
public interface XMLObject {
  /**
   * speichert die Daten in einem XMLElement und liefert dieses zurück.
   *
   * @return das XMLElement
   */
  public XMLElement toXMLElement();
}
