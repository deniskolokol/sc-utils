//behavior
Document.autoCompAll;
AutoCompMethodBrowser.exclude([\if, \do, \while, \loop, \collect, \select, \reject, \detect, \add, \put, \at]);

// automatic syntax colorization
Document.globalKeyDownAction_({arg doc, char, mod, unicode, keycode;
	var cursor, open, closed;
	var sstart=doc.selectionStart, ssize=doc.selectionSize;
	(doc.name.find("htm").isNil
	and: {doc.name.find("rtf").isNil}
	and: {doc.name.find("rtfd").isNil}
	and: {doc.isListener == false}).if({
		(unicode==13 or: {unicode==32 or: {unicode==3 or: {unicode==46}}}).if({
			doc.syntaxColorize;
		});
	});
});

// outlook
Document.themes.put(\myTheme, (
	classColor: Color.blue(0.9),
	textColor: Color.black,
	stringColor: Color.magenta(0.5),
	commentColor: Color.red(0.7),
	symbolColor: Color.green(0.4),
	numberColor: Color.blue(0.9)
	)
);
Document.setTheme(\myTheme);
CocoaDocument.defaultFont = Font("Monaco", 11);

Document.listener.font_(Font("Courier New", 13));
Document.listener.background = Color(0, 0, 0, 0.87);
Document.listener.stringColor = Color(1,1,1);
Document.postColor_(Color(1,1,1););
Document.listener.bounds_(Rect(0, 700, Window.screenBounds.width - Window.screenBounds.height, 550));

// change the standard synthDef directory to a custom one:
SynthDef.synthDefDir = "~/Music/SuperCollider/synthdefs".standardizePath;

// change the standard archive path to a custom one:
Archive.archiveDir = "~/Music/SuperCollider/scwork".standardizePath;

Server.default = s = Server.internal;
s.options.numInputBusChannels = 8;
s.options.numOutputBusChannels = 8;
s.options.memSize = 262144;
s.options.blockSize = 512;
s.boot;

s.waitForBoot{
	s.meter;
	this.executeFile(Archive.archiveDir ++ "/mysynths.rtf");
	XiiQuarks.new;

	MasterEQ(8);
	MasterEQ.window.bounds_(Rect(0, 127, 345, 270));
	LilyConfig.initClass
};
