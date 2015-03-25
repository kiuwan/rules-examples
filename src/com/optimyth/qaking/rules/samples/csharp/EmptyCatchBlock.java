/**
 * 
 */
package com.optimyth.qaking.rules.samples.csharp;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.RuleViolation;
import com.als.core.ast.BaseNode;
import com.optimyth.csharp.utils.DetailAST;
import com.optimyth.qaking.highlevelapi.ast.common.HLANode;
import com.optimyth.qaking.highlevelapi.ast.exception.HLACatch;
import com.optimyth.qaking.highlevelapi.ast.statement.HLABlock;

/**
 * EmptyCatchBlock - Find empty catch blocks using high level AST.
 * High level AST is a more compact, simple representation of AST, useful
 * depending on what we need to check in a rule.
 * 
 * @author <a href="mailto:jorge.para@optimyth.com">jpara</a>
 * @version 21/03/2015
 *
 */
public class EmptyCatchBlock extends AbstractRule {

  @Override
  protected void visit(BaseNode root, RuleContext ctx) {
    HLANode hlaRoot = ((DetailAST)root).getHighLevelNode();
    //In high level node we can find nodes using their class
    for (HLACatch catchBlock : hlaRoot.findAll(HLACatch.class)) {
      checkCatchBlock(ctx, catchBlock);
    }
  }

  private void checkCatchBlock(RuleContext ctx, HLACatch catchBlock) {
    if (catchBlock.child(HLABlock.class).isLeaf()) {
      RuleViolation rv = createRuleViolation(ctx,catchBlock.getBeginLine(), getMessage());
      ctx.getReport().addRuleViolation(rv);      
    }
  }
  
}
