(
var scrambledIntervals = (2..7).scramble;
var intervals = List[1];
var note;
[0, 2, 4].do { |i| intervals.add(scrambledIntervals[i])};

// 52 is the lowest note in standard tuning (E2 - lowest open string),
// 95 is the highest from which it's sensible to build a chord (B5 - fret 19 of the highest string)
note = rrand(52, 95);
postf(
    "% - %, inversion: %\nintervals: %\n",
    (Scale.chromatic.degrees + note).choose.midiname,
    ['minor', 'major'].choose,
    (0..2).choose,
    intervals
)
)