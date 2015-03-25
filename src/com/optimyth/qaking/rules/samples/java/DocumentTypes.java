/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 12/10/13 9:37 PM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.clases.JavaRuleUtils;
import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.optimyth.qaking.highlevelapi.dsl.Query;
import com.optimyth.qaking.java.hla.ast.JavaModifiers;

import static com.als.java5.rule.util.JavaDocUtil.TRIVIAL_COMMENT;
import static com.optimyth.qaking.highlevelapi.dsl.Query.query;
import static com.optimyth.qaking.highlevelapi.matchers.HLAPredicates.types;
import static com.optimyth.qaking.java.hla.JavaPredicates.commentCheck;
import static com.optimyth.qaking.java.hla.JavaPredicates.hasAnyAccessType;

/**
 * DocumentTypes - Sample rule to find uncommented Java classes.
 * <p/>
 * Strategy is simple: report any class in source (nested classes also) with the requested visibility,
 * that have trivial comments.
 * <p/>
 * This example uses a query on the high-level AST (matching JavaType nodes).
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 10-12-2013
 */
public class DocumentTypes extends AbstractRule {
  private Query uncommentedClass;

  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    String visibility = getProperty("visibility", "public");
    int modifiers = JavaModifiers.parse(visibility);

    uncommentedClass = query()
      .find( types(hasAnyAccessType(modifiers)) ) // classes of the requested visibility...
      .filter(commentCheck(TRIVIAL_COMMENT)) // ...should have non trivial comments...
      .report(); // ... so report classes of configured visibility with trivial comments    
  }

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    if(JavaRuleUtils.isJava(ctx)) { // check only for java code
      uncommentedClass.run(this, ctx, ctx.getHighLevelTree());
    }
  }
}
