package org.bizobj.tools.image_planner;

import java.io.File;

import org.apache.log4j.Logger;
import org.bizobj.tools.image_planner.core.PlanMaker;

/**
 * The main entry
 */
public class Main {
	private static final Logger log = Logger.getLogger(Main.class);
	
	/**
	 * Start application, with the only one argument - the data path.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length<1){
			throw new RuntimeException("Please indicate the first argument - the data path");
		}
		String dataPath = args[0];
		File dataDir = new File(dataPath);
		if (! dataDir.exists()){
			throw new RuntimeException("The data path '"+dataPath+"' is not exites");
		}
		if (! dataDir.isDirectory()){
			throw new RuntimeException("The data path '"+dataPath+"' MUST be a directory");
		}
		
		log.info("Start "+Main.class.getName()+", with data path = '"+dataDir.getCanonicalPath()+"'");
		
		File imagesDir = new File(dataDir, "images");
		log.info("Read images from: "+imagesDir.getCanonicalPath());
		File dataFile = new File(dataDir, "plan.xlsx");
		log.info("Read and write new plan to: "+dataFile.getCanonicalPath());
		File reportFile = new File(dataDir, "plan.html");
		log.info("Write new plan report to: "+reportFile.getCanonicalPath());

		PlanMaker.make(imagesDir, dataFile, reportFile);
		log.info("SUCCESS: "+Main.class.getName()+".");
	}

}
