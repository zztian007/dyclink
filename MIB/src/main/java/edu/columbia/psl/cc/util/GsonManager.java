package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.premain.MIBDriver;

public class GsonManager {
	
	private static Logger logger = LogManager.getLogger(GsonManager.class);
	
	public static void writePath(String fileName, List<InstNode> path) {
		StringBuilder sb = new StringBuilder();
		for (InstNode inst: path) {
			sb.append(inst.toString() + "\n");
		}
		
		try {
			File f = new File(MIBConfiguration.getInstance().getPathDir() + "/" + fileName + ".txt");
			if (f.exists())
				f.delete();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public static <T> void writeJson(T obj, String fileName, boolean isTemplate) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(Var.class, new VarAdapter());
		Gson gson = gb.create();
		String toWrite = gson.toJson(obj);
		try {
			File f;
			if (isTemplate) {
				f = new File(MIBConfiguration.getInstance().getTemplateDir() + "/" + fileName + ".json");
			} else {
				f = new File(MIBConfiguration.getInstance().getTestDir() + "/" + fileName + ".json");
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(toWrite);
			bw.close();
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public static <T> T readJson(File f, T type) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(Var.class, new VarAdapter());
		Gson gson = gb.create();
		try {
			JsonReader jr = new JsonReader(new FileReader(f));
			T ret = gson.fromJson(jr, type.getClass());
			jr.close();
			return ret;
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
		return null;
	}
	
	/**
	 * 0 for template, 1 for test, 2 for labelmap
	 * @param obj
	 * @param fileName
	 * @param typeToken
	 * @param isTemplate
	 */
	public static <T> void writeJsonGeneric(T obj, String fileName, TypeToken typeToken, int dirIdx) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(InstNode.class, new InstNodeAdapter());
		Gson gson = gb.enableComplexMapKeySerialization().create();
		String toWrite = gson.toJson(obj, typeToken.getType());
		try {
			File f;
			if (dirIdx == MIBConfiguration.TEMPLATE_DIR) {
				f = new File(MIBConfiguration.getInstance().getTemplateDir() + "/" + fileName + ".json");
			} else if (dirIdx == MIBConfiguration.TEST_DIR) {
				f = new File(MIBConfiguration.getInstance().getTestDir() + "/" + fileName + ".json");
			} else if (dirIdx == MIBConfiguration.LABEL_MAP_DIR) {
				f = new File(MIBConfiguration.getInstance().getLabelmapDir() + "/" + fileName + ".json");
			} else if (dirIdx == MIBConfiguration.CACHE_DIR){
				f = new File(MIBConfiguration.getInstance().getCacheDir() + "/" + fileName);
			} else {
				f = new File(fileName);
			}
			
			if (!f.exists()) {
				f.createNewFile();
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(toWrite);
			bw.close();
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public static boolean copyFile(File source, File cache) throws IOException{
		if (!cache.exists()) {
			cache.createNewFile();
		}
		
		FileChannel sourceChannel = new FileInputStream(source).getChannel();
		FileChannel cacheChannel = new FileOutputStream(cache).getChannel();
		
		try {
			cacheChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
			sourceChannel.close();
			cacheChannel.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (sourceChannel != null)
				sourceChannel.close();
			
			if (cacheChannel != null)
				cacheChannel.close();
		}
		return false;
	}
	
	public static void cacheGraph(String fileName, int dirIdx, boolean kept) {
		File f;
		if (dirIdx == 0) {
			f = new File(MIBConfiguration.getInstance().getTemplateDir() + "/" + fileName + ".json");
		} else if (dirIdx == 1) {
			f = new File(MIBConfiguration.getInstance().getTestDir() + "/" + fileName + ".json");
		} else {
			f = new File(MIBConfiguration.getInstance().getLabelmapDir() + "/" + fileName + ".json");
		}
		
		if (f.exists()) {
			try {
				String newFileName = StringUtil.genKeyWithId(fileName, UUID.randomUUID().toString());
				File cacheFile = new File(MIBConfiguration.getInstance().getCacheDir() + "/" + newFileName + ".json");
				//String newFileName = StringUtil.genKeyWithId(fileName, String.valueOf(g.getThreadMethodId()));
				if (kept) {
					if (!copyFile(f, cacheFile)) {
						logger.warn("Warning: fail to copy file: " + f.getName());
					}
				} else {
					if (!f.renameTo(new File(MIBConfiguration.getInstance().getCacheDir() + "/" + newFileName + ".json"))) {
						logger.warn("Warning: fail to move file: " + f.getName());
					}
				}
			} catch (Exception ex) {
				logger.error("Exception: ", ex);
			}
		}
	}
	
	public static void cacheAllGraphs(Map<String, List<GraphTemplate>> allGraphs, String...name) {
		if (allGraphs.size() == 0)
			return ;
		
		try {
			String zipFileName = null;
			if (name.length == 0) {
				Date d = new Date();
				SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd-HH-mm-ss-SSS");
				zipFileName = MIBConfiguration.getInstance().getCacheDir() + "/" + formatter.format(d) + ".zip";
			} else {
				zipFileName = MIBConfiguration.getInstance().getCacheDir() + "/" + name[0] + ".zip" ;
			}
			
			FileOutputStream fos = new FileOutputStream(zipFileName, true);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(bos);
			for (String shortKey: allGraphs.keySet()) {
				GsonManager.cacheDirectGraphs(shortKey, allGraphs.get(shortKey), zos);
			}
			zos.close();
			fos.write(bos.toByteArray());
			bos.close();
			fos.flush();
			try {
				fos.close();
			} catch (Exception ex) {
				logger.error("Exception: ", ex);
			}
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public static void cacheNameMap(NameMap nameMap, ZipOutputStream zos) {
		TypeToken<NameMap> nameToken = new TypeToken<NameMap>(){};
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		Gson gson = gb.enableComplexMapKeySerialization().create();
		String toWrite = gson.toJson(nameMap, nameToken.getType());
		
		try {
			ZipEntry zipEntry = new ZipEntry("nameMap");
			zos.putNextEntry(zipEntry);
			zos.write(toWrite.getBytes(Charset.forName("UTF-8")));
			zos.closeEntry();
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public static void cacheDirectGraphs(String fileName, List<GraphTemplate> graphs, ZipOutputStream zos) {
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(InstNode.class, new InstNodeAdapter());
		Gson gson = gb.enableComplexMapKeySerialization().create();
		
		try {
			for (GraphTemplate g: graphs) {
				String fullFileName = StringUtil.genKeyWithId(fileName, String.valueOf(g.getThreadMethodId())) + ".json";
				String toWrite = gson.toJson(g, graphToken.getType());
				ZipEntry zipEntry = new ZipEntry(fullFileName);
				zos.putNextEntry(zipEntry);
				zos.write(toWrite.getBytes(Charset.forName("UTF-8")));
				zos.closeEntry();
				//writeJsonGeneric(g, fullFileName, graphToken, MIBConfiguration.CACHE_DIR);
			}
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
		
	public static <T> T readJsonGeneric(File f, TypeToken typeToken) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(InstNode.class, new InstNodeAdapter());
		//Gson gson = gb.enableComplexMapKeySerialization().create();
		Gson gson = gb.create();
		try {
			JsonReader jr = new JsonReader(new FileReader(f));
			T ret = gson.fromJson(jr, typeToken.getType());
			jr.close();
			return ret;
		} catch (Exception ex) {
			logger.error("Excpetion: ", ex);
		}
		return null;
	}
		
	private static void cleanHelper(String fileName) {
		File dir = new File(fileName);
		if (!dir.isDirectory()) {
			dir.delete();
		} else {
			for (File f: dir.listFiles()) {
				cleanHelper(f.getAbsolutePath());
			}
		}
	}
	
	public static void cleanDir(File dir) {
		if (!dir.isDirectory()) {
			dir.delete();
			dir.mkdir();
		} else {
			cleanHelper(dir.getAbsolutePath());
		}
	}
	
	public static void cleanDirs(boolean cleanTemp, boolean cleanTest) {
		File tempDir = new File(MIBConfiguration.getInstance().getTemplateDir());
		File teDir = new File(MIBConfiguration.getInstance().getTestDir());
		//File cacheDir = new File(MIBConfiguration.getInstance().getCacheDir());
		File pathDir = new File(MIBConfiguration.getInstance().getPathDir());
		File costDir = new File(MIBConfiguration.getInstance().getCostTableDir());
		File labelmapDir = new File(MIBConfiguration.getInstance().getLabelmapDir());
		
		if (cleanTemp)
			cleanDir(tempDir);
		
		if (cleanTest)
			cleanDir(teDir);
		
		//No matter what, clean cache
		//cleanDir(cacheDir);
		cleanDir(pathDir);
		cleanDir(costDir);
		//cleanDir(labelmapDir);
	}
	
	public static void writeResult(String fileName, StringBuilder sb) {
		writeResult(fileName, sb.toString());
	}
	
	public static void writeResult(String fileName, String resultString) {
		//Date now = new Date();
		//String name = MIBConfiguration.getInstance().getResultDir() + "/" + compareName + now.getTime() + ".csv"; 
		File result = new File(fileName);
		
		try {
			if (!result.exists())
				result.createNewFile();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(result, true));
			bw.write(resultString);
			bw.close();
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}

}