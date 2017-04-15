package com.brashmonkey.spriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.brashmonkey.spriter.Entity.CharacterMap;
import com.brashmonkey.spriter.Entity.ObjectInfo;
import com.brashmonkey.spriter.Mainline.Key.BoneRef;
import com.brashmonkey.spriter.Mainline.Key.ObjectRef;
import com.brashmonkey.spriter.Timeline.Key.Bone;
import com.brashmonkey.spriter.Timeline.Key.Object;

/**
 * A Player instance is responsible for updating an {@link Animation} properly.
 * With the {@link #update()} method an instance of this class will increase its current time
 * and update the current set animation ({@link #setAnimation(Animation)}).
 * A Player can be positioned with {@link #setPivot(float, float)}, scaled with {@link #setScale(float)},
 * flipped with {@link #flip(boolean, boolean)} and rotated {@link #setAngle(float)}.
 * A Player has various methods for runtime object manipulation such as {@link #setBone(String, Bone)} or {@link #setObject(String, Bone)}.
 * Events like the ending of an animation can be observed with the {@link PlayerListener} interface.
 * Character maps can be changed on the fly, just by assigning a character maps to {@link #characterMaps}, setting it to <code>null</code> will remove the current character map.
 * 
 * @author Trixt0r
 *
 */
public class Player {
	
	protected Entity entity;
	Animation animation;
	int time;
	public int speed;
	Timeline.Key[] tweenedKeys, unmappedTweenedKeys;
	private Timeline.Key[] tempTweenedKeys, tempUnmappedTweenedKeys;
	private List<PlayerListener> listeners;
	public final List<Attachment> attachments = new ArrayList<Attachment>();
	Timeline.Key.Bone root = new Timeline.Key.Bone(new Point(0,0));
	private final Point position = new Point(0,0), pivot = new Point(0,0);
	private final HashMap<Object, Timeline.Key> objToTimeline = new HashMap<Object, Timeline.Key>();
	private float angle;
	private boolean dirty = true;
	public CharacterMap[] characterMaps;
	private Rectangle rect;
	public final Box prevBBox;
	private BoneIterator boneIterator;
	private ObjectIterator objectIterator;
	private Mainline.Key currentKey, prevKey;
	public boolean copyObjects = true;
	
	/**
	 * Creates a {@link Player} instance with the given entity.
	 * @param entity the entity this player will animate
	 */
	public Player(Entity entity){
		this.boneIterator = new BoneIterator();
		this.objectIterator = new ObjectIterator();
		this.speed = 15;
		this.rect = new Rectangle(0,0,0,0);
		this.prevBBox = new Box();
		this.listeners = new ArrayList<PlayerListener>();
		this.setEntity(entity);
	}
	
