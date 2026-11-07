package rsa;

import java.util.ArrayList;

public class Clique extends ArrayList<Integer>
{
	private static final long serialVersionUID = 1L;

	public static Clique withAllDemands(Instance instance)
	{
		Clique ret = new Clique();
		
		for(int d=0; d<instance.demands(); ++d)
			ret.add(d);
		
		return ret;
	}
}
