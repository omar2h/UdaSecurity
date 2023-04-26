package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private SecurityService securityService;
    @Mock
    private FakeImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private Sensor sensor;
    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    // 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void changeSensorActivationStatus_alarmArmedAndSensorActivated_changedSystemIntoPendingAlarmStatus(
            ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void changeSensorActivationStatus_alarmArmedAndSensorActivatedAndSystemAlarmPending_changedSystemIntoAlarmStatus(
            ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void changeSensorActivationStatus_pendingAlarmAndSensorsInactive_changedSystemIntoNoAlarmStatus() {
        when(sensor.getActive()).thenReturn(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    public void changeSensorActivationStatus_alarmActive_noChangeInAlarmState(Boolean sensorState) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, sensorState);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    public void changeSensorActivationStatus_sensorActivatedAlreadyActiveAndPendingAlarm_changeStatusToAlarm(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @MethodSource("differentAlarmStatus")
    public void changeSensorActivationStatus_sensorDeactivatedWhileInactive_noChangesToAlarmState(AlarmStatus alarmStatus) {
        sensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    public void processImage_imageServiceIdentifiesCatAndAlarmArmedHome_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void processImage_imageServiceIdentifiesNoCatAndSensorNotActive_changeStatusToNoAlarm() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9. If the system is disarmed, set the status to no alarm.
    @Test
    public void setArmingStatus_systemDisarmed_setNoAlarmState() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10. If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void setArmingStatus_systemArmed_resetSensors(ArmingStatus armingStatus) {
        Set<Sensor> sensors = createSensors(3, true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);

        assertTrue(sensors.stream().allMatch(s -> s.getActive() == false), "Expected all sensors to be inactive, but some are active.");
    }

    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void processImage_systemArmedHomeAndCatIdentified_setAlarmStatusToAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    private static Stream<Arguments> differentArmingStatus() {
        return Stream.of(
                Arguments.of(ArmingStatus.ARMED_AWAY),
                Arguments.of(ArmingStatus.ARMED_HOME)
        );
    }

    private static Stream<Arguments> differentAlarmStatus() {
        return Stream.of(
                Arguments.of(AlarmStatus.NO_ALARM),
                Arguments.of(AlarmStatus.PENDING_ALARM),
                Arguments.of(AlarmStatus.ALARM)
        );
    }

    private Set<Sensor> createSensors(int count, boolean status){
        return IntStream.range(0, count)
                .mapToObj(i -> new Sensor("Sensor " + i, SensorType.DOOR))
                .peek(sensor -> sensor.setActive(status))
                .collect(Collectors.toSet());
    }
}