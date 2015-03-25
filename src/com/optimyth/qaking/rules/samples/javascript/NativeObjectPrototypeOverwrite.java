/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/20/14 5:57 PM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.js.rules.AbstractJavaScriptRule;
import com.optimyth.qaking.js.ast.JSNode;
import es.als.util.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * NativeObjectPrototypeOverwrite - Prohibits overwriting prototypes of native objects such as Array,
 * Date, Object, Function and so on.
 * <p/>
 * There are many ways to code prototype overwriting. Rule must first define which types are
 * considered native, and then found a PropertyGet (native object + '.prototype') at the
 * left-hand side of an assignment.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 20-01-2014
 */
public class NativeObjectPrototypeOverwrite extends AbstractJavaScriptRule {

  private static final String DEFAULT_TYPES_TO_CHECK = "Object,Function,Array,String,Boolean,Number,Date,RegExp,Error,window";

  private Set<String> typesToCheck = Collections.emptySet();

  // look for FORBIDDEN_TYPE.prototype = ... or FORBIDDEN_TYPE.prototype.newprop = ...
  private final NodePredicate PROTOTYPE_ASSIGN = new NodePredicate() {
    public boolean is(BaseNode node) {
      if(node.isTypeName("PropertyGet")) {
        JSNode pg = (JSNode)node;
        JSNode left = pg.child(0);
        JSNode right = pg.child(1);
        if(left.isTypeName("Name") && right.isTypeName("Name")) {
          boolean isPrototypeAccess = typesToCheck.contains(left.getImage()) && "prototype".equals(right.getImage());
          if(isPrototypeAccess) {
            if(node.getParent().isTypeName("PropertyGet")) node = node.getParent();
            return inLeftOfAssignment((JSNode)node);
          }
        }
      }
      return false;
    }

    // Chck if in LHS of an assignment
    private boolean inLeftOfAssignment(JSNode propGet) {
      return propGet.getParent().isTypeName("Assignment") && propGet.getChildPos() == 0;
    }
  };

  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    typesToCheck = StringUtils.asSet( getProperty("typesToCheck", DEFAULT_TYPES_TO_CHECK), ',' );
  }

  // With the proper predicate, rule is trivial
  @Override protected void visit(BaseNode root, RuleContext ctx) {
    check(root, PROTOTYPE_ASSIGN, ctx);
  }
}
