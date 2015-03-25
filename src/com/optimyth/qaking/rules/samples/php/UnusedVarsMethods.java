/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/10/14 12:12 AM
 */

package com.optimyth.qaking.rules.samples.php;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.TreeNode;
import com.optimyth.qaking.php.ast.PhpNode;
import com.optimyth.qaking.php.rules.AbstractPhpRule;
import com.optimyth.qaking.php.symboltable.*;
import com.optimyth.qaking.php.util.ClassUtil;

import java.text.MessageFormat;

/**
 * UnusedVarsMethods - Sample rule showing how to use local symbol table.
 * <p/>
 * Detects unused parameters, and private fields and methods in classes.
 * Please note that local symbol table registers all usages on symbols present in current source file.
 * In this case global analysis is not necessary, as unused function parameters, or unused private class members
 * cannot be used outside of the declaring function or class, respectively.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 10-02-2014
 */
public class UnusedVarsMethods extends AbstractPhpRule {

  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    if(!(root instanceof PhpNode)) return;

    LocalSymbolTable symtab = LocalSymbolTableBuilder.getSymbolTable((PhpNode)root);
    symtab.visitForward(new Visitor() {
      public boolean onSymbol(Symbol symbol) {
        // ignore symbols with usages or global
        if(symbol.hasUsages() || symbol.isMagicConstant()) return true;
        
        if(symbol.getKind()== SymbolKind.PARAMETER) {
          // Check that the parameter symbol is in a function with body.
          // Interface methods and abstract methods do not use their parameters
          if(hasBody(symbol.getNode())) {
            report(symbol, "{0}: unused parameter {1}", ctx);
          }

        } else if(isPrivateField(symbol)) {
          report(symbol, "{0}: unused private field {1}", ctx);

        } else if(isPrivateMethod(symbol)) {
          report(symbol, "{0}: unused private method {1}()", ctx);
        }

        return true;
      }
    });
  }

  private boolean hasBody(PhpNode param) {
    return param.ancestor("ParameterList").parent().hasChildren("CompoundStatement");
  }

  public boolean isPrivateField(Symbol symbol) {
    PhpNode n = symbol.getNode();
    if(n == null) return false;
    PhpNode attrs = n.ancestor("MemberVariablesDeclaration").child("MemberVariableAttributes");
    return attrs.isNotNull() && ClassUtil.isPrivate(TreeNode.on(attrs));
  }


  private boolean isPrivateMethod(Symbol symbol) {
    PhpNode n = symbol.getNode();
    if(n == null) return false;
    PhpNode attrs = n.child("MemberFunctionAttributes");
    return attrs.isNotNull() && ClassUtil.isPrivate(TreeNode.on(attrs));
  }
  
  private void report(Symbol symbol, String format, RuleContext ctx) {
    String msg = MessageFormat.format(format, getMessage(), symbol.getName());
    reportViolation(ctx, symbol.getNode(), msg);
  }
}
