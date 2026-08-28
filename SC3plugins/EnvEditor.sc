(
// GUI settings.
var width = 850;
var height = 850;
var gap = 5, margin = 5;
var colorBg = Color.grey(0.15);
var colorPane = Color.grey(0.2);
var drawColor = Color.grey(0.1);
var transparent = Color.grey(alpha:0.0);
var fontHeader = Font("Helvetica", 14);
var fontLabel = Font("Helvetica", 12);
var fontControl = Font("Helvetica", 10);
var fontButton = Font("Helvetica", 11);


// GUI elements.
var win;
var paneTop, paneMain, paneRight, paneBottom;
var presetSelector;
var plotView;
var ctrlStripView;


// Default gloelope descriptor that opens when the editor is opened without Env.
var envLevels = [0.0, 0.219, 0.664, -0.511, -0.964, 0.556, 0.0];
var envTimes = [1.377, 1.059, 1.155, 1.245, 1.131, 2.117];
var envCurves = [\sine, 2.071, \sine, \sine, 0, -3.617];
var envelope = Env(envLevels, envTimes, envCurves);

// TODO: Read this from external file
var presets = (
    wiggly: (
        levels: [0.0, 0.855, -0.983, 0.911, -0.354, 0.093, 0.063, -0.697, -0.271, 0.0],
        times: [1.827, 1.104, 2.085, 2.682, 1.114, 2.001, 2.045, 2.911, 1.701],
        curve: [0.0, 0.0, 5.515, \sine, -16.179, \sine, \sine, \sine, \sine]
    ),
    broaderik: (
        levels: [0.0, -0.092, -0.469, 0.624, -0.847, 0.309, 0.939, 0.447, -0.053, 0.374, -0.009, 0.0],
        times: [3.516, 1.135, 4.689, 3.868, 3.263, 3.369, 3.301, 1.952, 1.168, 5.537, 4.497],
        curve: [2.886, 0.0, 4.29, \sine, -1.721, 0.0, -11.733, 1.154, -15.583, 3.664, 1.355]
    ),
    flow: (
        levels: [0.0, -0.597, 0.967, -0.077, -0.583, 0.602, -0.292, -0.76, 0.997, 0.34, -0.967, 0.089, 0.612, 0.841, 0.0],
        times: [2.608, 6.674, 2.612, 6.949, 1.392, 1.32, 8.755, 3.013, 5.986, 1.64, 1.756, 2.223, 1.43, 6.891],
        curve: [\sine, -8.932, \sine, -18.882, 0.0, 0.0, 0.0, \sine, -3.03, -1.938, \sine, 4.377, \sine, 2.907]
    ),
    zombki: (
        levels: [0.0, -0.168, 0.05, -0.129, -0.232, 0.196, 0.9, -0.644, -0.679, -0.793, 0.104, -0.571, 0.061, -0.617, 0.264, -0.867, 0.529, 0.925, 0.974, 0.0],
        times: [2.838, 11.887, 8.25, 2.021, 9.967, 10.5, 10.647, 11.555, 6.593, 4.335, 4.215, 9.586, 1.464, 3.892, 1.141, 8.736, 12.437, 9.063, 3.147],
        curve: [0.0, -2.067, \sine, 4.88, 0.0, 0.0, \sine, -13.804, 0.0, \sine, -7.557, \sine, \sine, 0.0, \sine, 7.58, 0.0, -3.453, 18.079]
    )
);

var makePanel = { |parent, x, y, w, h, color|
    var panel = CompositeView(parent, Rect(x, y, w, h)).background_(color ? colorPane);
    panel.decorator = FlowLayout(panel.bounds, margin@margin, gap@gap);
    panel;
};

// GUI elements for controlling individual segment of the envelope.
var makeControlStrip = { |index, parent|
    var knobWidth = parent.bounds.width-(margin*2);
    var knobHeight = knobWidth + 20;
    var knobLevel, knobTime, knobCurve, pumCurve;
    var knob = { |par, label, spec, action, initVal|
        if (initVal.isNil) { initVal = spec.default };
        EZKnob(par, knobWidth@knobHeight, " " ++ label.asString, spec,
            { |ez| action.(ez.value) },
            initVal, layout: \vert2
        )
        .font_(fontControl)
        .setColors(
            stringColor:Color.white,
            numBackground:Color.grey,
            knobColors:[Color.grey(0.1), Color.red, Color.white, Color.red],
            numNormalColor:Color.yellow,
        )
    };

    // Index label for the current envelope point.
    StaticText(parent, (parent.bounds.width - 30 - (2*gap))@20)
        .string_((index+1).asString.padLeft(2, string: "0"))
        .align_(\center)
        .stringColor_(Color.white)
        .background_(drawColor)
        .font_(fontHeader);

    // Delete segment.
    Button(parent, 25@20)
        .states_([["×", Color.white, drawColor]])
        .font_(fontButton)
        .action_({ |bt| "remove".postln });

    // Knobs for controlling parameters of the env (leaving labels empty to space them a bit).
    knobLevel = knob.(parent, " ", [-1.0, 1.0].asSpec,
        { |ez|
            envelope.levels[index+1] = ez.value;
            plotView.value = envelope.asSignal;
            plotView.refresh
        },
        envelope.levels[index+1]
    );
    knobTime = knob.(parent, " ", [0.01, 2.00].asSpec,
        { |ez|
            envelope.times[index] = ez.value;
            plotView.value = envelope.asSignal;
            plotView.refresh;
        },
        envelope.times[index]
    );
    knobCurve = knob.(parent, " ", [-5.0, 5.0].asSpec, { |ez| }, 0.0);

    // Standard curves (mutually exclusive with slope).
    pumCurve = EZPopUpMenu(parent,
        (parent.bounds.width - (margin*2))@18,
        items: ['..', \sine, \squared, \cubed, \exp, \exponential, \hold, \step, \welch],
        globalAction: { |menu| menu.value.postln },
        initVal: 0,
        labelWidth: 60
    ).setColors(
        stringColor: Color.white,
        menuStringColor: Color.white,
        menuBackground: colorBg,
        background: transparent,
    ).font_(Font("Monospace", 12));

    // TODO: move this to watcher
    if ( envelope.curves[index].isFloat ) {
        knobCurve.value_(envelope.curves[index]);
        knobCurve.enabled_(true)
    } {
        knobCurve.enabled_(false)
    };

    // Buttonms for inserting/adding segments.
    Button(parent, 25@20)
        .states_([["◀", Color.white, drawColor]])
        .action_({ |bt| "insert left".postln });
    StaticText(parent, (parent.bounds.width - 50 - (2*gap)-(2*margin))@20)
        .string_("+")
        .align_(\center)
        .stringColor_(Color.white)
        .font_(fontHeader);    
    Button(parent, 25@20)
        .states_([["▶", Color.white, drawColor]])
        .action_({ |bt| "add".postln });
};

var makeControlPanel = {
    var ctrlStripWidth = 80;

    // Recalculate the composite view size based on new envelope
    var compWidth = (envelope.times.size * ctrlStripWidth) + ((envelope.times.size - 1) * gap) + (margin * 2);

    // Clear existing elements from the control panel
    ctrlStripView.children.removeAll;

    ctrlStripView.bounds = Rect(0, 0, compWidth, ctrlStripView.bounds.height);

    // Reset the decorator
    ctrlStripView.decorator = FlowLayout(ctrlStripView.bounds);

    // Recreate GUI elements from the envelope instance
    (envelope.times-1).do { |value, i|
        var pane = makePanel.(ctrlStripView, 0, 0, 80, ctrlStripView.bounds.height-(margin*2), colorBg);
        makeControlStrip.value(i, pane);
    };
    
    ctrlStripView.refresh;
};

win = Window("EnvEditor", Rect(0, 0, width, height), resizable: false);
win.background_(colorBg);

// Available drawing area after outer margins and the gaps between rows.
{
    var innerW = width - (margin * 2);
    var innerH = height - (margin * 2);
    var stackH = innerH - (gap * 2);
    var topH = (stackH * 0.04).floor;
    var mainH = (stackH * 0.5).floor;
    var bottomH = stackH - topH - mainH;
    var mainW = ((innerW - gap) * 0.9).floor;
    var rightW = (innerW - mainW - gap).floor;

    paneTop = makePanel.(win, margin, margin, innerW, topH, colorPane);
    paneMain = makePanel.(win, margin, paneTop.bounds.bottom + gap, mainW, mainH, colorPane);
    paneRight = makePanel.(win, paneMain.bounds.right + gap, paneMain.bounds.top, rightW, mainH, colorPane);
    paneBottom = makePanel.(win, margin, paneMain.bounds.bottom + gap, innerW, bottomH, colorPane);
}.value;


// Main panel: Plotter
// Warning: filling up this panel first to have a plotter GUI element ready for the rest
{
    plotView = Plotter(
        name: "WT",
        bounds: Rect(0, 0, paneMain.bounds.width-(margin * 2), paneMain.bounds.height - (margin * 2)),
        parent: paneMain
    );
    plotView.value = envelope.asSignal;
    plotView.editMode = false;
    plotView.setProperties(
        \fontColor, Color(0.5, 1, 0);,
        \plotColor, Color.red.alpha_(0.5),
        \backgroundColor, drawColor,
        \gridColorY, Color.yellow(0.5),
        \gridOnX, false
    );
    plotView.refresh;
}.value;


// Labels for control elements (curve, duration, slope, etc.)
// Warning: creating and filling this panel prior to the top panel to have controls ready for the rest.
{
    var labelCtrl = { |parent, label, height|
        StaticText(parent, 80@height)
            .string_("  " ++ label.asString)
            .align_(\bottomLeft)
            .stringColor_(Color.white)
            .background_(colorBg)
            .font_(fontHeader);
    };

    var labelCtrlPanel = makePanel.(paneBottom, 0, 0, 90, paneBottom.bounds.height-(2*margin), colorPane);

    labelCtrl.(labelCtrlPanel, "segment", 20+gap);
    labelCtrl.(labelCtrlPanel, "level", 85+gap);
    labelCtrl.(labelCtrlPanel, "duration", 85+gap);
    labelCtrl.(labelCtrlPanel, "slope", 85+gap);
    labelCtrl.(labelCtrlPanel, "std curve", 20);
}.value;


// Scrolling area for controls of the envelope segments.
{
    var ctrlStripWidth = 80;
    var compWidth = (envelope.times.size * ctrlStripWidth) + ((envelope.times.size - 1) * gap) + (margin * 2);
    var scroll = ScrollView(paneBottom, Rect(0, 0, paneBottom.bounds.width-105, paneBottom.bounds.height-(margin*2)))
        .background_(colorPane)
        .hasVerticalScroller_(false)
        .hasBorder_(false);
    ctrlStripView = CompositeView(scroll, Rect(0, 0, compWidth, scroll.bounds.height)); // 'canvas' is this big

    ctrlStripView.decorator = FlowLayout(ctrlStripView.bounds); // now we can use a decorator
    makeControlPanel.value;
}.value;


// Top panel controls.
{
    presetSelector = EZPopUpMenu(paneTop,
        Rect(margin, margin, paneTop.bounds.width * 0.35, paneTop.bounds.height * 0.8),
        "Presets",
        globalAction: { |m|
            var val = presets[m.item];
            envelope = Env.new(val.levels, val.times, val.curve);

            // Re-draw the plot.
            plotView.value = envelope.asSignal;
            plotView.refresh;

            // Re-populate the controls.
            makeControlPanel.value;
        },
        initVal: 0,
        labelWidth: 60
    ).setColors(
        stringColor: Color.white,
        menuStringColor: Color.white,
        menuBackground: colorBg,
        background: transparent,
    ).font_(Font("Monospace", 12));

    presets.keysValuesDo { |name, val|
        presetSelector.addItem(name, { |a|
            envelope = Env.new(val.levels, val.times, val.curve) });

            // Re-draw the plot.
            plotView.value = envelope.asSignal;
            plotView.refresh;

            // Re-populate the controls.
            makeControlPanel.value;
    };
}.value;

// Preset control buttons
{
    ["Override", "Save as", "New", "New rand"].do { |label|
        Button(paneTop,
            Rect(0, 0, paneTop.bounds.width * 0.12, paneTop.bounds.height * 0.8)
        ).states_([
            [label, Color.white, Color.grey(0.5)]
        ]).font_(Font("Helvetica", 11)).action_({ |bt| bt.value.postln });
    };
}.value;


// TODO:
// Populate paneRight with the list of actions available for UNDO (use ScrollView).

win.front;

CmdPeriod.doOnce({
    win.close;
});
)