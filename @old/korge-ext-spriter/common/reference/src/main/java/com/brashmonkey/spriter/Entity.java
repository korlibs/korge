package com.brashmonkey.spriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Represents an entity of a Spriter SCML file.
 * An entity holds {@link Animation}s, an {@link #id}, a {@link #name}.
 * {@link #characterMaps} and {@link #objectInfos} may be empty.
 * @author Trixt0r
 *
 */
public class Entity {

    public final int id;
    public final String name;
    private final Animation[] animations;
    private int animationPointer = 0;
    private final HashMap<String, Animation> namedAnimations;
    private final CharacterMap[] characterMaps;
    private int charMapPointer = 0;
    private final ObjectInfo[] objectInfos;
    private int objInfoPointer = 0;
	
	Entity(int id, String name, int animations, int characterMaps, int objectInfos){
		this.id = id;
		this.name = name;
		this.animations = new Animation[animations];
		this.characterMaps = new CharacterMap[characterMaps];
		this.objectInfos = new ObjectInfo[objectInfos];
		this.namedAnimations = new HashMap<String, Animation>();
	}
	
	void addAnimation(Animation anim){
		this.animations[animationPointer++] = anim;
		this.namedAnimations.put(anim.name, anim);
	}
	
	/**
	 * Returns an {@link Animation} with the given index.
	 * @param index the index of the animation
	 * @return the animation with the given index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public Animation getAnimation(int index){
		return this.animations[index];
	}
	
	/**
	 * Returns an {@link Animation} with the given name.
	 * @param name the name of the animation
	 * @return the animation with the given name or null if no animation exists with the given name
	 */
	public Animation getAnimation(String name){
		return this.namedAnimations.get(name);
	}
	
	/**
	 * Returns the number of animations this entity holds.
	 * @return the number of animations
	 */
	public int animations(){
		return this.animations.length;
	}
	
	/**
	 * Returns whether this entity contains the given animation.
	 * @param anim the animation to check
	 * @return true if the given animation is in this entity, false otherwise.
	 */
	public boolean containsAnimation(Animation anim){
		for(Animation a: this.animations)
			if(a == anim) return true;
		return false;
	}
	
	/**
	 * Returns the animation with the most number of time lines in this entity.
	 * @return animation with the maximum amount of time lines.
	 */
	public Animation getAnimationWithMostTimelines(){
		Animation maxAnim = getAnimation(0);
		for(Animation anim: this.animations){
			if(maxAnim.timelines() < anim.timelines()) maxAnim = anim;
		}
		return maxAnim;
	}
    
    /**
     * Returns a {@link CharacterMap} with the given name.
     * @param name name of the character map
     * @return the character map or null if no character map exists with the given name
     */
    public CharacterMap getCharacterMap(String name){
    	for(CharacterMap map: this.characterMaps)
    		if(map.name.equals(name)) return map;
    	return null;
    }
    
    void addCharacterMap(CharacterMap map){
    	this.characterMaps[charMapPointer++] = map;
    }
    
    void addInfo(ObjectInfo info){
    	this.objectInfos[objInfoPointer++] = info;
    }
    
    /**
     * Returns an {@link ObjectInfo} with the given index.
     * @param index the index of the object info
     * @return the object info
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public ObjectInfo getInfo(int index){
    	return this.objectInfos[index];
    }
    
    /**
     * Returns an {@link ObjectInfo} with the given name.
     * @param name name of the object info
     * @return object info or null if no object info exists with the given name
     */
    public ObjectInfo getInfo(String name){
    	for(ObjectInfo info: this.objectInfos)
    		if(info.name.equals(name)) return info;
    	return null;
    }
    
    /**
     * Returns an {@link ObjectInfo} with the given name and the given {@link ObjectType} type.
     * @param name the name of the object info
     * @param type the type if the object info
     * @return the object info or null if no object info exists with the given name and type
     */
    public ObjectInfo getInfo(String name, ObjectType type){
    	ObjectInfo info = this.getInfo(name);
    	if(info != null && info.type == type) return info;
    	else return null;
    }
    
    /**
     * Represents the object types Spriter supports.
     * @author Trixt0r
     *
     */
    public static enum ObjectType{
    	Sprite, Bone, Box, Point, Skin;
    	
    	/**
    	 * Returns the object type for the given name
    	 * @param name the name of the type
    	 * @return the object type, Sprite is the default value
    	 */
    	public static ObjectType getObjectInfoFor(String name){
    		if(name.equals("bone")) return Bone;
    		else if(name.equals("skin")) return Skin;
    		else if(name.equals("box")) return Box;
    		else if(name.equals("point")) return Point;
    		else return Sprite;
    	}
    }
    
    /**
     * Represents the object info in a Spriter SCML file.
     * An object info holds a {@link #type} and a {@link #name}.
     * If the type is a Sprite it holds a list of frames. Otherwise it has a {@link #size} for debug drawing purposes.
     * @author Trixt0r
     *
     */
    public static class ObjectInfo{
    	public final ObjectType type;
    	public final List<FileReference> frames;
    	public final String name;
    	public final Dimension size;
    	
    	ObjectInfo(String name, ObjectType type, Dimension size, List<FileReference> frames){
    		this.type = type;
    		this.frames = frames;
    		this.name = name;
    		this.size = size;
    	}
    	
    	ObjectInfo(String name, ObjectType type, Dimension size){
    		this(name, type, size, new ArrayList<FileReference>());
    	}
    	
    	ObjectInfo(String name, ObjectType type, List<FileReference> frames){
    		this(name, type, new Dimension(0,0), frames);
    	}
    	
    	public String toString(){
    		return name + ": "+ type + ", size: "+size+"|frames:\n"+frames;
    	}
    }
    
    /**
     * Represents a Spriter SCML character map.
     * A character map maps {@link FileReference}s to {@link FileReference}s.
     * It holds an {@link CharacterMap#id} and a {@link CharacterMap#name}.
     * @author Trixt0r
     *
     */
    public static class CharacterMap extends HashMap<FileReference, FileReference>{
    	private static final long serialVersionUID = 6062776450159802283L;
    	
    	public final int id;
    	public final String name;
    	
    	public CharacterMap(int id, String name){
    		this.id = id;
    		this.name = name;
    	}
    	
    	/**
    	 * Returns the mapped reference for the given key.
    	 * @param key the key of the reference
    	 * @return The mapped reference if the key is in this map, otherwise the given key itself is returned.
    	 */
    	public FileReference get(FileReference key){
    		if(!super.containsKey(key)) return key;
    		else return super.get(key);
    	}
    }
    
    public String toString(){
    	String toReturn = getClass().getSimpleName()+"|[id: "+id+", name: "+name+"]";
    	toReturn +="Object infos:\n";
    	for(ObjectInfo info: this.objectInfos)
    		toReturn += "\n"+info;
    	toReturn +="Character maps:\n";
    	for(CharacterMap map: this.characterMaps)
    		toReturn += "\n"+map;
    	toReturn +="Animations:\n";
    	for(Animation animaton: this.animations)
    		toReturn += "\n"+animaton;
    	toReturn+="]";
    	return toReturn;
    }

}
