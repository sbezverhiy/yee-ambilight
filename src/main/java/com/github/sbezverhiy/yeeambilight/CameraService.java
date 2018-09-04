package com.github.sbezverhiy.yeeambilight;

import com.github.sarxos.webcam.Webcam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CameraService {
    Logger log = LoggerFactory.getLogger(CameraService.class);
    protected Webcam webcam;
    //protected Thread interruptedThread;
    protected Thread recThread=null;

    public synchronized Color getAvgColor() {
        return avgColor;
    }

    public synchronized void setAvgColor(Color avgColor) {

        this.avgColor = avgColor;
    }

    protected Color avgColor=null;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private int width=640;
    private int height=480;

    public int getColorDiffThreashold() {
        return colorDiffThreashold;
    }

    public void setColorDiffThreashold(int colorDiffThreashold) {
        this.colorDiffThreashold = colorDiffThreashold;
    }

    private int colorDiffThreashold=150;

    public void startRec(Thread interruptedThread){
        if(recThread!=null) return;
        webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(width, height));
        webcam.open();
        Runnable r = () -> {

            while(!Thread.currentThread().isInterrupted())
            {
                try {
                    Thread.sleep(50);

                }catch (InterruptedException i)
                {
                    //Thread.currentThread().interrupt();
                    log.debug("interrupt rec thread");
                    break;
                }
                Color avgColorNew=averageColor(webcam.getImage(),0,0,width,height);
                if(getAvgColor()!=null && colorDiff(avgColorNew,getAvgColor())>colorDiffThreashold){
                    setAvgColor(avgColorNew);
                    if(interruptedThread.isAlive()) interruptedThread.interrupt();
                    log.debug("Interrupt for great change");
                }else  setAvgColor(avgColorNew);
            }
        log.debug("rec thread exit");
        };
        recThread= new Thread(r);
        recThread.start();

    }

    public void stopRec()
    {
        if(recThread==null) return;
        log.debug("stop rec intered");
        recThread.interrupt();
        try {
            recThread.join();
            log.debug("after waiting for rec stop");
        } catch (InterruptedException e) {

        }
        recThread=null;
        webcam.close();
        log.debug("after closing web cam");
        setAvgColor(null);
    }

    public int colorDiff(Color color1,Color color2)
    {
        return Math.abs(color1.getRed()-color2.getRed())+Math.abs(color1.getBlue()-color2.getBlue())+Math.abs(color1.getGreen()-color2.getGreen());
    }
    protected  Color averageColor(BufferedImage bi, int x0, int y0, int w,
                                     int h) {
        int x1 = x0 + w;
        int y1 = y0 + h;
        long sumr = 0, sumg = 0, sumb = 0;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                Color pixel = new Color(bi.getRGB(x, y));
                sumr += pixel.getRed();
                sumg += pixel.getGreen();
                sumb += pixel.getBlue();
            }
        }
        int num = w * h;
        return new Color((int)(sumr / num), (int)(sumg / num), (int)(sumb / num));
    }
}
