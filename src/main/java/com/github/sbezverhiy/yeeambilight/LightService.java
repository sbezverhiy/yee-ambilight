package com.github.sbezverhiy.yeeambilight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class LightService {

    Logger log = LoggerFactory.getLogger(LightService.class);
    Thread lightingThread=null;

    public int getColorDiffMinThreshold() {
        return colorDiffMinThreshold;
    }

    public void setColorDiffMinThreshold(int colorDiffMinThreshold) {
        this.colorDiffMinThreshold = colorDiffMinThreshold;
    }

    public String getLedHost() {
        return ledHost;
    }

    public void setLedHost(String ledHost) {
        this.ledHost = ledHost;
    }

    public int getLedPort() {
        return ledPort;
    }

    public void setLedPort(int ledPort) {
        this.ledPort = ledPort;
    }

    protected int colorDiffMinThreshold=30;
    protected String ledHost="192.168.1.42";
    protected int ledPort=55443;

    public CameraService getCameraService() {
        return cameraService;
    }

    public void setCameraService(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    CameraService cameraService;

    public  synchronized boolean isAmbilightOn()
    {
        return lightingThread!=null;
    }

    public synchronized void startAmbilight(){
        if(lightingThread!=null) return;
        Runnable r = () -> {
            log.info("Ambilight Startting");
            cameraService.startRec(Thread.currentThread());
            Socket socket=null;
            OutputStream socketOutputStream=null;
            BufferedReader socketIn=null;

            try {
                Thread.sleep(2000);
            }catch (InterruptedException i){}
            try {
            socket = new Socket(ledHost, ledPort);
            socketOutputStream= socket.getOutputStream();
            socketIn=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socketOutputStream.write("{\"id\": 1,\"method\": \"set_power\",\"params\": [\"on\",\"smooth\",200]}\r\n".getBytes());
            socketOutputStream.flush();
            log.debug(socketIn.readLine());
                Color previousColor = null;
                Color currentColor = cameraService.getAvgColor();
                while (currentColor != null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException i) {
                        //Thread.currentThread().interrupt();
                        log.debug("interrupted for immediate color change");
                    }
                    currentColor = cameraService.getAvgColor();
                    if (currentColor == null) {
                        log.info("CurrentColor is null, exit process");
                        break;
                    }
                    if (previousColor != null && cameraService.colorDiff(currentColor, previousColor) < colorDiffMinThreshold)
                        continue;
                    previousColor = currentColor;
                    socketOutputStream.write(getCommandForChangeColor(currentColor).getBytes());
                    socketOutputStream.flush();
                    String serverResponse = socketIn.readLine();
                    log.debug("led answer"+ serverResponse);


                }
            }catch(Exception e)
            {
                log.error("error in ambilight thread",e);

            }finally {
                cameraService.stopRec();
                try {
                    //socketChannel.close();
                    socketIn.close();
                    socketOutputStream.close();
                    socket.close();
                } catch (Exception e) {

                }
            }
            log.info("ambilight thread finished");
        };
        lightingThread= new Thread(r);
        lightingThread.start();
        log.info("Lighting is startted");
    }

    public synchronized void stopAmbilight(){
        if(lightingThread==null) return;
        cameraService.stopRec();
        try {
            lightingThread.join();
        } catch (InterruptedException e) {

        }
        lightingThread=null;
    }
    protected String getCommandForChangeColor(Color color)
    {
        long rgbValue=color.getRed();
        rgbValue=rgbValue<<8;
        rgbValue=rgbValue|(long)color.getGreen();
        rgbValue=rgbValue<<8;
        rgbValue=rgbValue|(long)color.getBlue();
        return "{\"id\": 1,\"method\": \"set_rgb\",\"params\": ["+rgbValue+",\"smooth\",150]}\r\n";
    }

    public static void main(String[] args)
    {
        LightService lightService= new LightService();
        lightService.setCameraService(new CameraService());
        lightService.startAmbilight();
        try {
            Thread.sleep(1000*60*1);
        } catch (InterruptedException i) {
        }
        lightService.stopAmbilight();
    }
}
