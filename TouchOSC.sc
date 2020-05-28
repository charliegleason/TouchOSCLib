TouchOSCControl {
	var <type, <num, <page, <dim, <server;
	var <path, def, <bus;
	var <hasBeenFreed = false;

	*new { |type, num=nil, page=1, dim=1, server(Server.default)|
		^super.newCopyArgs(type, num, page, dim, server).init;
	}

	init {
		path = '/' ++ page.asSymbol ++ '/' ++ type.asSymbol ++ num.asSymbol;
		bus = Bus.control(server, dim);
		def = OSCdef(path, { |msg| bus.set(*msg[1..dim]) }, path, addr.port);
	}

	free {
		bus.free;
		def.free;
		hasBeenFreed = true;
	}

	scope { bus.scope; }
}

/*
TouchOSCDiscreteControl {
	*new { ^this.notYetImplemented }
}

TouchOSCButton : TouchOSCControl {
	*new { ^this.notYetImplemented }
}

TouchOSCToggle : TouchOSCButton {
	*new { ^this.notYetImplemented }
}

TouchOSCFader : TouchOSCControl {
	*new { ^this.notYetImplemented }
}

TouchOSCMultiControl {
	*new { ^this.notYetImplemented }
}
*/

TouchOSC {
	var <>addr;
	var <controls;

	// addr: a NetAddr indicating the receive ip and port
	*new { |addr| ^super.newCopyArgs(addr).init }
	init { controls = IdentityDictionary[]; }
	add { |control| controls[control.controlName] = control; }

	// remove { |control|
}
