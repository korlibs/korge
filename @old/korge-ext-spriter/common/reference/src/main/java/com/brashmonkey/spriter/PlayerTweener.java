package com.brashmonkey.spriter;

/**
 * A player tweener is responsible for tweening to {@link Player} instances.
 * Such a 
 * @author Trixt0r
 *
 */
public class PlayerTweener extends Player{
	
	private TweenedAnimation anim;
	private Player player1, player2;
	/**
	 * Indicates whether to update the {@link Player} instances this instance is holding.
	 * If this variable is set to <code>false</code>, you will have to call {@link Player#update()} on your own.
	 */
	public boolean updatePlayers = true;
	
	/**
	 * The name of root bone to start the tweening at.
	 * Set it to null to tween the whole hierarchy.
	 */
	public String baseBoneName = null;
	
	/**
	 * Creates a player tweener which will tween the given two players.
	 * @param player1 the first player
	 * @param player2 the second player
	 */
	public PlayerTweener(Player player1, Player player2){
		super(player1.getEntity());
		this.setPlayers(player1, player2);
	}
	
	/**
	 * Creates a player tweener based on the entity.
	 * The players to tween will be created by this instance.
	 * @param entity the entity the players will animate
	 */
	public PlayerTweener(Entity entity){
		this(new Player(entity), new Player(entity));
	}
	
	/**
	 * Tweens the current set players.
	 * This method will update the set players if {@link #updatePlayers} is <code>true</code>.
	 * @throws SpriterException if no bone with {@link #baseBoneName} exists
	 */
	@Override
	public void update(){
		if(updatePlayers){
			player1.update();
			player2.update();
		}
		anim.setAnimations(player1.animation, player2.animation);
		super.update();
		if(baseBoneName != null){
			int index = anim.onFirstMainLine()? player1.getBoneIndex(baseBoneName) : player2.getBoneIndex(baseBoneName);
			if(index == -1) throw new SpriterException("A bone with name \""+baseBoneName+"\" does no exist!");
			anim.base = anim.getCurrentKey().getBoneRef(index);
			super.update();
		}
	}
	
	/**
	 * Sets the players for this tweener.
	 * Both players have to hold the same {@link Entity}
	 * @param player1 the first player
	 * @param player2 the second player
	 */
	public void setPlayers(Player player1, Player player2){
		if(player1.entity != player2.entity)
			throw new SpriterException("player1 and player2 have to hold the same entity!");
		this.player1 = player1;
		this.player2 = player2;
		if(player1.entity == entity) return;
		this.anim = new TweenedAnimation(player1.getEntity());
		anim.setAnimations(player1.animation, player2.animation);
		super.setEntity(player1.getEntity());
		super.setAnimation(anim);
	}
	
	/**
	 * Returns the first set player.
	 * @return the first player
	 */
	public Player getFirstPlayer(){
		return this.player1;
	}
	
	/**
	 * Returns the second set player.
	 * @return the second player
	 */
	public Player getSecondPlayer(){
		return this.player2;
	}
	
	/**
	 * Sets the interpolation weight of this tweener.
	 * @param weight  the interpolation weight between 0.0f  and 1.0f
	 */
	public void setWeight(float weight){
		this.anim.weight = weight;
	}
	
	/**
	 * Returns the interpolation weight.
	 * @return the interpolation weight between 0.0f  and 1.0f
	 */
	public float getWeight(){
		return this.anim.weight;
	}
	
	
	/**
	 * Sets the base animation of this tweener.
	 * Has only an effect if {@link #baseBoneName} is not <code>null</code>. 
	 * @param anim the base animation
	 */
	public void setBaseAnimation(Animation anim){
		this.anim.baseAnimation = anim;
	}
	
	/**
	 * Sets the base animation of this tweener by the given animation index.
	 * Has only an effect if {@link #baseBoneName} is not <code>null</code>. 
	 * @param index the index of the base animation
	 */
	public void setBaseAnimation(int index){
		this.setBaseAnimation(entity.getAnimation(index));
	}
	
	/**
	 * Sets the base animation of this tweener by the given name.
	 * Has only an effect if {@link #baseBoneName} is not <code>null</code>. 
	 * @param name the name of the base animation
	 */
	public void setBaseAnimation(String name){
		this.setBaseAnimation(entity.getAnimation(name));
	}
	
	/**
	 * Returns the base animation if this tweener.
	 * @return the base animation
	 */
	public Animation getBaseAnimation(){
		return this.anim.baseAnimation;
	}
	
	/**
	 * Not supported by this class.
	 */
	@Override
	public void setAnimation(Animation anim){}
	
	/**
	 * Not supported by this class.
	 */
	@Override
	public void setEntity(Entity entity){}
}
