package com.prv;


import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.prv.rt_system.ControlSystem;
import com.prv.rt_system.PeriodicTask;
import com.prv.rt_system.EnvGUI;

public class Simulator {
	public static final int ENVIROMENT_UPDATE_PERIOD_MS = 10;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException { 	
    	ControlSystem controlSystem = new ControlSystem();
    	
    	EnvironmentState.initialize(controlSystem);
    	
    	// Start environment updater thread
    	PeriodicTask envUpdateTask = new PeriodicTask(ENVIROMENT_UPDATE_PERIOD_MS, ()-> EnvironmentState.getInstance().update(ENVIROMENT_UPDATE_PERIOD_MS), 8);
    	envUpdateTask.start();

    	
    	// Create GUI on the EDT. Use AtomicReference so we can modify the reference inside lambda
    	AtomicReference<EnvGUI> guiRef = new AtomicReference<>();
    	SwingUtilities.invokeAndWait(() -> 
    	    guiRef.set(new EnvGUI(controlSystem))
    	);
    	EnvGUI gui = guiRef.get();  // safe reference after invokeAndWait completes


        // Start GUI water level updater thread
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
        
        // Run control system for 50s
        controlSystem.join(20_000);
        if (controlSystem.isAlive()) {
            controlSystem.interrupt();
        }
        controlSystem.join();
        
        // Stop environment running
    	envUpdateTask.interrupt();
        
    }

	
	public static Simulator getInstance() {
		return Simulator.instance;
	}
	
	
	private static Simulator instance;
	
	private Simulator() {}
	
}
