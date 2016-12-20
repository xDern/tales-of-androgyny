package com.majalis.character;
/*
 * Represents an individual, discrete item.
 */
public abstract class Item {

	public abstract int getValue();
	protected abstract ItemEffect getUseEffect();
	public abstract String getName();
	
	public static class Weapon extends Item {
		
		private final String name;
		public Weapon(){
			this("Sword");
		}
		
		public Weapon(String name){
			this.name = name;
		}
		
		@Override
		public int getValue() {
			return 10;
		}

		@Override
		protected ItemEffect getUseEffect() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}
	}
	
	public static class Potion extends Item {

		private final int magnitude;
		public Potion() {
			this(10);
		}
		
		public Potion(int magnitude){
			this.magnitude = magnitude;
		}

		@Override
		public int getValue() {
			return magnitude / 2;
		}

		@Override
		protected ItemEffect getUseEffect() {
			return new ItemEffect(EffectType.HEALING, magnitude);
		}

		@Override
		public String getName() {
			return "Potion (" + magnitude + ")"; 
		}
	}
	
	public enum EffectType {
		HEALING		
	}
	
	public class ItemEffect {
	
		private final EffectType type;
		private final int magnitude;
		
		private ItemEffect(EffectType type, int magnitude){
			this.type = type;
			this.magnitude = magnitude;
		}
		
		public EffectType getType() { return type; }
		public int getMagnitude() { return magnitude; }
	}	
}
