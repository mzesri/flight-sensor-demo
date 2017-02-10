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
