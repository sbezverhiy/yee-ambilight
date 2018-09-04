package com.github.sbezverhiy.yeeambilight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightServiceMock {

    Thread lightingThread=null;
    Logger log = LoggerFactory.getLogger(LightService.class);
    public  synchronized boolean isAmbilightOn()
    {
        return lightingThread!=null;
    }

    public synchronized void startAmbilight(){
        if(lightingThread!=null) return;
        Runnable r = () -> {
            log.info("Start");
            while(!Thread.currentThread().isInterrupted())
            {
                try {
                    Thread.sleep(1000);
                }catch (InterruptedException i)
                {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("Stoping");
            for(long i=0;i<100000000;i++)
                Math.random();
            log.info("Stop");
        };
        lightingThread= new Thread(r);
        lightingThread.start();
        log.info("Lighting is started");
    }

    public synchronized void stopAmbilight(){
        if(lightingThread==null) return;
        lightingThread.interrupt();
        try {
            lightingThread.join();
        } catch (InterruptedException e) {

        }
        lightingThread=null;
    }
}
