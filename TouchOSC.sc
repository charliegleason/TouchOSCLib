TouchOSCControl {
	var <type, <prefix, <suffix, <dim, <server, <addr;
	var <sizeX, <sizeY;
	var <path, defs, <bus;
	var <active = false;

	*new { |type, prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs(type, prefix, suffix, dim, server, addr).init
	}

	init {
		var ip, port, paths;

		ip = try { if(addr.ip == "0.0.0.0") { nil } { addr } } { nil };
		port = try { addr.port } { 9000 };
		path = this.buildPath(type, prefix, suffix);
		server = server ? Server.default;
		dim = dim ? 1;

		case { dim.size > 1 } {
			bus = Bus.control(server, dim.product);
			defs = Array.fillND(dim[1..]) { |...args|
				var offset, coordPath = path;
				do(args) { |a| coordPath = coordPath ++ '/' ++ (a + 1).asSymbol; };
				offset = (dim[1] * args[0] + args[1]) * dim[0];
				OSCdef(coordPath, { |msg| bus.setAt(offset, *msg[1..dim[0]]) }, coordPath, ip, port)
			};
			defs.flatten;

		} { dim.size == 1 } {
			bus = Bus.control(server, dim[0]);
			defs = Array[OSCdef(path, { |msg| bus.set(*msg[1..dim[0]]) }, path, ip, port)];

		} { dim.size == 0 } {
			dim.tryPerform(\isEmpty) !? { "Dimensions cannot be empty".throw; };
			bus = Bus.control(server, dim);
			defs = Array[OSCdef(path, { |msg| bus.set(*msg[1..dim]) }, path, ip, port)];
		};

		active = true;
	}

	ar { |numChannels, offset=0| ^bus.ar(numChannels, offset) }

	kr { |numChannels, offset=0| ^bus.kr(numChannels, offset) }

	set { |value|
		//TODO: set value and send message to TouchOSC to update displayed value
	}

	free {
		bus.free;
		while { not(defs.isEmpty) } {
			defs.pop.free;
		};
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

TouchOSCPush : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, server=nil, addr=nil|
		^super.newCopyArgs('push', prefix, suffix, 1, server, addr).init
	}
}

TouchOSCToggle : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, server=nil, addr=nil|
		^super.newCopyArgs('toggle', prefix, suffix, 1, server, addr).init
	}
}

TouchOSCFader : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, server=nil, addr=nil|
		^super.newCopyArgs('fader', prefix, suffix, 1, server, addr).init
	}
}

TouchOSCXY : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, server=nil, addr=nil|
		^super.newCopyArgs('xy', prefix, suffix, 2, server, addr).init
	}
}

TouchOSCRotary : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, server=nil, addr=nil|
		^super.newCopyArgs('rotary', prefix, suffix, 1, server, addr).init
	}
}

TouchOSCEncoder : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, server=nil, addr=nil|
		^super.newCopyArgs('encoder', prefix, suffix, 1, server, addr).init
	}
}

TouchOSCAccXYZ : TouchOSCControl {
	*new { |server=nil, addr=nil|
		^super.newCopyArgs('accxyz', nil, nil, 3, server, addr).init
	}
}

// TODO: TouchOSCLED
// TODO: TouchOSCLabel
// TODO: TouchOSCBattery
// TODO: TouchOSCTime
// TODO: TouchOSCTouch

TouchOSCMultiPush : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs('multipush', prefix, suffix, [1] ++ dim[0] ++ dim[1], server, addr).init
	}
}

TouchOSCMultiToggle : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs('multitoggle', prefix, suffix, [1] ++ dim[0] ++ dim[1], server, addr).init
	}
}

TouchOSCMultiXY : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs('multixy', prefix, suffix, [2] ++ dim[0], server, addr).init
	}
}

TouchOSCMultiFader : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs('multifader', prefix, suffix, [1] ++ dim[0], server, addr).init
	}
}

TouchOSCPage[] {
	var <server, <>addr;
	var <controls;

	// addr: a NetAddr indicating the receive ip and port
	// server: a Server to allocate control buses on
	*new { |server=nil, addr=nil|
		^super.newCopyArgs(server, addr).init
	}

	init {
		server = server ? Server.default;
		addr = addr ? NetAddr(nil, 9000);
		controls = IdentityDictionary[];
	}

	add { |control|
		controls[control.type] = controls[control.type] ? List[];
		controls[control.type].add(control);
	}

	at { |key| ^controls[key] }

	free {
		controls.do(_.free);
		controls.clear;
	}

	doesNotUnderstand { |selector ...args|
		try {
			^controls[selector]
		} { |e|
			e.throw;
		}
	}
}

