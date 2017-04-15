package com.brashmonkey.spriter;

import java.util.HashMap;

import com.brashmonkey.spriter.Mainline.Key;
import com.brashmonkey.spriter.Mainline.Key.BoneRef;
import com.brashmonkey.spriter.Mainline.Key.ObjectRef;
import com.brashmonkey.spriter.Timeline.Key.Bone;
import com.brashmonkey.spriter.Timeline.Key.Object;
/**
 * Represents an animation of a Spriter SCML file.
 * An animation holds {@link Timeline}s and a {@link Mainline} to animate objects.
 * Furthermore it holds an {@link #id}, a {@link #length}, a {@link #name} and whether it is {@link #looping} or not.
 * @author Trixt0r
 *
 */
public class Animation {

    public final Mainline mainline;
    private final Timeline[] timelines;
    private int timelinePointer = 0;
    private final HashMap<String, Timeline> nameToTimeline;
    public final int id, length;
    public final String name;
    public final boolean looping;
	Key currentKey;
	Timeline.Key[] tweenedKeys, unmappedTweenedKeys;
	private boolean prepared;
    
    public Animation(Mainline mainline, int id, String name, int length, boolean looping, int timelines){
    	this.mainline = mainline;
    	this.id = id;
    	this.name = name;
    	this.length = length;
    	this.looping = looping;
    	this.timelines = new Timeline[timelines];
    	this.prepared = false;
    	this.nameToTimeline = new HashMap<String, Timeline>();
    	//this.currentKey = mainline.getKey(0);
    }
    
    /**
     * Returns a {@link Timeline} with the given index.
     * @param index the index of the timeline
     * @return the timeline with the given index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Timeline getTimeline(int index){
    	return this.timelines[index];
    }
    
    /**
     * Returns a {@link Timeline} with the given name.
     * @param name the name of the time line
     * @return the time line with the given name or null if no time line exists with the given name.
     */
    public Timeline getTimeline(String name){
    	return this.nameToTimeline.get(name);
    }
    
    void addTimeline(Timeline timeline){
    	this.timelines[timelinePointer++] = timeline;
    	this.nameToTimeline.put(timeline.name, timeline);
    }
    
    /**
     * Returns the number of time lines this animation holds.
     * @return the number of time lines
     */
    public int timelines(){
    	return timelines.length;
    }
    
    public String toString(){
    	String toReturn = getClass().getSimpleName()+"|[id: "+id+", "+name+", duration: "+length+", is looping: "+looping;
    	toReturn +="Mainline:\n";
    	toReturn += mainline;
    	toReturn += "Timelines\n";
    	for(Timeline timeline: this.timelines)
    		toReturn += timeline;
    	toReturn+="]";
    	return toReturn;
    }
    
    /**
     * Updates the bone and object structure with the given time to the given root bone.
     * @param time The time which has to be between 0 and {@link #length} to work properly.
     * @param root The root bone which is not allowed to be null. The whole animation runs relative to the root bone.
     */
    public void update(int time, Bone root){
    	if(!this.prepared) throw new SpriterException("This animation is not ready yet to animate itself. Please call prepare()!");
    	if(root == null) throw new SpriterException("The root can not be null! Set a root bone to apply this animation relative to the root bone.");
    	this.currentKey = mainline.getKeyBeforeTime(time);

    	for(Timeline.Key timelineKey: this.unmappedTweenedKeys)
			timelineKey.active = false;
		for(BoneRef ref: currentKey.boneRefs)
			this.update(ref, root, time);
		for(ObjectRef ref: currentKey.objectRefs)
			this.update(ref, root, time);
    }
    
