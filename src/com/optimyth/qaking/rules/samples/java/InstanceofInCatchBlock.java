/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 12/10/13 10:46 PM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.core.ast.NodeVisitor;
import com.als.core.ast.TreeNode;
import com.als.jkingcore.ast.ASTInstanceOfExpression;

import static com.als.clases.JavaRuleUtils.report;
import static com.als.core.ast.TreeNode.on;

/**
 * InstanceofInCatchBlock - Sample rule that reports a bad practice: usage of instanceof operator
 * in catch blocks on the exception variable.
 * <p/>
 * Within a catch block the instanceof operator should not be used to distinguish between exceptions:
 * that's the purpose of the catch block itself.
 * <p/>
 * This is an example of the low-level API (TreeNode). For simple things it is very easy to
 * locate the nodes of interest in the AST, using TreeNode.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 10-12-2013
 */
public class InstanceofInCatchBlock extends AbstractRule {

  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    on(root).accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        if(node.isTypeName("CatchStatement")) {
          TreeNode catchBlock = on(node);
          // Report all usages of instanceof on the exception variable, using the auxiliar predicate
          final NodePredicate pred = hasInstanceOf( getExceptionVariable(catchBlock) );

          on(catchBlock).child("Block").accept(new NodeVisitor() { // find in catch body subtree
            public void visit(BaseNode badInstanceOf) {
              if (pred.is(badInstanceOf)) {
                report(InstanceofInCatchBlock.this, badInstanceOf, ctx);
              }
            }
          });
        }
      }
      // Get variable name in exception declaration
      private String getExceptionVariable(TreeNode catchBlock) {
        return catchBlock.child("FormalParameter").find("VariableDeclaratorId").getImage();
      }
    });
  }

  // Builds auxiliar predicate that matches instanceof usages on the exception variable
  private static NodePredicate hasInstanceOf(final String exceptionVar) {
    return new NodePredicate() {
      public boolean is(BaseNode instOf) {
        return
          instOf instanceof ASTInstanceOfExpression && // instanceof operator
          exceptionVar.equals( on(instOf).find("Name").getImage() ); // same var referenced
      }
    };
  }
}
