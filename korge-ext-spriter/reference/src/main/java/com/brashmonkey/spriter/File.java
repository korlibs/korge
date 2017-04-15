package com.brashmonkey.spriter;

/**
 * Represents a file in a Spriter SCML file.
 * A file has an {@link #id}, a {@link #name}.
 * A {@link #size} and a {@link #pivot} point, i.e. origin of an image do not have to be set since a file can be a sound file.
 * @author Trixt0r
 *
 */
public class File {

    public final int id;
    public final String name;
    public final Dimension size;
    public final Point pivot;
    
    File(int id, String name, Dimension size, Point pivot){
    	this.id = id;
    	this.name = name;
    	this.size = size;
    	this.pivot = pivot;
    }
    
    /**
     * Returns whether this file is a sprite, i.e. an image which is going to be animated, or not. 
     * @return whether this file is a sprite or not.
     */
    public boolean isSprite(){
    	return pivot != null && size != null;
    }
    
    public String toString(){
    	return getClass().getSimpleName()+"|[id: "+id+", name: "+name+", size: "+size+", pivot: "+pivot;
    }

}
