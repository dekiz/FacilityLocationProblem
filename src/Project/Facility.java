package Project;

import java.util.Random;

import gurobi.*;

public class Facility {

	public static void main(String[] args) {
		try {
			Random rand = new Random();
			// int n = rand.nextInt(50);
			int n = 5;
			int m = 2;
			// Warehouse demand in thousands of units
			double Demand[] = new double[n];// { 15, 18, 14, 20, 5 };
			// Plant capacity in thousands of units
			double Capacity[] = new double[m];// { 20, 22, 17, 19, 18 };
			double sum = 0;
			int k = 0;
			for (int i = 0; i < n; i++) {
				Demand[i] = rand.nextInt(29) + 1;
				sum += Demand[i];
				if ((i+1) % (n / m) == 0) {
					if(k==m-1)
					{
						Capacity[k] += sum + rand.nextInt(10)+10;
					}
					else
					{
					Capacity[k] = sum + rand.nextInt(10)+10;
					k++;
					}
					//System.out.println(sum + "   " + i + "  " + k);
					sum = 0;
				}

			}
			//Print Demand
			for(int i=0;i<n;i++)
				System.out.print(Demand[i]+" ");
			System.out.println();
			
			//Print Capacity
			for(int i=0;i<m;i++)
				System.out.print(Capacity[i]+" ");
			System.out.println();
			
			// Fixed costs for each plant
			double FixedCosts[] = new double[m]; //{ 12000, 10000, 17000, 13000, 16000, 13000, 20000, 15000, 11000, 18000 };
			for(int i=0;i<m;i++)
				FixedCosts[i]=10000+rand.nextInt(10000);
			
			//Print Fixed Costs
			for(int i=0;i<m;i++)
				System.out.print(FixedCosts[i]+" ");
			System.out.println();
			
			// Transportation costs per thousand units and Print
			double TransCosts[][] = new double[n][m];
			for (int i = 0; i < n; i++)
			{
				for (int j = 0; j < m; j++)
				{
					TransCosts[i][j] = 2000 + rand.nextInt(3000);
					System.out.print(TransCosts[i][j]+" ");
				}
				System.out.println();
		}
			/*
			 * { { 4000, 2000, 3000, 2500, 4500 }, { 2500, 260000, 3400, 3000,
			 * 4000 }, { 1200, 180000, 2600, 4100, 3000 }, { 2200, 260000, 3100,
			 * 3700, 3200 }, { 1500, 180000, 3600, 100, 2000 }};
			 */

			// Number of plants and warehouses
			int nPlants = Capacity.length;
			int nWarehouses = Demand.length;

			// Model
			GRBEnv env = new GRBEnv();
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "facility");

			// Plant open decision variables: open[p] == 1 if plant p is open.
			GRBVar[] open = new GRBVar[nPlants];
			for (int p = 0; p < nPlants; ++p) {
				open[p] = model.addVar(0, 1, FixedCosts[p], GRB.BINARY, "Open" + p);
			}

			// Transportation decision variables: how much to transport from
			// a plant p to a warehouse w
			GRBVar[][] transport = new GRBVar[nWarehouses][nPlants];
			for (int w = 0; w < nWarehouses; ++w) {
				for (int p = 0; p < nPlants; ++p) {
					transport[w][p] = model.addVar(0, GRB.INFINITY, TransCosts[w][p], GRB.CONTINUOUS,
							"Trans" + p + "." + w);
				}
			}

			// The objective is to minimize the total fixed and variable costs
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

			// Production constraints
			// Note that the right-hand limit sets the production to zero if
			// the plant is closed
			for (int p = 0; p < nPlants; ++p) {
				GRBLinExpr ptot = new GRBLinExpr();
				for (int w = 0; w < nWarehouses; ++w) {
					ptot.addTerm(1.0, transport[w][p]);
				}
				GRBLinExpr limit = new GRBLinExpr();
				limit.addTerm(Capacity[p], open[p]);
				model.addConstr(ptot, GRB.LESS_EQUAL, limit, "Capacity" + p);
			}

			// Demand constraints
			for (int w = 0; w < nWarehouses; ++w) {
				GRBLinExpr dtot = new GRBLinExpr();
				for (int p = 0; p < nPlants; ++p) {
					dtot.addTerm(1.0, transport[w][p]);
				}
				model.addConstr(dtot, GRB.EQUAL, Demand[w], "Demand" + w);
			}

			// Guess at the starting point: close the plant with the highest
			// fixed costs; open all others

			// First, open all plants
			for (int p = 0; p < nPlants; ++p) {
				open[p].set(GRB.DoubleAttr.Start, 1.0);
			}

			// Now close the plant with the highest fixed cost
			System.out.println("Initial guess:");
			double maxFixed = -GRB.INFINITY;
			for (int p = 0; p < nPlants; ++p) {
				if (FixedCosts[p] > maxFixed) {
					maxFixed = FixedCosts[p];
				}
			}
			for (int p = 0; p < nPlants; ++p) {
				if (FixedCosts[p] == maxFixed) {
					open[p].set(GRB.DoubleAttr.Start, 0.0);
					System.out.println("Closing plant " + p + "\n");
					break;
				}
			}

			// Use barrier to solve root relaxation
			model.set(GRB.IntParam.Method, GRB.METHOD_BARRIER);

			// Solve
			model.optimize();

			// Print solution
			System.out.println("\nTOTAL COSTS: " + model.get(GRB.DoubleAttr.ObjVal));
			System.out.println("SOLUTION:");
			for (int p = 0; p < nPlants; ++p) {
				if (open[p].get(GRB.DoubleAttr.X) > 0.99) {
					System.out.println("Plant " + p + " open:");
					for (int w = 0; w < nWarehouses; ++w) {
						if (transport[w][p].get(GRB.DoubleAttr.X) > 0.0001) {
							System.out.println("  Transport " + transport[w][p].get(GRB.DoubleAttr.X)
									+ " units to warehouse " + w);
						}
					}
				} else {
					System.out.println("Plant " + p + " closed!");
				}
			}

			// Dispose of model and environment
			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
		}
	}
}