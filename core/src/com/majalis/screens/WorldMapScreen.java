package com.majalis.screens;

import com.badlogic.gdx.Gdx;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;
import static com.majalis.asset.AssetEnum.*;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.majalis.asset.AnimatedImage;
import com.majalis.asset.AssetEnum;
import com.majalis.character.PlayerCharacter;
import com.majalis.character.PlayerCharacter.QuestType;
import com.majalis.character.PlayerCharacter.QuestFlag;
import com.majalis.encounter.EncounterBounty;
import com.majalis.encounter.EncounterBounty.EncounterBountyResult;
import com.majalis.encounter.EncounterCode;
import com.majalis.save.LoadService;
import com.majalis.save.MutationResult;
import com.majalis.save.SaveEnum;
import com.majalis.save.SaveManager.GameContext;
import com.majalis.save.SaveManager.GameMode;
import com.majalis.scenes.MutationActor;
import com.majalis.talesofandrogyny.Logging;
import com.majalis.save.SaveService;
import com.majalis.save.MutationResult.MutationType;
import com.majalis.world.GameWorldHelper;
import com.majalis.world.GameWorldNode;
import com.majalis.world.GroundType;
/*
 * The screen that displays the world map.  UI that Handles player input while on the world map - will delegate to other screens depending on the gameWorld state.
 */
public class WorldMapScreen extends AbstractScreen {
	// this class needs major refactoring - far too many dependencies, properties, statefulness
	private final AssetManager assetManager;
	private final SaveService saveService;
	private final Array<GameWorldNode> world;
	private final Texture food;
	private final Texture cloud;
	private final Texture characterUITexture;
	private final PlayerCharacter character;
	private final Stage uiStage;
	private final PerspectiveCamera camera;
	private final Stage cloudStage;
	private final PerspectiveCamera cloudCamera;
	private final Stage dragStage;
	private final Group worldGroup;
	private final Group shadowGroup;
	private final Group cloudGroup;
	private final InputMultiplexer multi;
	private final AnimatedImage currentImage;
	private final AnimatedImage currentImageGhost;
	private final Skin skin;
	private final Texture hoverImageTexture;
	private final Image hoverImage;
	private final Label healthLabel;
	private final Label dateLabel;
	private final Label timeLabel;
	private final Label foodLabel;
	private final Label hoverLabel;
	private final TextButton campButton;
	private final boolean storyMode;
	private final RandomXS128 random;
	private final float travelTime;
	private final Array<FrameBuffer> frameBuffers;
	private final SpriteBatch frameBufferBatch;
	
	private GameWorldNode currentNode;
	private GameWorldNode hoveredNode;
	private int time;
	private boolean backgroundRendered = false;
	private GameContext currentContext;
	
	public static final Array<AssetDescriptor<?>> resourceRequirements = new Array<AssetDescriptor<?>>();
	static {
		resourceRequirements.add(UI_SKIN.getSkin());
		resourceRequirements.add(WORLD_MAP_MUSIC.getMusic());
		AssetEnum[] soundAssets = new AssetEnum[]{
			CLICK_SOUND, EQUIP, SWORD_SLASH_SOUND, THWAPPING
		};
		for (AssetEnum asset: soundAssets) {
			resourceRequirements.add(asset.getSound());
		}
		
		// need to refactor to get all stance textures
		AssetEnum[] assets = new AssetEnum[]{
			GROUND_SHEET, DOODADS, WORLD_MAP_BG, CHARACTER_ANIMATION, MOUNTAIN_ACTIVE, FOREST_ACTIVE, FOREST_INACTIVE, CASTLE, TOWN, COTTAGE, APPLE, MEAT, CLOUD, ROAD, WORLD_MAP_UI, WORLD_MAP_HOVER, ARROW, CHARACTER_SCREEN, EXP, GOLD, TIME, HEART, NULL
		};
		for (AssetEnum asset: assets) {
			resourceRequirements.add(asset.getTexture());
		}
		resourceRequirements.addAll(CharacterScreen.resourceRequirements);
	}
	
