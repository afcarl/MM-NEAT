package edu.utexas.cs.nn.tasks.microrts.fitness;

import edu.utexas.cs.nn.util.datastructures.Pair;
import micro.rts.GameState;
import micro.rts.PhysicalGameState;

public abstract class RTSFitnessFunction {
	
	protected final int MAXCYCLES = 5000;
	protected final int RESULTRANGE = 2;
	protected PhysicalGameState pgs = null;
	
	/**
	 * judges an individual agent's fitness
	 * @param gs
	 * @return fitness of an organism
	 */
	public abstract Pair<double[], double[]> getFitness(GameState gs);
	
	public void givePhysicalGameState(PhysicalGameState pgs){
		this.pgs = pgs;
	}
}
