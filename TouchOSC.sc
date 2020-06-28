TouchOSCControl {
	var <type, <prefix, <suffix, <dim, <server, <addr;
	var <path, defs, <bus, <elementDim, <sizeX, <sizeY;
	var <active = false;

	*new { |type, prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs(type, prefix, suffix, dim, server, addr).init
	}

	init {
		var ip, port;
		addr = addr ? NetAddr("0.0.0.0", nil);
		//ip = try { if(addr.ip == "0.0.0.0") { nil } { NetAddr(addr.ip) } } { nil };
		ip = try { if(addr.ip != "0.0.0.0") { NetAddr(addr.ip) } };
		//port = try { if(addr.port.isNil) { 9000 } { addr.port } } { 9000 };
		port = try { addr.port } ? 9000;
		path = this.buildPath(type, prefix, suffix);
		server = server ? Server.default;
		dim = dim ? 1;
		# elementDim, sizeX, sizeY = try { dim[..2] } { dim.asArray };
		bus = Bus.control(server, dim.asArray.product);
		defs = case { dim.size >= 2 } { // is a collection specifying the dimensions of a multi-control
			// In this case:
			// - elementDim (dim[0]) is the number of dimensions each element in a multi-control has
			// - sizeX (dim[1]) is the number of elements the multi-control has (or the horizontal/x
			// dimension if dim[2] is also specified).
			// - sizeY (dim[2]) is the number of elements the multi-control has in the vertical/y dimension
			Array.fillND(dim[1..]) { |...args|
				var i, j, offset, coordPath = path;
				# i, j = args[..1];
				do(args) { |index|
					coordPath = coordPath ++ '/' ++ (index + 1).asSymbol;
				};
				offset = if(sizeY.notNil) {
					((sizeY * j) + i) * elementDim
				} {
					i * elementDim
				};
				OSCdef(coordPath, { |msg| bus.setAt(offset, *msg[1..elementDim]) }, coordPath, ip, port)
			}.flatten
		} { dim.size >= 0 } { // is either a number or an ordered collection of size 0 or 1
			// Test whether dim is an empty collection. Numbers don't understand .isEmpty
			dim.tryPerform(\isEmpty) !? { "Dimensions cannot be empty".throw; };
			Array[OSCdef(path, { |msg| bus.set(*msg[1..dim.asArray[0]]) }, path, ip, port)]
		} { true } { // else
			"Dimensions must be an integer or an ordered collection of integers".throw;
		};
		active = true;
		this.reset;
	}

	ar { |numChannels, offset=0| ^bus.ar(numChannels, offset) }

	kr { |numChannels, offset=0| ^bus.kr(numChannels, offset) }

	get {
		// TODO: return an array that reflects the physical layout
		^if(server.hasShmInterface) { bus.getnSynchronous } { bus.getn }
	}

	set { |...args|
		// TODO: set by index/indices
		if(server.hasShmInterface) { bus.setnSynchronous(args); } { bus.setn(args); };
		do(defs.collect(_.path)) { |path, i| addr.sendMsg(path, args[i]); };
	}

	reset { this.set(*(0 ! dim.asArray.product)); }

	free {
		bus.free;
		while { defs.isEmpty.not } { defs.pop.free; };
		active = false;
	}

	scope { ^bus.scope }

	mapTo { |obj ...args|
		// TODO: map bus(es) to Synth, NodeProxy, etc. controls
		// Could use a list of Symbols?
	}

	// Private methods

	buildPath { |name, prefix=nil, suffix=nil|
		var path = '/' ++ name.asSymbol;
		prefix !? { path = prefix.asSymbol ++ path; };
		suffix !? { path = path ++ suffix.asSymbol; };
		if(path.asString.beginsWith("/").not) { path = '/' ++ path; };
		^path
	}

	getDef { |i=nil, j=nil|
		// TODO: index defs depending on whether sizeX and sizeY are set
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

TouchOSCMultiXY : TouchOSCControl {
	// NOTE: this does not have a dim argument, unlike all the other multi-controls
	*new { |prefix=nil, suffix=nil, server=nil, addr=nil|
		// Assumes 1 person is using a given MultiXY and has at most 10 fingers
		^super.newCopyArgs('multixy', prefix, suffix, [2, 10], server, addr).init
	}
}

// TODO: TouchOSCLED
// TODO: TouchOSCLabel
// TODO: TouchOSCBattery
// TODO: TouchOSCTime
// TODO: TouchOSCTouch

TouchOSCMultiPush : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs('multipush', prefix, suffix, [1] ++ dim[..1], server, addr).init
	}
}

