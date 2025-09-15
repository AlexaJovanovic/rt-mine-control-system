package com.prv.rt_system;

import com.prv.EnvironmentState;
import com.prv.rt_system.ControlSystem.AlarmType;

public class PumpControlSubSys {
	private ControlSystem controlSystem;
	private EnvGUI gui;
	
	PumpControlSubSys(ControlSystem ctrlSys, EnvGUI envGUI) {
		this.controlSystem = ctrlSys;
		this.gui = envGUI;
		
		PeriodicTask pumpController = new PeriodicTask(100, this::pumpControllerTask);
		PeriodicTask waterFlowMonitor = new PeriodicTask(100, this::pumpWaterFlowMonitorTask);
		
		pumpController.start();
		waterFlowMonitor.start();
	}

    private boolean pumpIsOn = false; 
    
    private volatile boolean operatorSignalOn = false;
    private volatile boolean operatorSignalOff = false;

    private boolean waterLevelHigh = false;
    private boolean waterLevelLow = false;
    
    public void setManualControl(boolean pump_on) {
    	operatorSignalOn = pump_on;
    	operatorSignalOff = !pump_on;
    }
    
	public void pumpControllerTask() {
		// pump should never run if CH4 concentration is to high
		if (controlSystem.getCH4Concentration() > ControlSystem.CH4_CONCENTRATION_LIMIT && pumpIsOn) {
    		this.turnPumpOff();
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
		
    	if (waterLevelHigh) {
    		this.turnPumpOn();

    		gui.log("PUMP OFF - WATER LEVEL HIGH");
    	}
    	if (waterLevelLow) {
    		this.turnPumpOff();

    		gui.log("PUMP OFF - WATER LEVEL LOW");
    	}
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
    public void pumpWaterFlowMonitorTask() {
    	float measuredFlow = this.controlSystem.getPumpWaterFlow();
    	
    	if (pumpIsOn && measuredFlow >= 0) { // pump on but no water
        	N++;
    		if (N >= 3) controlSystem.soundAnAlarm(AlarmType.PUMP_ON_NO_FLOW);
        } 
        else if (!pumpIsOn && measuredFlow < 0) { // pump off but water is flowing
        	N++;
    		if (N >= 3) controlSystem.soundAnAlarm(AlarmType.PUMP_OFF_FLOW);
        }
        else {
        	// everything is in regular state
        	controlSystem.clearAlarm(AlarmType.PUMP_ON_NO_FLOW);
    		controlSystem.clearAlarm(AlarmType.PUMP_OFF_FLOW);
    		
        	N = 0;
        }
    }
}
