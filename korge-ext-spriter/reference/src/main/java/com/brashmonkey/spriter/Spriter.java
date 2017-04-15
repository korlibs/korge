package com.brashmonkey.spriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A utility class for managing multiple {@link Loader} and {@link Player} instances.
 * @author Trixt0r
 *
 */

@SuppressWarnings("rawtypes")
public class Spriter {
	
	private static Object[] loaderDependencies = new Object[1], drawerDependencies = new Object[1];
	private static Class<?>[] loaderTypes = new Class<?>[1], drawerTypes = new Class<?>[1];
	static{
		loaderTypes[0] = Data.class;
		drawerTypes[0] = Loader.class;
	}
	private static Class<? extends Loader> loaderClass;
	
	private static final HashMap<String, Data> loadedData = new HashMap<String, Data>();
	private static final List<Player> players = new ArrayList<Player>();
	private static final List<Loader> loaders = new ArrayList<Loader>();
	private static Drawer<?> drawer;
	private static final HashMap<Entity, Loader> entityToLoader = new HashMap<Entity, Loader>();
	private static boolean initialized = false;
	
	/**
	 * Sets the dependencies for implemented {@link Loader}.
	 * @param loaderDependencies the dependencies a loader has to get
	 */
	public static void setLoaderDependencies(Object... loaderDependencies){
		if(loaderDependencies == null) return;
		Spriter.loaderDependencies = new Object[loaderDependencies.length+1];
		System.arraycopy(loaderDependencies, 0, Spriter.loaderDependencies, 1, loaderDependencies.length);
		loaderTypes = new Class[loaderDependencies.length+1];
		loaderTypes[0] = Data.class;
		for(int i = 0; i< loaderDependencies.length; i++)
			loaderTypes[i+1] = loaderDependencies[i].getClass();
	}
	
	/**
	 * Sets the dependencies for implemented {@link Drawer}.
	 * @param drawerDependencies the dependencies a drawer has to get
	 */
	public static void setDrawerDependencies(Object... drawerDependencies){
		if(drawerDependencies == null) return;
		Spriter.drawerDependencies = new Object[drawerDependencies.length+1];
		Spriter.drawerDependencies[0] = null;
		System.arraycopy(drawerDependencies, 0, Spriter.drawerDependencies, 1, drawerDependencies.length);
		drawerTypes = new Class[drawerDependencies.length+1];
		drawerTypes[0] = Loader.class;
		for(int i = 0; i< drawerDependencies.length; i++)
			if(drawerDependencies[i] != null)
				drawerTypes[i+1] = drawerDependencies[i].getClass();
	}
	
	/**
	 * Initializes this class with the implemented {@link Loader} class and {@link Drawer} class.
	 * Before calling this method make sure that you have set all necessary dependecies with {@link #setDrawerDependencies(Object...)} and {@link #setLoaderDependencies(Object...)}.
	 * A drawer is created with this method.
	 * @param loaderClass the loader class
	 * @param drawerClass the drawer class
	 */
	public static void init(Class<? extends Loader> loaderClass, Class<? extends Drawer> drawerClass){
		Spriter.loaderClass = loaderClass;
		try {
			drawer = drawerClass.getDeclaredConstructor(drawerTypes).newInstance(drawerDependencies);
		} catch (Exception e) {
			e.printStackTrace();
		}
		initialized = drawer != null;
	}
	
	/**
	 * Loads a SCML file with the given path.
	 * @param scmlFile the path to the SCML file
	 */
	public static void load(String scmlFile){
		load(new File(scmlFile));
	}
	
