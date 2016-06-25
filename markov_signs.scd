(
var msetWait, msetKey, msetAct;
var playRout, quant= 1;
var w, lbRec=[nil, nil], lbKey, lbSteps, lbWait, liWait, lbAction, lbRecAll; // main window
var statList= List.new, actionArr= #["+", "++", "-", "--", "="], statFile;
var warn= 0.6, crit= 0.9;

msetWait= MarkovSet([ // markov set for waiting time
	[\s, [2.5, 3.5, 4, 4.5, 5, 5.5]], // short
	[\a, [5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9, 10, 12]], // ave
	[\l, [20, 25, 27, 32, 40, 45, 55, 60, 90, 120], [6,7,4,4,3,3,2,2,1,1].normalizeSum ] // long
	]);

msetAct= MarkovSet([ // markov set for action
	[\s, actionArr, [ 0.22, 0.27, 0.21, 0.26, 0.04 ]], // short: +- prob is hihger
	[\a, actionArr], // ave: all action have even chances
	[\l, actionArr, [ 0.17, 0.10, 0.17, 0.10, 0.46 ] ] // long: +- prob is lower
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

2.do({|i|
	lbRec[i]= StaticText(w,Rect(40+(i*60), 5, w.bounds.width/2-10, 30))
		.string_("R")
		.align_(\center)
		.stringColor_(Color.red(alpha:0.1))
		.background_(Color.grey(0.8))
		.font_(Font("Helvetica", 24));
	});
lbAction= StaticText(w,Rect(5, 40, w.bounds.width-10, 170))
	.string_("")
	.align_(\center)
	.font_(Font("Helvetica", 124))
	.align_(\center);
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
.action_({ arg bt; if (bt.value == 1) {playRout.play} {playRout.stop}});

playRout= Routine({
	var key, keySteps, waitKey, wait, waitCycle, action, rec= -1;
	inf.do({
		keySteps= msetKey.next(\n);
		key= msetWait.dict.keys.asArray.wchoose([ 0.15, 0.57, 0.28 ]); // ave is the most probable
		keySteps.do({ |i|
			waitKey= msetWait.next(key);
			waitCycle= wait= rrand(waitKey * 0.75, waitKey).max(1).round;
			action= msetAct.next(key);
			//postf("key: %\tsteps: %\twait: %\taction: %\n", key, keySteps, wait, action);

			// probability of recording: (input OR all) OR (no rec)
			if(0.2.coin, { rec= if(0.5.coin, { 0 }, { 1 }) }, { rec= -1 });

			while({ wait > 0 }, {
				action.switch( // collect statistics
					"+", { statList.add(waitCycle-wait) },
					"++", { statList.add( (waitCycle-wait).linlin(0, waitCycle, 0, 1).pow(3) ) },
					"-", { statList.add(wait) },
					"--", { statList.add( wait.linlin(0, waitCycle, 0, 1).pow(3) ) },
					"=", { statList.add(1) }
				);

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
					if(rec == -1, {
						2.do({ |j|
							lbRec[j].stringColor_(Color.red(alpha:0.1));
							lbRec[j].background_(Color.grey(0.8));
							});
						},
					{
						lbRec[rec].stringColor_(Color.white);
						lbRec[rec].background_(Color.red);
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
	playRout.stop;
	w.close;
	statList.do({ |i| postf("%,", i) });
	statList.asArray.plot2;
	statFile= File.new("preferences/markov.dk", "w");
	statFile.write(statList.round(0.01).asCompileString);
	statFile.close;
	});
)