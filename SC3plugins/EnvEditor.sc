(
var win;
var width = 800;
var height = 450;

var gap = 5, margin = 5;
var paneTop, paneMain, paneRight, paneBottom;
var envView;

var colorBg = Color.grey(0.15);
var colorPane = Color.grey(0.2);
var transparent = Color.grey(alpha:0.0);
var fontControl = Font("Helvetica", 12);
var fontButton = Font("Helvetica", 11);
var fontLabel = Font("Helvetica", 10);

var makePanel = { |parent, x, y, w, h, color|
    var panel = CompositeView(parent, Rect(x, y, w, h)).background_(color ? colorPane);
    panel.decorator = FlowLayout(panel.bounds, margin@margin, gap@gap);
    panel;
};

win = Window("EnvEditor", Rect(0, 0, width, height), resizable: false);
win.background_(colorBg);

// Available drawing area after outer margins and the gaps between rows.
{
    var innerW = width - (margin * 2);
    var innerH = height - (margin * 2);
    var stackH = innerH - (gap * 2);
    var topH = (stackH * 0.08).floor;
    var mainH = (stackH * 0.84).floor;
    var bottomH = stackH - topH - mainH;
    var mainW = ((innerW - gap) * 0.9).floor;
    var rightW = (innerW - mainW - gap).floor;

    paneTop = makePanel.(win, margin, margin, innerW, topH, colorPane);
    paneMain = makePanel.(win, margin, paneTop.bounds.bottom + gap, mainW, mainH, colorPane);
    paneRight = makePanel.(win, paneMain.bounds.right + gap, paneMain.bounds.top, rightW, mainH, colorPane);
    paneBottom = makePanel.(win, margin, paneMain.bounds.bottom + gap, innerW, bottomH, colorPane);
}.value;

// Top panel controls.
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

// Main panel: MasterEQ-inspired custom editor sketch with a centered zero line.
{
    var selected = -1;

    // Default env descriptor for testing the editor.
    var envLevels = [0.0, 0.219, 0.664, -0.511, -0.964, 0.0];
    var envTimes = [1.377, 1.059, 1.155, 1.245, 1.131];
    var envCurves = [\sine, 2.071, 0, \sine, -4.617];

    var envPoints;

    var makeEnvPoints = { |levels, times|
        var totalTime = times.sum;
        levels.collect { |level, i|
            var x = if(i == 0) { 0.0 } { times[0..(i - 1)].sum / totalTime };
            [x, level.clip(-1.0, 1.0)]
        }
    };

    envPoints = makeEnvPoints.(envLevels, envTimes);

    envView = UserView(paneMain, paneMain.bounds.insetBy(margin, margin))
        .resize_(5)
        .focusColor_(Color.clear);

    envView.drawFunc = { |vw|
        var origin = vw.bounds.moveTo(0, 0);
        var zeroY = origin.height * 0.5;
        var minVal = -1.0;
        var maxVal = 1.0;
        var step = 0.25;
        var pts;
        var prevPt;
        var delta, deltaLen;
        var perpendicular;
        var amplitude;
        var cPoint1, cPoint2;

        envPoints[0][1] = 0.0;
        envPoints[envPoints.size - 1][1] = 0.0;
        envPoints[0][0] = 0.0;
        envPoints[envPoints.size - 1][0] = 1.0;
        pts = envPoints.collect { |pt|
            Point(
                pt[0].linlin(0.0, 1.0, 0, origin.width),
                pt[1].linlin(-1.0, 1.0, origin.height, 0, \none)
            )
        };

        Pen.color = Color.grey(0.1);
        Pen.fillRect(origin);

        Pen.color = Color.grey(0.35).alpha_(0.35);
        ((maxVal - minVal) / step + 1).asInteger.do { |i|
            var v = minVal + (i * step);
            var y = v.linlin(minVal, maxVal, origin.height, 0, \none);
            Pen.line(0@y, origin.width@y);
            Pen.stringAtPoint(v.asStringPrec(2), Point(4, y + 2));
        };
        Pen.stroke;

        Pen.color = Color.white.alpha_(0.75);
        Pen.line(0@zeroY, origin.width@zeroY);
        Pen.stroke;

        Pen.color = Color.red.alpha_(0.25);
        // Pen.moveTo(Point(0, zeroY));
        // Pen.lineTo(pts[0]);
        Pen.moveTo(pts[0]);
        pts[1..].do { |pt, i|
            prevPt = pts[i];
            case
            { envCurves[i] == \sine } {
                // Calculate the vector from start to end point
                delta = pt - prevPt;

                // Calculate perpendicular vector (rotated 90 degrees) and normalize it
                deltaLen = delta.dist(Point(0, 0));
                perpendicular = Point(delta.y.neg / deltaLen, delta.x / deltaLen);

                // Define wave amplitude (height of the sine wave)
                amplitude = deltaLen * 0.15; // Adjust multiplier to change wave height

                // Calculate control points
                // Control point 1: 1/3 along the line, offset perpendicular
                cPoint1 = prevPt + (delta * 0.33) + (perpendicular * amplitude);

                // Control point 2: 2/3 along the line, offset opposite perpendicular
                cPoint2 = pt - (delta * 0.33) - (perpendicular * amplitude);

                Pen.curveTo(pt, cPoint1, cPoint2);
                // Pen.lineTo(pt)
            }
            { envCurves[i] > 0.0 } {
                Pen.lineTo(pt)
            }
            { envCurves[i] < 0.0 } {
                Pen.lineTo(pt)
            }
            { 
                // default is a straight line
                Pen.lineTo(pt)
            }
        };
        Pen.lineTo(Point(origin.width, zeroY));
        Pen.lineTo(Point(0, zeroY));
        Pen.fill;

        Pen.color = Color.red.alpha_(0.85);
        Pen.moveTo(pts[0]);
        pts[1..].do { |pt| Pen.lineTo(pt); };
        Pen.stroke;

        pts.do { |pt, i|
            var radius = if(selected == i) { 6 } { 4 };
            var value = envPoints[i][1];
            var label = value.round(0.001).asStringPrec(3);
            var labelPos, labelX, labelY;

            Pen.color = if(selected == i) { Color.white } { Color.red.alpha_(0.75) };
            Pen.fillOval(Rect.aboutPoint(pt, radius, radius));

            if((i != 0) and: { i != (pts.size - 1) }) {
                labelPos = Point(pt.x, pt.y);
                if(value >= 0) {
                    // positive values: above the point
                    labelY = pt.y - 20;
                    labelPos = Point(pt.x, labelY);
                    if(pt.y < 20) {
                        // keep clear of the top border
                        labelPos = Point(pt.x + 12, pt.y);
                    };
                } {
                    // negative values: below the point
                    labelY = pt.y + 8;
                    labelPos = Point(pt.x, labelY);
                    if(pt.y > (origin.height - 20)) {
                        // keep clear of the bottom border
                        labelPos = Point(pt.x + 12, pt.y - 12);
                    };
                };

                if(pt.x > (origin.width - 40)) {
                    // keep the label inside the right edge
                    labelPos = Point(pt.x - 27, labelPos.y);
                } {
                    if(value.abs > 0.95) {
                        // slight right shift near the extreme values
                        labelX = pt.x + 10;
                        labelPos = Point(labelX, labelPos.y);
                    };
                };

                Pen.color = Color.white;
                Pen.stringAtPoint(label, labelPos);
            };
        };
    };

    envView.mouseDownAction = { |vw, x, y, mod|
        var origin = vw.bounds.moveTo(0, 0);
        var pts = envPoints.collect { |pt|
            Point(
                pt[0].linlin(0.0, 1.0, 0, origin.width),
                pt[1].linlin(-1.0, 1.0, origin.height, 0, \none)
            )
        };

        selected = pts.detectIndex { |pt, idx|
            if((idx == 0) or: { idx == (pts.size - 1) }) {
                false
            } {
                ((pt.x - x).abs <= 8) and: { (pt.y - y).abs <= 8 }
            }
        } ?? { -1 };

        vw.refresh;
    };

    envView.mouseMoveAction = { |vw, x, y, mod|
        if(selected != -1) {
            var origin = vw.bounds.moveTo(0, 0);
            envPoints[selected][0] = x.linlin(0, origin.width, 0.0, 1.0).clip(0.0, 1.0).round(0.01);
            envPoints[selected][1] = y.linlin(0, origin.height, 1.0, -1.0).clip(-1.0, 1.0).round(0.01);
            if(selected == 0) {
                envPoints[0][0] = 0.0;
                envPoints[0][1] = 0.0;
            };
            if(selected == (envPoints.size - 1)) {
                envPoints[envPoints.size - 1][0] = 1.0;
                envPoints[envPoints.size - 1][1] = 0.0;
            };
            vw.refresh;
        };
    };

    envView.mouseUpAction = { |vw, x, y|
        selected = -1;
        vw.refresh;
    };

    envView;
}.value;

// Right panel editor controls.
{
    var knobWidth = 65;
    var knobHeight = 85;
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

    StaticText(paneRight, (paneRight.bounds.width - (gap*2))@24)
        .string_("Edit segment")
        .align_(\left)
        .stringColor_(Color.white)
        .font_(fontLabel);

    knob.(paneRight, "Level", [-1.0, 1.0].asSpec, { |ez| }, 0.0);
    knob.(paneRight, "Dur", [0.0, 2.0].asSpec, { |ez| }, 1.0);
    knob.(paneRight, "Slope", [-5.0, 5.0].asSpec, { |ez| }, 0.0);

    Button(paneRight, 30@15)
        .states_([
            ["sine", Color.white, Color.grey(0.5)]
        ])
        .font_(fontButton)
        .action_({ |bt| bt.value.postln });
}.value;

// Bottom panel controls: navigation buttons and edit buttons.
{
    var btnSize = 24;
    var btnWidth = 50;

    // Left-aligned navigation buttons: first, previous, next, last
    Button(paneBottom, btnSize@btnSize)
        .states_([["⏮", Color.white, Color.grey(0.5)]])
        .font_(fontButton)
        .action_({ |bt| "first".postln });

    Button(paneBottom, btnSize@btnSize)
        .states_([["◀", Color.white, Color.grey(0.5)]])
        .font_(fontButton)
        .action_({ |bt| "previous".postln });

    Button(paneBottom, btnSize@btnSize)
        .states_([["▶", Color.white, Color.grey(0.5)]])
        .font_(fontButton)
        .action_({ |bt| "next".postln });

    Button(paneBottom, btnSize@btnSize)
        .states_([["⏭", Color.white, Color.grey(0.5)]])
        .font_(fontButton)
        .action_({ |bt| "last".postln });

    // 
    makePanel.(paneBottom, (btnSize * 4) + (gap * 3) + margin, margin, btnSize * 3, btnSize, transparent);

    // Right-aligned edit buttons: add and delete
    Button(paneBottom, btnWidth@btnSize)
        .states_([["add", Color.white, Color.grey(0.5)]])
        .font_(fontButton)
        .action_({ |bt| "add".postln });

    Button(paneBottom, btnWidth@btnSize)
        .states_([["delete", Color.white, Color.grey(0.5)]])
        .font_(fontButton)
        .action_({ |bt| "delete".postln });
}.value;

win.front;

CmdPeriod.doOnce({
    win.close;
});
)