package com.majalis.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.majalis.asset.AssetEnum;
import com.majalis.battle.BattleAttributes;
import com.majalis.battle.BattleFactory;
import com.majalis.character.PlayerCharacter;
import com.majalis.encounter.Encounter;
import com.majalis.encounter.EncounterCode;
import com.majalis.encounter.EncounterFactory;
import com.majalis.save.LoadService;
import com.majalis.save.ProfileEnum;
import com.majalis.save.SaveEnum;
import com.majalis.save.SaveManager;
import com.majalis.save.SaveService;
import com.majalis.save.SaveManager.GameMode;
import com.majalis.talesofandrogyny.Logging;
import com.majalis.talesofandrogyny.TalesOfAndrogyny;
import com.majalis.world.GameWorldFactory;
/*
 * ScreenFactory implementation to generate and cache screens.
 */
public class ScreenFactoryImpl implements ScreenFactory {

	private static final int winWidth = 1920;
	private static final int winHeight = 1080;
	private final Game game;
	private final AssetManager assetManager;
	private final SaveService saveService;
	private final LoadService loadService;	
	private final GameWorldFactory gameWorldFactory;
	private final EncounterFactory encounterFactory;
	private final BattleFactory battleFactory;
	private final PolygonSpriteBatch batch;
	private final FreeTypeFontGenerator fontGenerator;
	private boolean loading;
	
	public ScreenFactoryImpl(Game game, AssetManager assetManager, SaveManager saveManager, GameWorldFactory gameWorldFactory, EncounterFactory encounterFactory, BattleFactory battleFactory, PolygonSpriteBatch batch, FreeTypeFontGenerator fontGenerator) {
		this.game = game;
		this.assetManager = assetManager;
		this.saveService = saveManager;
		this.loadService = saveManager;
		this.gameWorldFactory = gameWorldFactory;
		this.encounterFactory = encounterFactory;
		this.battleFactory = battleFactory;
		this.batch = batch;
		this.fontGenerator = fontGenerator;
		loading = true;
		if (TalesOfAndrogyny.testing) testWorldGen();
	}
	
	/* Unit Test */
	private void testWorldGen() {
		saveService.newSave();
		Logging.logTime("Begin logging");
		for (int ii = 0; ii < 100; ii++) {
			Logging.logTime("Seed: " + ii);
			assetManager.load(AssetEnum.UI_SKIN.getSkin());
			for (AssetDescriptor<?> desc : WorldMapScreen.resourceRequirements) {
				assetManager.load(desc);
			}
			
			assetManager.finishLoading();
			AbstractScreen newScreen = new WorldMapScreen(this, getElements(), assetManager, saveService, loadService, gameWorldFactory.getGameWorld(ii, GameMode.SKIRMISH, 1), gameWorldFactory.getRandom());
			
			Screen currentScreen = game.getScreen();
	    	
	        // Dispose previous screen
	        if (currentScreen != null) {
	            currentScreen.dispose();
	        }
	    	// Show new screen
	    	newScreen.buildStage();
	        game.setScreen(newScreen);
			
			EncounterCode.resetState();
		}
		Logging.flush();
	}
	/* End Unit Test */

	private ScreenElements getElements() {
		OrthographicCamera camera = new OrthographicCamera();
        FitViewport viewport =  new FitViewport(winWidth, winHeight, camera);
        ScreenElements elements = new ScreenElements(viewport, batch, fontGenerator, assetManager);
        return elements;
	}
	
	@SuppressWarnings("unchecked")
	@Override  
	public AbstractScreen getScreen(ScreenEnum screenRequest) {
		ScreenElements elements = getElements();
        PlayerCharacter character = loadService.loadDataValue(SaveEnum.PLAYER, PlayerCharacter.class);
		AbstractScreen tempScreen;
		switch(screenRequest) {
			case SPLASH: 
				return new SplashScreen(this, elements, assetManager, 25, Gdx.app.getPreferences("tales-of-androgyny-preferences").getBoolean("preload", false));
			case MAIN_MENU: 
				if (getAssetCheck(MainMenuScreen.resourceRequirements)) {
					return new MainMenuScreen(this, elements, saveService, loadService); 
				}
				break;
			case NEW_GAME:	
			case ENCOUNTER: 
				tempScreen = getEncounter(elements, character);
				if (tempScreen != null) return tempScreen;
				break; 
			case CONTINUE: 
			case LOAD_GAME:
				tempScreen = getCurrentContextScreen(elements, character);
				if (tempScreen != null) return tempScreen;
				break;
			case SAVE:
				if (getAssetCheck(SaveScreen.resourceRequirements)) {
					return new SaveScreen(this, elements, saveService);
				}
				break;
			case QUEST:
				if (getAssetCheck(QuestScreen.resourceRequirements)) {
					return new QuestScreen(this, elements, character);
				}
				break;
			case BATTLE:
				tempScreen = getBattle(elements, character);
				if (tempScreen != null) return tempScreen;
				break;
			case CHARACTER:
				if (getAssetCheck(CharacterScreen.resourceRequirements)) {
					return new CharacterScreen(this, elements, saveService, character);
				}
				break;
			case INVENTORY:
				if (getAssetCheck(InventoryScreen.resourceRequirements)) {
					return new InventoryScreen(this, elements, saveService, character);
				}
				break;
			case GAME_OVER:				
				if (getAssetCheck(GameOverScreen.resourceRequirements)) {
					return new GameOverScreen(this, elements, saveService);
				}
				break;
			case OPTIONS: 	
				if (getAssetCheck(OptionScreen.resourceRequirements)) {
					return new OptionScreen(this, elements);
				}
				break;
			case REPLAY:
				if (getAssetCheck(ReplayScreen.resourceRequirements)) {
					return new ReplayScreen(this, elements, (ObjectMap<String, Integer>) loadService.loadDataValue(ProfileEnum.KNOWLEDGE, ObjectMap.class));
				}
				break;
			case CREDITS:
				if (getAssetCheck(CreditsScreen.resourceRequirements)) {
					return new CreditsScreen(this, elements);
				}
				break;
			case EXIT: 	return new ExitScreen(this, elements);
		}
		loading = true;
		return new LoadScreen(this, elements, screenRequest);
	}

