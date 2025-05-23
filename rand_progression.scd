// WARNING! THIS SOUNDS GREAT!
// c#3 MINOR
List[ ( 'inversion': 2, 'sequence': List[ 1, 4, 5, 7 ], 'scale': minor, 'tonic': 61 ) ]


(
var stIntervals;
var palette = QPalette.system;
var width=800;
var height=860;
var colorBg=Color.grey(0.15);

var getSequence = {
    var scrambledIntervals = (2..7).scramble;
    var intervals = List.new;

    // 52 is the lowest note in standard tuning (E2 - lowest open string),
    // 95 is the highest from which it's sensible to build a chord (B5 - fret 19 of the highest string)
    var note = rrand(52, 95);

    [0, 2, 4].do { |i| intervals.add(scrambledIntervals[i])};
    format(
        "% - %, inversion: %\n%\n",
        (Scale.chromatic.degrees + note).choose.midiname,
        ['minor', 'major'].choose,
        (0..2).choose,
        "Intervals: 1".ccatList(intervals)
    );
};

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

~getChord = { |root, chordName|
    var notes = List.new;
    ~chordStructure[chordName].do { |degree|
        notes.add((Scale.chromatic.degrees + root)[degree-1].midinote)
    };
    notes
};


~getSequence = {
    var degreesScrambled = (2..7).scramble;
    var sequence = List.new;

    // 52 is the lowest note in standard tuning (E2 - lowest open string),
    // 95 is the highest from which it's sensible to build a chord (B5 - fret 19 of the highest string)
    var note = rrand(52, 95);
    var scale;
    var inversion = (0..2).choose;

    [0, 2, 4].do { |i| sequence.add((2..7).scramble[i])};
    sequence.postln;
    scale = ['minor', 'major'].choose;
    (
        tonic: note,
        scale: scale,
        inversion: inversion,
        sequence: sequence
    )
};


// Main list for intervnals.
~sequences = List.new;


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
        var seq = ~getSequence.value;
        stIntervals.string = format(
            "% - %, inversion: %\n%\n",
            seq.tonic.midiname,
            seq.scale,
            seq.inversion,
            "sequence: 1".ccatList(seq.sequence)
        );
        ~sequences.add(seq);
        ~sequences.postln;
        // parent.bounds.postln;
    });

    var btHint = Button(paneL, hW.(paneL)@40)
    .states_([["Hint"]])
    .action_({ |bt|
        parent.bounds.postln;
    });

    var stIntervals = StaticText(paneL, paneL.bounds.width@(paneL.bounds.height-btRegenerate.bounds.height-20))
    .stringColor_(Color.white)
    .font_(Font("Monospace", 20))
    .align_(\center);
};


~win = Window("Chord sequence generator", Rect(0, 1200, width, height));
~win.view.decorator = FlowLayout(~win.view.bounds,5@5, 5@5);
palette.setColor(colorBg, \window);
~win.view.palette = palette;
~makeGUI.(~win);
~win.front;


CmdPeriod.doOnce({
    ~win.close;
});
)
