package com.brashmonkey.spriter;

/**
 * A utility class to keep the code short.
 * A point is essentially that what you would expect if you think about a point in a 2D space.
 * It holds an x and y value. You can {@link #translate(Point)}, {@link #scale(Point)}, {@link #rotate(float)} and {@link #set(Point)} a point.
 * @author Trixt0r
 *
 */
public class Point {
	
	/**
	 * The x coordinates of this point.
	 */
	public float x;
	/**
	 * The y coordinates of this point.
	 */
	public float y;
	
	/**
	 * Creates a point at (0,0).
	 */
	public Point(){
		this(0,0);
	}
	
	/**
	 * Creates a point at the position of the given point.
	 * @param point the point to set this point at
	 */
	public Point(Point point){
		this(point.x, point.y);
	}
	
	/**
	 * Creates a point at (x, y).
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	public Point(float x, float y){
		this.set(x, y);
	}
	
	/**
	 * Sets this point to the given coordinates.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return this point for chained operations
	 */
	public Point set(float x, float y){
		this.x = x;
		this.y = y;
		return this;
	}
	
	/**
	 * Adds the given amount to this point.
	 * @param x the amount in x direction to add
	 * @param y the amount in y direction to add
	 * @return this point for chained operations
	 */
	public Point translate(float x, float y){
		return this.set(this.x+x, this.y+y);
	}
	
	/**
	 * Scales this point by the given amount.
	 * @param x the scale amount in x direction
	 * @param y the scale amount in y direction
	 * @return this point for chained operations
	 */
	public Point scale(float x, float y){
		return this.set(this.x*x, this.y*y);
	}
	
	/**
	 * Sets this point to the given point.
	 * @param point the new coordinates
	 * @return this point for chained operations
	 */
	public Point set(Point point){
		return this.set(point.x, point.y);
	}
	
	/**
	 * Adds the given amount to this point.
	 * @param amount the amount to add
	 * @return this point for chained operations
	 */
	public Point translate(Point amount){
		return this.translate(amount.x, amount.y);
	}
	
	/**
	 * Scales this point by the given amount.
	 * @param amount the amount to scale
	 * @return this point for chained operations
	 */
	public Point scale(Point amount){
		return this.scale(amount.x, amount.y);
	}
	
	/**
	 * Rotates this point around (0,0) by the given amount of degrees.
	 * @param degrees the angle to rotate this point
	 * @return this point for chained operations
	 */
	public Point rotate(float degrees){
		if(x != 0 || y != 0){
			float cos = Calculator.cosDeg(degrees);
			float sin = Calculator.sinDeg(degrees);
			
			float xx = x*cos-y*sin;
			float yy = x*sin+y*cos;
			
			this.x = xx;
			this.y = yy;
		}
		return this;
	}
	
	/**
	 * Returns a copy of this point with the current set values.
	 * @return a copy of this point
	 */
	public Point copy(){
		return new Point(x,y);
	}
	
	public String toString(){
		return "["+x+","+y+"]";
	}

}
