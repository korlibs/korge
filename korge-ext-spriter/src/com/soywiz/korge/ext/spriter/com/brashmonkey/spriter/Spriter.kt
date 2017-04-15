package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.ArrayList
import java.util.HashMap

/**
 * A utility class for managing multiple [Loader] and [Player] instances.
 * @author Trixt0r
 */

object Spriter {

	private var loaderDependencies = arrayOfNulls<Any>(1)
	private var drawerDependencies = arrayOfNulls<Any>(1)
	private var loaderTypes = arrayOfNulls<Class<*>>(1)
	private var drawerTypes = arrayOfNulls<Class<*>>(1)

	init {
		loaderTypes[0] = Data::class.java
		drawerTypes[0] = Loader::class.java
	}

	private var loaderClass: Class<out Loader<*>>? = null

	private val loadedData = HashMap<String, Data>()
	private val players = ArrayList<Player>()
	private val loaders = ArrayList<Loader<*>>()
	private var drawer: Drawer<*>? = null
	private val entityToLoader = HashMap<Entity, Loader<*>>()
	private var initialized = false

	/**
	 * Sets the dependencies for implemented [Loader].
	 * @param loaderDependencies the dependencies a loader has to get
	 */
	fun setLoaderDependencies(vararg loaderDependencies: Any) {
		if (loaderDependencies == null) return
		Spriter.loaderDependencies = arrayOfNulls<Any>(loaderDependencies.size + 1)
		System.arraycopy(loaderDependencies, 0, Spriter.loaderDependencies, 1, loaderDependencies.size)
		loaderTypes = arrayOfNulls<Class<*>>(loaderDependencies.size + 1)
		loaderTypes[0] = Data::class.java
		for (i in loaderDependencies.indices)
			loaderTypes[i + 1] = loaderDependencies[i].javaClass
	}

	/**
	 * Sets the dependencies for implemented [Drawer].
	 * @param drawerDependencies the dependencies a drawer has to get
	 */
	fun setDrawerDependencies(vararg drawerDependencies: Any?) {
		if (drawerDependencies == null) return
		Spriter.drawerDependencies = arrayOfNulls<Any>(drawerDependencies.size + 1)
		Spriter.drawerDependencies[0] = null
		System.arraycopy(drawerDependencies, 0, Spriter.drawerDependencies, 1, drawerDependencies.size)
		drawerTypes = arrayOfNulls<Class<*>>(drawerDependencies.size + 1)
		drawerTypes[0] = Loader::class.java
		for (i in drawerDependencies.indices)
			if (drawerDependencies[i] != null)
				drawerTypes[i + 1] = drawerDependencies[i]!!.javaClass
	}

	/**
	 * Initializes this class with the implemented [Loader] class and [Drawer] class.
	 * Before calling this method make sure that you have set all necessary dependecies with [.setDrawerDependencies] and [.setLoaderDependencies].
	 * A drawer is created with this method.
	 * @param loaderClass the loader class
	 * *
	 * @param drawerClass the drawer class
	 */
	fun init(loaderClass: Class<out Loader<*>>, drawerClass: Class<out Drawer<*>>) {
		Spriter.loaderClass = loaderClass
		try {
			drawer = drawerClass.getDeclaredConstructor(*drawerTypes).newInstance(*drawerDependencies)
		} catch (e: Exception) {
			e.printStackTrace()
		}

		initialized = drawer != null
	}

	/**
	 * Loads a SCML file with the given path.
	 * @param scmlFile the path to the SCML file
	 */
	fun load(scmlFile: String) {
		load(File(scmlFile))
	}

	/**
	 * Loads the given SCML file.
	 * @param scmlFile the scml file
	 */
	fun load(scmlFile: File) {
		try {
			load(FileInputStream(scmlFile), scmlFile.path.replace("\\\\".toRegex(), "/"))
		} catch (e: FileNotFoundException) {
			e.printStackTrace()
		}

	}

	/**
	 * Loads the given SCML stream pointing to a file saved at the given path.
	 * @param stream the SCML stream
	 * *
	 * @param scmlFile the path to the SCML file
	 */
	fun load(stream: InputStream, scmlFile: String) {
		val reader = SCMLReader(stream)
		val data = reader.data
		loadedData.put(scmlFile, data)
		loaderDependencies[0] = data
		try {
			val loader = loaderClass!!.getDeclaredConstructor(*loaderTypes).newInstance(*loaderDependencies)
			loader.load(File(scmlFile))
			loaders.add(loader)
			for (entity in data.entities)
				entityToLoader.put(entity, loader)
		} catch (e: Exception) {
			e.printStackTrace()
		}

	}

