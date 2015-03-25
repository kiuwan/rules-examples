/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 2/18/14 12:59 PM
 */

package com.optimyth.qaking.rules.samples.cpp;

import com.als.core.RuleContext;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodePredicate;
import com.als.core.ast.NodePredicates;
import com.als.core.ast.TreeNode;
import com.optimyth.cpp.rules.AbstractCppRule;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowNavigator;
import com.optimyth.qaking.codeanalysis.controlflow.ControlFlowVisitor;
import com.optimyth.qaking.codeanalysis.controlflow.model.DataFlowNode;
import com.optimyth.qaking.codeanalysis.controlflow.model.IDataFlowNode;
import com.optimyth.qaking.codeanalysis.controlflow.model.IVarRef;
import com.optimyth.qaking.cpp.globana.TypedefResolver;
import com.optimyth.qaking.cpp.library.ErrorProcessingType;
import com.optimyth.qaking.cpp.library.FunctionDescriptor;
import com.optimyth.qaking.cpp.library.Libraries;
import com.optimyth.qaking.cpp.util.DeclarationUtil;
import com.optimyth.qaking.cpp.util.ExpressionUtil;
import com.optimyth.qaking.cpp.util.FunctionSignature;
import com.optimyth.qaking.cpp.util.FunctionUtil;
import com.optimyth.tags.Tags;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.optimyth.qaking.cpp.hla.primitives.CppPredicates.*;
import static com.optimyth.qaking.cpp.hla.primitives.CppPredicates.ARRAY_SUBSCRIPT;
import static com.optimyth.qaking.cpp.hla.primitives.CppPredicates.BOOLEAN_EXPR;

/**
 * CheckNullPointerDereference - Sample C rule showing how to use control-flow graph and library metadata
 * for detecting potential null pointer dereferences.
 * <p/>
 * Attempting to dereference a null pointer results in undefined behavior, typically abnormal program termination.
 * <p/>
 * This rule checks that any pointer to a memory area (typically as result from a function call like malloc() or fgets)
 * if checked for NULL before dereference, as a null pointer dereference have undefined behaviour. If pointer
 * is dereferenced without previous check against NULL (or zero), a violation is emitted on the call to the function
 * call that could return true.
 * <p/>
 * For functions that use errno to indicate error condition, a check against errno is considered an alternate
 * way to check for the validity of the pointer.
 * <p/>
 * In some situations and platforms, dereferencing a null pointer can lead to the execution of arbitrary code.
 * <p/>
 * NOTE: This rule is fairly complex. The standard provided <em>OPT.C.CERTC.EXP34</em> rule works similar to this sample.
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 18-02-2014
 */
public class CheckNullPointerDereference extends AbstractCppRule {

  private static final String TAG_PREFIX = "nullptr:";

