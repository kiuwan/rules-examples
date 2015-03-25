/**
 * checKing - Scorecard for software development processes
 * [C] Optimyth Software Technologies, 2009
 * Created by: lrodriguez Date: 1/12/14 7:31 PM
 */

package com.optimyth.qaking.rules.samples.cobol;

import com.als.cobol.UtilCobol;
import com.als.cobol.rule.model.Call;
import com.als.cobol.rules.AbstractCobolRule;
import com.als.core.RuleContext;
import com.als.core.RuleViolation;
import com.als.core.ast.BaseNode;
import com.als.core.ast.NodeVisitor;
import com.als.core.util.Tuple3;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.optimyth.qaking.cobol.ast.CobolNode;
import com.optimyth.qaking.cobol.hla.ast.*;
import com.optimyth.qaking.cobol.util.Procedures;
import com.optimyth.qaking.highlevelapi.ast.common.HLABehaviouralUnit;
import com.optimyth.qaking.highlevelapi.ast.common.HLACompilationUnit;

import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FanInFanOutCheck - Sample rule for controlling excessive fan-in / fan-out in PERFORMed procedures 
 * (paragraphs / sections) and subprograms / subroutines (CALLs).
 * <p/>
 * This sample rule shows how to process "global" information (e.g. calls between Cobol programs)
 * by accumulating enough information to compute FAN-OUT and FAN-IN metrics (restricted to programs with
 * source code in the analyzed software).
 *
 * @author <a href="mailto:lrodriguez@optimyth.org">lrodriguez</a>
 * @version 12-01-2014
 */
public class FanInFanOutCheck extends AbstractCobolRule {
  
  private int maxProcedureFanin, maxProcedureFanout, maxCallFanin, maxCallFanout;
  private Calls calls;

  @Override public void initialize(RuleContext ctx) {
    super.initialize(ctx);

    this.maxProcedureFanin = getProperty("maxProcedureFanin", 30);
    this.maxProcedureFanout = getProperty("maxProcedureFanout", 30);
    this.maxCallFanin = getProperty("maxCallFanin", 30);
    this.maxCallFanout = getProperty("maxCallFanout", 30);

    // State needed to remember inter-program CALLs, so calls to programs not in analyzed software are ignored
    this.calls = new Calls();
  }

  @Override protected void visit(BaseNode root, RuleContext ctx) {
    if(!(root instanceof CobolNode)) return;
    CobolNode lla = (CobolNode)root;
    
    // Register program name for the current source file 
    // (so CALL to programs in analyzed software could be resolved)
    File sourceFile = ctx.getSourceCodeFilename();
    String programName = UtilCobol.getProgramName(lla, sourceFile).toUpperCase();
    calls.registerProgram(programName, sourceFile);
    
    // Use high-level AST to analyze "jump" statements (PERFORM, GO TO, CALL), and accumulate FANIN/FANOUT
    // Please note that PERFORM / GO TO are "local" calls to procedures in same source, while CALL are "global"
    // calls that need to be remembered
    HLACompilationUnit cu = (HLACompilationUnit)lla.getHighLevelNode();    
    ProcedureDivision pd = cu.find(ProcedureDivision.class);
    if(pd == null) return;

    // JumpVisitor traverses PROCEDURE DIVISION and accumulate calls for PERFORM
    JumpVisitor jumpVisitor = new JumpVisitor(pd, programName);
    pd.accept(jumpVisitor);
    
    jumpVisitor.reportExcessiveLocalCalls(ctx);
  }

  @Override public void postProcess(RuleContext ctx) {
    super.postProcess(ctx);
    calls.reportExcessiveCalls(ctx);
    calls = null; // Let state be garbage-collected
  }

  // Simple utilities for accumulating FANIN/FANOUT metric values
  
  private <T> int increment(T key, Map<T, AtomicInteger> counts) {
    return add(key, counts, 1);
  }

