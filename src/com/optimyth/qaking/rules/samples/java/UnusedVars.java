/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 11/19/13 10:27 PM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.core.util.NodeToStr;
import com.optimyth.qaking.highlevelapi.dsl.Query;
import com.optimyth.qaking.highlevelapi.nodeset.ToViolation;
import com.optimyth.qaking.java.hla.ast.JavaVariable;

import static com.als.core.ast.NodePredicates.*;
import static com.optimyth.qaking.highlevelapi.dsl.Query.query;
import static com.optimyth.qaking.highlevelapi.nodeset.ToViolation.extraMessage;
import static com.optimyth.qaking.java.hla.JavaPredicates.*;

/**
 * UnusedVars - Simple rule that looks for unused vars: local var, class field or formal parameter.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 19-11-2013
 */
public class UnusedVars extends AbstractRule {
  // For reporting the name of the unused var
  private static final ToViolation reportVarName = extraMessage(new NodeToStr() {
    public String apply(BaseNode node) {
      return "unused " + ((JavaVariable) node).getName();
    }
  });

  // Match variable declarations of interest
  private static final NodePredicate varsPredicate = or(
    localVariablePred,
    // Unused private fields can only be used in other classes using reflection
    // In Java, some class fields like serialVersionUID or serialPersistentFields
    // are used by JVM, not by user code. How could you add such exceptions?
    and(instanceVariablePred, isPrivatePred),
    parameterPred
  );

  // What is unused var?
  private static final Query unusedVars = query()
    .find( varsPredicate )
    // An unused var should not have initialization with side-effects
    // (because then, declaration cannot be removed)
    .filter(not(hasSideEffectInInitPred))
    .filter(not(hasUsages))
    .report( reportVarName );


  @Override protected void visit(BaseNode root, RuleContext ctx) {
    unusedVars.run(this, ctx, ctx.getHighLevelTree()); // rule is simply query execution
  }

}
