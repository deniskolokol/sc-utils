// Simple beat-tracking GUI for SuperCollider
(
var window, beatView;
var margin = 5, gap = 5;
var colorBg = Color.new255(24, 24, 24);
var fontLabel = Font("Menlo", 12);
var palette = QPalette.dark;
var colors = (
    \beat: Color.gray(0.2),
    \click: Color.gray(0.3),
    \accent: Color.gray(0.8)
);

var cleanUp = {
    // Clean up and close the window
    if (~tempoClock.isRunning) {
        ~tempoClock.stop;
    };
    ~tempoClock.clear;
    ~tempoClock = nil;

    window.close;
};


var setBeatsPerBar = { |scheduler, beats|
    scheduler.schedAbs(
        scheduler.nextBar,
        { thisThread.clock.beatsPerBar_(beats) }
    );
};

var fillAccents = { |beats, clicks, accentOn|
    // Accent pattern generator.
    // the param `accentOn` can be either:
    // - an Integer indicating which click to accent (1-based index),
    //   e.g. 1 for the first click, 2 for the second click, etc.
    // - an Array of click indices to accent, e.g. [1, 3] for accents on the
    //   first and third clicks.
    // 
    // Returns an Array of 0s and 1s, where 1 indicates an accented click.
    // Examples:
    // - for 4 clicks per beat, accent on the first click: [1, 0, 0, 0]
    // - for 4 clicks per beat, accent on the third click: [0, 0, 1, 0]
    // Irregular accent patterns can be created by setting accentOn to an Array
    // of click indices to accent, e.g. [1, 3] for accents on the first and
    // third clicks - in this case the resulting pattern would be [1, 0, 1, 0]
    // for 4 clicks per beat.
    //
    // Warning: invalid values (such as negative integers or val;ues that exceed
    // the number of clicks per beat) will result in no accents being applied.
    var accents;
    case 
    { accentOn.isInteger and: { accentOn > 0 } } {
        accents = Array.fill(beats, { |i|
            Array.fill(clicks, { |j| if (accentOn == (j+1)) { 1 } { 0 }})
        });
    }
    { accentOn.isArray } {
        accents = Array.fill(beats, { |i|
            Array.fill(clicks, { |j| if (accentOn.includes(j+1)) { 1 } { 0 }})
        });
    }
    { "Invalid accentOn value. Must be a positive Integer or an Array of click indices.".postln; 
      accents = Array.fill(beats, { |i| Array.fill(clicks, 0) }) // Default to no accents
    };
};


// Tempo and time signature settings
~beatsPerBar = 5;
~clicksPerBeat = 2;
~accentOn = 0; // Accent on the first and third clicks of each beat
~accents = fillAccents.(~beatsPerBar, ~clicksPerBeat, ~accentOn).postln;
~tempo = 120;
~tempoClock = TempoClock.new(~tempo/60);
setBeatsPerBar.(~tempoClock, ~beatsPerBar);


// GUI
window = Window("Beat Visualizer", Rect(1500, 1000, 800, 100), resizable: false);
window.view.decorator = FlowLayout(window.view.bounds);
window.view.decorator.gap = gap@gap;
palette.setColor(colorBg, \window);
window.view.palette = palette;
window.front;

beatView = UserView(window, Rect(0, 0, window.bounds.width, window.bounds.height))
    .background_(Color.clear)
    .drawFunc_({ |view|
        var left;
        var top;
        var widthBeat = ((view.bounds.width - (margin * 2)) / ~beatsPerBar) - (gap * 2);
        var width = widthBeat / ~clicksPerBeat - gap;
        var height = view.bounds.height - 20 - (margin * 4);

        Pen.font = fontLabel;
        if (~tempoClock.isRunning) {
            Pen.fillColor = Color.blue(0.8);
            Pen.stringAtPoint(
                format("Bar: %, Beat: %", ~tempoClock.bar, ~tempoClock.beatInBar),
                Point(margin*4, margin)
            );

            Pen.strokeColor_(Color.gray(alpha: 0.5));
            ~beatsPerBar.do { |beat|
                left = margin + (beat * (widthBeat + (2 * gap)));
                top = margin + 20; // Leave space for the label

                // Draw the beat rectangles.
                Pen.strokeColor_(colors[\beat]);
                Pen.strokeRect(Rect(left, top, widthBeat, height));

                // Draw the click rectangles within each beat.
                Pen.strokeColor_(colors[\click]);
                ~clicksPerBeat.do { |click|
                    left = margin + (beat * (widthBeat + (2 * gap))) + (click * width) + (gap * (click + 1));
                    top = margin + 20 + gap;
                    if (beat > ~tempoClock.beatInBar.floor or:
                            (beat == ~tempoClock.beatInBar.floor and: 
                                (click > ((~tempoClock.beatInBar - ~tempoClock.beatInBar.floor) * ~clicksPerBeat).floor)
                            )
                        ) {
                        Pen.strokeRect(Rect(left, top, width - gap, height - (gap * 2)));
                    } {
                        if (~accents[beat][click] == 1) {
                            Pen.fillColor = colors[\accent];
                            Pen.fillRect(Rect(left, top, width, height - (gap * 2)));
                        } {
                            // Non-accented clicks are dimmer and smaller.
                            Pen.fillColor = colors[\click];
                            Pen.fillRect(Rect(left + (gap/2), top + (gap/2.5), width - (gap * 1.5), height - (gap * 2.5)));
                        };
                    };
                };
            };
        } {
            Pen.fillColor = Color.red(0.8);
            Pen.stringAtPoint(
                "Press [R] to start, [S] to stop, [ESC] to clear and close.",
                Point(margin*4, margin*4)
            );
        };
    });
beatView.animate = true;


// Set up key handlers
window.view.keyDownAction_({ |view, char, modifiers, unicode, keycode|
    case
    { (modifiers == 0) && (char == $r) } {
        if (~tempoClock.isRunning.not) {
            "Starting".postln;
            ~tempoClock.schedAbs(0, { ~tempoClock.beatsPerBar_(~beatsPerBar) });
        } {
            "Already running".postln;
        };
    }
    { (modifiers == 0) && (char == $s) } {
        "Stopping".postln;
        ~tempoClock.stop;
        beatView.refresh;
    }
    { (modifiers == 0) && (keycode == 53) } {
        "Clearing".postln;
        cleanUp.();
    }
    // ...all the rest
    { [view, char, modifiers, unicode, keycode].postln }
});


CmdPeriod.doOnce({
    "[!] Caught signal: <shut down>".postln;
    cleanUp.();
    postf("OK\n\n");
})
)
