/**
 *  Pentair Link2O Relay Switch
 *
 *  Copyright 2019 Tim Raymond
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
 *  7/28/2019 - Version 1.0.0
 *              Initial release.
 *
 */

private getRootUrl()		{ "https://calc.mylink2o.com" }
private getAuthUri()		{ "/api/user/login" }
private getRelayStartUri()	{ "/api/relay/start" }
private getRelayStopUri()	{ "/api/relay/stop" }
private getRelayStatusUri()	{ "/api/relay/status" }


metadata {
	definition (name: "Pentair Mylink Relay Switch", namespace: "Timeteo", author: "Tim Raymond", cstHandler: true) {
		capability "Switch"
		capability "Refresh"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	// UI tile definitions
	tiles {
        tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}
        standardTile("refresh", "device.switch", decoration: "flat", height: 2, width: 2) {
            state "default", label: "", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

		main "switch"
			details (["switch","refresh"])
		}
    }
}

preferences {   
	section("Settings"){
		input "pentEmail", "text", title: "Email", required: true
		input "pentPassword", "password", title: "Password", required: true
		input "pentId", "text", title: "Pentair Device ID", required: true
		input "pentRelay", "text", title: "Relay Number", required: true
	}
}


// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute
}

def installed() {
    poll()
    runEvery30Minutes(poll)
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
 * Calls mylink2o.com site to request a current relay status.
 * Unlike the on/off/api_token calls, mylink2o.com returns poorly formatted HTML
 * which includes the status for all relays. This code simply iterates through
 * the HTML looking for text that is specific to the On/Off function, decides
 * which relay the test corresponds to and then looks for text for the given 
 * relay that indicates on/off.
 *
*/
def getStatus() {
	log.debug "Executing: getStatus"
    def thisStatus = "off"
    def requestParams = [
    	uri			: "${rootUrl}${relayStatusUri}",
        headers		: ["Content-Type": "application/x-www-form-urlencoded", "Authorization": "Bearer ${apiToken}"],
        body		: "id=${pentId}&outlet=${pentRelay}"
        ]
    log.debug "requestParams: ${requestParams}"
    try {
    	httpPost(requestParams) { resp ->
            def cleanHTML = resp.data.toString().trim()
            def index = new Integer[pentRelay.toInteger()+1]
            for (int i = 1; i <= pentRelay.toInteger(); i++) {
                if (i == 1) {
                	index[i] = cleanHTML.indexOf("TAP TO")
                } else {
                   	index[i] = cleanHTML.indexOf("TAP TO",index[i-1]+7)
                    }
                }
                if (cleanHTML.indexOf("TAP TO DEACTIVATE", index[pentRelay.toInteger()]) > 0) {
                	thisStatus = "on"
                    }
             log.debug "thisStatus: ${thisStatus}"
             }
		} catch (e) {
    		log.error "something went wrong: $e"
		}
        sendEvent(name: 'switch', value: thisStatus)
}

/**
 * Calls mylink2o.com site to request current relay be turned on.
 *
*/
def on() {
	log.debug "Executing: On"
    boolean success
    def message
    def requestParams = [
    	uri			: "${rootUrl}${relayStartUri}",
        headers		: ["Content-Type": "application/x-www-form-urlencoded", "Authorization": "Bearer ${apiToken}"],
        body		: "id=${pentId}&outlet=${pentRelay}"
        ]
    log.debug "requestParams: ${requestParams}"
    try {
    	httpPost(requestParams) { resp ->
    		log.debug "resp.data: ${resp.data}"
        	log.debug "success: ${resp.data.success.value}"
        	success = resp.data.success.value.toBoolean()
        	message = resp.data.message.value
            }
		if (success) {
			log.debug "message: ${message}"
            } else { 
            	log.error "Turn on device ${pentRelay} failed with response: ${resp.data.message.value}"
                message = resp.data.message.value
                }
		} catch (e) {
    		log.error "something went wrong: $e"
		}	
}

/**
 * Calls mylink2o.com site to request current relay be turned off.
 *
*/
def off() {
	log.debug "Executing: Off"
    boolean success
    def message
    def requestParams = [
    	uri			: "${rootUrl}${relayStopUri}",
        headers		: ["Content-Type": "application/x-www-form-urlencoded", "Authorization": "Bearer ${apiToken}"],
        body		: "id=${pentId}&outlet=${pentRelay}"
        ]
    log.info "requestParams: ${requestParams}"
    try {
    	httpPost(requestParams) { resp ->
    	log.debug "resp.data: ${resp.data}"
        log.debug "success: ${resp.data.success.value}"
        success = resp.data.success.value.toBoolean()
        message = resp.data.message.value
		if (success) {
			log.debug "message: ${message}"
            } else { 
            	log.error "Turn on device ${pentRelay} failed with response: ${resp.data.message.value}"
                message = resp.data.message.value
                }
            }
		} catch (e) {
    		log.error "something went wrong: $e"
		}
}