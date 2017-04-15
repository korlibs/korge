package com.brashmonkey.spriter;

import com.brashmonkey.spriter.Entity.ObjectInfo;

/**
 * Represents a time line in a Spriter SCML file.
 * A time line holds an {@link #id}, a {@link #name} and at least one {@link Key}.
 * @author Trixt0r
 *
 */
public class Timeline {

    public final Key[] keys;
    private int keyPointer = 0;
    public final int id;
    public final String name;
    public final ObjectInfo objectInfo;
    
    Timeline(int id, String name, ObjectInfo objectInfo, int keys){
    	this.id = id;
    	this.name = name;
    	this.objectInfo = objectInfo;
    	this.keys = new Key[keys];
    }
    
    void addKey(Key key){
    	this.keys[keyPointer++] = key;
    }
    
    /**
     * Returns a {@link Key} at the given index
     * @param index the index of the key.
     * @return the key with the given index.
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Key getKey(int index){
    	return this.keys[index];
    }
    
    public String toString(){
    	String toReturn = getClass().getSimpleName()+"|[id:"+id+", name: "+name+", object_info: "+objectInfo;
    	for(Key key: keys)
    		toReturn += "\n"+key;
    	toReturn+="]";
    	return toReturn;
    }
    
    /**
     * Represents a time line key in a Spriter SCML file.
     * A key holds an {@link #id}, a {@link #time}, a {@link #spin}, an {@link #object()} and a {@link #curve}.
     * @author Trixt0r
     *
     */
    public static class Key{
    	
    	public final int id, spin;
    	public int time;
    	public final Curve curve;
    	public boolean active;
    	private Object object;
    	
    	public Key(int id, int time, int spin, Curve curve){
    		this.id = id;
    		this.time = time;
    		this.spin = spin;
    		this.curve = curve;
    	}
    	
    	public Key(int id,int time, int spin){
    		this(id, time, 1, new Curve());
    	}
    	
    	public Key(int id, int time){
    		this(id, time, 1);
    	}
    	
    	public Key(int id){
    		this(id, 0);
    	}
    	
    	public void setObject(Object object){
    		if(object == null) throw new IllegalArgumentException("object can not be null!");
    		this.object = object;
    	}
    	
    	public Object object(){
    		return this.object;
    	}
    	
    	public String toString(){
    		return getClass().getSimpleName()+"|[id: "+id+", time: "+time+", spin: "+spin+"\ncurve: "+curve+"\nobject:"+object+"]";
    	}
    	
    	/**
    	 * Represents a bone in a Spriter SCML file.
    	 * A bone holds a {@link #position}, {@link #scale}, an {@link #angle} and a {@link #pivot}.
    	 * Bones are the only objects which can be used as a parent for other tweenable objects.
    	 * @author Trixt0r
    	 *
    	 */
    	public static class Bone{
        	public final Point position, scale, pivot;
        	public float angle;
        	
        	public Bone(Point position, Point scale, Point pivot, float angle){
        		this.position = new Point(position);
        		this.scale =  new Point(scale);
        		this.angle = angle;
        		this.pivot =  new Point(pivot);
        	}
        	
        	public Bone(Bone bone){
        		this(bone.position, bone.scale, bone.pivot, bone.angle);
        	}
        	
        	public Bone(Point position){
        		this(position, new Point(1f,1f), new Point(0f, 1f), 0f);
        	}
        	
        	public Bone(){
        		this(new Point());
        	}
        	
        	/**
        	 * Returns whether this instance is a Spriter object or a bone.
        	 * @return true if this instance is a Spriter bone
        	 */
        	public boolean isBone(){
        		return !(this instanceof Object);
        	}
        	
        	/**
        	 * Sets the values of this bone to the values of the given bone
        	 * @param bone the bone
        	 */
        	public void set(Bone bone){
        		this.set(bone.position, bone.angle, bone.scale, bone.pivot);
        	}
        	
        	/**
        	 * Sets the given values for this bone.
			 * @param x the new position in x direction
			 * @param y the new position in y direction
			 * @param angle the new angle
			 * @param scaleX the new scale in x direction
			 * @param scaleY the new scale in y direction
			 * @param pivotX the new pivot in x direction
			 * @param pivotY the new pivot in y direction
        	 */
        	public void set(float x, float y, float angle, float scaleX, float scaleY, float pivotX, float pivotY){
        		this.angle = angle;
        		this.position.set(x, y);
        		this.scale.set(scaleX, scaleY);
        		this.pivot.set(pivotX, pivotY);
        	}
        	
