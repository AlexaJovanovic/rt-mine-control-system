package com.prv;

import com.prv.rt_system.MCU;

public class EnvironmentState {
    private static EnvironmentState instance;   // singleton instance

 // fields (private, with camelCase)
    private float coConcentration;
    private float ch4Concentration;
    private float airFlow;
    private float spontanetusWaterFlow;
    private volatile float pumpWaterFlow = 0f;
    private float waterLevel;
    
    private float timePassed_ms = 0f;
    
    private boolean pumpIsOn = false;
    private boolean pumpWorkingProperly = true;

    // getters
    public float getCoConcentration() {
        return coConcentration;
    }

    public float getCh4Concentration() {
        return ch4Concentration;
    }

    public float getAirFlow() {
        return airFlow;
    }

    public float getPumpWaterFlow() {
        return pumpWaterFlow;
    }

    public float getWaterLevel() {
        return waterLevel;
    }
    
    public void turnPumpOn() {
    	if (pumpWorkingProperly) {
    		this.pumpIsOn = true;
        	this.pumpWaterFlow = EnvConfig.PUMP_WATER_FLOW;
    	}
    	
    }
    
    public void turnPumpOff() {
    	if (pumpWorkingProperly) {
    		this.pumpIsOn = false;
    		this.pumpWaterFlow = 0;
    	}
    }
    
    public void setPumpIsOn(boolean val) {
    	if (val == true)
    		this.turnPumpOn();
    	else
    		this.turnPumpOff();
    }

    public void setPumpWorkingProperly(boolean val) {
    	this.pumpWorkingProperly = val;
    }
    
    // private constructor
    private EnvironmentState(float CO, float CH4, float air, float extWater, float level) {
        this.coConcentration = CO;
        this.ch4Concentration = CH4;
        this.airFlow = air;
        this.spontanetusWaterFlow = extWater;
        this.waterLevel = level;
    }

    // Singleton getter
    public static EnvironmentState getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Environment not initialized!");
        }
        return instance;
    }

    // Initialization method
    public static void initialize() {
        if (instance == null) {
            instance = new EnvironmentState(EnvConfig.INITIAL_CO_CONCENTRATION, EnvConfig.INITIAL_CH4_CONCENTRATION, EnvConfig.INITIAL_AIR_FLOW, EnvConfig.WATER_FILLING_RATE, EnvConfig.HIGH_WATER_LEVEL);
        }
    }

    // Dynamics update
    public void update(float dt_ms) {
    	timePassed_ms += dt_ms;
    	
        float totalWaterFlow = spontanetusWaterFlow;
        if (this.pumpIsOn) totalWaterFlow += pumpWaterFlow;
        
        waterLevel += (totalWaterFlow) * dt_ms * 0.001;
        if (waterLevel > 100) waterLevel = 100;
        
        // System.out.printf("%f %f\n", totalWaterFlow, pumpWaterFlow);
        
        coConcentration = EnvConfig.coConcentrationFunction(timePassed_ms);
        ch4Concentration = EnvConfig.ch4ConcentrationFunction(timePassed_ms);

        if (waterLevel < 0) waterLevel = 0;
        
        if (waterLevel >= EnvConfig.HIGH_WATER_LEVEL) MCU.instance.EXTIWaterLevelHigh();
        if (waterLevel <= EnvConfig.LOW_WATER_LEVEL) MCU.instance.EXTIWaterLevelLow();
    }

    @Override
    public String toString() {
        return String.format("CO=%.2f, CH4=%.2f, Air=%.2f, ExtWater=%.2f, Level=%.2f",
        		coConcentration, ch4Concentration, airFlow, spontanetusWaterFlow,
        		waterLevel);
    }
}
