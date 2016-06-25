// Skeleton visualizer
//$cd ~/Documents/OpenNI/Sensebloom-OSCeleton-7307683/


(
var responderKinectNewUser, responderKinectLostUser, responderKinect, removeKinectResp;
var skel, skelDict= Dictionary.new;
var w, width, height, depth;
var margin= 0, limX=[0,1], limY=[0,1], limZ=[0,7];

// Initial values
depth= width= height= Window.screenBounds.height/2+160;


// OSC responders
responderKinectNewUser= OSCresponderNode(nil, '/new_skel', {|t, r, msg|
	msg.postln;
	}).add;

responderKinectLostUser= OSCresponderNode(nil, '/lost_skel', {|t, r, msg|
	msg.postln;
	}).add;

responderKinect= OSCresponderNode(nil, '/joint', {|t, r, msg|
	var skel=nil, joint, x, y, z;
//	msg.postln;
	#joint, x, y, z= [msg[1],
		msg[3].linlin(limX[0], limX[1], margin, width - margin),
		msg[4].linlin(limY[0], limY[1], margin, depth - margin),
		msg[5].linlin(limZ[0], limZ[1], margin, height - margin)
		];

	skel= skelDict.atFail(msg[2].asInteger, {Skeleton.new});
	case // Note the mirror!
		{joint == \head} {skel.head= [x, y, z]}
		{joint == \neck} {skel.neck= [x, y, z]}
		{joint == \torso} {skel.torso= [x, y, z]}
		{joint == \r_collar} {skel.lCollar= [x, y, z]}
		{joint == \r_shoulder} {skel.lShoulder= [x, y, z]}
		{joint == \r_elbow} {skel.lElbow= [x, y, z]}
		{joint == \r_wrist} {skel.lWrist= [x, y, z]}
		{joint == \r_hand} {skel.lHand= [x, y, z]}
		{joint == \r_finger} {skel.lFinger= [x, y, z]}
		{joint == \l_collar} {skel.rCollar= [x, y, z]}
		{joint == \l_shoulder} {skel.rShoulder= [x, y, z]}
		{joint == \l_elbow} {skel.rElbow= [x, y, z]}
		{joint == \l_wrist} {skel.rWrist= [x, y, z]}
		{joint == \l_hand} {skel.rHand= [x, y, z]}
		{joint == \l_finger} {skel.rFinger= [x, y, z]}
		{joint == \r_hip} {skel.lHip= [x, y, z]}
		{joint == \r_knee} {skel.lKnee= [x, y, z]}    
		{joint == \r_ankle} {skel.lAnkle= [x, y, z]}
		{joint == \r_foot} {skel.lFoot= [x, y, z]}
		{joint == \l_hip} {skel.rHip= [x, y, z]}
		{joint == \l_knee} {skel.rKnee= [x, y, z]}
		{joint == \l_ankle} {skel.rAnkle= [x, y, z]}
		{joint == \l_foot} {skel.rFoot= [x, y, z]};
	skel.update= Date.getDate.secStamp;
	skelDict.put(msg[2].asInteger, skel);
	}).add;

removeKinectResp= {
	responderKinectNewUser.remove;
	responderKinectLostUser.remove;
	responderKinect.remove;
	};

w= RedWindow("tracking", Rect(1155, -10, width, height)).background_(Color.black).front;
w.draw{
	// Draw the back wall.
	Pen.strokeColor= Color.grey(0.5);
	Pen.strokeRect(
		Rect.aboutRedVector3D(
			RedVector[width*0.5, height*0.5, depth], width*0.5, width, height, depth
			)
		);
	if(skelDict.size > 0, {
		// Skeleton "trash cleaner" (if a skel stuck for more than 5sec, clear it out).
		skelDict.keys.do{ |k|
			if((Date.getDate.secStamp.asInteger - skelDict[k].update.asInteger) >= 5, {
				skelDict.removeAt(k);
				});
			};
		// Draw skeletons
		skelDict.keysValuesDo { |k, l|
			l.rHand.postln;
			Pen.fillColor= Color.white;
			Pen.strokeColor= Color.blue;
			Pen.fillOval(Rect.aboutRedVector3D(l.neck, l.neck[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.torso, l.torso[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.rHand, l.rHand[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.rElbow, l.rElbow[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.lHand, l.lHand[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.lElbow, l.lElbow[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.rFoot, l.rFoot[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.rKnee, l.rKnee[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.lFoot, l.lFoot[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			Pen.fillOval(Rect.aboutRedVector3D(l.lKnee, l.lKnee[2].linlin(margin, depth-margin, 10, 1), width, height, depth));
			};
		});
	};
w.animate= true;

CmdPeriod.doOnce({
	removeKinectResp.value();
	w.close;
	});
)