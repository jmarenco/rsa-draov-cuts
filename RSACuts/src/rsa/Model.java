package rsa;

import java.util.ArrayList;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.google.ortools.linearsolver.MPSolver.ResultStatus;

public class Model
{
	private Instance _instance;
	private ArrayList<Clique> _cliques;
	
	private MPSolver _solver;
	private MPVariable[] l;
	private MPVariable[][][] y;
	private MPVariable[][] x;
	private MPVariable z;
	private MPVariable[][] w;

	private ResultStatus _status;
	private double _primal;
	private double _dual;

	private int _infinity = 10000;
	private int _timeLimit = 3600;
	private boolean _verbose = true;
	private boolean _showSolution = false;
	private boolean _singleCliques = false;
	private boolean _doubleCliques = false;
	private boolean _scipMagic = false;
	private boolean _linearRelaxation = false;
	
	public static enum Objective { MaxSlot, SumSlot, PathLength };
	private Objective _objective = Objective.MaxSlot;

	public Model(Instance instance, ArrayList<Clique> cliques)
	{
		_instance = instance;
		_cliques = cliques;
	}
	
	public void solve()
	{
		createSolver();
		createVariables();
		createFlowConstraints();
		createBindingConstraints();
		createDemandConstraints();
		createSingleCliqueInequalities();
		createDoubleCliqueInequalities();
		createObjective();
		solveModel();
		showSummary();
		closeSolver();
	}
	
	private void createSolver()
	{
	    _solver = MPSolver.createSolver("SCIP");

	    if( _solver == null )
	    	throw new RuntimeException("Solver is null!");
	    
	    if( _verbose == true )
			_solver.enableOutput();
	}
	
	private void createVariables()
	{
		l = new MPVariable[_instance.demands()];
		y = new MPVariable[_instance.demands()][_instance.nodes()][_instance.nodes()];
		x = new MPVariable[_instance.demands()][_instance.demands()];
		z = _solver.makeNumVar(0, _instance.slots(), "z");
		
		for(int d=0; d<_instance.demands(); ++d)
			l[d] = _solver.makeNumVar(0, _instance.slots() - _instance.getVolume(d), "l(" + d + ")");

		for(int d=0; d<_instance.demands(); ++d)
		for(int dp=0; dp<_instance.demands(); ++dp) if( d != dp )
		{
			if( _linearRelaxation == false )
				x[d][dp] = _solver.makeBoolVar("x(" + d + "," + dp + ")");
			else
				x[d][dp] = _solver.makeNumVar(0, 1, "x(" + d + "," + dp + ")");
		}
		
		for(int d=0; d<_instance.demands(); ++d)
		for(int i=0; i<_instance.nodes(); ++i)
		for(int j=0; j<_instance.nodes(); ++j) if( _instance.isArc(i, j) )
		{
			if( _linearRelaxation == false )
				y[d][i][j] = _solver.makeBoolVar("y(" + d + "," + i + "," + j + ")");
			else
				y[d][i][j] = _solver.makeNumVar(0, 1, "y(" + d + "," + i + "," + j + ")");
		}
		
		if( _objective == Objective.SumSlot )
		{
			w = new MPVariable[_instance.nodes()][_instance.nodes()];

			for(int i=0; i<_instance.nodes(); ++i)
			for(int j=0; j<_instance.nodes(); ++j) if( _instance.isArc(i, j) )
				w[i][j] = _solver.makeNumVar(0, _instance.slots(), "w(" + i + "," + j + ")");
		}
	}
	
	private void createFlowConstraints()
	{
		for(int i=0; i<_instance.nodes(); ++i)
		for(int d=0; d<_instance.demands(); ++d)
		{
			int rhs = i == _instance.getOrigin(d) ? -1 : (i == _instance.getDestination(d) ? 1 : 0);
			MPConstraint constr = _solver.makeConstraint(rhs, rhs);

			for(int j=0; j<_instance.nodes(); ++j)
			{
				if( _instance.isArc(j,i) )
					constr.setCoefficient(y[d][j][i], 1);

				if( _instance.isArc(i,j) )
					constr.setCoefficient(y[d][i][j], -1);
			}
		}
	}
	
