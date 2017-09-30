package com.brashmonkey.spriter;

import static  com.brashmonkey.spriter.Calculator.*;
import static com.brashmonkey.spriter.Interpolator.*;

/**
 * Represents a curve in a Spriter SCML file.
 * An instance of this class is responsible for tweening given data.
 * The most important method of this class is {@link #tween(float, float, float)}.
 * Curves can be changed with sub curves {@link Curve#subCurve}.
 * @author Trixt0r
 *
 */
public class Curve {
	
	/**
	 * Represents a curve type in a Spriter SCML file.
	 * @author Trixt0r
	 *
	 */
	public static enum Type {
		Instant, Linear, Quadratic, Cubic, Quartic, Quintic, Bezier;
	}
	
	/**
	 * Returns a curve type based on the given curve name.
	 * @param name the name of the curve
	 * @return the curve type. {@link Type#Linear} is returned as a default type.
	 */
	public static Type getType(String name){
		if(name.equals("instant")) return Type.Instant;
		else if(name.equals("quadratic")) return Type.Quadratic;
		else if(name.equals("cubic")) return Type.Cubic;
		else if(name.equals("quartic")) return Type.Quartic;
		else if(name.equals("quintic")) return Type.Quintic;
		else if(name.equals("bezier")) return Type.Bezier;
		else return Type.Linear;
	}
	
	private Type type;
	/**
	 * The sub curve of this curve, which can be <code>null</code>.
	 */
	public Curve subCurve;
	/**
	 * The constraints of a curve which will affect a curve of the types different from {@link Type#Linear} and {@link Type#Instant}.
	 */
	public final Constraints constraints = new Constraints(0, 0, 0, 0);
	
	/**
	 * Creates a new linear curve.
	 */
	public Curve(){
		this(Type.Linear);
	}
	
	/**
	 * Creates a new curve with the given type.
	 * @param type the curve type
	 */
	public Curve(Type type){
		this(type, null);
	}
	
	/**
	 * Creates a new curve with the given type and sub cuve.
	 * @param type the curve type
	 * @param subCurve the sub curve. Can be <code>null</code>
	 */
	public Curve(Type type, Curve subCurve){
		this.setType(type);
		this.subCurve = subCurve;
	}
	
	/**
	 * Sets the type of this curve.
	 * @param type the curve type.
	 * @throws SpriterException if the type is <code>null</code>
	 */
	public void setType(Type type){
		if(type == null) throw new SpriterException("The type of a curve cannot be null!");
		this.type = type;
	}
	
	/**
	 * Returns the type of this curve.
	 * @return the curve type
	 */
	public Type getType(){
		return this.type;
	}

	
	private float lastCubicSolution = 0f;
	/**
	 * Returns a new value based on the given values.
	 * Tweens the weight with the set sub curve.
	 * @param a the start value
	 * @param b the end value
	 * @param t the weight which lies between 0.0 and 1.0
	 * @return tweened value
	 */
	public float tween(float a, float b, float t){
		t = tweenSub(0f,1f,t);
		switch(type){
		case Instant: return a;
		case Linear: return linear(a, b, t);
		case Quadratic: return quadratic(a, linear(a, b, constraints.c1), b, t);
		case Cubic: return cubic(a, linear(a, b, constraints.c1), linear(a, b, constraints.c2), b, t);
		case Quartic: return quartic(a, linear(a, b, constraints.c1), linear(a, b, constraints.c2),	linear(a, b, constraints.c3), b, t);
		case Quintic: return quintic(a, linear(a, b, constraints.c1), linear(a, b, constraints.c2),	linear(a, b, constraints.c3), linear(a, b, constraints.c4), b, t);
		case Bezier: float cubicSolution = solveCubic(3f*(constraints.c1-constraints.c3) + 1f, 3f*(constraints.c3-2f*constraints.c1), 3f*constraints.c1, -t);
					 if(cubicSolution == NO_SOLUTION) cubicSolution = lastCubicSolution;
					 else lastCubicSolution = cubicSolution;
					 return linear(a, b, bezier(cubicSolution, 0f, constraints.c2, constraints.c4, 1f));
		default: return linear(a, b, t);
		}
	}
	
	/**
	 * Interpolates the given two points with the given weight and saves the result in the target point.
	 * @param a the start point
	 * @param b the end point
	 * @param t the weight which lies between 0.0 and 1.0
	 * @param target the target point to save the result in
	 */
	public void tweenPoint(Point a, Point b, float t, Point target){
		target.set(this.tween(a.x, b.x, t), this.tween(a.y, b.y, t));
	}
	
	private float tweenSub(float a, float b, float t){
		if(this.subCurve != null) return subCurve.tween(a, b, t);
		else return t;
	}
	
	/**
	 * Returns a tweened angle based on the given angles, weight and the spin.
	 * @param a the start angle
	 * @param b the end angle
	 * @param t the weight which lies between 0.0 and 1.0
	 * @param spin the spin, which is either 0, 1 or -1
	 * @return tweened angle
	 */
	public float tweenAngle(float a, float b, float t, int spin){
	    if(spin>0){
	        if(b-a < 0)
	            b+=360;
	    }
	    else if(spin < 0){
	        if(b-a > 0)
	            b-=360;
	    }
	    else return a;

	    return tween(a, b, t);
	}
	
	/**
	 * @see {@link #tween(float, float, float)}
	 */
	public float tweenAngle(float a, float b, float t){
		t = tweenSub(0f,1f,t);
		switch(type){
		case Instant: return a;
		case Linear: return linearAngle(a, b, t);
		case Quadratic: return quadraticAngle(a, linearAngle(a, b, constraints.c1), b, t);
		case Cubic: return cubicAngle(a, linearAngle(a, b, constraints.c1), linearAngle(a, b, constraints.c2), b, t);
		case Quartic: return quarticAngle(a, linearAngle(a, b, constraints.c1), linearAngle(a, b, constraints.c2),	linearAngle(a, b, constraints.c3), b, t);
		case Quintic: return quinticAngle(a, linearAngle(a, b, constraints.c1), linearAngle(a, b, constraints.c2),	linearAngle(a, b, constraints.c3), linearAngle(a, b, constraints.c4), b, t);
		case Bezier: float cubicSolution = solveCubic(3f*(constraints.c1-constraints.c3) + 1f, 3f*(constraints.c3-2f*constraints.c1), 3f*constraints.c1, -t);
					 if(cubicSolution == NO_SOLUTION) cubicSolution = lastCubicSolution;
					 else lastCubicSolution = cubicSolution;
					 return linearAngle(a, b, bezier(cubicSolution, 0f, constraints.c2, constraints.c4, 1f));
		default: return linearAngle(a, b, t);
		}
	}
	
	public String toString(){
		return getClass().getSimpleName()+"|["+type+":"+constraints+", subCurve: "+subCurve+"]";
	}
	
	/**
	 * Represents constraints for a curve.
	 * Constraints are important for curves which have a order higher than 1.
	 * @author Trixt0r
	 *
	 */
	public static class Constraints{
		public float c1, c2, c3, c4;
		
		public Constraints(float c1, float c2, float c3, float c4){
			this.set(c1, c2, c3, c4);
		}
		
		public void set(float c1, float c2, float c3, float c4){
			this.c1 = c1;
			this.c2 = c2;
			this.c3 = c3;
			this.c4 = c4;
		}
		
		public String toString(){
			return getClass().getSimpleName()+"| [c1:"+c1+", c2:"+c2+", c3:"+c3+", c4:"+c4+"]";
		}
	}

}
