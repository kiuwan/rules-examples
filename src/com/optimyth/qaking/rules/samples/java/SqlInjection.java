/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/13/14 1:44 PM
 */

package com.optimyth.qaking.rules.samples.java;

import com.als.core.RuleContext;
import com.google.common.base.Predicate;
import com.optimyth.qaking.codeanalysis.metadata.model.Libraries;
import com.optimyth.qaking.codeanalysis.metadata.tainting.SinkDef;
import com.optimyth.qaking.rules.AbstractJavaTaintingRule;

/**
 * SqlInjection -
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 13-02-2014
 */
public class SqlInjection extends AbstractJavaTaintingRule {

  @Override protected Predicate<SinkDef> getSinkPredicate(Libraries libs, RuleContext ctx) {
    return new Predicate<SinkDef>() {
      public boolean apply(SinkDef input) {
        return "sql-injection".equals( input.getKind() );
      }
    };
  }

}
