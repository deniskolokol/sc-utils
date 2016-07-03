(
var num= 5, channels= 52;
var globalFont1=Font("Helvetica", 9);
var btColorBG = Color.new255(248, 248, 255), btColorOff=Color.new255(250, 250, 250), btColorOn=Color.new255(43, 43, 43), ctlColor11=Color.new255(205, 92, 92), btColorPlay=Color.new255(178, 34, 34), btColorStop=Color.new255(238, 216, 174);
var pmInBus, slInLevel, btDependOn, slRoom, nbRoom, slDamp, nbDamp, slDryWet, nbDryWet, slFxLevel, nbFxLevel, slDryLevel, nbDryLevel;
var btOnOff= Array.fill(num, nil);
var w;
var responder, ampAnalyserSynth, ampAnalFunc;
var chlistStereo= Array.fill(trunc(channels / 2), nil);
var grRev, reverbList= Array.fill(num, nil);

w= GUI.window.new("Flex FreeVerb", Rect(800, 300, 300, 170), resizable:false);
w.acceptsMouseOver= true;
w.view.background= Color.new255(205, 201, 165);

channels.do({ |i|
	if( i < (channels / 2), { chlistStereo.put(i, (i*2).asSymbol ++ ", " ++ ((i*2)+1).asSymbol ); } );
	i=i+1;
});

SCStaticText(w,Rect(5, 5, 40, 18))
	.string_("in bus")
	.font_(globalFont1);
pmInBus= SCPopUpMenu(w, Rect(45, 5, 62, 18))
	.items_(chlistStereo)
	.background_(btColorBG)
	.canFocus_(false)
	.font_(globalFont1)
	.action_({ |pm|
		ampAnalyserSynth.set(\inbus, pm.value*2);
	});
slInLevel= SmoothSlider(w, Rect(115, 6, 130, 16))
	.hilightColor_( Gradient( Color.red.alpha_(0.9), Color.green.alpha_(0.8), \v ) )
	.knobSize_(0.01)
	.knobColor_(ctlColor11)
	.canFocus_(false)
	.mode_(\move)
	.value_(0)
	.action_({ |sl|
		if(btDependOn.value == 1, {
			slFxLevel.valueAction= sl.value;
			slDryLevel.valueAction= 1-sl.value;
		})
	});
btDependOn= RoundButton(w, Rect(250, 5, 40, 18))
	.states_([
		["OFF", btColorPlay, btColorStop],
		["ON",  btColorStop, btColorPlay]
	])
	.font_(globalFont1)
	.canFocus_(false)
	.radius_( 3 )
	.action_({ |butt|


//		GUI.dialog.getPaths({arg paths;
//			this.loadBuffers(paths);
//		});


	});


//room


SCStaticText(w,Rect(5, 30, 40, 18))
	.string_("Room")
	.font_(globalFont1);
slRoom= SmoothSlider(w, Rect(45, 30, 200, 16))
	.knobSize_(0.4)
	.mode_(\jump)
	.value_(0.8)
	.action_({ |sl|
		nbRoom.value = round(sl.value, 0.01);
		num.do({ |i|
			if(reverbList[i] != nil, { reverbList[i].set(\room, sl.value) })
		});
	});
nbRoom= SCNumberBox(w,Rect(slRoom.bounds.left+slRoom.bounds.width+5, 30, 40, 16))
	.font_(globalFont1)
	.value_(slRoom.value)
	.action_({ |nb|
		slRoom.valueAction= nb.value;
	});


//damp


SCStaticText(w,Rect(5, 50, 40, 18))
	.string_("Damp")
	.font_(globalFont1);
slDamp= SmoothSlider(w, Rect(45, 50, 200, 16))
	.knobSize_(0.4)
	.mode_(\jump)
	.value_(0.8)
	.action_({ |sl|
		nbDamp.value = round(sl.value, 0.01);
		num.do({ |i|
			if(reverbList[i] != nil, { reverbList[i].set(\damp, sl.value) })
		});
	});
nbDamp= SCNumberBox(w,Rect(slDamp.bounds.left+slDamp.bounds.width+5, 50, 40, 16))
	.font_(globalFont1)
	.value_(slDamp.value)
	.action_({ |nb|
		slDamp.valueAction= nb.value;
	});


//dry/wet


SCStaticText(w,Rect(5, 70, 40, 18))
	.string_("Dry/Wet")
	.font_(globalFont1);
slDryWet= SmoothSlider(w, Rect(45, 70, 200, 16))
	.knobSize_(0.4)
	.mode_(\jump)
	.value_(0.75)
	.action_({ |sl|
		nbDryWet.value = round(sl.value, 0.01);
		num.do({ |i|
			if(reverbList[i] != nil, { reverbList[i].set(\mix, sl.value) })
		});
	});
nbDryWet= SCNumberBox(w,Rect(slDryWet.bounds.left+slDryWet.bounds.width+5, 70, 40, 16))
	.font_(globalFont1)
	.value_(slDryWet.value)
	.action_({ |nb|
		slDryWet.valueAction= nb.value;
	});


//fx level


SCStaticText(w,Rect(5, 90, 40, 18))
	.string_("Fx level")
	.font_(globalFont1);
slFxLevel= SmoothSlider(w, Rect(45, 90, 200, 16))
	.hilightColor_( Gradient( Color.blue.alpha_(0.8), Color.blue.alpha_(0.01), \v ) )
	.knobSize_(0.2)
	.mode_(\jump)
	.value_(0.8)
	.action_({ |sl|
		nbFxLevel.value = round(sl.value, 0.01);
		num.do({ |i|
			if(reverbList[i] != nil, { reverbList[i].set(\fxlevel, sl.value) })
		});
	});
nbFxLevel= SCNumberBox(w,Rect(slFxLevel.bounds.left+slFxLevel.bounds.width+5, 90, 40, 16))
	.font_(globalFont1)
	.value_(slFxLevel.value)
	.action_({ |nb|
		slFxLevel.valueAction= nb.value;
	});


//dry level


SCStaticText(w,Rect(5, 110, 40, 18))
	.string_("Dry level")
	.font_(globalFont1);
slDryLevel= SmoothSlider(w, Rect(45, 110, 200, 16))
	.hilightColor_( Gradient( Color.blue.alpha_(0.8), Color.blue.alpha_(0.01), \v ) )
	.knobSize_(0.2)
	.mode_(\jump)
	.value_(0.2)
	.action_({ |sl|
		nbDryLevel.value = round(sl.value, 0.01);
		num.do({ |i|
			if(reverbList[i] != nil, { reverbList[i].set(\level, sl.value) })
		});
	});
nbDryLevel= SCNumberBox(w,Rect(slDryLevel.bounds.left+slDryLevel.bounds.width+5, 110, 40, 16))
	.font_(globalFont1)
	.value_(slDryLevel.value)
	.action_({ |nb|
		slDryLevel.valueAction= nb.value;
	});


//channels on/off buttons
num.do({ |i|
	btOnOff[i]= RoundButton(w, Rect(56*i+9, 140, 56, 20))
		.states_([
			[chlistStereo[i].asString, btColorOn, btColorOff],
			[chlistStereo[i].asString, btColorOff, btColorOn]
		])
		.font_(globalFont1)
		.canFocus_(false)
		.border_(1)
		.radius_( 3 )
		.action_({ |bt|
			if ( bt.value == 1 )
			{
				reverbList[i]= Synth.tail(grRev, \gmpFreeverb2x2,
					[\inbus, pmInBus.value*2, \outbus, i*2, \mix, slDryWet.value, \room, slRoom.value,
					\damp, slDamp.value, \fxlevel, slFxLevel.value, \level, slDryLevel.value]);
			}
			{
				if ( reverbList[i] != nil )
				{
					reverbList[i].free; reverbList[i]= nil
				}
			};
			// WARNING! if ( true ) { 1 } { 0 }  seems to be clearer than  if(( true ), { 1 }, { 0 })


		});
});

responder = OSCresponderNode(s.addr,'/tr',{ arg time, responder, msg;
	{
		w.isClosed.not.if({ // if window is not closed, update GUI...
			if (msg[1] == ampAnalyserSynth.nodeID, {
				slInLevel.valueAction_(msg[3].ampdb.linlin(-60, 0, 0, 1));
			});
		});
	}.defer;
}).add;

ampAnalFunc = { // this is called on CmdPeriod
	ampAnalyserSynth = Synth(\xiiVuMeter, [\inbus, pmInBus.value*2, \amp, 1], addAction:\addToTail);
};
ampAnalFunc.value;

grRev= Group.after(1);

w.refresh;
w.front;

w.onClose = {
	ampAnalyserSynth.free; // kill the analyser
	grRev.free; // kill the group
	responder.remove; // kill the OSC responder
};

SynthDef(\gmpFreeverb2x2, { arg inbus=0, outbus=0, mix=0.25, room=0.15, damp=0.5, fxlevel=0.75, drylevel=0;
	var fx, sig;
	sig = In.ar(outbus, 2);
	fx = FreeVerb.ar(sig, mix, room, damp);
	ReplaceOut.ar(outbus, (fx*fxlevel) + (sig*drylevel));
},[0,0,0.1,0.1,0,0]).send(s);

)