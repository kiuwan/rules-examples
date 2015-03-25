/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/13/14 2:07 PM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.optimyth.qaking.cobol.ast.CobolNode;
import com.optimyth.qaking.cobol.hla.ast.*;
import com.optimyth.qaking.cobol.util.DataReference;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowNavigator;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowVisitor;
import com.optimyth.qaking.codeanalysis.controlflow.model.DataFlowGraph;
import com.optimyth.qaking.codeanalysis.controlflow.model.DataFlowNode;
import com.optimyth.qaking.codeanalysis.controlflow.model.IDataFlowNode;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import static com.als.cobol.UtilCobol.PROCEDURE_DIVISION;

/**
 * UninitializedDataRead - Sample rule to show how to use control-flow graph to find illegal access to data entries.
 * <p/>
 * The algorithm shown is simple: For each PROCEDURE DIVISION, get its control-flow graph,
 * and traverse depth-first using ControlFlowNavigator.forwardDFT() with a visitor that
 * remembers data entries set up to that moment. When an statement operand is "read",
 * emits a violation if not previously set and not VALUE clause initializing the data entry.
 * <p/>
 * As usual, devil is in the details. Table references are considered OK if all entries below are initialized.
 * REDEFINES (aliases in cobol) also make checks a bit more complex.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 13-01-2014
 */
public class UninitializedDataRead extends AbstractCobolRule {

  // Ignore anything not in working-storage or local-storage (Screen, Report and Linkage sections are "initialized" externally)
  private static final Set<String> SECTIONS_TO_CHECK = ImmutableSet.of("WorkingStorageSection", "LocalStorageSection");

  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    if(!(root instanceof CobolNode)) return;
    CobolNode ast = (CobolNode)root;

    ast.accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        // It is not usual to have multiple program units (multiple PROCEDURE DIVISION),
        // but this will work even with multiple units
        if(node.isTypeName(PROCEDURE_DIVISION)) {
          CobolNode n = (CobolNode)node;
          ProcedureDivision pd = (ProcedureDivision) n.getHighLevelNode();
          DataFlowGraph<DataFlowNode> cfg = getFlow(pd, ctx);
          checkIllegalUsages(pd, cfg, ctx);
        }
      }
    });
  }

  /**
   * Perform a depth-first navigation on the control-flow, and for each statement or clause
   * with data item usages, check if the data item was previously set (when statement semantics
   * says that operand was written) or initialized by a VALUE clause in the data definition.
   */
  private void checkIllegalUsages(ProcedureDivision pd, DataFlowGraph<DataFlowNode> cfg, final RuleContext ctx) {
    final Set<DataEntry> initialized = Sets.newHashSetWithExpectedSize(128);
    
    // A common case is a table of constants (like messages), control such case
    markInitializedTables(pd, initialized);
        
    ControlFlowNavigator.forwardDFT(cfg, new ControlFlowVisitor() {
      public boolean onDataFlowNode(IDataFlowNode node) {
        BaseNode stmt = node.getAstNode();
        if(stmt instanceof HasDataReferences) {
          // When control-flow graph was built, all operands referencing a data entry were registered here
          Set<DataReference> refs = ((HasDataReferences) stmt).getDataReferences();
          if(refs==null) return true; // no refs

          for(DataReference ref : refs) {
            checkDataReference(stmt, ref, initialized, ctx);
          }
        }
        return true; // continue navigation
      }
    });
  }

  private void markInitializedTables(ProcedureDivision pd, final Set<DataEntry> initialized) {
    DataDivision dd = pd.getDataDivision();
    if(dd==null) return;
    dd.accept(new NodeVisitor() {
      public void visit(BaseNode node) {
        if(node instanceof DataEntry) {
          DataEntry curr = (DataEntry)node;
          if(curr.isRedefines()) {
            DataEntry target = curr.getRedefinesTarget();
            // If the target data entry is initialized, mark this entry and all its subfields as initialized
            if(initialized.contains(target)) {
              markAsInitialized(curr, initialized);
            }
          } else {
            List<DataEntry> subfields = curr.directSubfields();
            if(subfields.size()==0) return;
            boolean init = true;
            for(DataEntry child : curr.directSubfields()) {
              if(child.isConditionValue() || !child.hasValue()) {
                init = false; // indicator, or non initial value
                break;
              }
            }
            if(init) {
              initialized.add(curr);
            }
          }
        }
      }
    });
  }

  private void checkDataReference(BaseNode stmt, DataReference ref, final Set<DataEntry> initialized, RuleContext ctx) {
    DataEntry de = ref.getDataEntry();
    // If not in working-storage or local-storage, data is initialized externally, so ignore
    Section section = de.getSection();
    if(section == null || !SECTIONS_TO_CHECK.contains(section.getName())) return;
    if(ref.isDefinition()) {
      // data value set in the statement, assumed that it is properly initialized
      // (the data item and all of its children implicitely)
      de.accept(new NodeVisitor() {
        public void visit(BaseNode node) {
          if(node instanceof DataEntry) {
            markAsInitialized((DataEntry)node, initialized);
          }
        }
      });
    } else {
      // An usage, check if the data entry has VALUE (initialized) or was previously initialized
      if(!checkInitialized(de, initialized)) {
        String msg = MessageFormat.format("{0}: Data entry {1} read but not initialized", getMessage(), de.getName());
        addViolation(violation(ctx, stmt, msg), ctx);
      }
    }
  }

  // Register dataEntry and all successors as initialized, including REDEFINES aliased data
  private void markAsInitialized(DataEntry dataEntry, final Set<DataEntry> initialized) {
    dataEntry.accept(new NodeVisitor() {
      public void visit(BaseNode subfield) {
        if(subfield instanceof DataEntry) {
          initialized.add((DataEntry)subfield);
        }
      }
    });
    if(dataEntry.isRedefines()) {
      DataEntry target = dataEntry.getRedefinesTarget();
      if(target != null) {
        markAsInitialized(target, initialized);
      }
    }
  }

  private boolean checkInitialized(DataEntry de, Set<DataEntry> initialized) {
    // check if the data entry has VALUE (initialized) or was previously initialized
    if(de.getValueNode().isNotNull() || initialized.contains(de)) return true;
    // ... but could be a REDEFINES, check if the "alias" was initialized
    if(de.isRedefines()) {
      DataEntry target = de.getRedefinesTarget();
      if(target != null && initialized.contains(target)) return true;
    }
    return false;
  }
}
