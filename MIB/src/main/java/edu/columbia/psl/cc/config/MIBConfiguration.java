package edu.columbia.psl.cc.config;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.util.GsonManager;

public class MIBConfiguration {
	
	public static int INST_DATA_DEP = 0;
	
	public static int WRITE_DATA_DEP = 1;
	
	public static int CONTR_DEP = 2;
	
	public static int INST_STRAT = 0;
	
	public static int SUBSUB_STRAT = 1;
	
	public static int SUB_STRAT = 2;
	
	public static int CAT_STRAT = 3;
	
	private static String srHandleCommon = "handleOpcode";
	
	private static String srHCDesc = "(III)V";
	
	private static String srHCDescString = "(IILjava/lang/String;)V";
	
	private static String srHandleField = "handleField";
	
	private static String srHandleFieldDesc = "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	private static String srHandleLdc = "handleLdc";
	
	private static String srHandleLdcDesc = "(IIILjava/lang/String;)V";
	
	private static String srHandleMultiArray = "handleMultiNewArray";
	
	private static String srHandleMultiArrayDesc = "(Ljava/lang/String;II)V";
	
	private static String srHandleMethod = "handleMethod";
	
	private static String srHandleMethodDesc = "(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	private static String srGraphDump = "dumpGraph";
	
	private static String srGraphDumpDesc = "()V";
	
	private static String srLoadParent = "loadParent";
	
	private static String srLoadParentDesc = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	private static String srCheckClInit = "checkNGetClInit";
	
	private static String srCheckClInitDesc = "(Ljava/lang/String;)V";
	
	private static String srUpdateCurLabel = "updateCurLabel";
	
	private static String srUpdateCurLabelDesc = "(Ljava/lang/String;)V";
	
	private static String __mib_id_gen = "__MIB_ID_GEN";
	
	private static String __mib_id_gen_method = "__getMIBIDGen";
	
	private static String __mib_id = "__MIB_ID";
	
	private static String recordObjMap = "recordObjId";
	
	private static String recordObjDesc = "(ILjava/lang/Object;)V";
	
	private static String objOnStack = "updateObjOnStack";
	
	private static String objOnStackDesc = "(Ljava/lang/Object;I)V";
	
	public static int TEMPLATE_DIR = 0;
	
	public static int TEST_DIR = 1;
	
	public static int LABEL_MAP_DIR = 2;
	
	public static int CACHE_DIR = 3;
	
	private List<String> excludeClass;
	
	private List<String> testPaths;
	
	private String opTablePath;
	
	private String opCodeCatId;
	
	private String costTableDir;
	
	private int costLimit;
	
	private String templateDir;
	
	private String testDir;
	
	private String pathDir;
	
	private String labelmapDir;
	
	private String resultDir;
	
	private String debugDir;
	
	private String cacheDir;
	
	private double controlWeight;
	
	private double instDataWeight;
	
	private double writeDataWeight;
	
	private int precisionDigit;
	
	//private static int parallelFactor = Runtime.getRuntime().availableProcessors();
	private int parallelFactor;
	
	private int instThreshold;
	
	private int instLimit;
	
	private double pgAlpha;
	
	private int pgMaxIter;
	
	private double pgEpsilon;
	
	//Static threshold, before subgraph matching, decide if subgraph matching should conduct
	private double staticThreshold;
	
	//Dynamic threshold after subgraph matching, decide if hotzone should be selected
	private double simThreshold;
	
	private int assignmentThreshold;
	
	//0: inst, 1: subsubcat, 2: subcat, 3: cat
	private int simStrategy;
	
	private int testMethodThresh;
	
	private int threadInit;
	
	private HashMap<Integer, Integer> threadMethodIdxRecord;
	
	private boolean annotGuard;
	
	private boolean fieldTrack;
	
	private boolean templateMode;
	
	private boolean overallAnalysis;
	
	private boolean debug;
	
	private boolean reduceGraph;
		
	private boolean nativeClass;
	
	private String dburl;
	
	private String dbusername;
	
