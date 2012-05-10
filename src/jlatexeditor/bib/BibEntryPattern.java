package jlatexeditor.bib;

import sce.codehelper.CHCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class BibEntryPattern extends CHCommand {
  private String[] required;
  private String[] optional;
  private HashSet<String> all;

  public BibEntryPattern(String name, String description, String[] required, String[] optional) {
    super("@" + name);

    setHint(description);
    this.required = required;
    this.optional = optional;

    all = new HashSet<String>();
    List<String> keys = new ArrayList<String>();
    keys.addAll(Arrays.asList(required));
    keys.addAll(Arrays.asList(optional));
    for(String key : keys) {
      String splits[] = key.split("/");
      all.addAll(Arrays.asList(splits));
    }

    String usage = "&at;" + name + "{@|@,\n";
    for(String key : getRequired()) usage += "  " + key + " = {},\n";
    usage += "}\n";
    setUsage(usage);
  }

  public String[] getRequired() {
    return required;
  }

  public String[] getOptional() {
    return optional;
  }

  public HashSet getAll() {
    return all;
  }

  public static BibEntryPattern getEntry(String type) {
    type = type.toLowerCase();
    for(BibEntryPattern entry : ENTRIES) {
      if(entry.getName().equals(type)) return entry;
    }
    return null;
  }
  
  public static BibEntryPattern ENTRIES[] = new BibEntryPattern[] {
          new BibEntryPattern(
                  "article",
                  "An article from a journal or magazine.",
                  new String[] {"author", "title", "journal", "year"},
                  new String[] {"volume", "number", "pages", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "book",
                  "A book with an explicit publisher.",
                  new String[] {"author/editor", "title", "publisher", "year"},
                  new String[] {"volume", "series", "address", "edition", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "booklet",
                  "A work that is printed and bound, but without a named publisher or sponsoring institution.",
                  new String[] {"title"},
                  new String[] {"author", "howpublished", "address", "month", "year", "note", "key"}
          ),
          new BibEntryPattern(
                  "conference",
                  "The same as inproceedings, included for Scribe compatibility.",
                  new String[] {"author", "title", "booktitle", "year"},
                  new String[] {"editor", "pages", "organization", "publisher", "address", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "inbook",
                  "A part of a book, usually untitled. May be a chapter (or section or whatever) and/or a range of pages.",
                  new String[] {"author/editor", "title", "chapter/pages", "publisher", "year"},
                  new String[] {"volume", "series", "address", "edition", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "incollection",
                  "A part of a book having its own title.",
                  new String[] {"author", "title", "booktitle", "year"},
                  new String[] {"editor", "pages", "organization", "publisher", "address", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "inproceedings",
                  "An article in a conference proceedings.",
                  new String[] {"author", "title", "booktitle", "year"},
                  new String[] {"editor", "series", "pages", "organization", "publisher", "address", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "manual",
                  "Technical documentation.",
                  new String[] {"title"},
                  new String[] {"author", "organization", "address", "edition", "month", "year", "note", "key"}
          ),
          new BibEntryPattern(
                  "mastersthesis",
                  "A Master's thesis.",
                  new String[] {"author", "title", "school", "year"},
                  new String[] {"address", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "misc",
                  "For use when nothing else fits.",
                  new String[] {"none"},
                  new String[] {"author", "title", "howpublished", "month", "year", "note", "key"}
          ),
          new BibEntryPattern(
                  "phdthesis",
                  "A Ph.D. thesis.",
                  new String[] {"author", "title", "school", "year"},
                  new String[] {"address", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "proceedings",
                  "The proceedings of a conference.",
                  new String[] {"title", "year"},
                  new String[] {"editor", "publisher", "organization", "address", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "techreport",
                  "A report published by a school or other institution, usually numbered within a series.",
                  new String[] {"author", "title", "institution", "year"},
                  new String[] {"type", "number", "address", "month", "note", "key"}
          ),
          new BibEntryPattern(
                  "unpublished",
                  "A document having an author and title, but not formally published.",
                  new String[] {"author", "title", "note"},
                  new String[] {"month", "year", "key"}
          )
  };
}
