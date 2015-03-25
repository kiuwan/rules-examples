/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/20/14 8:16 PM
 */

package com.optimyth.qaking.rules.samples.javascript;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.js.rules.AbstractJavaScriptRule;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.optimyth.qaking.js.ast.JSNode;
import com.optimyth.qaking.js.symbols.LocalSymbolTable;
import com.optimyth.qaking.js.symbols.SymbolEntry;
import es.als.util.StringUtils;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * AvoidUndefUnusedVars - Avoid undef and unused vars.
 * <p/>
 * Sample rule to show how to use (local) symbol table for resolving declarations and usages of symbols
 * (functions, function parameters, variables, constants...)
 * <p/>
 * The rule shows also how to fetch a configuration property from a comment in source code,
 * to reduce the number of violations reported.
 * <p/>
 * You may feel free to change or adapt the rule as appropiate: 
 * check unused inner functions (functions defined inside other functions but not called),
 * check undef symbols when there is a symbol named the same but with different case,
 * etc.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 20-01-2014
 */
public class AvoidUndefUnusedVars extends AbstractJavaScriptRule {
  
  private boolean checkUnusedException = true;
  private boolean checkUnusedParameter = true;
  private boolean checkUnusedGlobal = true;
  private Set<String> knownGlobals = ImmutableSet.of();

  // Filter variables to check for no usages
  // Restrict to variable and parameter declarations, excluding exception names: catch(e) {} is allowed
  private final Predicate<Symbol> UNUSED = new Predicate<Symbol>() {
    @SuppressWarnings("RedundantIfStatement")
    public boolean apply(Symbol symbol) {
      AstNode target = (AstNode)symbol.getNode();
      if(!checkUnusedException && target != null && target.getParent() instanceof CatchClause) {
        return false;
      }

      int type = symbol.getDeclType();
      if(checkUnusedParameter && type == Token.LP) return true;
      if(type == Token.VAR || type == Token.CONST) {
        // check if globals should be ignored
        Scope scope = symbol.getContainingTable();
        if(!checkUnusedGlobal && scope != null && scope.getParentScope() == null) return false;
        return true;
      }
      return type == Token.LET;
    }
  };
  
  // Check only vars apparently global (no declaration in same source unit),
  // excluding known symbols (built-in or "standard" symbols) to avoid too many positives
  private final Predicate<SymbolEntry> UNDEF = new Predicate<SymbolEntry>() {
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean apply(SymbolEntry se) {
      if(knownGlobals.contains(se.getName())) return false;
      if(Character.isUpperCase( se.getName().charAt(0) )) return false; // ignore Class references
      return UNUSED.apply(se.getSymbol()) && !se.isKnownGlobalSymbol();
    }
  };

  /** Configure rule from its properties */
  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    this.checkUnusedException = getProperty("checkUnusedException", true);
    this.checkUnusedParameter = getProperty("checkUnusedParameter", true);
    this.checkUnusedGlobal = getProperty("checkUnusedGlobal", true);
    this.knownGlobals = StringUtils.asSet( getProperty("knownGlobals", ""), ',' );
  }

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    // Build symbol table for this source unit
    LocalSymbolTable symTable = LocalSymbolTable.build((JSNode)root);

    // Fetch configured globals in source code comment
    Set<String> configuredGlobals = getGlobals((JSNode) root);

    // Report unused vars
    List<SymbolEntry> unusedList = symTable.getUnusedSymbols(UNUSED, NodePredicate.TRUE);
    for(SymbolEntry unused : unusedList) {
      String msg = getMessage() + ": unused symbol " + unused.getSymbol().getName();
      reportViolation(ctx, unused.getDefinition(), msg);
    }

    // Report undefined vars used
    List<SymbolEntry> undefList = symTable.getGlobalSymbols(UNDEF);
    for(SymbolEntry undef : undefList) {
      if(configuredGlobals.contains( undef.getName() )) continue; // ignore a global registered in source comment
      String msg = getMessage() + ": undefined symbol " + undef.getSymbol().getName();

      for(JSNode undefUsage : undef.getUsages()) {
        reportViolation(ctx, undefUsage, msg);
      }
    }
  }

  /** Fetch configuration in source comments: <code>QAK globals: comma-separated list of globals</code> */
  private Set<String> getGlobals(JSNode root) {
    String config = getConfigurationDirective((AstRoot)root.get(), "globals");
    return config != null ? StringUtils.asSet(config, ',') : Collections.<String>emptySet();
  }

}
