package jlatexeditor.gui;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.IOException;

public class SymbolsPanel extends JPanel {
  public SymbolsPanel() {
    setLayout(new BorderLayout());

    // TODO SHIT JAVA HAS NO STACKPANEL?
    
    read("Relation", "relation");
    read("Operators", "operators");
    read("Arrows", "arrows");
    read("Miscellaneous Math", "misc-math");
    read("Miscellaneous Text", "misc-text");
    read("Delimiter", "delimiters");
    read("Greek", "greek");
    read("Special Characters", "special");
    read("Cyrillic Characters", "cyrillic");
  }

  private void read(String title, String collection) {
    String fileName = "/images/symbols/" + collection + ".xml";

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(getClass().getResourceAsStream(fileName));
      doc.getDocumentElement().normalize();

      NodeList commands = doc.getElementsByTagName("commandDefinition");
      for(int s = 0; s < commands.getLength(); s++) {
        Node command = commands.item(s);
        NodeList children = command.getChildNodes();
        String latexCommand = null;
        for(int childNr = 0; childNr < children.getLength(); childNr++) {
          Node child = children.item(childNr);
          if(child.getNodeName().equals("latexCommand")) latexCommand = child.getTextContent();
        }
        System.out.println(latexCommand);
      }
    } catch (Exception ignored) {}
  }
}