	/**
	 * Creates a new [Player] instance based on the given SCML file with the given entity index and the given class extending a [Player]
	 * @param scmlFile name of the SCML file
	 * *
	 * @param entityIndex the index of the entity
	 * *
	 * @param playerClass the class extending a [Player] class, e.g. [PlayerTweener].
	 * *
	 * @return a [Player] instance managed by this class
	 * *
	 * @throws SpriterException if the given SCML file was not loaded yet
	 */
	@JvmOverloads fun newPlayer(scmlFile: String, entityIndex: Int, playerClass: Class<out Player> = Player::class.java): Player? {
		if (!loadedData.containsKey(scmlFile)) throw SpriterException("You have to load \"$scmlFile\" before using it!")
		try {
			val player = playerClass.getDeclaredConstructor(Entity::class.java).newInstance(loadedData[scmlFile]!!.getEntity(entityIndex))
			players.add(player)
			return player
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return null
	}

	/**
	 * Creates a new [Player] instance based on the given SCML file with the given entity name
	 * @param scmlFile name of the SCML file
	 * *
	 * @param entityName name of the entity
	 * *
	 * @return a [Player] instance managed by this class
	 * *
	 * @throws SpriterException if the given SCML file was not loaded yet
	 */
	fun newPlayer(scmlFile: String, entityName: String): Player? {
		if (!loadedData.containsKey(scmlFile)) throw SpriterException("You have to load \"$scmlFile\" before using it!")
		return newPlayer(scmlFile, loadedData[scmlFile]!!.getEntityIndex(entityName))
	}

	/**
	 * Returns a loader for the given SCML filename.
	 * @param scmlFile the name of the SCML file
	 * *
	 * @return the loader for the given SCML filename
	 * *
	 * @throws NullPointerException if the SCML file was not loaded yet
	 */
	fun getLoader(scmlFile: String): Loader<*>? {
		return entityToLoader[getData(scmlFile).getEntity(0)]
	}

	/**
	 * Updates each created player by this class and immediately draws it.
	 * This method should only be called if you want to update and render on the same thread.
	 * @throws SpriterException if [.init] was not called before
	 */
	fun updateAndDraw() {
		if (!initialized) throw SpriterException("Call init() before updating!")
		for (i in players.indices) {
			players[i].update()
			drawer!!.loader = entityToLoader[players[i].getEntity()]!!
			drawer!!.draw(players[i])
		}
	}

	/**
	 * Updates each created player by this class.
	 * @throws SpriterException if [.init] was not called before
	 */
	fun update() {
		if (!initialized) throw SpriterException("Call init() before updating!")
		for (i in players.indices) {
			players[i].update()
		}
	}

	/**
	 * Draws each created player by this class.
	 * @throws SpriterException if [.init] was not called before
	 */
	fun draw() {
		if (!initialized) throw SpriterException("Call init() before drawing!")
		for (i in players.indices) {
			drawer!!.loader = entityToLoader[players[i].getEntity()]!!
			drawer!!.draw(players[i])
		}
	}

	/**
	 * Returns the drawer instance this class is using.
	 * @return the drawer which draws all players
	 */
	fun drawer(): Drawer<*> {
		return drawer!!
	}

	/**
	 * Returns the data for the given SCML filename.
	 * @param fileName the name of the SCML file
	 * *
	 * @return the data for the given SCML filename or null if not loaed yet
	 */
	fun getData(fileName: String): Data {
		return loadedData[fileName]!!
	}

	/**
	 * The number of players this class is managing.
	 * @return number of players
	 */
	fun players(): Int {
		return players.size
	}

	/**
	 * Clears all previous created players, Spriter datas, disposes all loaders, deletes the drawer and resets all internal lists.
	 * After this methods was called [.init] has to be called again so that everything works again.
	 */
	fun dispose() {
		drawer = null
		drawerDependencies = arrayOfNulls<Any>(1)
		drawerTypes = arrayOfNulls<Class<*>>(1)
		drawerTypes[0] = Loader::class.java

		entityToLoader.clear()

		for (i in loaders.indices) {
			loaders[i].dispose()
		}
		loaders.clear()
		loadedData.clear()
		loaderClass = null
		loaderTypes = arrayOfNulls<Class<*>>(1)
		loaderTypes[0] = Data::class.java
		loaderDependencies = arrayOfNulls<Any>(1)

		players.clear()

		initialized = false
	}

}
/**
 * Creates a new [Player] instance based on the given SCML file with the given entity index
 * @param scmlFile name of the SCML file
 * *
 * @param entityIndex the index of the entity
 * *
 * @return a [Player] instance managed by this class
 * *
 * @throws SpriterException if the given SCML file was not loaded yet
 */
