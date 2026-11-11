package rsa;

import java.util.ArrayList;
import java.util.Random;

public class Benchmark
{
	private static int _slots = 500;
	private static int _minVolume = 10;
	private static int _maxVolume = 30;
	private static int _bigVolume = 480;
	
	public static Pair twoCyclesWithBridge(int verticesInEachCycle, int demands, int seed)
	{
		Random random = new Random(seed);
		Instance ret = new Instance(2 * verticesInEachCycle, _slots, demands);
		
		for(int i=0; i<verticesInEachCycle-1; ++i)
		{
			ret.addArc(i, i + 1);
			ret.addArc(verticesInEachCycle + i, verticesInEachCycle + i + 1);
		}
		
		ret.addArc(verticesInEachCycle-1, 0);
		ret.addArc(2*verticesInEachCycle-1, verticesInEachCycle);
		ret.addArc(0, verticesInEachCycle);
		
		for(int i=0; i<demands; ++i)
		{
			int origin = random.nextInt(verticesInEachCycle);
			int destination = random.nextInt(verticesInEachCycle) + verticesInEachCycle;
			int volume = _minVolume + random.nextInt(_maxVolume - _minVolume + 1);
			
			ret.addDemand(origin, destination, volume);
		}
		
		return new Pair(ret, Clique.withAllDemands(ret));
	}
	
	public static Pair twoCyclesWithTwoCut(int verticesInEachComponent, int additionalEdges, int demands, int seed)
	{
		Random random = new Random(seed);
		Instance ret = new Instance(2 * verticesInEachComponent, _slots, demands);
		
		for(int i=0; i<verticesInEachComponent-1; ++i)
		{
			ret.addArc(i, i + 1);
			ret.addArc(verticesInEachComponent + i, verticesInEachComponent + i + 1);
		}
		
		for(int t=0; t<additionalEdges; ++t)
		{
			int i = random.nextInt(verticesInEachComponent);
			int j = random.nextInt(verticesInEachComponent);
			
			if( i != j )
			{
				ret.addArc(i, j);
				ret.addArc(verticesInEachComponent + i, verticesInEachComponent + j);
			}
		}
		
		ret.addArc(verticesInEachComponent-1, 0);
		ret.addArc(2*verticesInEachComponent-1, verticesInEachComponent);
		ret.addArc(0, verticesInEachComponent);
		ret.addArc(2, verticesInEachComponent + 2);
		
		Clique clique = new Clique();
		
		for(int i=0; i<demands; ++i)
		{
			int origin = random.nextInt(verticesInEachComponent);
			int destination = random.nextInt(verticesInEachComponent) + verticesInEachComponent;
			int volume = i == 0 ? _bigVolume : _minVolume + random.nextInt(_maxVolume - _minVolume + 1);
			
			ret.addDemand(origin, destination, volume);
			
			if( i > 0 && volume > _slots - _bigVolume )
				clique.add(i);
		}
		
		System.out.println("Clique size: " + clique.size());
		
		return new Pair(ret, clique);
	}
	
	public static void setSlots(int value)
	{
		_slots = value;
	}
	
	public static void setMinVolume(int value)
	{
		_minVolume = value;
	}
	
	public static void setMaxVolume(int value)
	{
		_maxVolume = value;
	}
	
	public static void setBigVolume(int value)
	{
		_bigVolume = value;
	}

	public static class Pair
	{
		private Instance _instance;
		private ArrayList<Clique> _cliques;
		
		public Pair(Instance instance, Clique... cliques)
		{
			_instance = instance;
			_cliques = new ArrayList<Clique>();
			
			for(int i=0; i<cliques.length; ++i)
				_cliques.add(cliques[i]);
		}
		
		public Instance getInstance()
		{
			return _instance;
		}
		
		public ArrayList<Clique> getCliques()
		{
			return _cliques;
		}
	}
}
