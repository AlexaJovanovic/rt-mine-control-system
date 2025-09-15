package com.prv.rt_system;

import java.util.HashSet;
import java.util.Set;

import com.prv.EnvironmentState;
import com.prv.rt_system.ADC.ADCStatus;

public class ControlSystem extends Thread {
	public static float CO_CONCENTRATION_LIMIT = 1.0f;
	public static float CH4_CONCENTRATION_LIMIT = 1.0f;
	public static float AIR_FLOW_LIMIT = 0.1f;
	
	
	public static int READER_TASK_PERIOD_MS = 150;
	
    private boolean running = true;
    private PumpControlSubSys pumpControlSubSys = null;
    
    private EnvGUI gui;
    
    public void setGui(EnvGUI eGUI) {
    	gui = eGUI;
    }
    
    public enum AlarmType {
    	// sensor faults
    	CO_SENSOR_FAULT,
    	CH4_SENSOR_FAULT,
    	AIR_FLOW_SENSOR_FAULT,
        WATER_FLOW_SENSOR_FAULT,
        
        // physical quantities exceed thresholds
        CO_CONCENTRATION_TOO_HIGH,
        CH4_CONCENTRATION_TOO_HIGH,
        AIR_FLOW_TOO_LOW,
        
        // pump malfunctions
    	PUMP_ON_NO_FLOW,
        PUMP_OFF_FLOW
    }
    
