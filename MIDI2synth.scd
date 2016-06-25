

(
NoteOnResponder.removeAll; NoteOffResponder.removeAll;

MIDIClient.init; // intialize the client
13.do({ arg i; MIDIIn.connect(i, MIDIClient.sources.at(i)); });


e = NoteOnResponder({|src, chan, num, val|
	[src,chan,num,val].postln;
	p= Pbind(
		\instrument, \help_mdapiano,
		\midinote, Pseq([num], 1),
		\vel, Pseq([val], 1)
	).play
},


nil, nil, nil, nil );



f = NoteOffResponder({|src, chan, num, val|
	[src,chan,num,val].postln;
	p.stop;
},


nil, nil, nil, nil );
)

Event // use for help on events (pitch, legato, dur, amp, etc.)