package edu.columbia.psl.cc.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.premain.MIBDriver;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.StringUtil;

public class SVDKernel implements MIBSimilarity<double[][]>{
		
	private StringBuilder sb = new StringBuilder();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*double[][] A = {{3, 1, 1}, {-1, 3, 1}};
		SVDKernel sk = new SVDKernel();
		double[] s = getSingularVector(A);
		System.out.println(Arrays.toString(s));*/
		
		double[] a1 = {1, 2, 3};
		double[] a2 = {4, 5, 6, 9};
		System.out.println(similarityHelper(a1, a2));
	}
	
	public static double[] getSingularVector(double[][] matrix) {
		RealMatrix m = MatrixUtils.createRealMatrix(matrix);
		SingularValueDecomposition svd = new SingularValueDecomposition(m);
		double[] sValues = svd.getSingularValues();
		return sValues;
	}
	
	public static double innerProduct(double[] s1, double[] s2) {
		RealVector rv1 = new ArrayRealVector(s1);
		RealVector rv2 = new ArrayRealVector(s2);
		return rv1.dotProduct(rv2);
	}
	
	public static double sum(double[] array) {
		double ret = 0;
		for (int i = 0; i < array.length; i++) {
			ret += array[i];
		}
		return ret;
	}
	
	private static double similarityHelper(double[] s1, double[] s2) {
		int max = s1.length;
		int min = s2.length;
		boolean cutS1 = true;
		if (s2.length > s1.length) {
			max = s2.length;
			min = s1.length;
			cutS1 = false;
		}
		
		if (cutS1) {
			double[] cuttS1 = new double[min];
			System.arraycopy(s1, 0, cuttS1, 0, min);
			return innerProduct(cuttS1, s2)/max;
		} else {
			double[] cuttS2 = new double[min];
			System.arraycopy(s2, 0, cuttS2, 0, min);
			return innerProduct(s1, cuttS2)/max;
		}
	}
	
	/*private static double similarityHelper(double[] s1, double[] s2) {
		int max = s1.length;
		boolean expandS1 = false;
		if (s2.length > s1.length) {
			max = s2.length;
			expandS1 = true;
		}
		
		if (expandS1) {
			double[] expandedS1 = new double[max];
			System.arraycopy(s1, 0, expandedS1, 0, s1.length);
			for (int i = s1.length; i < max; i++) {
				expandedS1[i] = 0;
			}
			return innerProduct(expandedS1, s2);
		} else {
			double[] expandedS2 = new double[max];
			System.arraycopy(s2, 0, expandedS2, 0, s2.length);
			for (int i = s2.length; i < max; i++) {
				expandedS2[i] = 0;
			}
			return innerProduct(s1, expandedS2);
		}
	}*/
	
	public void updateResult(String key1, String key2, double similarity) {
		System.out.println(key1 + " vs. " + key2 + " " + similarity);
		this.sb.append(key1 + "," + key2 + "," + similarity + "\n");
	}
	
	public String getResult() {
		return this.sb.toString();
	}
	
	@Override
	public void calculateSimilarities(HashMap<String, GraphTemplate> gMap1, 
			HashMap<String, GraphTemplate> gMap2) {
		TreeMap<String, double[][]> cachedMap = new TreeMap<String, double[][]>();
		
		for (String key1: gMap1.keySet()) {
			if (cachedMap.containsKey(key1))
				continue ;
			
			InstPool instPool = gMap1.get(key1).getInstPool();
			
			double[][] adjMatrix = null;
			
			
			if (instPool.size() > 3000) {
				System.out.println("Graph is too large: " + instPool.size());
				System.out.println("Conduct PageRank selection");
				PageRankSelector pgs = new PageRankSelector(instPool, false, false);
				InstPool selectedPool = pgs.selectRepPool();
				adjMatrix = this.constructCostTable(key1, selectedPool);
			} else {
				adjMatrix = this.constructCostTable(key1, instPool);
				//double[][] adjMatrix = this.constructTransitionTables(key1, gMap1.get(key1).getInstPool());
			}
			cachedMap.put(key1, adjMatrix);
		}
		
		for (String key2: gMap2.keySet()) {
			if (cachedMap.containsKey(key2))
				continue ;
			
			InstPool instPool = gMap2.get(key2).getInstPool();
			
			double[][] adjMatrix = null;
			if (instPool.size() > 3000) {
				System.out.println("Graph is too large: " + instPool.size());
				System.out.println("Conduct PageRank selection");
				PageRankSelector pgs = new PageRankSelector(instPool, false, false);
				InstPool selectedPool = pgs.selectRepPool();
				adjMatrix = this.constructCostTable(key2, selectedPool);
			} else {
				adjMatrix = this.constructCostTable(key2, instPool);
				//double[][] adjMatrix = this.constructTransitionTables(key2, gMap2.get(key2).getInstPool());
			}
			cachedMap.put(key2, adjMatrix);
		}
		
		//SVD decomposition is the bottleneck, parallelize it
		HashMap<String, double[]> svdMap = new HashMap<String, double[]>();
		HashMap<String, Future<double[]>> svdFuture = new HashMap<String, Future<double[]>>();
		
		int parallelFactor = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(parallelFactor);
		for (String key: cachedMap.keySet()) {
			SVDWorker worker = new SVDWorker();
			worker.methodName = key;
			worker.adjMatrix = cachedMap.get(key);
			Future<double[]> submit = executor.submit(worker);
			svdFuture.put(key, submit);
		}
		
		executor.shutdown();
		while (!executor.isTerminated());
		System.out.println("All SVDs are done");
		
		for (String key: svdFuture.keySet()) {
			try {
				double[] result = svdFuture.get(key).get();
				svdMap.put(key, result);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		for (String key1: gMap1.keySet()) {
			double[] s1 = svdMap.get(key1);
			
			for (String key2: gMap2.keySet()) {
				double[] s2 = svdMap.get(key2);
				
				double sim = similarityHelper(s1, s2);
				this.updateResult(key1, key2, sim);
			}
		}
	}

	@Override
	public double calculateSimilarity(double[][] metric1, double[][] metric2) {
		double[] s1 = getSingularVector(metric1);
		double[] s2 = getSingularVector(metric2);
		
		System.out.println("s1 singular vector: " + Arrays.toString(s1));
		System.out.println("s2 singular vector: " + Arrays.toString(s2));
		
		return similarityHelper(s1, s2);
	}
	
	public double[][] constructTransitionTables(String methodName, InstPool pool) {
		System.out.println("Constructin transition matrix: " + methodName);
		ArrayList<InstNode> allNodes = new ArrayList<InstNode>(pool);
		double[][] ret = new double[256][256];
		
		for (int i = 0; i < allNodes.size(); i++) {
			InstNode inst = allNodes.get(i);
			double[] transArray = ret[inst.getOp().getOpcode()];
			
			for (String childKey: inst.getChildFreqMap().keySet()) {
				/*String[] keys = StringUtil.parseIdxKey(childKey);
				InstNode childNode = pool.searchAndGet(keys[0], 
						Long.valueOf(keys[1]), 
						Integer.valueOf(keys[2]), 
						Integer.valueOf(keys[3]));*/
				InstNode childNode = pool.searchAndGet(childKey);
				
				transArray[childNode.getOp().getOpcode()] += inst.getChildFreqMap().get(childKey);
			}
		}
		
		//Normalize
		for (int i = 0; i < ret.length; i++) {
			double[] transArray = ret[i];
			double base = sum(transArray);
			for (int j = 0; j < ret.length; j++) {
				double val = 0;
				
				if (base == 0)
					val = 0;
				else
					val = transArray[j]/base;
				
				ret[i][j] = val;
			}
		}
		
		int count = 0;
		String[] opcodeArray = new String[256];
		StringBuilder sb = new StringBuilder();
		sb.append("head,");
		for (int k = 0; k < ret.length; k++) {
			OpcodeObj oo = BytecodeCategory.getOpcodeObj(k);
			
			String toPrint = "";
			if (oo != null) {
				toPrint = oo.getInstruction();
			} else {
				toPrint = "emptyInst" + count++; 
			}
			
			opcodeArray[k] = toPrint;
			if (k == ret.length - 1) {
				sb.append(toPrint + "\n");
			} else {
				sb.append(toPrint + ",");
			}
		}
		
		
		for (int m = 0; m < ret.length; m++) {
			StringBuilder rawBuilder = new StringBuilder();
			rawBuilder.append(opcodeArray[m] + ",");
			for (int n = 0; n < ret.length; n++) {
				rawBuilder.append(ret[m][n] + ",");
			}
			
			if (rawBuilder.length() > 1)
				sb.append(rawBuilder.toString().substring(0, rawBuilder.length() - 1) + "\n");
		}
		
		try {
			File f = new File("./cost_table/" + methodName + ".csv");
			if (f.exists()) {
				f.delete();
			}
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return ret;
	} 
	
	@Override
	public double[][] constructCostTable(String methodName,
			InstPool pool) {
		// List here is just for facilitate dumping cost table. Should remove after
		//System.out.println("Check inst pool: " + pool);
		System.out.println("Constructing cost table for: " + methodName);
		ArrayList<InstNode> allNodes = new ArrayList<InstNode>(pool);
		double[][] ret = new double[allNodes.size()][allNodes.size()];
		
		for (int i = 0; i < allNodes.size(); i++) {
			InstNode i1 = allNodes.get(i);
			for (int j = 0; j < allNodes.size(); j++) {
				InstNode i2 = allNodes.get(j);
				String inst2Key = StringUtil.genIdxKey(i2.getThreadId(), i2.getThreadMethodIdx(), i2.getIdx());
				if (i1.getChildFreqMap().containsKey(inst2Key)) {
					/*double rawVal = (double)1/i1.getChildFreqMap().get(inst2Key);
					double roundVal = GraphUtil.roundValue(rawVal);
					ret[i][j] = roundVal;*/
					ret[i][j] = 1;
				} else {
					//ret[i][j] = MIBConfiguration.getCostLimit();
					ret[i][j] = 0;
				}
			}
		}
		
		/*ShortestPathKernel spk = new ShortestPathKernel();
		CostObj[][] costTable = spk.constructCostTable(methodName, pool);
		for (int i = 0; i < allNodes.size(); i++) {
			for (int j = 0; j < allNodes.size(); j++) {
				ret[i][j] = costTable[i][j].getCost();
			}
		}*/
		
		//Debugging purpose, dump cost table
		StringBuilder sb = new StringBuilder();
		sb.append("head,");
		try {
			for (int k = 0; k < allNodes.size(); k++) {
				if (k == pool.size() - 1) {
					sb.append(allNodes.get(k).toString() + "\n");
				} else {
					sb.append(allNodes.get(k).toString() + ",");
				}
			}
			System.out.println("Header append complets");
		
			for (int m = 0; m < ret.length; m++) {
				StringBuilder rawBuilder = new StringBuilder();
				rawBuilder.append(allNodes.get(m) + ",");
				for (int n = 0; n < ret.length; n++) {
					rawBuilder.append(ret[m][n] + ",");
				}
				
				if (rawBuilder.length() > 1)
					sb.append(rawBuilder.toString().substring(0, rawBuilder.length() - 1) + "\n");
			}
			System.out.println("Data append completes");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Current sb: " + sb.toString());
		}
				
		try {
			File f = new File("./cost_table/" + methodName + ".csv");
			if (f.exists()) {
				f.delete();
			}
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return ret;
	}
	
	public static class SVDWorker implements Callable<double[]> {
		
		String methodName;
		
		double[][] adjMatrix;
		
		@Override
		public double[] call() throws Exception {
			System.out.println("SVD decompose " + methodName + " starts");
			double[] ret = SVDKernel.getSingularVector(adjMatrix);
			System.out.println("SVD decompose " + methodName + " ends");
			//System.out.println(Arrays.toString(ret));
			return ret;
		}
	}

}
