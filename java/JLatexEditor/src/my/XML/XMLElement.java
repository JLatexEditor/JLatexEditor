/** XMLElement
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package my.XML;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

public class XMLElement{
  /* Name des XML-Elements */
  private String name = "";
  /* die Attribute des Elements */
  private Hashtable<String, String> attributes = new Hashtable<String, String>();
  private Vector<String> attributesOrdered = new Vector<String>();
  private Hashtable<String, String> attributesLowerCase = new Hashtable<String, String>();
  /* die KindElemete des XML-Elements*/
  private Vector<XMLElement> childs = new Vector<XMLElement>();
  /* falls das Element reinen Text enthält (keine KindElemente) */
  private String text = "";

  /* Konstruktor ohne Elementnamen */
  public XMLElement(){
  }

  /* Konstruktor mit dem Namen des XML-Elements */
  public XMLElement(String name){
    setName(name);
  }

  /* Auslesen der Eigenschaften des XML-Elements */
  public String getAttribute(String key){
    String caseKey = attributesLowerCase.get(key.toLowerCase());
    if(caseKey != null) return attributes.get(caseKey);
    return null;
  }

  public XMLElement getChildElement(String names){
    /* die Namen der Child-Elemente durch Punkt getrennt */
    StringTokenizer tokens = new StringTokenizer(names, ".");

    XMLElement element = this;
    while(tokens.hasMoreElements()){
      String name = tokens.nextToken();
      Vector<XMLElement> element_childs = element.getChildElements();

      /* alle Child-Elemente durchsuchen, nach dem Namen */
      boolean found = false;
      for(int child_nr = 0; child_nr < element_childs.size(); child_nr++){
        XMLElement child = element_childs.elementAt(child_nr);

        if((child.getName()).equals(name)){
          element = child;
          found = true;
          break;
        }
      }
      /* falls das Child Element nicht gefunden wurde */
      if(!found) return null;
    }
    /* das Element wurde gefunden */
    return element;
  }

  public Vector<XMLElement> getChildElements(){
    return (Vector<XMLElement>) childs.clone();
  }

  public Enumeration<String> getAttributeNames(){
    return attributes.keys();
  }

  public String getName(){
    return name.toLowerCase();
  }

  public String getText(){
    return text;
  }

  /* Setzen der Eigenschaften des XML-Elements */
  public void addChildElement(XMLElement element){
    childs.addElement(element);
  }

  public void removeAttribute(String key){
    String caseKey = attributesLowerCase.get(key.toLowerCase());
    if(caseKey != null){
      attributes.remove(caseKey);
      attributesLowerCase.remove(key.toLowerCase());
    }
  }

  public void setAttribute(String key, String value){
    attributes.put(key, value);
    attributesLowerCase.put(key.toLowerCase(), key);
  }

  public void setAttributeOrdered(String key){
    attributesOrdered.addElement(key);
  }

  public String getAttributeOrdered(int nr){
    return attributesOrdered.elementAt(nr);
  }

  public void setName(String name){
    this.name = name;
  }

  public void setText(String text){
    this.text = text;
  }

  public String toString(){
    String xmlString = "";

    /* das XML-Tag öffnen */
    xmlString = "<" + name;
    /* die Attribute hinzufügen */
    for(Enumeration<String> e = attributes.keys(); e.hasMoreElements();){
      String key = e.nextElement();
      xmlString += " " + key + "=\"" + attributes.get(key) + "\"";
    }
    xmlString += ">";

    /* den Körper des Elements (child elements) */
    for(int child_nr = 0; child_nr < childs.size(); child_nr++){
      xmlString += (childs.elementAt(child_nr)).toString();
    }
    /* falls das Element reinen Text enthält */
    xmlString += text;

    /* das XML-Element wieder schließen */
    xmlString += "</" + name + ">";

    return xmlString;
  }
}