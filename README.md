# flight-sensor-demo
A step-to-step guide to setting up a raspberry pi as a flight data source and displaying the flights on an ArcGIS online webmap

Ingredients:

1. Raspberry Pi (2 model B or 3 model B) 
2. NooElec NESDR Mini 2 SDR & DVB-T USB Stick (RTL2832 + R820T2) with Antenna and Remote Control

This guide assumes you already have the Raspberry Pi provisioned.  This includes the OS (Raspbian is the one I used), wired or wireless internet connection, and ssh enabled.  Everything in this guide can be done via ssh or via a command window on the Raspberry Pi desktop.

## Enable the NESDR USB stick and install dump1090

Reference: http://www.satsignal.eu/raspberry-pi/dump1090.html

1. Always update and upgrade your OS 
    
    ```
    sudo apt-get update
    sudo apt-get upgrade
    sudo reboot
    ```
2. Install tools and related software
    
    ```
    sudo apt-get install git-core
    sudo apt-get install git
    sudo apt-get install cmake
    sudo apt-get install libusb-1.0-0-dev
    sudo apt-get install build-essential
    ```
3. Install the rtl-sdr software that enables the USB stick
   
   ```
   git clone git://git.osmocom.org/rtl-sdr.git
   cd rtl-sdr/
   mkdir build
   cd build
   cmake ../ -DINSTALL_UDEV_RULES=ON
   make -j5
   sudo make install
   ```
4. Make sure the following command returns nothing
    
    ```
    sudo ldconfig
    ```
5. Insert the NEDSDR USB stick to the raspberry Pi

6. Tell the system what the new device is allowed to do
    
    ```
   18  sudo ldconfig
   19  cd ~
   20  sudo cp ./rtl-sdr/rtl-sdr.rules /etc/udev/rules.d/
   21  sudo reboot
   ```
   
7. Test the device for the first time with the following command.
    
    ```
    rtl_test -t
    ```
    You may get an error that looks like this:
    
    Found 1 device(s):
    0: Generic RTL2832U
    Using device 0: Generic RTL2832U
    Kernel driver is active, or device is claimed by second instance of librtlsdr.
    In the first case, please either detach or blacklist the kernel module
    (dvb_usb_rtl28xxu), or enable automatic detaching at compile time.
    usb_claim_interface error -6
    Failed to open rtlsdr device #0.
    
8. If you did get this error then edit add the following blacklist text to solve the problem.
    
    ```
    sudo vi /etc/modprobe.d/raspi-blacklist.conf
    ```
    add the following lines in the file.
    
    ```
    blacklist dvb_usb_rtl28xxu
    blacklist rtl2832
    blacklist rtl2830
    ```
    Save and exit the editor.
    
    ```
    sudo reboot
    ```
    
9. Install dump1090
    
    ```
    cd ~
    git clone git://github.com/MalcolmRobb/dump1090.git
    cd dump1090/
    make
    sudo apt-get install pkg-config
    make
    ./dump1090 --interactive --net
    ```
    At this point, if you should be able to see the flight data it captured.  You can use the following command to see all dump1090 options.
    
    ```
    ./dump1090 --help
    ```
    If you don't want the flights to be displayed on the console, use the --quiet option instead of the --interactive option.     The --net option starts a web server on the localhost:8080.  You can poll the flights from this web server by the             following command:
    
    ```
    curl http://localhost:8080/data.json
    ```
    
    This marks the end of the device setup.
    
    
## Install Kura and the FlightSensor Package on Raspberry Pi

Eclipse Kura is an Eclipse IoT project that provides a platform for building IoT gateways. It is a smart application container that enables remote management of such gateways and provides a wide range of APIs for allowing you to write and deploy your own IoT application.  

Reference: https://eclipse.github.io/kura/doc/raspberry-pi-quick-start.html

1. Make sure your java version is 1.8.  

   ```
   java -version
   ```
   
   If it is not 1.8, you need to refer to the above link to install the latest version.
   
2. Install the gdebi command line tool:

   ```
   sudo apt-get install gdebi-core
   ```
   
3. Download the Kura project. 

   ```
   wget http://download.eclipse.org/kura/releases/2.1.0/kura_2.1.0_raspberry-pi-2-3-nn_installer.deb
   ```
   
4. Install Kura and reboot

   ```
   sudo gdebi kura_2.1.0_raspberry-pi-2-3-nn_installer.deb
   ```
   
5. Login to Kura

   Find the ip or host of the Raspberry Pi.  Goto http://[raspberry_pi_ip_or_host]/kura on a browser.  Whether you should use ip or hostname all depneds on your network setup.  If this does not work, you need to go to the desktop of the raspberry pi and use the browser there.  Then you can use http://localhost/kura and the url.  When you initially connect to Kura, you will be prompted with a log in page.  Type admin/admin as the user/password.  Now you are in the Kura UI.
   
