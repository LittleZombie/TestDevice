//package com.test.device
//
//
//import android.bluetooth.BluetoothGattCharacteristic
//
//
///**
// * Created by jjh860627 on 2017. 10. 30..
// */
//class GattDataParser {
//    fun parseBPData(characteristic: BluetoothGattCharacteristic): BP {
//        val flag = characteristic.value[0]
//        val unit: Int = flag and 0x01
//        val sys = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1)
//        val dia = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 3)
//        val map = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 5)
//        var index = 7
//        var timestamp: DateTime? = null
//        if (flag and 0x02 != 0) { //Time stamp(0: Not presented, 1: presented)
//            timestamp = getDateTimeFromCharacteristic(characteristic, index)
//            index += 7
//        }
//        var pulseRate = -1f
//        if (flag and 0x04 != 0) { //Pulse Rate(0: Not presented, 1: Presented)
//            pulseRate =
//                characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//        }
//        var userId = -1
//        if (flag and 0x08 != 0) { //User ID(0: Not presented, 1: Presented)
//            userId = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//        }
//        var measurementStatus = -1
//        if (flag and 0x10 != 0) {
//            measurementStatus =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//        }
//        val bp = BP()
//        bp.setSys(sys)
//        bp.setDia(dia)
//        bp.setMap(map)
//        bp.setTimestamp(timestamp)
//        bp.setPulseRate(pulseRate)
//        bp.setUserId(userId)
//        bp.setMeasurementStatus(measurementStatus)
//        return bp
//    }
//
//    fun parseHeartRateData(characteristic: BluetoothGattCharacteristic): HeartRate {
//        val flag = characteristic.value[0]
//        var format = BluetoothGattCharacteristic.FORMAT_UINT8
//        if (flag and 0x01 != 0) { //Heart Rate Value Format
//            format = BluetoothGattCharacteristic.FORMAT_UINT16
//        }
//        val heartRateValue = characteristic.getIntValue(format, 1)
//        val sensorContactStatus: Int = flag shr 1 and 0x03
//        var index = 2
//        var energyExpended = -1
//        if (flag and 0x08 != 0) { //Energy Expended
//            energyExpended =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//            index += 2
//        }
//        var interval = -1
//        if (flag and 0x10 != 0) {
//            interval = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//        }
//        val heartRate = HeartRate()
//        heartRate.setHeartRate(heartRateValue)
//        heartRate.setSensorContactStatus(sensorContactStatus)
//        heartRate.setEnergyExpended(energyExpended)
//        heartRate.setInterval(interval)
//        return heartRate
//    }
//
//    fun parseTemperatureData(characteristic: BluetoothGattCharacteristic): Temperature {
//        val temperatureValue =
//            characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1)
//        val flag = characteristic.value[0]
//        val unit: Int = flag and 0x01
//        var index = 5
//        var timestamp: DateTime? = null
//        if (flag and 0x02 != 0) { //Time stamp(0: Not presented, 1: presented)
//            timestamp = getDateTimeFromCharacteristic(characteristic, index)
//            index += 7
//        }
//        var temperatureType = -1
//        if (flag and 0x04 != 0) { //Temperature type(0: Not presented, 1: presented)
//            temperatureType =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//        }
//        val temperature = Temperature()
//        temperature.setTemperature(temperatureValue)
//        temperature.setUnit(unit)
//        temperature.setTimestamp(timestamp)
//        temperature.setTemperatureType(temperatureType)
//        return temperature
//    }
//
//    fun parseGlucoseData(characteristic: BluetoothGattCharacteristic): Glucose {
//        val flag = characteristic.value[0]
//        val sequenceNum = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1)
//        val baseDateTime: DateTime = getDateTimeFromCharacteristic(characteristic, 3)
//        var index = 10
//        var timeOffset = -1
//        if (flag and 0x01 != 0) {
//            timeOffset =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, index)
//            index += 2
//        }
//        val unit: Int = flag and 0x04
//        var gluco = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//        if (unit == 0) {
//            gluco *= 100000f // kg/L => mm/gL
//        }
//        index += 2
//        var type = -1
//        var sampleLocation = -1
//        if (flag and 0x02 != 0) {
//            val typeSampleLocation =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//            type = typeSampleLocation and 0x0F
//            sampleLocation = typeSampleLocation shr 4 and 0x0F
//        }
//        var sensorStatus = -1
//        if (flag and 0x08 != 0) {
//            sensorStatus =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//        }
//        val glucose = Glucose()
//        glucose.setBaseTime(baseDateTime)
//        glucose.setGlucose(gluco)
//        glucose.setSampleLocation(sampleLocation)
//        glucose.setSensorStatusAnnunciation(sensorStatus)
//        glucose.setSeqenceNumber(sequenceNum)
//        glucose.setTimeOffset(timeOffset)
//        glucose.setType(type)
//        glucose.setUnit(unit)
//        return glucose
//    }
//
//    fun parseGlucoseContextData(characteristic: BluetoothGattCharacteristic): Glucose.MeasurementContext {
//        val flag = characteristic.value[0]
//        val sequenceNumber =
//            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1)
//        var index = 3
//        var carbohydrateID = -1
//        var carbohydrate = -1.0f
//        if (flag and 0x01 != 0) { //Carbohydrate ID and Carbohydrate Present
//            carbohydrateID =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//            carbohydrate =
//                characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//        }
//        var meal = -1
//        if (flag and 0x02 != 0) { //Meal Present
//            meal = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//        }
//        var tester = -1
//        var health = -1
//        if (flag and 0x04 != 0) { //Tester-Health Present
//            val testerHealth =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//            tester = testerHealth and 0x0F
//            health = testerHealth shr 4 and 0x0F
//        }
//        var exerciseDuration = -1
//        var exerciseIntensity = -1
//        if (flag and 0x08 != 0) { //Exercise Duration And Exercise Intensity Present
//            exerciseDuration =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//            index += 2
//            exerciseIntensity =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//        }
//        var medicationID = -1
//        var medicationUnit = -1
//        var medication = -1f
//        if (flag and 0x10 != 0) { //Medication ID And Medication Present
//            medicationID =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//            medicationUnit = flag and 0x20
//            medication =
//                characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//        }
//        val measurementContext: Glucose.MeasurementContext = MeasurementContext()
//        measurementContext.setSequenceNumber(sequenceNumber)
//        measurementContext.setCarbohydrateID(carbohydrateID)
//        measurementContext.setCarbohydrate(carbohydrate)
//        measurementContext.setMeal(meal)
//        measurementContext.setTester(tester)
//        measurementContext.setHealth(health)
//        measurementContext.setExerciseDuration(exerciseDuration)
//        measurementContext.setExerciseIntensity(exerciseIntensity)
//        measurementContext.setMedicationID(medicationID)
//        measurementContext.setMedicationUnit(medicationUnit)
//        measurementContext.setMedication(medication)
//        return measurementContext
//    }
//
//    fun parseWeightScaleData(characteristic: BluetoothGattCharacteristic): Weight {
//        val flag = characteristic.value[0]
//        val unit: Int = flag and 0x01
//        val weightValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1)
//        var index = 3
//        var timestamp: DateTime? = null
//        if (flag and 0x02 != 0) {
//            timestamp = getDateTimeFromCharacteristic(characteristic, index)
//            index += 7
//        }
//        var userID = -1
//        if (flag and 0x04 != 0) {
//            userID = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index)
//            index += 1
//        }
//        var bmi = -1
//        var height = -1
//        if (flag and 0x08 != 0) {
//            bmi = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//            index += 2
//            height = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//        }
//        val weight = Weight()
//        weight.setUnit(unit)
//        weight.setWeight(weightValue)
//        weight.setTimestamp(timestamp)
//        weight.setUserId(userID)
//        weight.setBmi(bmi)
//        weight.setHeight(height)
//        return weight
//    }
//
//    fun parseSpo2Data(characteristic: BluetoothGattCharacteristic): Spo2 {
//        val flag = characteristic.value[0]
//        val spo2Value = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1)
//        val pr = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 3)
//        var index = 5
//        var timestamp: DateTime? = null
//        if (flag and 0x01 != 0) {
//            timestamp = getDateTimeFromCharacteristic(characteristic, index)
//            index += 7
//        }
//        var measurementStatus = -1
//        if (flag and 0x02 != 0) {
//            measurementStatus =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//            index += 2
//        }
//        var deviceAndSensorStatus = -1
//        if (flag and 0x04 != 0) {
//            //8 + 16 = 24bit
//            deviceAndSensorStatus = characteristic.getIntValue(
//                BluetoothGattCharacteristic.FORMAT_UINT8,
//                index
//            ) + characteristic.getIntValue(
//                BluetoothGattCharacteristic.FORMAT_UINT16,
//                index + 1
//            ) shl 8
//            index += 3
//        }
//        var pulseAmplitudeIndex = -1f
//        if (flag and 0x08 != 0) {
//            pulseAmplitudeIndex =
//                characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//        }
//        val isDeviceClockSet = flag and 0x10 == 1
//        val spo2 = Spo2()
//        spo2.setSpo2(spo2Value)
//        spo2.setPr(pr)
//        spo2.setTimestamp(timestamp)
//        spo2.setMeasurementStatus(measurementStatus)
//        spo2.setDeviceAndSensorStatus(deviceAndSensorStatus)
//        spo2.setPulseAmplitudeIndex(pulseAmplitudeIndex)
//        spo2.setDeviceClockSet(isDeviceClockSet)
//        return spo2
//    }
//
//    fun parseSpo2ContinuousData(characteristic: BluetoothGattCharacteristic): Spo2 {
//        val flag = characteristic.value[0]
//        val spo2Value = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1)
//        val pr = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 3)
//        var index = 5
//        var spo2Fast = -1f
//        var prFast = -1f
//        if (flag and 0x01 != 0) {
//            spo2Fast =
//                characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//            prFast = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//        }
//        var spo2Slow = -1f
//        var prSlow = -1f
//        if (flag and 0x02 != 0) {
//            spo2Slow =
//                characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//            prSlow = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//        }
//        var measurementStatus = -1
//        if (flag and 0x04 != 0) {
//            measurementStatus =
//                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index)
//            index += 2
//        }
//        var deviceAndSensorStatus = -1
//        if (flag and 0x08 != 0) {
//            //8 + 16 = 24bit
//            deviceAndSensorStatus = characteristic.getIntValue(
//                BluetoothGattCharacteristic.FORMAT_UINT8,
//                index
//            ) + characteristic.getIntValue(
//                BluetoothGattCharacteristic.FORMAT_UINT16,
//                index + 1
//            ) shl 8
//            index += 3
//        }
//        var pulseAmplitudeIndex = -1f
//        if (flag and 0x10 != 0) {
//            pulseAmplitudeIndex =
//                characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//            index += 2
//        }
//        val spo2 = Spo2()
//        spo2.setSpo2(spo2Value)
//        spo2.setPr(pr)
//        spo2.setMeasurementStatus(measurementStatus)
//        spo2.setDeviceAndSensorStatus(deviceAndSensorStatus)
//        spo2.setPulseAmplitudeIndex(pulseAmplitudeIndex)
//        val measurementContinuous: Spo2.MeasurementContinuous = MeasurementContinuous()
//        measurementContinuous.setSpo2Fast(spo2Fast)
//        measurementContinuous.setPrFast(prFast)
//        measurementContinuous.setSpo2Slow(spo2Slow)
//        measurementContinuous.setPrSlow(prSlow)
//        spo2.setMeasurementContinuous(measurementContinuous)
//        return spo2
//    }
//
//    private fun getDateTimeFromCharacteristic(
//        characteristic: BluetoothGattCharacteristic,
//        startIndex: Int
//    ): DateTime {
//        var startIndex = startIndex
//        val year = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, startIndex)
//        startIndex += 2
//        val month =
//            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
//        val day = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
//        val hours =
//            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
//        val minutes =
//            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
//        val seconds =
//            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
//        return DateTime(year, month, day, hours, minutes, seconds)
//    }
//}