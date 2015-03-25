/**
 * 
 */
package com.optimyth.qaking.rules.samples.csharp;

import java.util.Set;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.als.core.ast.TreeNode;
import com.google.common.collect.ImmutableSet;
import com.optimyth.csharp.model.CallSignature;

/**
 * UseOfConsoleOutput - Find uses of Console.Write or Console.WriteLine. We need low
 * level AST in this kind of rules, because high level AST does not represent expressions.
 * We use CallSignature class to process method calls.
 * @author <a href="mailto:jorge.para@optimyth.com">jpara</a>
 * @version 21/03/2015
 *
 */
public class UseOfConsoleOutput extends AbstractRule {

  private static final String CONSOLE_CLASS = "Console";
  private static final Set<String> FORBIDDEN_METHODS = ImmutableSet.<String>builder()
      .add("Write")
      .add("WriteLine")
      .build();
  
  @Override
  protected void visit(BaseNode root, final RuleContext ctx) {
    TreeNode.on(root).accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        CallSignature callSignature = CallSignature.build(node);
        //If CallSignature could not be built, node does not represents a method call
        if (callSignature == null) return;
        if(!FORBIDDEN_METHODS.contains(callSignature.getMethodName())) return;
        if(CONSOLE_CLASS.equals(callSignature.getClassName())){
          ctx.getReport().addRuleViolation( createRuleViolation(ctx, TreeNode.on(node).findLine()) );
        }
      }
    });
  }
  
}