	private void createBindingConstraints()
	{
		for(int d=0; d<_instance.demands(); ++d)
		for(int dp=d+1; dp<_instance.demands(); ++dp)
		for(int i=0; i<_instance.nodes(); ++i)
		for(int j=0; j<_instance.nodes(); ++j) if( _instance.isArc(i,j) )
		{
			MPConstraint constr = _solver.makeConstraint(-1, _infinity);
			constr.setCoefficient(x[d][dp], 1);
			constr.setCoefficient(x[dp][d], 1);
			constr.setCoefficient(y[d][i][j], -1);
			constr.setCoefficient(y[dp][i][j], -1);
		}
	}
	
	private void createDemandConstraints()
	{
		for(int d=0; d<_instance.demands(); ++d)
		for(int dp=0; dp<_instance.demands(); ++dp) if( d != dp )
		{
			MPConstraint constr = _solver.makeConstraint(_instance.getVolume(d) - _instance.slots(), _infinity);
			constr.setCoefficient(l[dp], 1);
			constr.setCoefficient(l[d], -1);
			constr.setCoefficient(x[d][dp], -_instance.slots());
		}
	}
	
	private void createSingleCliqueInequalities()
	{
		if( _singleCliques == false )
			return;
		
		for(Clique clique: _cliques)
		for(Integer d: clique)
		{
			MPConstraint constr = _solver.makeConstraint(0, _infinity);
			constr.setCoefficient(l[d], 1);
			
			for(Integer k: clique) if( d != k )
				constr.setCoefficient(x[k][d], -_instance.getVolume(k));
		}
	}
	
	private void createDoubleCliqueInequalities()
	{
		if( _doubleCliques == false )
			return;
		
		for(Clique clique: _cliques)
		for(Integer d: clique)
		for(Integer dp: clique) if( d != dp )
		{
			MPConstraint constr = _solver.makeConstraint(-_infinity, -_instance.getVolume(d));
			constr.setCoefficient(l[d], 1);
			constr.setCoefficient(l[dp], -1);
			constr.setCoefficient(x[dp][d], clique.stream().mapToDouble(k -> _instance.getVolume(k)).sum() - _instance.slots());
			
			for(Integer k: clique) if( d != k && dp != k )
			{
				constr.setCoefficient(x[d][k], _instance.getVolume(k));
				constr.setCoefficient(x[dp][k], -_instance.getVolume(k));
			}
		}
	}

	private void createObjective()
	{
		if( _objective == Objective.MaxSlot )
		{
			for(int d=0; d<_instance.demands(); ++d)
			{
				MPConstraint constr = _solver.makeConstraint(-_infinity, -_instance.getVolume(d));
				constr.setCoefficient(l[d], 1);
				constr.setCoefficient(z, -1);
			}
			
			MPObjective obj = _solver.objective();
			obj.setCoefficient(z, 1);
		}
		
		if( _objective == Objective.SumSlot )
		{
			for(int d=0; d<_instance.demands(); ++d)
			for(int i=0; i<_instance.nodes(); ++i)
			for(int j=0; j<_instance.nodes(); ++j) if( _instance.isArc(i, j) )
			{
				MPConstraint constr = _solver.makeConstraint(-_infinity, _instance.slots() - _instance.getVolume(d));
				constr.setCoefficient(l[d], 1);
				constr.setCoefficient(w[i][j], -1);
				constr.setCoefficient(y[d][i][j], _instance.slots());
			}
			
			MPObjective obj = _solver.objective();

			for(int i=0; i<_instance.nodes(); ++i)
			for(int j=0; j<_instance.nodes(); ++j) if( _instance.isArc(i, j) )
				obj.setCoefficient(w[i][j], 1);
		}
		
		if( _objective == Objective.PathLength )
		{
			MPObjective obj = _solver.objective();

			for(int d=0; d<_instance.demands(); ++d)
			for(int i=0; i<_instance.nodes(); ++i)
			for(int j=0; j<_instance.nodes(); ++j) if( _instance.isArc(i,j) )
				obj.setCoefficient(y[d][i][j], 1);
		}
	}
	
