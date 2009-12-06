package util.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff.
 */
public class Diff {
  public <T extends Metric> List<Modification> diff(T[] list1, T[] list2) {
    int length1 = list1.length;
    int length2 = list2.length;

    ArrayList<Modification> modifications = new ArrayList<Modification>();
    if(length1 == 0) {
      modifications.add(new Modification(Modification.TYPE_ADD, 0, length2));
      return modifications;
    }
    if (length2 == 0) {
      modifications.add(new Modification(Modification.TYPE_REMOVE, 0, length1));
      return modifications;
    }

    /*
    int p[] = new int[length1+1]; //'previous' cost array, horizontally
    int d[] = new int[length1+1]; // cost array, horizontally
    int _d[]; //placeholder to assist in swapping p and d

    // indexes into strings list1 and list2
    int i; // iterates through list1
    int j; // iterates through list2

    char t_j; // jth character of list2

    int cost; // cost

    for (i = 0; i<=length1; i++) {
       p[i] = i;
    }

    for (j = 1; j<=length2; j++) {
       t_j = list2.charAt(j-1);
       d[0] = j;

       for (i=1; i<=length1; i++) {
          cost = list1.charAt(i-1)==t_j ? 0 : 1;
          // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
          d[i] = Math.min(Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);
       }

       // copy current distance counts to 'previous row' distance counts
       _d = p;
       p = d;
       d = _d;
    }

    // our last action in the above loop was to switch d and p, so p now
    // actually has the most recent cost counts
    return p[length1];
    */
    return null;
  }

  private class Cost {
    public static final byte TYPE_UNCHANGED = 0;
    public static final byte TYPE_ADD       = 1;
    public static final byte TYPE_REMOVE    = 2;
    public static final byte TYPE_CHANGE    = 3;

    private int type = TYPE_UNCHANGED;
    private int costs = 0;
    private Cost parent = null;

    // references count for own garbage reuse
    private int references = 1;

    private Cost() {
    }

    public int getType() {
      return type;
    }

    public void setType(int type) {
      this.type = type;
    }

    public int getCosts() {
      return costs;
    }

    public void setCosts(int costs) {
      this.costs = costs;
    }

    public Cost getParent() {
      return parent;
    }

    public void setParent(Cost parent) {
      if(parent != null) parent.referencesDecrease();
      this.parent = parent;
    }

    public void referencesDecrease() {

    }
  }
}
