Server.default = s = Server.internal
s.boot

// // Port Miron
// b = Buffer.read(s, "/Users/deniskolokol/Music/port miron/recordings/set1.aif", startFrame: 25*60*44100, numFrames:10*60*44100)
(
// convert and playback
SynthDef("sr_aaf_cut", {arg bufnum;
    var w, x, y, z, r, s, t, u, v;
    var pb, zout;
    pb= PlayBuf.ar(8, bufnum, doneAction:2);
    // Specify an original setup here.
    #w, x, y, z, r, s, t, u, v = FMHEncode1.ar(
        pb, [-0.25pi, -0.6pi, 0.6pi, 0.25pi], [0,0,0,0], 1
    ).sum;
    // #w, x, y, z, r, s, t, u, v = FMHEncode1.ar(
    //     pb, [-0.20pi, -0.5pi, -0.70pi, -pi, 0.70pi, 0.5pi, 0.20pi, 0pi], [0,0,0,0,0,0,0,0], 1
    // ).sum;
    zout= FMHDecode1.stereo(w, y, mul:2);
    Out.ar(0, zout);
}).send(s);

// this will record to the disk
SynthDef("help-Diskout", {arg bufnum;
    DiskOut.ar(bufnum, In.ar(0,2));
}).send(s);
)

// d = "/Users/deniskolokol/Music/Sympli Romatiko/Audio.art_2012/recording/cuts_for_video/"; // directory
// f = "sr_aaf_cut1.aif"; // filename: 7'04'' - 8'06''
// f = "sr_aaf_cut2.aif"; // filename: 10'12'' - 10'27''
// f = "sr_aaf_cut3.aif"; // filename: 16'09'' - 17'02''
// f = "sr_aaf_cut4.aif"; // filename: 17'18'' - 18'15''
// f = "sr_aaf_cut5.aif"; // filename: 18'15'' - 18'30''
// f = "sr_aaf_cut6.aif"; // filename: 19'55'' - 20'30'' for fade out
// b = Buffer.read(s, d ++ "8ch/" ++ f, bufnum:10);

d = "/Users/deniskolokol/Music/the_volume/rehearsals/20131106_Josefinska/";

f = "proba1.aif";

b = Buffer.read(s, d ++ f, bufnum:10);

// allocate a disk i/o buffer
s.sendMsg("/b_alloc", 0, 65536, 2); // Buffer number is 0

// create an output file for this buffer, leave it open
s.sendMsg("/b_write", 0, (d++"stereo/"++f).standardizePath, "aiff", "int16", 0, 0, 1);

(
s.sendMsg("/s_new", "sr_aaf_cut", 2003, 1, 1, "bufnum", b.bufnum); // play converted sound back
s.sendMsg("/s_new", "help-Diskout", 2004, 3, 2003, "bufnum", 0); // create the diskout node
)

s.sendMsg("/n_free", 2004); // stop recording
s.sendMsg("/n_free", 2003); // stop playback

s.sendMsg("/b_close", 0); // close the file.
s.sendMsg("/b_free", 0); // free recorded buffer
s.sendMsg("/b_free", 10); // free buffer to convert