/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/7/14 10:06 PM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.optimyth.qaking.cobol.hla.ast.Paragraph;
import com.optimyth.qaking.cobol.hla.ast.ProcedureSection;
import com.optimyth.qaking.cobol.util.Procedures;
import com.optimyth.qaking.highlevelapi.ast.common.HLABehaviouralUnit;
import com.optimyth.qaking.highlevelapi.ast.common.HLACompilationUnit;
import com.optimyth.qaking.highlevelapi.ast.common.HLANode;

/**
 * DocumentProcedures - Sample rule that demands that each main procedure is properly commented.
 * <p/>
 * "Main procedure" is a procedural section or paragraph (when not in section).
 * "Properly commented" means to have code comments (and possibly that such comments
 * follow a certain layout).
 * <p/>
 * Please note that paragraphs layout seem to be more usual in North America, while sections layout
 * seem to be more usual in Europe, with a mix of preferences in other parts of the world.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 07-01-2014
 */
public class DocumentProcedures extends AbstractCobolRule {

  // Rule uses high-level AST and com.optimyth.qaking.cobol.util.Procedures utility
  // for getting the paragraphs and sections
  @Override protected void visit(BaseNode root, RuleContext ctx) {
    //Do not process if high level AST could not be built, rule logic is based on HLA
    if (HLANode.isNull(getHighLevelTree(root,ctx))) { return; }
    HLACompilationUnit cu = (HLACompilationUnit)getHighLevelTree(root, ctx);
    Procedures procedures = new Procedures(cu);
    
    for(Paragraph para : procedures.paragraphs()) {
      if(para.getSection() == null) {
        // A paragraph not in section, must have prepending comment properly describing the behaviour
        checkComment(para, ctx);
      }
    }
    
    for(ProcedureSection section : procedures.sections()) {
      // All procedural sections shoud have prepending comment properly describing the behaviour
      checkComment(section, ctx);
    }
  }

  private void checkComment(HLABehaviouralUnit unit, RuleContext ctx) {
    if( !hasProperContent(unit) ) {
      addViolation(violation(ctx, unit), ctx);
    }
  }

  private boolean hasProperContent(HLABehaviouralUnit unit) {
    String comment = unit.getCommentOn();
    // This accepts any non empty comment, but you may modify to check for a certain comment layout
    return comment != null;
  }

}
