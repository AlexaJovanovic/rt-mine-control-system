package com.prv.rt_system;

import java.util.HashSet;
import java.util.Set;

import com.prv.EnvConfig;
import com.prv.EnvironmentState;
import com.prv.rt_system.ADC.ADCStatus;

public class ControlSystem extends Thread {
	
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
        PeriodicTask coReader = new PeriodicTask(READER_TASK_PERIOD_MS, () -> this.coReaderTask(), 2);
        MCU.instance.adc1_co.startConversion();
        coReader.start();
        
        PeriodicTask ch4Reader = new PeriodicTask(READER_TASK_PERIOD_MS, () -> this.ch4ReaderTask(), 4);
        MCU.instance.adc2_ch4.startConversion();
        ch4Reader.start();
        
        PeriodicTask airFlowReader = new PeriodicTask(READER_TASK_PERIOD_MS, () -> this.airFlowReaderTask(), 3);
        MCU.instance.adc3_af.startConversion();
        airFlowReader.start();
        
        PeriodicTask waterFlowReader = new PeriodicTask(READER_TASK_PERIOD_MS, this::pumpWaterFlowReaderTask, 5);
        MCU.instance.adc4_wf.startConversion();
        waterFlowReader.start();
        
        PeriodicTask loggerTask = new PeriodicTask(1000, this::loggerTask, 1);
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
        
        coReader.interrupt();
        ch4Reader.interrupt();
        airFlowReader.interrupt();
        waterFlowReader.interrupt();
        loggerTask.interrupt();
        
        double pumpCtrlWCET = pumpControlSubSys.pumpController.getMaxExecTimeMs();
        double pumpMotiorWCET = pumpControlSubSys.waterFlowMonitor.getMaxExecTimeMs();
        

        System.out.printf("coReader max_exec_time:%f\n", coReader.getMaxExecTimeMs());
        System.out.printf("ch4Reader max_exec_time:%f\n", ch4Reader.getMaxExecTimeMs());
        System.out.printf("airFlowReader max_exec_time:%f\n", airFlowReader.getMaxExecTimeMs());
        System.out.printf("waterFlowReader max_exec_time:%f\n", waterFlowReader.getMaxExecTimeMs());
        System.out.printf("loggerTask max_exec_time:%f\n", loggerTask.getMaxExecTimeMs());
        System.out.printf("pumpController max_exec_time:%f\n", pumpCtrlWCET);
        System.out.printf("waterFlowMonitor max_exec_time:%f\n", pumpMotiorWCET);
        
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

	public void EXTIWaterLevelHigh() {
		if (this.pumpControlSubSys != null)
			this.pumpControlSubSys.setwaterLevelHighFlag();
	}
	public void EXTIWaterLevelLow() {
		if (this.pumpControlSubSys != null)
			this.pumpControlSubSys.setwaterLevelLowFlag();
	}
    
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
    
    private volatile ADCStatus prevCoReaderStatus = ADCStatus.DATA_READY;
    public void coReaderTask() {
    	ADCStatus adc_status = MCU.instance.adc1_co.getStatus();

		if (adc_status != ADCStatus.DATA_READY) {
    		
    		if (prevCoReaderStatus != ADCStatus.DATA_READY) {
        		// two malfunctions in a row -> sound an alarm

        		this.soundAnAlarm(AlarmType.CO_SENSOR_FAULT);
    		}
    	}
    	else {
			this.clearAlarm(AlarmType.CO_SENSOR_FAULT);
    		
			this.coConcentration = MCU.instance.adc1_co.getValue();
    		
    		if (this.coConcentration > EnvConfig.CO_CONCENTRATION_LIMIT) {
    			this.soundAnAlarm(AlarmType.CO_CONCENTRATION_TOO_HIGH);
    		}
    		else this.clearAlarm(AlarmType.CO_CONCENTRATION_TOO_HIGH);
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
			this.clearAlarm(AlarmType.CH4_SENSOR_FAULT);
			
    		this.ch4Concentration = MCU.instance.adc2_ch4.getValue();
    		if (this.ch4Concentration > EnvConfig.CH4_CONCENTRATION_LIMIT) {
    			// two malfunctions in a row -> sound an alarm
    			this.soundAnAlarm(AlarmType.CH4_CONCENTRATION_TOO_HIGH);
    		}
    		else this.clearAlarm(AlarmType.CH4_CONCENTRATION_TOO_HIGH);
    	}
    	
    	prevCh4ReaderStatus = adc_status;
    	
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
			this.clearAlarm(AlarmType.AIR_FLOW_SENSOR_FAULT);
			
    		this.airFlow = MCU.instance.adc3_af.getValue();
    		if (this.airFlow < EnvConfig.AIR_FLOW_LIMIT) {
    			this.soundAnAlarm(AlarmType.AIR_FLOW_TOO_LOW);
    		}
    		else this.clearAlarm(AlarmType.AIR_FLOW_TOO_LOW);
    	}
    	prevAirFlowReaderStatus = adc_status;
    	
    	// period displacement
    	MCU.instance.adc3_af.startConversion();
    }
    
    private ADCStatus prevWaterFlowReaderStatus = ADCStatus.DATA_READY;
    public void pumpWaterFlowReaderTask() {
    	ADCStatus adc_status = MCU.instance.adc4_wf.getStatus();
    	if (adc_status != ADCStatus.DATA_READY) {
    		if (prevWaterFlowReaderStatus != ADCStatus.DATA_READY) {
    			this.soundAnAlarm(AlarmType.WATER_FLOW_SENSOR_FAULT);
    		}
    	}
    	else {
    		this.clearAlarm(AlarmType.WATER_FLOW_SENSOR_FAULT);
    		
    		this.waterFlow = MCU.instance.adc4_wf.getValue();
    	}
    	prevWaterFlowReaderStatus = adc_status;
    	
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
