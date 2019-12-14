package submit;

import flow.Flow;
import java.util.Iterator;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadIterator;

public class ReferenceSolver implements Flow.Solver {

    private Flow.Analysis analyzer;

    public void registerAnalysis(Flow.Analysis analyzer) {
        this.analyzer = analyzer;
    }

    public void visitCFG(ControlFlowGraph cfg) {
        analyzer.preprocess(cfg);

        QuadIterator quadIterator = new QuadIterator(cfg);
        while (quadIterator.hasNext()) {
            Quad quad = quadIterator.next();

            Flow.DataflowObject topValue = analyzer.newTempVar();
            topValue.setToTop();

            if (analyzer.isForward()) {
                analyzer.setOut(quad, topValue);
            } else {
                analyzer.setIn(quad, topValue);
            }
        }

        boolean changed = true;
        boolean nodeChanged;
        while (changed) {
            changed = false;
            quadIterator = new QuadIterator(cfg, analyzer.isForward());
            while (analyzer.isForward() && quadIterator.hasNext() || !analyzer.isForward() && quadIterator.hasPrevious()) {
                if (processQuad(quadIterator)) {
                    changed = true;
                }
            }
        }

        if (analyzer.isForward()) {
            calculateExit(cfg);
        } else {
            calculateEntry(cfg);
        }

        analyzer.postprocess(cfg);
    }

    private boolean processQuad(QuadIterator quadIterator) {
        Quad quad;
        if (analyzer.isForward())
            quad =  quadIterator.next();
        else
            quad = quadIterator.previous();

        Flow.DataflowObject quadIn = analyzer.getIn(quad);
        Flow.DataflowObject quadOut = analyzer.getOut(quad);

        if (analyzer.isForward()) {
            quadIn.setToTop();
            meetAllPredecessors(quadIn, quadIterator);
            analyzer.setIn(quad, quadIn);
        } else {
            quadOut.setToTop();
            meetAllSuccessors(quadOut, quadIterator);
            analyzer.setOut(quad, quadOut);
        }

        //System.out.println("In: " + analyzer.getIn(quad).toString();)
        //System.out.println("Out: " + analyzer.getOut(quad).toString());

        // process the quad
        analyzer.processQuad(quad);


        // check if the node's value changed and return the flag
        Flow.DataflowObject newValue;
        if (analyzer.isForward()) {
            newValue = analyzer.getOut(quad);
            return !newValue.equals(quadOut);
        } else {
            newValue = analyzer.getIn(quad);
            return !newValue.equals(quadIn);
        }
    }

    private void meetAllPredecessors(Flow.DataflowObject quadIn, QuadIterator quadIterator) {
        // get all predecessors
        Iterator<Quad> iterator = quadIterator.predecessors();
        // meet with all the predecessors' Out dataflow objects
        Quad quad;
        while (iterator.hasNext()) {
            quad = iterator.next();
            // use the entry value where appropriate
            if (quad == null) {
                quadIn.meetWith(analyzer.getEntry());
            } else {
                quadIn.meetWith(analyzer.getOut(quad));
            }
        }
    }

    private void meetAllSuccessors(Flow.DataflowObject quadOut, QuadIterator quadIterator) {
        // get all successors
        Iterator<Quad> iterator = quadIterator.successors();
        // meet with all the successors' In dataflow objects
        Quad quad;
        while (iterator.hasNext()) {
            quad = iterator.next();
            // use the exit value where appropriate
            if (quad == null) {
                quadOut.meetWith(analyzer.getExit());
            } else {
                quadOut.meetWith(analyzer.getIn(quad));
            }
        }
    }

    private void calculateExit(ControlFlowGraph cfg) {
        Flow.DataflowObject newExit = analyzer.newTempVar();
        newExit.setToTop();
        QuadIterator quadIterator = new QuadIterator(cfg);
        while (quadIterator.hasNext()) {
            Quad quad = quadIterator.next();
            if (isExitPredecessor(quadIterator)) // meet with its Out value
            {
                newExit.meetWith(analyzer.getOut(quad));
            }
        }
        analyzer.setExit(newExit);
    }


    private void calculateEntry(ControlFlowGraph cfg) {
        Flow.DataflowObject newEntry = analyzer.newTempVar();
        newEntry.setToTop();
        QuadIterator quadIterator = new QuadIterator(cfg);
        while (quadIterator.hasNext()) {
            Quad quad = quadIterator.next();
            if (isEntrySuccessor(quadIterator)) // meet with its In value
            {
                newEntry.meetWith(analyzer.getIn(quad));
            }
        }
        analyzer.setEntry(newEntry);
    }

    private boolean isExitPredecessor(QuadIterator quadIterator) {
        return quadIterator.successors1().contains(null);
    }

    private boolean isEntrySuccessor(QuadIterator quadIterator) {
        return quadIterator.predecessors1().contains(null);
    }
}
