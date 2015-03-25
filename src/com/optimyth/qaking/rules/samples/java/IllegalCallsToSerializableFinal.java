/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 11/19/13 2:23 PM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.java5.rule.model.sentence.MethodCall;
import com.optimyth.qaking.highlevelapi.dsl.Query;
import es.als.util.StringUtils;

import java.util.Collections;
import java.util.Set;

import static com.als.core.ast.NodePredicates.or;
import static com.als.java5.rule.util.ExpressionUtil.parseRefChain;
import static com.optimyth.qaking.highlevelapi.matchers.HLAPredicates.methods;
import static com.optimyth.qaking.highlevelapi.dsl.Query.query;
import static com.optimyth.qaking.highlevelapi.navigation.Navigations.sequence;
import static com.optimyth.qaking.java.hla.JavaPredicates.*;
import static com.optimyth.qaking.java.hla.JavaNavigations.*;

/**
 * IlllegalCallsToSerializableFinal - Look for calls to forbidden methods in private methods,
 * when method is in a Serializable or final class.
 * <p/>
 * Not much sense for quality issues, but interesting as an example of what could be done
 * with the Query API.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 19-11-2013
 */
public class IllegalCallsToSerializableFinal extends AbstractRule {
  
  private Set<String> forbiddenMethodNames = Collections.emptySet();

  // Predicate that will match calls to forbidden methods
  private final NodePredicate forbiddenMethods = new NodePredicate() {
    public boolean is(BaseNode node) {
      if(node.isTypeName("PrimaryExpression")) {
        // Not very efficient, but easy to read
        MethodCall call = (MethodCall) parseRefChain(node).getNext();
        return forbiddenMethodNames.contains( call.getMethodName() );
      }
      return false;
    }
  };

  // Auxiliar traversal query, going to method definition, then to its container (class),
  // and then keep only class if serializable or final.
  // Please note that definition() is an "inter-AST" facility,
  // that will try to fetch a method definition in (possibly) another class.
  private final Query inSerialOrFinalClass = query()
    .navigate(sequence(methodDeclaration, container))
    .filter(or(isSerializable, isFinalPred));

  // In private methods, find calls to forbidden methods, and keep only calls
  // to methods reachable by isDefiningClassSerialOrFinal. Such calls are reported
  private final Query badCalls = query()
    .find(methods(isPrivate))
    .find(calls(forbiddenMethods))
    .filter(inSerialOrFinalClass)
    .report();
  
  @Override protected void visit(BaseNode root, RuleContext ctx) {
    // Rule simply executes badCalls query
    badCalls.run(this, ctx, ctx.getHighLevelTree());
  }

  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    // names for forbidden methods could be configured in rule property 'forbiddenMethodNames'
    forbiddenMethodNames = StringUtils.asSet(getProperty("forbiddenMethodNames", ""), ',');
  }
}