	private boolean getAssetCheck(Array<AssetDescriptor<?>> assetsToLoad) {
		// if the loading screen has just loaded the assets, don't perform the checks or increment the reference counts
		if (loading) {
			loading = false;
			return true;
		}
		// if screens are being switched but no assets need to be loaded, don't call the loading screen
		boolean assetsLoaded = true;
		for (AssetDescriptor<?> path: assetsToLoad) {
			if (!assetManager.isLoaded(path.fileName)) {
				assetsLoaded = false;
			}
			assetManager.load(path);
		}
		// temporary hack to ensure skin is always loaded
		assetManager.load(AssetEnum.UI_SKIN.getSkin());
		assetManager.load(AssetEnum.BATTLE_SKIN.getSkin());
		return assetsLoaded;
	}
	
	private EncounterScreen getEncounter(ScreenElements elements, PlayerCharacter character) {
		if (getAssetCheck(EncounterScreen.getRequirements((EncounterCode)loadService.loadDataValue(SaveEnum.ENCOUNTER_CODE, Integer.class)))) {
			EncounterCode encounterCode = loadService.loadDataValue(SaveEnum.ENCOUNTER_CODE, EncounterCode.class);
			Encounter encounter = encounterFactory.getEncounter(encounterCode, elements.getFont(48));			
			return new EncounterScreen(this, elements, loadService, encounter);
		}
		else {
			return null;
		}
	}

	private BattleScreen getBattle(ScreenElements elements, PlayerCharacter character) {
		if (getAssetCheck(BattleScreen.getRequirements((BattleAttributes) loadService.loadDataValue(SaveEnum.BATTLE_CODE, BattleAttributes.class)))) {
			BattleAttributes battleCode = loadService.loadDataValue(SaveEnum.BATTLE_CODE, BattleAttributes.class);
			return new BattleScreen(this, elements, saveService, battleFactory.getBattle(battleCode, character));
		}
		else {
			return null;
		}
	}
	
	private LevelUpScreen getLevel(ScreenElements elements, PlayerCharacter character) {
		if (getAssetCheck(LevelUpScreen.resourceRequirements)) {
			// -3 is the magic number for the level up screen encounter
			return new LevelUpScreen(this, elements, saveService, encounterFactory.getEncounter(EncounterCode.LEVEL_UP, elements.getFont(48)));
		}
		return null;
	}
	
	private TownScreen getTown(ScreenElements elements, PlayerCharacter character) {
		if (getAssetCheck(TownScreen.resourceRequirements)) {
			return new TownScreen(this, elements, saveService, (Integer)loadService.loadDataValue(SaveEnum.TIME, Integer.class));
		}
		return null;
	}
	
	private CampScreen getCamp(ScreenElements elements, PlayerCharacter character) {
		if (getAssetCheck(CampScreen.resourceRequirements)) {
			return new CampScreen(this, elements, saveService, character, (Integer)loadService.loadDataValue(SaveEnum.TIME, Integer.class));
		}
		return null;
	}
	
	private AbstractScreen getCurrentContextScreen(ScreenElements elements, PlayerCharacter character) {
		SaveManager.GameContext context = loadService.loadDataValue(SaveEnum.CONTEXT, SaveManager.GameContext.class);
		switch (context) {
			case ENCOUNTER: return getEncounter(elements, character);
			case WORLD_MAP: 
				if (getAssetCheck(WorldMapScreen.resourceRequirements)) {
					int worldSeed = loadService.loadDataValue(SaveEnum.WORLD_SEED, Integer.class);
					return new WorldMapScreen(this, elements, assetManager, saveService, loadService, gameWorldFactory.getGameWorld(worldSeed, (GameMode)loadService.loadDataValue(SaveEnum.MODE, GameMode.class), (Integer)loadService.loadDataValue(SaveEnum.NODE_CODE, Integer.class)), gameWorldFactory.getRandom());
				}
				else return null;
			case BATTLE:
				return getBattle(elements, character);
			case LEVEL:
				return getLevel(elements, character);
			case TOWN:
				return getTown(elements, character); 
			case CAMP:
				return getCamp(elements, character);
			case GAME_OVER:
				if (getAssetCheck(GameOverScreen.resourceRequirements)) {
					return new GameOverScreen(this, elements, saveService);
				}
				else return null;
			default: return null;
		}	
	}
	
	@Override
	public Game getGame() {
		return game;
	}
	
	private class ExitScreen extends AbstractScreen {
		protected ExitScreen(ScreenFactory factory, ScreenElements elements) {
			super(factory, elements, null);
		}
		@Override
		public void buildStage() {
			Gdx.app.exit();
		}
	}
}