	private static MIBConfiguration instance = null;
	
	private MIBConfiguration() {
		
	}
	
	public static MIBConfiguration getInstance() {
		if (instance == null) {
			File f = new File("./config/mib_config.json");
			TypeToken<MIBConfiguration> configType = new TypeToken<MIBConfiguration>(){};
			instance = GsonManager.readJsonGeneric(f, configType);
			return instance;
		}
		return instance;
	}
	
	public static MIBConfiguration reloadInstance() {
		instance = null;
		return getInstance();
	}
	
	public static void main(String args[]) {
		System.out.println(MIBConfiguration.getInstance());
	}
	
	public static String getSrHandleCommon() {
		return srHandleCommon;
	}

	public static String getSrHCDesc() {
		return srHCDesc;
	}

	public static String getSrHCDescString() {
		return srHCDescString;
	}

	public static String getSrHandleField() {
		return srHandleField;
	}

	public static String getSrHandleFieldDesc() {
		return srHandleFieldDesc;
	}

	public static String getSrHandleLdc() {
		return srHandleLdc;
	}

	public static String getSrHandleLdcDesc() {
		return srHandleLdcDesc;
	}

	public static String getSrHandleMultiArray() {
		return srHandleMultiArray;
	}

	public static String getSrHandleMultiArrayDesc() {
		return srHandleMultiArrayDesc;
	}

	public static String getSrHandleMethod() {
		return srHandleMethod;
	}

	public static String getSrHandleMethodDesc() {
		return srHandleMethodDesc;
	}

	public static String getSrGraphDump() {
		return srGraphDump;
	}

	public static String getSrGraphDumpDesc() {
		return srGraphDumpDesc;
	}
	
	public static String getSrLoadParent() {
		return srLoadParent;
	}
	
	public static String getSrLoadParentDesc() {
		return srLoadParentDesc;
	}
	
	public static String getSrCheckClInit() {
		return srCheckClInit;
	}
	
	public static String getSrCheckClInitDesc() {
		return srCheckClInitDesc;
	}
	
	public static String getSrUpdateCurLabel() {
		return srUpdateCurLabel;
	}
	
	public static String getSrUpdateCurLabelDesc() {
		return srUpdateCurLabelDesc;
	}

	public static String getMibIdGen() {
		return __mib_id_gen;
	}

	public static String getMibIdGenMethod() {
		return __mib_id_gen_method;
	}

	public static String getMibId() {
		return __mib_id;
	}

	public static String getRecordObjMap() {
		return recordObjMap;
	}

	public static String getRecordObjDesc() {
		return recordObjDesc;
	}

	public static String getObjOnStack() {
		return objOnStack;
	}

	public static String getObjOnStackDesc() {
		return objOnStackDesc;
	}
	
	public List<String> getExcludeClass() {
		return this.excludeClass;
	}
	
	public void setExcludeClass(List<String> excludeClass) {
		this.excludeClass = excludeClass;
	}
	
	public List<String> getTestPaths() {
		return this.testPaths;
	}
	
	public void setTestPaths(List<String> testPaths) {
		this.testPaths = testPaths;
	}
	
	public String getOpTablePath() {
		return opTablePath;
	}

	public void setOpTablePath(String opTablePath) {
		this.opTablePath = opTablePath;
	}

	public String getOpCodeCatId() {
		return opCodeCatId;
	}

	public void setOpCodeCatId(String opCodeCatId) {
		this.opCodeCatId = opCodeCatId;
	}

	public String getCostTableDir() {
		return costTableDir;
	}

	public void setCostTableDir(String costTableDir) {
		this.costTableDir = costTableDir;
	}

	public int getCostLimit() {
		return costLimit;
	}

	public void setCostLimit(int costLimit) {
		this.costLimit = costLimit;
	}

	public double getControlWeight() {
		return controlWeight;
	}

	public void setControlWeight(double controlWeight) {
		this.controlWeight = controlWeight;
	}

	public double getInstDataWeight() {
		return instDataWeight;
	}