  private Map<String, NullPointerFunction> nullptrFunctions;

  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);
    // load library metadata, as function behaviour with regard to null pointers in calls is needed
    loadMetadata(ctx);
  }

  /** Operate on calls on functions that return a potential null pointer */
  @Override protected void doVisit(BaseNode node, RuleContext ctx) {
    if(C_FUNCTION_CALL.is(node)) {
      String fname = FunctionUtil.getCFunctionCallSignature(node);
      NullPointerFunction npf = nullptrFunctions.get(fname);
      if(npf != null) {
        TreeNode call = TreeNode.on(node);
        if( isMemoryAccessedWithoutNullCheck(call, npf, ctx) ) {
          ctx.getReport().addRuleViolation( violation(ctx, call) );
        }
      }
    }
  }

  private boolean isMemoryAccessedWithoutNullCheck(TreeNode call, NullPointerFunction npf, RuleContext ctx) {
    if(isNullChecked(call)) return false; // Check done in same point of assignment, like if( (v=malloc(...)) == NULL ) ...

    // Parse the call to get info about the variable to check
    final NullCheckProblem problem = NullCheckProblem.compile(call, npf);
    if(problem == null) return false; // nothing could be said

    // Prepare flow graph
    final DataFlowNode start = getDataFlowNode(call, ctx);
    if(start==null) return false;

    final AtomicBoolean isNullChecked = new AtomicBoolean(false);
    final AtomicBoolean isMemoryAccessedWithoutNullCheck = new AtomicBoolean(false);
    final AtomicBoolean isPotentialNullReturned = new AtomicBoolean(false);

    // Forward DFA for checks on null candidate, and behaviour
    // (checked for null/errno, ptr returned without check, ptr dereferenced)
    analyzeNullCheck(problem, start, isNullChecked, isMemoryAccessedWithoutNullCheck, isPotentialNullReturned);

    if(!isNullChecked.get() && isPotentialNullReturned.get()) {
      // If function may return null without check, register this function as a NullPointerFunction
      NullPointerFunction current = NullPointerFunction.create(call.ancestor("function_definition"), -1);
      nullptrFunctions.put(current.getFunctionName(), current);
    }

    return isMemoryAccessedWithoutNullCheck.get(); // If true, violation !!!
  }

  /**
   * Traverse control-flow graph (CFG) forward from the "nullpointer" call
   * to check if target pointer is checked against null ('neutralized')
   * or it is dereferenced somewhere/somehow ('potential null pointer dereference').
   *
   * Note: no alias analysis done, ptr could be asigned to other pointer, etc.
   */
  private void analyzeNullCheck(final NullCheckProblem problem, final DataFlowNode start, final AtomicBoolean nullChecked, final AtomicBoolean memoryAccessedWithoutNullCheck, final AtomicBoolean potentialNullReturned) {
    ControlFlowNavigator.forwardBFT(start, new ControlFlowVisitor() {
      public boolean onDataFlowNode(IDataFlowNode node) {
        if (start == node) return true;
        // Find variable usages for target pointer variable
        for (IVarRef va : node.getVariableAccesses()) {
          if (problem.getVarDeclarator().equals(va.getDeclaration())) {
            if (va.isDefinition() || va.isUndefinition()) return false; // redefinition, terminate

            // analyze if check with NULL
            TreeNode unaryExp = TreeNode.on(va.getReference());
            if (isNullChecked(unaryExp)) {
              nullChecked.set(true);
              return false; // null-checked, terminate analysis

            } else if (isPtrDereference(unaryExp)) {
              // (dangerous) access to memory contents, terminate path analysis
              memoryAccessedWithoutNullCheck.set(true);
              return false;

            } else if (unaryExp.hasAncestor(RETURN)) {
              // potential null pointer returned, user function needs to be registered
              potentialNullReturned.set(true);
            }

          } else if (problem.getNullPointerFunction().isErrnoSupported() && "errno".equals(va.getVariableName())) {
            // errno check is allowed (but discouraged anyway in recent C versions)
            if (isErrnoChecked(TreeNode.on(va.getReference()))) {
              nullChecked.set(true);
              return false; // errno-checked, terminate analysis
            }
          }
        }

        return true; // go to next statement in CFG
      }

      private boolean isErrnoChecked(TreeNode unaryExp) {
        return unaryExp.hasAncestor(BOOLEAN_EXPR) || unaryExp.ancestor("expression").parent(SWITCH).isNotNull();
      }

      private final NodePredicate DEREF_OP = NodePredicates.or(INDIRECTION, PTR_MEMBER, C_FUNCTION_CALL);

      // ptr[] or *ptr or ptr->f are dereferences
      private boolean isPtrDereference(TreeNode unaryExp) {
        return unaryExp.has(ARRAY_SUBSCRIPT) || unaryExp.hasAncestor(DEREF_OP);
      }

    });
  }

  @Override public void postProcess(RuleContext ctx) {
    super.postProcess(ctx);
    nullptrFunctions = null;
  }

  private boolean isNullChecked(TreeNode unaryExp) {
    return unaryExp.hasAncestor(BOOLEAN_EXPR);
  }

  // Load API functions that could produce null (typically as return value or in modifiable pointer argument)
  // Such functions have tags element with nullptr:i where i is the argument position (-1 or 0..N-1)
  // where null exits the function
  private void loadMetadata(RuleContext ctx) {
    nullptrFunctions = createSynchronizedMap(30);
    Libraries libs = loadLibraries(ctx);
    for(FunctionDescriptor fd : libs.functions()) {
      Tags tags = fd.getTags();
      for(String v : tags.values()) {
        if(v.startsWith(TAG_PREFIX)) {
          String argPosStr = v.substring(TAG_PREFIX.length());
          int argPos = Integer.parseInt(argPosStr);
          NullPointerFunction npf = new NullPointerFunction(fd, argPos);
          nullptrFunctions.put(fd.getName(), npf);
        }
      }
    }
  }

  /**
   * Represents a function that could return null pointer.
   * argPos is the argument where null outputs from function (starting with 0, -1 means in return value).
   */
  private static final class NullPointerFunction {
    private final FunctionDescriptor functionDescriptor;
    private final int argPos;

    private NullPointerFunction(FunctionDescriptor functionDescriptor, int argPos) {
      this.functionDescriptor = functionDescriptor;
      this.argPos = argPos;
    }

    // Register a user function returning a pointer not checked against null as a NullPointerFunction
    private static NullPointerFunction create(TreeNode fdef, int argPos) {
      FunctionSignature signature = FunctionSignature.functionSignature(fdef, TypedefResolver.NULL);
      FunctionDescriptor fd = new FunctionDescriptor(signature, signature.toString(), "__custom__");
      fd.setTags("nullptr:-1");
      return new NullPointerFunction(fd, argPos);
    }

    public FunctionDescriptor getFunctionDescriptor() { return functionDescriptor; }
    public String getFunctionName() { return functionDescriptor.getName(); }
    public boolean isErrnoSupported() { return ErrorProcessingType.errno == functionDescriptor.getErrorProcessing(); }
    public int getArgPos() { return argPos; }
  }

  /**
   * A NULL check problem is defined by the call, the NullPointerFunction modelling
   * the function that need check for null, and the target var and its declaration.
   * If target is not a simple variable reference or its declaration cannot be found,
   * compile() returns null to indicate that call cannot be tested for NULL checks.
   */
  private static final class NullCheckProblem {
    private final NullPointerFunction nullPointerFunction; // descriptor for nullptr returning function
    private final TreeNode varDeclarator; // declaration of target var (candidate null pointer) to check

    private NullCheckProblem(NullPointerFunction nullPointerFunction, TreeNode varDeclarator) {
      this.nullPointerFunction = nullPointerFunction;
      this.varDeclarator = varDeclarator;
    }

    public NullPointerFunction getNullPointerFunction() { return nullPointerFunction; }
    public TreeNode getVarDeclarator() { return varDeclarator; }

    private static NullCheckProblem compile(TreeNode call, NullPointerFunction npf) {
      TreeNode exp = FunctionUtil.getExpression(call, npf.getArgPos());
      if(exp.isNull()) return null; // nothing could be said

      String varName = null;
      TreeNode varDeclarator = TreeNode.NULLTREE;

      if(exp.isTypeName("declarator")) {
        varName = exp.find("direct_declarator").findImage();
        varDeclarator = exp.topmostAncestor("declarator");

      } else {
        TreeNode ue = exp.find("unary_expression");
        if(ExpressionUtil.isSimpleVarReference(ue)) {
          varName = ExpressionUtil.getReferencedVariable(ue);
          BaseNode vd = DeclarationUtil.getVariableDeclaration(ue);
          if(vd != null) {
            varDeclarator = DeclarationUtil.findDeclarator(TreeNode.on(vd), varName).topmostAncestor("declarator");
          }
        }
      }

      // No LHS simple varname nor declaration found
      if(varName == null || varDeclarator == null || varDeclarator.isNull()) return null;

      return new NullCheckProblem(npf, varDeclarator);
    }
  }

}
