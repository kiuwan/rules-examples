/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/13/14 8:13 AM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.CobolAstUtil;
import com.als.cobol.rule.model.DataDescriptionEntry;
import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.als.core.util.StringUtil;
import com.optimyth.qaking.cobol.ast.CobolNode;
import com.optimyth.qaking.cobol.hla.ast.DataEntry;
import com.optimyth.qaking.cobol.util.Declarations;

import java.util.Collections;
import java.util.Set;

import static com.als.cobol.UtilCobol.COBOL_WORD;
import static com.als.cobol.UtilCobol.QUALIFIED_DATA_NAME;
import static com.als.core.ast.NodePredicates.type;

/**
 * OptimizeTableSubscript -  Checks if the access to the table elements is the most efficient. 
 * Types of any index variable on tables must be one of the types defined in binaryField property 
 * (BINARY, COMP, COMP-4 or COMP-4 by default); additionally, type must be CPU halfword for tables 
 * below 32000 entries, fullword if above 32000 entries.
 * <p/>
 * This rule shows how to check for data types in subscripted references to elements in Cobol tables,
 * via {@link Declarations}, {@link DataEntry} and {@link DataDescriptionEntry.CobolTable}.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 13-01-2014
 */
public class OptimizeTableSubscript extends AbstractCobolRule {

  private static final int TABLE_SIZE = 32000;
  private static final int DEFAULT_BYTES_FULL_WORD = 8; // CPU halfword below this
  private static final String PARAM_BINARYFIELD = "binaryField";
  private static final String PARAM_BYTES_FULL_WORD = "bytesFullWord";

  private Set<String> binaryField = Collections.emptySet();
  private int fullWordSize = DEFAULT_BYTES_FULL_WORD;
  private int halfWordMin = 1+(DEFAULT_BYTES_FULL_WORD/2);
  
  
  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    String binaryFieldStr = getProperty(PARAM_BINARYFIELD, "BINARY,COMP,COMP-4,COMP-5").toUpperCase();
    binaryField = StringUtil.asSet(binaryFieldStr, ',');
    fullWordSize = getProperty(PARAM_BYTES_FULL_WORD, DEFAULT_BYTES_FULL_WORD);
    halfWordMin = 1+(fullWordSize/2);    
  }

  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    CobolAstUtil.getProcedureDivision(root).accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        if(node.isTypeName("Subscript"))  {
          CobolNode subscript = (CobolNode)node;
          CobolNode tableNode = subscript.leftSibling(type(QUALIFIED_DATA_NAME));
          DataEntry de = Declarations.getDataEntry(tableNode);
          if(de == null) return;
          DataDescriptionEntry.CobolTable table = de.getTable();
          if(table == null) return;  // The subscript is not on a Cobol table resolved (possibly because defining COPY was not found)

          subscript = subscript.find(QUALIFIED_DATA_NAME); 
          if(subscript.isNotNull()) { // if subscript is a literal, nothing more to check
            checkTableSubscript(table, subscript, ctx);
          }
        }
      }
    });
    
  }

  private void checkTableSubscript(DataDescriptionEntry.CobolTable table, CobolNode subscript, RuleContext ctx) {
    // If the table defines the subscript as INDEX, it is OK
    String indexName = subscript.find(COBOL_WORD).getImage();
    if( table.getIndexFields().contains(indexName) ) return;

    DataEntry subscriptEntry = Declarations.getDataEntry(subscript);
    if(subscriptEntry == null) return; // subscript declaration ot found, typically because not present in the COPY
    String subscriptType = subscriptEntry.getType();
    if(subscriptType==null) return;
    if(!binaryField.contains(subscriptType.toUpperCase())) {
      // subscript definitely not of binary type
      addViolation(violation(ctx, subscript), ctx);
    
    } else {
      // check subscript var size (at most CPU halfword for tables below 32000 entries, fullword if above 32000 entries)
      int tableSize = table.getMaxCount();
      int indexSize = subscriptEntry.getSize();
      boolean improperSize = tableSize < TABLE_SIZE ? indexSize > halfWordMin : indexSize != fullWordSize;
      if(improperSize) {
        addViolation(violation(ctx, subscript), ctx);
      }
    }
  }
}
