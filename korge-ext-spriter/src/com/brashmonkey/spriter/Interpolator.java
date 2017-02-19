package com.brashmonkey.spriter;



/**
 * Utility class for various interpolation techniques, Spriter is using.
 * @author Trixt0r
 *
 */
public class Interpolator {
	
	public static float linear(float a, float b, float t){
		return a+(b-a)*t;
	}
	
	public static float linearAngle(float a, float b, float t){
		return a + Calculator.angleDifference(b, a)*t;
	}
	
	public static float quadratic(float a, float b, float c, float t){
		return linear(linear(a, b, t), linear(b, c, t), t);
	}
	
	public static float quadraticAngle(float a, float b, float c, float t){
		return linearAngle(linearAngle(a, b, t), linearAngle(b, c, t), t);
	}
	
	public static float cubic(float a, float b, float c, float d, float t){
		return linear(quadratic(a, b, c, t), quadratic(b, c, d, t), t);
	}
	
	public static float cubicAngle(float a, float b, float c, float d, float t){
		return linearAngle(quadraticAngle(a, b, c, t), quadraticAngle(b, c, d, t), t);
	}
	
	public static float quartic(float a, float b, float c, float d, float e, float t){
		return linear(cubic(a, b, c, d, t), cubic(b, c, d, e, t), t);
	}
	
	public static float quarticAngle(float a, float b, float c, float d, float e, float t){
		return linearAngle(cubicAngle(a, b, c, d, t), cubicAngle(b, c, d, e, t), t);
	}
	
	public static float quintic(float a, float b, float c, float d, float e, float f, float t){
		return linear(quartic(a, b, c, d, e, t), quartic(b, c, d, e, f, t), t);
	}
	
	public static float quinticAngle(float a, float b, float c, float d, float e, float f, float t){
		return linearAngle(quarticAngle(a, b, c, d, e, t), quarticAngle(b, c, d, e, f, t), t);
	}
	
	public static float bezier(float t, float x1, float x2, float x3,float x4){
		return bezier0(t)*x1 + bezier1(t)*x2 + bezier2(t)*x3 + bezier3(t)*x4;
	}
	
	private static float bezier0(float t){
		float temp = t*t;
		return -temp*t + 3*temp - 3*t + 1;
	}
	
	private static float bezier1(float t){
		float temp = t*t;
		return 3*t*temp - 6*temp + 3*t;
	}
	
	private static float bezier2(float t){
		float temp = t*t;
		return -3*temp*t+3*temp;
	}
	
	private static float bezier3(float t){
		return t*t*t;
	}

}
