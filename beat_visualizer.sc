// Simple beat-tracking GUI for SuperCollider
(
var window, beatView;
var margin = 5, gap = 5;
var colorBg = Color.new255(24, 24, 24);
var fontLabel = Font("Menlo", 12);
var palette = QPalette.dark;

var runTempoClock = { |tempo, beatsPerBar|
    var scheduler = TempoClock.new(tempo/60)
        .schedAbs(0, { scheduler.beatsPerBar_(beatsPerBar) });

    scheduler
};

// Tempo and time signature settings
~beatsPerBar = 4;
~clicksPerBeat = 4;
~barsPerCycle = 4;
~tempo = 120;


window = Window("Beat Visualizer", Rect(1500, 1000, 500, 150), resizable: false);
window.view.decorator = FlowLayout(window.view.bounds);
window.view.decorator.gap = gap@gap;
palette.setColor(colorBg, \window);
window.view.palette = palette;
window.front;

beatView = UserView(window, Rect(0, 0, window.bounds.width, window.bounds.height))
    .background_(Color.clear)
    .drawFunc_({ |view|
        Pen.font = fontLabel;

        if (~tempoClock.isRunning) {
            // Pen.fillColor = Color.new255(19, 26, 42);
            Pen.fillColor = Color.blue(0.8);
            Pen.stringAtPoint(
                format("Bar: %, Beat: %", ~tempoClock.bar, ~tempoClock.beatInBar),
                Point(margin*4, margin*4)
            );
        } {
            Pen.fillColor = Color.red(0.8);
            Pen.stringAtPoint(
                "Press R to start, ESC to stop, C to clear.",
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
            ~tempoClock = runTempoClock.(~tempo, ~beatsPerBar);
        } {
            "Already running".postln;
        };
    }
    { (modifiers == 0) && (char == $c) } {
        "Clearing".postln;
        beatView.refresh;
    }
    { (modifiers == 0) && (keycode == 53) } {
        "Stopping".postln;
        ~tempoClock.stop;
        beatView.refresh;
    }
    // ...all the rest
    { [view, char, modifiers, unicode, keycode].postln }
});


CmdPeriod.doOnce({
    "[!] Caught signal: <shut down>".postln;
    ~tempoClock.stop;
    window.close;
    postf("OK\n\n");
})
)
