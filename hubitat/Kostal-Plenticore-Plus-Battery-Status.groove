/**
 *  Kostal Plenticore Plus Battery Status
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
 *    2021-03-27  Tomas Paulas   Initial version
 * 
 */

metadata {
    definition (name: "Kostal Plenticore Plus Battery Status", namespace: "kostal", author: "TOP-automate", importUrl: "https://raw.githubusercontent.com/ogiewon/Hubitat/master/Drivers/http-momentary-switch.src/http-momentary-switch.groovy") {

        capability "Polling"
        capability "Battery"
    }

    preferences {
        input(name: "deviceIP", type: "string", title:"Device IP Address", description: "Enter IP Address of your Kostal Relay server", required: true, displayDuringSetup: true)
        input(name: "devicePort", type: "string", title:"Device Port", description: "Enter Port of your Kostal Relay server (defaults to 9000)", defaultValue: "9000", required: false, displayDuringSetup: true)
        input(name: "processdata", type: "string", title:"Process Data", description: "Rest of the URL, include forward slash.", defaultValue: "devices:local:battery", displayDuringSetup: true)
        input(name: "devicelocal", type: "string", title:"Device Local", description: "Rest of the URL, include forward slash.", defaultValue: "SoC", displayDuringSetup: true)
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: 'updateMins', type: 'enum', description: "Select the update frequency", title: "Update frequency (minutes)\n0 is disabled", defaultValue: '5', options: ['0', '1', '2', '5','10','15','30'], required: true
                
        attribute "relayconnected", "bool"
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
        log.info("Kostal Plenticore Plus battery status is autoupdated every ${updateMins} minutes.")
        schedule("0 */${updateMins} * ? * *", poll)
    }
    else
    {
        unschedule(poll)
        log.info("Kostal Plenticore Plus battery status autoupdate is off.")
    }
    if (logEnable) runIn(1800, logsOff)
    log.info "Device update complete."
}

def parse(String description) {
    log.debug(description)
}


def poll() {
    try {   
        if (logEnable) log.debug "Calling GET ${settings.deviceIP}:${devicePort}${devicePath}?processdata=${processdata}&devicelocal=${devicelocal}"
        httpGet("http://${settings.deviceIP}:${devicePort}${devicePath}?processdata=${processdata}&devicelocal=${devicelocal}") { resp ->
            if (logEnable) log.debug "Result: ${resp.data.value}"
            if (resp.success) {
                //sendEvent(name: "energy", value: resp.data.value, descriptionText: "battery")
                sendEvent(name: "battery", value: resp.data.value, isStateChange: true)
                sendEvent(name:"relayconnected", value: true)
                log.info("Kostal Plenticore Plus Battery level is now ${resp.data.value}% ")
            }
            else
            {
               log.warn "Call to relay was not successfull: ${resp}, setting battery level to 0 and rellayconnected to FALSE"
               sendEvent(name: "battery", value: 0, isStateChange: true)
               sendEvent(name:"relayconnected", value: false)
            }
        }
    } catch (Exception e) {
        log.warn "Error occured: ${e.message}, setting battery level to 0 and rellayconnected to FALSE"
        sendEvent(name: "battery", value: 0, isStateChange: true)
        sendEvent(name:"relayconnected", value: false)
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