	private void solveModel()
	{
		_solver.setTimeLimit((int)(1000 * _timeLimit));
		
		if( _scipMagic == false )
		{
			_solver.setSolverSpecificParametersAsString("separating/maxcuts = 0");
			_solver.setSolverSpecificParametersAsString("separating/maxcutsroot = 0");
		}
		
		_status = _solver.solve();
		
		if( _status == ResultStatus.OPTIMAL || _status == ResultStatus.FEASIBLE )
		{
			_primal = _solver.objective().value();
			_dual = _status == ResultStatus.OPTIMAL ? _solver.objective().value() : _solver.objective().bestBound();
		}

		if( _showSolution == true )
		{
			System.out.println("Status: " + _status);
			
			if( _status == ResultStatus.OPTIMAL || _status == ResultStatus.FEASIBLE )
			{
				System.out.println("Obj: " + _solver.objective().value());
				System.out.println();

				for(int d=0; d<_instance.demands(); ++d)
				{
					System.out.println("Demand " + d + ", s = " + _instance.getOrigin(d) + ", t = " + _instance.getDestination(d) + ", v = " + _instance.getVolume(d));
					System.out.println();
					System.out.println("   " + l[d].name() + " = " + l[d].solutionValue());

					for(int i=0; i<_instance.nodes(); ++i)
					for(int j=0; j<_instance.nodes(); ++j) if( _instance.isArc(i,j) && y[d][i][j].solutionValue() > 0.01 )
						System.out.println("   " + y[d][i][j].name() + " = " + y[d][i][j].solutionValue());
					
					System.out.println();
				}
			}

			for(int i=0; i<_instance.nodes(); ++i)
			for(int j=0; j<_instance.nodes(); ++j) if( w != null && _instance.isArc(i,j) &&  w[i][j].solutionValue() > 0.01 )
				System.out.println("   " + w[i][j].name() + " = " + w[i][j].solutionValue());
	
			System.out.println();
		}
	}
	
	private static boolean _headerShown = false;

	private void showSummary()
	{
		if( _headerShown == false )
		{
			System.out.print("Vars | ");
			System.out.print("Cons | ");
			System.out.print("Status | ");
			System.out.print("LB | ");
			System.out.print("UB | ");
			System.out.print("Gap | ");
			System.out.print("Nodes | ");
			System.out.print("Time | ");
			System.out.print("Params ");
			System.out.println();
			
			_headerShown = true;
		}
		
		System.out.print(_solver.numVariables() + " | ");
		System.out.print(_solver.numConstraints() + " | ");
		System.out.print(_status + " | ");
		System.out.print(_primal + " | ");
		System.out.print(_dual + " | ");
		System.out.print((_primal > 0 ? (_dual - _primal) * 100 / _primal : 0) + " | ");
		System.out.print(_solver.nodes() + " | ");
		System.out.print(_solver.wallTime() + " | ");
		System.out.print(EntryPoint.argmap);
		System.out.println();
	}
	
	private void closeSolver()
	{
		_solver.clear();
	}

	public void setVerbose(boolean value)
	{
		_verbose = value;
	}

	public void setShowSolution(boolean value)
	{
		_showSolution = value;
	}
	
	public void setSingleCliques(boolean value)
	{
		_singleCliques = value;
	}
	
	public void setDoubleCliques(boolean value)
	{
		_doubleCliques = value;
	}
	
	public void setScipMagic(boolean value)
	{
		_scipMagic = value;
	}
	
	public void setLinearRelaxation(boolean value)
	{
		_linearRelaxation = value;
	}
	
	public void setTimeLimit(int value)
	{
		_timeLimit = value;
	}
	
	public void setObjective(Objective value)
	{
		_objective = value;
	}
	
	public ResultStatus getStatus()
	{
		return _status;
	}
}
