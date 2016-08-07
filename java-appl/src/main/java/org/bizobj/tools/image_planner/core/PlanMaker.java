package org.bizobj.tools.image_planner.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.bizobj.tools.image_planner.model.PlanItem;
import org.bizobj.tools.image_planner.model.ReportItem;
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
	 * @param reportFile 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws InvalidFormatException 
	 */
	public static void make(File imgDir, File dataXlsx, File reportFile) throws IOException, InvalidFormatException, SAXException{
		if (! imgDir.exists()){
			throw new RuntimeException("Images' directory '"+imgDir.getCanonicalPath()+"' not existed!");
		}
		
		//Read plan from images
		List<PlanItem> plans = readImages(imgDir);
		log.info("Read images success, with "+plans.size()+" items.");
		//Read plan(with percentage data) from data excel file
		List<PlanItem> lostedPlans = new ArrayList<>();
		if (dataXlsx.exists()){
			List<PlanItem> plansWithData = readDataXlsx(dataXlsx);
			copyPercentageData(plans, plansWithData);
			lostedPlans = findLostedPlans(plans, plansWithData);
			log.info("Read plan state success, with "+plansWithData.size()+" items, and "+lostedPlans.size()+" items are lost.");
		}
		
		writeToDataXlsx(plans, lostedPlans, dataXlsx);
		log.info("Write new plan data to '"+dataXlsx.getCanonicalPath()+"' success.");
		
		writeHtmlReport(plans, reportFile);
		log.info("Write plan report to '"+reportFile.getCanonicalPath()+"' success.");
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
	
	private static void writeHtmlReport(List<PlanItem> plans, File htmlFile) throws IOException{
		int id=1;
		List<ReportItem> rptItems = new ArrayList<>();
		ReportItem lastModule = null;
		ReportItem lastSubMdl = null;
		for (PlanItem plan: plans){
			String module = plan.getMainModule();
			if ( (null==lastModule) || (! lastModule.getName().equals(module)) ){
				ReportItem moduleItem = new ReportItem();
				moduleItem.setId(id++);
				moduleItem.setName(module);
				Number[] statData = moduleStat(module, plans);
				moduleItem.setPoints((Integer)statData[0]);
				moduleItem.setPercentage(((BigDecimal)statData[1]).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN));
				moduleItem.setType("module");
				rptItems.add(moduleItem);
				//Remember prevModule
				lastModule = moduleItem;
			}
			String subModule = plan.getSubModule();
			if ( lastModule.getName().equals(module) &&
					( (null==lastSubMdl) || (! lastSubMdl.getName().equals(subModule)) ) ){
				ReportItem submItem = new ReportItem();
				submItem.setId(id++);
				submItem.setName(subModule);
				Number[] statData = subModuleStat(module, subModule, plans);
				submItem.setPoints((Integer)statData[0]);
				submItem.setPercentage(((BigDecimal)statData[1]).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN));
				submItem.setParentId(lastModule.getId());
				submItem.setType("sub-module");
				rptItems.add(submItem);
				//Remember prevModule
				lastSubMdl = submItem;
			}
			ReportItem taskItem = new ReportItem();
			taskItem.setId(id++);
			taskItem.setName(plan.getTaskName());
			taskItem.setPercentage(plan.getPercent().multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN));
			taskItem.setParentId(lastSubMdl.getId());
			taskItem.setImageUrl("images/" + URLEncoder.encode(plan.getFileName(), "UTF-8"));
			taskItem.setType("task");
			rptItems.add(taskItem);
		}
		
		Number[] statData = globalStat(plans);
		int statPoints = (Integer)statData[0];
		BigDecimal statPercent = ((BigDecimal)statData[1]).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN);
		
		String tmpl = IOUtils.toString(PlanMaker.class.getResourceAsStream("report.template.html"), "UTF-8");
		Properties p = new Properties();
		VelocityEngine ve = new VelocityEngine(p);
		VelocityContext context = new VelocityContext();
		String jqueryRes = "/frontend/jquery-1.12.4/dist/jquery.js";
		context.put("jqueryJavascript",
				IOUtils.toString(PlanMaker.class.getResourceAsStream(jqueryRes), "UTF-8"));
		String treegridRes = "/frontend/jquery-treegrid-0.3.0/js/jquery.treegrid.js";
		context.put("jqueryTreegridJavascript",
				IOUtils.toString(PlanMaker.class.getResourceAsStream(treegridRes), "UTF-8"));
		context.put("reportItems", rptItems);
		context.put("statPoints", statPoints);
		context.put("statPercent", statPercent);
		StringWriter sw = new StringWriter();
		ve.evaluate(context, sw, "report", tmpl);
		String html = sw.toString();
		FileUtils.write(htmlFile, html, "UTF-8");
	}

	private static Number[] subModuleStat(String module, String subModule, List<PlanItem> plans) {
		int points = 0;
		double percents = 0.0;
		for(PlanItem plan: plans){
			if (module.equals(plan.getMainModule()) && subModule.equals(plan.getSubModule())){
				points = points+1;
				percents = percents + plan.getPercent01UI()+plan.getPercent02Biz()+plan.getPercent03UE()+plan.getPercent04Misc();
			}
		}
		return new Number[]{new Integer(points), new BigDecimal(percents/points/4)};
	}

	private static Number[] moduleStat(String module, List<PlanItem> plans) {
		int points = 0;
		double percents = 0.0;
		for(PlanItem plan: plans){
			if (module.equals(plan.getMainModule())){
				points = points+1;
				percents = percents + plan.getPercent01UI()+plan.getPercent02Biz()+plan.getPercent03UE()+plan.getPercent04Misc();
			}
		}
		return new Number[]{new Integer(points), new BigDecimal(percents/points/4)};
	}

	private static Number[] globalStat(List<PlanItem> plans) {
		int points = 0;
		double percents = 0.0;
		for(PlanItem plan: plans){
			points = points+1;
			percents = percents + plan.getPercent01UI()+plan.getPercent02Biz()+plan.getPercent03UE()+plan.getPercent04Misc();
		}
		return new Number[]{new Integer(points), new BigDecimal(percents/points/4)};
	}
}
