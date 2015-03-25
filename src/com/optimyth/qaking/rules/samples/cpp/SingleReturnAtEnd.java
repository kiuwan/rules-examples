/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/18/14 1:52 PM
 */

package com.optimyth.qaking.rules.samples.cpp;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.TreeNode;
import com.optimyth.cpp.rules.AbstractCppRule;

import java.util.List;

import static com.optimyth.qaking.cpp.hla.primitives.CppPredicates.FUNCTION_DEFINITION;
import static com.optimyth.qaking.cpp.hla.primitives.CppPredicates.RETURN;

/**
 * SingleReturnAtEnd - Sample rule that checks if function definitions have at most one return statement as last
 * statement in function body. This is required by IEC 61508 (or MISRA-C 14.7), under good programming style,
 * to avoid subtle errors in control logic.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 18-02-2014
 */
public class SingleReturnAtEnd extends AbstractCppRule {
  
  @Override protected void doVisit(BaseNode function, final RuleContext ctx) {
    if(FUNCTION_DEFINITION.is(function)) {
      TreeNode f = TreeNode.on(function);
      List<BaseNode> rets = f.findAll(RETURN);
      if(rets.isEmpty()) return; // no ret, allowed
      if(rets.size() > 1) {
        // 2 or more returns, violation
        ctx.getReport().addRuleViolation( violation(ctx, function) );

      } else if(rets.size()==1) {
        // violation unless return is the last statement in function body
        TreeNode statements = f.child("func_decl_def").child("compound_statement").child("statement_list");
        BaseNode lastStatement = statements.lastChild().child("jump_statement").get();
        if(rets.get(0) != lastStatement) {
          // Return was NOT the last statement
          ctx.getReport().addRuleViolation( violation(ctx, function) );
        }
      }
    }
  }
  
}
