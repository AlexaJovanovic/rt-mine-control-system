package com.prv.rt_system;

import com.prv.EnvironmentState;

public class MCU {
	public static MCU instance = new MCU();   // singleton instance
	
	public ADC adc1_co = new ADC(() -> EnvironmentState.getInstance().getCoConcentration());
	public ADC adc2_ch4 = new ADC(() -> EnvironmentState.getInstance().getCh4Concentration());
	public ADC adc3_af = new ADC(() -> EnvironmentState.getInstance().getAirFlow());
	public ADC adc4_wf = new ADC(() -> EnvironmentState.getInstance().getPumpWaterFlow());
	
    public void brakeDownADCDevice(int adc_no) {
    	switch (adc_no) {
    	case 1:
    		adc1_co.brakeDownADCDevice();
    		break;
    	case 2:
    		adc2_ch4.brakeDownADCDevice();
    		break;
    	case 3:
    		adc3_af.brakeDownADCDevice();
    		break;
    	case 4:
    		adc4_wf.brakeDownADCDevice();
    		break;
    	}
    }
    
    public void fixADCDevice(int adc_no) {
    	switch (adc_no) {
    	case 1:
    		adc1_co.fixADCDevice();
    		break;
    	case 2:
    		adc2_ch4.fixADCDevice();
    		break;
    	case 3:
    		adc3_af.fixADCDevice();
    		break;
    	case 4:
    		adc4_wf.fixADCDevice();
    		break;
    	}
    }
    
	
	public void EXTIWaterLevelHigh() {}
	public void EXTIWaterLevelLow() {}
	
	private MCU() {}
}
