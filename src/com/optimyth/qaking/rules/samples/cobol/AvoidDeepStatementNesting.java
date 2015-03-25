/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/3/14 9:51 PM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.als.core.ast.TreeNode;
import com.google.common.collect.ImmutableMap;
import com.optimyth.qaking.cobol.hla.ast.CobolStatement;
import com.optimyth.qaking.cobol.hla.ast.ProcedureDivision;

import java.util.Map;

/**
 * AvoidDeepStatementNesting - Sample rule that checks for excessive nesting on different
 * control statements (IF, EVALUATE, PERFORM).
 * <p/>
 * Shows how to implement a rule using the high-level AST.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 03-02-2014
 */
public class AvoidDeepStatementNesting extends AbstractCobolRule {
  
  // Maximum allowed nesting for each statement
  // If you want, thresholds could be configured by rule properties...
  private final Map<String, Integer> THRESHOLDS = ImmutableMap.<String, Integer>builder()
    .put("IfStatement", 2)
    .put("EvaluateStatement", 1)
    .put("PerformStatement", 1)
    .build();
      
  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    ProcedureDivision pd = getProcedureDivision(ctx);
    if(pd == null) return; // possibly a COPY without statements
    pd.accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        if (!(node instanceof CobolStatement)) {return;}
        CobolStatement stmt = (CobolStatement)node;
        if(THRESHOLDS.containsKey(stmt.getTypeName())) {
          int maxAncestors = THRESHOLDS.get(stmt.getTypeName());
          TreeNode n = TreeNode.on(stmt);
          int ancestors = n.countAncestors(stmt.getTypeName());
          if(ancestors > maxAncestors) {
            // Report on the topmost statement of the same type
            TreeNode topStatement = n.topmostAncestor(stmt.getTypeName());
            addViolation(violation(ctx, topStatement), ctx);
          }
        }
      }
    });
  }
}