TouchOSCLayout[] {
	var <server, <>addr, <pageSchema, <controlSchema;
	var <pages;
	classvar types;

	*initClass {
		types = Dictionary[
			"push" -> TouchOSCPush,
			"toggle" -> TouchOSCToggle,
			"faderh" -> TouchOSCFader,
			"faderv" -> TouchOSCFader,
			"rotaryv" -> TouchOSCRotary,
			"rotaryh" -> TouchOSCRotary,
			"encoder" -> TouchOSCEncoder,
			"xy" -> TouchOSCXY,
			"multipush" -> nil,
			"multitoggle" -> nil,
			"multixy" -> nil,
			"multifaderv" -> nil,
			"multifaderh" -> nil,
			"led" -> nil,
			"labelv" -> nil,
			"labelh" -> nil,
			"batteryv" -> nil,
			"batteryh" -> nil,
			"timev" -> nil,
			"timeh" -> nil
		];
	}

	// addr: a NetAddr indicating the receive ip and port
	// server: a Server to allocate control buses on
	*new { |server=nil, addr=nil, pageSchema=nil, controlSchema=nil|
		^super.newCopyArgs(server, addr, pageSchema, controlSchema).init
	}

	*simple { |server=nil, addr=nil|
		var instance = this.new(server, addr);
		var add4Toggles = { |pIndex|
			do(4) { |i|
				instance.pages[pIndex].add(TouchOSCToggle(pIndex + 1, i + 1, server, addr));
			};
		};
		// Add pages
		do(4) { |i| instance.add(TouchOSCPage(server, addr)); };
		// Page 1
		do(5) { |i| instance.pages[0].add(TouchOSCFader(1, i + 1, server, addr)); };
		add4Toggles.(0);
		// Page 2
		do(16) { |i| instance.pages[1].add(TouchOSCPush(2, i + 1, server, addr)); };
		add4Toggles.(1);
		// Page 3
		instance.pages[2].add(TouchOSCXY(3, nil, server, addr));
		add4Toggles.(2);
		// Page 4
		// TODO: multitoggle
		instance.pages[3].add(TouchOSCMultiToggle(4, nil, [8, 8], server, addr));
		add4Toggles.(3);

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

	*load { |fileName, server=nil, addr=nil, pageSchema=nil, controlSchema=nil|
		var instance = this.new(server, addr, pageSchema, controlSchema);
		instance.buildLayout(instance.getRoot(instance.getText(fileName)));
		^instance
	}

	init {
		server = server ?? { Server.default };
		addr = addr ?? { NetAddr(nil, 9000) };
		pages = List[];
		pageSchema = pageSchema ?? { _ + 1 };
		controlSchema = controlSchema ?? { _ + 1 };
	}

	add { |page| pages.add(page); }

	free {
		pages.do(_.free);
		pages.clear;
	}

	at { |index| ^pages[index] }

	// Private methods

	getText { |fileName|
		var file, line, text;

		text = String.newClear;

		if(fileName.endsWith(".touchosc") and: thisProcess.platform.isKindOf(UnixPlatform)) {
			// Assume gzipped XML
			file = Pipe("cat " ++ fileName ++ " | gunzip", "r");
		} {
			// Assume plain XML
			file = File(fileName, "r");
		};

		line = file.getLine;
		while { line.notNil } {
			text = text ++ line;
			line = file.getLine;
		};
		file.close;
		^text
	}

	getRoot { |text| ^DOMDocument.new.parseXML(text).getDocumentElement }

	buildLayout { |root|
		var tabpages = root.getElementsByTagName("tabpage");
		if(not(pages.isEmpty)) { this.free; };
		// Build the layout
		do(tabpages) { |tabpage, i|
			this.add(this.buildPage(tabpage, pageSchema.(i)));
		};
	}

	buildPage { |tabpage, pageID|
		var page, counts, control, controls, type, class, controlID;

		page = TouchOSCPage(server, addr);
		controls = tabpage.getElementsByTagName("control");
		counts = types.values.collect(_ -> 0).asDict;

		do(controls) { |control|
			type = control.getAttribute("type");

			if(types.keys includes: type) {
				class = types[type];
				controlID = controlSchema.(counts[class]);
				control = class.new(controlID, pageID, server, addr);
				page.add(control);
				counts[class] = counts[class] + 1;
			} {
				("Unrecognized control type: " ++ type).warn;
			};
		};
		^page
	}
}

// TODO: free buses when no longer in use or on CmdPeriod
// TODO: GUI
TouchOSC {
	var <server, <addr, enableAccXYZ, layoutName, <pageSchema, <controlSchema, <accxyz, <layout;

	// addr: a NetAddr indicating the receive ip and port
	// server: a Server to allocate control buses on
	*new { |server=nil, addr=nil, enableAccXYZ=true, layoutName=nil, pageSchema=nil, controlSchema=nil|
		^super.newCopyArgs(server, addr, enableAccXYZ, layoutName, pageSchema, controlSchema).init
	}

	init {
		server = server ?? { Server.default };
		addr = addr ?? { NetAddr(nil, 9000) };
		accxyz = if(enableAccXYZ) { TouchOSCAccXYZ.new };
		layout = layoutName !? {
			TouchOSCLayout.tryPerform(layoutName.asSymbol, server, addr)
		} ?? {
			TouchOSCLayout.load(layoutName, server, addr, pageSchema, controlSchema)
		};
	}

	layout_ { |newLayout|
		layout !? { layout.free; };
		layout = newLayout;
	}

	at { |index| ^layout.pages[index] }
}
