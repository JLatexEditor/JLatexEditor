/** XMLDocument
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package my.XML;

public class XMLDocument {
  /* die Standardwerte für Version und Encoding */
  private String xmlVersion = "1.0";
  private String xmlEncoding = "UTF-8";

  /* das Root-Element des Dokuments (hier ARCXML) */
  private XMLElement rootElement = null;

  /* Eigenschaften des XMLDokuments auslesen */
  public XMLElement getRootElement() {
    return rootElement;
  }

  public String getXMLEncoding() {
    return xmlEncoding;
  }

  public String getXMLVersion() {
    return xmlVersion;
  }

  /* Eigenschaften des XMLDokuments setzen */
  public void setRootElement(XMLElement element) {
    rootElement = element;
  }

  public void setXMLEncoding(String encoding) {
    xmlEncoding = encoding;
  }

  public void setXMLVerion(String version) {
    xmlVersion = version;
  }

  /* das XMLDokument in einen String wandeln (ohne Zeilenumbrüche) */
  public String toString() {
    String xmlString = "";

    /* die XML-Typ Definition einfügen */
    xmlString = "<?xml version=\"" + xmlVersion + "\" encoding=\"" + xmlEncoding + "\"?>";
    /* der Körper des XMLStrings (rekursiv durch die Elemente) */
    xmlString += rootElement.toString();

    return xmlString;
  }
}