/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/17/14 12:48 PM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.js.rules.AbstractJavaScriptRule;
import com.optimyth.qaking.highlevelapi.dsl.Query;

import static com.als.core.ast.NodePredicates.has;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.CONDITION_EXPR;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.getTypeImagePred;

/**
 * AvoidBitwiseOperators - Avoid bitwise operators (& and |) in conditional expressions
 * (i.e. when type is a boolean).
 * <p/>
 * This is to avoid errors when confusing a bitwise operator with its equivalent logical operator
 * (e.g. & instead of && or | instead of ||).
 * <p/>
 * The rest of bitwise operators (^ a.k.a. XOR, >>, >>>, <<, ~) are allowed by the rule.
 * Rule allows assignment composed with bitwise operator, like &= and |=.
 * <p/>
 * A conditional expression is the if(expr), while(expr), do...while(expr), for(...;expr;...),
 * or left-hand operand in ternary conditional operator <code>cond ? expr1 : expr2</code>.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 17-01-2014
 */
public class AvoidBitwiseOperatorsInConditions extends AbstractJavaScriptRule {

  // bitwise operator that could be mistyped with equivalent logical operator (&& and ||)
  private static NodePredicate BITWISE_EXPR = getTypeImagePred("InfixExpression", "&", "|");

  private Query forbiddenExpr = Query.query()
    .find(CONDITION_EXPR)      // conditional expression
    .filter(has(BITWISE_EXPR)) // ... having a bitwise operator that could be mistyped
    .report();

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    forbiddenExpr.run(this, ctx, root);
  }
}
