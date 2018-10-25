package com.github.sbezverhiy.yeeambilight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;

public class LightServiceImpl implements LightService {

    Logger log = LoggerFactory.getLogger(LightServiceImpl.class);
    Thread lightingThread=null;
    private int colorUpdateDelay = 1000;
    protected int colorDiffMinThreshold=30;
    protected String ledHost="192.168.1.42";
    protected int ledPort=55443;

    public int getColorUpdateDelay() {
        return colorUpdateDelay;
    }

    public void setColorUpdateDelay(int colorUpdateDelay) {
        this.colorUpdateDelay = colorUpdateDelay;
    }


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


    public CameraService getCameraService() {
        return cameraService;
    }

    public void setCameraService(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    CameraService cameraService;

    @Override
    public  synchronized boolean isAmbilightOn()
    {
        return lightingThread!=null && lightingThread.isAlive();
    }

    @Override
    public synchronized void startAmbilight(){
        if(lightingThread!=null && lightingThread.isAlive()) return;
        Runnable r = () -> {
            log.info("Camera service Startting");
            if (!cameraService.startRec(Thread.currentThread())){
                log.warn("Camera service didnt start,break execution");
                return;
            }

            log.info("Ambilight Startting");
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
                        Thread.sleep(colorUpdateDelay);
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
                    {
                        log.debug("color didnt change,continue");
                        continue;
                    }
                        
                    previousColor = currentColor;
                    try {
                        socketOutputStream.write(getCommandForChangeColor(currentColor).getBytes());
                        socketOutputStream.flush();
                        String serverResponse = socketIn.readLine();
                        log.debug("led answer"+ serverResponse);
                    }catch (SocketException se)
                    {
                        log.error("error in ambilight thread while write to socket",se);
                        socketIn.close();
                        socketOutputStream.close();
                        socket.close();
                        Date fromTime = new Date();
                        while((new Date().getTime()- fromTime.getTime())<5000)
                        {

                        }
                        log.info("try to reconnect to socket after waiting");
                        socket = new Socket(ledHost, ledPort);
                        socketOutputStream= socket.getOutputStream();
                        socketIn=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    }
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

    @Override
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
        LightServiceImpl lightService= new LightServiceImpl();
        lightService.setCameraService(new CameraService());
        lightService.startAmbilight();
        try {
            Thread.sleep(1000*60*1);
        } catch (InterruptedException i) {
        }
        lightService.stopAmbilight();
    }
}
