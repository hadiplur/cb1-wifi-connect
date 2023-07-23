# CB1 WîFi Connect
This Android App is shipped with the CB1 System Image.
It allows the CB1 to be connected to a WiFi-Network without the user ever accessing the device.

This application starts on boot and listens to WiFi-Direct to be paired with a RX1 or RX2.
This app is needed for the CB1 to be setup headless (without a screen and without an input device).

## Process

When the CB1 WiFi Connect is launched, it starts advertising a WiFi-Direct Connection.
It therefore searches for other devices using WiFi-Direct.

As soon as it registers an RX1 or an RX2 on WiFi-Direct, it will setup a connection to that device.
The connection has to be confirmed on the device that the CB1 tries to connect to.
This is also the reason that the connection is initiated from the CB1 and not from the other device.
Otherwise an input would be required on the CB1.

As soon as the WiFi-Direct Connection is Setup as a WiFi-Direct Group, CB1 starts a Socket on Port 13133.
When the other device connects to CB1's Socket, it will start listening to messages on said Socket Connection.

CB1 expects two un-encoded messages from the other Socket Member.
The first message must contain the SSID of the Network it should connect to.
The second message must contain the Password of the Network it should connect to.

When the SSID and Password have been received, CB1 will try to setup a connection to that WiFi-Connection.

## Example WiFi-Direct Socket Client

Here's an example on how to setup and send the SSID and Password to the CB1 Socket.
Make sure a Connection with WiFi-Direct is already setup.

```kotlin
Log.d(TAG, "Socket Start Procedure")
var socket = Socket()
socket.bind(null)
socket.connect(InetSocketAddress(info.groupOwnerAddress, port), 500)
Log.d(TAG, "Connected to host")

Log.d(TAG, "Socket isConnected = ${socket.isConnected}")

var output = PrintWriter(socket.getOutputStream())
var input = BufferedReader(InputStreamReader(socket.getInputStream()))

output.println("ssid")
output.flush()

Log.d(TAG, "Flushed SSID")
Thread.sleep(500)
output.println("password")
output.flush()

Log.d(TAG, "Flushed Messages")
```

## Permissions and WiFi Connection

For this app to run, it has to be a system app.
This is due to the fact that the Android API has changed in Android 10.
Therefore WiFi Connections can not be initiated without user confirmation.

To work around this, this app uses shell commands to establish a connection.
The used shell command looks as follows:

```linux
cmd wifi connect-network "SSID" wpa2 "Password"
```

To test if the permissions are right without executing the whole app, pls. use this ADB Command:

```shell
adb shell
cmd wifi connect-network "SSID" wpa2 "Password"
```

### Set the permissions correctly

In some cases we have to set the permissions manually.
This can be due to testing, or other circumstances.
In any case you can do the following to set the permissions:

```shell
adb shell
su
cd /data/user/0/
chmod 751 com.ava.cb1wificonnect
chown system com.ava.cb1wificonnect
chgrp system com.ava.cb1wificonnect
```

Then check the permissions using

```shell
ls -l
```

It should now show as follows:

```shell
rwxr-x--x system system com.ava.cb1wificonnect
```

Congratulations, you just elevated the CB1 WiFi Connect App to a System App!

## Logging & Debugging

This application is rather cumbersome to debug, here are some tips to make things a little easier:

- It is highly recommended to use a CB1 that is connected to your machine using a USB Cable
- Have an active connection using adb and display the Logcat output
- Filter Logcat by Tag using the following command `adb logcat -s CB1_WIFI_ADVERTISER` to see the process play out
- The Tag is defined in `MainActivity.kt`
- Log-Output is quite detailed and leads you through the process defined above

## How to contribute

This application is always reliant on another application to provide the details.
So remember to adjust the other applications when doing changes to this.
Also keep in mind this application is out there released on CB1s, so make sure you keep backward compatibility in mind in Client Apps.

### GIT Basics

* main branch is stable
* use gitflow for new features/releases
* create PR's for changes
* use the notion issue number as the branch name eg. `feature/AVA23042401`

### Feature Branches

* create a new branch from main
* use the following naming convention `feature/AVA23042401`
* create a PR to merge back into main
* a feature branch should be deleted after it has been merged into main
* a feature branch should be deleted if it is no longer needed
* a feature branch should be created for each new feature (eg. added functionality)

### Hotfix Branches

* create a new branch from main
* use the following naming convention `hotfix/AVA23042401`
* create a PR to merge back into main
* a hotfix branch should be deleted after it has been merged into main
* a hotfix branch should be deleted if it is no longer needed
* a hotfix branch should be created for each new hotfix (eg. bug fix to existing functionality)

### Versioning

* use semantic versioning (https://semver.org/)
* use the following naming convention `1.0.0`
* use minor versions for new features (eg. all merged feature branches)
* use patch versions for hotfixes (eg. all merged hotfix branches)
* create a Tag and a Release for each version
* Make sure the Release Versions are documented with *ALL* changes (to be able to maintain backward compatibility)

## How to release this app?

- TBD

## Contributors

A list of all Contributors sorted by Versions

### V1.0.0
- [Michael J. Lopez](https://github.com/Michu44) - Design & Architecture & Coding
