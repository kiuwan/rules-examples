/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/17/14 8:07 AM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.js.rules.AbstractJavaScriptRule;
import com.optimyth.qaking.highlevelapi.dsl.Query;
import com.optimyth.qaking.js.ast.JSNode;
import com.optimyth.qaking.js.utils.NodeUtil;

import static com.als.core.ast.NodePredicates.not;
import static com.als.core.ast.NodePredicates.or;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.*;

/**
 * NoBreakReturnInCase - Sample rule that reports on case blocks without break or return statements.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 17-01-2014
 */
public class NoBreakReturnInCase extends AbstractJavaScriptRule {

  private final NodePredicate breakOrReturn = or(BREAK_STATEMENT, RETURN_STATEMENT);

  // match SwitchCase nodes allowed:
  // - is last default in switch
  // - is empty case (first and second cases in case expr: case expr2: case expr3: nonEmpty allowed)
  // - has break/return child statement, or a block delimited with break/return child statement)
  private final NodePredicate isProperCase = new NodePredicate() {
    public boolean is(BaseNode node) {
      JSNode swCase = (JSNode)node;
      boolean isDefault = SWITCH_DEFAULT.is(swCase);
      if(isDefault && NodeUtil.isLastChild(swCase)) return true; // last default does not need break, allowed      
      if(isDefault && swCase.isLeaf()) return true; // empty default, allowed, even when not last in switch
      if(swCase.getNumChildren()==1) return true; // empty case, allowed

      // Rule mandates that break/return must be a child of switch case (or if a block, a block child)
      for(JSNode child : swCase) {
        if(breakOrReturn.is(child)) return true;
        if(child.isTypeName("Scope") && child.hasChildren(breakOrReturn)) return true;
      }

      return false;
    }
  };

  // Report violation on switch case (or default if not last)
  private final Query badSwitchCase = Query.query()
    .find(SWITCH_CASE)
    .filter(not(isProperCase))
    .report();

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    badSwitchCase.run(this, ctx, root);
  }
}