  private <T> int add(T key, Map<T, AtomicInteger> counts, int toAdd) {
    AtomicInteger count = counts.get(key);
    if(count==null) {
      count = new AtomicInteger(0);
      counts.put(key, count);
    }
    return count.addAndGet(toAdd);
  }  
  
  // Accumulate CALL data. After processing all input source files, FAN-IN/FAN-OUT metric values
  // for all programs in analyzed software could be computed (excluding CALLs to programs not in analyzed software)
  private class Calls {
    private Map<String, File> programsFound = Maps.newHashMapWithExpectedSize(64);
    private Map<String, AtomicInteger> fanin = Maps.newHashMapWithExpectedSize(64);
    private Map<String, AtomicInteger> fanout = Maps.newHashMapWithExpectedSize(64);
    private ListMultimap<String, String> pendingCalls = ArrayListMultimap.create(64, 3);
        
    // Register a source code file analyzed and program name for that source file
    public synchronized void registerProgram(String programName, File sourceFile) {
      programName = programName.toUpperCase(); // In cobol, identifiers are case-insensitive
      programsFound.put(programName, sourceFile); 
      
      // Resolve pending calls to current program, trying to avoid too much state data accumulated
      List<String> callers = pendingCalls.removeAll(programName);
      if(!callers.isEmpty()) {
        add(programName, fanin, callers.size());
        for(String caller : callers) {
          increment(caller, fanout);
        }
      }
    }
    
    // Register a call to the given program
    public synchronized void registerCall(String caller, String called) {       
      called = called.toUpperCase(); // In cobol, identifiers are case-insensitive
      
      if(programsFound.containsKey(called)) {
        // Both caller and called in analyzed software, 
        // increment FANIN(called) and FANOUT(caller)
        increment(called, fanin);
        increment(caller, fanout);
        
      } else {
        // Need to remember the call (called <- caller) for later resolution
        pendingCalls.put(called, caller);
      }
      
    }
    
    // Get the triplets {programName, sourceFile, integer} for all programs in analyzed software 
    // with outgoing (fanout) or incomming CALLs exceedint the threshold
    private Iterable<Tuple3<String, File, Integer>> getExcessiveItems(final Map<String, AtomicInteger> map, final int threshold) {
      return new Iterable<Tuple3<String, File, Integer>>() {
        public Iterator<Tuple3<String, File, Integer>> iterator() {
          return new AbstractIterator<Tuple3<String, File, Integer>>() {
            private Iterator<String> i = programsFound.keySet().iterator();
            private String next = findNext();
            
            @Override protected Tuple3<String, File, Integer> computeNext() {
              if(next == null) return endOfData();
              String curr = next;
              next = findNext();
              return new Tuple3<String, File, Integer>(curr, programsFound.get(curr), map.get(curr).intValue());
            }

            private String findNext() {
              while(i.hasNext()) {
                String candidate = i.next();                
                // Check if candidate program has metrics, and metric is exceeded
                if(map.containsKey(candidate) && map.get(candidate).intValue() > threshold) {
                  return candidate;
                }
              }
              return null;
            }            
          };
        }
      };
    }

    public void reportExcessiveCalls(RuleContext ctx) {
      for(Tuple3<String, File, Integer> e : getExcessiveItems(fanin, maxCallFanin)) {
        reportViolation(e.v1, e.v2, e.v3, maxCallFanin, "FAN-IN", ctx);
      }
      for(Tuple3<String, File, Integer> e : getExcessiveItems(fanout, maxCallFanout)) {
        reportViolation(e.v1, e.v2, e.v3, maxCallFanout, "FAN-OUT", ctx);
      }
    }
    
    private void reportViolation(String programName, File file, int value, int threshold, String metric, RuleContext ctx) {
      String msg = MessageFormat.format(
        "{0}: " + metric + "({1}) = {2} > max {3}",
        getMessage(), programName, value, threshold
      );
      RuleViolation rv = new RuleViolation(FanInFanOutCheck.this, -1, msg, file);
      addViolation(rv, ctx);
    }
  }

