package com.prv.rt_system;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class ADC {
    public ADC(Supplier<Float> signalSource) {
        this.signalSource = signalSource;
    }

    public void brakeDownADCDevice() {
    	this.ADCworksProperly = false;
    }
    
    public void fixADCDevice() {
    	this.ADCworksProperly = true;
    }
    public enum ADCStatus {
    	DATA_READY,
    	DATA_NOT_READY,
    	FAILED_CONVERSION
    }
    
    // Start ADC conversion
    public void startConversion() {
        if (conversionInProgress) return;

        conversionInProgress = true;
        dataReady = false;
        
        // capture current input value and start conversion
        value = signalSource.get();
        
        scheduler.schedule(() -> {
        	// make the data available only after conversion delay
            dataReady = true;
            conversionInProgress = false;
        }, CONVERSION_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    // Returns true if data is ready for reading
    public ADCStatus getStatus() {
    	if (!ADCworksProperly) return ADCStatus.FAILED_CONVERSION;
    	if (conversionInProgress) return ADCStatus.DATA_NOT_READY;
    	
        return ADCStatus.DATA_READY;
    }

    // Returns the converted value if ready
    public float getValue() {
        if (!dataReady) {
            throw new IllegalStateException("Data not ready yet!");
        }
        
        dataReady = false; // flag cleared on read
        return value;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
    
    private static final int CONVERSION_DELAY_MS = 50;
	
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Supplier<Float> signalSource; // function to get current signal value
    private volatile boolean conversionInProgress = false;
    private volatile boolean dataReady = false;
    private volatile float value = 0;
    
    private volatile boolean ADCworksProperly = true;
    
    // ADC test
    public static void main(String[] args) throws InterruptedException {
        // Shared "signals" updated by another thread
        final float[] signals = {100, 200, 300, 400};

        // Create 4 ADCs, each bound to one signal
        ADC adc1 = new ADC(() -> signals[0]);
        ADC adc2 = new ADC(() -> signals[1]);
        ADC adc3 = new ADC(() -> signals[2]);
        ADC adc4 = new ADC(() -> signals[3]);

        // Thread to simulate changing signals
        Thread signalThread = new Thread(() -> {
            int t = 0;
            while (true) {
                signals[0] = (int) (100 + 50 * Math.sin(t / 10.0));
                signals[1] = (int) (200 + 80 * Math.cos(t / 15.0));
                signals[2] = (int) (300 + (t % 100));
                signals[3] = (int) (400 - (t % 50));
                t++;
                try {
                    Thread.sleep(20); // update every 20 ms
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        signalThread.start();

        // Use ADCs
        for (int i = 0; i < 5; i++) {
            adc1.startConversion();
            adc2.startConversion();
            adc3.startConversion();
            adc4.startConversion();

            Thread.sleep(100); // wait long enough for conversions

            System.out.printf("ADC1=%f, ADC2=%f, ADC3=%f, ADC4=%f%n",
                    adc1.getValue(),
                    adc2.getValue(),
                    adc3.getValue(),
                    adc4.getValue());
        }

        // Cleanup
        adc1.shutdown();
        adc2.shutdown();
        adc3.shutdown();
        adc4.shutdown();
        signalThread.interrupt();
    }
}

