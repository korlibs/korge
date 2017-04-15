package com.brashmonkey.spriter;

import com.brashmonkey.spriter.Mainline.Key.BoneRef;
import com.brashmonkey.spriter.Mainline.Key.ObjectRef;
import com.brashmonkey.spriter.Timeline.Key.Bone;
import com.brashmonkey.spriter.Timeline.Key.Object;

/**
 * A tweened animation is responsible for updating itself based on two given animations.
 * The values of the two given animations will get interpolated and save in this animation.
 * When tweening two animations, you have to make sure that they have the same structure.
 * The best result is achieved if bones of two different animations are named in the same way.
 * There are still issues with sprites, which are hard to resolve since Spriter does not save them in a useful order or naming convention.
 * @author Trixt0r
 *
 */
public class TweenedAnimation extends Animation{
	
	/**
	 * The weight of the interpolation. 0.5f is the default value.
	 * Values closer to 0.0f mean the first animation will have more influence.
	 */
	public float weight = .5f;
	
	/**
	 * Indicates when a sprite should be switched form the first animation object to the second one.
	 * A value closer to 0.0f means that the sprites of the second animation will be drawn.
	 */
	public float spriteThreshold = .5f;
	
	/**
	 * The curve which will tween the animations.
	 * The default type of the curve is {@link Curve.Type#Linear}.
	 */
	public final Curve curve;
	
	/**
	 * The entity the animations have be part of.
	 * Animations of two different entities can not be tweened.
	 */
	public final Entity entity;
	private Animation anim1, anim2;
	
	/**
	 * The base animation an object or bone will get if it will not be tweened.
	 */
	public Animation baseAnimation;
	BoneRef base = null;
	
	/**
	 * Indicates whether to tween sprites or not. Default value is <code>false</code>.
	 * Tweening sprites should be only enabled if they have exactly the same structure.
	 * If all animations are bone based and sprites only change their references it is not recommended to tween sprites.
	 */
	public boolean tweenSprites = false;
	
	/**
	 * Creates a tweened animation based on the given entity.
	 * @param entity the entity animations have to be part of
	 */
	public TweenedAnimation(Entity entity) {
		super(new Mainline(0), -1, "__interpolatedAnimation__", 0, true, entity.getAnimationWithMostTimelines().timelines());
		this.entity = entity;
		this.curve = new Curve();
		this.setUpTimelines();
	}
	
	/**
	 * Returns the current mainline key.
	 * @return the mainline key
	 */
	public Mainline.Key getCurrentKey(){
		return this.currentKey;
	}
	
	@Override
	public void update(int time, Bone root){
		super.currentKey = onFirstMainLine() ? anim1.currentKey: anim2.currentKey;
    	for(Timeline.Key timelineKey: this.unmappedTweenedKeys)
			timelineKey.active = false;
    	if(base != null){//TODO: Sprites not working properly because of different timeline naming
        	Animation currentAnim = onFirstMainLine() ? anim1: anim2;
        	Animation baseAnim = baseAnimation == null ? (onFirstMainLine() ? anim1:anim2) : baseAnimation;
	    	for(BoneRef ref: currentKey.boneRefs){
	        	Timeline timeline = baseAnim.getSimilarTimeline(currentAnim.getTimeline(ref.timeline));
	        	if(timeline == null) continue;
	    		Timeline.Key key, mappedKey;
    			key = baseAnim.tweenedKeys[timeline.id];
    			mappedKey = baseAnim.unmappedTweenedKeys[timeline.id];
	    		this.tweenedKeys[ref.timeline].active = key.active;
	    		this.tweenedKeys[ref.timeline].object().set(key.object());
	    		this.unmappedTweenedKeys[ref.timeline].active = mappedKey.active;
				this.unmapTimelineObject(ref.timeline, false,(ref.parent != null) ?
						this.unmappedTweenedKeys[ref.parent.timeline].object(): root);
	    	}
	    	/*for(ObjectRef ref: baseAnim.currentKey.objectRefs){
	        	Timeline timeline = baseAnim.getTimeline(ref.timeline);//getSimilarTimeline(ref, tempTimelines);
	        	if(timeline != null){
	        		//tempTimelines.addLast(timeline);
	        		Timeline.Key key = baseAnim.tweenedKeys[timeline.id];
	        		Timeline.Key mappedKey = baseAnim.mappedTweenedKeys[timeline.id];
	        		Object obj = (Object) key.object();
	        		
		    		this.tweenedKeys[ref.timeline].active = key.active;
		    		((Object)this.tweenedKeys[ref.timeline].object()).set(obj);
		    		this.mappedTweenedKeys[ref.timeline].active = mappedKey.active;
					this.unmapTimelineObject(ref.timeline, true,(ref.parent != null) ?
							this.mappedTweenedKeys[ref.parent.timeline].object(): root);
	        	}
	    	}*/
	    	//tempTimelines.clear();
    	}
    		
    	this.tweenBoneRefs(base, root);
		for(ObjectRef ref: super.currentKey.objectRefs){
			//if(ref.parent == base)
				this.update(ref, root, 0);
		}
    }
	
