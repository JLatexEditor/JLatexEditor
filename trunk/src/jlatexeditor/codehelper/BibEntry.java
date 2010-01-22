package jlatexeditor.codehelper;

/**
* BibEntry.
*/
public class BibEntry {
  private String name = "";
  private String title = "";
  private String authors = "";
  private String year = "";

  private String block = "";

  public BibEntry() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getBlock() {
    return block;
  }

  public void setBlock(String block) {
    this.block = block;
  }

  public String toString() {
    return name + ": " + title + "</br>\n" + authors + ", " + year; 
  }
}
