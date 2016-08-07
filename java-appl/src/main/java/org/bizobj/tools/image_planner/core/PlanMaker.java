package org.bizobj.tools.image_planner.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.bizobj.tools.image_planner.model.PlanItem;
import org.jxls.common.Context;
import org.jxls.reader.ReaderBuilder;
import org.jxls.reader.XLSReadStatus;
import org.jxls.reader.XLSReader;
import org.jxls.util.JxlsHelper;
import org.xml.sax.SAXException;

public class PlanMaker {
	private static final Logger log = Logger.getLogger(PlanMaker.class);
	
	/**
	 * Read all images and the data from excel file(if exists), compute the new plan states, and output it to plan html file 
	 * @param imgDir
	 * @param dataXlsx
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws InvalidFormatException 
	 */
	public static void make(File imgDir, File dataXlsx) throws IOException, InvalidFormatException, SAXException{
		if (! imgDir.exists()){
			throw new RuntimeException("Images' directory '"+imgDir.getCanonicalPath()+"' not existed!");
		}
		
		//Read plan from images
		List<PlanItem> plans = readImages(imgDir);
		log.info("Read images success, with "+plans.size()+"items.");
		//Read plan(with percentage data) from data excel file
		List<PlanItem> lostedPlans = new ArrayList<>();
		if (dataXlsx.exists()){
			List<PlanItem> plansWithData = readDataXlsx(dataXlsx);
			copyPercentageData(plans, plansWithData);
			lostedPlans = findLostedPlans(plans, plansWithData);
			log.info("Read plan state success, with "+plansWithData.size()+"items, and "+lostedPlans.size()+" items are lost.");
		}
		
		writeToDataXlsx(plans, lostedPlans, dataXlsx);
		log.info("Write new plan data to '"+dataXlsx.getCanonicalPath()+"' success.");
	}
	
	private static List<PlanItem> findLostedPlans(List<PlanItem> plans, List<PlanItem> plansWithData) {
		List<PlanItem> result = new ArrayList<>();
		for(PlanItem data: plansWithData){
			String dataFileName = data.getFileName();
			PlanItem plan = getByFileName(plans, dataFileName);
			if (null==plan){
				result.add(data);
			}
		}
		return result;
	}

	private static void copyPercentageData(List<PlanItem> plans, List<PlanItem> plansWithData) {
		for(PlanItem plan: plans){
			String fileName = plan.getFileName();
			PlanItem data = getByFileName(plansWithData, fileName);
			if (null!=data){
				plan.setPercent01UI(data.getPercent01UI());
				plan.setPercent02Biz(data.getPercent02Biz());
				plan.setPercent03UE(data.getPercent03UE());
				plan.setPercent04Misc(data.getPercent04Misc());
			}
		}
	}
	
	private static PlanItem getByFileName(List<PlanItem> plans, String fileName){
		for(PlanItem plan: plans){
			if (fileName.equalsIgnoreCase(plan.getFileName())){
				return plan;
			}
		}
		return null;
	}

	private static List<PlanItem> readDataXlsx(File dataXlsx) throws IOException, SAXException, InvalidFormatException {
		InputStream inputXML = new BufferedInputStream(PlanMaker.class.getResourceAsStream("template.reader.xml"));
	    XLSReader mainReader = ReaderBuilder.buildFromXML( inputXML );
	    InputStream inputXLS = new BufferedInputStream(new FileInputStream(dataXlsx));
	    List<PlanItem> plans = new ArrayList<>();
	    Map<String, Object> beans = new HashMap<>();
	    beans.put("plans", plans);
	    XLSReadStatus readStatus = mainReader.read( inputXLS, beans);
	    if (readStatus.isStatusOK()){
	    	@SuppressWarnings("unchecked")
			List<PlanItem> plansData = (List<PlanItem>) beans.get("plans");
			return plansData;
	    }else{
	    	throw new RuntimeException(
	    			"Read '"+dataXlsx.getCanonicalPath()+"' with problem: "
	    	       +StringUtils.join(readStatus.getReadMessages(), "; "));
	    }
	}

	private static List<PlanItem> readImages(File imgDir){
		List<PlanItem> result = new ArrayList<>();
		
		String[] files = imgDir.list();
		Arrays.sort(files);
		
		for (int i = 0; i < files.length; i++) {
			String fileName = files[i];
			PlanItem pi = PlanItem.buildFromFileName(fileName);
			result.add(pi);
		}
		
		return result;
	}
	
	private static void writeToDataXlsx(List<PlanItem> plans, List<PlanItem> lostedPlans, File dataXlsx) throws IOException{
		try(InputStream is = PlanMaker.class.getResourceAsStream("template.xlsx")) {
	        try (OutputStream os = new FileOutputStream(dataXlsx)) {
	            Context context = new Context();
	            context.putVar("planItems", plans);
	            context.putVar("lostedPlanItems", lostedPlans);
	            JxlsHelper.getInstance().processTemplate(is, os, context);
	        }
	    }
	}
}
