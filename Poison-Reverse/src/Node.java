import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the class that students need to implement. The code skeleton is
 * provided. Students need to implement rtinit(), rtupdate() and linkhandler().
 * printdt() is provided to pretty print a table of the current costs for
 * reaching other nodes in the network.
 */
public class Node {

	public static final int INFINITY = 9999;

	int[] lkcost; /* The link cost between node 0 and other nodes */
	int[][] costs; /* Define distance table */
	int nodename; /* Name of this node */
	Map<Integer, Integer> map;

	/* Class constructor */
	public Node() {
		this.costs = new int[4][4];
		this.lkcost = new int[4];
		this.map = new HashMap<Integer, Integer>();
	}

	// public Node(int nodename) {
	// this.costs = new int[4][4];
	// this.lkcost = new int[4];
	// this.nodename = nodename;
	// }

	/* students to write the following two routines, and maybe some others */
	void rtinit(int nodename, int[] initial_lkcost) {
		this.nodename = nodename;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (i == j) {
					this.costs[i][j] = initial_lkcost[i];
				} else
					this.costs[i][j] = INFINITY;
			}
		}
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (i == j && this.costs[i][j] < INFINITY && nodename != i) {
					Packet packet = new Packet(nodename, i, initial_lkcost);
					NetworkSimulator.tolayer2(packet);
					break;
				}
			}
		}
	}

	void rtupdate(Packet rcvdpkt) {
		int next_hop = rcvdpkt.sourceid;
		int[] extra_cost = rcvdpkt.mincost;
		// System.out.println(Arrays.toString(extra_cost));
		boolean flag = false;
		int newcost = 0;
		for (int i = 0; i < 4; i++) {
			if (i != nodename && extra_cost[i] < INFINITY && lkcost[i] != 0) {
				costs[i][next_hop] = costs[next_hop][next_hop] + extra_cost[i];
			}
			if (i != nodename && lkcost[i] == 0 && extra_cost[i] < INFINITY) {
				lkcost[i] = costs[next_hop][next_hop] + extra_cost[i];
				costs[i][next_hop] = lkcost[i];
				map.put(next_hop, i);
				continue;
			}
			// If a link is broken, then we need update
			if (extra_cost[i] >= INFINITY) {
				costs[i][next_hop] = INFINITY;
				if (map.containsKey(next_hop)) {
					if (map.get(next_hop) == i) {
						map.remove(next_hop);
					}
				}
				// The lkcost might change or not. Here we double check in
				// any
				// case
				int tmp = lkcost[i];
				for (int j = 0; j < 4; j++) {
					if (j == nodename)
						continue;
					lkcost[i] = Math.min(lkcost[i], costs[i][j]);

				}
				if (tmp != lkcost[i])
					flag = true;
				continue;
			}

			// If the link is not broken
			newcost = extra_cost[i] + costs[next_hop][next_hop];
			// System.out.println("new_cost: " + newcost);
			// System.out.println("lkcost: " + lkcost[i]);
			// System.out.println("cost table: " + costs[next_hop][next_hop]);
			if (costs[next_hop][next_hop] < INFINITY && newcost < lkcost[i]) {
				if (newcost < costs[i][i]) {
					costs[i][next_hop] = newcost;
					lkcost[i] = newcost;
					map.put(next_hop, i);
					flag = true;
					continue;
				}
			}
			if (costs[i][i] < lkcost[i]) {
				if (map.containsKey(next_hop)) {
					if (map.get(next_hop) == i)
						map.remove(next_hop);
				}
				lkcost[i] = costs[i][i];
				flag = true;
			}
			// else if ()
		}
		if (flag) {
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					if (i != nodename && i == j && costs[i][j] < INFINITY) {
						if (!map.containsKey(i)) {
							Packet standard_packet = new Packet(nodename, i, lkcost);
							NetworkSimulator.tolayer2(standard_packet);
						} else {
							int tmp = lkcost[map.get(i)];
							lkcost[map.get(i)] = INFINITY;
							Packet poison_packet = new Packet(nodename, i, lkcost);
							NetworkSimulator.tolayer2(poison_packet);
							lkcost[map.get(i)] = tmp;
						}
						break;
					}
				}
			}
		}
		printdt();
		// System.out.println(Arrays.deepToString(costs));
	}

	/*
	 * called when cost from the node to linkid changes from current value to
	 * newcost
	 */
	void linkhandler(int linkid, int newcost) {
		boolean change = false;
		costs[linkid][linkid] = newcost;
		// for (int i = 0; i < 4; i++) {
		// if (i == nodename)
		// continue;
		// if (i == linkid)
		// continue;
		// costs[linkid][i] = costs[linkid][i] + newcost;
		// }
		for (int i = 0; i < 4; i++) {
			if (i == nodename)
				continue;
			for (int j = 0; j < 4; j++) {
				if (j == nodename)
					continue;
				// System.out.println("lkcost: " + lkcost[i]);
				// System.out.println("costs[i][j]: " + costs[i][j]);
				boolean flag = false;
				if (costs[i][0] > lkcost[i] && costs[i][1] > lkcost[i] && costs[i][2] > lkcost[i]
						&& costs[i][3] > lkcost[i]) {
					flag = true;
				}
				if (flag || lkcost[i] > costs[i][j]) {
					change = true;
					if (i != j) {
						List<Integer> tmp_list = new ArrayList<>();
						for (int k : map.keySet()) {
							if (map.get(k) == i) {
								tmp_list.add(k);
							}
						}
						for (int m : tmp_list) {
							map.remove(m);
						}
						map.put(j, i);
					}
					lkcost[i] = costs[i][j];
				}
			}
		}
		if (change) {
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					if (i != nodename && i == j && costs[i][j] < INFINITY) {
						if (map.containsKey(i)) {
							int k = map.get(i);
							int tmp = lkcost[i];
							lkcost[i] = INFINITY;
							Packet poison = new Packet(nodename, i, lkcost);
							NetworkSimulator.tolayer2(poison);
							lkcost[i] = tmp;
						} else {
							Packet standard = new Packet(nodename, i, lkcost);
							NetworkSimulator.tolayer2(standard);
						}
					}
				}
			}
		}

	}

	/* Prints the current costs to reaching other nodes in the network */
	void printdt() {
		switch (nodename) {

		case 0:
			System.out.printf("                via     \n");
			System.out.printf("   D0 |    1     2 \n");
			System.out.printf("  ----|-----------------\n");
			System.out.printf("     1|  %3d   %3d \n", costs[1][1], costs[1][2]);
			System.out.printf("dest 2|  %3d   %3d \n", costs[2][1], costs[2][2]);
			System.out.printf("     3|  %3d   %3d \n", costs[3][1], costs[3][2]);
			break;
		case 1:
			System.out.printf("                via     \n");
			System.out.printf("   D1 |    0     2    3 \n");
			System.out.printf("  ----|-----------------\n");
			System.out.printf("     0|  %3d   %3d   %3d\n", costs[0][0], costs[0][2], costs[0][3]);
			System.out.printf("dest 2|  %3d   %3d   %3d\n", costs[2][0], costs[2][2], costs[2][3]);
			System.out.printf("     3|  %3d   %3d   %3d\n", costs[3][0], costs[3][2], costs[3][3]);
			break;
		case 2:
			System.out.printf("                via     \n");
			System.out.printf("   D2 |    0     1    3 \n");
			System.out.printf("  ----|-----------------\n");
			System.out.printf("     0|  %3d   %3d   %3d\n", costs[0][0], costs[0][1], costs[0][3]);
			System.out.printf("dest 1|  %3d   %3d   %3d\n", costs[1][0], costs[1][1], costs[1][3]);
			System.out.printf("     3|  %3d   %3d   %3d\n", costs[3][0], costs[3][1], costs[3][3]);
			break;
		case 3:
			System.out.printf("                via     \n");
			System.out.printf("   D3 |    1     2 \n");
			System.out.printf("  ----|-----------------\n");
			System.out.printf("     0|  %3d   %3d\n", costs[0][1], costs[0][2]);
			System.out.printf("dest 1|  %3d   %3d\n", costs[1][1], costs[1][2]);
			System.out.printf("     2|  %3d   %3d\n", costs[2][1], costs[2][2]);
			break;
		}
	}

}