package com.brashmonkey.spriter;

import java.util.HashMap;

/**
 * A loader is responsible for loading all resources.
 * Since this library is meant to be as generic as possible, it cannot be assumed how to load a resource. Because of this this class has to be abstract.
 * This class takes care of loading all resources a {@link Data} instance contains.
 * To load all resources an instance relies on {@link #loadResource(FileReference)} which has to implemented with the backend specific methods.
 * 
 * @author Trixt0r
 *
 * @param <R> The backend specific resource. In general such a resource is called "sprite", "texture" or "image".
 */
public abstract class Loader<R> {
	
	/**
	 * Contains all loaded resources if not {@link #isDisposed()}.
	 */
	protected final HashMap<FileReference, R> resources;
	
	/**
	 * The current set data containing {@link Folder}s and {@link File}s.
	 */
	protected Data data;
	
	/**
	 * The root path to the previous loaded Spriter SCML file.
	 */
	protected String root = "";
	
	private boolean disposed;
	
	/**
	 * Creates a loader with the given Spriter data.
	 * @param data the generated Spriter data
	 */
	public Loader(Data data){
		this.data = data;
		this.resources = new HashMap<FileReference, R>(100);
	}
	
	/**
	 * Loads a resource.
	 * The path to the file can be resolved with {@link #root} and {@link #data}.
	 * I recommend using {@link Data#getFile(FileReference)}. Then the path to the resource is {@link File#name} relative to {@link #root}.
	 * @param ref the reference to load
	 * @return the loaded resource
	 */
	protected abstract R loadResource(FileReference ref);
	
	/**
	 * Called when all resources from {@link #data} have been loaded.
	 */
	protected void finishLoading(){}
	
	/**
	 * Called before all resources get loaded.
	 */
	protected void beginLoading(){}
	
	/**
	 * Loads all resources indicated by {@link #data}.
	 * @param root the root folder of the previously loaded Spriter SCML file
	 */
	public void load(String root){
		this.root = root;
		this.beginLoading();
		for(Folder folder: data.folders){
			for(File file: folder.files){
				//if(new java.io.File(root+"/"+file.name).exists()){
					FileReference ref = new FileReference(folder.id, file.id);
					this.resources.put(ref, this.loadResource(ref));
				//}
			}
		}
		this.disposed = false;
		this.finishLoading();
	}
	
	/**
	 * Loads all resources indicated by {@link #data}.
	 * @param file the previously loaded Spriter SCML file
	 */
	public void load(java.io.File file){
		this.load(file.getParent());
	}
	
	/**
	 * Returns a resource the given reference is pointing to.
	 * @param ref the reference pointing to a resource
	 * @return the resource or <code>null</code> if the resource is not loaded yet.
	 */
	public R get(FileReference ref){
		return this.resources.get(ref);
	}
	
	/**
	 * Removes all loaded resources from the internal reference-resource map.
	 * Override this method and dispose all your resources. After that call {@link #dispose()} of the super class.
	 */
	public void dispose(){
		resources.clear();
		data = null;
		root = "";
		disposed = true;
	}
	
	/**
	 * Returns whether this loader has been disposed or not.
	 * @return true if this loader is disposed
	 */
	public boolean isDisposed(){
		return disposed;
	}

}
