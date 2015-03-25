/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/17/14 8:13 AM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.js.rules.AbstractJavaScriptRule;
import com.optimyth.qaking.js.ast.JSNode;

import static com.optimyth.qaking.js.utils.JavaScriptPredicates.*;

/**
 * UseBracesInConditionalsLoops - Simple rule that checks that conditional statements (if) or loops
 * (while, for, for in) have body between braces.
 * <p/>
 * This rule shows how to use a predicate to check AST for bad conditions.
 * <pre>
 * Always put (curly) braces around blocks in loops and conditionals.
 *
 * JavaScript allows omitting curly braces when body consists of only one statement:
 *    while (moreItems())
 *      x = consume(); // VIOLATION here
 *
 * However this can lead to bugs (processEach(x) seems part of the loop but it is NOT):
 *
 *    while (moreItems())
 *      x = consume();
 *      processEach(x); // indentation does not make processEach() call in loop
 * </pre>
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 17-01-2014
 */
public class UseBracesInConditionalsLoops extends AbstractJavaScriptRule {

  // Match a non-block statement representing body of conditional or loop
  private static NodePredicate badStatement = new NodePredicate() {
    public boolean is(BaseNode node) {
      JSNode parent = (JSNode)node.getParent();
      if(parent != null && (IF.is(parent) || LOOP.is(parent))) {
        if(node.isTypeName("Block") || node.isTypeName("Scope"))  return false; // a block, OK
        // Is target statement the body in parent control statement?
        JSNode target = (JSNode)node;
        int pos = target.getChildPos();
        int last = parent.getNumChildren() - 1;
        if(IF.is(parent)) return pos > 0; // 1 body (pos==1) and optionally else (pos==2). Pos 0 is the test expression
        else if(WHILE.is(parent)) return pos==1;
        else if(DO_WHILE.is(parent)) return pos==0;
        else if(FOR.is(parent) || FOR_IN.is(parent)) return pos==last; // body is the last node

      }

      return false;
    }
  };

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    // you may use Query.query().find(badStatement).report() instead
    check(root, badStatement, ctx);
  }
}
