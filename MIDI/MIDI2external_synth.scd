// This is an example of how to send MIDI messages from SuperCollider to an
// external synthesizer like the Behringer Neutron.

// Initialize the MIDI Client
MIDIClient.init;

// Look at the list of MIDI Destinations in the Post window.
// You'll see something like:
// MIDI Endpoint("Your MIDI Interface Name", "Port Name")
MIDIClient.destinations.do(_.postln);

// Create the MIDIOut object.
// Replace the strings below with the exact names from your MIDI destination list.
// For example: ~ifaceMIDIOut = MIDIOut.newByName("Scarlett 2i2 MIDI", "Scarlett 2i2 MIDI Port");
~ifaceMIDIOut = MIDIOut.newByName("Studio 1824c", "Studio 1824c");

// Optional: Set latency to match your server for better sync
~ifaceMIDIOut.latency = Server.default.latency;

// Option 1:
// ---------
// Define a MIDI pattern to send note events to the Neutron. In this scenario
// only NoteOn is used.
(
p = Pbind(
    // --- MIDI Specific Keys ---
    \type, \midi,          // This is a MIDI event
    \midiout, ~ifaceMIDIOut, // Send it to your Neutron
    \midicmd, \noteOn,     // The command is a note (default)
    \chan, 0,              // MIDI Channel (0 is channel 1 for Neutron)

    // --- Musical Content ---
    // Scale and degree for melodic patterns (converted automatically to MIDI notes)
    // \scale, Scale.minor,
    // \degree, Pseq([0, 2, 4, 5, 7, 9, 11, 12], inf),

    // Or, use \midinote directly for specific note numbers (e.g., 60 = Middle C)
    \midinote, Pseq([71, 70, 67, 64, Rest(), Rest(), 67, 66, 64, 60, Rest(), Rest()], inf),

    // --- Performance Parameters ---
    \dur, 0.25, // Duration between notes (quarter notes)
    \legato, 0.5, // Note length (0.5 = staccato, 1 = full duration)
    \amp, Pwhite(0.8, 1, inf) // Velocity (MIDI velocity mapped from amp)
).play;
)

// Stop the pattern
// Warning: do not press Cmd+. - this will stop the entire server and all MIDI
// communication, rendering the last note played indefinitely. Use the line
// below instead.
p.stop;



// Option 2:
// ---------
// Send MIDI messages directly without using a pattern. Both NoteOn and NoteOff
// messages are sent manually.
(
// Send a NoteOn message (e.g., Middle C with velocity 100)
~ifaceMIDIOut.noteOn(0, 40, 100); // (channel, note, velocity)
// Wait for a duration (e.g., 1 second) and then send a NoteOff message
~rout = Routine({
    1.5.wait; // Wait for 1 second
    ~ifaceMIDIOut.noteOff(0, 40, 0); // (channel, note, velocity)
}).play;
)

~rout.stop; // Stop the routine if needed


// Panic.
~ifaceMIDIOut.allNotesOff(0)