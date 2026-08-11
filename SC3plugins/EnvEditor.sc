(
var win;
var width = 800;
var height = 400;

var gap = 5, margin = 5;
var paneTop, paneMain, paneRight, paneBottom;
var envView;

var colorBg = Color.grey(0.15);
var colorPane = Color.grey(0.2);
var transparent = Color.grey(alpha:0.0);

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
    var topH = (stackH * 0.1).floor;
    var mainH = (stackH * 0.8).floor;
    var bottomH = stackH - topH - mainH;
    var mainW = ((innerW - gap) * 0.8).floor;
    var rightW = (innerW - mainW - gap).floor;

    paneTop = makePanel.(win, margin, margin, innerW, topH, colorPane);
    paneMain = makePanel.(win, margin, paneTop.bounds.bottom + gap, mainW, mainH, colorPane);
    paneRight = makePanel.(win, paneMain.bounds.right + gap, paneMain.bounds.top, rightW, mainH, colorPane);
    paneBottom = makePanel.(win, margin, paneMain.bounds.bottom + gap, innerW, bottomH, colorPane);
}.value;

// Top panel controls.
EZPopUpMenu(paneTop,
    Rect(margin, margin, paneTop.bounds.width * 0.35, 28),
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
    var envPoints = [
        [0.0, 0.0],
        [0.2, 0.85],
        [0.45, -0.35],
        [0.7, 0.5],
        [1.0, 0.0]
    ];

    envView = UserView(paneMain, paneMain.bounds.insetBy(margin, margin))
        .resize_(5)
        .focusColor_(Color.clear);

    envView.drawFunc = { |vw|
        var b = vw.bounds.moveTo(0, 0);
        var zeroY = b.height * 0.5;
        var minVal = -1.0;
        var maxVal = 1.0;
        var step = 0.25;
        var pts;
        envPoints[0][1] = 0.0;
        envPoints[envPoints.size - 1][1] = 0.0;
        envPoints[0][0] = 0.0;
        envPoints[envPoints.size - 1][0] = 1.0;
        pts = envPoints.collect { |pt|
            Point(
                pt[0].linlin(0.0, 1.0, 0, b.width),
                pt[1].linlin(-1.0, 1.0, b.height, 0, \none)
            )
        };

        Pen.color = Color.grey(0.1);
        Pen.fillRect(b);

        Pen.color = Color.grey(0.35).alpha_(0.35);
        ((maxVal - minVal) / step + 1).asInteger.do { |i|
            var v = minVal + (i * step);
            var y = v.linlin(minVal, maxVal, b.height, 0, \none);
            Pen.line(0@y, b.width@y);
            Pen.stringAtPoint(v.asStringPrec(2), Point(4, y + 2));
        };
        Pen.stroke;

        Pen.color = Color.white.alpha_(0.75);
        Pen.line(0@zeroY, b.width@zeroY);
        Pen.stroke;

        Pen.color = Color.red.alpha_(0.25);
        Pen.moveTo(Point(0, zeroY));
        Pen.lineTo(pts[0]);
        pts[1..].do { |pt| Pen.lineTo(pt); };
        Pen.lineTo(Point(b.width, zeroY));
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
                    if(pt.y > (b.height - 20)) {
                        // keep clear of the bottom border
                        labelPos = Point(pt.x + 12, pt.y - 12);
                    };
                };

                if(pt.x > (b.width - 40)) {
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
        var b = vw.bounds.moveTo(0, 0);
        var pts = envPoints.collect { |pt|
            Point(
                pt[0].linlin(0.0, 1.0, 0, b.width),
                pt[1].linlin(-1.0, 1.0, b.height, 0, \none)
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
            var b = vw.bounds.moveTo(0, 0);
            envPoints[selected][0] = x.linlin(0, b.width, 0.0, 1.0).clip(0.0, 1.0).round(0.01);
            envPoints[selected][1] = y.linlin(0, b.height, 1.0, -1.0).clip(-1.0, 1.0).round(0.01);
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

// Right/bottom placeholders remain as visual separators.
StaticText(paneRight, paneRight.bounds.insetBy(margin, margin)).string_("RIGHT").align_(\center).stringColor_(Color.white);
StaticText(paneBottom, paneBottom.bounds.insetBy(margin, margin)).string_("BOTTOM").align_(\center).stringColor_(Color.white);

win.front;

CmdPeriod.doOnce({
    win.close;
});
)