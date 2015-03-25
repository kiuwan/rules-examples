/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/9/14 6:46 PM
 */

package com.optimyth.qaking.rules.samples.php;

import com.als.core.RuleContext;
import com.als.core.RuleViolation;
import com.als.core.ast.BaseNode;
import com.als.core.rule.XPathRule;
import com.optimyth.qaking.php.rules.ViolationFactory;

/**
 * AvoidRefOnMultidimArray - Sample rule that shows how to extend an XPathRule
 * and override {@link #reportViolation} to report violations correctly, even on
 * nodes in include'd AST trees.
 * <p/>
 * Checks for taking references on values from multidimensional arrays, like <code>&$arr[expr][expr]</code>.
 * Please note that <code>&$arr[expr]</code> is allowed, there must be two or more array index suffixes.
 * <p/>
 * A "simple" XPath expression fits the bill, easy to find and check in RuleEditor:
 * <pre>
 * //UnaryExpression[ UnaryOperator[1][@Image='&'] ]
 * [ count(UnaryExpression/PostfixExpression/PostfixExpressionArrayIndexSuffix) > 1 ]
 * </pre>
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 09-02-2014
 */
public class AvoidRefOnMultidimArray extends XPathRule {
  
  private static final String REF_ON_MULTIDIM_ARRAY =
    "//UnaryExpression[ UnaryOperator[1][@Image='&'] ]" +
    "[ count(UnaryExpression/PostfixExpression/PostfixExpressionArrayIndexSuffix) > 1 ]";
  
  @Override public void initialize(RuleContext ctx) {
    addProperty(XPATH_PROP, REF_ON_MULTIDIM_ARRAY);
    super.initialize(ctx);
  }

  @Override protected RuleViolation reportViolation(BaseNode node, RuleContext ctx) {
    RuleViolation rv = ViolationFactory.createViolation(this, ctx, node);
    ctx.getReport().addRuleViolation( rv );
    return rv;
  }
}
