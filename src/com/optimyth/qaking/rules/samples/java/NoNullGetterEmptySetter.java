/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 11/19/13 1:17 PM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.optimyth.qaking.highlevelapi.ast.oo.HLAMethod;
import com.optimyth.qaking.highlevelapi.dsl.Query;

import static com.optimyth.qaking.highlevelapi.dsl.Query.query;
import static com.als.core.ast.NodePredicates.asNodePredicate; 
import static com.optimyth.qaking.highlevelapi.matchers.HLAPredicates.methods;
import static com.optimyth.qaking.java.hla.JavaPredicates.*;

/**
 * NoNullGetterEmptySetter - Sample rule that reports "trivial" getters/setters
 * (a setter with no body, or a getter with a trivial return null).
 * <p/>
 * This is very simple (a one-liner) with the Query API and the proper predicates.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 19-11-2013
 */
public class NoNullGetterEmptySetter extends AbstractRule {
  // Find setters with empty body and report 'em
  private final Query emptySetter = query().find(methods(isSetter))
    .filter(asNodePredicate(emptyBody, HLAMethod.class)).report();

  // Find getters that only return null and report 'em
  private final Query returnNullGetter = query().find(methods(isGetter))
    .filter(asNodePredicate(strictReturnsNull, HLAMethod.class)).report();

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    emptySetter.run(this, ctx, ctx.getHighLevelTree());
    returnNullGetter.run(this, ctx, ctx.getHighLevelTree());
  }
}
