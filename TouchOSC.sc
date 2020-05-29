TouchOSCControl {
	var <type, <num, <page, <dim, <server, <addr;
	var <path, def, <bus;
	var <active = false;

	*new { |type, num=nil, page=nil, dim=1, server=nil, addr=nil|
		^super.newCopyArgs(type, num, page, dim, server, addr).init;
	}

	init {
		var ip, port;

		ip = addr !? { NetAddr(addr.ip) };
		port = try { addr.port } { 9000 };

		path = this.buildPath(type, page, num);
		this.buildPath(type, page, num);
		server = server ?? { Server.default };
		bus = Bus.control(server, dim);
		def = OSCdef(path, { |msg| bus.set(*msg[1..dim]) }, path, ip, port);
		active = true;
	}

	ar { |numChannels, offset=0| ^bus.ar(numChannels, offset) }

	kr { |numChannels, offset=0| ^bus.kr(numChannels, offset) }

	free {
		bus.free;
		def.free;
		active = false;
	}

	scope { ^bus.scope }

	// Private methods

	buildPath { |name, prefix=nil, suffix=nil|
		var path = '/' ++ name.asSymbol;
		prefix !? { path = '/' ++ prefix.asSymbol ++ path; };
		suffix !? { path = path ++ suffix.asSymbol; };
		^path
	}
}

TouchOSCPushButton : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('push', num, page, 1, server, addr).init
	}
}

TouchOSCToggleButton : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('toggle', num, page, 1, server, addr).init
	}
}

TouchOSCFader : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('fader', num, page, 1, server, addr).init
	}
}

TouchOSCXYPad : TouchOSCControl {
	*new { |num=nil, page=1, server=nil, addr=nil|
		^super.newCopyArgs('xy', num, page, 2, server, addr).init
	}
}

TouchOSCAccXYZ : TouchOSCControl {
	*new { |server=nil, addr=nil|
		^super.newCopyArgs('accxyz', nil, nil, 3, server, addr).init
	}
}

TouchOSCPage[] {
	var <server, <>addr;
	var controls;

	// addr: a NetAddr indicating the receive ip and port
	// server: a Server to allocate control buses on
	*new { |server=nil, addr=nil|
		^super.newCopyArgs(server, addr).init
	}

	init {
		server = server ?? { Server.default };
		addr = addr ?? { NetAddr(nil, 9000) };
		controls = IdentityDictionary[];
	}

	add { |control|
		controls[control.type] ?? { controls[control.type] = List[] };
		controls[control.type].add(control);
	}

	at { |key| ^controls[key] }

	free { controls do: _.free; }

	doesNotUnderstand { |selector ...args|
		try {
			^controls[selector]
		} { |e|
			e.throw;
		}
	}
}

TouchOSCLayout[] {
	var <server, <>addr;
	var <pages;

	// addr: a NetAddr indicating the receive ip and port
	// server: a Server to allocate control buses on
	*new { |server=nil, addr=nil|
		^super.newCopyArgs(server, addr).init
	}

	*simple { |server=nil, addr=nil|
		var instance = this.new(server, addr);

		do(4) { |i|
			instance.add(TouchOSCPage(server, addr));
			// Add 4 toggle buttons to each page
			do(4) { |j|
				instance.pages[i].add(TouchOSCToggleButton(j + 1, i + 1, server, addr));
			};
		};

		// Page 1
		do(5) { |i| instance.pages[0].add(TouchOSCFader(i + 1, 1, server, addr)); };

		// Page 2
		do(16) { |i| instance.pages[1].add(TouchOSCPushButton(i + 1, 2, server, addr)); };

		// Page 3
		instance.pages[2].add(TouchOSCXYPad(nil, 3, server, addr));

		// Page 4
		// TODO: multitoggle

		^instance
	}

	*beatmachine { |server=nil, addr=nil|
		^this.notYetImplemented
	}

	*mix2 { |server=nil, addr=nil|
		^this.notYetImplemented
	}

	*mix16 { |server=nil, addr=nil|
		^this.notYetImplemented
	}

	init {
		server = server ?? { Server.default };
		addr = addr ?? { NetAddr(nil, 9000) };
		pages = List[];
	}

	add { |page| pages.add(page); }
	free { pages do: _.free; }

	at { |index| ^pages[index] }
}

// TODO: free buses when no longer in use or on CmdPeriod
// TODO: GUI
TouchOSC {
	var <server, <addr, enableAccXYZ, layoutName, <accxyz, <>layout;

	// addr: a NetAddr indicating the receive ip and port
	// server: a Server to allocate control buses on
	*new { |server=nil, addr=nil, enableAccXYZ=true, layoutName=nil|
		^super.newCopyArgs(server, addr, enableAccXYZ, layoutName).init
	}

	init {
		server = server ?? { Server.default };
		addr = addr ?? { NetAddr(nil, 9000) };
		accxyz = if (enableAccXYZ) { TouchOSCAccXYZ.new };
		layout = layoutName !? { TouchOSCLayout.perform(layoutName) };
	}

	at { |index| ^layout.pages[index] }
}
