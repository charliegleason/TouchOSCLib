# TouchOSCLib

TouchOSCLib is a SuperCollider Quark which aims to provide a dead simple way to control
SuperCollider via OSC using [TouchOSC](https://hexler.net/products/touchosc).

## Status

This library is currently in very early stages of development. At the time of writing, only the
"Simple" interface has been implemented in `TouchOSCLayout` and multi-controls (such as
multisliders, multi-touch XY pad, etc.) have yet to be implemented.

# Usage

Example:
```supercollider
// Create an instance which allocates control buses on the default server, listens on port 9000 of
// all interfaces, listens for /accxyz messages, and listens for messages corresponding to the
// "Simple" layout
~touchOSC = TouchOSC(layoutName: 'simple');

// Get the bus associated with fader four on the first page of the layout
~touchOSC[0].fader[4].bus

// Synchronously get the value of the control bus of fader four on the first page of the layout
~touchOSC[0].fader[4].bus.getSynchronous

// Get the OSC path of fader four on the first page of the layout
~touchOSC[0].fader[4].path

// Get the value of fader four on the first page of the layout as a Pattern:
Pfunc { ~touchOSC[0].fader[4].bus.getSynchronous };

// Create a scope window which plots the accelerometer X, Y, and Z components over time
~touchOSC.accxyz.bus.scope
```

## `TouchOSC`
### Class methods

`.new(server=nil, addr=nil, enableAccXYZ=true, layoutName=nil)`: Creates a new instance of
`TouchOSC`.
- `server`: The server to create control buses on. Defaults to `nil`, in which case the default
    server is used (i.e. `Server.default`).
- `addr`: An instance of `NetAddr` indicating the IP address of the and port to listen on. Defaults
    to `nil`, in which case `NetAddr(nil, 9000)` is used. A `nil` IP address (the first argument of
    `NetAddr`) indicates that the instance of `TouchOSC` should listen on all interfaces (`0.0.0.0`).
    By default, the TouchOSC app uses UDP port 9000 to send and receive OSC messages, so the `TouchOSC`
    class matches this.

    There are a couple conditions where specifying an IP address is necessary:
    - You aren't on a private network (such as your home WiFi)
    - You're using multiple devices running TouchOSC, each using the same layout

    In the first case, listening on all interfaces (i.e. accepting any UDP message on the chosen
    port) is a security issue, which is mitigated by listening only for messages from a trusted
    source. In the second case, you'll need to use one instance of `TouchOSC` per instance of the
    app. Specifying the IP address of each app prevents each instance of the `TouchOSC` class from
    listening to all of the apps so its control buses reflect the values of the controls of
    one (and only one) app.
- `enableAccXYZ`: Indicates whether to listen for accelerometer messages (`/accxyz`). If
    `true` (the default), An instance of `TouchOSCAccXYZ` is created and can be accessed
    via the `.accxyz` instance (getter) method. Otherwise, `.accxyz` returns `nil`.

### Instance methods

`.at(index)`: Accesses the `TouchOSCPage` at the given `index`, starting from zero.

Example:
```supercollider
(
~touchOSC = TouchOSC(layoutName: 'simple');
~touchOSC.at(0).postln;
~touchOSC[0].postln;
)
```
Post window output:
```
-> a TouchOSCPage
a TouchOSCPage
-> a TouchOSCPage
a TouchOSCPage
```

`.server`: The server the instance has allocated control buses on.

The rest of the classes (not yet documented):

- `TouchOSCPage`
- `TouchOSCLayout`
- `TouchOSCControl`
- `TouchOSCPushButton`
- `TouchOSCToggleButton`
- `TouchOSCFader`
- `TouchOSCXYPad`
- `TouchOSCAccXYZ`
