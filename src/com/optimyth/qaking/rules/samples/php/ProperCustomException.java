package com.optimyth.qaking.rules.samples.php;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.core.ast.NodeVisitor;
import com.als.core.ast.TreeNode;
import com.optimyth.qaking.php.ast.PhpNode;
import com.optimyth.qaking.php.rules.AbstractPhpRule;
import com.optimyth.qaking.php.util.ClassUtil;
import com.optimyth.qaking.php.util.PhpPredicates;
import es.als.util.StringUtils;

import static com.optimyth.qaking.php.util.FunctionUtil.getFunctionName;
import static com.optimyth.qaking.php.util.PhpPredicates.*;


/**
 * ProperCustomException - Sample rule that demand for custom Exceptions to call parent constructor
 * and implement __toString().
 * <p/>
 * Rule shows how to operate with low-level AST (PhpNode, which is a NavigableNode with many search facilities).
 * Common utilities are also showed: Looking for superclass is easy with ClassUtil.getSuperclass(clazz),
 * and checking for method calls with PhpPredicates.methodCallsPred.
 * <p/>
 * Note: A similar standard rule com.optimyth.qaking.php.rules.reliability.ExceptionExtension exists.

 * @author lrodriguez
 */
public class ProperCustomException extends AbstractPhpRule {

  protected void visit(BaseNode root, final RuleContext ctx) {
    // Report violation on class extending exception ("custom exception"),
    // without constructor calling superclass' constructor
    // or with no __toString() method
    TreeNode.on(root).accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        if (CLASS_DECL.is(node) && extendsFromException((PhpNode) node)) {
          PhpNode clazz = (PhpNode) node;
          if (!checkConstructor(clazz) || !checkToString(clazz)) {
            reportViolation(ctx, node);
          }
        }
      }
    });
  }

  // True if superclass with name ending with "exception"
  private boolean extendsFromException(PhpNode clazz) {
    String superClass = ClassUtil.getSuperclass(clazz);
    return StringUtils.hasText(superClass) && superClass.toLowerCase().endsWith("exception");
  }

  // Return true if clazz has a constructor and calls parent class constructor (either old or new syntax)
  private boolean checkConstructor(PhpNode clazz) {
    PhpNode cons = clazz.find(constructorPred);
    final String superClazz = ClassUtil.getSuperclass(clazz);

    //noinspection SimplifiableIfStatement
    if (cons.isNull() || !cons.has(STATEMENT_BLOCK)) return false; // no constructor means violation

    // Find a method call to parent::__construct (PHP5) or old syntax (PHP4) parent::ParentClassName
    return cons.child(STATEMENT_BLOCK).has(new NodePredicate() {
      public boolean is(BaseNode node) {
        if(PhpPredicates.methodCallsPred.is(node)) {
          PhpNode pexp = (PhpNode)node;
          BaseNode leftNode = pexp.child(0).findLeftMost();
          String left = leftNode.getImage();
          BaseNode methodNode = pexp.child(1).findLeftMost(); 
          String methodName = methodNode.getImage();
          return "parent".equals(left) && ("__construct".equals(methodName) || methodName.equals(superClazz));
        }
        return false;
      }
    });
  }

  // Check if __toString() method is provided
  private boolean checkToString(PhpNode clazz) {
    return clazz.has(new NodePredicate() {
      public boolean is(BaseNode function) {
        return FUNCTION_DECL.is(function) && "__toString".equals(getFunctionName(function));
      }
    });
  }

}
