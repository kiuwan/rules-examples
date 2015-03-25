/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/13/14 7:29 AM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.CobolAstUtil;
import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.optimyth.qaking.cobol.ast.CobolNode;
import com.optimyth.qaking.cobol.hla.ast.DataEntry;
import com.optimyth.qaking.cobol.util.Declarations;
import com.optimyth.qaking.highlevelapi.dsl.Query;

import static com.als.cobol.UtilCobol.STATEMENT;
import static com.optimyth.qaking.cobol.hla.primitives.CobolPredicates.ARITH_STATEMENTS;

/**
 * NoDisplayDataInArithmeticOp - Do not use DISPLAY data types as operands in arithmetic expressions.
 * <p/>
 * The strategy for implementing this rule is based on two predicates, dataRefInArithStmt (which
 * find data references in arithmetic operations, but not in other operations) for search,
 * and isDisplayType (which match data references on DISPLAY types) for filtering. Of course
 * both predicates could be collapsed in a single one. The rule then simply combines these predicates
 * in a Query.
 *
 * Standard rule COBOYR_NDIS does exactly what this sample rule performs.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 13-01-2014
 */
public class NoDisplayDataInArithmeticOp extends AbstractCobolRule {
  
  // Match QualifiedDataName operand in arithmetic statement
  private final NodePredicate dataRefInArithStmt = new NodePredicate() {
    // To check that data item reference iside arithmetic statement
    private NodePredicate onArithStatement = new NodePredicate() {
      public boolean is(BaseNode node) {
        CobolNode containerStmt = ((CobolNode)node).ancestor(STATEMENT).child(0);
        return ARITH_STATEMENTS.is(containerStmt);
      }
    };
    public boolean is(BaseNode node) {
      if(node.isTypeName("QualifiedDataName")) {
        CobolNode n = (CobolNode)node;
        return onArithStatement.is(n);
      }
      return false;
    }
  };

  // Match data item with DISPLAY or DISPLAY-1 types
  private final NodePredicate isDisplayType = new NodePredicate() {
    public boolean is(BaseNode node) {
      DataEntry de = Declarations.getDataEntry(node); // Find data item declaration in DATA DIVISION
      if(de == null) return false; // no data item declaration in DATA DIVISION
      String type = de.getType();
      return "DISPLAY".equalsIgnoreCase(type) || "DISPLAY-1".equalsIgnoreCase(type);
    }
  };
  
  // "Match data reference in arithmetic expression where the data item type is DISPLAY type"
  private final Query query = Query.query()
    .find(dataRefInArithStmt) // get data references in arithmetic statement
    .filter(isDisplayType) // ... but only DISPLAY / DISPLAY-1 types
    .report();

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    query.run(this, ctx, CobolAstUtil.getProcedureDivision(root));
  }
}
