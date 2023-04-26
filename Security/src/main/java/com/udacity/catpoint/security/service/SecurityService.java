package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */

public class SecurityService {
    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        else if(getIsCatDetected() && armingStatus == ArmingStatus.ARMED_HOME){
            setAlarmStatus(AlarmStatus.ALARM);
        }
        else{
            resetSensors();
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private boolean catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if(!cat && checkAllSensorsStatus(false)) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
        return cat;
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated(boolean previousState) {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM ->{
                if(previousState){
                    setAlarmStatus(AlarmStatus.ALARM);
                }
                else if(getArmingStatus() == ArmingStatus.ARMED_HOME || getArmingStatus() == ArmingStatus.ARMED_AWAY)
                    setAlarmStatus(AlarmStatus.ALARM);
            }
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated(boolean previousState) {
        if(!previousState)
            return;
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> {
                if(checkAllSensorsStatus(false))
                    setAlarmStatus(AlarmStatus.NO_ALARM);
            }
            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        boolean previousState = sensor.getActive();
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

        if(securityRepository.getAlarmStatus() == AlarmStatus.ALARM){
            return;
        }
        else if(active){
            handleSensorActivated(previousState);
        }
        else{
            handleSensorDeactivated(previousState);
        }
    }

    private boolean checkAllSensorsStatus(boolean status){
        return getSensors().stream().allMatch(s -> s.getActive() == status);
    }

    private void resetSensors(){
        getSensors().stream().forEach(s -> s.setActive(false));
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        setIsCatDetected(catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f)));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Collection<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    public boolean getIsCatDetected(){
        return securityRepository.isCatDetected();
    }

    public void setIsCatDetected(boolean status){
        securityRepository.setIsCatDetected(status);
    }
}
