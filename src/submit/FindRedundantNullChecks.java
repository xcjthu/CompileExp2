package submit;

import java.util.*;
import flow.Flow;
import flow.FlowSolver;
import joeq.Class.jq_Class;
import joeq.Main.Helper;

public class FindRedundantNullChecks {

    /**
     * Main method of FindRedundantNullChecks.
     * This method should print out a list of quad ids of redundant null checks for each function.
     * The format should be "method_name id0 id1 id2", integers for each id separated by spaces.
     *
     * @param args an array of class names
     */
    public static void main(String[] args) {
        // TODO: Fill in this, like:
        Flow.Solver solver = new FlowSolver();
        
        // Flow.Analysis analysis = new NonNull(NonNull.LEVEL_NORMAL);

        jq_Class[] classes = new jq_Class[args.length];
        for (int i = 0; i < classes.length; i++)
            classes[i] = (jq_Class) Helper.load(args[i]);
        
            solver.registerAnalysis(new NullAnalysis());
        for (int i = 0; i < classes.length; i++) {
            Helper.runPass(classes[i], solver);
        }
        
    }
}