	public void setInstDataWeight(double instDataWeight) {
		this.instDataWeight = instDataWeight;
	}
	
	public double getWriteDataWeight() {
		return this.writeDataWeight;
	}
	
	public void setWriteDataWeight(double writeDataWeight) {
		this.writeDataWeight = writeDataWeight;
	}
	
	public int getPrecisionDigit() {
		return precisionDigit;
	}

	public void setPrecisionDigit(int precisionDigit) {
		this.precisionDigit = precisionDigit;
	}

	public int getParallelFactor() {
		return parallelFactor;
	}

	public void setParallelFactor(int parallelFactor) {
		this.parallelFactor = parallelFactor;
	}
	
	public int getInstThreshold() {
		return this.instThreshold;
	}
	
	public void setInstThreshold(int instThreshold) {
		this.instThreshold = instThreshold;
	}
	
	public int getInstLimit() {
		return this.instLimit;
	}
	
	public void setInstLimit(int instLimit) {
		this.instLimit = instLimit;
	}
	
	public void setTestMethodThresh(int testMethodThresh) {
		this.testMethodThresh = testMethodThresh;
	}
	
	public int getTestMethodThresh() {
		return this.testMethodThresh;
	}
	
	public void setThreadInit(int threadInit) {
		this.threadInit = threadInit;
	}
	
	public int getThreadInit() {
		return this.threadInit;
	}
	
	public void setThreadMethodIdxRecord(HashMap<Integer, Integer> threadMethodIdxRecord) {
		this.threadMethodIdxRecord = threadMethodIdxRecord;
	}
	
	public HashMap<Integer, Integer> getThreadMethodIdxRecord() {
		return this.threadMethodIdxRecord;
	}
	
	public double getPgAlpha() {
		return this.pgAlpha;
	}
	
	public void segPgAlpha(double pgAlpha) {
		this.pgAlpha = pgAlpha;
	}
	
	public void setPgMaxIter(int pgMaxIter) {
		this.pgMaxIter = pgMaxIter;
	}
	
	public int getPgMaxIter() {
		return this.pgMaxIter;
	}
	
	public void setPgEpsilon(double pgEpsilon) {
		this.pgEpsilon = pgEpsilon;
	}
	
	public double getPgEpsilon() {
		return this.pgEpsilon;
	}
	
	public String getTemplateDir() {
		return templateDir;
	}
	
	public void setStaticThreshold(double staticThreshold) {
		this.staticThreshold = staticThreshold;
	}
	
	public double getStaticThreshold() {
		return this.staticThreshold;
	}
	
	public void setSimThreshold(double simThreshold) {
		this.simThreshold = simThreshold;
	}
	
	public double getSimThreshold() {
		return this.simThreshold;
	}
	
	public void setAssignmentThreshold(int assignmentThreshold) {
		this.assignmentThreshold = assignmentThreshold;
	}
	
	public int getAssignmentThreshold() {
		return this.assignmentThreshold;
	}
	
	public void setSimStrategy(int simStrategy) {
		this.simStrategy = simStrategy;
	}
	
	public int getSimStrategy() {
		return this.simStrategy;
	}

	public void setTemplateDir(String templateDir) {
		this.templateDir = templateDir;
	}

	public String getTestDir() {
		return testDir;
	}

	public void setTestDir(String testDir) {
		this.testDir = testDir;
	}

	public String getPathDir() {
		return pathDir;
	}

	public void setPathDir(String pathDir) {
		this.pathDir = pathDir;
	}

	public String getLabelmapDir() {
		return labelmapDir;
	}

	public void setLabelmapDir(String labelmapDir) {
		this.labelmapDir = labelmapDir;
	}

	public String getResultDir() {
		return resultDir;
	}

	public void setResultDir(String resultDir) {
		this.resultDir = resultDir;
	}
	
	public String getDebugDir() {
		return this.debugDir;
	}
	
	public void setDebugDir(String debugDir) {
		this.debugDir = debugDir;
	}
	
