package com.majalis.encounter;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.OrderedMap;
import com.majalis.battle.BattleCode;
import com.majalis.character.PlayerCharacter;
import com.majalis.save.SaveEnum;
import com.majalis.save.SaveManager;
import com.majalis.save.SaveService;
import com.majalis.scenes.BattleScene;
import com.majalis.scenes.CharacterCreationScene;
import com.majalis.scenes.ChoiceScene;
import com.majalis.scenes.EndScene;
import com.majalis.scenes.Mutation;
import com.majalis.scenes.Scene;
import com.majalis.scenes.TextScene;
/*
 * Given a sceneCode, reads that encounter and constructs it from a script file.
 */
public class EncounterBuilder {
	private final Array<Scene> scenes;
	private final Array<EndScene> endScenes;
	private final Array<BattleScene> battleScenes; 
	private final EncounterReader reader;
	private final SaveService saveService;
	private final BitmapFont font;
	private final int sceneCode;
	private int battleCode;
	// can probably be replaced with a call to scenes.size
	private int sceneCounter;
	
	protected EncounterBuilder(EncounterReader reader, SaveService saveService, BitmapFont font, int sceneCode, int battleCode){
		scenes = new Array<Scene>();
		endScenes = new Array<EndScene>();
		battleScenes = new Array<BattleScene>();
		this.reader = reader;
		this.saveService = saveService;
		this.font = font;
		this.sceneCode = sceneCode;
		this.battleCode = battleCode;
		sceneCounter = 0;
	}
	/* different encounter "templates" */
	@SuppressWarnings("unchecked")
	protected Encounter getClassChoiceEncounter(AssetManager assetManager, PlayerCharacter playerCharacter){	
		Array<String> jobButtonLabels = new Array<String>();
		for (SaveManager.JobClass jobClass: SaveManager.JobClass.values()){
			jobButtonLabels.add(jobClass.getLabel());
		}
		
		Array<String> skipCharacterCreation = new Array<String>(true, new String[]{"Create Character", "Default"}, 0, 2);
		
		addScene(getChoiceScene(
			aggregateMaps(			
				getTextScenes(new String[]{"Welcome to the world of tRaPG!", "You're looking mighty fine.", "Please choose your class."}, 
					addScene(getChoiceScene(addScene(getJobClassScenes(addScene
							// need to add other class options; should probably be a custom interface scene with a button that pipes to this end scene
							(new CharacterCreationScene(addScene(new EndScene(new OrderedMap<Integer, Scene>(), -1, EndScene.Type.ENCOUNTER_OVER)), sceneCounter, saveService, font, assetManager, playerCharacter)
									
									), font)), assetManager, "Select a class:", jobButtonLabels)), font), 
				addScene(new EndScene(new OrderedMap<Integer, Scene>(), -1, EndScene.Type.ENCOUNTER_OVER))
				), assetManager, "Skip character creation?", skipCharacterCreation
		));
		return new Encounter(scenes, endScenes, new Array<BattleScene>(), getStartScene(scenes, sceneCode));
	}
	
	private Scene getChoiceScene(OrderedMap<Integer, Scene> sceneMap, AssetManager assetManager, String choiceDialogue, Array<String> buttonLabels){
		// use sceneMap to generate the table
		Table table = new Table();

		Skin skin = assetManager.get("uiskin.json", Skin.class);
		Sound buttonSound = assetManager.get("sound.wav", Sound.class);

		ChoiceScene choiceScene = new ChoiceScene(sceneMap, sceneCounter, saveService, font, choiceDialogue, table);
		int ii = 0;
		for (String label  : buttonLabels){
			TextButton button = new TextButton(label, skin);
			button.addListener(getListener(choiceScene, sceneMap.get(sceneMap.orderedKeys().get(ii++)), buttonSound));
			table.add(button).row();
		}
				
		return choiceScene;
	}
	

	private ClickListener getListener(final ChoiceScene currentScene, final Scene nextScene, final Sound buttonSound){
		return new ClickListener(){
	        @Override
	        public void clicked(InputEvent event, float x, float y) {
	        	buttonSound.play();
	        	// set new Scene as active based on choice
	        	nextScene.setActive();
	        	currentScene.finish();
	        	
	        }
	    };
	}
	
	protected Encounter getDefaultEncounter(){
		getTextScenes(new String[]{"You encounter a stick!", "It's actually rather sexy looking.", "There is nothing left here to do."}, addScene(new EndScene(new OrderedMap<Integer, Scene>(), -1, EndScene.Type.ENCOUNTER_OVER)), font);
		return new Encounter(scenes, endScenes, new Array<BattleScene>(), getStartScene(scenes, sceneCode));
	}
	
