/**
 * @author Jörg Endrullis
 */

package editor.syntaxhighlighting;

import java.util.Iterator;
import java.util.Stack;

public class ParserStateStack{
  private Stack stateStack = new Stack();

  /**
   * Adds a state at the top of the stack.
   *
   * @param state the state
   */
  public void push(ParserState state){
    stateStack.push(state);
  }

  /**
   * Removes the top element of the stack.
   *
   * @return the top element
   */
  public ParserState pop(){
    return (ParserState) stateStack.pop();
  }

  /**
   * Returns the top element without removing it.
   *
   * @return the top element of the stack
   */
  public ParserState peek(){
    return (ParserState) stateStack.peek();
  }

  /**
   * Returns true, if the stack is empty.
   *
   * @return true, if stack empty
   */
  public boolean isEmpty(){
    return stateStack.isEmpty();
  }

  /**
   * Returns the size of the stack.
   *
   * @return number of elements
   */
  protected int size(){
    return stateStack.size();
  }

  /**
   * Returns the element at the given position.
   *
   * @param nr
   * @return
   */
  protected ParserState get(int nr){
    return (ParserState) stateStack.get(nr);
  }

  /**
   * Compares two parser state stacks.
   *
   * @param psStack the stack to compare with
   * @return true, if they are equal
   */
  public boolean equals(ParserStateStack psStack){
    if(psStack == null) return false;
    if(stateStack.size() != psStack.size()) return false;

    for(int state_nr = 0; state_nr < stateStack.size(); state_nr++){
      if(!get(state_nr).equals(psStack.get(state_nr))) return false;
    }

    return true;
  }

  /**
   * Clone the stack.
   *
   * @return the clone
   */
  public ParserStateStack copy(){
    ParserStateStack clone = new ParserStateStack();

    Iterator stateIterator = stateStack.iterator();
    while(stateIterator.hasNext()){
      clone.push(((ParserState) stateIterator.next()).copy());
    }

    return clone;
  }
}
