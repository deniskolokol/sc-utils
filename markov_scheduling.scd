(
var msetWait, msetKey, msetAct;
var playRout, quant= 1;
var w, lbKey, lbSteps, lbWait, liWait, lbAction, lbRecAll; // main window
var warn= 0.6, crit= 0.9;
var receiverNet;

// Markov set for waiting time.
msetWait= MarkovSet([
    // [\s, ({rrand(0.5, 3.0)} ! 10)], // short
    // [\a, ({rrand(2.0, 5.0)} ! 10)], // average
    // [\l, ({rrand(4.0, 20.0)} ! 10)] // long
    [\s, ({rrand(3.0, 5.0)} ! 10)], // short
    [\a, ({rrand(5.0, 40.0)} ! 10)], // average
    [\l, ({rrand(30.0, 120.0)} ! 10)] // long
]);

msetKey= MarkovSet([ // markov set for number of msetWait use
	[\n,
		[ 2, 5, 7, 10, 12], // number of repeats
		[ 0.2, 0.4, 0.3, 0.07, 0.03] // longer sequences are less probable
		]
	]);

w= Window("markov seq", Rect(1800, 900, 170, 340), resizable:false)
	.acceptsMouseOver_(true);
w.view.decorator = FlowLayout(w.view.bounds);

lbKey= StaticText(w,Rect(5, 200, 35, 50))
	.string_("")
	.align_(\center)
	.font_(Font("Helvetica", 28));
lbSteps= StaticText(w,Rect(50, 200, 45, 50))
	.string_("")
	.align_(\center)
	.font_(Font("Helvetica", 28));
lbWait= StaticText(w,Rect( 100, 200, 70, 50 ))
	.string_("")
	.align_(\center)
	.font_(Font("Helvetica", 40));
liWait= LevelIndicator(w, Rect(5, 260, w.bounds.width-10, 30))
	.canFocus_(false) .style_(0) .warning_(warn) .critical_(crit);

Button(w, Rect(5, 5, 160, 30))
.states_([
    ["RUN", Color.black, Color.white],
    ["STOP", Color.white, Color.red]
])
.action_({ arg bt; if (bt.value == 1) {playRout.play} {playRout.pause}});

// Net Address and Port of the Receiver.
receiverNet = NetAddr.new("127.0.0.1", 57120);

playRout= Task({
	var key, keySteps, waitKey, nextKey, wait, waitCycle, action, rec= -1;
    var objType, synthType;
	inf.do({
		keySteps= msetKey.next(\n);
		key= msetWait.dict.keys.asArray.wchoose([ 0.15, 0.57, 0.28 ]); // ave is the most probable
		keySteps.do({ |i|
            if (waitKey.isNil) {waitKey = msetWait.next(key)} {waitKey = nextKey};
            nextKey= msetWait.next(key);

            // Send the application the message with
            // WARNING! Specifying object type and synth name explicitly only TEMPORARY!
            // TO-DO: Invent something smart, how to get the from the pool,
            // depending on time of execution (the poll can be a JSON file).
            objType = ["a RedObject", "a RedBoid"].choose;
            synthType = [\sr_pulse, \sr_ringz].wchoose([0.8, 0.2]);
            receiverNet.sendMsg("/worldobj", synthType, objType, waitKey + nextKey, 600.0.rand, 200.0.rand, 20);

            waitCycle= wait= rrand(waitKey * 0.75, waitKey).max(1).round;
			action= msetAct.next(key);
			//postf("key: %\tsteps: %\twait: %\taction: %\n", key, keySteps, wait, action);

			// probability of recording: (input OR all) OR (no rec)
			if(0.2.coin, { rec= if(0.5.coin, { 0 }, { 1 }) }, { rec= -1 });

			while({ wait > 0 }, {
				{ // interface
					lbAction.string= action;
					lbSteps.string= keySteps-i;
					lbKey.string= key.asString.toUpper;
					lbWait.string= wait.asString;
					liWait.value= wait.linlin(0, waitCycle, 1, 0);
					if(liWait.value <= warn, {
						lbWait.stringColor= Color.new255( 34, 139, 34 )
						},
					{
						if( liWait.value < crit, {
							lbWait.stringColor= Color.new255( 139, 90, 43 );
							},
						{
							lbWait.stringColor= Color.new255( 183, 57, 57 )
							});
						});
					}.defer;
				wait= wait-1;
				quant.wait;
				});
			});
		0.001.wait;
		});
	});

w.refresh;
w.front;

CmdPeriod.doOnce({
    // playRout.stop;
    // w.close;
	});
)