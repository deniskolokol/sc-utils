// MasterEQ-inspired custom editor sketch using Bézier curves
(
{
    // Functions
    var bezierConcave = { |pointFro, pointTo, ampScale|
        // Calculate the vector from start to end point
        var delta = pointTo - pointFro;

        // Calculate the magnitude of the vector
        var deltaLen = delta.dist(Point(0, 0));

        // Calculate perpendicular vector (rotated 90 degrees) and normalize it
        var perpendicular = Point(delta.y.neg / deltaLen, delta.x / deltaLen);

        // Define wave amplitude (height of the curve)
        var amplitude = deltaLen * 0.03; // Adjust multiplier to change curve height

        // Calculate and return control points for power curve
        var ctlPt1 = pointFro - (delta / ampScale) - (perpendicular * amplitude * ampScale);
        var ctlPt2 = pointTo + (delta / ampScale) - (perpendicular * amplitude * ampScale);
        
        [ctlPt1, ctlPt2]
    };

    var getBezierCtrlPoint = { |pointFro, pointTo, ampScale, time|
        var midPoint, direction, perpendicular, controlPoint, totalDistance;
        var isConvex = true;
        var curveDirectionModifier;

        // 1. Calculate linear distance between endpoints
        totalDistance = pointFro.dist(pointTo);

        // 2. Shift baseline anchor position based on 'time'
        midPoint = pointFro.blend(pointTo, time);

        // 3. Get the baseline direction vector
        direction = pointTo - pointFro;

        // 4. Base perpendicular vector (oriented for screen inverted-Y space)
        perpendicular = Point(direction.y.neg, direction.x);

        // 5. Normalize vector safely
        if (totalDistance > 0) {
            perpendicular = perpendicular / totalDistance;
        };

        // 6. Apply skew calculation to maintain the asymmetric peak
        if (time != 0.5) {
            var skewDirection = (pointFro - midPoint);
            if (skewDirection.rho > 0) {
                perpendicular = perpendicular + (skewDirection / totalDistance * 0.5);
            };
        };

        // 7. Explicitly evaluate convexity rules using robust method-style .if calls
        // (Inverted Y: "above" means smaller Y value)
        isConvex = (ampScale < 0).if({
            pointFro.y > pointTo.y
        }, {
            pointFro.y < pointTo.y
        });

        // 8. Determine directional multiplier (convex = 1, concave = -1)
        curveDirectionModifier = isConvex.if({ 1 }, { -1 });

        // 9. Calculate final control point using absolute amplitude scale
        controlPoint = midPoint + (perpendicular * (ampScale * 0.33).abs * curveDirectionModifier * (totalDistance * 0.25));

        // 10. Force return of the Point instance to avoid nil bugs downstream
        controlPoint;
    };


    var selected = -1;
    var envPoints;
    var makeEnvPoints = { |levels, times|
        var totalTime = times.sum;
        levels.collect { |level, i|
            var x = if(i == 0) { 0.0 } { times[0..(i - 1)].sum / totalTime };
            [x, level.clip(-1.0, 1.0)]
        }
    };

    envPoints = makeEnvPoints.(envLevels, envTimes);

    envView = UserView(paneMain, paneMain.bounds.insetBy(margin, margin))
        .resize_(5)
        .focusColor_(Color.clear);

    envView.drawFunc = { |vw|
        var origin = vw.bounds.moveTo(0, 0);
        var zeroY = origin.height * 0.5;
        var minVal = -1.0;
        var maxVal = 1.0;
        var step = 0.25;
        var pts;
        var prevPt;
        var delta, deltaLen;
        var perpendicular;
        var amplitude;
        var amplitudeScale;
        var controlPointRatio;
        var cPoint;
        var cPoint1, cPoint2;

        envPoints[0][1] = 0.0;
        envPoints[envPoints.size - 1][1] = 0.0;
        envPoints[0][0] = 0.0;
        envPoints[envPoints.size - 1][0] = 1.0;
        pts = envPoints.collect { |pt|
            Point(
                pt[0].linlin(0.0, 1.0, 0, origin.width),
                pt[1].linlin(-1.0, 1.0, origin.height, 0, \none)
            )
        };

        Pen.color = Color.grey(0.1);
        Pen.fillRect(origin);

        Pen.color = Color.grey(0.35).alpha_(0.35);
        ((maxVal - minVal) / step + 1).asInteger.do { |i|
            var v = minVal + (i * step);
            var y = v.linlin(minVal, maxVal, origin.height, 0, \none);
            Pen.line(0@y, origin.width@y);
            Pen.stringAtPoint(v.asStringPrec(2), Point(4, y + 2));
        };
        Pen.stroke;

        Pen.color = Color.white.alpha_(0.75);
        Pen.line(0@zeroY, origin.width@zeroY);
        Pen.stroke;

        Pen.color = Color.red.alpha_(0.25);
        Pen.moveTo(pts[0]);
        pts[1..].do { |pt, i|
            prevPt = pts[i];
            case
            { envCurves[i] == \sine } {
                // Calculate the vector from start to end point
                delta = pt - prevPt;

                // Calculate perpendicular vector (rotated 90 degrees) and normalize it
                deltaLen = delta.dist(Point(0, 0));
                perpendicular = Point(delta.y.neg / deltaLen, delta.x / deltaLen);

                // Define wave amplitude (height of the sine wave)
                amplitude = deltaLen * 0.1; // Adjust multiplier to change wave height
                amplitudeScale = perpendicular * amplitude;

                // If sine crosses the X-axis, change amplitudeScale sign to preserve the wave direction
                if ((pt.y - prevPt.y) > 0) { amplitudeScale = amplitudeScale.neg };

                // Calculate control points
                cPoint1 = prevPt + (delta * 0.33) + amplitudeScale;

                // Control point 2: 2/3 along the line, offset opposite perpendicular
                cPoint2 = pt - (delta * 0.33) - amplitudeScale;

                Pen.curveTo(pt, cPoint1, cPoint2);
            }
            { envCurves[i] != 0.0 } {
                cPoint = getBezierCtrlPoint.(prevPt, pt, envCurves[i], 0.9);
                Pen.quadCurveTo(pt, cPoint);
            }
            { 
                // default is a straight line
                Pen.lineTo(pt)
            }
        };
        Pen.lineTo(Point(origin.width, zeroY));
        Pen.lineTo(Point(0, zeroY));
        Pen.fill;

        // WARNING: temporary! Remove after testing curves
        Pen.color = Color.white;
        Pen.fillOval(Rect.aboutPoint(cPoint, 5, 5));

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
                    if(pt.y > (origin.height - 20)) {
                        // keep clear of the bottom border
                        labelPos = Point(pt.x + 12, pt.y - 12);
                    };
                };

                if(pt.x > (origin.width - 40)) {
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
        var origin = vw.bounds.moveTo(0, 0);
        var pts = envPoints.collect { |pt|
            Point(
                pt[0].linlin(0.0, 1.0, 0, origin.width),
                pt[1].linlin(-1.0, 1.0, origin.height, 0, \none)
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
            var origin = vw.bounds.moveTo(0, 0);
            envPoints[selected][0] = x.linlin(0, origin.width, 0.0, 1.0).clip(0.0, 1.0).round(0.01);
            envPoints[selected][1] = y.linlin(0, origin.height, 1.0, -1.0).clip(-1.0, 1.0).round(0.01);
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
)