/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 12/24/13 9:53 AM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.core.AbstractRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.optimyth.qaking.globalmodel.UseGlobalSymbolTable;
import com.optimyth.qaking.globalmodel.model.Type;
import com.optimyth.qaking.globalmodel.model.Variable;
import com.optimyth.qaking.globalmodel.query.InheritanceQuery;
import com.optimyth.qaking.globalmodel.query.InheritanceRow;
import com.optimyth.qaking.globalmodel.query.SymbolTable;
import com.optimyth.qaking.highlevelapi.dsl.Query;
import com.optimyth.qaking.java.hla.ast.JavaModifiers;
import com.optimyth.qaking.java.hla.ast.JavaType;
import com.optimyth.qaking.java.hla.ast.JavaVariable;
import es.als.util.LanguageConstants;

import static com.als.clases.JavaRuleUtils.report;
import static com.optimyth.qaking.highlevelapi.ast.HLAConstants.TYPE_ANONYMOUS_CLASS;
import static com.optimyth.qaking.highlevelapi.ast.HLAConstants.TYPE_CLASS;
import static com.optimyth.qaking.java.hla.JavaPredicates.isSerializable;

/**
 * SerializableWithVersionUid - Emit violation on any serializable class that do not have a private serialVersionUID field.
 * <p/>
 * Javadoc for java.io.Serializable states: "It is strongly recommended that all serializable classes 
 * explicitly declare serialVersionUID values, since the default serialVersionUID computation is highly sensitive 
 * to class details that may vary depending on compiler implementations, and can thus result in unexpected 
 * InvalidClassExceptions during deserialization. Therefore, to guarantee a consistent serialVersionUID value 
 * across different java compiler implementations, a serializable class must declare an 
 * explicit serialVersionUID value. It is also strongly advised that explicit serialVersionUID declarations 
 * use the private modifier where possible, since such declarations apply only to the immediately declaring class
 * --serialVersionUID fields are not useful as inherited members."
 * <p/>
 * This sample rule uses global symbol table, when available, to check for indirect inheritance on java.io.Serializable,
 * showing how to combine both local (for direct inheritance) and global analysis.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 24-12-2013
 */
@UseGlobalSymbolTable(technology = LanguageConstants.JAVA)
public class SerializableWithVersionUid extends AbstractRule {

  private static final String SERIAL_VERSION_UID = "serialVersionUID";
  
  private static final NodePredicate isClass = new NodePredicate() {
    public boolean is(BaseNode node) {
      if(node instanceof JavaType) {
        JavaType type = (JavaType)node;
        return TYPE_CLASS.equals(type.getKind()) || TYPE_ANONYMOUS_CLASS.equals(type.getKind());
      }
      return false;
    }
  };

  // Return true for JavaType that does NOT declare a private static final long serialVersionUID
  private static final NodePredicate noPrivateSerialVersionUID = new NodePredicate() {
    public boolean is(BaseNode node) {
      if(node instanceof JavaType) {
        JavaType type = (JavaType)node;
        for(JavaVariable field : type.getVariables()) {
          if(
            field.isPrivate() && field.isStatic() && field.isFinal() &&
            "long".equals(field.getType()) &&
            SERIAL_VERSION_UID.equals(field.getName())
          ) return false;
        }
      }
      return true;
    }
  };

  // Find direct serializable classes that do not provide a proper serialVersionUID
  Query serializableNoVersionUID = Query.query()
    .find(isSerializable).filter(isClass) // serializable class (abstract or not)
    .filter(noPrivateSerialVersionUID) // with no private static final long serialVersionUID
    .report();

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    // High-level AST is enough for this rule 
    serializableNoVersionUID.run(this, ctx, ctx.getHighLevelTree());
  }

  @Override public void postProcess(RuleContext ctx) {
    super.postProcess(ctx);

    SymbolTable table = SymbolTable.get(ctx);
    if(table != null) {
      // Check only indirect inheritance, not covered by direct 
      InheritanceQuery inh = new InheritanceQuery(table);
      for(InheritanceRow rel : inh.findInheritanceRows("supername = 'java.io.Serializable' AND level > 1")) {
        Variable field = table.findVariable(SERIAL_VERSION_UID, rel.getSubtype(), LanguageConstants.JAVA);
        if(field == null || !isProper(field)) {
          // No proper serialVersionUID, emit violation on the class declaration
          Type classWithoutVersionUID = table.getType(rel.getSubtypeId());
          report(this, classWithoutVersionUID, ctx);
        }
      }

    } else {
      // No symbol table, notify it
      getLogger().warn("No global symbol table, indirect subtypes from java.io.Serializable will be ignored");
    }
  }
  
  private static final int PRIV_STATIC_FINAL = JavaModifiers.parse("private static final");
  private boolean isProper(Variable field) {
    return
      JavaModifiers.all(field.getModifiers(), PRIV_STATIC_FINAL) &&
      "long".equals(field.getType());
  }
}
