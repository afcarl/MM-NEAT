package edu.southwestern.evolution.genotypes;

import java.util.List;

import edu.southwestern.networks.TWEANN;
import edu.southwestern.networks.dl4j.DL4JNetworkWrapper;
import edu.southwestern.networks.dl4j.TensorNetworkFromHyperNEATSpecification;
import edu.southwestern.networks.hyperneat.HyperNEATTask;
import edu.southwestern.networks.hyperneat.HyperNEATUtil;
import edu.southwestern.networks.hyperneat.Substrate;

/**
 * Most of this class is just a facade for the HyperNEATCPPNGenotype
 * class, but the phenotype network it returns is a DL4J network instead
 * of a TWEANN.
 * 
 * @author Jacob Schrum
 */
public class HyperNEATCPPNforDL4JGenotype implements NetworkGenotype<DL4JNetworkWrapper> {

	// Original CPPN format using a TWEANN
	HyperNEATCPPNGenotype cppn;
	
	public HyperNEATCPPNforDL4JGenotype() {
		this(new HyperNEATCPPNGenotype());
	}

	public HyperNEATCPPNforDL4JGenotype(HyperNEATCPPNGenotype cppn) {
		// Store original type of CPPN within the class
		this.cppn = cppn;
	}
	
	public HyperNEATCPPNGenotype getCPPN() {
		return cppn;
	}
	
	@Override
	public void addParent(long id) {
		cppn.addParent(id);
	}

	@Override
	public List<Long> getParentIDs() {
		return cppn.getParentIDs();
	}

	@Override
	public Genotype<DL4JNetworkWrapper> copy() {
		return new HyperNEATCPPNforDL4JGenotype((HyperNEATCPPNGenotype) cppn.copy());
	}

	@Override
	public void mutate() {
		cppn.mutate();
	}

	@Override
	public Genotype<DL4JNetworkWrapper> crossover(Genotype<DL4JNetworkWrapper> g) {
		// Do standard TWEANN crossover on the internal CPPNs
		Genotype<TWEANN> result = cppn.crossover(((HyperNEATCPPNforDL4JGenotype) g).cppn);
		// Then wrap in the correct genotype and return
		return new HyperNEATCPPNforDL4JGenotype((HyperNEATCPPNGenotype) result);
	}

	@Override
	public DL4JNetworkWrapper getPhenotype() {
		// Create a Tensor network based on the HyperNEAT substrate specification
		HyperNEATTask hnt = HyperNEATUtil.getHyperNEATTask();		
		TensorNetworkFromHyperNEATSpecification tensorNetwork = new TensorNetworkFromHyperNEATSpecification(hnt);
        // Network generated by CPPN
        TWEANNGenotype substrateGenotype = cppn.getSubstrateGenotype(hnt);
        // DL4J network weights replaced with weights from HyperNEAT network
        tensorNetwork.fillWeightsFromHyperNEATNetwork(hnt, substrateGenotype);   
        // Wrap using my Network interface
		List<Substrate> substrates = hnt.getSubstrateInformation();
		int[] inputShape = HyperNEATUtil.getInputShape(substrates);
		int outputCount = HyperNEATUtil.getOutputCount(substrates);
		// DL4JNetworkWrapper implements Network
		DL4JNetworkWrapper dl4jNetwork = new DL4JNetworkWrapper(tensorNetwork, inputShape, outputCount);
		return dl4jNetwork;
	}

	@Override
	public Genotype<DL4JNetworkWrapper> newInstance() {
		return new HyperNEATCPPNforDL4JGenotype((HyperNEATCPPNGenotype) cppn.newInstance());
	}

	@Override
	public long getId() {
		return cppn.getId();
	}

	@Override
	public int numModules() {
		return cppn.numModules();
	}

	@Override
	public void setModuleUsage(int[] usage) {
		cppn.setModuleUsage(usage);
	}

	@Override
	public int[] getModuleUsage() {
		return cppn.getModuleUsage();
	}

}