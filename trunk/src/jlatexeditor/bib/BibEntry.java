package jlatexeditor.bib;

public class BibEntry {
  private String name;
  private String description;
  private String[] required;
  private String[] optional;

  public BibEntry(String name, String description, String[] required, String[] optional) {
    this.name = name;
    this.description = description;
    this.required = required;
    this.optional = optional;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String[] getRequired() {
    return required;
  }

  public String[] getOptional() {
    return optional;
  }

  public static BibEntry ENTRIES[] = new BibEntry[] {
          new BibEntry(
                  "article",
                  "An article from a journal or magazine.",
                  new String[] {"author", "title", "journal", "year"},
                  new String[] {"volume", "number", "pages", "month", "note", "key"}
          ),
          new BibEntry(
                  "book",
                  "A book with an explicit publisher.",
                  new String[] {"author/editor", "title", "publisher", "year"},
                  new String[] {"volume", "series", "address", "edition", "month", "note", "key"}
          ),
          new BibEntry(
                  "booklet",
                  "A work that is printed and bound, but without a named publisher or sponsoring institution.",
                  new String[] {"title"},
                  new String[] {"author", "howpublished", "address", "month", "year", "note", "key"}
          ),
          new BibEntry(
                  "conference",
                  "The same as inproceedings, included for Scribe compatibility.",
                  new String[] {"author", "title", "booktitle", "year"},
                  new String[] {"editor", "pages", "organization", "publisher", "address", "month", "note", "key"}
          ),
          new BibEntry(
                  "inbook",
                  "A part of a book, usually untitled. May be a chapter (or section or whatever) and/or a range of pages.",
                  new String[] {"author/editor", "title", "chapter/pages", "publisher", "year"},
                  new String[] {"volume", "series", "address", "edition", "month", "note", "key"}
          ),
          new BibEntry(
                  "incollection",
                  "A part of a book having its own title.",
                  new String[] {"author", "title", "booktitle", "year"},
                  new String[] {"editor", "pages", "organization", "publisher", "address", "month", "note", "key"}
          ),
          new BibEntry(
                  "inproceedings",
                  "An article in a conference proceedings.",
                  new String[] {"author", "title", "booktitle", "year"},
                  new String[] {"editor", "series", "pages", "organization", "publisher", "address", "month", "note", "key"}
          ),
          new BibEntry(
                  "manual",
                  "Technical documentation.",
                  new String[] {"title"},
                  new String[] {"author", "organization", "address", "edition", "month", "year", "note", "key"}
          ),
          new BibEntry(
                  "mastersthesis",
                  "A Master's thesis.",
                  new String[] {"author", "title", "school", "year"},
                  new String[] {"address", "month", "note", "key"}
          ),
          new BibEntry(
                  "misc",
                  "For use when nothing else fits.",
                  new String[] {"none"},
                  new String[] {"author", "title", "howpublished", "month", "year", "note", "key"}
          ),
          new BibEntry(
                  "phdthesis",
                  "A Ph.D. thesis.",
                  new String[] {"author", "title", "school", "year"},
                  new String[] {"address", "month", "note", "key"}
          ),
          new BibEntry(
                  "proceedings",
                  "The proceedings of a conference.",
                  new String[] {"title", "year"},
                  new String[] {"editor", "publisher", "organization", "address", "month", "note", "key"}
          ),
          new BibEntry(
                  "techreport",
                  "A report published by a school or other institution, usually numbered within a series.",
                  new String[] {"author", "title", "institution", "year"},
                  new String[] {"type", "number", "address", "month", "note", "key"}
          ),
          new BibEntry(
                  "unpublished",
                  "A document having an author and title, but not formally published.",
                  new String[] {"author", "title", "note"},
                  new String[] {"month", "year", "key"}
          )
  };
}
