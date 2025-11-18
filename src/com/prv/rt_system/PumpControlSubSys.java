package com.prv.rt_system;

import com.prv.EnvConfig;
import com.prv.EnvironmentState;
import com.prv.rt_system.ControlSystem.AlarmType;

public class PumpControlSubSys {
	private ControlSystem controlSystem;
	private EnvGUI gui;
	
	public PeriodicTask pumpController;
	public PeriodicTask waterFlowMonitor;
	
	PumpControlSubSys(ControlSystem ctrlSys, EnvGUI envGUI) {
		this.controlSystem = ctrlSys;
		this.gui = envGUI;
		
		pumpController = new PeriodicTask(140, this::pumpControllerTask, 6);
		waterFlowMonitor = new PeriodicTask(140, this::pumpWaterFlowMonitorTask, 7);
		
		pumpController.start();
		waterFlowMonitor.start();
	}
	
	public void shutdown() {
    	this.pumpController.shutdown();
    	this.waterFlowMonitor.shutdown();
    }
    
    private boolean pumpIsOn = false; 
    
    private volatile boolean operatorSignalOn = false;
    private volatile boolean operatorSignalOff = false;

    private boolean waterLevelHighFlag = false;
    private boolean waterLevelLowFlag = false;
    
    public void setManualControl(boolean pump_on) {
    	operatorSignalOn = pump_on;
    	operatorSignalOff = !pump_on;
    }
    
    public void setwaterLevelHighFlag() {
    	waterLevelHighFlag = true;
    }
    public void setwaterLevelLowFlag() {
    	waterLevelLowFlag = true;
    }
    
	public void pumpControllerTask() {
		// pump should never run if CH4 concentration is to high
		if (controlSystem.getCH4Concentration() > EnvConfig.CH4_CONCENTRATION_LIMIT) {
    		if (this.pumpIsOn) {
    			this.turnPumpOff();
        		gui.log("PUMP OFF - CH4 CONCENTRATION TOO HIGH");
    		}
    		return;
    	}
    	
    	if (operatorSignalOn) {
    		// clear flag
    		operatorSignalOn = false;
    		this.turnPumpOn();
    		gui.log("PUMP ON - OPERATOR SIGNAL");

    	}
    	if (operatorSignalOff) {
    		// clear flag
    		operatorSignalOff = false;
    		this.turnPumpOff();
    		gui.log("PUMP OFF - OPERATOR SIGNAL");
    	}
		
    	if (waterLevelHighFlag) {
    		waterLevelHighFlag = false;
    		
    		if (!pumpIsOn) {
    			this.turnPumpOn();
    			gui.log("PUMP ON - WATER LEVEL HIGH");
    		}
    	}
    	if (waterLevelLowFlag) {
    		waterLevelLowFlag = false;
    		
    		if (pumpIsOn) {
    			this.turnPumpOff();
    			gui.log("PUMP OFF - WATER LEVEL LOW");
    		}
    	}
    	
    	EnvironmentState.getInstance().setPumpIsOn(this.pumpIsOn);
    }
	
	
    // helper
    public void turnPumpOn() {
		pumpIsOn = true;
    	EnvironmentState.getInstance().turnPumpOn();
    	gui.setPumpState(true);
    }
    public void turnPumpOff() {
    	pumpIsOn = false;
    	EnvironmentState.getInstance().turnPumpOff();
    	gui.setPumpState(false);
    }
    
    
	int N = 0;
	final int periodsToWait = 9;
    public void pumpWaterFlowMonitorTask() {
    	float measuredFlow = this.controlSystem.getPumpWaterFlow();
    	
    	if (pumpIsOn && measuredFlow >= 0) { // pump on but no water
        	N++;
    		if (N >= periodsToWait) controlSystem.soundAnAlarm(AlarmType.PUMP_ON_NO_FLOW);
        } 
        else if (!pumpIsOn && measuredFlow < 0) { // pump off but water is flowing
        	N++;
    		if (N >= periodsToWait) controlSystem.soundAnAlarm(AlarmType.PUMP_OFF_FLOW);
        }
        else {
        	// everything is in regular state
        	controlSystem.clearAlarm(AlarmType.PUMP_ON_NO_FLOW);
    		controlSystem.clearAlarm(AlarmType.PUMP_OFF_FLOW);
    		
        	N = 0;
        }
    }
}
