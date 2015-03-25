/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 11/22/13 8:52 AM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.optimyth.qaking.highlevelapi.dsl.Query;

import static com.als.java5.rule.util.JavaDocUtil.TRIVIAL_COMMENT;
import static com.optimyth.qaking.highlevelapi.dsl.Query.query;
import static com.optimyth.qaking.highlevelapi.matchers.HLAPredicates.methods;
import static com.optimyth.qaking.java.hla.JavaPredicates.byName;
import static com.optimyth.qaking.java.hla.JavaPredicates.commentCheck;

/**
 * DocumentToString - Sample rule that checks that toString() methods have (non-empty) JavaDoc.
 * Please note that appropiate predicates make rule implementation trivial.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 22-11-2013
 */
public class DocumentToString extends AbstractRule {
  /**
   * Find toString() methods with no or trivial preceeding comment
   */
  private static final Query uncommented = query()
    .find(methods(byName("toString")))
    .filter(commentCheck(TRIVIAL_COMMENT))
    .report();

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    uncommented.run(this, ctx, ctx.getHighLevelTree());
  }

}
