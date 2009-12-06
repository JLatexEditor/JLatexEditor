package util.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff.
 */
public class Diff {
  public static final int COST_REMOVE = 3;
  public static final int COST_ADD    = 3;
  public static final int COST_CHANGE = 3;
  
  private ArrayList<Cost> garbage = new ArrayList<Cost>();

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

    Cost[] previousCosts = new Cost[length1+1];
    Cost[] costs = new Cost[length1+1];

    previousCosts[0] = createCost(Cost.TYPE_UNCHANGED, 0, null);
    for(int i = 1; i<=length1; i++) previousCosts[i] = createCost(Cost.TYPE_REMOVE, i, previousCosts[i-1]);

    /*
    for(int j = 1; j<=length2; j++) {
       T t_j = list2[j-1];
       costs[0] = j;

       for(int i=1; i<=length1; i++) {
          cost = list1.charAt(i-1)==t_j ? 0 : 1;
          // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
          costs[i] = Math.min(Math.min(costs[i-1]+1, previousCosts[i]+1),  previousCosts[i-1]+cost);
       }

       // copy current distance counts to 'previous row' distance counts
       Cost[] swap = previousCosts;
       previousCosts = costs;
       costs = swap;
    }
    */

    // our last action in the above loop was to switch d and p, so p now
    // actually has the most recent cost counts
    //return p[length1];

    return null;
  }

  private Cost createCost(int type, int costs, Cost parent) {
    Cost cost;
    if(garbage.isEmpty()) {
      cost = new Cost();
    } else {
      cost = garbage.remove(garbage.size()-1);
    }
    cost.create(type, costs, parent);
    return cost;
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

    public void create(int type, int costs, Cost parent) {
      this.type = type;
      this.parent = parent;
      this.costs = costs;

      references = 1;
      if(parent != null) parent.referencesIncrease();
    }

    public int getType() {
      return type;
    }

    public int getCosts() {
      return costs;
    }

    public Cost getParent() {
      return parent;
    }

    public void referencesDecrease() {
      references--;
      if(references == 0) {
        if(this.parent != null) this.parent.referencesDecrease();
        garbage.add(this);
      }
    }

    private void referencesIncrease() {
      references++;
    }
  }
}
