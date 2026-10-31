package com.example.healthjournal.domain

import com.example.healthjournal.data.local.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the manual-vitals validation contract: every field is
 * optional, display-unit input parses back to canonical metric storage, and
 * errors are returned as typed codes so the UI can localize them.
 */
class ValidateVitalsTest {

    private fun input(
        systolic: String = "",
        diastolic: String = "",
        heartRate: String = "",
        glucose: String = "",
        temperature: String = "",
        spo2: String = ""
    ) = VitalsInput(
        systolic = systolic,
        diastolic = diastolic,
        heartRate = heartRate,
        glucose = glucose,
        temperature = temperature,
        spo2 = spo2
    )

    @Test
    fun blankInput_isValid_andParsesToEmptyVitals() {
        val errors = ValidateVitals.validate(input(), UnitSystem.METRIC)

        assertTrue(errors.isEmpty())
        assertEquals(EntryVitals(), ValidateVitals.toEntryVitals(input(), UnitSystem.METRIC))
    }

    @Test
    fun validMetricInput_producesNoErrors_andParsesCanonicalValues() {
        val raw = input(
            systolic = "120",
            diastolic = "80",
            heartRate = "72",
            glucose = "5.4",
            temperature = "36.6",
            spo2 = "98"
        )

        val errors = ValidateVitals.validate(raw, UnitSystem.METRIC)
        val vitals = ValidateVitals.toEntryVitals(raw, UnitSystem.METRIC)

        assertTrue(errors.isEmpty())
        assertEquals(
            EntryVitals(
                bpSystolic = 120.0,
                bpDiastolic = 80.0,
                heartRate = 72,
                bloodGlucoseMmol = 5.4,
                bodyTemperatureC = 36.6,
                spo2Percent = 98
            ),
            vitals
        )
    }

    @Test
    fun validImperialInput_convertsGlucoseAndTemperatureToCanonical() {
        val raw = input(
            systolic = "120",
            diastolic = "80",
            heartRate = "72",
            glucose = "97",
            temperature = "98.6",
            spo2 = "98"
        )

        val errors = ValidateVitals.validate(raw, UnitSystem.IMPERIAL)
        val vitals = ValidateVitals.toEntryVitals(raw, UnitSystem.IMPERIAL)

        assertTrue(errors.isEmpty())
        assertEquals(5.38, vitals!!.bloodGlucoseMmol!!, 0.001)
        assertEquals(37.0, vitals.bodyTemperatureC!!, 0.001)
    }

    @Test
    fun loneSystolic_reportsPairRequiredOnDiastolic() {
        val errors = ValidateVitals.validate(input(systolic = "120"), UnitSystem.METRIC)

        assertEquals(
            VitalsError.BloodPressurePairRequired,
            errors[VitalField.DIASTOLIC]
        )
        assertNull(errors[VitalField.SYSTOLIC])
    }

    @Test
    fun loneDiastolic_reportsPairRequiredOnSystolic() {
        val errors = ValidateVitals.validate(input(diastolic = "80"), UnitSystem.METRIC)

        assertEquals(
            VitalsError.BloodPressurePairRequired,
            errors[VitalField.SYSTOLIC]
        )
        assertNull(errors[VitalField.DIASTOLIC])
    }

    @Test
    fun systolicNotGreaterThanDiastolic_reportsRelationError() {
        val errors = ValidateVitals.validate(
            input(systolic = "80", diastolic = "80"),
            UnitSystem.METRIC
        )

        assertEquals(VitalsError.SystolicNotGreater, errors[VitalField.SYSTOLIC])
    }

    @Test
    fun bloodPressureOutOfRange_reportsBounds() {
        val errors = ValidateVitals.validate(
            input(systolic = "49", diastolic = "201"),
            UnitSystem.METRIC
        )

        assertEquals(
            VitalsError.OutOfRange(50.0, 300.0),
            errors[VitalField.SYSTOLIC]
        )
        assertEquals(
            VitalsError.OutOfRange(30.0, 200.0),
            errors[VitalField.DIASTOLIC]
        )
    }