	private void tweenBoneRefs(BoneRef base, Bone root){
    	int startIndex = base == null ? -1 : base.id-1;
    	int length = super.currentKey.boneRefs.length;
		for(int i = startIndex+1; i < length; i++){
			BoneRef ref = currentKey.boneRefs[i];
			if(base == ref || ref.parent == base) this.update(ref, root, 0);
			if(base == ref.parent) this.tweenBoneRefs(ref, root);
		}
	}
	
	@Override
	protected void update(BoneRef ref, Bone root, int time){
    	boolean isObject = ref instanceof ObjectRef;
		//Tween bone/object
    	Bone bone1 = null, bone2 = null, tweenTarget = null;
    	Timeline t1 = onFirstMainLine() ? anim1.getTimeline(ref.timeline) : anim1.getSimilarTimeline(anim2.getTimeline(ref.timeline));
    	Timeline t2 = onFirstMainLine() ? anim2.getSimilarTimeline(t1) : anim2.getTimeline(ref.timeline);
    	Timeline targetTimeline = super.getTimeline(onFirstMainLine() ? t1.id:t2.id);
    	if(t1 != null) bone1 = anim1.tweenedKeys[t1.id].object();
    	if(t2 != null) bone2 = anim2.tweenedKeys[t2.id].object();
    	if(targetTimeline != null) tweenTarget = this.tweenedKeys[targetTimeline.id].object();
    	if(isObject && (t2 == null || !tweenSprites)){
    		if(!onFirstMainLine()) bone1 = bone2;
    		else bone2 = bone1;
    	}
		if(bone2 != null && tweenTarget != null && bone1 != null){
			if(isObject) this.tweenObject((Object)bone1, (Object)bone2, (Object)tweenTarget, this.weight, this.curve);
			else this.tweenBone(bone1, bone2, tweenTarget, this.weight, this.curve);
			this.unmappedTweenedKeys[targetTimeline.id].active = true;
		}
		//Transform the bone relative to the parent bone or the root
		if(this.unmappedTweenedKeys[ref.timeline].active){
			this.unmapTimelineObject(targetTimeline.id, isObject,(ref.parent != null) ?
					this.unmappedTweenedKeys[ref.parent.timeline].object(): root);
		}
    }
	
	private void tweenBone(Bone bone1, Bone bone2, Bone target, float t, Curve curve){
		target.angle = curve.tweenAngle(bone1.angle, bone2.angle, t);
		curve.tweenPoint(bone1.position, bone2.position, t, target.position);
		curve.tweenPoint(bone1.scale, bone2.scale, t, target.scale);
		curve.tweenPoint(bone1.pivot, bone2.pivot, t, target.pivot);
	}
	
	private void tweenObject(Object object1, Object object2, Object target, float t, Curve curve){
		this.tweenBone(object1, object2, target, t, curve);
		target.alpha = curve.tweenAngle(object1.alpha, object2.alpha, t);
		target.ref.set(object1.ref);
	}
	
	/**
	 * Returns whether the current mainline key is the one from the first animation or from the second one.
	 * @return <code>true</code> if the mainline key is the one from the first animation 
	 */
	public boolean onFirstMainLine(){
		return this.weight < this.spriteThreshold;
	}
	
	private void setUpTimelines(){
		Animation maxAnim = this.entity.getAnimationWithMostTimelines();
		int max = maxAnim.timelines();
		for(int i = 0; i < max; i++){
			Timeline t = new Timeline(i, maxAnim.getTimeline(i).name, maxAnim.getTimeline(i).objectInfo, 1);
			addTimeline(t);
		}
		prepare();
	}
	
	/**
	 * Sets the animations to tween.
	 * @param animation1 the first animation
	 * @param animation2 the second animation
	 * @throws SpriterException if {@link #entity} does not contain one of the given animations.
	 */
	public void setAnimations(Animation animation1, Animation animation2){
		boolean areInterpolated = animation1 instanceof TweenedAnimation || animation2 instanceof TweenedAnimation;
		if(animation1 == anim1 && animation2 == anim2) return;
		if((!this.entity.containsAnimation(animation1) || !this.entity.containsAnimation(animation2)) && !areInterpolated)
			throw new SpriterException("Both animations have to be part of the same entity!");
		this.anim1 = animation1;
		this.anim2 = animation2;
	}
	
	/**
	 * Returns the first animation.
	 * @return the first animation
	 */
	public Animation getFirstAnimation(){
		return this.anim1;
	}
	
	/**
	 * Returns the second animation.
	 * @return the second animation
	 */
	public Animation getSecondAnimation(){
		return this.anim2;
	}

}
