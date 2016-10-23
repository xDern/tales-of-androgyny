package com.majalis.battle;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.ObjectMap;
import com.majalis.asset.AssetEnum;
import com.majalis.character.EnemyCharacter;
import com.majalis.character.PlayerCharacter;
import com.majalis.encounter.Background;
import com.majalis.character.AbstractCharacter.Stance;
import com.majalis.save.LoadService;
import com.majalis.save.SaveEnum;
import com.majalis.save.SaveManager;
import com.majalis.save.SaveService;
/*
 * Controls the construction of a battle either from a saved state or net new.
 */
public class BattleFactory {

	private final SaveService saveService;
	private final LoadService loadService;
	private final AssetManager assetManager;
	private final BitmapFont font;
	public BattleFactory(SaveManager saveManager, AssetManager assetManager, FreeTypeFontGenerator fontGenerator){
		this.saveService = saveManager;
		this.loadService = saveManager;
		this.assetManager = assetManager;
		FreeTypeFontParameter fontParameter = new FreeTypeFontParameter();
	    fontParameter.size = 18;
	    font = fontGenerator.generateFont(fontParameter);
	}
	
	public Battle getBattle(BattleCode battleCode, PlayerCharacter playerCharacter) {
		EnemyCharacter enemy = loadService.loadDataValue(SaveEnum.ENEMY, EnemyCharacter.class);
		// need a new Enemy
		if (enemy == null || enemy.getCurrentHealth() <= 0){
			enemy = getEnemy(battleCode.battleCode);
			enemy.setStance(battleCode.enemyStance);
			if (enemy.getStance() == Stance.DOGGY || enemy.getStance() == Stance.FELLATIO || enemy.getStance() == Stance.ANAL ){
				enemy.setLust(10);
			}
			playerCharacter.setStance(battleCode.playerStance);			
		}
		// loading old enemy
		else {
			ObjectMap<Stance, Texture> textures = new ObjectMap<Stance, Texture>();
			for (String key : enemy.getTextureImagePaths().keys()){
				textures.put(Stance.valueOf(key), assetManager.get(enemy.getTextureImagePaths().get(key), Texture.class)) ;
			}
			enemy.init(assetManager.get(enemy.getImagePath(), Texture.class), textures);
		}
		switch(battleCode.battleCode){	
			default: 
				return new Battle(saveService, assetManager, font, playerCharacter, enemy, battleCode.victoryScene, battleCode.defeatScene, new Background(assetManager.get(AssetEnum.BATTLE_BG.getPath(), Texture.class)), new Background(assetManager.get(enemy.getBGPath(), Texture.class)));
		}
	}
	
	private Texture getTexture(EnemyEnum type){
		return assetManager.get(type.getPath(), Texture.class);
	}
	
	private ObjectMap<Stance, Texture> getTextures(EnemyEnum type){
		ObjectMap<Stance, Texture> textures = new ObjectMap<Stance, Texture>();
		
		if (type == EnemyEnum.SLIME){
			textures.put(Stance.DOGGY, assetManager.get(AssetEnum.SLIME_DOGGY.getPath(), Texture.class));
		}
		
		return textures;
	}
	
	private EnemyCharacter getEnemy(int battleCode){
		
		switch(battleCode){
			case 0: return new EnemyCharacter(getTexture(EnemyEnum.WERESLUT), getTextures(EnemyEnum.WERESLUT), EnemyEnum.WERESLUT);
			case 1: return new EnemyCharacter(getTexture(EnemyEnum.HARPY), getTextures(EnemyEnum.HARPY), EnemyEnum.HARPY);
			case 2: return new EnemyCharacter(getTexture(EnemyEnum.SLIME), getTextures(EnemyEnum.SLIME), EnemyEnum.SLIME);
			case 3: return new EnemyCharacter(getTexture(EnemyEnum.BRIGAND), getTextures(EnemyEnum.BRIGAND), EnemyEnum.BRIGAND);
			default: return null;
		}
	}

	public enum EnemyEnum {
		WERESLUT ("Wereslut", AssetEnum.WEREBITCH.getPath()),
		HARPY ("Harpy", AssetEnum.HARPY.getPath()),
		SLIME ("Slime", AssetEnum.SLIME.getPath()),
		BRIGAND ("Brigand", AssetEnum.BRIGAND.getPath());
		
		private final String text;
		private final String path;
	    private EnemyEnum(final String text, final String path) { this.text = text; this.path = path; }
	    @Override
	    public String toString() { return text; }	
	    public String getPath() { return path; }
	}
}
