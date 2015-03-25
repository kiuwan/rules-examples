/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 12/10/13 1:57 PM
 */
package com.optimyth.qaking.rules.samples.java;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.core.ast.TreeNode;
import com.optimyth.qaking.java.tainting.StringConcatTaintingPropagation;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.optimyth.qaking.highlevelapi.ast.common.HLAVariableDeclaration;
import com.optimyth.qaking.highlevelapi.dsl.Query;
import com.optimyth.qaking.highlevelapi.nodeset.NodeSet;

import static com.als.clases.JavaRuleUtils.report;
import static com.optimyth.qaking.highlevelapi.dsl.Query.query;
import static com.optimyth.qaking.highlevelapi.matchers.HLAPredicates.variables;
import static com.optimyth.qaking.highlevelapi.matchers.HLAPredicates.variablesInTypes;
import static com.optimyth.qaking.java.hla.JavaNavigations.navigateToCalls;
import static com.optimyth.qaking.java.hla.JavaPredicates.calls;

/**
 * AvoidConcatJdbcStatement - Very simple "tainting propagation" rule,
 * looks for execution of SQL statements via JDBC calls to Statement where the sql code
 * was concatenated with user-controlled input.
 * <p/>
 * This is just to show how to match calls of interest with the high-level API
 * and perform simple tainting propagation, not intended for production code.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 10-12-2013
 */
public class AvoidConcatJdbcStatement extends AbstractRule {

  // Check for any variable (formal param, field, local var) of type java.sql.Statement
  private static final Predicate<HLAVariableDeclaration> JDBC_STATEMENT = variablesInTypes("java.sql.Statement");

  // Check for calls to Statement methods with SQL in first arg
  private static final NodePredicate JDBC_CALL = calls(ImmutableSet.of("execute", "executeQuery", "executeUpdate", "addBatch"));

  // Find calls of interest: Statement.execute ... Statement.addBatch
  private final Query jdbcCalls = query().find( variables(JDBC_STATEMENT) ).find(JDBC_CALL, navigateToCalls);
  
  @Override protected void visit(BaseNode root, RuleContext ctx) {
    NodeSet calls = jdbcCalls.run(this, ctx, ctx.getHighLevelTree()).current();
    for(BaseNode call : calls) {
      TreeNode arg = TreeNode.on(call).find("ArgumentList").child(0); // first arg expression
      if(new StringConcatTaintingPropagation().isTainted(arg)) {
        report(this, call, ctx);
      }
    }
  }
}
