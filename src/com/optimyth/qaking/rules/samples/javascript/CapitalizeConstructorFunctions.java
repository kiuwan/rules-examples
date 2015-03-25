/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/20/14 7:47 PM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.js.rules.AbstractJavaScriptRule;
import com.optimyth.qaking.js.ast.JSNode;

/**
 * CapitalizeConstructorFunctions - Capitalize names of constructor functions.
 * <p/>
 * Capitalizing functions that are intended to be used with new operator is just a convention
 * that helps programmers to visually distinguish constructor functions from other types of functions
 * to help spot mistakes when using <code>this</code>.
 * <p/>
 * Not doing so won't break your code in any browsers or environments but it will be a bit harder
 * to figure out—by reading the code—if the function was supposed to be used with or without new.
 * And this is important because when the function that was intended to be used with new is used without it,
 * this will point to the global object instead of a new object.
 * <p/>
 * The sample rule simply checks that operator at the right of the <code>new</code> operator is
 * capitalized.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 20-01-2014
 */
public class CapitalizeConstructorFunctions extends AbstractJavaScriptRule {

  private static NodePredicate NEW_ON_BAD_CONSTRUCTOR_NAME = new NodePredicate() {
    public boolean is(BaseNode node) {
       if(node.isTypeName("NewExpression")) {
         JSNode newExpr = (JSNode)node;
         String name = newExpr.child(0).getImage();
         return !Character.isUpperCase( name.charAt(0) );
       }
      return false;
    }
  };

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    check(root, NEW_ON_BAD_CONSTRUCTOR_NAME, ctx);
  }
}
