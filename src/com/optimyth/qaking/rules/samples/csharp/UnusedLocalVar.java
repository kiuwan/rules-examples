/**
 * 
 */
package com.optimyth.qaking.rules.samples.csharp;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.RuleViolation;
import com.als.core.ast.BaseNode;
import com.google.common.base.Predicate;
import com.optimyth.csharp.symboltable.LocalSymbolTable;
import com.optimyth.csharp.symboltable.LocalSymbolTableBuilder;
import com.optimyth.csharp.symboltable.Symbol;
import com.optimyth.csharp.symboltable.SymbolKind;
import com.optimyth.csharp.utils.DetailAST;

/**
 * UnusedLocalVar - Find unused local variabless, using local symbol table.
 * 
 * @author <a href="mailto:jorge.para@optimyth.com">jpara</a>
 * @version 21/03/2015
 *
 */
public class UnusedLocalVar extends AbstractRule {

  //Predicate that matches variable symbols with no usages. It can be easily adapted to
  //match fields or methods.
  private static final Predicate<Symbol> localVarNotUsed = new Predicate<Symbol>() {
    public boolean apply(Symbol symbol) {
      return symbol.getKind() == SymbolKind.VARIABLE && !symbol.hasUsages();
    }    
  };
  
  @Override
  protected void visit(BaseNode root, RuleContext ctx) {
    LocalSymbolTable table = LocalSymbolTableBuilder.getSymbolTable((DetailAST)root);
    //Just find symbols matching predicate and report violations
    for (Symbol notUsed : table.findAll(localVarNotUsed)) {
      RuleViolation rv = createRuleViolation(ctx,notUsed.getNode().findLine(), getMessage());
      ctx.getReport().addRuleViolation(rv);
    }

  }
  
}
