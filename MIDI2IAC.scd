// WARNING: OS X only!
// sending MIDI info to IAC driver
// used in the piece GP/1 (see https://mathka.bandcamp.com/album/sympli-romatik)

// TODO:
// - Add more channels to MIDIsetup:IAC for sending several streams simultaneously
// - Figure out how to send CC messages


MIDIClient.init;
m = MIDIOut(0); // check number first (1 for saffire)!!!!

//test line
a = Pbind(\degree, Prand([1, 2, 3, [0, 2]], inf), \bend, Pwhite(0, 26, inf), \amp, 1);

//---Cubik Vader
(
~tone_shift1= -12;
~dur1= 5;

TempoClock.default.tempo= 45/60;
~phrases= (
	line1: Pbind(
		\midinote, Pshuf([\, 65, Pxrand(#[63, 65, 67, 68, 56], inf)] + ~tone_shift1, inf),
		\dur, Pshuf([0.125, Pxrand(#[1, 1.5, 2], inf), 0.25 ] * ~dur1, inf),
		\legato, Pwhite( 0.6, 1, inf),
		\amp, Pfunc({  1 })
	).trace,
	line2: Pbind(
		\degree, Pseq([\, \, \] + ~tone_shift1, 1),
		\dur, 0.25
	).trace
);
p= Psym(Pxrand(#[line1, line2], inf), ~phrases).trace
)

//---Artic Ghost - main
(
~tone_shift1= -12;
~dur1= 5;

TempoClock.default.tempo= 45/60;
~phrases= (
	line1: Pbind(
		\midinote, Pshuf([\, 111, Pxrand(#[ 111, 113, 115, 116, 104 ], inf)] + ~tone_shift1, inf),
		\dur, Pshuf([0.125, Pxrand(#[1, 1.5, 2], inf), 0.25 ] * ~dur1, inf),
		\legato, Pwhite( 0.6, 1, inf),
		\amp, Pfunc({  1 })
	).trace,
	line2: Pbind(
		\degree, Pseq([\, \, \] + ~tone_shift1, 1),
		\dur, 0.25
	).trace
);
p= Psym(Pxrand(#[line1, line2], inf), ~phrases).trace
)

//--Beavis mid
(
~tone_shift1= -12;
~dur1= 10;
TempoClock.default.tempo= 45/60;
~phrases= (
	line1: Pbind(
		\midinote, Pshuf([\, 97, Pxrand(#[94, 96, 97, 101], inf)] + ~tone_shift1, inf),
		\dur, Pshuf([0.125, Prand(#[1, 1.5, 2], inf), 0.25 ] * ~dur1, inf),
		\legato, Pwhite( 0.6, 1, inf),
		\amp, Pfunc({  1 })
	).trace,
	line2: Pbind(
		\degree, Pseq([\, \, \] + ~tone_shift1, 1),
		\dur, 0.25
	).trace
);
p= Psym(Pxrand(#[line1, line2], inf), ~phrases).trace
)

//--Beavis hi
(
~tone_shift1= -12;
~dur1= 1;

TempoClock.default.tempo= 120/60;
~phrases= (
	line1: Pbind(
		\midinote, Pwhite(20, 86, inf),
		\dur, Pshuf([0.125, Pxrand(#[0.5, 1, 0.75], inf), 0.25 ] * ~dur1, inf),
		\legato, Pwhite( 0.6, 1, inf),
		\amp, Pshuf([0.75, 0.5, 1, 0.75, 0.25 ], inf)
	).trace,
	line2: Pbind(
		\degree, Pseq([\, 104, 104, \, 102, 98] + ~tone_shift1, inf),
		\dur, 0.25
	).trace
);
p= Psym(Pxrand(#[line1, line2], inf), ~phrases).trace
)

(p <> (type: \midi, midiout: m)).play;
thisProcess.stop;