        	/**
        	 * Sets the given values for this bone.
			 * @param position the new position
			 * @param angle the new angle
			 * @param scale the new scale
			 * @param pivot the new pivot
        	 */
        	public void set(Point position, float angle, Point scale, Point pivot){
        		this.set(position.x, position.y, angle, scale.x, scale.y, pivot.x, pivot.y);
        	}
        	
        	/**
        	 * Maps this bone from it's parent's coordinate system to a global one.
        	 * @param parent the parent bone of this bone
        	 */
        	public void unmap(Bone parent){
        		this.angle *= Math.signum(parent.scale.x)*Math.signum(parent.scale.y);
        		this.angle += parent.angle;
        		this.scale.scale(parent.scale);
        		this.position.scale(parent.scale);
        		this.position.rotate(parent.angle);
        		this.position.translate(parent.position);
        	}
        	
        	/**
        	 * Maps this from it's global coordinate system to the parent's one.
        	 * @param parent the parent bone of this bone
        	 */
        	public void map(Bone parent){
        		this.position.translate(-parent.position.x, -parent.position.y);
        		this.position.rotate(-parent.angle);
        		this.position.scale(1f/parent.scale.x, 1f/parent.scale.y);
        		this.scale.scale(1f/parent.scale.x, 1f/parent.scale.y);
        		this.angle -=parent.angle;
    			this.angle *= Math.signum(parent.scale.x)*Math.signum(parent.scale.y);
        	}
        	
        	public String toString(){
        		return getClass().getSimpleName()+"|position: "+position+", scale: "+scale+", angle: "+angle;
        	}
    	}
    	
    	
    	/**
    	 * Represents an object in a Spriter SCML file.
    	 * A file has the same properties as a bone with an alpha and file extension.
    	 * @author Trixt0r
    	 *
    	 */
    	public static class Object extends Bone{
    		
    		public float alpha;
    		public final FileReference ref;

			public Object(Point position, Point scale, Point pivot, float angle, float alpha, FileReference ref) {
				super(position, scale, pivot, angle);
				this.alpha = alpha;
				this.ref = ref;
			}
			
			public Object(Point position) {
				this(position, new Point(1f,1f), new Point(0f,1f), 0f, 1f, new FileReference(-1,-1));
			}
			
			public Object(Object object){
				this(object.position.copy(), object.scale.copy(),object.pivot.copy(),object.angle,object.alpha,object.ref);
			}
			
			public Object(){
				this(new Point());
			}
			
			/**
        	 * Sets the values of this object to the values of the given object.
        	 * @param object the object
        	 */
			public void set(Object object){
				this.set(object.position, object.angle, object.scale, object.pivot, object.alpha, object.ref);
			}
			
			/**
        	 * Sets the given values for this object.
			 * @param x the new position in x direction
			 * @param y the new position in y direction
			 * @param angle the new angle
			 * @param scaleX the new scale in x direction
			 * @param scaleY the new scale in y direction
			 * @param pivotX the new pivot in x direction
			 * @param pivotY the new pivot in y direction
			 * @param alpha the new alpha value
			 * @param folder the new folder index
			 * @param file the new file index
        	 */
			public void set(float x, float y, float angle, float scaleX, float scaleY, float pivotX, float pivotY, float alpha, int folder, int file){
				super.set(x, y, angle, scaleX, scaleY, pivotX, pivotY);
				this.alpha = alpha;
				this.ref.folder = folder;
				this.ref.file = file;
			}
			
			/**
        	 * Sets the given values for this object.
			 * @param position the new position
			 * @param angle the new angle
			 * @param scale the new scale
			 * @param pivot the new pivot
			 * @param alpha the new alpha value
			 * @param fileRef the new file reference
        	 */
        	public void set(Point position, float angle, Point scale, Point pivot, float alpha, FileReference fileRef){
        		this.set(position.x, position.y, angle, scale.x, scale.y, pivot.x, pivot.y, alpha , fileRef.folder, fileRef.file);
        	}
			
			public String toString(){
				return super.toString()+", pivot: "+pivot+", alpha: "+alpha+", reference: "+ref;
			}
    		
    	}
    }

}