	public String getCacheDir() {
		return this.cacheDir;
	}
	
	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}

	public boolean isAnnotGuard() {
		return annotGuard;
	}

	public void setAnnotGuard(boolean annotGuard) {
		this.annotGuard = annotGuard;
	}
	
	public boolean isFieldTrack() {
		return this.fieldTrack;
	}
	
	public void setFieldTrack(boolean fieldTrack) {
		this.fieldTrack = fieldTrack;
	}
	
	public boolean isTemplateMode() {
		return this.templateMode;
	}
	
	public void setTemplateMode(boolean templateMode) {
		this.templateMode = templateMode;
	}
	
	public boolean isOverallAnalysis() {
		return this.overallAnalysis;
	}
	
	public void setOverallAnalysis(boolean overallAnalysis) {
		this.overallAnalysis = overallAnalysis;
	}
	
	public boolean isDebug() {
		return this.debug;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public boolean isReduceGraph() {
		return this.reduceGraph;
	}
	
	public void setReduceGraph(boolean reduceGraph) {
		this.reduceGraph = reduceGraph;
	}
	
	/*public boolean isCacheGraph() {
		return this.cacheGraph;
	}
	
	public void setCacheGraph(boolean cacheGraph) {
		this.cacheGraph = cacheGraph;
	}*/
	
	public boolean isNativeClass() {
		return this.nativeClass;
	}
	
	public void setNativeClass(boolean nativeClass) {
		this.nativeClass = nativeClass;
	}
	
	public void setDburl(String dburl) {
		this.dburl = dburl;
	}
	
	public String getDburl() {
		return this.dburl;
	}
	
	public void setDbusername(String dbusername) {
		this.dbusername = dbusername;
	}
	
	public String getDbusername() {
		return this.dbusername;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("op table path: " + this.opTablePath + "\n");
		sb.append("op code cat id: " + this.opCodeCatId + "\n");
		sb.append("cost table dir: " + this.costTableDir + "\n");
		sb.append("cost limit: " + this.costLimit + "\n");
		sb.append("template dir: " + this.templateDir + "\n");
		sb.append("test dir: " + this.testDir + "\n");
		sb.append("path dir: " + this.pathDir + "\n");
		sb.append("label dir: " + this.labelmapDir + "\n");
		sb.append("result dir: " + this.resultDir + "\n");
		sb.append("debug dir: " + this.debugDir + "\n");
		sb.append("control weigtht: " + this.controlWeight + "\n");
		sb.append("inst data weigtht: " + this.instDataWeight + "\n");
		sb.append("write data weight: " + this.writeDataWeight + "\n");
		//sb.append("idx expand factor: " + this.idxExpandFactor + "\n");
		sb.append("precision digit: " + this.precisionDigit + "\n");
		sb.append("parallel factor: " + this.parallelFactor + "\n");
		sb.append("minimum inst number: " + this.instThreshold + "\n");
		sb.append("inst selection number: " + this.instLimit + "\n");
		sb.append("test method threshold: " + this.testMethodThresh + "\n");
		sb.append("alpha of PageRank: " + this.pgAlpha + "\n");
		sb.append("max iteration of PageRank: " + this.pgMaxIter + "\n");
		sb.append("epsilon of PageRank: " + this.pgEpsilon + "\n");
		sb.append("instruction cat lavel: " + this.simStrategy + "\n");
		sb.append("assignment threshold: " + this.assignmentThreshold + "\n");
		sb.append("static threshold: " + this.staticThreshold + "\n");
		sb.append("dynamic threshod: " + this.simThreshold + "\n");
		sb.append("annotation guard: " + this.annotGuard + "\n");
		sb.append("field track: " + this.fieldTrack + "\n");
		sb.append("template mode: " + this.templateMode + "\n");
		sb.append("overall analysis: " + this.overallAnalysis + "\n");
		sb.append("reduce graph: " + this.reduceGraph + "\n");
		//sb.append("cache graphs: " + this.cacheGraph + "\n");
		sb.append("native classification: " + this.nativeClass + "\n");
		sb.append("debug: " + this.debug + "\n");
		return sb.toString();
	}
}