	/**
	 * Updates this player.
	 * This means the current time gets increased by {@link #speed} and is applied to the current animation.
	 */
	public void update(){
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).preProcess(this);
		}
		if(dirty) this.updateRoot();
		this.animation.update(time, root);
		this.currentKey = this.animation.currentKey;
		if(prevKey != currentKey){
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).mainlineKeyChanged(prevKey, currentKey);
			}
			prevKey = currentKey;
		}
		if(copyObjects){
			tweenedKeys = tempTweenedKeys;
			unmappedTweenedKeys = tempUnmappedTweenedKeys;
			this.copyObjects();
		}
		else{
			tweenedKeys = animation.tweenedKeys;
			unmappedTweenedKeys = animation.unmappedTweenedKeys;
		}

		for (int i = 0; i < attachments.size(); i++) {
			attachments.get(i).update();
		}

		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).postProcess(this);
		}
		this.increaseTime();
	}
	
	private void copyObjects(){
		for(int i = 0; i < animation.tweenedKeys.length; i++){
			this.tweenedKeys[i].active = animation.tweenedKeys[i].active;
			this.unmappedTweenedKeys[i].active = animation.unmappedTweenedKeys[i].active;
			this.tweenedKeys[i].object().set(animation.tweenedKeys[i].object());
			this.unmappedTweenedKeys[i].object().set(animation.unmappedTweenedKeys[i].object());
		}
	}
	
	private void increaseTime(){
		time += speed;
		if(time > animation.length){
			time = time-animation.length;
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).animationFinished(animation);
			}
		}
		if(time < 0){
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).animationFinished(animation);
			}
			time += animation.length;
		}
	}
	
	private void updateRoot(){
		this.root.angle = angle;
		this.root.position.set(pivot);
		this.root.position.rotate(angle);
		this.root.position.translate(position);
		dirty = false;
	}
	
	/**
	 * Returns a time line bone at the given index.
	 * @param index the index of the bone
	 * @return the bone with the given index.
	 */
	public Bone getBone(int index){
		return this.unmappedTweenedKeys[getCurrentKey().getBoneRef(index).timeline].object();
	}
	
	/**
	 * Returns a time line object at the given index.
	 * @param index the index of the object
	 * @return the object with the given index.
	 */
	public Object getObject(int index){
		return (Object) this.unmappedTweenedKeys[getCurrentKey().getObjectRef(index).timeline].object();
	}
	
	/**
	 * Returns the index of a time line bone with the given name.
	 * @param name the name of the bone
	 * @return the index of the bone or -1 if no bone exists with the given name
	 */
	public int getBoneIndex(String name){
		for(BoneRef ref: getCurrentKey().boneRefs)
			if(animation.getTimeline(ref.timeline).name.equals(name))
				return ref.id;
		return -1;
	}
	
	/**
	 * Returns a time line bone with the given name.
	 * @param name the name of the bone
	 * @return the bone with the given name
	 * @throws ArrayIndexOutOfBoundsException if no bone exists with the given name
	 * @throws NullPointerException if no bone exists with the given name
	 */
	public Bone getBone(String name){
		return this.unmappedTweenedKeys[animation.getTimeline(name).id].object();
	}
	
	/**
	 * Returns a bone reference for the given time line bone.
	 * @param bone the time line bone
	 * @return the bone reference for the given bone
	 * @throws NullPointerException if no reference for the given bone was found
	 */
	public BoneRef getBoneRef(Bone bone){
		return this.getCurrentKey().getBoneRefTimeline(this.objToTimeline.get(bone).id);
	}
	
	/**
	 * Returns the index of a time line object with the given name.
	 * @param name the name of the object
	 * @return the index of the object or -1 if no object exists with the given name
	 */
	public int getObjectIndex(String name){
		for(ObjectRef ref: getCurrentKey().objectRefs)
			if(animation.getTimeline(ref.timeline).name.equals(name))
				return ref.id;
		return -1;
	}
	
	/**
	 * Returns a time line object with the given name.
	 * @param name the name of the object
	 * @return the object with the given name
	 * @throws ArrayIndexOutOfBoundsException if no object exists with the given name
	 * @throws NullPointerException if no object exists with the given name
	 */
	public Object getObject(String name){
		return (Object)this.unmappedTweenedKeys[animation.getTimeline(name).id].object();
	}
	
	/**
	 * Returns a object reference for the given time line bone.
	 * @param object the time line object
	 * @return the object reference for the given bone
	 * @throws NullPointerException if no reference for the given object was found
	 */
	public ObjectRef getObjectRef(Object object){
		return this.getCurrentKey().getObjectRefTimeline(this.objToTimeline.get(object).id);
	}
	
	/**
	 * Returns the name for the given bone or object.
	 * @param boneOrObject the bone or object
	 * @return the name of the bone or object
	 * @throws NullPointerException if no name for the given bone or bject was found
	 */
	public String getNameFor(Bone boneOrObject){
		return this.animation.getTimeline(objToTimeline.get(boneOrObject).id).name;
	}
	
	/**
	 * Returns the object info for the given bone or object.
	 * @param boneOrObject the bone or object
	 * @return the object info of the bone or object
	 * @throws NullPointerException if no object info for the given bone or bject was found
	 */
	public ObjectInfo getObjectInfoFor(Bone boneOrObject){
		return this.animation.getTimeline(objToTimeline.get(boneOrObject).id).objectInfo;
	}
	
	/**
	 * Returns the time line key for the given bone or object
	 * @param boneOrObject the bone or object
	 * @return the time line key of the bone or object, or null if no time line key was found
	 */
	public Timeline.Key getKeyFor(Bone boneOrObject){
		return objToTimeline.get(boneOrObject);
	}
	
	/**
	 * Calculates and returns a {@link Box} for the given bone or object.
	 * @param boneOrObject the bone or object to calculate the bounding box for
	 * @return the box for the given bone or object
	 * @throws NullPointerException if no object info for the given bone or object exists
	 */
	public Box getBox(Bone boneOrObject){
		ObjectInfo info = getObjectInfoFor(boneOrObject);
		this.prevBBox.calcFor(boneOrObject, info);
		return this.prevBBox;
	}
	
	/**
	 * Returns whether the given point at x,y lies inside the box of the given bone or object.
	 * @param boneOrObject the bone or object
	 * @param x the x value of the point
	 * @param y the y value of the point
	 * @return <code>true</code> if x,y lies inside the box of the given bone or object
	 * @throws NullPointerException if no object info for the given bone or object exists
	 */
	public boolean collidesFor(Bone boneOrObject, float x, float y){
		ObjectInfo info = getObjectInfoFor(boneOrObject);
		this.prevBBox.calcFor(boneOrObject, info);
		return this.prevBBox.collides(boneOrObject, info, x, y);
	}
	
	/**
	 * Returns whether the given point lies inside the box of the given bone or object.
	 * @param bone the bone or object
	 * @param point the point
	 * @return <code>true</code> if the point lies inside the box of the given bone or object
	 * @throws NullPointerException if no object info for the given bone or object exists
	 */
	public boolean collidesFor(Bone boneOrObject, Point point){
		return this.collidesFor(boneOrObject, point.x, point.y);
	}
	
	/**
	 * Returns whether the given area collides with the box of the given bone or object.
	 * @param boneOrObject the bone or object
	 * @param area the rectangular area
	 * @return <code>true</code> if the area collides with the bone or object
	 */
	public boolean collidesFor(Bone boneOrObject, Rectangle area){
		ObjectInfo info = getObjectInfoFor(boneOrObject);
		this.prevBBox.calcFor(boneOrObject, info);
		return this.prevBBox.isInside(area);
	}
	
	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * @param x the new x value of the bone
	 * @param y the new y value of the bone
	 * @param angle the new angle of the bone
	 * @param scaleX the new scale in x direction of the bone
	 * @param scaleY the new scale in y direction of the bone
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, float x, float y, float angle, float scaleX, float scaleY){
		int index = getBoneIndex(name);
		if(index == -1) throw new SpriterException("No bone found of name \""+name+"\"");
		BoneRef ref = getCurrentKey().getBoneRef(index);
		Bone bone = getBone(index);
		bone.set(x, y, angle, scaleX, scaleY, 0f, 0f);
		unmapObjects(ref);
	}
	
	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * @param position the new position of the bone
	 * @param angle the new angle of the bone
	 * @param scale the new scale of the bone
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, Point position, float angle, Point scale){
		this.setBone(name, position.x, position.y, angle, scale.x, scale.y);
	}
	
	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * @param x the new x value of the bone
	 * @param y the new y value of the bone
	 * @param angle the new angle of the bone
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, float x, float y, float angle){
		Bone b = getBone(name);
		setBone(name, x, y, angle, b.scale.x, b.scale.y);
	}
	
	/**
	 * Sets the given values of the bone with the given name.
	 * @param name the name of the bone
	 * @param position the new position of the bone
	 * @param angle the new angle of the bone
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, Point position, float angle){
		Bone b = getBone(name);
		setBone(name, position.x, position.y, angle, b.scale.x, b.scale.y);
	}
	
	/**
	 * Sets the position of the bone with the given name.
	 * @param name the name of the bone
	 * @param x the new x value of the bone
	 * @param y the new y value of the bone
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, float x, float y){
		Bone b = getBone(name);
		setBone(name, x, y, b.angle);
	}
	
	/**
	 * Sets the position of the bone with the given name.
	 * @param name the name of the bone
	 * @param position the new position of the bone
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, Point position){
		setBone(name, position.x, position.y);
	}
	
	/**
	 * Sets the angle of the bone with the given name
	 * @param name the name of the bone
	 * @param angle the new angle of the bone
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, float angle){
		Bone b = getBone(name);
		setBone(name, b.position.x, b.position.y, angle);
	}
	
	/**
	 * Sets the values of the bone with the given name to the values of the given bone
	 * @param name the name of the bone
	 * @param bone the bone with the new values
	 * @throws SpriterException if no bone exists of the given name
	 */
	public void setBone(String name, Bone bone){
		setBone(name, bone.position, bone.angle, bone.scale);
	}
	
	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * @param x the new position in x direction of the object
	 * @param y the new position in y direction of the object
	 * @param angle the new angle of the object
	 * @param scaleX the new scale in x direction of the object
	 * @param scaleY the new scale in y direction of the object
	 * @param pivotX the new pivot in x direction of the object
	 * @param pivotY the new pivot in y direction of the object
	 * @param alpha the new alpha value of the object
	 * @param folder the new folder index of the object
	 * @param file the new file index of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, float x, float y, float angle, float scaleX, float scaleY, float pivotX, float pivotY, float alpha, int folder, int file){
		int index = getObjectIndex(name);
		if(index == -1) throw new SpriterException("No object found for name \""+name+"\"");
		ObjectRef ref = getCurrentKey().getObjectRef(index);
		Object object = getObject(index);
		object.set(x, y, angle, scaleX, scaleY, pivotX, pivotY, alpha, folder, file);
		unmapObjects(ref);
	}
	
	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * @param position the new position of the object
	 * @param angle the new angle of the object
	 * @param scale the new scale of the object
	 * @param pivot the new pivot of the object
	 * @param alpha the new alpha value of the object
	 * @param ref the new file reference of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, Point position, float angle, Point scale, Point pivot, float alpha, FileReference ref){
		this.setObject(name, position.x, position.y, angle, scale.x, scale.y, pivot.x, pivot.y, alpha, ref.folder, ref.file);
	}
	
	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * @param x the new position in x direction of the object
	 * @param y the new position in y direction of the object
	 * @param angle the new angle of the object
	 * @param scaleX the new scale in x direction of the object
	 * @param scaleY the new scale in y direction of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, float x, float y, float angle, float scaleX, float scaleY){
		Object b = getObject(name);
		setObject(name, x, y, angle, scaleX, scaleY, b.pivot.x, b.pivot.y, b.alpha, b.ref.folder, b.ref.file);
	}
	
	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * @param x the new position in x direction of the object
	 * @param y the new position in y direction of the object
	 * @param angle the new angle of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, float x, float y, float angle){
		Object b = getObject(name);
		setObject(name, x, y, angle, b.scale.x, b.scale.y);
	}
	
	/**
	 * Sets the given values of the object with the given name.
	 * @param name the name of the object
	 * @param position the new position of the object
	 * @param angle the new angle of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, Point position, float angle){
		Object b = getObject(name);
		setObject(name, position.x, position.y, angle, b.scale.x, b.scale.y);
	}
	
	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * @param x the new position in x direction of the object
	 * @param y the new position in y direction of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, float x, float y){
		Object b = getObject(name);
		setObject(name, x, y, b.angle);
	}
	
	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * @param position the new position of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, Point position){
		setObject(name, position.x, position.y);
	}
	
	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * @param angle the new angle of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, float angle){
		Object b = getObject(name);
		setObject(name, b.position.x, b.position.y, angle);
	}
	
	/**
	 * Sets the position of the object with the given name.
	 * @param name the name of the object
	 * @param alpha the new alpha value of the object
	 * @param folder the new folder index of the object
	 * @param file the new file index of the object
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, float alpha, int folder, int file){
		Object b = getObject(name);
		setObject(name, b.position.x, b.position.y, b.angle, b.scale.x, b.scale.y, b.pivot.x, b.pivot.y, alpha, folder, file);
	}
	
	/**
	 * Sets the values of the object with the given name to the values of the given object.
	 * @param name the name of the object
	 * @param object the object with the new values
	 * @throws SpriterException if no object exists of the given name
	 */
	public void setObject(String name, Object object){
		setObject(name, object.position, object.angle, object.scale, object.pivot, object.alpha, object.ref);
	}
	
	/**
	 * Maps all object from the parent's coordinate system to the global coordinate system.
	 * @param base the root bone to start at. Set it to <code>null</code> to traverse the whole bone hierarchy.
	 */
	public void unmapObjects(BoneRef base){
		int start = base == null ? -1 : base.id-1;
    	for(int i = start+1; i < getCurrentKey().boneRefs.length; i++){
    		BoneRef ref = getCurrentKey().getBoneRef(i);
    		if(ref.parent != base && base != null) continue;
			Bone parent = ref.parent == null ? this.root : this.unmappedTweenedKeys[ref.parent.timeline].object();
			unmappedTweenedKeys[ref.timeline].object().set(tweenedKeys[ref.timeline].object());
			unmappedTweenedKeys[ref.timeline].object().unmap(parent);
			unmapObjects(ref);
		}
		for(ObjectRef ref: getCurrentKey().objectRefs){
    		if(ref.parent != base && base != null) continue;
			Bone parent = ref.parent == null ? this.root : this.unmappedTweenedKeys[ref.parent.timeline].object();
			unmappedTweenedKeys[ref.timeline].object().set(tweenedKeys[ref.timeline].object());
			unmappedTweenedKeys[ref.timeline].object().unmap(parent);
		}
	}
	
	/**
	 * Sets the entity for this player instance.
	 * The animation will be switched to the first one of the new entity.
	 * @param entity the new entity
	 * @throws SpriterException if the entity is <code>null</code>
	 */
	public void setEntity(Entity entity){
		if(entity == null) throw new SpriterException("entity can not be null!");
		this.entity = entity;
		int maxAnims = entity.getAnimationWithMostTimelines().timelines();
		tweenedKeys = new Timeline.Key[maxAnims];
		unmappedTweenedKeys = new Timeline.Key[maxAnims];
		for(int i = 0; i < maxAnims; i++){
			Timeline.Key key = new Timeline.Key(i);
			Timeline.Key keyU = new Timeline.Key(i);
			key.setObject(new Timeline.Key.Object(new Point(0,0)));
			keyU.setObject(new Timeline.Key.Object(new Point(0,0)));
			tweenedKeys[i] = key;
			unmappedTweenedKeys[i] = keyU;
			this.objToTimeline.put(keyU.object(), keyU);
		}
		this.tempTweenedKeys = tweenedKeys;
		this.tempUnmappedTweenedKeys = unmappedTweenedKeys;
		this.setAnimation(entity.getAnimation(0));
	}
	
	/**
	 * Returns the current set entity.
	 * @return the current entity
	 */
	public Entity getEntity(){
		return this.entity;
	}
	
	/**
	 * Sets the animation of this player.
	 * @param animation the new animation
	 * @throws SpriterException if the animation is <code>null</code> or the current animation is not a member of the current set entity
	 */
	public void setAnimation(Animation animation){
		Animation prevAnim = this.animation;
		if(animation == this.animation) return;
		if(animation == null) throw new SpriterException("animation can not be null!");
		if(!this.entity.containsAnimation(animation) && animation.id != -1) throw new SpriterException("animation has to be in the same entity as the current set one!");
		if(animation != this.animation) time = 0;
		this.animation = animation;
		int tempTime = this.time;
		this.time = 0;
		this.update();
		this.time = tempTime;
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).animationChanged(prevAnim, animation);
		}
	}
	
	/**
	 * Sets the animation of this player to the one with the given name.
	 * @param name the name of the animation
	 * @throws SpriterException if no animation exists with the given name
	 */
	public void setAnimation(String name){
		this.setAnimation(entity.getAnimation(name));
	}
	
	/**
	 * Sets the animation of this player to the one with the given index.
	 * @param index the index of the animation
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public void setAnimation(int index){
		this.setAnimation(entity.getAnimation(index));
	}
	
	/**
	 * Returns the current set animation.
	 * @return the current animation
	 */
	public Animation getAnimation(){
		return this.animation;
	}
	
	/**
	 * Returns a bounding box for this player.
	 * The bounding box is calculated for all bones and object starting from the given root.
	 * @param root the starting root. Set it to null to calculate the bounding box for the whole player
	 * @return the bounding box
	 */
	public Rectangle getBoundingRectangle(BoneRef root){
		Bone boneRoot = root == null ? this.root : this.unmappedTweenedKeys[root.timeline].object();
		this.rect.set(boneRoot.position.x, boneRoot.position.y, boneRoot.position.x, boneRoot.position.y);
		this.calcBoundingRectangle(root);
		this.rect.calculateSize();
		return this.rect;
	}
	
	/**
	 * Returns a bounding box for this player.
	 * The bounding box is calculated for all bones and object starting from the given root.
	 * @param root the starting root. Set it to null to calculate the bounding box for the whole player
	 * @return the bounding box
	 */
	public Rectangle getBoudingRectangle(Bone root){
		return this.getBoundingRectangle(root == null ? null: getBoneRef(root));
	}
	
	private void calcBoundingRectangle(BoneRef root){
		for(BoneRef ref: getCurrentKey().boneRefs){
			if(ref.parent != root && root != null) continue;
			Bone bone = this.unmappedTweenedKeys[ref.timeline].object();
			this.prevBBox.calcFor(bone, animation.getTimeline(ref.timeline).objectInfo);
			Rectangle.setBiggerRectangle(rect, this.prevBBox.getBoundingRect(), rect);
			this.calcBoundingRectangle(ref);
		}
		for(ObjectRef ref: getCurrentKey().objectRefs){
			if(ref.parent != root) continue;
			Bone bone = this.unmappedTweenedKeys[ref.timeline].object();
			this.prevBBox.calcFor(bone, animation.getTimeline(ref.timeline).objectInfo);
			Rectangle.setBiggerRectangle(rect, this.prevBBox.getBoundingRect(), rect);
		}
	}
	
	/**
	 * Returns the current main line key based on the current {@link #time}.
	 * @return the current main line key
	 */
	public Mainline.Key getCurrentKey(){
		return this.currentKey;
	}
	
	/**
	 * Returns the current time.
	 * The player will make sure that the current time is always between 0 and {@link Animation#length}.
	 * @return the current time
	 */
	public int getTime() {
		return time;
	}
	
	/**
	 * Sets the time for the current time.
	 * The player will make sure that the new time will not exceed the time bounds of the current animation.
	 * @param time the new time
	 * @return this player to enable chained operations
	 */
	public Player setTime(int time){
		this.time = time;
		int prevSpeed = this.speed;
		this.speed = 0;
		this.increaseTime();
		this.speed = prevSpeed;
		return this;
	}
	
	/**
	 * Sets the scale of this player to the given one.
	 * Only uniform scaling is supported.
	 * @param scale the new scale. 1f means 100% scale.
	 * @return this player to enable chained operations
	 */
	public Player setScale(float scale){
		this.root.scale.set(scale*flippedX(), scale*flippedY());
		return this;
	}
	
	/**
	 * Scales this player based on the current set scale.
	 * @param scale the scaling factor. 1f means no scale.
	 * @return this player to enable chained operations
	 */
	public Player scale(float scale){
		this.root.scale.scale(scale, scale);
		return this;
	}
	
	/**
	 * Returns the current scale.
	 * @return the current scale
	 */
	public float getScale(){
		return root.scale.x;
	}
	
	/**
	 * Flips this player around the x and y axis.
	 * @param x whether to flip the player around the x axis
	 * @param y whether to flip the player around the y axis
	 * @return this player to enable chained operations
	 */
	public Player flip(boolean x, boolean y){
		if(x) this.flipX();
		if(y) this.flipY();
		return this;
	}
	
	/**
	 * Flips the player around the x axis.
	 * @return this player to enable chained operations
	 */
	public Player flipX(){
		this.root.scale.x *= -1;
		return this;
	}
	
	/**
	 * Flips the player around the y axis.
	 * @return this player to enable chained operations
	 */
	public Player flipY(){
		this.root.scale.y *= -1;
		return this;
	}
	
	/**
	 * Returns whether this player is flipped around the x axis.
	 * @return 1 if this player is not flipped, -1 if it is flipped
	 */
	public int flippedX(){
		return (int) Math.signum(root.scale.x);
	}
	
	/**
	 * Returns whether this player is flipped around the y axis.
	 * @return 1 if this player is not flipped, -1 if it is flipped
	 */
	public int flippedY(){
		return (int) Math.signum(root.scale.y);
	}
	
	/**
	 * Sets the position of this player to the given coordinates.
	 * @param x the new position in x direction
	 * @param y the new position in y direction
	 * @return this player to enable chained operations
	 */
	public Player setPosition(float x, float y){
		this.dirty = true;
		this.position.set(x,y);
		return this;
	}
	
	/**
	 * Sets the position of the player to the given one.
	 * @param position the new position
	 * @return this player to enable chained operations
	 */
	public Player setPosition(Point position){
		return this.setPosition(position.x, position.y);
	}
	
	/**
	 * Adds the given coordinates to the current position of this player.
	 * @param x the amount in x direction
	 * @param y the amount in y direction
	 * @return this player to enable chained operations
	 */
	public Player translatePosition(float x, float y){
		return this.setPosition(position.x+x, position.y+y);
	}
	
	/**
	 * Adds the given amount to the current position of this player.
	 * @param amount the amount to add
	 * @return this player to enable chained operations
	 */
	public Player translate(Point amount){
		return this.translatePosition(amount.x, amount.y);
	}
	
	/**
	 * Returns the current position in x direction.
	 * @return the current position in x direction
	 */
	public float getX(){
		return position.x;
	}
	
	/**
	 * Returns the current position in y direction.
	 * @return the current position in y direction
	 */
	public float getY(){
		return position.y;
	}
	
	/**
	 * Sets the angle of this player to the given angle.
	 * @param angle the angle in degrees
	 * @return this player to enable chained operations
	 */
	public Player setAngle(float angle){
		this.dirty = true;
		this.angle = angle;
		return this;
	}
	
	/**
	 * Rotates this player by the given angle.
	 * @param angle the angle in degrees
	 * @return this player to enable chained operations
	 */
	public Player rotate(float angle){
		return this.setAngle(angle+this.angle);
	}
	
	/**
	 * Returns the current set angle.
	 * @return the current angle
	 */
	public float getAngle(){
		return this.angle;
	}
	
	/**
	 * Sets the pivot, i.e. origin, of this player.
	 * A pivot at (0,0) means that the origin of the played animation will have the same one as in Spriter.
	 * @param x the new pivot in x direction
	 * @param y the new pivot in y direction
	 * @return this player to enable chained operations
	 */
	public Player setPivot(float x, float y){
		this.dirty = true;
		this.pivot.set(x, y);
		return this;
	}
	
	/**
	 * Sets the pivot, i.e. origin, of this player.
	 * A pivot at (0,0) means that the origin of the played animation will have the same one as in Spriter.
	 * @param pivot the new pivot
	 * @return this player to enable chained operations
	 */
	public Player setPivot(Point pivot){
		return this.setPivot(pivot.x, pivot.y);
	}
	
	/**
	 * Translates the current set pivot position by the given amount.
	 * @param x the amount in x direction
	 * @param y the amount in y direction
	 * @return this player to enable chained operations
	 */
	public Player translatePivot(float x, float y){
		return this.setPivot(pivot.x+x, pivot.y+y);
	}
	
	/**
	 * Adds the given amount to the current set pivot position.
	 * @param amount the amount to add
	 * @return this player to enable chained operations
	 */
	public Player translatePivot(Point amount){
		return this.translatePivot(amount.x, amount.y);
	}
	
	/**
	 * Returns the current set pivot in x direction.
	 * @return the pivot in x direction
	 */
	public float getPivotX(){
		return pivot.x;
	}
	
	/**
	 * Returns the current set pivot in y direction.
	 * @return the pivot in y direction
	 */
	public float getPivotY(){
		return pivot.y;
	}
	
	/**
	 * Appends a listener to the listeners list of this player.
	 * @param listener the listener to add
	 */
	public void addListener(PlayerListener listener){
		this.listeners.add(listener);
	}
	
	/**
	 * Removes a listener from  the listeners list of this player.
	 * @param listener the listener to remove
	 */
	public void removeListener(PlayerListener listener){
		this.listeners.remove(listener);
	}
	
	/**
	 * Returns an iterator to iterate over all time line bones in the current animation.
	 * @return the bone iterator
	 */
	public Iterator<Bone> boneIterator(){
		return this.boneIterator(this.getCurrentKey().boneRefs[0]);
	}
	
	/**
	 * Returns an iterator to iterate over all time line bones in the current animation starting at a given root.
	 * @param start the bone reference to start at
	 * @return the bone iterator
	 */
	public Iterator<Bone> boneIterator(BoneRef start){
		this.boneIterator.index = start.id;
		return this.boneIterator;
	}
	
	/**
	 * Returns an iterator to iterate over all time line objects in the current animation.
	 * @return the object iterator
	 */
	public Iterator<Object> objectIterator(){
		return this.objectIterator(this.getCurrentKey().objectRefs[0]);
	}
	
	/**
	 * Returns an iterator to iterate over all time line objects in the current animation starting at a given root.
	 * @param start the object reference to start at
	 * @return the object iterator
	 */
	public Iterator<Object> objectIterator(ObjectRef start){
		this.objectIterator.index = start.id;
		return this.objectIterator;
	}
	
	/**
	 * An iterator to iterate over all time line objects in the current animation.
	 * @author Trixt0r
	 *
	 */
	class ObjectIterator implements Iterator<Object>{
		int index = 0;
		
		public boolean hasNext() {
			return index < getCurrentKey().objectRefs.length;
		}

		
		public Object next() {
			return unmappedTweenedKeys[getCurrentKey().objectRefs[index++].timeline].object();
		}

		
		public void remove() {
			throw new SpriterException("remove() is not supported by this iterator!");
		}
		
	}
	
	/**
	 * An iterator to iterate over all time line bones in the current animation.
	 * @author Trixt0r
	 *
	 */
	class BoneIterator implements Iterator<Bone>{
		int index = 0;
		
		public boolean hasNext() {
			return index < getCurrentKey().boneRefs.length;
		}

		public Bone next() {
			return unmappedTweenedKeys[getCurrentKey().boneRefs[index++].timeline].object();
		}

		public void remove() {
			throw new SpriterException("remove() is not supported by this iterator!");
		}
	}

	/**
	 * A listener to listen for specific events which can occur during the runtime of a {@link Player} instance.
	 * @author Trixt0r
	 *
	 */
	public static interface PlayerListener{
		
		/**
		 * Gets called if the current animation has reached it's end or it's beginning (depends on the current set {@link Player#speed}).
		 * @param animation the animation which finished.
		 */
		public void animationFinished(Animation animation);
		
		/**
		 * Gets called if the animation of the player gets changed.
		 * If {@link Player#setAnimation(Animation)} gets called and the new animation is the same as the previous one, this method will not be called.
		 * @param oldAnim the old animation
		 * @param newAnim the new animation
		 */
		public void animationChanged(Animation oldAnim, Animation newAnim);
		
		/**
		 * Gets called before a player updates the current animation.
		 * @param player the player which is calling this method.
		 */
		public void preProcess(Player player);
		
		/**
		 * Gets called after a player updated the current animation.
		 * @param player the player which is calling this method.
		 */
		public void postProcess(Player player);
		
		/**
		 * Gets called if the mainline key gets changed.
		 * If {@link Player#speed} is big enough it can happen that mainline keys between the previous and the new mainline key will be ignored.
		 * @param prevKey the previous mainline key
		 * @param newKey the new mainline key
		 */
		public void mainlineKeyChanged(Mainline.Key prevKey, Mainline.Key newKey);
	}
	
	/**
	 * An attachment is an abstract object which can be attached to a {@link Player} object.
	 * An attachment extends a {@link Bone} which means that {@link Bone#position}, {@link Bone#scale} and {@link Bone#angle} can be set to change the relative position to its {@link Attachment#parent}
	 * The {@link Player} object will make sure that the attachment will be transformed relative to its {@link Attachment#parent}.
	 * @author Trixt0r
	 *
	 */
	public static abstract class Attachment extends Timeline.Key.Bone{
		
		private Bone parent;
		private final Point positionTemp, scaleTemp;
		private float angleTemp;
		
		/**
		 * Creates a new attachment 
		 * @param parent the parent of this attachment
		 */
		public Attachment(Bone parent){
			this.positionTemp = new Point();
			this.scaleTemp = new Point();
			this.setParent(parent);
		}
		
		/**
		 * Sets the parent of this attachment.
		 * @param parent the parent
		 * @throws SpriterException if parent is <code>null</code>
		 */
		public void setParent(Bone parent){
			if(parent == null) throw new SpriterException("The parent cannot be null!");
			this.parent = parent;
		}
		
		/**
		 * Returns the current set parent.
		 * @return the parent
		 */
		public Bone getParent(){
			return this.parent;
		}
		
		final void update(){
			//Save relative positions
			this.positionTemp.set(super.position);
			this.scaleTemp.set(super.scale);
			this.angleTemp = super.angle;
			
			super.unmap(parent);
			this.setPosition(super.position.x, super.position.y);
			this.setScale(super.scale.x, super.scale.y);
			this.setAngle(super.angle);
			
			//Load realtive positions
			super.position.set(this.positionTemp);
			super.scale.set(this.scaleTemp);
			super.angle = this.angleTemp;
		}
		/**
		 * Sets the position to the given coordinates.
		 * @param x the x coordinate
		 * @param y the y coordinate
		 */
		protected abstract void setPosition(float x, float y);
		/**
		 * Sets the scale to the given scale.
		 * @param xscale the scale in x direction
		 * @param yscale the scale in y direction
		 */
		protected abstract void setScale(float xscale, float yscale);
		/**
		 * Sets the angle to the given one.
		 * @param angle the angle in degrees
		 */
		protected abstract void setAngle(float angle);
	}
}
