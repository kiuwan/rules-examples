/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/16/14 9:52 PM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.core.ast.NodeVisitor;
import com.als.js.rules.AbstractJavaScriptRule;
import com.google.common.collect.Sets;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowNavigator;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowVisitor;
import com.optimyth.qaking.codeanalysis.controlflow.model.DataFlowGraph;
import com.optimyth.qaking.codeanalysis.controlflow.model.DataFlowNode;
import com.optimyth.qaking.codeanalysis.controlflow.model.IDataFlowNode;
import com.optimyth.qaking.js.ast.JSNode;
import com.optimyth.qaking.js.controlflow.builder.JavascriptControlFlowSupport;

import java.util.Set;

import static com.als.core.ast.NodePredicates.types;
import static com.optimyth.qaking.js.utils.JavaScriptPredicates.FUNCTION;

/**
 * UnreachableCode - Sample rule that finds unreachable code, to show how to use control-flow
 * graph and constant
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 16-01-2014
 */
public class UnreachableCode extends AbstractJavaScriptRule {

  // Statements of interest
  private static final NodePredicate STATEMENT = types(
    "VariableInitializer", "ExpressionStatement", "EmptyStatement",
    "IfStatement", "SwitchStatement",
    "WhileLoop", "DoLoop", "ForLoop", "ForInLoop",
    "ReturnStatement", "BreakStatement", "ContinueStatement", "ThrowStatement"
  );

  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    if(!(root instanceof JSNode)) return;

    // process all function definitions
    ((JSNode)root).accept(new NodeVisitor() {
      public void visit(BaseNode function) {
        if(FUNCTION.is(function)) {
          checkUnused((JSNode)function, ctx);
        }
      }
    });
  }

  // To find unused statements in given function, first all statements of interest are registered,
  // then the control flow graph
  private void checkUnused(JSNode function, RuleContext ctx) {
    // JavascriptControlFlowSupport.getFlowGraph() compiles control-flow graph for FunctionNode
    DataFlowGraph<DataFlowNode> cfg = new JavascriptControlFlowSupport().getFlowGraph(function);

    // Get all statements of interest in current function
    final Set<BaseNode> statements = Sets.newHashSetWithExpectedSize(128);
    function.accept(new NodeVisitor() { 
      public void visit(BaseNode node) {
        if(STATEMENT.is(node)) statements.add(node);
      }
    });

    // Remove all statements reachable traversing control-flow graph in depth-first order
    // (breadth-first should visit same statements).
    // Start at the start node of the control-flow graph for function, up to the end
    ControlFlowNavigator.forwardDFT(cfg, new ControlFlowVisitor() {
      public boolean onDataFlowNode(IDataFlowNode node) {
        statements.remove(node.getAstNode()); // reachable from function entry point, remove from set
        return true; // navigation should continue while pending nodes
      }
    });

    // Statements remaining up to this point are unreachable, report them
    for(BaseNode unreached : statements) {
      reportViolation(ctx, unreached);
    }
  }
}
