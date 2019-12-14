package submit;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.QuadIterator;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import flow.Flow;

import java.util.*;
/**
 * Class of reaching definitions analysis.
 */
public class NullAnalysis implements Flow.Analysis {
    private VarSet[] in;
    private VarSet[] out;

    private VarSet entry;
    private VarSet exit;

    private TransferFunction transferFunction = new TransferFunction();


    public void preprocess(ControlFlowGraph cfg) {
        System.out.print(cfg.getMethod().getName().toString()+" " );
        /* Generate initial conditions. */
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int x = qit.next().getID();
            if (x > max) max = x;
        }
        max += 1;
        in = new VarSet[max];
        out = new VarSet[max];
        qit = new QuadIterator(cfg);

        Set<String> s = new TreeSet<String>();
        VarSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            s.add("R"+i);
        }

        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }
        }

        entry = new VarSet();
        exit = new VarSet();
        transferFunction.value = new VarSet();
        for (int i=0; i<in.length; i++) {
            in[i] = new VarSet();
            out[i] = new VarSet();
        }
    }



    public void postprocess(ControlFlowGraph cfg) {
        QuadIterator qit = new QuadIterator(cfg);
        List<Integer> quadlist = new ArrayList<Integer>();
        while(qit.hasNext()){
            Quad qd = qit.next();
            if (qd.getOperator() == Operator.NullCheck.NULL_CHECK.INSTANCE){
                Flow.DataflowObject s = getIn(qd);
                for (Operand.RegisterOperand def : qd.getUsedRegisters()){
                    if (checkInc2(s,def.getRegister().toString())){
                        quadlist.add(qd.getID());
                    }
                }
            }
	    }
        java.util.Collections.sort(quadlist);
        int i;
        for (i=0;i<quadlist.size();i++){
            System.out.print(quadlist.get(i)+ " " + "gg");
        }
        System.out.println();		
    }

    public boolean isForward() { 
        return true; 
    }

    private Flow.DataflowObject getNewCopy(Flow.DataflowObject dataflowObject) {
        Flow.DataflowObject result = newTempVar();
        result.copy(dataflowObject);
        return result;
    }

    public Flow.DataflowObject getEntry() { 
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }
    

    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }


    public Flow.DataflowObject getIn(Quad quad) { 
        Flow.DataflowObject result = newTempVar();
        result.copy(in[quad.getID()]);
        return result;
    }
    
    public boolean checkInc (Flow.DataflowObject o, String s) {
        VarSet ov = (VarSet) o;
        return ov.contains(s);
    }
	 
    public boolean checkInc2 (Flow.DataflowObject o, String s) {
        VarSet ov = (VarSet) o;
        return ov.contains2(s);
    }

    /**
     * @param quad the quad to return the Out value of
     * @return a copy of the Out value of the given quad
     */
    public Flow.DataflowObject getOut(Quad quad) {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[quad.getID()]);
        return result;
    }

    public void setIn(Quad quad, Flow.DataflowObject newIn) { 
        in[quad.getID()].copy(newIn); 
    }

    public void setOut(Quad quad, Flow.DataflowObject newOut) { 
        out[quad.getID()].copy(newOut); 
    }

    public void setEntry(Flow.DataflowObject newEntry) { 
        entry.copy(newEntry); 
    }

    public void setExit(Flow.DataflowObject newExit) { 
        exit.copy(newExit); 
    }

    public Flow.DataflowObject newTempVar() { 
        return new VarSet(); 
    }

    public void processQuad(Quad quad) {
        transferFunction.value.copy(in[quad.getID()]);
        transferFunction.visitQuad(quad);
        out[quad.getID()].copy(transferFunction.value);
    }



    public static class VarSet implements Flow.DataflowObject {
        private Set<String> set;
	    private Set<String> nullChecked;

        public static Set<String> universalSet;
        public VarSet() { 
            set = new TreeSet<String>(); 
	        nullChecked = new TreeSet<String>();	
	    }

        public void setToTop() {
            set = new TreeSet<String>(); 
            nullChecked = new TreeSet<String>(universalSet);
	    }
    
        public void setToBottom() {
            set = new TreeSet<String>(universalSet); 
            nullChecked = new TreeSet<String>();
        }

	    public void addChecked(String s){
		    nullChecked.add(s);
	    }
        
        public void meetWith(Flow.DataflowObject o) {
            VarSet a = (VarSet)o;
            set.addAll(a.set);
	        nullChecked.retainAll(a.nullChecked);
        }

        public void copy(Flow.DataflowObject o) {
            VarSet a = (VarSet) o;
            set = new TreeSet<String>(a.set);
        	nullChecked = new TreeSet<String>(a.nullChecked);	
        }

        public boolean contains (String s){
            return set.contains(s);
        }

        public boolean contains2 (String s) {
            return nullChecked.contains(s);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VarSet) {
                VarSet a = (VarSet) o;
                return (set.equals(a.set)&&nullChecked.equals(a.nullChecked));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }
        
        @Override
        public String toString() {
            return set.toString();
        }



    	protected void add(String s) {
		    set.add(s);
		    nullChecked.remove(s);	
        }
        
	    protected void kill (Quad quad) {
            if (quad.getOperator() == Operator.NullCheck.NULL_CHECK.INSTANCE){
                Set<String> toRemove = new TreeSet<String>();		
                for (Operand.RegisterOperand def : quad.getUsedRegisters()){
                    toRemove.add(def.getRegister().toString());
                    //VarSet h = (VarSet)getIn(quad);
                    //h.addChecked(def.getRegister().toString());
                    //if(set.contains(def.getRegister().toString()))
                    nullChecked.add(def.getRegister().toString());
                }
    
			    set.removeAll(toRemove);
            }
	    }
    }


    /**
     * The QuadVisitor that performs the computation of the new Out
     * definition set of a quad
     */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        VarSet value;

        public TransferFunction() {}

        /**
         * Visits a quad and calculates the new Out value
         * @param quad the quad to visit
         */
        @Override
        public void visitQuad(Quad quad) {
            // first iterate over the quad's defined registers and kill
            // definitions
            for (Operand.RegisterOperand def : quad.getDefinedRegisters()) {
                value.add(def.getRegister().toString());
            }
	        if (!quad.getUsedRegisters().isEmpty())
		    value.kill(quad);
        }
    }
}