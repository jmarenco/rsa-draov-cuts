package rsa;

public class Instance
{
	private boolean[][] A;
	private int[] d;
	private int[] s;
	private int[] t;
	private int n;
	private int m;
	private int p;
	
	public Instance(int vertices, int slots, int maxdemands)
	{
		m = 0;
		p = slots;
		n = vertices;
		A = new boolean[n][n];
		d = new int[maxdemands];
		s = new int[maxdemands];
		t = new int[maxdemands];
	}
	
	public void addArc(int i, int j)
	{
		if( i == j )
			throw new RuntimeException("The loop (" + i + ", " + j + ") is not allowed");
		
		A[i][j] = true;
	}
	
	public boolean isArc(int i, int j)
	{
		return A[i][j];
	}
	
	public void addDemand(int origin, int destination, int volume)
	{
		s[m] = origin;
		t[m] = destination;
		d[m] = volume;
		
		++m;
	}
	
	public int getVolume(int i)
	{
		if( i < 0 || i >= m )
			throw new RuntimeException("Unknown demand: " + i);
		
		return d[i];
	}
	
	public int getOrigin(int i)
	{
		if( i < 0 || i >= m )
			throw new RuntimeException("Unknown demand: " + i);
		
		return s[i];
	}
	
	
	public int getDestination(int i)
	{
		if( i < 0 || i >= m )
			throw new RuntimeException("Unknown demand: " + i);
		
		return t[i];
	}
	
	public int nodes()
	{
		return n;
	}
	
	public int demands()
	{
		return m;
	}
	
	public int slots()
	{
		return p;
	}
}
