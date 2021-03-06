/**
 * @author Jörg Endrullis
 */

package sce.syntaxhighlighting;

import java.util.Stack;

public class ParserStateStack {
  private Stack<ParserState> stateStack = new Stack<ParserState>();

  /**
   * Adds a state at the top of the stack.
   *
   * @param state the state
   */
  public void push(ParserState state) {
    stateStack.push(state);
  }

  /**
   * Removes the top element of the stack.
   *
   * @return the top element
   */
  public ParserState pop() {
    return stateStack.pop();
  }

  /**
   * Returns the top element without removing it.
   *
   * @return the top element of the stack
   */
  public ParserState peek() {
    return stateStack.peek();
  }

  /**
   * Returns true, if the stack is empty.
   *
   * @return true, if stack empty
   */
  public boolean isEmpty() {
    return stateStack.isEmpty();
  }

  /**
   * Returns the size of the stack.
   *
   * @return number of elements
   */
  protected int size() {
    return stateStack.size();
  }

  /**
   * Returns the element at the given position.
   *
   * @param nr
   * @return
   */
  protected ParserState get(int nr) {
    return stateStack.get(nr);
  }

  /**
   * Compares two states state stacks.
   *
   * @param psStack the stack to compare with
   * @return true, if they are equal
   */
  public boolean equals(ParserStateStack psStack) {
    if (psStack == null) return false;
    if (stateStack.size() != psStack.size()) return false;

    for (int state_nr = 0; state_nr < stateStack.size(); state_nr++) {
      if (!get(state_nr).equals(psStack.get(state_nr))) return false;
    }

    return true;
  }

  /**
   * Clone the stack.
   *
   * @return the clone
   */
  public ParserStateStack copy() {
    ParserStateStack clone = new ParserStateStack();

    for (ParserState aStateStack : stateStack) {
      clone.push(aStateStack.copy());
    }

    return clone;
  }
}
