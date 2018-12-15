#!groovy
def buildArduino(config, String buildFlags, String sketch, String key) {
	def root              = '/opt/arduino-1.8.8/'
	if (config.nightly_arduino_ide)
	{
		root = '/opt/arduino-nightly/'
	}
	def esp8266_tools     = '/opt/arduino-nightly/hardware/esp8266com/esp8266/tools'
	def jenkins_root      = '/var/lib/jenkins/'
	def builder           = root+'arduino-builder'
	def standard_args     = ' -warnings="all"' //-verbose=true
	def builder_specifics = ' -hardware '+root+'hardware -tools '+root+'hardware/tools/avr -tools '+
		root+'tools-builder -tools '+esp8266_tools+' -built-in-libraries '+root+'libraries'
	def jenkins_packages  = jenkins_root+'.arduino15/packages'
	def site_specifics    = ' -hardware '+jenkins_packages+' -tools '+jenkins_packages
	def repo_specifics    = ' -hardware hardware -libraries . '
	def build_cmd         = builder+standard_args+builder_specifics+site_specifics+repo_specifics+buildFlags
	sh """#!/bin/bash
				printf "\\e[1m\\e[32mBuilding \\e[34m${sketch} \\e[0musing \\e[1m\\e[36m${build_cmd}\\e[0m\\n"
				${build_cmd} ${sketch} 2>> compiler_${key}.log"""
}

def parseWarnings(String key) {
	warnings canResolveRelativePaths: false, canRunOnFailed: true, categoriesPattern: '',
 		defaultEncoding: '',
 		excludePattern: '''.*/EEPROM\\.h,.*/Dns\\.cpp,.*/socket\\.cpp,.*/util\\.h,.*/Servo\\.cpp,
 											 .*/Adafruit_NeoPixel\\.cpp,.*/UIPEthernet.*,.*/SoftwareSerial\\.cpp,
 											 .*/pins_arduino\\.h,.*/Stream\\.cpp,.*/USBCore\\.cpp,.*/Wire\\.cpp,
 											 .*/hardware/esp8266.*,.*/libraries/SD/.*''',
 		healthy: '', includePattern: '', messagesPattern: '',
 		parserConfigurations: [[parserName: 'Arduino/AVR', pattern: 'compiler_'+key+'.log']],
 		unHealthy: '', unstableNewAll: '0', unstableTotalAll: '0'
	sh """#!/bin/bash
				echo "Compiler warnings/errors:"
				printf "\\e[101m"
				cat compiler_${key}.log
				printf "\\e[0m"
				rm compiler_${key}.log"""
}

def buildMySensorsMicro(config, sketches, String key) {
	def fqbn = '-fqbn MySensors:avr:MysensorsMicro -prefs build.f_cpu=1000000 -prefs build.mcu=atmega328p'
	config.pr.setBuildStatus(config, 'PENDING', 'Toll gate (MySensorsMicro - '+key+')', 'Building...', '${BUILD_URL}flowGraphTable/')
	try {
		for (sketch = 0; sketch < sketches.size(); sketch++) {
			if (sketches[sketch].path != config.library_root+'examples/GatewayESP8266/GatewayESP8266.ino' &&
					sketches[sketch].path != config.library_root+'examples/GatewayESP8266MQTTClient/GatewayESP8266MQTTClient.ino' &&
					sketches[sketch].path != config.library_root+'examples/GatewayESP8266OTA/GatewayESP8266OTA.ino' &&
			        sketches[sketch].path != config.library_root+'examples/GatewayESP32/GatewayESP32.ino' &&
			        sketches[sketch].path != config.library_root+'examples/GatewayESP32MQTTClient/GatewayESP32MQTTClient.ino' &&
					sketches[sketch].path != config.library_root+'examples/GatewayGSMMQTTClient/GatewayGSMMQTTClient.ino' &&
					sketches[sketch].path != config.library_root+'examples/SensebenderGatewaySerial/SensebenderGatewaySerial.ino') {
				buildArduino(config, fqbn, sketches[sketch].path, key+'_MySensorsMicro')
			}
		}
	} catch (ex) {
		echo "Build failed with: "+ ex.toString()
		config.pr.setBuildStatus(config, 'FAILURE', 'Toll gate (MySensorsMicro - '+key+')', 'Build error', '${BUILD_URL}')
		throw ex
	} finally {
		parseWarnings(key+'_MySensorsMicro')
	}
	if (currentBuild.currentResult == 'UNSTABLE') {
		config.pr.setBuildStatus(config, 'ERROR', 'Toll gate (MySensorsMicro - '+key+')', 'Warnings found', '${BUILD_URL}warnings2Result/new')
		if (config.is_pull_request) {
			error 'Termiated due to warnings found'
		}
	} else if (currentBuild.currentResult == 'FAILURE') {
		config.pr.setBuildStatus(config, 'FAILURE', 'Toll gate (MySensorsMicro - '+key+')', 'Build error', '${BUILD_URL}')
	} else {
		config.pr.setBuildStatus(config, 'SUCCESS', 'Toll gate (MySensorsMicro - '+key+')', 'Pass', '')
	}
}

return this
