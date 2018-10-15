package com.brashmonkey.spriter;

/**
 * An Exception which will be thrown if a Spriter specific issue happens at runtime.
 * @author Trixt0r
 *
 */
public class SpriterException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public SpriterException(String message){
		super(message);
	}

}
