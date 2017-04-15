package com.brashmonkey.spriter;

/**
 * Represents a mainline in a Spriter SCML file.
 * A mainline holds only keys and occurs only once in an animation.
 * The mainline is responsible for telling which draw order the sprites have
 * and how the objects are related to each other, i.e. which bone is the root and which objects are the children.
 * @author Trixt0r
 *
 */
public class Mainline {

    final Key[] keys;
    private int keyPointer = 0;
    
    public Mainline(int keys){
    	this.keys = new Key[keys];
    }
    
    public String toString(){
    	String toReturn = getClass().getSimpleName()+"|";
    	for(Key key: keys)
    		toReturn += "\n"+key;
    	toReturn+="]";
    	return toReturn;
    }
    
    public void addKey(Key key){
    	this.keys[keyPointer++] = key;
    }
    
    /**
     * Returns a {@link Key} at the given index.
     * @param index the index of the key
     * @return the key with the given index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Key getKey(int index){
    	return this.keys[index];
    }
    
    /**
     * Returns a {@link Key} before the given time.
     * @param time the time a key has to be before
     * @return a key which has a time value before the given one.
     * The first key is returned if no key was found.
     */
    public Key getKeyBeforeTime(int time){
    	Key found = this.keys[0];
    	for(Key key: this.keys){
    		if(key.time <= time) found = key;
    		else break;
    	}
    	return found;
    }
    
    /**
     * Represents a mainline key in a Spriter SCML file.
     * A mainline key holds an {@link #id}, a {@link #time}, a {@link #curve}
     * and lists of bone and object references which build a tree hierarchy.
     * @author Trixt0r
     *
     */
    public static class Key{
    	
    	public final int id, time;
    	final BoneRef[] boneRefs;
    	final ObjectRef[] objectRefs;
    	private int bonePointer = 0, objectPointer = 0;
    	public final Curve curve;
    	
    	public Key(int id, int time, Curve curve, int boneRefs, int objectRefs){
    		this.id = id;
    		this.time = time;
    		this.curve = curve;
    		this.boneRefs = new BoneRef[boneRefs];
    		this.objectRefs = new ObjectRef[objectRefs];
    	}
    	
    	/**
    	 * Adds a bone reference to this key.
    	 * @param ref the reference to add
    	 */
    	public void addBoneRef(BoneRef ref){
    		this.boneRefs[bonePointer++] = ref;
    	}
    	
    	/**
    	 * Adds a object reference to this key.
    	 * @param ref the reference to add
    	 */
    	public void addObjectRef(ObjectRef ref){
    		this.objectRefs[objectPointer++] = ref;
    	}
    	
    	/**
    	 * Returns a {@link BoneRef} with the given index.
    	 * @param index the index of the bone reference
    	 * @return the bone reference or null if no reference exists with the given index
    	 */
    	public BoneRef getBoneRef(int index){
    		if(index < 0 || index >= this.boneRefs.length) return null;
    		else return this.boneRefs[index];
    	}
    	
    	/**
    	 * Returns a {@link ObjectRef} with the given index.
    	 * @param index the index of the object reference
    	 * @return the object reference or null if no reference exists with the given index
    	 */
    	public ObjectRef getObjectRef(int index){
    		if(index < 0 || index >= this.objectRefs.length) return null;
    		else return this.objectRefs[index];
    	}
    	
    	/**
    	 * Returns a {@link BoneRef} for the given reference.
    	 * @param ref the reference to the reference in this key
    	 * @return a bone reference with the same time line as the given one
    	 */
        public BoneRef getBoneRef(BoneRef ref){
        	return getBoneRefTimeline(ref.timeline);
        }
        
        /**
    	 * Returns a {@link BoneRef} with the given time line index.
         * @param timeline the time line index
         * @return the bone reference with the given time line index or null if no reference exists with the given time line index
         */
        public BoneRef getBoneRefTimeline(int timeline){
    		for(BoneRef boneRef: this.boneRefs)
    			if(boneRef.timeline == timeline) return boneRef;
        	return null;
        }
        
        /**
    	 * Returns an {@link ObjectRef} for the given reference.
    	 * @param ref the reference to the reference in this key
    	 * @return an object reference with the same time line as the given one
    	 */
        public ObjectRef getObjectRef(ObjectRef ref){
        	return getObjectRefTimeline(ref.timeline);
        }
        
        /**
    	 * Returns a {@link ObjectRef} with the given time line index.
         * @param timeline the time line index
         * @return the object reference with the given time line index or null if no reference exists with the given time line index
         */
        public ObjectRef getObjectRefTimeline(int timeline){
    		for(ObjectRef objRef: this.objectRefs)
    			if(objRef.timeline == timeline) return objRef;
        	return null;
        }
    	
    	public String toString(){
        	String toReturn = getClass().getSimpleName()+"|[id:"+id+", time: "+time+", curve: ["+curve+"]";
        	for(BoneRef ref: boneRefs)
        		toReturn += "\n"+ref;
        	for(ObjectRef ref: objectRefs)
        		toReturn += "\n"+ref;
        	toReturn+="]";
        	return toReturn;
        }
    	
    	/**
    	 * Represents a bone reference in a Spriter SCML file.
    	 * A bone reference holds an {@link #id}, a {@link #timeline} and a {@link #key}.
    	 * A bone reference may have a parent reference.
    	 * @author Trixt0r
    	 *
    	 */
    	public static class BoneRef{
    		public final int id, key, timeline;
    		public final BoneRef parent;
    		
    		public BoneRef(int id, int timeline, int key, BoneRef parent){
    			this.id = id;
    			this.timeline = timeline;
    			this.key = key;
    			this.parent = parent;
    		}
    		
    		public String toString(){
    			int parentId = (parent != null) ? parent.id:-1;
    			return getClass().getSimpleName()+"|id: "+id+", parent:"+parentId+", timeline: "+timeline+", key: "+key;
    		}
    	}
    	
    	/**
    	 * Represents an object reference in a Spriter SCML file.
    	 * An object reference extends a {@link BoneRef} with a {@link #zIndex},
    	 * which indicates when the object has to be drawn.
    	 * @author Trixt0r
    	 *
    	 */
    	public static class ObjectRef extends BoneRef implements Comparable<ObjectRef>{
    		public final int zIndex;
    		
    		public ObjectRef(int id, int timeline, int key, BoneRef parent, int zIndex){
    			super(id, timeline, key, parent);
    			this.zIndex = zIndex;
    		}
    		
    		public String toString(){
    			return super.toString()+", z_index: "+zIndex;
    		}

			public int compareTo(ObjectRef o) {
				return (int)Math.signum(zIndex-o.zIndex);
			}
    	}
    }

}
