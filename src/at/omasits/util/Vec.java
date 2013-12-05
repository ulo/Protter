package at.omasits.util;

public class Vec {
	public float x;
	public float y;
	
	public Vec(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public Vec scaled(float scalar) {
		return new Vec(x*scalar, y*scalar);
	}
	
	public float length() {
		return (float) Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
	}
	
	public Vec normalized() {
		float length = length();
		return new Vec(x/length, y/length);
	}
	
	public Vec added(Vec v2) {
		return new Vec(x+v2.x, y+v2.y);
	}
	
	public Vec orthogonalized() {
		return new Vec(-y, x);
	}
	
	/*
	 * static functions
	 */
	public static Vec midpoint(Vec v1, Vec v2) {
		return new Vec((v1.x+v2.x)/2, (v1.y+v2.y)/2);
	}
	
	public static Vec diff(Vec v1, Vec v2) {
		return new Vec(v1.x-v2.x, v1.y-v2.y);
	}
	
	
}
