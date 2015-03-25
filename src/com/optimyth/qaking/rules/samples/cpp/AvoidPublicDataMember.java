/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/18/14 12:06 PM
 */

package com.optimyth.qaking.rules.samples.cpp;

import com.als.core.RuleContext;
import com.optimyth.cpp.rules.CppXPathRule;

/**
 * AvoidPublicDataMember - Avoid class data members that are public, because they can be changed from anywhere.
 * <p/>
 * C++ data members should have accessor methods that control "read / write" access to class data.
 * This rule enforces information hiding, a cornerstone in object-oriented technology.
 * <p/>
 * Rule is trivial, implemented with XPath expression (on high-level AST) looking for class fields that are
 * public and non-constant (public constants cannot be modified, do not need same encapsulation
 * level, and are allowed).
 * <p/>
 * Fields in Struct / Union do not apply, and you may simply remove the "Class" path in the XPath
 * to report on public fields in any C/C++ aggregate. But in the case of C, all fields in struct/union
 * are public, and no "getter/setter" could be added, so this rule has no sense.
 * <p/>
 * Note: This class is provided as example, but you do not need to subclass CppXPathRule;
 * in the XML descriptor you may instantiate CppXpathRule directly and register the 'xpath' property.
 * <p/>
 * As an exercice, try to find alternate XPath operating on low-level tree (much more complex !),
 * and explore other implementations based on Query API, predicates, etc.
 * <p/>
 * NOTE: A similar standard rule <em>OPT.CPP.AvoidPublicDataMember</em> is provided.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 18-02-2014
 */
public class AvoidPublicDataMember extends CppXPathRule {

  // XPath: go to high-level tree, then search for any field (as class child) been public and non-constant.
  @Override public void initialize(RuleContext ctx) {
    addProperty(XPATH_PROP, "qak:hla()//Class/Field[@Public='true'][@Constant='false']");
    super.initialize(ctx);
  }
}
