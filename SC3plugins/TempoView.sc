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

    *new { arg parent, tempoClock, clicks, accents;
        ^super.new.init(parent, tempoClock, clicks, accents)
    }

    init { arg parent, tempoClock, clicks, accents;
        clock = tempoClock;
        accents = accents ? Array.fill(clock.beatsPerBar, { Array.fill(clicks, 0) });
        view = UserView(parent, Rect(0, 0, parent.bounds.width, parent.bounds.height))
            .background_(Color.clear)
            .drawFunc_({ |usrView|
                var left, top, width, height;
                var margin = 5, gap = 5;
                var widthBeat, localRect;
                var colorBg = Color.new255(24, 24, 24);
                var currColor = colorBg;
                var fontLabel = Font("Menlo", 12);
                var palette = QPalette.dark;
                var colors = (
                    \beat: Color.gray(0.2),
                    \click: Color.gray(0.6),
                    \accent: Color.gray(0.8)
                );

                var getLocalRect = { |left, top, height, width, gap|
                    var localLeft, localTop, localWidth, localHeight;

                    localHeight = height - (gap * 2);
                    localWidth = width - gap;
                    localHeight = min(localHeight, localWidth); // Make it a square
                    localWidth = localHeight; // Make it a square
                    localLeft = left + ((width - localWidth) / 2); // Center horizontally
                    localTop = top + ((height - (gap * 2) - localHeight) / 2); // Center vertically

                    Rect(localLeft, localTop, localWidth, localHeight)
                };

                Pen.font = fontLabel;
                if (clock.isRunning) {
                    widthBeat = ((usrView.bounds.width - (margin * 2)) / clock.beatsPerBar) - (gap * 2);
                    width = widthBeat / clicks - gap;
                    height = usrView.bounds.height - 20 - (margin * 4);

                    Pen.fillColor = colors[\accent];
                    Pen.stringAtPoint(
                        format("Bar: %, Beat: %", clock.bar, clock.beatInBar),
                        Point(margin*4, margin)
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

                            localRect = getLocalRect.(left, top, height, width, gap);

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
        view.animate = true;
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