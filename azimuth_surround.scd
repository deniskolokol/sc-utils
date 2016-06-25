// concert at Crikoteka:

// MEASUREMENTS:
// original:
// [13.7, 7.7, 5.6, 4.9, 7.5, 12.7]
// [60, 37, 115, 243, 310, 342]
//
// scaled:
// [13.7, 7.7, 5.6, 4.9, 7.5, 12.7]
// [-30, -37, -115, 117, 50, 18] / 180
// [ -0.17, -0.2, -0.64, 0.65, 0.28, 0.1 ]

// visualization of the kr bus values
(
var win, view, vSize=500, margin=20, func, run=true;
var posComplex=[1, 0], // array [distance, azimuth]
    posPoint=Point(0, 0), // array [x, y]
    elevation=0;
var azimuthNodeID = 60,
    distanceNodeID = 61,
    elevationNodeID = 62;

var params; // main parameters

// synth defs
// test sound
SynthDef(\calib__e__proba1, {
    arg bufnum=0, inbus=30, outbus=20,
        amp=1, master=1, gate=1, att=1, rel=1, ca=3, cr= -3,
        ax=0.1, ay=0.1, az=0.1;
    Out.ar(
        outbus,
        RLPF.ar(WhiteNoise.ar(amp), 10000, 0.1) * master * EnvGen.kr(
            Env.adsr(att, 0.1, 1, rel, curve:[ca, cr]), gate: gate, doneAction: 2
        )
    );
}).add;

// spatializer
SynthDef(\calib__s__ambisonic4, {
    arg inbus=20, outbus=0, gate=1, rel=1,
        azimuth=0, elevation=0, distance=0, depth=5;
    var w, x, y, z, r, f, t, u, v,
        speakerAzim=[-0.25pi, -0.6pi, 0.6pi, 0.25pi], speakerElev=0, speakerDist=[1, 1, 1, 1],
        maxDist=1, scaleFlag=0,
        in, signal, out;
    distance = distance.linlin(0, 2.sqrt, 0.5, depth);
    in = In.ar(inbus, 1);
    signal = in * EnvGen.kr(Env.cutoff(rel * 2, 1, \sin), gate: gate, doneAction: 2);
    #w, x, y, z, r, f, t, u, v = FMHEncode1.ar(signal, azimuth, elevation, distance);
    out = FMHDecode1.ar1(w, x, y, z, r, f, t, u, v,
        azimuth: speakerAzim, elevation: speakerElev, distance: speakerDist, maxDist:maxDist, scaleflag:scaleFlag);
    Out.ar(outbus, out);
}).send(s);


SynthDef(\calib__s__ambisonic6, {
    arg inbus=20, outbus=0, gate=1, rel=1,
        azimuth=0, elevation=0, distance=0, depth=5;
    var w, x, y, z, r, f, t, u, v,
        speakerAzim=[-0.17pi, -0.2pi, -0.64pi, 0.65pi, 0.28pi, 0.1pi], speakerElev=0, speakerDist=[13.7, 7.7, 5.6, 4.9, 7.5, 12.7],
        maxDist=13.7, scaleFlag=0,
        in, signal, out;
    distance = distance.linlin(0, 2.sqrt, 0.5, depth);
    in = In.ar(inbus, 1);
    signal = in * EnvGen.kr(Env.cutoff(rel * 2, 1, \sin), gate: gate, doneAction: 2);
    #w, x, y, z, r, f, t, u, v = FMHEncode1.ar(signal, azimuth, elevation, distance);
    out = FMHDecode1.ar1(w, x, y, z, r, f, t, u, v,
        azimuth: speakerAzim, elevation: speakerElev, distance: speakerDist, maxDist:maxDist, scaleflag:scaleFlag);
    Out.ar(outbus, out);
}).send(s);



// kr synths
SynthDef(\calib__k__sin, {
    arg freq=440, phase=0, mul=1, add=0, outbus=0, gate=1, att=1, rel=1, trigID=60, trigRate=30;
    var env = EnvGen.kr(Env.adsr(att, 0.1, 1, rel, curve:[3, -3]), gate: gate, doneAction: 2);
    var out = env * SinOsc.kr(freq, phase, mul, add);
    SendTrig.kr(Impulse.kr(30), trigID, out);
    Out.kr(outbus, out);
}).add;

SynthDef(\calib__k__tri, {
    arg freq=440, phase=0, mul=1, add=0, outbus=0, gate=1, att=1, rel=1, trigID=60, trigRate=30;
    var env = EnvGen.kr(Env.adsr(att, 0.1, 1, rel, curve:[3, -3]), gate: gate, doneAction: 2);
    var out = env * LFTri.kr(freq, phase, mul, add);
    SendTrig.kr(Impulse.kr(30), trigID, out);
    Out.kr(outbus, out);
}).add;

SynthDef(\calib__k__cub, {
    arg freq=440, phase=0, mul=1, add=0, outbus=0, gate=1, att=1, rel=1, trigID=60, trigRate=30;
    var env = EnvGen.kr(Env.adsr(att, 0.1, 1, rel, curve:[3, -3]), gate: gate, doneAction: 2);
    var out = env * LFCub.kr(freq, phase, mul, add);
    SendTrig.kr(Impulse.kr(30), trigID, out);
    Out.kr(outbus, out);
}).add;

SynthDef(\calib__k__stepnoise, {
    arg freq=1, phase=0, mul=1, add=0, outbus=0, gate=1, att=1, rel=1, trigID=60, trigRate=30;
    var env = EnvGen.kr(Env.adsr(att, 0.1, 1, rel, curve:[3, -3]), gate: gate, doneAction: 2);
    var out = env * TChoose.kr(
        Dust.ar(1),
        [LFNoise0.kr(freq, mul, add), LFNoise1.kr(freq, mul, add), LFNoise2.kr(freq).clip2 * mul + add]
    );
    SendTrig.kr(Impulse.kr(30), trigID, out);
    Out.kr(outbus, out);
}).add;

// INITIAL
//
params = (
    azimut: (
        shape: [\const, \sin, \saw, \tri, \cub, \stepnoise],
        freq: 0.1,
        mul: 0.5,
        add: 0.8
    ),
    rho: (
        shape: [\const, \sin, \saw, \tri, \cub, \stepnoise],
        freq: 0.1,
        mul: 0.7, // 2.sqrt/2,
        add: 0
    )
    elevation: (
        shape: [\const, \sin, \saw, \tri, \cub, \stepnoise],
        freq: 0.1,
        mul: 1pi,
        add: 0
    ),
    outbus:            0, // main output starts from
    confSurround:      "Ambisonic4",
    fieldDepth:        5, // how "far" sound travels beyond the circle of speakers
    master:            1, // master volume
);

// synths
s.sendBundle(0.01,
    ["/g_new", ~id_calib=s.nextNodeID], // add new group to root's tail
    ["/s_new", \calib__s__ambisonic6, s.nextNodeID, 1, ~id_calib, \distance, 2.sqrt, // add spatializer to new group's tail
        \inbus, 12, // getting signal from synth
        \outbus, 0], // to main output
    ["/s_new", \calib__e__proba1, s.nextNodeID, 0, ~id_calib, // add synth to new group's head
        \outbus, 12, // feeding into spatilizer
        \amp, 0.2],
    ["/s_new", \calib__k__sin, ~id_test1=s.nextNodeID, 0, ~id_calib, // kr synths - to the group's head
        \freq, 0.1, \mul, 1pi, \outbus, 10, \trigID, azimuthNodeID],
    // ["/s_new", \calib__k__stepnoise, ~id_test2=s.nextNodeID, 0, ~id_calib,
    // \freq, 0.5, \mul, 0.557, \add, 0.857, \outbus, 11, \trigID, distanceNodeID],
    // ["/s_new", \calib__k__stepnoise, ~id_test3=s.nextNodeID, 0, ~id_calib,
    // \freq, 0.2, \mul, 0.5pi, \outbus, 12, \trigID, elevationNodeID],
    ["/n_map", ~id_calib, \azimuth, 10],
    //["/n_map", ~id_calib, \distance, 11],
    // ["/n_map", ~id_calib, \elevation, 12]
);

// OSCFunc catching kr values
func = OSCFunc({ arg msg, time;
    case
    { msg[2] == distanceNodeID  } { posComplex[0] = msg[3] } // rho
    { msg[2] == azimuthNodeID   } { posComplex[1] = msg[3] } // azimuth
    { msg[2] == elevationNodeID } { elevation = msg[3] };
    posPoint = Polar(posComplex[0], posComplex[1]).asPoint;
}, '/tr', s.addr); // register to receive this message

// interface
win = Window("kr visualization", Rect(500, 500, vSize + 500, vSize + (margin * 3))).front;
view = UserView(win, Rect(20, 20, vSize + margin, vSize + margin));
view.background_(Color.black);
view.drawFunc = { |view|
    var elev, reportStr;

    // border
    Pen.strokeColor = Color.yellow;
    Pen.strokeRect(
        Rect(margin, margin, vSize - margin, vSize - margin)
    );

    // position pointer
    elev = elevation.linlin(-0.5pi, 0.5pi, 2, 20);

    Pen.fillColor = Color.white;
    Pen.fillOval(
        Rect.aboutPoint(
            Point(
                posPoint.x.linlin(-1, 1, margin, vSize),
                posPoint.y.linlin(-1, 1, vSize, margin)
            ),
            elev, elev
        )
    );

    // report
    reportStr = { |val, label, coord, round=0|
        Pen.stringAtPoint(
            format((label ?? "") ++ " %", val.round(round)), coord.asPoint);
    };
    [ // using array here because it is ordered: [label, value, position X]
        ["rho", posComplex[0], margin],
        ["azimuth", posComplex[1] / 1pi, 90],
        ["elevation", elevation, 200],
        ["x", posPoint.x, 390],
        ["y", posPoint.y, 450]
    ].do { |v, i|
        reportStr.(v[1], v[0], Point(v[2], vSize + 3), 0.001)
    };
};

// animate
{ while { run } { view.refresh; 0.01.wait } }.fork(AppClock);

// exit
CmdPeriod.doOnce({
    s.sendBundle(0.01,
        ["/n_set", ~id_test1, \gate, 0],
        ["/n_set", ~id_test2, \gate, 0],
        ["/n_set", ~id_test3, \gate, 0]
    );
    if (win.isClosed.not) {win.close};
    func.free;
    run = false;
});
)