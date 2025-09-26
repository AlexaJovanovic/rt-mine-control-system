package com.prv;


import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.prv.rt_system.ControlSystem;
import com.prv.rt_system.PeriodicTask;
import com.prv.rt_system.EnvGUI;

public class Simulator {
	public static final int ENVIROMENT_UPDATE_PERIOD_MS = 10;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
    	
        
    	ControlSystem controlSystem = new ControlSystem();
    	
    	EnvironmentState.initialize(controlSystem);
    	PeriodicTask envUpdateTask = new PeriodicTask(ENVIROMENT_UPDATE_PERIOD_MS, ()-> EnvironmentState.getInstance().update(ENVIROMENT_UPDATE_PERIOD_MS), 8);
    	envUpdateTask.start();

    	
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
        
        // Wait max 20s for it to finish
        //controlSystem.join();
        controlSystem.join(50_000);

        // If still running after 20s, stop it
        if (controlSystem.isAlive()) {
            controlSystem.interrupt();   // or controlSystem.shutdown() if you implemented it
        }
    	
        controlSystem.join();
        
    	envUpdateTask.interrupt();
    	
    	System.out.printf("coReader max_exec_time:%f\n", envUpdateTask.getMaxExecTimeMs());
        
    }

	
	public static Simulator getInstance() {
		return Simulator.instance;
	}
	
	
	private static Simulator instance;
	
	private Simulator() {}
	
}
