package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.objectweb.asm.Opcodes;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.GraphReducer;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.MethodNode.MetaGraph;

public class GraphConstructor {
	
	private static Logger logger = Logger.getLogger(GraphConstructor.class);
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static String objInit = "java.lang.Object:<init>:():V";
	
	private double maxFreqFromParents(Collection<InstNode> parents, String myId) {
		double ret = 0;
		//System.out.println("My id: " + myId);
		for (InstNode p: parents) {
			//System.out.println("Parent: " + p.getFromMethod() + " " + p.getIdx());
			//System.out.println("Childern map: " + p.getChildFreqMap());
			double freq = p.getChildFreqMap().get(myId);
			if (freq > ret)
				ret = freq;
		}
		return ret;
	}
	
	private HashMap<Integer, HashSet<InstNode>> retrieveParentsWithIdx(HashMap<Integer, HashSet<String>> parentFromCaller, 
			InstPool callerPool) {
		HashMap<Integer, HashSet<InstNode>> ret = new HashMap<Integer, HashSet<InstNode>>();
		for (Integer idx: parentFromCaller.keySet()) {
			HashSet<String> parentStrings = parentFromCaller.get(idx);
			HashSet<InstNode> parents = new HashSet<InstNode>();
			
			for (String parentString: parentStrings) {
				InstNode parentNode = callerPool.searchAndGet(parentString);
				parents.add(parentNode);
			}
			ret.put(idx, parents);
		}
		return ret;
	}
	
	private HashSet<InstNode> flattenParentMap(Collection<HashSet<InstNode>> allParents) {
		HashSet<InstNode> ret = new HashSet<InstNode>();
		for (HashSet<InstNode> partParents: allParents) {
			ret.addAll(partParents);
		}
		return ret;
	}
	
	private void calDepForMethodNodes(InstNode methodNode, InstPool instPool, List<InstNode> collecter) {
		if (collecter.contains(methodNode))
			return ;
		
		for (String key: methodNode.getChildFreqMap().keySet()) {
			InstNode childInst = instPool.searchAndGet(key);
			
			if ((childInst instanceof MethodNode)) {
				calDepForMethodNodes(childInst, instPool, collecter);
			}
		}
		collecter.add(methodNode);
	}
	
