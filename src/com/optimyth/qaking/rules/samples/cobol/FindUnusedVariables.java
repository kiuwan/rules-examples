/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 10/2/13 10:04 PM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.google.common.collect.ImmutableSet;
import com.optimyth.qaking.cobol.hla.ast.DataEntry;
import com.optimyth.qaking.cobol.hla.ast.ProgramUnit;
import com.optimyth.qaking.cobol.hla.primitives.Usages;
import com.optimyth.qaking.highlevelapi.ast.common.HLACompilationUnit;
import es.als.util.StringUtils;

import java.util.Set;

import static com.als.core.ast.NodePredicates.type;
import static com.optimyth.qaking.highlevelapi.dsl.Query.query;
import static com.optimyth.qaking.highlevelapi.navigation.Region.SUCCESSORS;

/**
 * FindUnusedVariables - Sample rule that shows how a relatively complex rule could be implemented with few code.
 * <p/>
 * This rule finds all top-level data entries (with levels like 01, 77, 78...) that are not used.
 * For 01 (structured) entries, any usage of a subfield is considered an usage of the top-level data entry.
 * 01 records for files used are considered as well, as well as variables referenced in EXEC statements.
 * <p/>
 * Reported data entries are candidates for removal, making the program more .
 * <p/>
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 02-10-2013
 */
public class FindUnusedVariables extends AbstractCobolRule {
  private Set<String> toIgnore = ImmutableSet.of();
  
  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    toIgnore = StringUtils.asSet(getProperty("toIgnore", "").toUpperCase(), ',');
  }

  @Override protected void visit(BaseNode root, RuleContext ctx) {

    // Copies should be ignored, as they are frequently included in programs
    // and do not need to have usages of data they define.
    // For simplicity, we exclude Cobol files without a ProgramUnit
    HLACompilationUnit cu = ctx.getHighLevelTree();
    if(!cu.has(ProgramUnit.class)) return;

    // Data entries processing (could be hierarchical in Cobol) done here
    // All fields and subfields in DATA DIVISION are processed
    Usages topLevelUsages = new Usages(ctx);

    // Search for data entry usages (direct or indirect) in any PROCEDURE DIVISION
    query()
      .find(type("ProcedureDivision"))
      .visit(topLevelUsages.getUsagesVisitor(), SUCCESSORS)
      .run(this, ctx, root);

    // Report violations on unused data, ignoring registered data names
    for(DataEntry unused : topLevelUsages.unused()) {
      String dataname = unused.getName().toUpperCase(); // cobol is case insensitive, remember...
      if(toIgnore.contains(dataname)) continue; // Skip if one of those data entries to ignore
      // Report violation
      String msg = getMessage() + ": Unused top-level data " + dataname;
      addViolation(violation(ctx, unused, unused.getBeginLine(), msg), ctx);
    }
  }
}
