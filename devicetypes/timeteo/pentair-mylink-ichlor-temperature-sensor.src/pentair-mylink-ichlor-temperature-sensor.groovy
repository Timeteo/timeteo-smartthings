/**
 *  Pentair Link2O iChlor Temperature Sensor
 *
 *  Copyright 2020 Tim Raymond
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  5/10/2020 - Version 1.0.0
 *              Initial release.
 *
 */

private getRootUrl()		{ "https://calc.mylink2o.com" }
private getAuthUri()		{ "/api/user/login" }
private getTempUri()		{ "/api/ichlor/status" }

 
metadata {
	definition (name: "Pentair Mylink iChlor Temperature Sensor", namespace: "Timeteo", author: "Tim Raymond", cstHandler: true) {
		capability "Temperature Measurement"
		capability "Sensor"
		capability "Refresh"

		attribute "lastUpdate", "string"
	}

	// simulator metadata
	simulator {
		for (int i = 0; i <= 100; i += 10) {
			status "${i}F": "temperature: $i F"
		}
	}

	// UI tile definitions
tiles(scale: 2) {
		multiAttributeTile(name: "temperature", type: "generic", width: 6, height: 4, canChangeIcon: true) {
      			tileAttribute ("device.temperature", key: "PRIMARY_CONTROL") {
					attributeState "temperature", label:'${currentValue}°',
						backgroundColors:[
							[value: 40, color: "#003d99"],
							[value: 64, color: "#00ace6"],
							[value: 68, color: "#33ccff"],
							[value: 72, color: "#00e6e6"],
							[value: 75, color: "#239B56"],
							[value: 79, color: "#b3ff1a"],
							[value: 82, color: "#ffeb99"],
							[value: 85, color: "#ffdb4d"],
							[value: 89, color: "#ffcc00"],
							[value: 92, color: "#ff0000"],
							[value: 94, color: "#b30000"]
					    ]
				}
		}

		valueTile("lastUpdate", "device.lastUpdate", decoration: "flat", height: 2, width: 3) {
            state "default", label:'Last update:\n${currentValue}'
        }
        
		valueTile("refresh", "device.refresh", decoration: "flat",  height: 2, width: 3) {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "temperature"
		details(["temperature","lastUpdate","refresh"])
	}
}

preferences {   
	section("Settings"){
		input "pentEmail", "text", title: "Email", required: true
		input "pentPassword", "password", title: "Password", required: true
		input "pentId", "text", title: "Pentair Device ID", required: true
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def installed() {
    poll()
}

def updated() {
    poll()
}

def uninstalled() {
    unschedule()
}
// self-explanatory
def poll() {
	log.debug "Executing: poll"
    // Last update time stamp
    def timeZone = location.timeZone ?: timeZone(timeOfDay)
    def timeStamp = new Date().format("MMM dd yyyy EEE h:mm:ss a", location.timeZone)
    sendEvent(name: "lastUpdate", value: timeStamp)
    getStatus()
}

// self-explanatory
def refresh() {
	poll()
}


/**
 * Calls mylink2o.com site to request a current api_token.
 *
 * @return api_token.
*/
def getApiToken() {
	log.debug "Executing: getApiToken"
    boolean success
    def message
    def api_token
    def requestParams = [
    	uri			: "${rootUrl}${authUri}",
        headers		: ["Content-Type": "application/x-www-form-urlencoded"],
        body		: "email=${pentEmail}&password=${pentPassword}"
        ]
    log.debug "requestParams: [uri:${requestParams.uri},headers:${requestParams.headers},body:email=${pentEmail}&password=********]"
	try {
	   	httpPost(requestParams) { resp ->
        	log.debug "resp.data: ${resp.data}"
            log.debug "success: ${resp.data.success.value}"
            success = resp.data.success.value.toBoolean()
			if (success) {
                api_token = resp.data.api_token.value
				log.debug "api_token: ${api_token}"
                } else { 
                   	log.error "Get api_token failed with response: ${resp.data.message.value}"
                    message = resp.data.message.value
                    }
           	}
		} catch (e) {
    		log.error "something went wrong: $e"
		}
    if (api_token || success) {
    	return api_token
    } else {
    	log.error "Error retrieving api_token, ${api_token}, ${message}, ${success}"
    }
}


/**
 * Calls mylink2o.com site to request the current iChlor status.
 * mylink2o.com returns poorly formatted HTML which includes the status of 
 * the iChlor. This code simply parses the HTML looking for text that is
 * specific to the temperature and then looks for the temperature value.
 *
*/
def getStatus() {
	log.debug "Executing: getStatus"
    def temperature = ""
    def requestParams = [
    	uri			: "${rootUrl}${tempUri}",
        headers		: ["Content-Type": "application/x-www-form-urlencoded", "Authorization": "Bearer ${apiToken}"],
        body		: "id=${pentId}"
        ]
    log.debug "requestParams: ${requestParams}"
    try {
    	httpPost(requestParams) { resp ->
            def cleanHTML = resp.data.toString().trim()
			def error = cleanHTML.indexOf("Communication Error")
			if (error != -1){
				temperature = "--"
				log.debug "Communication Error"
			} else {
				def startParse = cleanHTML.indexOf("TEMPERATURE:")
			
				for (int i = startParse; i <= cleanHTML.length()-1; i++) {
					if (cleanHTML[i].isInteger()) {
						temperature = temperature + cleanHTML[i]
					}
				}
				log.debug "Temperature: ${temperature}"
			 }
        }
	} catch (e) {
    	log.error "something went wrong: $e"
	}
    sendEvent(name: "temperature", value: temperature, displayed: true)
}
