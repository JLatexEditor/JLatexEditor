package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;

/**
* BibEntry.
*/
public class BibEntry extends CHCommand {
  private String entryName = "";
  private String title = "";
  private String authors = "";
  private String year = "";

  private String text = "";

  public BibEntry() {
    super("");
  }

  public String getEntryName() {
    return entryName;
  }

  public void setEntryName(String entryName) {
    this.entryName = entryName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthors() {
    return authors;
  }

  public void setAuthors(String authors) {
    this.authors = authors;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getUsage() {
    return entryName + "}";
  }

  public String getName() {
    return "<html>" + entryName + "<br>" + title + "<br>\n" + authors + ", " + year + "</html>";
  }
}
