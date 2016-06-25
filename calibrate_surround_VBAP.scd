Server.default = s = Server.internal;

// 1
// 2D - GUI example

a = VBAPSpeakerArray.new(2, [-45, -135, 135, 45]); // 4 channel ring: WARNING! this setup is the best for UI 2d-slid
//a= VBAPSpeakerArray.new(2, [-45, -90, -135, 180, 135, 90, 45, 0]);
a= VBAPSpeakerArray.new(2, [-45, 0, 45, 90, 135, 180, -135, -90]);
// a= VBAPSpeakerArray.new(2, [-45, 0, 45, 90]);
a.speakers[1].dump;


{PinkNoise.ar([0.2, 0.2, 0.2, 0.2], mul:[0.15, 0.15, 0.15, 0.15])}.play

b = a.loadToBuffer;

(
var pos, sl;
var rout, perl;

x = { arg azimuth= 0, elevation= 0, spread= 0, amp=0.2;
    VBAP.ar(4, PinkNoise.ar(0.2, mul:MouseY.kr(0.2, 0)), b.bufnum, SinOsc.ar(0.1, 0, 180), elevation, spread);
}.scope;

w = Window.new("VBAP ctrl",Rect(300, 500, 400, 400)).front;
sl= Slider2D(w, Rect(5, 5, 390, 390))
	.x_(1).y_(1)
	.action_{ |v|
		pos= Complex(v.x.linlin(0,1,1,-1), v.y.linlin(0,1,1,-1));
		x.set(
			\azimuth, pos.theta.linlin(-pi,pi, -180, 180),
//			\spread, pos.magnitude.linlin(0, 1.41, 100, 0), // linear shift
			\spread, ((1-((pos.magnitude.round(0.01)/1.41).pow(0.33)))*100) // exp shift: obj moving towards the center spreads slower
//			\amp, (1-((pos.magnitude.round(0.01)/1.41).sqrt))
		);
	};

//rout= Routine({
//	var xi, yi;
//	inf.do({
//		xi= sl.x-0.01;
//		yi= sl.y-0.01;
//		sl.activex_(xi).activey_(yi);
//		if( (sl.x <= 0.01) && (sl.y <= 0.01), { rout.stop });
//		0.01.wait;
//	});
//});

AppClock.sched(0.0, rout);

CmdPeriod.doOnce({ w.close; });
)

x.free; b.free;