	/**
	 * Loads the given SCML file.
	 * @param scmlFile the scml file
	 */
	public static void load(File scmlFile){
		try {
			load(new FileInputStream(scmlFile), scmlFile.getPath().replaceAll("\\\\", "/"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the given SCML stream pointing to a file saved at the given path.
	 * @param stream the SCML stream
	 * @param scmlFile the path to the SCML file
	 */
	public static void load(InputStream stream, String scmlFile){
		SCMLReader reader = new SCMLReader(stream);
		Data data = reader.data;
		loadedData.put(scmlFile, data);
		loaderDependencies[0] = data;
		try {
			Loader loader = loaderClass.getDeclaredConstructor(loaderTypes).newInstance(loaderDependencies);
			loader.load(new File(scmlFile));
			loaders.add(loader);
			for(Entity entity: data.entities)
				entityToLoader.put(entity, loader);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a new {@link Player} instance based on the given SCML file with the given entity index
	 * @param scmlFile name of the SCML file
	 * @param entityIndex the index of the entity
	 * @return a {@link Player} instance managed by this class
	 * @throws SpriterException if the given SCML file was not loaded yet
	 */
	public static Player newPlayer(String scmlFile, int entityIndex){
		return newPlayer(scmlFile, entityIndex, Player.class);
	}
	
	/**
	 * Creates a new {@link Player} instance based on the given SCML file with the given entity index and the given class extending a {@link Player}
	 * @param scmlFile name of the SCML file
	 * @param entityIndex the index of the entity
	 * @param playerClass the class extending a {@link Player} class, e.g. {@link PlayerTweener}.
	 * @return a {@link Player} instance managed by this class
	 * @throws SpriterException if the given SCML file was not loaded yet
	 */
	public static Player newPlayer(String scmlFile, int entityIndex, Class<? extends Player> playerClass){
		if(!loadedData.containsKey(scmlFile)) throw new SpriterException("You have to load \""+scmlFile+"\" before using it!");
		try {
			Player player = playerClass.getDeclaredConstructor(Entity.class).newInstance(loadedData.get(scmlFile).getEntity(entityIndex));
			players.add(player);
			return player;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Creates a new {@link Player} instance based on the given SCML file with the given entity name
	 * @param scmlFile name of the SCML file
	 * @param entityName name of the entity
	 * @return a {@link Player} instance managed by this class
	 * @throws SpriterException if the given SCML file was not loaded yet
	 */
	public static Player newPlayer(String scmlFile, String entityName){
		if(!loadedData.containsKey(scmlFile)) throw new SpriterException("You have to load \""+scmlFile+"\" before using it!");
		return newPlayer(scmlFile, loadedData.get(scmlFile).getEntityIndex(entityName));
	}
	
	/**
	 * Returns a loader for the given SCML filename.
	 * @param scmlFile the name of the SCML file
	 * @return the loader for the given SCML filename
	 * @throws NullPointerException if the SCML file was not loaded yet
	 */
	public static Loader<?> getLoader(String scmlFile){
		return entityToLoader.get(getData(scmlFile).getEntity(0));
	}

	/**
	 * Updates each created player by this class and immediately draws it.
	 * This method should only be called if you want to update and render on the same thread.
	 * @throws SpriterException if {@link #init(Class, Class)} was not called before
	 */
	@SuppressWarnings("unchecked")
	public static void updateAndDraw(){
		if(!initialized) throw new SpriterException("Call init() before updating!");
		for (int i = 0; i < players.size(); i++) {
			players.get(i).update();
			drawer.loader = entityToLoader.get(players.get(i).getEntity());
			drawer.draw(players.get(i));
		}
	}
	
	/**
	 * Updates each created player by this class.
	 * @throws SpriterException if {@link #init(Class, Class)} was not called before
	 */
	public static void update(){
		if(!initialized) throw new SpriterException("Call init() before updating!");
		for (int i = 0; i < players.size(); i++) {
			players.get(i).update();
		}
	}
	
	/**
	 * Draws each created player by this class.
	 * @throws SpriterException if {@link #init(Class, Class)} was not called before
	 */
	@SuppressWarnings("unchecked")
	public static void draw(){
		if(!initialized) throw new SpriterException("Call init() before drawing!");
		for (int i = 0; i < players.size(); i++) {
			drawer.loader = entityToLoader.get(players.get(i).getEntity());
			drawer.draw(players.get(i));
		}
	}
	
	/**
	 * Returns the drawer instance this class is using.
	 * @return the drawer which draws all players
	 */
	public static Drawer drawer(){
		return drawer;
	}
	
	/**
	 * Returns the data for the given SCML filename.
	 * @param fileName the name of the SCML file
	 * @return the data for the given SCML filename or null if not loaed yet
	 */
	public static Data getData(String fileName){
		return loadedData.get(fileName);
	}
	
	/**
	 * The number of players this class is managing.
	 * @return number of players
	 */
	public static int players(){
		return players.size();
	}
	
	/**
	 * Clears all previous created players, Spriter datas, disposes all loaders, deletes the drawer and resets all internal lists.
	 * After this methods was called {@link #init(Class, Class)} has to be called again so that everything works again.
	 */
	public static void dispose(){
		drawer = null;
		drawerDependencies = new Object[1];
		drawerTypes = new Class<?>[1];
		drawerTypes[0] = Loader.class;
		
		entityToLoader.clear();

		for (int i = 0; i < loaders.size(); i++) {
			loaders.get(i).dispose();
		}
	    loaders.clear();
		loadedData.clear();
		loaderClass = null;
		loaderTypes = new Class<?>[1];
		loaderTypes[0] = Data.class;
		loaderDependencies = new Object[1];
		
		players.clear();
		
		initialized = false;
	}

}
