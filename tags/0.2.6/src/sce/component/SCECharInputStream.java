/**
 * @author Jörg Endrullis
 */

package sce.component;

import java.io.IOException;
import java.io.InputStream;

public interface SCECharInputStream {

  public SCEDocumentChar read() throws IOException;

  // from document
  public class FromDocument implements SCECharInputStream {
    // the associated document
    SCEDocumentRow rows[] = null;

    // the position in the document
    int rows_count = 0;
    int row_nr = 0;
    int column_nr = 0;

    // special characters
    SCEDocumentChar newLine = null;

    public FromDocument(SCEDocument document) {
      this(document.getRowsModel().getRows(), 0, 0);
    }

    public FromDocument(SCEDocumentRow rows[], int row_nr, int column_nr) {
      this.rows_count = rows.length;
      this.rows = rows;

      this.row_nr = row_nr;
      this.column_nr = column_nr;

      newLine = new SCEDocumentChar();
      newLine.character = '\n';
    }

    public SCEDocumentChar read() throws IOException {
      // read from a document
      if (row_nr >= rows_count) throw new IOException("Exception: end of document stream reached.");
      if (column_nr >= rows[row_nr].length) {
        column_nr = 0;
        row_nr++;
        return newLine;
      }
      SCEDocumentChar character = rows[row_nr].chars[column_nr];
      column_nr++;

      return character;
    }
  }

  // from input stream
  public class FromInputStream implements SCECharInputStream {
    // input stream
    InputStream in;

    public FromInputStream(InputStream in) {
      this.in = in;
    }

    public SCEDocumentChar read() throws IOException {
      // read from a input stream
      int read = in.read();
      if (read == -1) throw new IOException("Exception: end of input stream reached.");
      SCEDocumentChar character = new SCEDocumentChar();
      character.character = (char) read;

      return character;
    }
  }
}
