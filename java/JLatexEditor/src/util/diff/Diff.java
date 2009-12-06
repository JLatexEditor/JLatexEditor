package util.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff.
 */
public class Diff {
  public static final int COST_REMOVE = 1;
  public static final int COST_ADD = 1;
  public static final int COST_CHANGE = 1;

  private ArrayList<Cost> garbage = new ArrayList<Cost>();

  public <T extends Metric> int costs(T[] list1, T[] list2) {
    // TODO: optimize, we do not need modifications for the costs
    List<Modification> modifications = diff(list1, list2);

    int costs = 0;
    for(Modification modification : modifications) {
      if(modification.getType() == Modification.TYPE_ADD) costs += modification.getLength();
      if(modification.getType() == Modification.TYPE_REMOVE) costs += modification.getLength();
      if(modification.getType() == Modification.TYPE_CHANGED) costs += modification.getLength();
    }

    return costs;
  }

  public <T extends Metric> List<Modification> diff(T[] list1, T[] list2) {
    int length1 = list1.length;
    int length2 = list2.length;

    ArrayList<Modification> modifications = new ArrayList<Modification>();
    if (length1 == 0) {
      modifications.add(new Modification(Modification.TYPE_ADD, 0, 0, length2));
      return modifications;
    }
    if (length2 == 0) {
      modifications.add(new Modification(Modification.TYPE_REMOVE, 0, 0, length1));
      return modifications;
    }

    Cost[] previousCosts = new Cost[length1 + 1];
    Cost[] currentCosts = new Cost[length1 + 1];

    previousCosts[0] = createCost(Cost.TYPE_UNCHANGED, 0, null);
    for (int i = 1; i <= length1; i++) previousCosts[i] = createCost(Cost.TYPE_REMOVE, COST_REMOVE, previousCosts[i - 1]);

    for (int index2 = 1; index2 <= length2; index2++) {
      T object2 = list2[index2 - 1];
      currentCosts[0] = createCost(Cost.TYPE_ADD, COST_ADD, previousCosts[0]);

      for (int index1 = 1; index1 <= length1; index1++) {
        T object1 = list1[index1 - 1];

        int distance = object1.getDistance(object2, COST_CHANGE);
        int remove = currentCosts[index1 - 1].getCosts() + COST_REMOVE;
        int add = previousCosts[index1].getCosts() + COST_ADD;
        int change = previousCosts[index1-1].getCosts() + distance;

        if(remove <= add && remove <= change) {
          currentCosts[index1] = createCost(Cost.TYPE_REMOVE, COST_REMOVE, currentCosts[index1 - 1]);
        } else if(change <= add) {
          currentCosts[index1] = createCost(distance == 0 ? Cost.TYPE_UNCHANGED : Cost.TYPE_CHANGE, distance, previousCosts[index1 - 1]);
        } else {
          currentCosts[index1] = createCost(Cost.TYPE_ADD, COST_ADD, previousCosts[index1]);
        }
      }

      // garbage collect
      for(Cost cost : previousCosts) cost.referencesDecrease();
      
      for(Cost cost : currentCosts) {
        System.out.print(cost.getCosts());
      }
      System.out.println("");

      Cost[] swap = previousCosts;
      previousCosts = currentCosts;
      currentCosts = swap;
    }

    // create list of changes
    ArrayList<Cost> costs = new ArrayList<Cost>(length1*2);
    Cost cost = previousCosts[length1];
    while(cost.getParent() != null) {
      costs.add(cost);
      cost = cost.getParent();
    }

    int index1 = 0;
    int index2 = 0;
    for(int i = costs.size() - 1; i >= 0; i--) {
      cost = costs.get(i);
      if(cost.type == Cost.TYPE_UNCHANGED) {
        index1++;
        index2++;
        continue;
      }

      int count = 1;
      while(i >= 1 && costs.get(i-1).type == cost.type) {
        i--;
        count++;
      }

      if(cost.type == Cost.TYPE_ADD) {
        modifications.add(new Modification(Modification.TYPE_ADD, index1, index2, count));
        index2 += count;
      } else
      if(cost.type == Cost.TYPE_REMOVE) {
        modifications.add(new Modification(Modification.TYPE_REMOVE, index1, index2, count));
        index1 += count;
      } else
      if(cost.type == Cost.TYPE_CHANGE) {
        modifications.add(new Modification(Modification.TYPE_CHANGED, index1, index2, count));
        index1 += count;
        index2 += count;
      }
    }

    return modifications;
  }

  private Cost createCost(int type, int costs, Cost parent) {
    Cost cost;
    if (garbage.isEmpty()) {
      cost = new Cost();
    } else {
      cost = garbage.remove(garbage.size() - 1);
    }
    cost.create(type, costs, parent);
    return cost;
  }

  private class Cost {
    public static final byte TYPE_UNCHANGED = 0;
    public static final byte TYPE_ADD = 1;
    public static final byte TYPE_REMOVE = 2;
    public static final byte TYPE_CHANGE = 3;

    private int type = TYPE_UNCHANGED;
    private int costs = 0;
    private Cost parent = null;

    // references count for own garbage reuse
    private int references = 1;

    public void create(int type, int costs, Cost parent) {
      this.type = type;
      this.parent = parent;
      references = 1;

      if (parent != null) {
        parent.referencesIncrease();
        this.costs = parent.costs + costs;
      } else {
        this.costs = costs;
      }
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
