# VICINITY OpenHAB Adapter

## About OpenHAB
OpenHAB is a widely known, open-source solution for IoT Gateways, specifically targeting the home automation domain. However the concept behind is not strictly limited to this domain, as it is build upon the modular Open Services Gateway Initiative (OSGi) Architecture and already supports a broad variety of protocols available on the market.

The openHAB project follows the OSGi specification and consists of OSGi bundles. The OSGi architecture describes a modular Java-based system where modules can be started, stopped, remotely installed and updated during system runtime. One main concept of openHAB is the distinction between _Things_ and _Items_. _Things_ are the internal representation of physical Objects and Devices, integrated into openHAB. Each _Things_ comes with a variety of channels, mapped to specific properties of the devices. E.g. one channel of a Lightbulb would represent the brightness of this Lightbulb, another channel could then represent the color of it.

However, a user never directly interacts with these _Things_ and _Channels_ directly. Instead, the concept of _Items_ is used. _Items_ can be linked to specific channels (and more than one _Item_ can be linked to the same channel) and offer ways to interact with the physical objects in a unified way. E.g. one _Item_ could be of a Switch-Type, allowing the user to turn this switch on and off. The resulting on/off-command is then sent to the corresponding channel and hence to the corresponding item and is on the way translated into a Thing-specific format. Thus _Items_ abstract the functionality from the actual interface to the devices. The user only needs to interact with _Items_ to control certain functionalities and properties, without knowing the actual technical details involved in communication between them.


## Adopting the OpenHAB concepts and mapping to VICINITY
The abstraction and the concept of _Items_ maps onto the VICINITY infrastructure quite nicely. The VICINITY Adapter is implemented as an openHAB Binding (OSGI-Bundle). At the time of writing, only exporting Items and exposing them to VICINITY is implemented. In future releases of this Adapter, the opposite way (e.g. showing and controlling shared VICINITY objects inside openHAB) is also possible. 

In its current implementation, this adapter interprets every _Item_ as potential VICINITY object property or action. As _Items_ can be grouped in openHAB, this adapter relies on Item groups and Item Tags to identify which object should be exposed and to which VICINITY object it belongs. See _Item Configuration_ for more details.


## Adapter Deployment in Development Environment
The VICINITY adapter for openHAB is implemented as an ordinary openHAB Binding. It can be used just as any other Binding.
Follow the Instructions provided by openHAB, set up your IDE and openHAB installation accordingly: [openHAB IDE Setup](https://www.openhab.org/docs/developer/development/ide.html)

After setting up your IDE this repository is supposed to be cloned as
```
<your IDE root>/openhab2-addons/addons/binding/org.openhab.binding.vicinityadapter
```
From there it can be importe as existing project into your IDE. Finally, update the runtime configuration to build and include this VICINITY adapter as well.


## Adapter Deployment on Standalone Device (e.g. RaspberryPi)
The VICINITY adapter for openHAB is implemented as an ordinary openHAB Binding. It can be used just as any other Binding.
Follow the Instructions provided by openHAB to set up your openHAB Device, depending on your Hardware Platform: [openHAB Installation](https://www.openhab.org/docs/installation/#platform-recommendations)

Afterwards, the binding .jar should be placed into openHAB's addon folder. E.g. for Debian Package-based Installation at:
```
/usr/share/openhab2/addons/
e.g.
/usr/share/openhab2/addons/org.openhab.binding.vicinityadapter-2.4.0-SNAPSHOT.jar
```

OpenHAB should dynamically load new addons. Otherwise, the binding needs to be enabled manually, via command-line:
```
# openhab-cli console
(log in as openhab:habopen)

openhab> bundle:list
(find the ID of the VICINITY Adapter, e.g.)

180 │ Active   │  80 │ 2.4.0.201809051336     │ VICINITYadapter Binding
(in case the bundle is not yet loaded and active, start the bundle)

openhab> bundle:start <id of VICINITY adapter. 180 in the above example>
```


## Item configuration
In order for this adapter to find all _Items_ you intend to expose to the VICINITY, Item Tags need to be added. Additionally the Adapter needs to know, how to group the selected _Items_ together and form VICINTY objects. In order to do so, all _Items_ that belong to the same VICINITY object need to be grouped together:

```
Dimmer  Bulb1_Dimmer    "Helligkeit"  (vcntBulb1)  ["vcntpropBrightness"] {channel = xxx}
Switch  Bulb1_OnOff    "Farbe"  (vcntBulb1)  ["vcntpropColor"] {channel = xxx}
```

The above example will expose on VICINITY Object called __Bulb1__ with two properties __Brightness__ and __Color__. At this moment, all Properties are exported with Read and Write Links. Object name and Object ID (oid) are called __Bulb1__. For each property, the pid is called according to the single item names.


## Connecting openHAB into VICINITY
Right now, each tagged and group item will be exposed according to the VICINITY Thing Description (TD) Format. Once started, the VICINITY Adapter (not included in this Binding) can be started and can read the TD from
> http://localhost:8080/rest/objects

Further improvements need to be implemented. User config parameters will be added to control read/write to properties, control the object type and human readable object names.

Stay tuned!