    protected void update(BoneRef ref, Bone root, int time){
    	boolean isObject = ref instanceof ObjectRef;
		//Get the timelines, the refs pointing to
		Timeline timeline = getTimeline(ref.timeline);
		Timeline.Key key = timeline.getKey(ref.key);
		Timeline.Key nextKey = timeline.getKey((ref.key+1)%timeline.keys.length);
		int currentTime = key.time;
		int nextTime = nextKey.time;
		if(nextTime < currentTime){
			if(!looping) nextKey = key;
			else nextTime = length;
		}
		//Normalize the time
		float t = (float)(time - currentTime)/(float)(nextTime - currentTime);
		if(Float.isNaN(t) || Float.isInfinite(t)) t = 1f;
		if(currentKey.time > currentTime){
			float tMid = (float)(currentKey.time - currentTime)/(float)(nextTime - currentTime);
			if(Float.isNaN(tMid) || Float.isInfinite(tMid)) tMid = 0f;
			t = (float)(time - currentKey.time)/(float)(nextTime - currentKey.time);
			if(Float.isNaN(t) || Float.isInfinite(t)) t = 1f;
			t = currentKey.curve.tween(tMid, 1f, t);
		}
		else 
			t = currentKey.curve.tween(0f, 1f, t);
		//Tween bone/object
		Bone bone1 = key.object();
		Bone bone2 = nextKey.object();
		Bone tweenTarget = this.tweenedKeys[ref.timeline].object();
		if(isObject) this.tweenObject((Object)bone1, (Object)bone2, (Object)tweenTarget, t, key.curve, key.spin);
		else this.tweenBone(bone1, bone2, tweenTarget, t, key.curve, key.spin);
		this.unmappedTweenedKeys[ref.timeline].active = true;
		this.unmapTimelineObject(ref.timeline, isObject,(ref.parent != null) ?
				this.unmappedTweenedKeys[ref.parent.timeline].object(): root);
    }
    
    void unmapTimelineObject(int timeline, boolean isObject, Bone root){
		Bone tweenTarget = this.tweenedKeys[timeline].object();
		Bone mapTarget = this.unmappedTweenedKeys[timeline].object();
		if(isObject) ((Object)mapTarget).set((Object)tweenTarget);
		else mapTarget.set(tweenTarget);
		mapTarget.unmap(root);
    }
	
	protected void tweenBone(Bone bone1, Bone bone2, Bone target, float t, Curve curve, int spin){
		target.angle = curve.tweenAngle(bone1.angle, bone2.angle, t, spin);
		curve.tweenPoint(bone1.position, bone2.position, t, target.position);
		curve.tweenPoint(bone1.scale, bone2.scale, t, target.scale);
		curve.tweenPoint(bone1.pivot, bone2.pivot, t, target.pivot);
	}
	
	protected void tweenObject(Object object1, Object object2, Object target, float t, Curve curve, int spin){
		this.tweenBone(object1, object2, target, t, curve, spin);
		target.alpha = curve.tweenAngle(object1.alpha, object2.alpha, t);
		target.ref.set(object1.ref);
	}
	
	Timeline getSimilarTimeline(Timeline t){
    	Timeline found = getTimeline(t.name);
    	if(found == null && t.id < this.timelines()) found = this.getTimeline(t.id);
    	return found;
	}
	
	/*Timeline getSimilarTimeline(BoneRef ref, Collection<Timeline> coveredTimelines){
		if(ref.parent == null) return null;
    	for(BoneRef boneRef: this.currentKey.objectRefs){
    		Timeline t = this.getTimeline(boneRef.timeline);
    		if(boneRef.parent != null && boneRef.parent.id == ref.parent.id && !coveredTimelines.contains(t))
    			return t;
    	}
    	return null;
	}
	
	Timeline getSimilarTimeline(ObjectRef ref, Collection<Timeline> coveredTimelines){
		if(ref.parent == null) return null;
    	for(ObjectRef objRef: this.currentKey.objectRefs){
    		Timeline t = this.getTimeline(objRef.timeline);
    		if(objRef.parent != null && objRef.parent.id == ref.parent.id && !coveredTimelines.contains(t))
    			return t;
    	}
    	return null;
	}*/
	
	/**
	 * Prepares this animation to set this animation in any time state.
	 * This method has to be called before {@link #update(int, Bone)}.
	 */
	public void prepare(){
		if(this.prepared) return;
		this.tweenedKeys = new Timeline.Key[timelines.length];
		this.unmappedTweenedKeys = new Timeline.Key[timelines.length];
		
		for(int i = 0; i < this.tweenedKeys.length; i++){
			this.tweenedKeys[i] = new Timeline.Key(i);
			this.unmappedTweenedKeys[i] = new Timeline.Key(i);
			this.tweenedKeys[i].setObject(new Timeline.Key.Object(new Point(0,0)));
			this.unmappedTweenedKeys[i].setObject(new Timeline.Key.Object(new Point(0,0)));
		}
		if(mainline.keys.length > 0) currentKey = mainline.getKey(0);
		this.prepared = true;
	}

}
