package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    public void changeSensorActivationStatus_alarmArmedAndRandomSensorActivated_changedSystemIntoPendingAlarmStatus(
            ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, false);
        int numberOfSensorsActivated = random.nextInt(0, numberOfSensors+1);
        sensors.stream()
                .limit(numberOfSensorsActivated)
                .forEach(s -> securityService.changeSensorActivationStatus(s, true));

        verify(securityRepository, times(numberOfSensorsActivated)
                .description("Expected setAlarmStatus(AlarmStatus.PENDING_ALARM) to be called "
                        + numberOfSensorsActivated + " time(s)"))
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void changeSensorActivationStatus_alarmArmedAndOneSensorActivated_changedSystemIntoPendingAlarmStatus(
            ArmingStatus armingStatus){
        List<Sensor> sensors = (List)createSensorsList(3, false);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensors.get(0), true);

        verify(securityRepository, times(1)
                .description("Expected setAlarmStatus to be called 1 time"))
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void changeSensorActivationStatus_alarmArmedAndNoSensorsActivated_changedSystemIntoPendingAlarmStatus(
            ArmingStatus armingStatus){
        List<Sensor> sensors = createSensorsList(3, false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensors.get(0), false);

        verify(securityRepository, never()
                .description("Expected setAlarmStatus to never be called"))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.

    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void changeSensorActivationStatus_alarmArmedAndRandomSensorsActivatedAndSystemAlarmPending_changedSystemIntoAlarmStatus(
            ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, false);
        int numberOfSensorsActivated = random.nextInt(0, numberOfSensors);
        sensors.stream()
                .limit(numberOfSensorsActivated)
                .forEach(s -> securityService.changeSensorActivationStatus(s, true));

        verify(securityRepository, times(numberOfSensorsActivated)
                .description("Expected setAlarmStatus(AlarmStatus.PENDING_ALARM) to be called "
                + numberOfSensorsActivated + " time(s)"))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void changeSensorActivationStatus_alarmArmedAndOneSensorActivatedAndSystemAlarmPending_changedSystemIntoAlarmStatus(
            ArmingStatus armingStatus){
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        List<Sensor> sensors = createSensorsList(3, false);

        securityService.changeSensorActivationStatus(sensors.get(0), true);

        verify(securityRepository, times(1)
                .description("Expected setAlarmStatus(AlarmStatus.PENDING_ALARM) to be called "
                        + 1 + " time(s)"))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void changeSensorActivationStatus_alarmArmedAndNoSensorsActivatedAndSystemAlarmPending_notChangedSystemIntoAlarmStatus(
            ArmingStatus armingStatus){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        List<Sensor> sensors = createSensorsList(3, false);

        securityService.changeSensorActivationStatus(sensors.get(0), false);

        verify(securityRepository, never()
                .description("Expected setAlarmStatus(AlarmStatus.PENDING_ALARM) to not be called"))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.

    @RepeatedTest(3)
    public void changeSensorActivationStatus_pendingAlarmAndAllSensorsInactive_changedSystemIntoNoAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, true);
        when(securityRepository.getSensors()).thenReturn((Set<Sensor>) sensors);

        sensors.stream()
                .forEach(s -> securityService.changeSensorActivationStatus(s, false));

        verify(securityRepository, times(1)
                .description("Expected setAlarmStatus to be called 1 time"))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @RepeatedTest(3)
    public void changeSensorActivationStatus_pendingAlarmAndNotAllSensorsInactive_noChangedSystemIntoNoAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, true);
        when(securityRepository.getSensors()).thenReturn((Set<Sensor>) sensors);
        sensors.stream()
                .limit(numberOfSensors-1)
                .forEach(s -> securityService.changeSensorActivationStatus(s, false));

        verify(securityRepository, never()
                .description("Expected setAlarmStatus to not be called"))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state.

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    public void changeSensorActivationStatus_alarmActive_noChangeInAlarmState(Boolean sensorState) {
        List<Boolean> state = List.of(true, false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, sensorState);

        sensors.stream()
                .forEach(s -> securityService
                        .changeSensorActivationStatus(s, state.get(random.nextInt(state.size()))));

        verify(securityRepository, never()
                .description("Expected setAlarmStatus to never be called"))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @RepeatedTest(3)
    public void changeSensorActivationStatus_sensorActivatedAlreadyActiveAndPendingAlarm_changeStatusToAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, true);
        int numberOfSensorsActivated = random.nextInt(0, numberOfSensors);
        sensors.stream()
                .limit(numberOfSensorsActivated)
                .forEach(s -> securityService.changeSensorActivationStatus(s, true));

        verify(securityRepository, times(numberOfSensorsActivated)
                .description("Expected setAlarmStatus(AlarmStatus.ALARM) to be called "
                        + numberOfSensorsActivated + " time(s)"))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @RepeatedTest(3)
    public void changeSensorActivationStatus_sensorActivatedAlreadyNotActiveAndPendingAlarm_noChangeStatusToAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, false);
        int numberOfSensorsActivated = random.nextInt(0, numberOfSensors);

        sensors.stream()
                .limit(numberOfSensorsActivated)
                .forEach(s -> securityService.changeSensorActivationStatus(s, true));

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.

    @ParameterizedTest
    @MethodSource("differentAlarmStatus")
    public void changeSensorActivationStatus_sensorDeactivatedWhileInactive_noChangesToAlarmState(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, false);
        int numberOfSensorsActivated = random.nextInt(0, numberOfSensors);

        sensors.stream()
                .limit(numberOfSensorsActivated)
                .forEach(s -> securityService.changeSensorActivationStatus(s, false));

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
    @RepeatedTest(3)
    public void processImage_imageServiceIdentifiesNoCatAndAllSensorNotActive_changeStatusToNoAlarm() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, false);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)
                .description("Expected setAlarmStatus(AlarmStatus.NO_ALARM) called 1 time"))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @RepeatedTest(3)
    public void processImage_imageServiceIdentifiesNoCatAndSensorsActive_noChangeStatusToNoAlarm() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, true);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, never()
                .description("Expected setAlarmStatus(AlarmStatus.NO_ALARM) never called"))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @RepeatedTest(3)
    public void processImage_imageServiceIdentifiesNoCatAndSomeSensorsActive_noChangeStatusToNoAlarm() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);

        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, true);
        int numberOfSensorsActivated = random.nextInt(0, numberOfSensors);

        sensors.stream()
                .limit(numberOfSensorsActivated)
                .forEach(s -> s.setActive(false));
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, never()
                .description("Expected setAlarmStatus(AlarmStatus.NO_ALARM) never called"))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9. If the system is disarmed, set the status to no alarm.
    @Test
    public void setArmingStatus_systemDisarmed_setNoAlarmState() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)
                .description("Expected setAlarmStatus(AlarmStatus.NO_ALARM) called once"))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void setArmingStatus_systemArmed_notSetNoAlarmState(ArmingStatus armingStatus) {
        securityService.setArmingStatus(armingStatus);

        verify(securityRepository, never()
                .description("Expected setAlarmStatus(AlarmStatus.NO_ALARM) never called"))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10. If the system is armed, reset all sensors to inactive.

    @ParameterizedTest
    @MethodSource("differentArmingStatus")
    public void setArmingStatus_systemArmed_resetSensors(ArmingStatus armingStatus) {
        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, true);

        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);

        assertTrue(sensors.stream().allMatch(s -> s.getActive() == false), "Expected all sensors to be inactive, but some are active.");
    }

    @RepeatedTest(3)
    public void setArmingStatus_systemDisarmed_noResetSensors() {
        Random random = new Random();
        int numberOfSensors = random.nextInt(1, 10);
        Set<Sensor> sensors = createSensorsSet(numberOfSensors, true);

        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        assertTrue(sensors.stream().allMatch(s -> s.getActive() == true), "Expected all sensors to be active, but some are inactive.");
    }

    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void setArmingStatus_systemIsBeingArmedHomeAndCatIdentified_setAlarmStatusToAlarm() {
        when(securityRepository.isCatDetected()).thenReturn(true);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void setArmingStatus_systemIsBeingArmedHomeAndCatNotIdentified_setAlarmStatusToAlarm() {
        when(securityRepository.isCatDetected()).thenReturn(false);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
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

    private Set<Sensor> createSensorsSet(int count, boolean status){
        return IntStream.range(0, count)
                .mapToObj(i -> new Sensor("Sensor " + i, SensorType.DOOR))
                .peek(sensor -> sensor.setActive(status))
                .collect(Collectors.toSet());
    }

    private List<Sensor> createSensorsList(int count, boolean status){
        return IntStream.range(0, count)
                .mapToObj(i -> new Sensor("Sensor " + i, SensorType.DOOR))
                .peek(sensor -> sensor.setActive(status))
                .collect(Collectors.toList());
    }

    private boolean checkAllSensorsStatus(boolean status, Collection<Sensor> sensors){
        return sensors.stream().allMatch(s -> s.getActive() == status);
    }
}