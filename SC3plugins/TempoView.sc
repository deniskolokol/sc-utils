// Place this file in your SuperCollider Extensions folder and run 
// `thisProcess.recompile` to use the TempoView class.

// TempoView is a visual representation of a TempoClock's current bar and beat,
// along with the ability to set accents on specific beats and clicks.
// It provides a clear visual feedback of the tempo and rhythm structure,
// making it easier for musicians and producers to understand and interact
// with their tempo settings in real-time.

// It does require a running TempoClock to function, and it will display the
// current bar and beat, as well as the structure of beats and clicks, with
// accents visually distinguished.

// Warning: work-in-prpogress! This implementation is a basic visualization and
// may not be optimized for performance with very high tempo settings or complex
// rhythms.

TempoView {
    var <>accents, <view;
    var clock;
    classvar <fontLabel, <colorBg, <colors, <margin, <gap;

    *initClass {
        fontLabel = Font("Menlo", 12);
        colorBg = Color.new255(24, 24, 24);
        colors = (
            \beat: Color.gray(0.2),
            \click: Color.gray(0.6),
            \accent: Color.gray(0.8)
        );
        margin = 5;
        gap = 5;
    }

    *new { arg parent, tempoClock, clicks, accents, animate = true;
        ^super.new.init(parent, tempoClock, clicks, accents, animate)
    }

    init { arg parent, tempoClock, clicks, accents, animate = true;
        clock = tempoClock;
        accents = accents ? Array.fill(clock.beatsPerBar, { Array.fill(clicks, 0) });
        view = UserView(parent, Rect(0, 0, parent.bounds.width, parent.bounds.height))
            .background_(Color.clear)
            .drawFunc_({ |usrView|
                var left, top, width, height;
                var widthBeat, localRect;
                var currColor = colorBg;

                var getLocalRect = { |left, top, height, width|
                    var localLeft, localTop, localWidth, localHeight;

                    localHeight = height - (gap * 2);
                    localWidth = width - (gap * 2);
                    localHeight = min(localHeight, localWidth); // Make it a square
                    localWidth = localHeight; // Make it a square
                    localLeft = left + ((width - localWidth - gap) / 2); // Center horizontally
                    localTop = top + ((height - (gap * 2) - localHeight) / 2); // Center vertically

                    Rect(localLeft, localTop, localWidth, localHeight)
                };

                Pen.font = fontLabel;
                if (clock.isRunning) {
                    widthBeat = ((usrView.bounds.width - (margin * 2)) / clock.beatsPerBar) - (gap * 2);
                    width = widthBeat / clicks - gap;
                    height = usrView.bounds.height - 20 - (margin * 4);

                    // Visualize the current bar and beat
                    Pen.fillColor = colors[\accent];
                    Pen.stringAtPoint(
                        format(
                            "Bar: %, Beat: %",
                            clock.bar,
                            clock.beatInBar.round(0.001) + 1
                        ),
                        Point(80 + margin*4, margin)
                    );

                    Pen.strokeColor_(Color.gray(alpha: 0.5));
                    clock.beatsPerBar.do { |beat|
                        left = margin + (beat * (widthBeat + (2 * gap)));
                        top = margin + 20; // Leave space for the label                        

                        // Draw the beat rectangles.
                        Pen.width = 1;
                        Pen.strokeColor_(colors[\beat]);
                        Pen.strokeRect(Rect(left, top, widthBeat, height));

                        // Draw the click circles within each beat.
                        Pen.strokeColor_(colors[\click]);
                        Pen.width = 4;

                        clicks.do { |click|
                            left = margin + (beat * (widthBeat + (2 * gap))) + (click * width) + (gap * (click + 1));
                            top = margin + 20 + gap;

                            localRect = getLocalRect.(left, top, height, width);

                            if (beat > clock.beatInBar.floor or:
                                    (beat == clock.beatInBar.floor and: 
                                        (click > ((clock.beatInBar - clock.beatInBar.floor) * clicks).floor)
                                    )
                                ) {
                                Pen.addOval(localRect);
                                Pen.stroke;
                            } {
                                if (accents[beat][click] == 1) {
                                    currColor = Color.gray((beat+1)/(clock.beatInBar+1));
                                    Pen.fillColor = currColor;
                                    Pen.addOval(localRect);
                                } {
                                    // Non-accented clicks are dimmer
                                    currColor.alpha_(0.2);
                                    Pen.fillColor = currColor;
                                    Pen.addOval(localRect);
                                };
                                Pen.fillStroke;
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
        view.animate = animate;
        this.prAddAnimateControls;
    }

    prAddAnimateControls {
        var labelRect, buttonRect, button;

        labelRect = Rect(12, 4, 60, 18);
        buttonRect = Rect(labelRect.right + 6, 4, 56, 18);

        StaticText(view, labelRect)
            .string_("Animate")
            .font_(fontLabel)
            .align_(\left);

        button = Button(view, buttonRect)
            .states_([
                ["OFF", Color.white, Color.gray(0.25)],
                ["ON", Color.black, Color.green(0.6)]
            ])
            .value_(view.animate.binaryValue)
            .action_({ |btn|
                view.animate = (btn.value == 1);
            });

        ^button;
    }

    stop {
        if (clock.isRunning) {
            clock.stop;
        };
        clock.clear;
        clock = nil;
    }

    remove {
        view.remove
    }
}