/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/18/14 10:41 AM
 */

package com.optimyth.qaking.rules.samples.cpp;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.optimyth.cpp.rules.AbstractCppRule;
import com.optimyth.cpp.rules.CppToViolation;
import com.optimyth.qaking.cpp.hla.ast.CppVariableDeclaration;
import com.optimyth.qaking.highlevelapi.dsl.Query;

/**
 * AvoidGlobalVars - Avoid using global variables.
 * <p/>
 * Global constants or type definitions (typedef) are excluded.
 * The rule also permits configuration allowing static global vars
 * (as file scope is more restricted than global scope).
 * <p/>
 * Sample rule for detecting global variables, using Query API and high-level syntax tree.
 * Optimyth provides a similar standard rule OPT.CPP.AvoidGlobalVars.
 * <p/>
 * Please note that this rule could be implemented in different ways.
 * A concise alternative way is to use XPath <code>qak:hla()//GlobalVariable[@Constant='false']</code>
 * (adding extra predicate <code>[@Static='false']</code> when admitStaticGlobalVars enabled).
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 18-02-2014
 */
public class AvoidGlobalVars extends AbstractCppRule {
  private static final boolean DEFAULT_ADMIT_STATIC_GLOBALS = false;

  private boolean admitStaticGlobalVars = DEFAULT_ADMIT_STATIC_GLOBALS;

  private final NodePredicate GLOBAL_VAR = new NodePredicate() {
    public boolean is(BaseNode node) {
      if( node.isTypeName("GlobalVariable") ) {
        CppVariableDeclaration varDecl = (CppVariableDeclaration)node;
        // exclude constants, and static vars (when admitStaticGlobalVars enabled)
        //noinspection RedundantIfStatement
        if(varDecl.isConstant() || (admitStaticGlobalVars && varDecl.isStatic())) return false;
        return true;
      }
      return false;
    }
  };

  // Find (and report) violations of interest
  private final Query query = Query.query().find(GLOBAL_VAR).report(CppToViolation.instance());

  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    admitStaticGlobalVars = getProperty("admitStaticGlobalVars", DEFAULT_ADMIT_STATIC_GLOBALS);
  }

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    query.run(this, ctx, ctx.getHighLevelTree()); // run the query on the high-level tree
  }
}
