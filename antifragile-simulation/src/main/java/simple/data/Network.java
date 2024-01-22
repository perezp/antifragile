package simple.data;

import java.util.ArrayList;
import java.util.Random;

/**
 * 
 */
public class Network {

//An array for the types and an array for the nodes
	private ArrayList<Integer>[] neighbors;
	private int[] type;
	private boolean[] isInfected;
	private double meanHealingT; // all nodes have the same healtime TODO: make it node type specific, or node specific?

	private VulnerabilityExploit[] exploits;

	private Random r = new Random();

//Creates a single vulnerability for each type of sofware
	/**
	 * @param nnodes number of nodes
	 * @param nsoftware number of types of software
	 * @param avgNeighbors average number of neighbors of each node
	 * @param avgProbAttack average probability of attack at a given time
	 * @param meanHealT average healing time
	 * @param filename
	 */
	public void createSystem(int nnodes, int nsoftware, double avgNeighbors, double avgProbAttack, double meanHealT,
			String filename) {

	
		exploits = new VulnerabilityExploit[nsoftware];
		type = new int[nnodes];
		neighbors = new ArrayList[nnodes];
		isInfected = new boolean[nnodes];
		meanHealingT = meanHealT;
		// fill exploits
		fillExploits(nsoftware, avgProbAttack);
		fillType(nnodes, nsoftware);
		fillNeighbor(nnodes, avgNeighbors);
		fillInfected(nnodes);

	}

	private void fillInfected(int nnodes) {
		for (int i = 0; i < nnodes; i++) {
			isInfected[i] = false;
		}
	}

	private void fillNeighbor(int nnodes, double avgNeighbors) {
		for (int i = 0; i < nnodes; i++) {
			neighbors[i] = new ArrayList<Integer>();
		}

		// Since each neighbor counts double, the avgNeignobrs is divided by 2.
		double neigForNode = avgNeighbors / 2.0;
		double probabilityAnotherNeigh = (neigForNode - 1.0) / neigForNode;
		for (int i = 0; i < nnodes; i++) {

			// Create the first (to a previous node) and then create the rest to any that is
			// not our neighbor.
			// If node is 0, we do not create the first to a previous

			int neigh;
			if (!allPreviousAreNeighbors(i)) {
				neigh = r.nextInt(i); // Creates a random between 0 (incl) and i (excl)
				int count = 0;
				int indexSearchNodes = 0;

				// find the neighbor. Not the "neigh" because it may be already a neighbor
				while (count < neigh) {
					if (!isAlreadyNeighbor(indexSearchNodes, i)) {
						count++;
					}
					indexSearchNodes = (indexSearchNodes + 1) % i;
				}
				addNeighbor(indexSearchNodes, i);
			} else {// create the first neighbor between i and nnodes
				if (i < nnodes - 1) {
					neigh = r.nextInt(nnodes - 1 - i); // TODO: this may be called with a 0 if we are in the last node
					addNeighbor(i + 1 + neigh, i);
				}
			}

			// First created
			while (r.nextDouble() < probabilityAnotherNeigh) {
				neigh = r.nextInt(nnodes - 1);
				addNeighbor(searchIthNodeNotNeighborYet(neigh, i, nnodes), i);
			}
		}

	}

	private int searchIthNodeNotNeighborYet(int ithNotNeighbor, int nodeIndex, int nnodes) {
		int count = 0;
		int indexSearched = 0;
		while (count < ithNotNeighbor) {
			if (!isAlreadyNeighbor(indexSearched, nodeIndex)) {
				count++;
			}
			indexSearched = (indexSearched + 1) % nnodes;
		}
		return indexSearched;
	}

	private boolean allPreviousAreNeighbors(int i) {
		for (int j = 0; j < i; j++) {
			if (!isAlreadyNeighbor(j, i)) {
				return false;
			}
		}
		return true;
	}

	private boolean isAlreadyNeighbor(int node, int neigh) {
		if (node == neigh) {
			return true;
		}
		return neighbors[node].contains(neigh);
	}

	private void addNeighbor(int j, int i) {
		neighbors[j].add(i);
		neighbors[i].add(j);

	}

	private void fillType(int nnodes, int differents) {
		for (int i = 0; i < nnodes; i++) {
			type[i] = (int) Math.floor(Math.random() * differents);

		}

	}

	private void fillExploits(int nsoftware, double avgProbAttack) {
		for (int i = 0; i < nsoftware; i++) {
			exploits[i] = new VulnerabilityExploit(i, avgProbAttack, i);
		}
	}
	
	
	//To speedup simulations, we reuse the same array;
	private boolean[] infectedAtCurrentTime;
	/**
	 * This method propagates the infection to the neighbors of the same type;
	 */
	public void propagateInfections() {
		if(infectedAtCurrentTime==null) {
			infectedAtCurrentTime = new boolean[isInfected.length];
		}
		for(int n=0; n<neighbors.length; n++) {
			if(isInfected[n]) {
				//Add to the infected at current time
				infectedAtCurrentTime[n]=true;
				//Propagate to neighbors of same type
				for(Integer neigh : neighbors[n]) {
					if(type[n]==type[neigh]) {
						infectedAtCurrentTime[neigh]=true;
					}
				}
			}
		}
		swapArraysAndSetToFalse();
	}
	
	/**
	 * This method creates a new outbreak in each node according to the probability of attack;
	 */
	public void createOutbreaks() {
		for(int n=0; n<isInfected.length; n++) {
			//If the node is not already infected
			if (!isInfected[n]){
				if(r.nextDouble()<exploits[type[n]].getProbAttack()) {
					isInfected[n]=true;
				}
			}
		}
	}
	
	/**
	 * This method heals infected node according to the probability of healing;
	 */
	public void healNodes() {
		double healingProbability = (meanHealingT-1.0)/meanHealingT;
		for(int n=0; n<isInfected.length; n++) {
			//If the node is already infected
			if (isInfected[n]){
				if(r.nextDouble()>healingProbability) {
					//Here it is higher than because the larger the probability calculated 
					//means that the larger the average time to realize that a node should 
					//heal and, therefore, the lower the chances that we heal it at the 
					//current moment
					isInfected[n]=false;
				}
			}
		}
	}
	
	
	private void swapArraysAndSetToFalse() {
		boolean[] aux = isInfected;
		isInfected=infectedAtCurrentTime;
		infectedAtCurrentTime=aux;
		for(int i=0 ; i<infectedAtCurrentTime.length; i++) {
			infectedAtCurrentTime[i]=false;
		}
		
	}

	@Override
	public String toString() {
		String result = "";

		for (int i=0; i<neighbors.length; i++) {
			result +="Node " + i+"- Neighbors: ";
			for(Integer n : neighbors[i]) {
				result+=n+", ";
			}
			result+=System.getProperty("line.separator") ;
		
					
		}
	return result;
	}

	public double calculateProportionInfected() {
		double numInfected=0.0;
		for(int i=0; i<isInfected.length; i++) {
			if(isInfected[i]) {
				numInfected++;
			}
		}
		return numInfected/((double) isInfected.length);
	}
	
	public double calculateProportionHealthy() {
		return 1.0-calculateProportionInfected();
	}

	public void resetInfected() {
		for(int i=0; i<isInfected.length; i++) {
			isInfected[i]=false;
		}
		
	}




	
}
