/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/3/14 4:23 PM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.google.common.collect.Sets;
import com.optimyth.qaking.cobol.ast.CobolNode;
import com.optimyth.qaking.cobol.hla.ast.CobolStatement;
import com.optimyth.qaking.cobol.hla.ast.ProcedureDivision;
import com.optimyth.qaking.cobol.hla.ast.ProcedureSection;
import com.optimyth.qaking.cobol.hla.ast.Section;
import com.optimyth.qaking.cobol.hla.primitives.CobolPredicates;
import com.optimyth.qaking.cobol.util.Procedures;
import com.optimyth.qaking.cobol.util.ProcedureName;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowNavigator;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowVisitor;
import com.optimyth.qaking.codeanalysis.controlflow.model.DataFlowGraph;
import com.optimyth.qaking.codeanalysis.controlflow.model.DataFlowNode;
import com.optimyth.qaking.codeanalysis.controlflow.model.IDataFlowNode;
import com.optimyth.qaking.highlevelapi.ast.common.HLABehaviouralUnit;
import com.optimyth.qaking.highlevelapi.ast.statement.HLAStatement;

import java.util.Set;

import static com.optimyth.qaking.cobol.hla.primitives.CobolPredicates.NOP_STATEMENT_PRED;
import static com.optimyth.qaking.cobol.util.Procedures.UsedChecker;

/**
 * FindUnusedCode - Sample rule for Cobol, showing how to use control-flow graph to find unused code.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 03-01-2014
 */
public class FindUnusedCode extends AbstractCobolRule {
  
  @Override protected void visit(BaseNode root, RuleContext ctx) {
    CobolNode cu = (CobolNode)root;
    ProcedureDivision pd = getDivision(cu.getHighLevelNode(), ProcedureDivision.class);
    if(pd==null) return; // No procedure division, probably this is not a Cobol (sub)program
    
    DataFlowGraph<DataFlowNode> cfg = getFlow(pd, ctx);
    if(cfg==null) {
      getLogger().warn("Cannot fetch control flow graph for unit " + ctx.getSourceCodeFilename());
      return;
    }

    // Get the structure of Cobol procedures and related usages checker
    final Procedures units = new Procedures(pd);
    final UsedChecker checker = units.buildUsedChecker();
    // Get all statements, and traverse control flow graph to remove all reachable statements from program entry point
    // (for each reachable statement, containing procedure is marked as used)
    final Set<CobolStatement> statements = findStatements(pd);
    removeReachableStatements(cfg, checker, statements);
    // reporting of unused procedures (either trivial procedural sections or paragraphs, or )
    reportUnused(checker, statements, ctx);
  }

  /**
   * Collect all statements, but ignore NO-OP statements, like EXIT or CONTINUE
   * (they are used as markers or to delimit a sequence of paragraphs),
   * and excluding statements in DECLARATIVES (as they are not called explicitely)
   */
  private Set<CobolStatement> findStatements(ProcedureDivision pd) {
    final Set<CobolStatement> statements = Sets.newHashSetWithExpectedSize(256);

    pd.accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        if(node instanceof CobolStatement) {
          CobolStatement stmt = (CobolStatement)node;
          if( !NOP_STATEMENT_PRED.apply(stmt) && !inDeclaratives(stmt) ) {
            statements.add(stmt);
          }
        }
      }
      
      private boolean inDeclaratives(CobolStatement stmt) {
        return stmt.hasAncestor( Section.getPredicateByName("Declaratives") );
      }
    });

    return statements;
  }
  
  
  private void removeReachableStatements(DataFlowGraph<DataFlowNode> cfg, final UsedChecker checker, final Set<CobolStatement> statements) {
  // Visit all statements reachable in control-flow
    // BFT = breadth-first traversal of the statements graph, DFT (depth-first traversal) should have the same effect
    ControlFlowNavigator.forwardBFT(cfg, new ControlFlowVisitor() {
      public boolean onDataFlowNode(IDataFlowNode cfgNode) {
        BaseNode node = cfgNode.getAstNode();
        if (node instanceof CobolStatement) {
          CobolStatement stmt = (CobolStatement) node;
          statements.remove(stmt);
          checker.registerUsage(stmt);
        }
        return true; // continue navigation up to when no more statements could be traversed
      }
    });
    // statements not visited are unused
  }

  /**
   * Report unsued code without emitting too much violations. Strategy is:<ul>
   * <li>For full unused section, report it</li>
   * <li>For full unused paragraph, report it unless in a unused section, or when trivial 'marker' paragraph (single EXIT statement)</li>
   * <li>For each unused statement, report unless when it belongs to an already reported unused procedure</li>
   * </ul>
   */
  private void reportUnused(UsedChecker checker, Set<CobolStatement> unusedStmts, RuleContext ctx) {
    for(HLABehaviouralUnit unused : checker.unusedProcedures()) {
      if(CobolPredicates.TRIVIAL_PROCEDURE.apply(unused)) continue; // Single EXIT or CONTINUE paragraph, ignore
      String msg = getMessage() + ": unused " + (unused instanceof ProcedureSection ? "section " : "procedure ") + unused.getName();
      addViolation(violation(ctx, unused, unused.getBeginLine(), msg), ctx);
    }

    for(HLAStatement unusedStmt : unusedStmts) {
      ProcedureName where = ProcedureName.getContainingProcedure(unusedStmt);
      if( where != null && !checker.isUnusedProcedure(where) ) {
        String msg = getMessage() + ": unused " + unusedStmt.getTypeName();
        addViolation(violation(ctx, unusedStmt, unusedStmt.getBeginLine(), msg), ctx);
      }
    }
  }

}