TouchOSCMultiToggle : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs('multitoggle', prefix, suffix, [1] ++ dim[..1], server, addr).init
	}
}

TouchOSCMultiFader : TouchOSCControl {
	*new { |prefix=nil, suffix=nil, dim=nil, server=nil, addr=nil|
		^super.newCopyArgs('multifader', prefix, suffix, [1] ++ dim.asArray[0], server, addr).init
	}
}

// TODO: make TouchOSCPage inherit from TouchOSCControl to capture page changes?
// Alternatively, use multiple OSCdefs in TouchOSCLayout to set the value of a single bus
// to the index of the current page
TouchOSCPage[] {
	var <server, <>addr;
	var <controls;

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

// TODO: ability to automatically generate layouts?
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
			"multipush" -> TouchOSCMultiPush,
			"multitoggle" -> TouchOSCMultiToggle,
			"multixy" -> TouchOSCMultiXY,
			"multifaderv" -> TouchOSCMultiFader,
			"multifaderh" -> TouchOSCMultiFader,
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
		server = server ? Server.default;
		addr = addr ? NetAddr(nil, 9000);
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

	getRoot { |text| ^DOMDocument().parseXML(text).getDocumentElement }

	buildLayout { |root|
		var tabpages = root.getElementsByTagName("tabpage");
		if(pages.isEmpty.not) { this.free; };
		// Build the layout
		do(tabpages) { |tabpage, i|
			this.add(this.buildPage(tabpage, pageSchema.(i)));
		};
	}

	buildPage { |tabpage, pageID|
		var page, counts, controls;

		page = TouchOSCPage(server, addr);
		controls = tabpage.getElementsByTagName("control");
		counts = types.values.collect(_ -> 0).asDict;

		do(controls) { |control|
			var type = control.getAttribute("type");

			if(types.keys includes: type) {
				var class, instance, controlID;
				class = types[type];
				controlID = controlSchema.(counts[class]);

				instance = case {
					control.hasAttribute("number_x") and: control.hasAttribute("number_y")
				} {
					// is a 2D arrayed control
					var dim = collect(["number_x", "number_y"]) { |attr|
						control.getAttribute(attr).asInteger
					};
					class.new(pageID, controlID, dim, server, addr)
				} { control.hasAttribute("number") } {
					// is a 1D arrayed control
					var dim = [control.getAttribute("number").asInteger];
					class.new(pageID, controlID, dim, server, addr)
				} { true } { // else
					// is not an arrayed control or is a multixy
					class.new(pageID, controlID, server, addr)
				};
				page.add(instance);
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
	var <server, <addr, enableAccXYZ, layoutName, <pageSchema, <controlSchema;
	var <accxyz, <layout;

	// addr: a NetAddr indicating the receive ip and port
	// server: a Server to allocate control buses on
	*new { |server=nil, addr=nil, enableAccXYZ=true, layoutName=nil, pageSchema=nil, controlSchema=nil|
		^super.newCopyArgs(server, addr, enableAccXYZ, layoutName, pageSchema, controlSchema).init
	}

	init {
		server = server ?? { Server.default };
		addr = addr ?? { NetAddr(nil, 9000) };
		accxyz = if(enableAccXYZ) { TouchOSCAccXYZ() };
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
