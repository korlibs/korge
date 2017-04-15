package com.brashmonkey.spriter;

/**
 * Represents a dimension in a 2D space.
 * A dimension has a width and a height.
 * @author Trixt0r
 *
 */
public class Dimension {
	
	public float width, height;
	
	/**
	 * Creates a new dimension with the given size.
	 * @param width the width of the dimension
	 * @param height the height of the dimension
	 */
	public Dimension(float width, float height){
		this.set(width, height);
	}
	
	/**
	 * Creates a new dimension with the given size.
	 * @param size the size
	 */
	public Dimension(Dimension size){
		this.set(size);
	}
	
	/**
	 * Sets the size of this dimension to the given size.
	 * @param width the width of the dimension
	 * @param height the height of the dimension
	 */
	public void set(float width, float height){
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Sets the size of this dimension to the given size.
	 * @param size the size
	 */
	public void set(Dimension size){
		this.set(size.width, size.height);
	}
	
	public String toString(){
		return "["+width+"x"+height+"]";
	}

}
