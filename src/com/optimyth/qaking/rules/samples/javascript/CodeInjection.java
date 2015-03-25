/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/22/14 6:30 PM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.als.core.ast.TreeNode;
import com.als.js.rules.AbstractJavaScriptRule;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.optimyth.qaking.js.ast.JSNode;
import com.optimyth.qaking.js.symbols.LocalSymbolTable;
import com.optimyth.qaking.js.utils.ExpressionUtil;
import com.optimyth.qaking.js.utils.FunctionUtil;
import es.als.util.StringUtils;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Symbol;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.optimyth.qaking.js.utils.JavaScriptPredicates.*;

/**
 * CodeInjection - Sample rule that detects uses of functions that evaluate code, and may be
 * vulnerable to code injection attacks when the code evaluated could be modified from external input.
 * <p/>
 * Properties:
 * <ul>
 *   <li><em>check</em> - Comma-separated list of dangerous items to consider: eval, execScript, Function, setInterval or setTimeout</li>
 * </ul>
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 22-01-2014
 */
public class CodeInjection extends AbstractJavaScriptRule {
  
  private static final String DEFAULT_CHECKERS = "eval,execScript,Function,setInterval,setTimeout";
  private List<InsanityChecker> insanityCheckers = Collections.emptyList();
  private InsanityChecker functionChecker = null;

  // Configure sequence of InsanityChecker helpers to use, in the "check" property 
  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);

    Set<String> checks = StringUtils.asSet(getProperty("check", DEFAULT_CHECKERS), ',');
    insanityCheckers = Lists.newLinkedList();
    for(String check : checks) {
      InsanityChecker checker = CHECKERS.get(check);
      if(checker != null) {
        insanityCheckers.add(checker);
        if(checker==FUNCTION_CHECK) functionChecker = checker;
      } else {
        getLogger().warn("check for " + check + "not available. Ignored");
      }
    }    
  }

  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    TreeNode.on(root).accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        if(FUNCTION_CALL.is(node)) {
          JSNode call = (JSNode)node;
          for(InsanityChecker checker : insanityCheckers) {
            if(checker.match(call)) {
              reportViolation(ctx, call);
            }
          }
        } else if(NEW_EXPRESSION.is(node) && functionChecker != null) {
          JSNode newExpr = (JSNode)node;
           if(functionChecker.match(newExpr)) {
             reportViolation(ctx, newExpr);
           }
        }
      }
    });
  }
  
  // Checks for a call or additional 
  private static interface InsanityChecker {
    public boolean match(JSNode call);
  }
  
  private static abstract class BaseInsanityChecker implements InsanityChecker {
    public boolean match(JSNode call) {
      JSNode target = call.child(0);
      // Check direct call (e.g. eval() or execScript())
      if(IDENTIFIER.is(target)) {
        String fname = target.getImage();
        if(fname != null) {
          return isBadFunction(fname) && hasDangerousArgs(call);
        }
      }

      // check method call x.dangerousFunction(), when called on the global object (
      return
        target.isTypeName("PropertyGet") && target.getNumChildren() == 2 &&
        ExpressionUtil.isGlobalObject(target.child(0)) &&
        isBadFunction(target.child(1).getImage());
    }
    

    protected boolean hasDangerousArgs(JSNode call) {
      @SuppressWarnings("unchecked")
      JSNode injectionPoint = getInjectionPoint(call);
      if(injectionPoint.isNull()) return false;
      // A constant string is allowed
      if(STRING_LITERAL.is(injectionPoint)) return false; // a constant
      if(FUNCTION.is(injectionPoint)) return false; // function code, not a problem
      if(FUNCTION_CALL.is(injectionPoint)) return true; // code is given by a function call, consider "tainted" code

      // Perform a rather simple static analysis to check if the injection point
      // could be "tainted" with external input
      final LocalSymbolTable symTab = LocalSymbolTable.build(call.rootForTechnology());

      if(IDENTIFIER.is(injectionPoint)) {
        return checkSymbol(injectionPoint, symTab);
        
      } else {
        final AtomicBoolean hasVar = new AtomicBoolean(false);
        injectionPoint.accept(new NodeVisitor() {
          public void visit(BaseNode node) {
            if(hasVar.get()) return;
            if(IDENTIFIER.is(node)) {
              JSNode item = (JSNode)node;
              if(checkSymbol(item, symTab)) hasVar.set(true);
            }
          }
        });

        return hasVar.get();
      }
    }

    private boolean checkSymbol(JSNode item, LocalSymbolTable symTab) {
      Symbol symbol = symTab.getSymbol(item);
      if(symbol ==null) return true; // regarding security, it is better to be safe than to be sorry...
      if(symbol.getDeclType() == Token.FUNCTION) return false; // A function, allowed
      if(symbol.getDeclType() == Token.CONST) return false; // constant, allowed
      if(symbol.getDeclType() == Token.LP) return true; // a function parameter, considered external input for simplicity

      // TODO potential usages for target var could be analyzed here
      // for any var reaching injection point, consider that it could be tainted from external input (may produce false positives...)
      return true;
    }

    protected abstract boolean isBadFunction(String fname);
    // For most functions, first arg is the injection point for code
    protected JSNode getInjectionPoint(JSNode call) { return call.child(1); }
  }

  // eval()
  private static final InsanityChecker EVAL = new BaseInsanityChecker() {
    @Override protected boolean isBadFunction(String fname) { return "eval".equals(fname); }
  };

  // execScript()
  private static final InsanityChecker EXEC_SCRIPT = new BaseInsanityChecker() {
    @Override protected boolean isBadFunction(String fname) { return "execScript".equals(fname); }
  };

  // Function(..., functionBody)
  private static final InsanityChecker FUNCTION_CHECK = new BaseInsanityChecker() {
    @Override protected boolean isBadFunction(String fname) { return "Function".equals(fname); }

    // last arg is the injection point for code
    @Override protected JSNode getInjectionPoint(JSNode call) { return call.lastChild(); }
  };

  // setInterval()
  private static final InsanityChecker SET_INTERVAL = new BaseInsanityChecker() {
    @Override protected boolean isBadFunction(String fname) { return "setInterval".equals(fname); }
  };

  // setTimeout()
  private static final InsanityChecker SET_TIMEOUT = new BaseInsanityChecker() {
    @Override protected boolean isBadFunction(String fname) { return "setTimeout".equals(fname); }
  };

  // If necessary, register additional checkers here...
  private static final Map<String, InsanityChecker> CHECKERS = ImmutableMap.<String, InsanityChecker>builder()
    .put("eval", EVAL)
    .put("execScript", EXEC_SCRIPT)
    .put("Function", FUNCTION_CHECK)
    .put("setInterval", SET_INTERVAL)
    .put("setTimeout", SET_TIMEOUT)
    .build();  
}
