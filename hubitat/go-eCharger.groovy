/**
 *  go-eCharger
 *
 *  Copyright 2021 Tomas Paulas
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
 *  Change History:
 *
 *    Date        Who             What
 *    ----        ---             ----
 *    2021-08-15  Tomas Paulas   Initial version
 * 
 */

 metadata {
    definition (name: "go-eCharger", namespace: "TOP-automate-egocharger", author: "TOP", importUrl: "https://github.com/TOP-automate/automation/blob/main/hubitat/go-eCharger.groovy") {

        capability "Polling"
        capability "Switch"
      
    }

    preferences {
        input(name: "deviceIP", type: "string", title:"Device IP Address", description: "Enter IP Address of your go-eCharger", required: true, displayDuringSetup: true)
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: 'updateMins', type: 'enum', description: "Select the update frequency", title: "Update frequency (minutes)\n0 is disabled", defaultValue: '5', options: ['0', '1', '2', '5','10','15','30'], required: true
                
        attribute "carstatus", "int"
        attribute "amp", "int"
        attribute "phasesUsed", "int"
        attribute "carcharging", "bool"
        attribute "chargingkWh", "int"
        attribute "chargingallowed", "bool"
        command "setAMPs", ["NUMBER"]
        command "setPhases", ["NUMBER"]
    }
}

def logsOff() {
    log.info "Device debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {

    log.info "Device updating.."
    log.info "Debug logging is: ${logEnable == true}"
    if(updateMins != "0") {
        log.info("go-eCharge status is autoupdated every ${updateMins} minutes.")
        schedule("0 */${updateMins} * ? * *", poll)
    }
    else
    {
        unschedule(poll)
        log.info("go-eCharge status autoupdate is off.")
    }
    if (logEnable) runIn(1800, logsOff)
    log.info "Device update complete."
}

def parse(String description) {
    log.debug(description)
}


def poll() {
    try {   
        if (logEnable) log.debug "Calling GET ${settings.deviceIP}/api/status"
        httpGet([uri:"http://${settings.deviceIP}/api/status",contentType: "application/json", timeout:45]) { resp ->
            if (logEnable) log.debug "Result: ${resp.data}"
            if (resp.success) {
                
               
                //sendEvent(name: "energy", value: resp.data.value, descriptionText: "battery")
                sendEvent(name:"amp", value: resp.data.amp.toInteger(), isStateChange: true)
                sendEvent(name:"carstatus", value: resp.data.car.toInteger(), isStateChange: true)
                sendEvent(name:"phasesUsed", value: resp.data.psm.toInteger()==1 ? 1 : 3)
                sendEvent(name:"chargingkWh", value: resp.data.nrg[11].toInteger()/1000, isStateChange: true)
                sendEvent(name:"carcharging", value: resp.data.car.toInteger()==2 ? true : false, isStateChange: true)
                sendEvent(name:"chargingallowed", value: resp.data.alw, isStateChange: true)
                if (logEnable) log.debug("go-eCharger amp is ${resp.data.amp}A ")
                //log.info "ChargingAllowed: ${resp.data.alw}"
                //log.info device.currentValue("switch")
                if (resp.data.alw == true && device.currentValue("switch") == "off") sendEvent(name: "switch", value: "on")
                if (resp.data.alw  == false && device.currentValue("switch") == "on") sendEvent(name: "switch", value: "off")
            }
            else
            {
               log.warn "Call to go-eCharger was not successfull: ${resp}, setting all values to 0"
               sendEvent(name: "amp", value: 0, isStateChange: true)
               sendEvent(name: "carstatus", value: 0)
               sendEvent(name: "carcharging", value: false)
            }
        }
    } catch (Exception e) {
        log.warn "Error occured: ${e.message}, setting battery level to 0 and rellayconnected to FALSE"
        sendEvent(name: "amp", value: 0, isStateChange: true)
        sendEvent(name:"carstatus", value: 0)
    }
}


def setEnergy(energy) {
    def descriptionText = "${device.displayName} is ${energy} energy"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "energy", value: energy, descriptionText: descriptionText)
}

def setPower(power) {
    def descriptionText = "${device.displayName} is ${power} power"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "power", value: power, descriptionText: descriptionText)
}

def off() {
    try {   
        if (logEnable) log.debug "Calling GET ${settings.deviceIP}/api/set?frc=1"
        httpGet([uri:"http://${settings.deviceIP}/api/set?frc=1",contentType: "application/json", timeout:45]) { resp ->
            if (logEnable) log.debug "Result: ${resp.data}"
            if (resp.success) {
                //sendEvent(name: "chargingallowed", value: false)
                sendEvent(name: "switch", value: "off")
                runIn(10,poll)
            }
            else
            {
               log.warn "Call to go-eCharger was not successfull: ${resp}"
               runIn(10,poll)
            }
        }
    } catch (Exception e) {
        log.warn "Error occured: ${e.message}"
    }
}

def on() {
	try {   
        if (logEnable) log.debug "Calling GET ${settings.deviceIP}/api/set?frc=0"
        httpGet([uri:"http://${settings.deviceIP}/api/set?frc=0",contentType: "application/json", timeout:45]) { resp ->
            if (logEnable) log.debug "Result: ${resp.data}"
            if (resp.success) {
                //sendEvent(name: "chargingallowed", value: true)
                sendEvent(name: "switch", value: "on")
                runIn(10,poll)
            }
            else
            {
               log.warn "Call to go-eCharger was not successfull: ${resp}, setting all values to 0"
               runIn(10,poll)
            }
        }
    } catch (Exception e) {
        log.warn "Error occured: ${e.message}"
    }
}


void setAMPs(amps)
{
   
    try {   
        if (logEnable) log.debug "Calling GET ${settings.deviceIP}/api/set?amp=${amps}"
        httpGet([uri:"http://${settings.deviceIP}/api/set?amp=${amps}",contentType: "application/json", timeout:45]) { resp ->
            if (logEnable) log.debug "Result: ${resp.data}"
            if (resp.success) {
                runIn(10,poll)
            }
            else
            {
               log.warn "Call to go-eCharger was not successfull: ${resp}"
               runIn(10,poll)
            }
        }
    } catch (Exception e) {
        log.warn "Error occured: ${e.message}"
    }
    
    
    
}

void setPhases(phasesNo)
{
   
    try {   
        if (logEnable) log.debug "Calling GET ${settings.deviceIP}/api/set?psm=${phasesNo}"
        httpGet([uri:"http://${settings.deviceIP}/api/set?psm=${phasesNo}",contentType: "application/json", timeout:45]) { resp ->
            if (logEnable) log.debug "Result: ${resp.data}"
            if (resp.success) {
                
                runIn(10,poll)
            }
            else
            {
               log.warn "Call to go-eCharger was not successfull: ${resp}"
               runIn(10,poll)
            }
        }
    } catch (Exception e) {
        log.warn "Error occured: ${e.message}"
    }
    
    
    
}