6. Configure the Cloud services

   Click on the Cloud Services label on the left hand.  Select the MqttDataTransport tab.  If you have your own broker, enter the url under **broker-url**.  Otherwise, you can use the Eclipse IoT sandbox url which is 
   
   ```
   mqtt://iot.eclipse.org:1883/
   ```
   
   Enter a client-id.  The topic your raspberry pi will write to is ${account-name}/${client-id}/${service-name}/${semantic topic}.  On this page, you can define the account-name and the client-id.
   
   Click **Apply** to save the change.
   
   Select the DataService tab.  Change **Enable automatic connect of the Data Publishers on startup** to true.  
   
   Click **Apply** to save the change.
   
7. Compile and install the FlightSensor package.
   
   Download the flightsensor eclipse project from this repo.  Import the project to Eclipse.  Follow this example https://eclipse.github.io/kura/doc/hello-example.html to build the OSGI bundle and create a deployment package.
   
   Once you have the .dp file, you can use the Kura UI that is open on your mac or pc to import the package.  If you run the Kura UI on the pi, you will need to scp the file to the pi first.
   
   ```
   scp flight_sensor.dp pi@[ip]
   ```
   
   Then, on the Kura UI, click on **Packages** on the left side.  Click on **+ Install/Upgrade** to install the package.
   
   Once the package is successfully installed, you will see the FlightSensor under Services on the left side of the Kura UI.
   
8.  Configure Flight Sensor

   There are three properties you need to set for Flight Sensor.  
   
   - url: This should be set to http://localhost:8080/data.json
   - Publish.rate: This property determines how often you send the flight data to the Mqtt server.  In my demo, I set it to 6 secnods.
   - publish.semanticTopic: This is the last part of the topic string.  So, if your account name is "account-name", your client id is "abc", your flight sensor package name is FlightSensor and your semanticTopic is set to "data", your topic string will be "account-name/abc/FlightSensor/data".  
   
   Click **Apply** to save the change.
   
   Now you are able to receive the flight data nad send the flight data to the mqtt server at a fixed interval.  You can use the MQTTLens (https://chrome.google.com/webstore/detail/mqttlens/hemojaaeigabkbcookmlgmdigohjobjm?hl=en) to connect to the Mqtt broker and subscribe to your topic.
   
## Set up a GeoEvent server to publish the flight info in JSON format to feature services.

This is assuming you have access to and the knowledge of the ArcGIS server and GeoEvent extension.  This is really the focus of this exercise since this demo is created for the IoT session of the Esri DevSummit.

Reference: https://server.arcgis.com/en/geoevent/

1. Install the Mqtt transport on Geoevent

   Download or clone the Mqtt-for-geoevent project from here https://github.com/Esri/mqtt-for-geoevent.
   
   Since it is a maven project, you need java and maven installed before you can build the project.  You build the project with the following command:
   
   ```
   mvn clean install
   ```
   
   This will create the following jar file:
   
   ....\mqtt-for-geoevent\mqtt-transport\target\mqtt-transport-10.5.0.jar
   
   Copy this jar file and paste it in the deploy folder under your GeoEvent install folder.  If you need to copy it to a remote machine, make sure you copy the jar file to a temp folder first on the remote machine and then drag it to the deploy folder.  You can also use the ArcGIS GeoEvent Manager->Site->Components->Transports->Add Local Transport tool to import the jar.  This will install the custom transport on Geoevent.
   
   
2. Create an Inbound Connector from the Newly Installed transport.

   Make sure you choose the MqttInboundTransport and the Generic-JSON adapter for this connector.
   
   ![Image of MQTT-json inbound connector]
   (https://github.com/mzesri/flight-sensor-demo/blob/master/images/Mqtt-Connector.png)
   
2. Create GeoEvent Definitions.

   Create two GeoEvent Definitions as illustrated in the diagram below.  The diffrence between these two GeoEvent Definitions is that one has an extra field called received_time which is a time stamp.  We will use a Field Mapper to update this field with the received timestamp when we save the data into the feature service.
   
   ![Image of GeoEvent Definition FlightSensorGED]
   (https://github.com/mzesri/flight-sensor-demo/blob/master/images/FlightSensorGED.png)
   
   ![Image of GeoEvent Definition FlightSensorGED-withTimeStamp]
   (https://github.com/mzesri/flight-sensor-demo/blob/master/images/FlightSeonsorGED-withTS.png)
   
   
3. Create Two new Feature Services.

   Use your favorite too to create two new feature services.  In my case, they are called Flights and Flights_trail.  Their schema should be the same.  They should all match the FlightSensorGED-withTimeStamp GoeEvent Definition.  
   
4. Create an Input Connector using the 

   

