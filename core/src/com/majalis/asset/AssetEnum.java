package com.majalis.asset;

public enum AssetEnum {
	WORLD_MAP_UI("worldmap/CharacterInfo.png"),
	WORLD_MAP_HOVER("worldmap/HoverBox.png"),
	ARROW("worldmap/Arrow.png"),
	MOUNTAIN_ACTIVE("worldmap/MountainNode0.png"),
	FOREST_ACTIVE("worldmap/ForestNode1.png"),
	FOREST_INACTIVE("worldmap/ForestNode0.png"),
	GRASS0("worldmap/BaseGrass0.png"),
	GRASS1("worldmap/BaseGrass1.png"),
	GRASS2("worldmap/BaseGrass2.png"),
	CLOUD("worldmap/Cloud.png"),
	APPLE("worldmap/Apple.png"),
	MEAT("worldmap/Meat.png"),
	ROAD("worldmap/Road.png"),
	CASTLE("worldmap/Castle.png"),
	CHARACTER_SPRITE("worldmap/TinySprite0.png"),
	
	CHARACTER_SCREEN("ClassSelect.jpg"),
	VIGNETTE("CreamVignetteBottom.png"),
	
	BATTLE_BG("enemies/ForestBG.jpg"),
	WEREBITCH("enemies/WerebitchBasic.png"),
	HARPY("enemies/Harpy.png"),
	BRIGAND("enemies/Brigand.png"),
	SLIME("enemies/HeartSlime.png"), 
	
	AIRBORNE("stances/Airborne.png"),
	BALANCED("stances/Balanced.png"),
	BLITZ("stances/Blitz.png"),
	CASTING("stances/Casting.png"),
	DEFENSIVE("stances/Defensive.png"),

	DOGGY("stances/Doggy.png"),
	ERUPT("stances/Erupt.png"),
	FELLATIO("stances/Fellatio.png"),
	KNEELING("stances/Kneeling.png"),
	KNOTTED("stances/Knotted.png"),
	OFFENSIVE("stances/Offensive.png"),
	PRONE("stances/Prone.png"),
	SUPINE("stances/Supine.png"),
	;
	
	private final String path;

	AssetEnum(String path) {
	    this.path = path;
	 }
	public String getPath(){return path;}
}
