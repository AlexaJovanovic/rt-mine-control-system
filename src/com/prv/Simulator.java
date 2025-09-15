package com.prv;


import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.prv.rt_system.ControlSystem;
import com.prv.rt_system.PeriodicTask;
import com.prv.rt_system.EnvGUI;

public class Simulator {
	public static final int ENVIROMENT_UPDATE_PERIOD_MS = 10;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
    	
    	EnvironmentState.initialize();
    	PeriodicTask envUpdateTask = new PeriodicTask(ENVIROMENT_UPDATE_PERIOD_MS, ()-> EnvironmentState.getInstance().update(ENVIROMENT_UPDATE_PERIOD_MS));
    	envUpdateTask.start();

        
    	ControlSystem controlSystem = new ControlSystem();
    	
    	
        // Create GUI on EDT
        final EnvGUI[] guiHolder = new EnvGUI[1];
        SwingUtilities.invokeAndWait(() -> guiHolder[0] = new EnvGUI(controlSystem));
        EnvGUI gui = guiHolder[0];  // safe reference

        // Start GUI updater thread
        new Thread(() -> {
            while (true) {
                try {
                    float waterLevel = EnvironmentState.getInstance().getWaterLevel();
                    gui.updateWaterLevel(waterLevel, 0, 100);
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    	
        controlSystem.setGui(gui);
        controlSystem.start();
        
    	controlSystem.join();
    	
    	envUpdateTask.interrupt();
    }

	
	public static Simulator getInstance() {
		return Simulator.instance;
	}
	
	
	private static Simulator instance;
	
	private Simulator() {}
	
}
