(
w= SCWindow("", Rect(140, 100, 200, 100)).front;       

SCButton(w, Rect(50, 30, 100, 25)) // Record button
	.states_([
		["Record", Color.black, Color.new(0.8,0.8,0.8)],
		["Recording", Color.red, Color.new(0.8,0.8,0.8)]
		])
	.action_({ |bt|
		if (bt.value == 1, {
			CocoaDialog.savePanel({ arg path; var recordPath;
				s.recChannels= 8;
				s.recHeaderFormat= "AIFF";
				s.recSampleFormat= "int24";
				recordPath= path ++ ".aif";
				AppClock.sched(0, {fork{s.prepareForRecord(recordPath); s.sync; s.record}})
				},
			{"Cancelled".postln})
			},
		{
			s.stopRecording
			})
        });
w.front;
) 