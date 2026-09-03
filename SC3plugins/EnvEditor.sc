EnvManager {
    var <>levels, <>times, <>curve;
    var envelope, envelopeOrig;

    *new { arg levels, times, curve;
        ^super.new.init(levels, times, curve)
    }

    init { arg levels, times, curve;
        levels = levels ? [0.0, 1.0];
        times = times ? [1.0];
        curve = curve ? [\linear];

        envelope = this.makeEnvelope(levels, times, curve);

        // Original envelope is used to reset the envelope to its initial state.
        envelopeOrig = envelope.copy;
    }

    makeEnvelope { arg levels, times, curve;
        ^Env(levels, times, curve)
    }

    reset { envelope = envelopeOrig }

    insertSegment { arg index, level = 1.0, time = 1.0, curve = \linear;
        envelope.levels.insert(index+1, level);
        envelope.times.insert(index, time);
        envelope.curve.insert(index, curve);
        envelope = this.makeEnvelope(envelope.levels, envelope.times, envelope.curve);
    }

    removeSegment { arg index;
        if (envelope.levels.size > 2) {
            envelope.levels.removeAt(index+1);
            envelope.times.removeAt(index);
            envelope.curve.removeAt(index);
            envelope = this.makeEnvelope(envelope.levels, envelope.times, envelope.curve);
        } {
            "Cannot remove segment. Envelope must have at least 2 levels.".warn;
        }
    }

    getLevels { ^envelope.levels }
    getTimes { ^envelope.times }
    getCurves { ^envelope.curve }
}

// bookmark
EnvEditor {
    var <>envManager, <>view;
    var <parentView;

    *new { arg parent, envManager;
        ^super.new.init(parent, envManager)
    }

    init { arg parent, envManager;
        parentView = parent;
        envManager = envManager;

        view = UserView(parent, Rect(0, 0, parent.bounds.width, parent.bounds.height))
            .background_(Color.clear)
            .drawFunc_({ |usrView|
                // Drawing code for the envelope editor will go here.
            });
    }
}