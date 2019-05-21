# Revised Smart Weather Station
After the WeatherUnderground (WU) API was shutdown, all the device handlers using it (including the stock SmartThings (ST) Smart Weather Station Tile) would break if not updated.

SmartThings switched from the deprecated WU API to its replacement, The Weather Channel (TWC) API. The TWC API does not provide the same information as the WU API did, and a big gap is with Personal Weather Stations (PWS).

The SmartThings implementation (as of March 2019) requires either zip code OR PWS. Since the TWC API for PWS does not return all the same data attributes as zip code does, this meant when using PWS, you would have fewer data attributes available than when using zip.

This is a modified version of the ST handler. If you want PWS data, it requires both a zip and PWS ID. It will pull all attributes from zip, and then will overwrite those with PWS attributes for the attributes the PWS API supports.

The following attributes are coming from PWS.
* Temp
* Feels Like
* Humidity
* Wind
* Wind Vector
* Location (PWS name)
* UV Index (if the PWS provides this data)
* Alerts

The following attributes are coming from zip:
* uvDescription
* localSunrise
* localSunset
* illuminance
* percentPrecip
* forecastIcon
* forecastToday
* forecastTonight
* forecastTomorrow