	public void reconstructGraph(GraphTemplate rawGraph, boolean addCallerLine) {
		String myId = StringUtil.genThreadWithMethodIdx(rawGraph.getThreadId(), rawGraph.getThreadMethodId());
		String baseDir = MIBConfiguration.getInstance().getCacheDir() + "/" + myId;
		
		File baseDirProbe = new File(baseDir);
		if (!baseDirProbe.exists()) {
			if (MIBConfiguration.getInstance().isReduceGraph()) {
				GraphReducer gr = new GraphReducer(rawGraph);
				gr.reduceGraph();
			}
			return ;
		}
		
		try {
			List<InstNode> processQueue = new ArrayList<InstNode>();
			for (InstNode inst: rawGraph.getInstPool()) {
				if (processQueue.contains(inst))
					continue ;
				
				if (!(inst instanceof MethodNode)) {
					processQueue.add(inst);
				}
				
				calDepForMethodNodes(inst, rawGraph.getInstPool(), processQueue);
			}
			
			List<GraphTemplate> toMerge = new ArrayList<GraphTemplate>();
			InstNode lastBeforeReturn = rawGraph.getLastBeforeReturn();
			for (InstNode inst: processQueue) {
				if (inst instanceof MethodNode) {
					logger.info("Method node: " + inst);
					MethodNode mn = (MethodNode)inst;
					
					String mnId = StringUtil.genIdxKey(mn.getThreadId(), mn.getThreadMethodIdx(), mn.getIdx());
					TreeMap<String, Double> childMap = mn.getChildFreqMap();
					Set<InstNode> cRemoveMn = new HashSet<InstNode>();
					
					HashMap<Integer, HashSet<InstNode>> parentFromCaller = null;
					double allFreq = 0;
					if (mn.getCalleeInfo().parentReplay != null && mn.getCalleeInfo().parentReplay.size() > 0) {
						parentFromCaller = retrieveParentsWithIdx(mn.getCalleeInfo().parentReplay, rawGraph.getInstPool());
						
						HashSet<InstNode> allParents = flattenParentMap(parentFromCaller.values());
						allFreq = maxFreqFromParents(allParents, mnId);
					}
					
					HashSet<InstNode> controlInsts = null;
					if (mn.getControlParentList().size() > 0) {
						controlInsts = GraphUtil.retrieveRequiredParentInsts(mn, 
								rawGraph.getInstPool(), 
								MIBConfiguration.CONTR_DEP);
					}
					
					for (MetaGraph meta: mn.getCalleeInfo().metaCallees) {
						String calleeId = meta.calleeIdx;
						double frac = meta.normFreq;
						
						logger.info("Callee idx: " + calleeId);
						File f = new File(baseDir + "/" + calleeId);
						GraphTemplate callee = GsonManager.readJsonGeneric(f, graphToken);
						reconstructGraph(callee, false);
						toMerge.add(callee);
						
						if (addCallerLine) {
							for (InstNode ia: callee.getInstPool()) {
								ia.callerLine = inst.getLinenumber();
							}
						}
						
						//This means that the method call is the last inst, will only have 1 graph
						if (lastBeforeReturn != null && inst.equals(lastBeforeReturn)) {
							rawGraph.setLastBeforeReturn(callee.getLastBeforeReturn());
						}
						
						HashSet<String> cReads = callee.getFirstReadLocalVars();
						HashSet<InstNode> cReadNodes = new HashSet<InstNode>();
						for (String cString: cReads) {
							InstNode cReadNode = callee.getInstPool().searchAndGet(cString);
							
							if (cReadNode == null) {
								logger.error("Cannot find read node: " + cString);
							}
							
							cReadNodes.add(cReadNode);
						}
						
						if (parentFromCaller != null) {
							double freq = allFreq * frac;
							GraphUtil.multiplyGraph(callee, freq);	
							//GraphUtil.dataDepFromParentToChildWithFreq(parentFromCaller, cReadNodes, mnId, freq);
							GraphUtil.dataDepFromParentToChildWithFreq(parentFromCaller, cReadNodes, freq);
						}
						
						if (controlInsts != null) {
							for (InstNode controlInst: controlInsts) {
								double freq = controlInst.getChildFreqMap().get(mnId) * frac;
								GraphUtil.controlDepFromParentToChildWithFreq(controlInst, cReadNodes, freq);
								//controlInst.getChildFreqMap().remove(mnId);
								//pRemoveMn.add(controlInst);
							}
						}
						
						if (childMap.size() > 0) {
							//String calleeChildReplaceId = meta.lastInstString;
							//InstNode calleeChildReplace = callee.getInstPool().searchAndGet(calleeChildReplaceId);
							
							InstNode calleeChildReplace = callee.getLastBeforeReturn();
							if (calleeChildReplace == null) {
								logger.info("Current inst: " + inst);
								logger.info("Find no last inst in callee: " + calleeId);
								//System.exit(1);
								continue ;
							}
							
							for (String childKey: childMap.keySet()) {
								InstNode childNode = rawGraph.getInstPool().searchAndGet(childKey);
								double cFreq = childMap.get(childKey) * frac;	
								
								if (childNode == null) {
									logger.info("Current inst: " + inst);
									logger.info("Empty child: " + childKey);
									logger.info("Search toMerge");
									
									//In the merge
									for (GraphTemplate toM: toMerge) {
										childNode = toM.getInstPool().searchAndGet(childKey);
										if (childNode != null)
											break;
									}
								}
								
								if (childNode == null) {
									logger.error("Current inst: " + inst);
									logger.error("Missing inst: " + childKey);
									//System.exit(1);
									continue ;
								}
								
								calleeChildReplace.increChild(childNode.getThreadId(), 
										childNode.getThreadMethodIdx(), 
										childNode.getIdx(), 
										cFreq);
								childNode.registerParent(calleeChildReplace.getThreadId(), 
										calleeChildReplace.getThreadMethodIdx(), 
										calleeChildReplace.getIdx(), 
										MIBConfiguration.INST_DATA_DEP);
								
								//childNode.getInstDataParentList().remove(mnId);
								cRemoveMn.add(childNode);
							}
						}
					}
					
					//Remove inst parent
					if (parentFromCaller != null) {
						for (Integer i: parentFromCaller.keySet()) {
							HashSet<InstNode> parents = parentFromCaller.get(i);
							for (InstNode pNode: parents) {
								pNode.getChildFreqMap().remove(mnId);
							}
						}
					}
					
					//Remove control parent
					if (controlInsts != null) {
						for (InstNode pInst: controlInsts) {
							pInst.getChildFreqMap().remove(mnId);
						}
					}
					
					for (InstNode cInst: cRemoveMn) {
						cInst.getInstDataParentList().remove(mnId);
					}
					
					rawGraph.getInstPool().remove(inst);
				} else if (addCallerLine) {
					inst.callerLine = inst.getLinenumber();
				}
			}
			
			for (GraphTemplate callee: toMerge) {
				InstPool calleePool = callee.getInstPool();
				GraphUtil.unionInstPools(rawGraph.getInstPool(), calleePool);
			}
			
			if (MIBConfiguration.getInstance().isReduceGraph()) {
				GraphReducer gr = new GraphReducer(rawGraph);
				gr.reduceGraph();
			}
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public void cleanObjInit(GraphTemplate constructedGraph) {
		HashSet<InstNode> toRemove = new HashSet<InstNode>();
		InstPool pool = constructedGraph.getInstPool();
		for (InstNode inst: pool) {
			int opcode = inst.getOp().getOpcode();
			if (opcode == Opcodes.INVOKESPECIAL && !toRemove.contains(inst)) {
				String addInfo = inst.getAddInfo();
				
				if (addInfo != null && addInfo.equals(objInit)) {
					this.collectSingleParent(inst, pool, toRemove);
				}
			}
		}
		
		for (InstNode r: toRemove) {
			pool.remove(r);
		}
	}
	
	public void collectSingleParent(InstNode inst, InstPool pool, HashSet<InstNode> recorder) {
		recorder.add(inst);
		
		for (String parentKey: inst.getInstDataParentList()) {
			InstNode parentNode = pool.searchAndGet(parentKey);
			
			if (parentNode == null) {
				logger.error("Missing parent when cleaning obj init: " + parentKey);
				continue ;
			}
			
			//Singel child, which is me
			if (parentNode.getChildFreqMap().size() == 1) {
				this.collectSingleParent(parentNode, pool, recorder);
			}
		}
	} 
	
	public static void main(String[] args) {
		//File testFile = new File("./test/cern.colt.matrix.linalg.Algebra:hypot:0:0:130.json");
		//File testFile = new File("/Users/mikefhsu/Mike/Research/ec2/mib_sandbox/jama_graphs/Jama.EigenvalueDecomposition:<init>:0:1:1515059.json");
		File testFile = new File("/Users/mikefhsu/ccws/jvm-clones/MIB/test/org.ejml.alg.dense.decomposition.svd.SvdImplicitQrDecompose_D64:decompose:0:0:14.json");
		GraphTemplate testGraph = GsonManager.readJsonGeneric(testFile, graphToken);
		GraphConstructor constructor = new GraphConstructor();
		constructor.reconstructGraph(testGraph, true);
		System.out.println("Recorded graph size: " + testGraph.getVertexNum());
		System.out.println("Actual graph size: " + testGraph.getInstPool().size());
		
		System.out.println("Recorded edge size: " + testGraph.getEdgeNum());
		int eCount = 0;
		for (InstNode inst: testGraph.getInstPool()) {
			//System.out.println("Inst: " + inst);
			//System.out.println(inst.callerLine);
			eCount += inst.getChildFreqMap().size();
		}
		System.out.println("Actual edge size: " + eCount);
		
		GraphReducer gr = new GraphReducer(testGraph);
		gr.reduceGraph();
		System.out.println("Reduce result: " + gr.getGraph().getInstPool().size());
		
		/*String methodName = testGraph.getMethodName();
		TreeSet<Integer> lineTrace = new TreeSet<Integer>();
		for (InstNode inst: testGraph.getInstPool()) {
			if (inst.getFromMethod().contains(methodName)) {
				int line = inst.getLinenumber();
				lineTrace.add(line);
			}
		}
		System.out.println("Line trace: " + lineTrace);*/
		
		String fileName = "/Users/mikefhsu/Desktop/" + testGraph.getShortMethodKey() + "_re2.json";
		/*List<InstNode> sorted = GraphUtil.sortInstPool(testGraph.getInstPool(), true);
		for (InstNode inst: sorted) {
			System.out.println(inst);
			System.out.println(inst.getStartTime());
			System.out.println(inst.getChildFreqMap());
			System.out.println();
		}*/
		GsonManager.writeJsonGeneric(testGraph, fileName, graphToken, 8);
	}

}
