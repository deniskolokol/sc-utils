(
var win;
var width = 850;
var height = 850;

var gap = 5, margin = 5;
var paneTop, paneMain, paneRight, paneBottom;
var envView;

var colorBg = Color.grey(0.15);
var colorPane = Color.grey(0.2);
var drawColor = Color.grey(0.1);
var transparent = Color.grey(alpha:0.0);
var fontHeader = Font("Helvetica", 14);
var fontLabel = Font("Helvetica", 12);
var fontControl = Font("Helvetica", 10);
var fontButton = Font("Helvetica", 11);

var makePanel = { |parent, x, y, w, h, color|
    var panel = CompositeView(parent, Rect(x, y, w, h)).background_(color ? colorPane);
    panel.decorator = FlowLayout(panel.bounds, margin@margin, gap@gap);
    panel;
};

var controlStrip;

// Default env descriptor for testing the editor.
var envLevels = [0.0, 0.219, 0.664, -0.511, -0.964, 0.556, 0.0];
var envTimes = [1.377, 1.059, 1.155, 1.245, 1.131, 2.117];
var envCurves = [\sine, 2.071, \sine, \sine, 0, -3.617];
var env = Env(envLevels, envTimes, envCurves);

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

// Top panel controls.
{
    EZPopUpMenu(paneTop,
        Rect(margin, margin, paneTop.bounds.width * 0.35, paneTop.bounds.height * 0.8),
        "Presets",
        ["sine", "saw", "tri"],
        globalAction: { |menu| menu.value.postln },
        initVal: 0,
        labelWidth: 60
    ).setColors(
        stringColor: Color.white,
        menuStringColor: Color.white,
        menuBackground: colorBg,
        background: transparent,
    ).font_(Font("Monospace", 12));

    ["Override", "Save as", "New", "New rand"].do { |label|
        Button(paneTop,
            Rect(0, 0, paneTop.bounds.width * 0.12, paneTop.bounds.height * 0.8)
        ).states_([
            [label, Color.white, Color.grey(0.5)]
        ]).font_(Font("Helvetica", 11)).action_({ |bt| bt.value.postln });
    };
}.value;

// Main panel: Plotter
{
    var plotView = Plotter(
        name: "WT",
        bounds: Rect(0, 0, paneMain.bounds.width-(margin * 2), paneMain.bounds.height - (margin * 2)),
        parent: paneMain
    );
    plotView.value = env.asSignal;
    plotView.editMode = false;
    plotView.plotMode = \stems;
    plotView.setProperties(
        \fontColor, Color(0.5, 1, 0);,
        \plotColor, Color.red.alpha_(0.85),
        \backgroundColor, drawColor,
        \gridColorY, Color.yellow(0.5),
        \gridOnX, false
    );
    plotView.refresh;
}.value;


// TODO:
// Populate paneRight with the list of actions available for UNDO (use ScrollView).


// Labels for control elements (curve, duration, slope, etc.)
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


// Control strip.
controlStrip = { |index, parent|
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
    knobLevel = knob.(parent, " ", [-1.0, 1.0].asSpec, { |ez| }, envLevels[index+1]);
    knobTime = knob.(parent, " ", [0.0, 2.0].asSpec, { |ez| }, envTimes[index]);
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
    if ( envCurves[index].isFloat ) {
        knobCurve.value_(envCurves[index]);
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

// Scrolling area for controls of the envelope segments.
{
    var ctrlStripWidth = 80;
    var compWidth = (envTimes.size * ctrlStripWidth) + ((envTimes.size - 1) * gap) + (margin * 2);
    var scroll = ScrollView(paneBottom, Rect(0, 0, paneBottom.bounds.width-105, paneBottom.bounds.height-(margin*2)))
        .background_(colorPane);
    var comp = CompositeView(scroll, Rect(0, 0, compWidth, scroll.bounds.height)); // 'canvas' is this big

    comp.decorator = FlowLayout(comp.bounds); // now we can use a decorator
    envTimes.do { |value, i|
        var pane = makePanel.(comp, 0, 0, 80, comp.bounds.height-(margin*2), colorBg);
        controlStrip.value(i, pane);
    }
}.value;

win.front;

CmdPeriod.doOnce({
    win.close;
});
)