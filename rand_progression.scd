// WARNING! THIS SOUNDS GREAT!
// c#3 MINOR
List[ ( 'inversion': 2, 'sequence': List[ 1, 4, 5, 7 ], 'scale': minor, 'tonic': 61 ) ]


(
var stIntervals;
var palette = QPalette.system;
var width=800;
var height=860;
var colorBg=Color.grey(0.15);

// Chord structure based on chromatic score.
~chordStructure = (
    major: #[1, 5, 8],
    minor: #[1, 4, 8],
    diminished: #[1, 4, 7],
    dominant_7: #[1, 5, 8, 11],
    minor_7: #[1, 4, 8, 11],
    major_7: #[1, 5, 8, 12],
    major_6: #[1, 5, 8, 10],
    minor_6: #[1, 4, 8, 10],
    diminished_7: #[1, 4, 7, 10],
    half_diminished: #[1, 4, 7, 11],
    augmented_7: #[1, 5, 9, 11]
);

// Major and minor scales for chord generation
~chordsMajorScale = List[\major, \minor, \minor, \major, \major, \minor, \diminished];
// Minor scale is a rotation of the major scale
~chordsMinorScale = ~chordsMajorScale.rotate(2);

// Basic cadence degrees for chord generation
~baseCadence = #[0, 2, 4];
~baseChordDegrees = #[0, 2, 4];
// The list of all generated sequences in a session
~sequences = List.new;

~getChord = {
    // Generate a chord based on the tonic note and chord type.
    arg
    tonic, // tonic note, <Integer>
    chordType; // chord type, <Symbol>
    // return:
    // MIDI note numbers, <List>

    var scale = Scale.at(chordType);
    var degrees = scale.degrees[~baseCadence];

    degrees
};


~getSequence = {
    // Generate a sequence of chord degrees based on a random tonic note and
    // chord type.
    arg
    tonic, // tonic note, <Integer>
    chordType; // chord type, <Symbol>
    // return:
    // sequence with tonic, chord degrees and inversion, <List> of <Dictionary>

    var degreesScrambled = (1..6).scramble;
    var sequence = List.new;
    var scale;
    var result = List.new;
    var inversion = (0..2).choose; // random inversion, but the same for all
                                   // chords in the sequence

    var fillChord = { |num, tonic, scale, inversion|
        // Calculate chord root, type, degrees and MIDI notes
        var chordRoot, chordType, chordDegrees, chordMidi, chordScale;

        chordRoot = num + tonic;
        chordType = scale[num];
        chordScale = Scale.at(chordType);
        chordDegrees = chordScale.degrees[~baseChordDegrees];
        chordMidi = chordDegrees + chordRoot;
        
        (
            num: num,
            root: chordRoot,
            tonic: chordRoot.midinote,
            type: chordType,
            degrees: chordDegrees,
            midinotes: chordMidi,
            intervals: chordMidi.midinote,
            inversion: inversion
        )
    };

    // 52 is the lowest note in standard tuning (E2 - lowest open string),
    // 95 is the highest from which it's sensible to build a chord (B5 - fret
    // 19 of the highest string)
    tonic = tonic ?? rrand(52, 95); // default tonic note
    chordType = chordType ?? [\major, \minor].choose; // default chord type
    scale = if (chordType == \major) {~chordsMajorScale} {~chordsMinorScale};

    // The first chord is the root
    result.add(fillChord.(0, tonic, scale, inversion));

    // Fill the cadence with chord degrees, tonic and inversion
    ~baseCadence.do { |i|
        result.add(fillChord.(degreesScrambled[i], tonic, scale, inversion));
    };

    result;
};

// GUI
~makeGUI = { |parent|
    var hW = { |par| (par.bounds.width*0.49).floor }; // half width
    var fH = { |par| par.bounds.height.floor-10 }; // full height
    var panFl = { |par| FlowView(par, hW.(par)@fH.(par), 5@5, 5@5).background_(Color.gray(alpha:0.1))};
    var paneL = panFl.(parent);
    var paneR = panFl.(parent);

    var btRegenerate = Button(paneL, (hW.(paneL))@40)
    .states_([["Generate"]])
    .action_({ |bt|
        ~seq = ~getSequence.(~tonic, ~chordType);
        ~rootChord = ~seq[0];
        stHint.string = "";
        stIntervals.string = format(
            "% - %, inversion: %\n%\n",
            ~rootChord.tonic.toUpper,
            ~rootChord.type,
            ~rootChord.inversion,
            "sequence:".scatList((all {: x[\num], x <- ~seq}) + 1)
        );
        ~sequences.add(~seq);

        // TODO: update ~tonic and ~chordType - take them from _some_? note of last chord in the ~seq
        ~tonic = ~seq.last.root;
        ~chordType = ~seq.last.type;
    });

    var btHint = Button(paneL, hW.(paneL)@40)
    .states_([["Hint"]])
    .action_({ |bt|
        var hint = "sequence:\n";
        ~seq.do { |chord|
            hint = hint.scatList(
                format("  % - %: %\n",
                    chord.tonic.toUpper, chord.type, chord.intervals
                )
            );
        };
        stHint.string = hint;
    });

    var stIntervals = StaticText(
        paneL,
        paneL.bounds.width@(paneL.bounds.height-btRegenerate.bounds.height-500)
    )
    .stringColor_(Color.white)
    .font_(Font("Monospace", 20))
    .align_(\center);

    var stHint = StaticText(paneL, paneL.bounds.width@490)
    .stringColor_(Color.white)
    .font_(Font("Monospace", 18))
    .align_(\left);
};


~win = Window("Chord sequence generator", Rect(0, 1200, width, height), resizable: false);
~win.view.decorator = FlowLayout(~win.view.bounds,5@5, 5@5);
palette.setColor(colorBg, \window);
~win.view.palette = palette;
~makeGUI.(~win);
~win.front;


CmdPeriod.doOnce({
    ~win.close;
});
)
