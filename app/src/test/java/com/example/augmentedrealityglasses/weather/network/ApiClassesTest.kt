package com.example.augmentedrealityglasses.weather.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Date

class APIWeatherTest {

    @Test
    fun idParsesNumericStringTest() {
        val w = APIWeather(main = "Clear", _id = "800", icon = "01d", description = "clear sky")
        assertThat(w.id).isEqualTo(800)
    }

    @Test(expected = NumberFormatException::class)
    fun idParsingThrowsNonNumericTest() {
        val w = APIWeather(main = "Clear", _id = "not_valid_id", icon = "01d", description = "")
        w.id
    }
}

class APIMainTest {

    @Test
    fun tempRoundTest() {
        val m = APIMain(
            _temp = "25.2",
            _feels_like = "26.6",
            _temp_min = "22.49",
            _temp_max = "27.50",
            _pressure = "1012.7",
            humidity = 55
        )
        assertThat(m.temp).isEqualTo(25)
        assertThat(m.feels_like).isEqualTo(27)
        assertThat(m.temp_min).isEqualTo(22)
        assertThat(m.temp_max).isEqualTo(28)
        assertThat(m.pressure).isEqualTo(1013)
    }

    @Test
    fun invalidNumbersTest() {
        val m = APIMain(
            _temp = "not_valid_number",
            _feels_like = "not_valid_number",
            _temp_min = " ",
            _temp_max = "",
            _pressure = "",
            humidity = 0
        )
        assertThat(m.temp).isEqualTo(0)
        assertThat(m.feels_like).isEqualTo(0)
        assertThat(m.temp_min).isEqualTo(0)
        assertThat(m.temp_max).isEqualTo(0)
        assertThat(m.pressure).isEqualTo(0)
    }
}

class APIWindTest {

    @Test
    fun speedConversionTest() {

        assertThat(APIWind("0").speed).isEqualTo(0f)

        assertThat(APIWind("1").speed).isWithin(0.0001f).of(3.6f)

        assertThat(APIWind("3.5").speed).isWithin(0.0001f).of(12.6f)
    }

    @Test
    fun invalidSpeedTest() {
        assertThat(APIWind("not_valid_speed").speed).isEqualTo(0f)
        assertThat(APIWind(" ").speed).isEqualTo(0f)
    }
}

class APIWeatherConditionAndForecastTest {

    @Test
    fun weatherReturnsFirstElementTest() {
        val weatherList = listOf(
            APIWeather("Clear", "800", "01d", "a"),
            APIWeather("Rain", "500", "10d", "b")
        )
        val cond = APIWeatherCondition(
            _weather = weatherList,
            coord = APICoord("45.0", "9.0"),
            main = APIMain("25", "26", "22", "27", "1013", 50),
            wind = APIWind("3.5"),
            sys = APISys("IT", Date(1700000000L * 1000), Date(1700040000L * 1000)),
            name = "Milan",
            dt = Date(1700012345L * 1000)
        )

        assertThat(cond.weather).isEqualTo(weatherList[0])
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun weatherThrowsWheListIsEmptyTest() {
        val cond = APIWeatherCondition(
            _weather = emptyList(),
            coord = APICoord("0", "0"),
            main = APIMain("0", "0", "0", "0", "0", 0),
            wind = APIWind("0"),
            sys = APISys("X", Date(0), Date(0)),
            name = "Milan",
            dt = Date(0)
        )
        cond.weather
    }

    @Test
    fun forecastWeatherReturnsFirstElementTest() {
        val weatherList = listOf(APIWeather("Clear", "800", "01d", ""))
        val f = APIForecast(
            dt = Date(1700016000L * 1000),
            main = APIMain("24.0", "24.0", "22.0", "25.0", "1012", 55),
            _weather = weatherList,
            wind = APIWind("3.0")
        )
        assertThat(f.weather).isEqualTo(weatherList[0])
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun forecastWeatherThrowsWheListIsEmptyTest() {
        val f = APIForecast(
            dt = Date(0),
            main = APIMain("0", "0", "0", "0", "0", 0),
            _weather = emptyList(),
            wind = APIWind("0")
        )
        f.weather
    }
}