TouchOSCControl {
	var <type, <num, <page, <dim, <server, <>addr;
	var <path, def, <bus;
	var <hasBeenFreed = false;

	*new { |type, num=nil, page=1, dim=1, server=nil, addr=nil|
		^super.newCopyArgs(type, num, page, dim, server, addr).init
	}

	init {
		var ip, port;

		ip = addr !? { addr.ip };
		port = if(addr.isNil.not) { addr.port } { 9000 };

		server ?? { server = Server.default; };
		~accxyz.scope;
		path = '/' ++ (page ?? { '' }).asSymbol ++ '/' ++ type.asSymbol ++ (num ?? { '' }).asSymbol;
		bus = Bus.control(server, dim);
		def = OSCdef(path, { |msg| bus.set(*msg[1..dim]) }, path, ip, port);
	}

	free {
		bus.free;
		def.free;
		hasBeenFreed = true;
	}

	scope { bus.scope }
}

TouchOSCPushButton : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('push', num, page, 1, server, addr)
	}
}

TouchOSCToggleButton : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('toggle', num, page, 1, server, addr)
	}
}

TouchOSCFader : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('fader', num, page, 1, server, addr)
	}
}

TouchOSCXYPad : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('xy', num, page, 2, server, addr)
	}
}

TouchOSCAccXYZ : TouchOSCControl {
	*new { |server=nil, addr=nil|
		^super.newCopyArgs('accxyz', nil, nil, 3, server, addr)
	}
}

TouchOSC {
	var <>addr, <server;
	var <controls;

	// addr: a NetAddr indicating the receive ip and port
	*new { |addr=nil, server=nil| ^super.newCopyArgs(addr, server).init }

	init {
		server ?? { server = Server.default; };
		addr ?? { addr = NetAddr("0.0.0.0", 9000); };
		controls = IdentityDictionary[];
	}

	add { |control|
		controls[control.type] ?? { controls[control.type] = List[]; };
		controls[control.type].add(control);
	}
}
