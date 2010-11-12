/** XMLParser
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package my.XML;

import java.util.Vector;

public class XMLParser {
  public XMLDocument parse(String xmlstring)
          throws XMLException {
    /* alle Zeilenumbrüche durch Leerzeichen ersetzen */
    xmlstring = xmlstring.replace('\n', ' ');
    xmlstring = xmlstring.replace('\r', ' ');

    /* alle Kommentare filtern */
    int comment_start, comment_end;
    while ((comment_start = xmlstring.indexOf("<!--")) != -1) {
      comment_end = xmlstring.indexOf("-->", comment_start);

      if (comment_end == -1) {
        /* Kommentar bis zum Ende der Datei */
        xmlstring = xmlstring.substring(0, comment_start);
        break;
      } else {
        /* den Kommentar herausschneiden */
        xmlstring = xmlstring.substring(0, comment_start) + xmlstring.substring(comment_end + 3);
      }
    }

    /* Doctype Definition entfernen */
    int doctype_start = xmlstring.indexOf("<!DOCTYPE");
    if (doctype_start != -1) {
      int bracket_open = xmlstring.indexOf('[', doctype_start);
      int bracket_close = xmlstring.indexOf(']', doctype_start);

      int doctype_end = doctype_start;
      if (bracket_open != -1 && bracket_close != -1 && bracket_open < xmlstring.indexOf('>', doctype_start)) {
        doctype_end = bracket_close;
      }
      doctype_end = xmlstring.indexOf('>', doctype_end);

      xmlstring = xmlstring.substring(0, doctype_start) + xmlstring.substring(doctype_end + 1);
    }

    /* das Dokument nach XML-Elementen parsen */
    XMLElement raw_document = new XMLElement();
    parseXMLString(raw_document, xmlstring);

    XMLDocument document = new XMLDocument();
    /* enthält der String eine XML-Type-Definition? */
    Vector childs = raw_document.getChildElements();
    for (int child_nr = 0; child_nr < childs.size(); child_nr++) {
      XMLElement element = (XMLElement) childs.elementAt(child_nr);
      String name = element.getName();

      if (name.startsWith("?")) {
        /* ist dies eine XML-Typ Definition? */
        if (name.equals("?xml")) {
          /* Version und Encoding Informationen auslesen */
          if (element.getAttribute("version") != null) {
            document.setXMLVerion(element.getAttribute("version"));
          }
          if (element.getAttribute("encoding") != null) {
            document.setXMLEncoding(element.getAttribute("encoding"));
          }
        }
      } else {
        /* gibt es noch mehr Elemente, das wäre ein Fehler */
        if (child_nr < childs.size() - 1) throw new XMLException("more than one root element");
        /* dies ist das Root-Element des Dokuments */
        document.setRootElement(element);
      }
    }

    return document;
  }


  private String parseXMLString(XMLElement parent, String xmlstring)
          throws XMLException {
    int search_start;
    int tag_start;
    int tag_ende = -1;
    XMLElement aParent;
    Vector theParents = new Vector();

    aParent = parent;
    theParents.addElement(aParent);
    while (++tag_ende < xmlstring.length()) {

      /* String von links trimmen */
      search_start = tag_ende;
      while (xmlstring.charAt(search_start) <= '\u0020') {
        if (++search_start >= xmlstring.length()) return "";
      }

      /* das nächste Tag im String suchen */
      tag_start = xmlstring.indexOf("<", search_start);

      // Text, der auch < und > enthalten darf
      if (xmlstring.startsWith("|--", search_start)) {
        int end_text = xmlstring.indexOf("--|", search_start);
        if (end_text != -1) tag_start = xmlstring.indexOf("<", end_text);
      }

      tag_ende = xmlstring.indexOf(">", tag_start);
      if (tag_start == -1 || tag_start > tag_ende) {
        throw new XMLException("falsche Syntax in XMLDaten");
      }

      String start_string = xmlstring.substring(search_start, tag_start).trim();
      String tag_string = xmlstring.substring(tag_start + 1, tag_ende).trim();

      /* ist dies ein leeres Tag (schließt sich selbst)? */
      boolean isEmptyTag = false;
      if (tag_string.endsWith("/") || tag_string.endsWith("?")) {
        isEmptyTag = true;
        tag_string = tag_string.substring(0, tag_string.length() - 1);
      }

      /* aus dem Tag-String ein XML-Element erstellen */
      XMLElement element = parseTagString(tag_string);

      /* ist dies ein öffnendes oder schließendes Tag? */
      if (element.getName().startsWith("/")) {
        /* es darf kein Tag kommen, wenn auch reiner Text enthalten ist */
        if (!(start_string.equals(""))) {
          if (aParent.getChildElements().size() != 0) {
            throw new XMLException("element must not contain text: " + start_string);
          } else {
            /* das Element enthält nur Text */
            start_string = start_string.replaceFirst("\\|--", "");
            start_string = start_string.replaceFirst("--\\|", "");
            aParent.setText(start_string);
          }
        }

        /* schließendes Tag: TagName muss mit dem parent-Tag übereinstimmen */
        if (!aParent.getName().equalsIgnoreCase(element.getName().substring(1).trim())) {
          throw new XMLException("wrong closing tag: " + element.getName());
        }

        /* der letzte parent ist abgearbeitet, eine Ebene zurück gehen */
        if (theParents.size() > 1) {
          theParents.removeElementAt(theParents.size() - 1);
          aParent = (XMLElement) theParents.elementAt(theParents.size() - 1);
        }
        continue;

      } else {
        /* es darf kein Tag kommen, wenn auch reiner Text enthalten ist */
        if (!(start_string.equals(""))) {
          throw new XMLException("element must not contain text: " + start_string);
        }

        /* das Element als ChildElement hinzufügen */
        aParent.addChildElement(element);
      }

      /* innerhalb des XML-Elements nach Child-Elementen suchen */
      if (!isEmptyTag) {
        aParent = element;
        theParents.addElement(aParent);
      }

    }

    return "";
  }


  private XMLElement parseTagString(String tag_string)
          throws XMLException {
    /* Leerzeichen am Anfang und Ende entfernen */
    tag_string = tag_string.trim();

    /* den Tagnamen ermittlen */
    int index = tag_string.indexOf(" ");
    if (index == -1) return new XMLElement(tag_string);

    /* das XML-Element erstellen */
    XMLElement element = new XMLElement(tag_string.substring(0, index));

    /* die Attribute parsen */
    tag_string = tag_string.substring(index);
    while (!(tag_string = tag_string.trim()).equals("")) {
      int number = 0;
      index = tag_string.indexOf("=");
      /* falls es kein = gibt, dann muss der String leer sein */
      if (index == -1) throw new XMLException("wrong attribute definition");

      /* der Attributname steht vor dem = */
      String attribute_name = tag_string.substring(0, index).trim();
      tag_string = tag_string.substring(index + 1).trim();

      /* der Attributwert steht nach dem = */
      int index_start_dquote = tag_string.indexOf("\"");
      if (index_start_dquote == -1) index_start_dquote = Integer.MAX_VALUE;
      int index_start_quote = tag_string.indexOf("\'");
      if (index_start_quote == -1) index_start_quote = Integer.MAX_VALUE;

      int index_start = Math.min(index_start_dquote, index_start_quote);
      if (index_start == Integer.MAX_VALUE) {
        throw new XMLException("attribute value must be surrounded by quotes: " + attribute_name);
      }
      int index_ende = tag_string.indexOf(tag_string.charAt(index_start), index_start + 1);

      /* das letzte Ausführungszeichen vor dem nächsten Tag als schließendes */
      int index_nexttag = tag_string.indexOf("=", index_ende);
      if (index_nexttag == -1) index_nexttag = Integer.MAX_VALUE;

      index_ende = tag_string.lastIndexOf(tag_string.charAt(index_start), index_nexttag);
      if (index_ende <= index_start) {
        throw new XMLException("attribute value must be surrounded by quotes: " + attribute_name);
      }
      if (!(tag_string.substring(0, index_start).trim().equals(""))) {
        throw new XMLException("attribute value must be surrounded by quotes: " + attribute_name);
      }

      String attribute_value = tag_string.substring(index_start + 1, index_ende);
      /* mit dem restlichen String weitermachen */
      tag_string = tag_string.substring(index_ende + 1);

      /* das Attribut/Wert Paar dem Element hinzufügen */
      element.setAttribute(attribute_name, attribute_value);
      if (element.getName().equals(new String("queryresults"))) {
        element.setAttributeOrdered(attribute_name);
      }
    }

    return element;
  }
}