	@SuppressWarnings("unchecked")
	protected Encounter getRandomEncounter(int encounterCode){
		// if there isn't already a battlecode set, it's determined by the encounterCode; for now, that means dividing the various encounters up by modulus
		if (battleCode == -1) battleCode = encounterCode % 2;
		getTextScenes(getScript(battleCode, 0), 
			addScene(
					new BattleScene(
							aggregateMaps(
									getTextScenes(new String[]{"You won!  You get NOTHING.", "Sad :(", "What a pity.  Go away."}, addScene(new EndScene(new OrderedMap<Integer, Scene>(), -1, EndScene.Type.ENCOUNTER_OVER)), font),
									getTextScenes(getDefeatText(battleCode), addScene(new EndScene(new OrderedMap<Integer, Scene>(), -1, EndScene.Type.GAME_OVER)), font)					
							), -1, saveService, battleCode)), font);
		// reporting that the battle code has been consumed - this should be encounter code
		saveService.saveDataValue(SaveEnum.BATTLE_CODE, new BattleCode(-1, -1, -1));
		return new Encounter(scenes, endScenes, battleScenes, getStartScene(scenes, sceneCode));	
	}
	
	private OrderedMap<Integer, Scene> addScene(Scene scene){ return addScene(getSceneList(scene)); }
	// pass in one or multiple scenes that the next scene will branch into
	private OrderedMap<Integer, Scene> addScene(Array<Scene> scenes){
		IntArray sceneCodes = new IntArray();
		for (Scene scene : scenes){
			this.scenes.add(scene);
			if (scene instanceof BattleScene) battleScenes.add((BattleScene)scene);
			if (scene instanceof EndScene) endScenes.add((EndScene)scene);
			sceneCodes.add(sceneCounter++);
		}
		return getSceneMap(sceneCodes, scenes);
	}
	
	private OrderedMap<Integer, Scene> getSceneMap(IntArray integers, Array<Scene> scenes){
		OrderedMap<Integer, Scene> sceneMap = new OrderedMap<Integer, Scene>();
		for (int ii = 0; ii < integers.size; ii++){
			sceneMap.put(integers.get(ii), scenes.get(ii));
		}
		return sceneMap;
	}
	
	private OrderedMap<Integer, Scene> getTextScenes(String[] script, OrderedMap<Integer, Scene> sceneMap, BitmapFont font){ return getTextScenes(new Array<String>(true, script, 0, script.length), sceneMap, font); }	
	// pass in a list of script lines in chronological order, this will reverse their order and add them to the stack
	private OrderedMap<Integer, Scene> getTextScenes(Array<String> script, OrderedMap<Integer, Scene> sceneMap, BitmapFont font){
		script.reverse();
		for (String scriptLine: script){
			sceneMap = addScene(new TextScene(sceneMap, sceneCounter, saveService, font, scriptLine, getMutationList(new Mutation())));
		}	
		return sceneMap;
	}
	
	private OrderedMap<Integer, Scene> aggregateMaps(OrderedMap<Integer, Scene>... sceneMaps){
		OrderedMap<Integer, Scene> aggregatedMap = new OrderedMap<Integer, Scene>();
		for (OrderedMap<Integer, Scene> map : sceneMaps){
			aggregatedMap.putAll(map);
		}
		return aggregatedMap;	
	}
	
	// reads from a text file with the battleCode followed by the scene - either delimited by a certain character or just endline and use the wordwrapping of font.draw
	// this will need to be combined with getScript and be called with ENCOUNTERCODE (not battlecode!) as well as the textSceneCode (there should be an incrementer each time getTextScenes is called), which will be passed to the key converter in reader and then passed to reader.loadscript 
	private String[] getDefeatText(int battleCode){
		switch(battleCode){
			case 0:
				return new String[]{"You lost! You get knotty werewolf cock! (up the butt)."};
			default:
				return new String[]{"You lost! The harpy mounts you! (up the butt)."};
		}
	}
	
	private String[] getScript(int battleCode, int scene){
		return reader.loadScript(battleCode);
	}
	private Scene getStartScene(Array<Scene> scenes, Integer sceneCode){
		// default case	
		if (sceneCode == 0){
			// returns the final scene and plays in reverse order
			return scenes.get(scenes.size - 1);
		}
		for (Scene objScene: scenes){
			if (objScene.getCode() == sceneCode){
				return objScene;
			}
		}
		return null;
	}
	
	private Array<Scene> getJobClassScenes(OrderedMap<Integer, Scene> sceneMap, BitmapFont font){
		Array<Scene> jobClassScenes = new Array<Scene>();
		int tempCounter = sceneCounter;
		for (SaveManager.JobClass jobClass: SaveManager.JobClass.values()){
			jobClassScenes.add(new TextScene(sceneMap, tempCounter++, saveService, font, "You are now "+ getJobClass(jobClass) +".", getMutationList(new Mutation(saveService, SaveEnum.CLASS, jobClass))));
		}
		return jobClassScenes;
	}
	/* Helper methods that may go away with refactors*/
	private String getJobClass(SaveManager.JobClass jobClass){ return jobClass == SaveManager.JobClass.ENCHANTRESS ? "an Enchantress" : "a " + jobClass.getLabel(); }
	private Array<Scene> getSceneList(Scene... scenes){ return new Array<Scene>(true, scenes, 0, scenes.length); }	
	private Array<Mutation> getMutationList(Mutation... mutations){ return new Array<Mutation>(true, mutations, 0, mutations.length); }
}