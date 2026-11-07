package rsa;

import com.google.ortools.Loader;

import rsa.Benchmark.Pair;

public class EntryPoint
{
	public static String version = "0.03";
	public static ArgMap argmap;
	
	public static void main(String[] args)
	{
	    Loader.loadNativeLibraries();
		argmap = new ArgMap(args);

		if( argmap.containsArg("-help") )
			showParameters();
		
		Benchmark.setSlots(argmap.intArg("-s", 500));
		Benchmark.setMinVolume(argmap.intArg("-minv", 10));
		Benchmark.setMaxVolume(argmap.intArg("-maxv", 30));
		Benchmark.setBigVolume(argmap.intArg("-bigv", 490));
		
		int n = argmap.intArg("-n", 15);
		int ae = argmap.intArg("-ae", 100);
		int d = argmap.intArg("-d", 15);
		int seed = argmap.intArg("-seed", 0);
		int time = argmap.intArg("-time", 3600);

		Pair pair = null;
		
		if( argmap.stringArg("-inst", "b").equals("b") )
			pair = Benchmark.twoCyclesWithBridge(n, d, seed);
		else
			pair = Benchmark.twoCyclesWithTwoCut(n, ae, d, seed);

		Model model = new Model(pair.getInstance(), pair.getCliques());
		model.setObjective(toObjective(argmap.stringArg("-obj", "max")));
		model.setLinearRelaxation(argmap.containsArg("-lr"));
		model.setTimeLimit(time);
		model.setVerbose(argmap.containsArg("-verbose"));
		model.setSingleCliques(argmap.containsArg("-clique"));
		model.setDoubleCliques(argmap.containsArg("-double"));
		model.setScipMagic(argmap.containsArg("-magic"));
		model.solve();
	}
	
	private static void showParameters()
	{
		System.out.println("RSA DR-AOV Cuts - v" + version);
		System.out.println("-inst [s]   Instance type (b|c)");
		System.out.println("-obj [s]    Objective function (max|sum|path)");
		System.out.println("-n [n]      Nodes in each component");
		System.out.println("-s [n]      Total slots");
		System.out.println("-d [n]      Total demands");
		System.out.println("-ae [n]     Additional edges");
		System.out.println("-minv [n]   Minimum volume");
		System.out.println("-maxv [n]   Maximum volume");
		System.out.println("-bigv [n]   Big volume");
		System.out.println("-seed [n]   Random seed");
		System.out.println("-time [n]   Time limit");
		System.out.println("-clique     Add clique inequalities");
		System.out.println("-double     Add double clique inequalities");
		System.out.println("-magic      Use SCIP magic");
		System.out.println("-lr         Solve linear relaxation");
		System.out.println("-verbose    Verbose mode");
	}
	
	private static Model.Objective toObjective(String obj)
	{
		if( obj.toLowerCase().trim().equals("max") )
			return Model.Objective.MaxSlot;
		
		if( obj.toLowerCase().trim().equals("sum") )
			return Model.Objective.SumSlot;

		if( obj.toLowerCase().trim().equals("path") )
			return Model.Objective.PathLength;
		
		throw new RuntimeException("Unknown objective: " + obj);
	}
}