    @Override
    public void run() {
        System.out.println("Control system started");
        
        // sensor initialization
        PeriodicTask coReader = new PeriodicTask(READER_TASK_PERIOD_MS, () -> this.coReaderTask());
        MCU.instance.adc1_co.startConversion();
        coReader.start();
        
        PeriodicTask ch4Reader = new PeriodicTask(READER_TASK_PERIOD_MS, () -> this.ch4ReaderTask());
        MCU.instance.adc2_ch4.startConversion();
        ch4Reader.start();
        
        PeriodicTask airFlowReader = new PeriodicTask(READER_TASK_PERIOD_MS, () -> this.airFlowReaderTask());
        MCU.instance.adc3_af.startConversion();
        airFlowReader.start();
        
        PeriodicTask waterFlowReader = new PeriodicTask(READER_TASK_PERIOD_MS, this::pumpWaterFlowReaderTask);
        MCU.instance.adc4_wf.startConversion();
        waterFlowReader.start();
        
        PeriodicTask loggerTask = new PeriodicTask(1100, this::loggerTask);
        loggerTask.start();
       
        
        this.pumpControlSubSys = new PumpControlSubSys(this, gui);
        
        while (running) {
            try {
                Thread.sleep(1000);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("Control system stopped");
    }

    public void shutdown() {
        running = false;
    }
    
    public float getCH4Concentration() { 
    	return this.ch4Concentration; 
    }
    
    public float getPumpWaterFlow() { 
    	return this.waterFlow; 
    }
    
    // ALARMS MANAGING
    
    private final Set<AlarmType> activeAlarms = new HashSet<>();
    
 // Raise (or re-raise) an alarm
    public void soundAnAlarm(AlarmType type) {
        if (!activeAlarms.contains(type)) {
            activeAlarms.add(type);
            gui.log("⚠ ALARM RAISED: " + type);
            gui.showAlarm(type);
        }

        // TODO: hook into real alarm system (sound buzzer, etc.)
    }

    // Clear/reset an alarm
    public void clearAlarm(AlarmType type) {
    	if (activeAlarms.contains(type)) {
    		activeAlarms.remove(type);
            gui.log("✅ ALARM CLEARED: " + type);
            gui.clearAlarm(type);
        }
    }
    
    public void pumpManualControlSignal(boolean pumpOn) {
    	this.pumpControlSubSys.setManualControl(pumpOn);
    }
    
    private ADCStatus prevCoReaderStatus = ADCStatus.DATA_READY;
    public void coReaderTask() {
    	ADCStatus adc_status = MCU.instance.adc1_co.getStatus();
    	if (adc_status != ADCStatus.DATA_READY) {
    		if (prevCoReaderStatus != ADCStatus.DATA_READY) {
        		// two malfunctions in a row -> sound an alarm
    			this.soundAnAlarm(AlarmType.CO_SENSOR_FAULT);
    		}
    	}
    	else {
    		this.coConcentration = MCU.instance.adc1_co.getValue();
    		
    		if (this.coConcentration > ControlSystem.CO_CONCENTRATION_LIMIT) {
    			this.soundAnAlarm(AlarmType.CO_CONCENTRATION_TOO_HIGH);
    		}
    	}
    	
    	prevCoReaderStatus = adc_status;
    	
    	// period displacement
    	MCU.instance.adc1_co.startConversion();
    }
    
    private ADCStatus prevCh4ReaderStatus = ADCStatus.DATA_READY;
    public void ch4ReaderTask() {
    	ADCStatus adc_status = MCU.instance.adc2_ch4.getStatus();
    	if (adc_status != ADCStatus.DATA_READY) {
    		if (prevCh4ReaderStatus != ADCStatus.DATA_READY) {
        		// two malfunctions in a row -> sound an alarm
    			this.soundAnAlarm(AlarmType.CH4_SENSOR_FAULT);
    		}
    	}
    	else {
    		this.ch4Concentration = MCU.instance.adc2_ch4.getValue();
    		if (this.ch4Concentration > ControlSystem.CH4_CONCENTRATION_LIMIT) {
    			this.soundAnAlarm(AlarmType.CH4_CONCENTRATION_TOO_HIGH);
    		}
    	}
    	
    	prevCoReaderStatus = adc_status;
    	
    	// period displacement
    	MCU.instance.adc2_ch4.startConversion();
    }
    
    private ADCStatus prevAirFlowReaderStatus = ADCStatus.DATA_READY;
    public void airFlowReaderTask() {
    	ADCStatus adc_status = MCU.instance.adc3_af.getStatus();
    	if (adc_status != ADCStatus.DATA_READY) {
    		if (prevAirFlowReaderStatus != ADCStatus.DATA_READY) {
        		// two malfunctions in a row -> sound an alarm
    			this.soundAnAlarm(AlarmType.AIR_FLOW_SENSOR_FAULT);
    		}
    	}
    	else {
    		this.airFlow = MCU.instance.adc3_af.getValue();
    		if (this.airFlow < ControlSystem.AIR_FLOW_LIMIT) {
    			this.soundAnAlarm(AlarmType.AIR_FLOW_TOO_LOW);
    		}
    	}
    	prevCoReaderStatus = adc_status;
    	
    	// period displacement
    	MCU.instance.adc3_af.startConversion();
    }
    
    private ADCStatus prevWaterFlowReaderStatus = ADCStatus.DATA_READY;
    public void pumpWaterFlowReaderTask() {
    	ADCStatus adc_status = MCU.instance.adc4_wf.getStatus();
    	if (adc_status != ADCStatus.DATA_READY) {
    		if (prevWaterFlowReaderStatus != ADCStatus.DATA_READY) {
        		// two malfunctions in a row -> sound an alarm
    		}
    	}
    	else {
    		this.waterFlow = MCU.instance.adc4_wf.getValue();
    	}
    	prevCoReaderStatus = adc_status;
    	
    	// period displacement
    	MCU.instance.adc4_wf.startConversion();
    }
    
    public void loggerTask() {
        gui.log("CO: %.2f%% | CH4: %.2f%% | AirFlow: %.2f m^3/s | WaterFlow: %.2f cm/s | waterLevel: %.2f cm",
                coConcentration, ch4Concentration, airFlow, waterFlow, EnvironmentState.getInstance().getWaterLevel());
    }
    
    private volatile float coConcentration = 0;
    private volatile float ch4Concentration = 0;
    private volatile float airFlow = 0;
    private volatile float waterFlow = 0;
    
}
