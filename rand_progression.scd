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

// Start from random tonic note and chord type
// 52 is the lowest note in standard tuning (E2 - lowest open string),
// 95 is the highest from which it's sensible to build a chord (B5 - fret
// 19 of the highest string)
~lowest = 52;
~highest = 95;

// Random tonic note in the range of standard tuning
~tonic = rrand(~lowest, ~highest);
~chordType = [\major, \minor].choose;


~getSequence = {
    // Generates a sequence of chord degrees based on a random tonic note and
    // chord type.
    // Returns: sequence with tonic, chord degrees and inversion,
    // <List> of <Event>
    arg
    tonic, // tonic note, <Integer>
    chordType; // chord type, <Symbol>

    var degreesScrambled = (1..6).scramble;
    var sequence = List.new;
    var chords;
    var result = List.new;
    var inversion = (0..2).choose; // random inversion, but the same for all
                                   // chords in the sequence
    var fillChord = { 
        // Calculates chord root, type, degrees and MIDI notes.
        // Returns: <Event> with chord information
        arg
        num, // chord number in the sequence, <Int>
        tonic, // tonic of the whole sequence, <Str>
        root, // current chord root MIDI, <Int>
        chords, // chord types in scale, <Symbol>
        inversion; // inversion, <Int>

        var chordType = chords[num];
        var chordScale = Scale.at(chordType);
        var chordDegrees = chordScale.degrees[~baseChordDegrees];
        var chordMidi = chordDegrees + root;
        (
            num: num,
            root: root,
            tonic: root.midinote,
            type: chordType,
            degrees: chordDegrees,
            midinotes: chordMidi,
            intervals: chordMidi.midinote,
            inversion: inversion
        )
    };

    // Chord types in scale
    chords = if (chordType == \major) {~chordsMajorScale} {~chordsMinorScale};

    // The first chord is the root
    result.add(fillChord.(0, tonic, tonic, chords, inversion));

    // Fill the cadence with chord degrees, tonic and inversion
    ~baseCadence.do { |i|
        var root = tonic + Scale.at(chordType).degrees[degreesScrambled[i]];
        result.add(fillChord.(degreesScrambled[i], tonic, root, chords, inversion));
    };

    result;
};


~formatNote = { |note| // <String>
    note.replace(" ", "").toUpper
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
        var tonicIndex;

        ~seq = ~getSequence.(~tonic, ~chordType);
        ~rootChord = ~seq[0];
        stHint.string = "";
        stIntervals.string = format(
            "% - %, inversion: %\n%\n",
            ~formatNote.(~rootChord.tonic),
            ~rootChord.type,
            ~rootChord.inversion,
            "sequence:".scatList((all {: x[\num], x <- ~seq}) + 1)
        );
        ~sequences.add(~seq);

        // Update ~tonic and ~chordType - take them from the last chord
        // in the current sequence, but change the octave if necessary.
        ~chordType = ~seq.last.type;
        ~tonic = ~seq.last.root;

        // Obtain the index of ~tonic in the chromatic scale (0 = C, 1 = C#, ..., 11 = B)
        tonicIndex = ~seq.last.root % 12;

        // New ~tonic is a MIDI note number for the tonic class in a random
        // octave between 2 and 5
        ~tonic = tonicIndex + (rrand(2, 5) * 12);

        // Ensure the tonic is within the range of standard tuning
        while {~tonic < ~lowest} {~tonic = ~tonic + 12};
        while {~tonic > ~highest} {~tonic = ~tonic - 12};
    });

    var btHint = Button(paneL, hW.(paneL)@40)
    .states_([["Hint"]])
    .action_({ |bt|
        var hint = "sequence:\n\n";
        ~seq.do { |chord|
            hint = hint + format("%\n% - % (%)\n\n",
                chord.num + 1,
                ~formatNote.(chord.tonic),
                chord.type,
                (all {: ~formatNote.(x), x <- chord.intervals}).join(" ")
            );¡
        };
        stHint.string = hint;
    });

    var stIntervals = StaticText(
        paneL,
        paneL.bounds.width@(paneL.bounds.height-btRegenerate.bounds.height-700)
    )
    .stringColor_(Color.white)
    .align_(\center)
    .font_(Font("Monaco", 24));

    var stHint = StaticText(paneL, paneL.bounds.width@690)
    .stringColor_(Color.gray(0.6))
    .align_(\left)
    .font_(Font("Monaco", 20));
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
