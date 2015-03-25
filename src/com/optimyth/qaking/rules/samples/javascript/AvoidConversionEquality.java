/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/17/14 9:01 AM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.js.rules.AbstractJavaScriptRule;
import com.optimyth.qaking.highlevelapi.dsl.Query;
import com.optimyth.qaking.js.ast.JSNode;

import static com.als.core.ast.NodePredicates.not;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.NONSTRICT_EQUALITY_EXP;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.LITERAL_NULL;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.STRING_LITERAL;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.TYPEOF;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.IS_LENGTH_PROP;

/**
 * AvoidConversionEquality - Do not use non-strict == and != operators.
 *
 * The == (or !=) version of equality is quite liberal. Values may be considered equal even if they are different types,
 * since the operator will force coercion of one or both operators into a single type (usually a number)
 * before performing a comparison. This behaviour could be misinterpreted by programmers that introduce
 * subtle bugs.
 * <p/>
 * To see coercion rules in action, consider that condition <code>[0] == true</code> is false
 * (true is coerced to 1, while [0] -> "0" (because [0].valueOf() not primitive, [0].toString() used) -> 0,
 * and obviously 0 != 1).
 * <p/>
 * Please remember that type coercion, besides comparison operators, is everywhere in Javascript:
 * boolean conditions, array indexing, concatenation...
 * <p/>
 * Rule excludes some common cases where the semantics of non-strict operands does not pose any risk:
 * comparing with null, checking length property, or comparing result of typeof operator against a String literal.
 * <p/>
 * See <a hred="http://javascriptweblog.wordpress.com/2011/02/07/truth-equality-and-javascript/">Truth, Equality and JavaScript</a>
 * by Angus Croll, for a detailed description of ECMA rules describing type conversion for JavaScript
 * and how comparison operators work.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 17-01-2014
 */
public class AvoidConversionEquality extends AbstractJavaScriptRule {
  // Allowed cases
  private final NodePredicate ALLOWED = new NodePredicate() {
    public boolean is(BaseNode node) {
      JSNode expr = (JSNode)node;
      return isTypeofString(expr) || isNullCheck(expr) || isLengthCheck(expr);
    }
  };

  private final Query query = Query.query()
    .find(NONSTRICT_EQUALITY_EXP) // find expressions using == and !=
    .filter(not(ALLOWED)) // exclude allowed cases
    .report();
  
  @Override protected void visit(BaseNode root, RuleContext ctx) {
    query.run(this, ctx, root);
  }

  // Return true if node is InfixExpression containing UnaryExpression(typeof) as operand
  // and StringLiteral as other operand, that is, typeof expr == "function", which is allowed as typeof expr is a String.
  private static boolean isTypeofString(JSNode expr) {
    JSNode left = expr.child(0);
    JSNode right = expr.child(1);
    return
      (TYPEOF.is(left) && STRING_LITERAL.is(right)) ||
      (STRING_LITERAL.is(left) && TYPEOF.is(right));
  }

  // expr == null is typically correct and allowed (undefined and null are equal for == but not for ===)
  private static boolean isNullCheck(JSNode expr) {
    return LITERAL_NULL.is(expr.child(0)) || LITERAL_NULL.is(expr.child(1));
  }

  // x.length == constant_number is allowed, as length property is usually an integer
  // (unless an object definition with a different semantics on "length"). As Array and String are usual
  // objects with length property, this is enforced
  private static boolean isLengthCheck(JSNode expr) {
    JSNode left = expr.child(0);
    JSNode right = expr.child(1);
    return
      (left.isTypeName("NumberLiteral") && IS_LENGTH_PROP.is(right)) ||
      (right.isTypeName("NumberLiteral") && IS_LENGTH_PROP.is(left));
  }

}
