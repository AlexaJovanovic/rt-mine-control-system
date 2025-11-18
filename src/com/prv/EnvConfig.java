package com.prv;


// State class that encapsulates configuration parameters for the environment simulation
public final class EnvConfig {
	
    // Gas concentrations (percentages)
    public static final float INITIAL_CO_CONCENTRATION = 0.5f;
    public static final float INITIAL_CH4_CONCENTRATION = 0.1f;
	public static final float CO_CONCENTRATION_LIMIT = 1.0f;
	public static final float CH4_CONCENTRATION_LIMIT = 1.0f;
	
    // Water levels (cm)
    public static final float INITIAL_WATER_LEVEL = 10.0f;
    public static final float LOW_WATER_LEVEL = 10.0f;
    public static final float HIGH_WATER_LEVEL = 20.0f;
    
    // Airflow (m³/s)
    public static final float INITIAL_AIR_FLOW = 1.0f;
    public static final float AIR_FLOW_LIMIT = 0.3f;
	
    // Water flow rates (cm/s)
    public static final float WATER_FILLING_RATE = 1f; 
    public static final float PUMP_WATER_FLOW = -3.0f;

    
    // defining mathematical functions that will cleanly define signal values in time
    public static float coConcentrationFunction(float time_ms) {
    	if (time_ms < 0) return INITIAL_CO_CONCENTRATION;
    	
    	if (time_ms < 2000) return 0.01f;
    	if (time_ms < 8000) return 1.2f;
    			
    	return 0.01f;
    }
    
    public static float ch4ConcentrationFunction(float time_ms) {
    	return stepFunction(time_ms) * INITIAL_CH4_CONCENTRATION + 1.2f * (stepFunction(time_ms - 2000)-stepFunction(time_ms - 16000));
    }
    
    public static float airFlowFunction(float time_ms) {
    	if (time_ms < 0) return INITIAL_AIR_FLOW;
    	
    	return (float) Math.sin(time_ms/2000) +  1.0f;
    }
    
    public static float stepFunction(float time_ms) {
    	if (time_ms < 0) return 0;
    	
    	return 1.0f;
    }
    
    // Prevent instantiation
    private EnvConfig() {}
}
