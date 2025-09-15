package com.prv;

public final class EnvConfig {

    // Gas concentrations (percentages)
    public static final float INITIAL_CO_CONCENTRATION = 0.5f;
    public static final float INITIAL_CH4_CONCENTRATION = 0.1f;

    // Water levels (cm)
    public static final float INITIAL_WATER_LEVEL = 10.0f;
    public static final float LOW_WATER_LEVEL = 5.0f;
    public static final float HIGH_WATER_LEVEL = 20.0f;

    // Airflow (m³/s)
    public static final float INITIAL_AIR_FLOW = 1.0f;

    // Water flow rates (cm/s)
    public static final float WATER_FILLING_RATE = 0.5f; 
    public static final float PUMP_WATER_FLOW = -3.0f;

    
    // defining mathematical functions that will define signal values in time
    public static float coConcentrationFunction(float time_ms) {
    	if (time_ms < 0) return 0;
    	
    	if (time_ms < 10) return INITIAL_CO_CONCENTRATION + time_ms / 1000 * 0.2f;
    	
    	return 0;
    }
    public static float ch4ConcentrationFunction(float time_ms) {
    	
    	return INITIAL_CH4_CONCENTRATION;
    }
    
    // Prevent instantiation
    private EnvConfig() {}
}