  // Accumulate local jump data (PERFORM and GOTO). After visitor processes PROCEDURE DIVISION,
  // all procedure (paragraph or procedure section) FAN-IN/FAN-OUT metric is available.
  private class JumpVisitor implements NodeVisitor {
    private final Procedures procedures;
    private final String caller;
    private final Map<HLABehaviouralUnit, AtomicInteger> proceduresFanout;
    private final Map<HLABehaviouralUnit, AtomicInteger> proceduresFanin;

    public JumpVisitor(ProcedureDivision procedureDivision, String caller) {
      this.procedures = new Procedures(procedureDivision);
      this.caller = caller;
      proceduresFanin = Maps.newHashMap();
      proceduresFanout = Maps.newHashMap();
    }
    
    public void visit(BaseNode stmt) {
      if (stmt instanceof PerformStatement && ((PerformStatement)stmt).hasCalledParagraphs()) {
        PerformStatement perform = (PerformStatement) stmt;
        // getPerformedUnits() resolve ALL procedures implicitely called, for example with PERFORM p1 THRU pN.
        List<HLABehaviouralUnit> calledUnits = perform.getPerformedUnits(procedures);
        //  FANOUT(from) += # of called procedures; FANIN(called) += 1
        faninFanout(perform, calledUnits);

      } else if (stmt instanceof GotoStatement) {
        GotoStatement gotoStmt = (GotoStatement)stmt;
        // getTargets() resolve ALL procedures that could be called with GO TO p1 ... pN DEPENDING ON varname
        List<HLABehaviouralUnit> calledUnits = gotoStmt.getTargets(procedures);
        //  FANOUT(from) += # of called procedures; FANIN(called) += 1
        faninFanout(gotoStmt, calledUnits);

      } else if (stmt instanceof CallStatement) {
        CallStatement call = (CallStatement)stmt;
        // Shows how to resolve the potential name(s) for static or dynamic CALL
        // Dynamic CALL means that program name is encoded in a data item.
        // Call.processAnyCall() tries to find such names by static analysis
        Call.processAnyCall(call.getNode(), new Call.OnCandidateCall<Calls>() {
          public void onCall(Call call, Calls calls) {
            String called = call.getProgram();
            calls.registerCall(caller, called);
          }
        }, calls);
      }
    }

    private void faninFanout(CobolStatement stmt, List<HLABehaviouralUnit> calledUnits) {
      HLABehaviouralUnit from = stmt.getProcedure(); // The containing procedure
      add(from, proceduresFanout, calledUnits.size());
      for(HLABehaviouralUnit called : calledUnits) {
        increment(called, proceduresFanin);
      }
    }

    // Report procedures (paragraphs or procedure sections) that have exceeding fan-in / fan-out metrics
    public void reportExcessiveLocalCalls(RuleContext ctx) {
      reportExcessiveLocalCalls(ctx, proceduresFanin, maxProcedureFanin, "FAN-IN");
      reportExcessiveLocalCalls(ctx, proceduresFanout, maxProcedureFanout, "FAN-OUT");
    }
    
    private void reportExcessiveLocalCalls(RuleContext ctx, Map<HLABehaviouralUnit, AtomicInteger> counts, int threshold, String metric) {
      for(Map.Entry<HLABehaviouralUnit, AtomicInteger> e : counts.entrySet()) {
        int value = e.getValue().intValue();
        if(value > threshold) {
          HLABehaviouralUnit procedure = e.getKey();
          String msg = MessageFormat.format(
            "{0}: " + metric + "({1}) = {2} > max {3}",
            getMessage(), procedure.getName(), value, threshold
          );
          addViolation(violation(ctx, procedure, procedure.getBeginLine(), msg), ctx);
        }
      }
    }
  }
}
