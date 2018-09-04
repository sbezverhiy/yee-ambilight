package com.github.sbezverhiy.yeeambilight.bean;

import com.github.sbezverhiy.yeeambilight.LightService;

import javax.faces.event.ActionEvent;

public class LightServiceBean {

    public LightService getLightService() {
        return lightService;
    }

    public void setLightService(LightService lightService) {
        this.lightService = lightService;
    }

    protected LightService lightService;


    public String getStatus()
    {
        return lightService.isAmbilightOn() ? "Включен" : "Выключен";
    }

    public boolean isAmbilightOn()
    {
        return lightService.isAmbilightOn();
    }

    public void startAmbilight(ActionEvent event)
    {
        lightService.startAmbilight();
    }
    public void stopAmbilight(ActionEvent event)
    {
        lightService.stopAmbilight();
    }

}