    @Test
    fun boundaryValues_areAccepted() {
        val errors = ValidateVitals.validate(
            input(
                systolic = "300",
                diastolic = "30",
                heartRate = "300",
                glucose = "50",
                temperature = "45",
                spo2 = "100"
            ),
            UnitSystem.METRIC
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun heartRateOutOfRange_reportsBounds() {
        val errors = ValidateVitals.validate(input(heartRate = "19"), UnitSystem.METRIC)

        assertEquals(
            VitalsError.OutOfRange(20.0, 300.0),
            errors[VitalField.HEART_RATE]
        )
    }

    @Test
    fun heartRateDecimal_reportsIntegerRequired() {
        val errors = ValidateVitals.validate(input(heartRate = "72.5"), UnitSystem.METRIC)

        assertEquals(VitalsError.IntegerRequired, errors[VitalField.HEART_RATE])
    }

    @Test
    fun glucoseOutOfRange_metricAndImperial() {
        val metric = ValidateVitals.validate(input(glucose = "0.9"), UnitSystem.METRIC)
        val imperial = ValidateVitals.validate(input(glucose = "17"), UnitSystem.IMPERIAL)

        assertEquals(
            VitalsError.OutOfRange(1.0, 50.0),
            metric[VitalField.GLUCOSE]
        )
        assertEquals(
            VitalsError.OutOfRange(1.0, 50.0),
            imperial[VitalField.GLUCOSE]
        )
    }

    @Test
    fun temperatureOutOfRange_metricAndImperial() {
        val metric = ValidateVitals.validate(input(temperature = "29.9"), UnitSystem.METRIC)
        val imperial = ValidateVitals.validate(input(temperature = "85"), UnitSystem.IMPERIAL)

        assertEquals(
            VitalsError.OutOfRange(30.0, 45.0),
            metric[VitalField.TEMPERATURE]
        )
        assertEquals(
            VitalsError.OutOfRange(30.0, 45.0),
            imperial[VitalField.TEMPERATURE]
        )
    }

    @Test
    fun spo2OutOfRange_reportsBounds() {
        val errors = ValidateVitals.validate(input(spo2 = "101"), UnitSystem.METRIC)

        assertEquals(
            VitalsError.OutOfRange(0.0, 100.0),
            errors[VitalField.SPO2]
        )
    }

    @Test
    fun spo2Decimal_reportsIntegerRequired() {
        val errors = ValidateVitals.validate(input(spo2 = "98.5"), UnitSystem.METRIC)

        assertEquals(VitalsError.IntegerRequired, errors[VitalField.SPO2])
    }

    @Test
    fun commaDecimal_isAccepted() {
        val raw = input(glucose = "5,4", temperature = "36,6")

        assertTrue(ValidateVitals.validate(raw, UnitSystem.METRIC).isEmpty())
        assertEquals(5.4, ValidateVitals.toEntryVitals(raw, UnitSystem.METRIC)!!.bloodGlucoseMmol!!, 0.001)
        assertEquals(36.6, ValidateVitals.toEntryVitals(raw, UnitSystem.METRIC)!!.bodyTemperatureC!!, 0.001)
    }

    @Test
    fun malformedNumber_reportsInvalidFormat() {
        val errors = ValidateVitals.validate(input(glucose = "abc"), UnitSystem.METRIC)

        assertEquals(VitalsError.InvalidFormat, errors[VitalField.GLUCOSE])
    }

    @Test
    fun toEntryVitals_returnsNullWhenAnyFieldIsInvalid() {
        assertNull(
            ValidateVitals.toEntryVitals(
                input(systolic = "120", diastolic = "80", glucose = "not-a-number"),
                UnitSystem.METRIC
            )
        )
    }

    @Test
    fun formatVitals_rendersDisplayUnits() {
        val vitals = EntryVitals(
            bpSystolic = 120.0,
            bpDiastolic = 80.0,
            heartRate = 72,
            bloodGlucoseMmol = 5.4,
            bodyTemperatureC = 36.6,
            spo2Percent = 98
        )

        assertEquals(
            VitalsInput(
                systolic = "120",
                diastolic = "80",
                heartRate = "72",
                glucose = "5.4",
                temperature = "36.6",
                spo2 = "98"
            ),
            ValidateVitals.formatVitals(vitals, UnitSystem.METRIC)
        )
        assertEquals(
            VitalsInput(
                systolic = "120",
                diastolic = "80",
                heartRate = "72",
                glucose = "97",
                temperature = "97.9",
                spo2 = "98"
            ),
            ValidateVitals.formatVitals(vitals, UnitSystem.IMPERIAL)
        )
    }
}
