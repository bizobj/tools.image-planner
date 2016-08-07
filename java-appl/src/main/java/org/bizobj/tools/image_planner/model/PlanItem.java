package org.bizobj.tools.image_planner.model;

import java.math.BigDecimal;

public class PlanItem {
	/** Image file name, as the identity of a plan item */
	private String fileName;
	/** Main module of a plan item */
	private String mainModule;
	/** Sub-module of a plan item */
	private String subModule;
	/** Task name */
	private String taskName;
	/** percentage of UI (user interface) */
	private double percent01UI;
	/** percentage of business logic */
	private double percent02Biz;
	/** percentage of UE (User Experience) */
	private double percent03UE;
	/** percentage of the final works */
	private double percent04Misc;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getMainModule() {
		return mainModule;
	}
	public void setMainModule(String mainModule) {
		this.mainModule = mainModule;
	}
	public String getSubModule() {
		return subModule;
	}
	public void setSubModule(String subModule) {
		this.subModule = subModule;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public double getPercent01UI() {
		return percent01UI;
	}
	public void setPercent01UI(double percent01ui) {
		percent01UI = percent01ui;
	}
	public double getPercent02Biz() {
		return percent02Biz;
	}
	public void setPercent02Biz(double percent02Biz) {
		this.percent02Biz = percent02Biz;
	}
	public double getPercent03UE() {
		return percent03UE;
	}
	public void setPercent03UE(double percent03ue) {
		percent03UE = percent03ue;
	}
	public double getPercent04Misc() {
		return percent04Misc;
	}
	public void setPercent04Misc(double percent04Misc) {
		this.percent04Misc = percent04Misc;
	}
	
	public BigDecimal getPercent(){
		double avg = (getPercent01UI()+getPercent02Biz()+getPercent03UE()+getPercent04Misc())/4;
		return new BigDecimal(avg);
	}
	
	/**
	 * Build {@link PlanItem} from file name: [Seq No.]-[Module.SubModule]-[TaskName]
	 * @param name
	 * @return
	 */
	public static PlanItem buildFromFileName(String name){
		PlanItem pi = new PlanItem();
		pi.setFileName(name);
		
		//Split first "-" to remove [Seq No.] part
		int pos = name.indexOf('-');
		if (pos<=0){
			throw new RuntimeException("File name pattern error: MUST be '[Seq No.]-[Module.SubModule]-[TaskName].png'");
		}
		name = name.substring(pos+1);
		
		//From second '-' to split [[Module.SubModule] and [TaskName]
		pos = name.indexOf('-');
		if (pos<=0){
			throw new RuntimeException("File name pattern error: MUST be '[Seq No.]-[Module.SubModule]-[TaskName].png'");
		}
		String modules = name.substring(0, pos);
		String taskName = name.substring(pos+1);
		
		//Split [Module] and [SubModule]
		if (modules.contains(".")){
			pos = modules.indexOf('.');
			pi.setMainModule(modules.substring(0, pos));
			pi.setSubModule(modules.substring(pos+1));
		}else{
			//No "." to define sub-module, so it's the same as main-module
			pi.setMainModule(modules);
			pi.setSubModule(modules);
		}
		
		//Remove file extension then get the task name
		pos = taskName.lastIndexOf('.');
		if (pos>0){
			taskName = taskName.substring(0, pos);
		}
		pi.setTaskName(taskName);
		
		return pi;
	}
}