	public WorldMapScreen(ScreenFactory factory, ScreenElements elements, AssetManager assetManager, SaveService saveService, LoadService loadService, Array<GameWorldNode> world, RandomXS128 random) {
		super(factory, elements, AssetEnum.WORLD_MAP_MUSIC);
		this.assetManager = assetManager;
		this.saveService = saveService;
		this.random = random;
		this.travelTime = 1;
		this.frameBuffers = new Array<FrameBuffer>();
		this.frameBufferBatch = new SpriteBatch();
		
		this.storyMode = loadService.loadDataValue(SaveEnum.MODE, GameMode.class) == GameMode.STORY;
		uiStage = new Stage(new FitViewport(this.getViewport().getWorldWidth(), this.getViewport().getWorldHeight(), getCamera()), batch);
		uiStage.getCamera().update();
		
		dragStage = new Stage3D(new FitViewport(this.getViewport().getWorldWidth(), this.getViewport().getWorldHeight(), getCamera()), batch);
		
		camera = new PerspectiveCamera(70, 0, 1000);
		this.getViewport().setCamera(camera);

		time = loadService.loadDataValue(SaveEnum.TIME, Integer.class);
		
		camera.position.set(0, 0, storyMode ? 500 : 750);
		camera.near = 1f;
		camera.far = 10000;
		camera.lookAt(0, 0, 0);
		camera.translate(1280/2, 720/2, 200);
		
		cloudCamera = new PerspectiveCamera(70, 0, 1000);
        FitViewport viewport2 = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), cloudCamera);
		cloudStage = new Stage3D(viewport2, batch);
		
		cloudCamera.position.set(0, 0, 500);
		cloudCamera.near = 1f;
		cloudCamera.far = 10000;
		cloudCamera.lookAt(0,0,0);
		cloudCamera.translate(1280/2, 720/2, -200);
		
		// create base group for world stage
		worldGroup = new Group();
		this.addActor(worldGroup);
		
		shadowGroup = new Group();
		
		// load assets
		hoverImageTexture = assetManager.get(AssetEnum.WORLD_MAP_HOVER.getTexture());		
		food = assetManager.get(AssetEnum.APPLE.getTexture());
		cloud = assetManager.get(AssetEnum.CLOUD.getTexture());
		characterUITexture = assetManager.get(AssetEnum.WORLD_MAP_UI.getTexture());
		hoverImage = new Image(hoverImageTexture);
		
		skin = assetManager.get(AssetEnum.UI_SKIN.getSkin());
		campButton = new TextButton("", skin);
		
		// these should be updated with emitters
		healthLabel = new Label("", skin);
		dateLabel = new Label("", skin);
		timeLabel = new Label("", skin);
		foodLabel = new Label("", skin);
		hoverLabel = new Label("", skin);
		
		for (final GameWorldNode actor : world) {
			if (actor.isCurrent()) {
				setCurrentNode(actor);
			}
		}
		
		// move camera to saved position
		Vector3 initialTranslation = new Vector3(currentNode.getX(), currentNode.getY(), 0);
		initialTranslation = new Vector3(initialTranslation);
		initialTranslation.x -= camera.position.x;
		initialTranslation.y -= camera.position.y;
		camera.translate(initialTranslation);
		if (camera.position.x < 500) camera.position.x = 500;
		if (camera.position.y < 500) camera.position.y = 500;
		camera.update();

		// this should probably be a separate class
		Texture characterSheet = assetManager.get(AssetEnum.CHARACTER_ANIMATION.getTexture());
		Array<TextureRegion> frames = new Array<TextureRegion>();
		for (int ii = 0; ii < 4; ii++) {
			frames.add(new TextureRegion(characterSheet, ii * 72, 0, 72, 128));
		}
		
		Animation animation = new Animation(.14f, frames);
		animation.setPlayMode(PlayMode.LOOP);
		currentImage = new AnimatedImage(animation, Scaling.fit, Align.right);
		currentImage.setScale(.7f);
		currentImage.setState(0);
		// this is currently placing the character based on the camera in a way that conveniently places them on their current node - this needs to instead be aware of the current node and be able to grab its position from there (will need to know current node for behavior of Camp/Enter button regardless)
		currentImage.setPosition(initialTranslation.x + 646, initialTranslation.y + 390);
		
		currentImageGhost = new AnimatedImage(animation, Scaling.fit, Align.right);
		currentImageGhost.setScale(.7f);
		currentImageGhost.setState(0);
		currentImageGhost.getColor().a = .4f;
		
		// this is currently placing the character based on the camera in a way that conveniently places them on their current node - this needs to instead be aware of the current node and be able to grab its position from there (will need to know current node for behavior of Camp/Enter button regardless)
		currentImageGhost.setPosition(initialTranslation.x + 646, initialTranslation.y + 390);
		
		this.world = world;
		
		this.character = loadService.loadDataValue(SaveEnum.PLAYER, PlayerCharacter.class);
		this.cloudGroup = new Group();
		
		int leftWrap = -3000;
		int rightWrap = 10000;
		for (int ii = 0; ii < 200; ii++) {
			Actor actor = new Image(cloud);
			actor.setPosition((float)Math.random()*10000-1000, (float)Math.random()*10000-1000);
			actor.addAction(Actions.alpha(.3f));
			float speed = 10f;
			// move from starting position to leftWrap, then warp to rightWrap, then repeat those two actions forever
			actor.addAction(sequence(moveTo(leftWrap, actor.getY(), (actor.getX() - leftWrap) / speed), moveTo(rightWrap, actor.getY()), repeat(RepeatAction.FOREVER, sequence(moveTo(leftWrap, actor.getY(), rightWrap - leftWrap / speed), moveTo(rightWrap, actor.getY())))));
			cloudGroup.addActor(actor);
		}
		
		cloudStage.addActor(cloudGroup);
		
		multi = new InputMultiplexer();
		multi.addProcessor(uiStage);
		multi.addProcessor(this);
		multi.addProcessor(dragStage);
	}
	
	private void mutateLabels() {
		healthLabel.setText(String.valueOf(character.getCurrentHealth()));
		dateLabel.setText("Day: " + (time / 6 + 1));
		timeLabel.setText(getTime());
		timeLabel.setColor(getTimeColor());
		foodLabel.setText("X " + character.getFood());
		if (hoveredNode != null) {
			String text = hoveredNode.getHoverText();
			hoverLabel.setText(text);
			if (!text.equals("")) {
				hoverImage.setVisible(true);
			}
			else {
				hoverImage.setVisible(false);
			}
		}
	}
	
	private void addLabel(Group include, Actor toAdd, int x, int y, Color toSet) {
		include.addActor(toAdd);
		toAdd.setPosition(x, y);
		toAdd.setColor(toSet);		
	}
	
	@Override
	public void buildStage() {
		final Group uiGroup = new Group();
		uiGroup.addActor(hoverImage);
		hoverImage.setVisible(false);
		hoverImage.setBounds(1500, 5, 400, 300);
		
		addLabel(uiGroup, healthLabel, 310, 130, Color.WHITE);
		addLabel(uiGroup, dateLabel, 360,  140, Color.WHITE);
		addLabel(uiGroup, timeLabel, 380,  115, Color.WHITE);
		addLabel(uiGroup, foodLabel, 23,  15, Color.WHITE);
		addLabel(uiGroup, hoverLabel, 1575, 160, Color.BLACK);
		hoverLabel.setAlignment(Align.center);
		hoverLabel.setWrap(true);
		hoverLabel.setWidth(250);
		// need to add a pane for the hoverLabel
		
		mutateLabels();
		
		final Sound buttonSound = assetManager.get(AssetEnum.CLICK_SOUND.getSound()); 
		int storedLevels = character.getStoredLevels();
		
		Image characterUI = new Image(characterUITexture);
		uiStage.addActor(characterUI);
		characterUI.setScale(1.1f);
		
		TextButton characterButton = new TextButton(storedLevels > 0 ? "Level Up!" : "Character", skin);
		
		if (storedLevels > 0) {
			TextButtonStyle style = new TextButtonStyle(characterButton.getStyle());
			style.fontColor = Color.OLIVE;
			characterButton.setStyle(style);
		}
		
		Table table = new Table();
		table.setPosition(377, 65);
		uiStage.addActor(table);
		
		Table actionTable = new Table();
		uiStage.addActor(actionTable);
		actionTable.setPosition(900, 60);
		
		actionTable.add(characterButton).size(200, 50);
		
		characterButton.setBounds(185, 45, 185, 40);
		characterButton.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					showScreen(ScreenEnum.CHARACTER);		   
		        }
			}
		);
		
		TextButton inventoryButton = new TextButton("Inventory", skin);
		
		inventoryButton.setBounds(185, 45, 185, 40);
		inventoryButton.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					showScreen(ScreenEnum.INVENTORY);		   
		        }
			}
		);
		
		actionTable.add(inventoryButton).size(200, 50);
		
		Image foodIcon = new Image(food);
		foodIcon.setSize(75, 75);
		uiStage.addActor(foodIcon);
		
		final Label console = new Label("", skin);
		uiStage.addActor(console);
		console.setPosition(1250, 80);
		console.setWrap(true);
		console.setWidth(600);
		console.setColor(Color.CHARTREUSE);
		
		final TextButton rest = new TextButton("Rest", skin);
		
		checkCanEat(rest);
	
		table.add(rest).size(145, 40);
		
		// rest will eventually just wait some time - eating food if possible to maintain hunger level
		rest.addListener(
			new ClickListener() {
				@SuppressWarnings("unchecked")
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					setConsole(console, saveService.saveDataValue(SaveEnum.HEALTH, 10), saveService.saveDataValue(SaveEnum.TIME, 1));
					console.addAction(Actions.alpha(1));
					console.addAction(Actions.fadeOut(10));
					time++;
					tintForTimeOfDay();
					checkCanEat(rest);
					mutateLabels();
		        }
			}
		);

		final TextButton scout = new TextButton("Scout", skin);
		
		table.add(scout).size(145, 40).row();;

		// rest will eventually just wait some time - eating food if possible to maintain hunger level
		scout.addListener(
			new ClickListener() {
				@SuppressWarnings("unchecked")
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					if (checkForForcedRest());
					else {
						setConsole(console, saveService.saveDataValue(SaveEnum.SCOUT, 1), saveService.saveDataValue(SaveEnum.TIME, 1));
						console.addAction(Actions.alpha(1));
						console.addAction(Actions.fadeOut(10));
						currentNode.deactivate();
						currentNode.setAsCurrentNode();
						time++;
						tintForTimeOfDay();	
						checkCanEat(rest);
						mutateLabels();
						checkForForcedRest();
					}
		        }
			}
		);
	
		table.add(campButton).size(145, 40);
		
		campButton.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					saveService.saveDataValue(SaveEnum.CONTEXT, currentContext);
					saveService.saveDataValue(SaveEnum.RETURN_CONTEXT, GameContext.WORLD_MAP);
					showScreen(ScreenEnum.CONTINUE);
		        }
			}
		);
		
		TextButton questButton = new TextButton("Quest Log", skin);
		actionTable.add(questButton).size(200, 50).row();;
		questButton.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					showScreen(ScreenEnum.QUEST);
		        }
			}
		);	
		
		TextButton saveButton = new TextButton("QuickSave", skin);
		actionTable.add(saveButton).size(200, 50);
		saveButton.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					saveService.manualSave(".toa-data/quicksave.json");
					console.setText("Game Saved.");
					console.addAction(alpha(1));
					console.addAction(fadeOut(6));
					Logging.flush();
		        }
			}
		);	
		
		final TextButton quickLoadButton = new TextButton ("QuickLoad", skin);
		actionTable.add(quickLoadButton).size(200, 50);
		quickLoadButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
        		saveService.newSave(".toa-data/quicksave.json");
        		buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
				showScreen(ScreenEnum.LOAD_GAME);
			}
		});
		
		
		TextButton hardSaveButton = new TextButton("Save", skin);
		actionTable.add(hardSaveButton).size(200, 50).row();
		hardSaveButton.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					showScreen(ScreenEnum.SAVE);
		        }
			}
		);	
		
		if (!backgroundRendered) {
			generateBackground();
		}
		tintForTimeOfDay();
		
		uiStage.addActor(uiGroup);
		// this needs refactoring - probably replace ChangeListener with a custom listener/event type, and rather than an action on a delay, have a trigger for when the character reaches a node that will perform the act() function
		worldGroup.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (event.getTarget() instanceof GameWorldNode) {
					final GameWorldNode node = (GameWorldNode) event.getTarget();
					final int timePassed = 1;
					saveService.saveDataValue(SaveEnum.TIME, timePassed);
					boolean switchScreen = false;
					if(checkForForcedRest());
					else if (character.getCurrentDebt() >= 150 || (character.getCurrentDebt() >= 100 && character.getQuestStatus(QuestType.DEBT) < 1)) {
						autoEncounter(uiGroup, EncounterCode.BUNNY);
					}
					else if (time >= 11 && character.getQuestStatus(QuestType.ELF) == 0) { // forced elf encounter
						saveService.saveDataValue(SaveEnum.QUEST, new QuestFlag(QuestType.ELF, 1));	
						autoEncounter(uiGroup, EncounterCode.ELF);
					}
					else if (time >= 23 && character.getQuestStatus(QuestType.TRUDY) == 0) { // forced Trudy encounter
						autoEncounter(uiGroup, EncounterCode.ADVENTURER);
					}
					else {
						Vector2 finish = node.getHexPosition();
						Vector2 start = new Vector2(currentNode.getHexPosition());
						Array<Action> moveActions = new Array<Action>();
						Array<Action> moveActionsGhost = new Array<Action>();
						int distance = GameWorldHelper.distance((int)start.x, (int)start.y, (int)finish.x, (int)finish.y);
						int totalDistance = distance;
						while (distance > 0) {
							if (start.x + start.y == finish.x + finish.y) { // z is constant
								if (start.x < finish.x) { // downright
									start.x++;
									start.y--;
								}
								else { // upleft
									start.x--;
									start.y++;
								}
							}
							else if (start.y == finish.y) { // y is constant
								if (start.x < finish.x) start.x++; // upright
								else start.x--; // downleft
							}
							else if (start.x == finish.x) { // x is constant
								if (start.y < finish.y) start.y++; // up
								else start.y--; // down
							}
							else {
								int startZ = (int) (0 - (start.x + start.y));
								int finishZ = (int) (0 - (finish.x + finish.y));
								if (start.x > finish.x && startZ < finishZ) {
									start.x--;
								}
								else if (finish.y > start.y && startZ > finishZ) {
									start.y++;
								}
								else {
									start.x++;
									start.y--;
								}			
							}
							moveActions.add(moveTo(getTrueX((int)start.x) + 8, getTrueY((int)start.x, (int)start.y) + 27, travelTime/totalDistance));
							moveActionsGhost.add(moveTo(getTrueX((int)start.x) + 8, getTrueY((int)start.x, (int)start.y) + 27, travelTime/totalDistance));
							distance = GameWorldHelper.distance((int)start.x, (int)start.y, (int)finish.x, (int)finish.y);
						}
						
						Action[] allActionArray = moveActions.toArray(Action.class);
						Action[] allActionsGhostArray = moveActionsGhost.toArray(Action.class);
						currentImage.addAction(sequence(allActionArray));
						currentImageGhost.addAction(sequence(allActionsGhostArray));
						
						setCurrentNode(node);
						worldGroup.addAction(sequence(delay(travelTime), new Action() {
							@Override
							public boolean act(float delta) {
								time += timePassed;
								tintForTimeOfDay();
								boolean switchScreen = false;
								EncounterCode newEncounter = node.getEncounterCode();
								EncounterBounty miniEncounter = newEncounter.getMiniEncounter();
								if(newEncounter == EncounterCode.DEFAULT) {
									// this will need to also check if the node is a town/dungeon node and appropriately swap the button from "Camp" to "Enter"
									saveService.saveDataValue(SaveEnum.SCOUT, 0);
									node.deactivate();
									node.setAsCurrentNode();
								}
								else if (miniEncounter != null) {
									final Image displayNewEncounter = new Image(hoverImageTexture);
									displayNewEncounter.setBounds(250, 150, 500, 400);
									uiGroup.addActor(displayNewEncounter);
									EncounterBountyResult result = miniEncounter.execute(character.getScoutingScore(), saveService);
									
									final Label newEncounterText = new Label(result.displayText(), skin);
									
									if (result.soundToPlay() != null) {
										assetManager.get(result.soundToPlay()).play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f); 
									}
																										
									newEncounterText.setColor(Color.GOLD);

									final Table statusResults = new Table();
									statusResults.setPosition(350, 425);
									newEncounterText.setWrap(true);
									statusResults.align(Align.topLeft);
									statusResults.add(newEncounterText).width(325).row();
									Array<MutationResult> compactedResults = MutationResult.collapse(result.getResults()); 
									for (MutationResult miniResult : compactedResults) {
										MutationActor actor = new MutationActor(miniResult, assetManager.get(miniResult.getTexture()), skin, true);
										actor.setWrap(true);
										// this width setting is going to be tricky once we implement images for perk and skill gains and such
										statusResults.add(actor).width(miniResult.getType() == MutationType.NONE ? 325 : 50).height(50).align(Align.left).row();
									}
									
									uiGroup.addActor(statusResults); 									
									uiGroup.addAction(sequence(
										delay(1), 
										new Action(){ @Override
											public boolean act(float delta) {
												checkForForcedRest();
												return true;
										}}, 
										delay(7), 
										new Action() {
											@Override
											public boolean act(float delta) {
												uiGroup.removeActor(displayNewEncounter);
												uiGroup.removeActor(statusResults);
												return true;
											}
									}));
									saveService.saveDataValue(SaveEnum.VISITED_LIST, node.getNodeCode());
									saveService.saveDataValue(SaveEnum.SCOUT, 0);
									node.deactivate();
									node.setAsCurrentNode();
								}
								else {
									saveService.saveDataValue(SaveEnum.ENCOUNTER_CODE, newEncounter); 
									saveService.saveDataValue(SaveEnum.VISITED_LIST, node.getNodeCode());
									saveService.saveDataValue(SaveEnum.CONTEXT, node.getEncounterContext());
									saveService.saveDataValue(SaveEnum.RETURN_CONTEXT, GameContext.WORLD_MAP);
									switchScreen = true;
								}
								saveService.saveDataValue(SaveEnum.NODE_CODE, node.getNodeCode());
								if (switchScreen) {
									switchContext();
								}
								mutateLabels();
								return true;
							}
						}));
					}
					if (switchScreen) {
						switchContext();
					}
				}
				mutateLabels();
			}			
		});
		dragStage.addListener(new DragListener(){
			@Override
			public void drag(InputEvent event, float x, float y, int pointer) {
				translateCamera(new Vector3(getDeltaX(), getDeltaY(), 0));
			}
		});
	}

	private void setConsole(Label console, Array<MutationResult> ...allResults) {
		String consoleText = "";
		for (Array<MutationResult> results : allResults) {
			for (MutationResult result : results) {
				consoleText += result.getText() + " ";
			}
		}
		console.setText(consoleText.trim());
	}
	
	private void setCurrentNode(GameWorldNode newCurrentNode) {
		currentNode = newCurrentNode;
		currentContext = currentNode.getEncounterContext() == GameContext.TOWN ? GameContext.TOWN : GameContext.CAMP;
		campButton.setText(currentContext == GameContext.TOWN ? "Enter" : "Camp");
	}
	
	private boolean checkForForcedRest() {
		if (character.getCurrentHealth() <= 0) {		
			if (character.getFood() <= 0) {
				saveService.saveDataValue(SaveEnum.ENCOUNTER_CODE, EncounterCode.STARVATION);	
				saveService.saveDataValue(SaveEnum.CONTEXT, GameContext.ENCOUNTER);
			}
			else {
				saveService.saveDataValue(SaveEnum.CONTEXT, GameContext.CAMP);
			}			
			saveService.saveDataValue(SaveEnum.RETURN_CONTEXT, GameContext.WORLD_MAP);
			switchContext();
			return true;
		}
		return false;
	}

	private void switchContext() {
		character.popPortraitPath();
		showScreen(ScreenEnum.CONTINUE);
	}
	
	private void autoEncounter(Group uiGroup, EncounterCode encounter) {
		saveService.saveDataValue(SaveEnum.ENCOUNTER_CODE, encounter);	
		saveService.saveDataValue(SaveEnum.CONTEXT, GameContext.ENCOUNTER);
		saveService.saveDataValue(SaveEnum.RETURN_CONTEXT, GameContext.WORLD_MAP);
		Image displayNewEncounter = new Image(hoverImageTexture);
		displayNewEncounter.setBounds(250, 150, 500, 400);
		uiGroup.addActor(displayNewEncounter);
		Label newEncounterText = new Label("Encounter!", skin);
		newEncounterText.setColor(Color.GOLD);
		newEncounterText.setPosition(430, 335);
		uiGroup.addActor(newEncounterText);
		uiGroup.addAction(sequence(delay(travelTime), new Action() {
			@Override
			public boolean act(float delta) {
				switchContext();
				return true;
			}
		}));
	}
	
	private void checkCanEat(TextButton camp) {
		if (character.getFood() < character.getMetabolicRate()) {
			TextButtonStyle style = new TextButtonStyle(camp.getStyle());
			style.fontColor = Color.RED;
			camp.setStyle(style);
			camp.setTouchable(Touchable.disabled);
		}
	}
	
	@Override
	public void render(float delta) {
		translateCamera();
		
		if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
			showScreen(ScreenEnum.CHARACTER);
		}			
		else if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			showScreen(ScreenEnum.MAIN_MENU);
		}
		else {
			// draws the world
			super.render(delta);
			// draws the cloud layer
			cloudStage.act(delta);
			cloudStage.draw();
			// this draws the UI
			uiStage.act();
			uiStage.draw();
		}
	}
	
	private void tintForTimeOfDay() {
		TimeOfDay timeOfDay = TimeOfDay.getTime(time);
		
		for (Actor actor : worldGroup.getChildren()) {
			actor.setColor(getTimeColor());
		}
		for (Actor actor : shadowGroup.getChildren()) {
			Shadow shadow = (Shadow) actor;
			shadow.setColor(timeOfDay.getShadowColor());
			shadow.addAction(Actions.alpha(timeOfDay.getShadowAlpha()));
			shadow.setSkew(timeOfDay.getShadowDirection(), timeOfDay.getShadowLength());
		}
	}
	
	private Color getTimeColor() { return TimeOfDay.getTime(time).getColor(); }
	private String getTime() { return TimeOfDay.getTime(time).getDisplay(); }
	
	private void translateCamera() {
		Vector3 translationVector = new Vector3(0, 0, 0);
		int speed = 8;
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) speed = 16;
		
		if (Gdx.input.isKeyPressed(Keys.LEFT) && !Gdx.input.isKeyPressed(Keys.RIGHT) && camera.position.x > 500) {
			translationVector.x -= speed;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT) && !Gdx.input.isKeyPressed(Keys.LEFT) && camera.position.x < (storyMode ? 1000 : 4000)) {
			translationVector.x += speed;
		}
		if (Gdx.input.isKeyPressed(Keys.DOWN) && !Gdx.input.isKeyPressed(Keys.UP) && camera.position.y > 500) {
			translationVector.y -= speed;
		}
		if (Gdx.input.isKeyPressed(Keys.UP) && !Gdx.input.isKeyPressed(Keys.DOWN) && camera.position.y < (storyMode ? 1000 : 4600)) {
			translationVector.y += speed;
		}
		translateCamera(translationVector);
	}
	
	private void translateCamera(Vector3 translationVector) {
		float x = camera.position.x;
		float y = camera.position.y;
		camera.translate(translationVector);
		Vector3 position = camera.position;
		position.x = Math.max(Math.min(position.x, 4000), 500);
		position.y = Math.max(Math.min(position.y, 4600), 500);		
		x = position.x - x;
		y = position.y - y;		
		Vector3 cloudTranslate = new Vector3(x, y, 0);
		cloudTranslate.x *= 2;
		cloudTranslate.y *= 2;
		cloudCamera.translate(cloudTranslate);
		position = cloudCamera.position;
	}
	
	private ObjectMap<String, TextureRegion> groundSlices = new ObjectMap<String, TextureRegion>();
	private final static int tileWidth = 61;
	private final static int tileHeight = 55;
	
	private int distance(int x, int y, int x2, int y2) { return GameWorldHelper.distance(x, y, x2, y2); }
	
	private int worldCollide(int x, int y) {
		int minDistance = 100;
		for (GameWorldNode node : world) {
			int distance = distance(x, y, (int)node.getHexPosition().x, (int)node.getHexPosition().y);
			if (distance < minDistance) minDistance = distance;
		}
		return minDistance;
	}
	
	private class Doodad extends Image {
		public Doodad(TextureRegion textureRegion) {
			super(textureRegion);
		}
		
		@Override
		public Actor hit(float x, float y, boolean touchable) { return null; }		
	}
		
	private class Shadow extends Actor {

		private final TextureRegion texture;
		private Affine2 affine = new Affine2();
		private float shadowDirection;
		private float shadowLength;
		
		public Shadow(TextureRegion textureRegion) {
			this.texture = textureRegion;
		}

		public void setSkew(float shadowDirection, float shadowLength) {
			this.shadowDirection = shadowDirection;
			this.shadowLength = shadowLength;			
		}

		@Override
	    public void draw(Batch batch, float parentAlpha) {
			Color color = getColor();
			batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
			affine.setToTrnRotScl(getX() + texture.getRegionWidth() + (getOriginX()), getY() + (getOriginY()*2)+3, 180, 1, 1);
	        affine.shear(shadowDirection, 0);  // this modifies the skew
			batch.draw(texture, texture.getRegionWidth(), texture.getRegionHeight() * shadowLength, affine);
	    }
	}
	
	private void generateBackground() {
		backgroundRendered = true;
		if (storyMode) {
			Image background = new Image(assetManager.get(WORLD_MAP_BG.getTexture()));
			background.setPosition(-400, 0);
			worldGroup.addActorAt(0, background);
			addWorldActors();
		}
		else {
			Logging.logTime("Generate Background BEGIN");
			/* MODELLING - SHOULD BE MOVED TO GAME WORLD GEN */
			Array<Array<GroundType>> ground = new Array<Array<GroundType>>();
			Array<Doodad> doodads = new Array<Doodad>();
			Array<Shadow> shadows = new Array<Shadow>();		
			Array<Image> reflections = new Array<Image>();
			Array<AnimatedImage> lilies = new Array<AnimatedImage>();
			
			Texture doodadTextureSheet = assetManager.get(AssetEnum.DOODADS.getTexture());
			Array<TextureRegion> treeTextures = new Array<TextureRegion>();
			Array<TextureRegion> treeShadowTextures = new Array<TextureRegion>();
			int treeArraySize = 26;
			int treeWidth = 192;
			int treeHeight = 256;
			for (int ii = 0; ii < treeArraySize; ii++) {
				treeTextures.add(new TextureRegion(doodadTextureSheet, ii * treeWidth, 0, treeWidth, treeHeight));
				TextureRegion shadowTexture = new TextureRegion(doodadTextureSheet, ii * treeWidth, 0, treeWidth, treeHeight);
				shadowTexture.flip(true, false);
				treeShadowTextures.add(shadowTexture);
			}
			
			Array<TextureRegion> rockTextures = new Array<TextureRegion>();
			Array<TextureRegion> rockShadowTextures = new Array<TextureRegion>();
			int rockArraySize = 19;
			int rockWidth = 256;
			int rockHeight = 128;
			for (int ii = 0; ii < rockArraySize; ii++) {
				rockTextures.add(new TextureRegion(doodadTextureSheet, ii * rockWidth, treeHeight, rockWidth, rockHeight));
				TextureRegion shadowTexture = new TextureRegion(doodadTextureSheet, ii * rockWidth, treeHeight, rockWidth, rockHeight);
				shadowTexture.flip(true, false);
				rockShadowTextures.add(shadowTexture);
			}
			
			Array<Animation> lilyAnimations = new Array<Animation>();
			int lilyArraySize = 15;
			int lilyWidth = 64;
			int lilyHeight = 64;
			for (int ii = 0; ii < lilyArraySize; ii++) {
				Array<TextureRegion> frames = new Array<TextureRegion>();
				frames.add(new TextureRegion(doodadTextureSheet, ii * lilyWidth, treeHeight + rockHeight, lilyWidth, lilyHeight));
				frames.add(new TextureRegion(doodadTextureSheet, ii * lilyWidth, treeHeight + rockHeight + lilyHeight, lilyWidth, lilyHeight));
				Animation lilyAnimation = new Animation(.28f, frames);
				lilyAnimation.setPlayMode(PlayMode.LOOP);
				lilyAnimations.add(lilyAnimation);
			}		
			
			int maxX = 170;
			int maxY = 235;
			
			// first figure out what all of the tiles are - dirt, greenLeaf, redLeaf, moss, or water - create a model without drawing anything	
			for (int x = 0; x < maxX; x++) {
				Array<GroundType> layer = new Array<GroundType>();
				ground.add(layer);
				for (int y = 0; y < maxY; y++) {
					// redLeaf should be the default			
					// dirt should be randomly spread throughout redLeaf  
					// greenLeaf might also be randomly spread throughout redLeaf
					// bodies of water should be generated as a single central river that runs through the map for now, that randomly twists and turns and bulges at the turns
					// moss should be in patches adjacent to water
					
					int closest = worldCollide(x, y);
					
					GroundType toAdd;
					
					if (closest >= 2 && (river(x, y) || lake(x, y))) toAdd = GroundType.WATER;
					else if (closest <= 3 || shoreline(x, y)) toAdd = GroundType.DIRT;
					else {
						toAdd = GroundType.valueOf("RED_LEAF_" + Math.abs(random.nextInt() % 6));					
					}
					
					layer.add(toAdd);
					
					if (toAdd == GroundType.WATER && random.nextInt() % 5 == 0) {
						AnimatedImage lily = new AnimatedImage(lilyAnimations.get(Math.abs(random.nextInt() % lilyArraySize)), Scaling.fit, Align.center);
						lily.setState(0);
						int trueX = getTrueX(x) - (int)lily.getWidth() / 2 + tileWidth / 2;
						int trueY = getTrueY(x, y) + tileHeight / 2;
						lily.setPosition(trueX, trueY);
						lilies.add(lily);
					}
					
					boolean treeAbundance = isAbundantTrees(x, y);				
					if (closest >= 3 && toAdd == GroundType.DIRT || toAdd == GroundType.RED_LEAF_0 || toAdd == GroundType.RED_LEAF_1) {
						if (random.nextInt() % (treeAbundance ? 2 : 15) == 0) {
							Array<TextureRegion> textures;
							Array<TextureRegion> shadowTextures;
							int arraySize;
							if (!(treeAbundance) && random.nextInt() % 6 == 0) {
								textures = rockTextures;
								shadowTextures = rockShadowTextures;
								arraySize = rockArraySize;
							}
							else {
								textures = treeTextures;
								shadowTextures = treeShadowTextures;
								arraySize = treeArraySize;
							}
							int chosen = Math.abs(random.nextInt() % arraySize);
							Doodad doodad = new Doodad(textures.get(chosen));
							Shadow shadow = new Shadow(shadowTextures.get(chosen));
							Image reflection = new Image(shadowTextures.get(chosen));
							int trueX = getTrueX(x) - (int)doodad.getWidth() / 2 + tileWidth / 2;
							int trueY = getTrueY(x, y) + tileHeight / 2;
							doodad.setPosition(trueX , trueY);
							shadow.setPosition(trueX, trueY);
							reflection.setPosition(trueX, trueY);
							reflection.setOrigin(reflection.getWidth() / 2, 16);
							reflection.rotateBy(180);
							reflection.addAction(Actions.alpha(.6f));
							
							boolean doodadInserted = false;
							int ii = 0;
							for (Image compare : doodads) {
								if (doodad.getY() > compare.getY()) {
									doodadInserted = true;
									doodads.insert(ii, doodad);
									shadows.insert(ii, shadow);
									reflections.insert(ii, reflection);
									break;
								}
								ii++;
							}
							if (!doodadInserted) {
								doodads.add(doodad);
								shadows.add(shadow);
								reflections.add(reflection);
							}
						}
					}	
				}
			}			
			
			// iterate a second pass through and determine where rocks and trees and shadows should go
			/*for (int x = 0; x < ground.size; x++) {
				int layerSize = ground.get(x).size;
				for (int y = 0; y < ground.get(x).size; y++) {
					// place random rocks on tiles adjacent to water
					
					// place random trees on redLeaf/greenLeaf/dirt tiles that aren't adjacent to water
					// for each tree, create a shadow (should be mapped shadow textures placed at the same location as the tree)

				}
			}*/
			
			Logging.logTime("Finished Generation");
			Logging.logTime("Drawing BEGIN");
			/* DRAWING */
			Texture groundSheet = assetManager.get(AssetEnum.GROUND_SHEET.getTexture());	
			
			// draw (add drawings as actors) ground layer
			drawLayer(ground, groundSheet, false);
						
			// draw (add reflections as actors) reflections
			for (AnimatedImage lily :lilies) {
				worldGroup.addActorAt(0, lily);
			}
			
			for (Image reflection : reflections) {
				worldGroup.addActorAt(0, reflection);
			}
		
			// draw (add drawings as actors) water layer
			drawLayer(ground, groundSheet, true);
							
			worldGroup.addActor(shadowGroup);	
			
			addWorldActors();
			
			for (Shadow shadow : shadows) {
				shadowGroup.addActor(shadow);
			}
			
			for (Doodad doodad : doodads) {
				worldGroup.addActor(doodad);
			}	
			Group tempGroup = new Group();
			tempGroup.addActor(currentImageGhost);
			worldGroup.addActor(tempGroup);
			Logging.logTime("Finished Drawing");
		}
		frameBufferBatch.dispose();
		Logging.logTime("Generate Background End");
	}
	
	private boolean river(int x, int y) {
		return (x + y > 140 && x + y < 148 && y > 50) || (y > 50 && y < 60 && x + y > 140);
	}
	
	private boolean shoreline(int x, int y) {
		for (int ii = x - 2; ii <= x + 2; ii++) {
			for (int jj = y - 2; jj <= y + 2; jj++) {
				if (river(ii, jj)) return true;
				if (lake(ii, jj)) return true;
			}
		}
		return false;
	}
	
	private boolean lake(int x, int y) {
		return distance(x, y, 13, 90) < 5 || distance(x, y, 87, 55) < 7 || distance(x, y, 80, 62) < 5 || distance(x, y, 94, 55) < 5;
	}
	
	private boolean isAbundantTrees(int x, int y) {
		return (x + y > 147 && x + y < 150) || (x > 0 && x < 15) || (x + y * 2 > 180 && x + y * 2 < 195);
	}
	
	private static int maxTextureSize = getMaxTextureSize();
	private static int getMaxTextureSize() {
		int textureSize = 128;
		while (textureSize <= GL20.GL_MAX_TEXTURE_SIZE) {
			textureSize *= 2;
		}
		if (textureSize > GL20.GL_MAX_TEXTURE_SIZE) textureSize /= 2;
		return textureSize;
	}
	
	private void drawLayer(Array<Array<GroundType>> ground, Texture groundSheet, boolean waterLayer) {
		int[] layers = new int [GroundType.values().length];
		int boxWidth = maxTextureSize;
		int boxHeight = maxTextureSize;
		int xScreenBuffer = 683;
		int yScreenBuffer = 165;
		Matrix4 matrix = new Matrix4();
		matrix.setToOrtho2D(0, 0, boxWidth, boxHeight); 
		frameBufferBatch.setProjectionMatrix(matrix);
		int xSize = 5888 / boxWidth + 1;
		int ySize = 5632 / boxHeight + 1;
		
		for (int xTile = 0; xTile < xSize; xTile++) {
			for (int yTile = 0; yTile < ySize; yTile++) {
				FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, boxWidth, boxHeight, false); // this and matrix need to preserve a ratio
				frameBuffers.add(frameBuffer);
				frameBuffer.begin();
				Gdx.gl.glClearColor(.2f, .2f, .4f, 0);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				frameBufferBatch.begin();
				
				for (int x = 0; x < ground.size; x++) {
					int trueX = getTrueX(x) - (boxWidth) * xTile + xScreenBuffer;
					if (trueX < -60 || trueX > boxWidth) continue;
					Array<GroundType> left = x - 1 >= 0 ? ground.get(x - 1) : null;
					Array<GroundType> middle = ground.get(x);
					Array<GroundType> right = x + 1 < ground.size ? ground.get(x + 1) : null;
					int layerSize = middle.size;
					for (int y = 0; y < middle.size; y++) {
						int trueY = getTrueY(x, y) - (boxHeight) * yTile + yScreenBuffer;
						if (trueY < -60 || trueY > boxHeight) continue;
						for (int i = 0; i < layers.length; i++) {
							layers[i] = 0;
						}
						
						GroundType currentHexType = middle.get(y);
						// check the six adjacent tiles and add accordingly
						if (right != null) {
							layers[right.get(y).ordinal()] += 1;
						}
						if (y - 1 >= 0)	{
							if (right != null) {		
								layers[right.get(y - 1).ordinal()] += 2;
								
							}
							layers[middle.get(y - 1).ordinal()] += 4;
						}
						if (left != null)	{
							layers[left.get(y).ordinal()] += 8;
						}
						if (y + 1 < layerSize)	{
							if (left != null) {
								layers[left.get(y + 1).ordinal()] += 16;
							}		
							layers[middle.get(y + 1).ordinal()] += 32;
						}
						if (waterLayer) {
							if (currentHexType == GroundType.WATER) {
								frameBufferBatch.draw(getFullTexture(GroundType.WATER, groundSheet), trueX, trueY); // with appropriate type
								frameBufferBatch.draw(getTexture(GroundType.WATER, groundSheet, layers[GroundType.WATER.ordinal()]), trueX, trueY); // appropriate blend layer
							}
						}
						else {
							for (GroundType groundType: GroundType.values()) {
								if (currentHexType == groundType) {
									if ( groundType != GroundType.WATER) {
										frameBufferBatch.draw(getFullTexture(groundType, groundSheet), trueX, trueY); // with appropriate type
									}
								}
								else {
									frameBufferBatch.draw(getTexture(groundType, groundSheet, layers[groundType.ordinal()]), trueX, trueY); // appropriate blend layer
								}
								
							}
						}
					}
				}
				frameBufferBatch.end();
				frameBuffer.end();
				
				Image background = new Image(new TextureRegion(frameBuffer.getColorBufferTexture(), 0, boxHeight, boxWidth, -boxHeight));
				background.addAction(Actions.moveTo(-xScreenBuffer + xTile * boxWidth, -yScreenBuffer + yTile * (boxHeight)));
				worldGroup.addActorAt(0, background);
			}
		}
		Logging.logTime("Drawing " + (waterLayer ? "water layer" : "ground layer") + " END");
	}
	
	private void addWorldActors() {
		for (GameWorldNode node : world) {
			for (Actor actor : node.getPaths()) {
				worldGroup.addActor(actor);
			}
		}
		
		for (final GameWorldNode actor : world) {
			worldGroup.addActor(actor);
			actor.addListener(new ClickListener(){
				@Override
		        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
					String text = actor.getHoverText();
					hoverLabel.setText(text);
					if (!text.equals("")) {
						hoverImage.setVisible(true);
					}
					hoveredNode = actor;
				}
				@Override
		        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
					hoverLabel.setText("");
					hoverImage.setVisible(false);
					hoveredNode = null;
				}
			});
		}
		worldGroup.addActor(currentImage);
	}

	private int getTrueX(int x) {
		return GameWorldHelper.getTrueX(x);
	}
	
	private int getTrueY(int x, int y) {
		return GameWorldHelper.getTrueY(x, y);
	}
	
	public TextureRegion getFullTexture(GroundType groundType, Texture groundSheet) {
		String key = groundType.toString();
		TextureRegion slice = groundSlices.get(key, new TextureRegion(groundSheet,  (groundType.ordinal() + 1) * (tileWidth) + 1, 0, tileWidth, tileHeight));
		groundSlices.put(key, slice);
		return slice;
	}

	public TextureRegion getTexture(GroundType groundType, Texture groundSheet, int mask) {
		String key = groundType.toString() + "-" + mask;
		TextureRegion slice = groundSlices.get(key, new TextureRegion(groundSheet, mask * (tileWidth) + 1, (groundType.ordinal()) * (tileHeight), tileWidth, tileHeight));
		groundSlices.put(key, slice);
		return slice;
	}
	
	@Override
    public void show() {
        Gdx.input.setInputProcessor(multi);
        font.setUseIntegerPositions(false);
    }
	
    @Override
    public void resize(int width, int height) {
    	super.resize(width, height);
        uiStage.getViewport().update(width, height, false);
        cloudStage.getViewport().update(width, height, false);
    }
	
	@Override
	public void dispose() {
		for(AssetDescriptor<?> path: resourceRequirements) {
			if (path.fileName.equals(AssetEnum.CLICK_SOUND.getSound().fileName) || path.type == Music.class) continue;
			assetManager.unload(path.fileName);
		}
		for (FrameBuffer frameBuffer : frameBuffers) {
			frameBuffer.dispose();
		}
	}	
}