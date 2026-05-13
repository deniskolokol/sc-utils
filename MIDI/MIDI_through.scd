// MIDI Through Example
//
// Useful when:
// - You want to simply route MIDI from a MIDI keyboard connected via USB to
//   external synths connected via MIDI Out ports.
// - You want to use SuperCollider as a MIDI monitor to log incoming MIDI events.
// - You want to use SuperCollider for MIDI processing (e.g., arpeggiation,
//   generative patterns, MIDI effects) and send the output to an external synth.

(
// Connect MIDI keyboard.

// Start by booting the server
Server.default = s = Server.local;
s.boot;

s.waitForBoot({
    // Initialize the MIDI Client
    MIDIClient.init;

    // Look at the list of MIDI Destinations in the Post window.
    // You'll see something like:
    // MIDI Endpoint("Your MIDI Interface Name", "Port Name")
    MIDIClient.destinations.do(_.postln);

    // Create the MIDIOut object.
    // Replace the strings below with the exact names from your MIDI destination list.
    // For example:
    // ~ifaceMIDIOut = MIDIOut.newByName("Scarlett 2i2 MIDI", "Scarlett 2i2 MIDI Port");
    ~ifaceMIDIOut = MIDIOut.newByName("Studio 1824c", "Studio 1824c");

    // Optional: Set latency to match your server for better sync
    ~ifaceMIDIOut.latency = Server.default.latency;

    // ...or adjust as needed
    ~ifaceMIDIOut.latency = 0.1;

    MIDIIn.connectAll;

    // DIctionary to store active notes
    ~notes = Dictionary.new;


    // Log MIDI event
    ~postMIDI = { |val, num, chan, src, setParm, setVal|
        var ln = format("src  %\tchan  %\tnum  % ", src, chan, num);
        if (setParm.isNil) {
            ln = ln + format(" | val: %", val);
        } {
            if (setVal.isFloat) { setVal = setVal.round(0.0001) };
            ln = ln + format(" | val: % -> %  (%)", val, setVal, setParm);
        };
        postf("%\n", ln)
    };


    // MIDI Note On handler function
    MIDIdef.noteOn(\noteOn, { |vel, num, chan, src|
        switch(chan)

        // Channel 0 is a keyboard
        { 0 } {
            // Send a NoteOn message
            ~ifaceMIDIOut.noteOn(0, num, vel); // (channel, note, velocity)

            // Store the synth in the dictionary with the note number as key
            ~notes[num] = vel;
        }

        // Channel 9 is a keypad
        { 9 } { nil }

        // Default function
        { nil };

        // Post MIDI event
        ~postMIDI.(vel, num, chan, src, "Note ON", num);
    },
    chan: 0
    );

    // MIDI Note Off handler function
    MIDIdef.noteOff(\noteOff, { |vel, num, chan, src|
        ~ifaceMIDIOut.noteOff(0, num, 0); // (channel, note, velocity)

        // Remove from tracking dictionary
        ~notes.removeAt(num);
    },
    chan: 0
    );
});

CmdPeriod.doOnce({
    "Clean up on exit...".post;
    ~notes = nil;
    MIDIIn.disconnectAll;
    " done".postln;
});
)