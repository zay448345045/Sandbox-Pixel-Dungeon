/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.editor.Checkpoint;
import com.shatteredpixel.shatteredpixeldungeon.editor.levels.LevelScheme;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.CustomDungeonSaves;
import com.shatteredpixel.shatteredpixeldungeon.effects.ShadowBox;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.LostBackpack;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError;
import com.watabou.NotAllowedInLua;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

@NotAllowedInLua
public class InterlevelScene extends PixelScene {
	
	//slow fade on entering a new region
	private static final float SLOW_FADE = 1f; //.33 in, 1.33 steady, .33 out, 2 seconds total
	//norm fade when loading, falling, returning, or descending to a new floor
	private static final float NORM_FADE = 0.67f; //.33 in, .67 steady, .33 out, 1.33 seconds total
	//fast fade when ascending, or descending to a floor you've been on
	private static final float FAST_FADE = 0.50f; //.33 in, .33 steady, .33 out, 1 second total
	
	private static float fadeTime;
	
	public enum Mode {
		DESCEND, ASCEND, CONTINUE, CHECKPOINT, RESURRECT, RETURN, FALL, RESET, NONE
	}
	public static Mode mode;

    public static LevelTransition curTransition = null;
    public static String returnLevel;
    public static int returnPos, returnBranch;

	public static boolean fallIntoPit;
	public static int fallPosition;

	private enum Phase {
		FADE_IN, STATIC, FADE_OUT
	}
	private Phase phase;
	private float timeLeft;

	public Image background;

	private RenderedTextBlock loadingText;

	private RenderedTextBlock storyMessage;
	private ShadowBox storyBG;
	private StyledButton btnContinue;
	private IconButton btnHideStory;
	
	private static Thread thread;
	private static Exception error = null;
	private float waitingTime;

	public static int lastRegion = -1;

	{
		inGameScene = true;
	}
	
	@Override
	public void create() {
		super.create();

		long seed = Dungeon.seed;
		String loadingAsset;
		int loadingDepth;
		final float scrollSpeed;
		fadeTime = NORM_FADE;
		switch (mode){
			default:
				scrollSpeed = 0;
				break;
			case CONTINUE:
				scrollSpeed = 5;
				break;
			case CHECKPOINT:
				fadeTime = SLOW_FADE;
				scrollSpeed = 5;
				break;
			case DESCEND:
				if (Dungeon.hero == null){
					loadingDepth = 1;
					fadeTime = SLOW_FADE;
				} else {
					loadingDepth = Dungeon.customDungeon.getFloor(curTransition.destLevel).getDepth();
					if (Statistics.deepestFloor >= loadingDepth) {
						fadeTime = FAST_FADE;
					} else if (Dungeon.customDungeon.getFloor(Dungeon.levelName).hasBoss()) {
						fadeTime = SLOW_FADE;
					}
				}
				scrollSpeed = 5;
				break;
			case FALL:
				scrollSpeed = 50;
				break;
			case ASCEND:
				fadeTime = FAST_FADE;
				scrollSpeed = -5;
				break;
			case RETURN:
				scrollSpeed = Dungeon.customDungeon.getFloor(returnLevel).getDepth() > Dungeon.depth ? 15 : -15;
				break;
		}

        //flush the texture cache whenever moving between regions, helps reduce memory load
        if (curTransition != null) {
            int region = Dungeon.customDungeon.getFloor(curTransition.destLevel).getVisualRegion();
            if (region != lastRegion) {
                TextureCache.clear();
                lastRegion = region;
            }
        }

		int loadingCenter = 400;

		//for portrait users, each run the splashes change what details they focus on
		Random.pushGenerator(seed+lastRegion);
		switch (lastRegion){
			case 1:
				loadingAsset = Assets.Splashes.SEWERS;
				switch (Random.Int(2)){
					case 0: loadingCenter = 180; break; //focus on rats and left side
					case 1: loadingCenter = 485; break; //focus on center pipe and door
				}
				break;
			case 2:
				loadingAsset = Assets.Splashes.PRISON;
				switch (Random.Int(3)){
					case 0: loadingCenter = 190; break; //focus on left skeleton
					case 1: loadingCenter = 402; break; //focus on center arch
				}
				break;
			case 3:
				loadingAsset = Assets.Splashes.CAVES;
				switch (Random.Int(3)){
					case 0: loadingCenter = 340; break; //focus on center gnoll groups
					case 1: loadingCenter = 625; break; //focus on right gnoll
				}
				break;
			case 4:
				loadingAsset = Assets.Splashes.CITY;
				switch (Random.Int(3)){
					case 0: loadingCenter = 275; break; //focus on left bookcases
					case 1: loadingCenter = 485; break; //focus on center pathway
				}
				break;
			case 5: default:
				loadingAsset = Assets.Splashes.HALLS;
				switch (Random.Int(3)){
					case 0: loadingCenter = 145; break; //focus on left arches
					case 1: loadingCenter = 400; break; //focus on ripper demon
				}
				break;
		}
		Random.popGenerator();
		
		if (DeviceCompat.isDebug()&&false){
			fadeTime = 0f;
		} else if (Dungeon.isLevelTesting()){
			fadeTime /= 3.5f;
		}

		background = new Image(loadingAsset);
		background.scale.set(Camera.main.height/background.height);

		if (Camera.main.width >= background.width()){
			background.x = (Camera.main.width - background.width())/2f;
		} else {
			background.x = Camera.main.width / 2f - loadingCenter * background.scale.x;
			background.x = GameMath.gate(Camera.main.width - background.width(), background.x, 0);
		}
		background.y = (Camera.main.height - background.height()) / 2f;
		PixelScene.align(background);
		add(background);

		Image fadeLeft, fadeRight;
		fadeLeft = new Image(TextureCache.createGradient(0xFF000000, 0xFF000000, 0x00000000));
		fadeLeft.x = background.x-2;
		fadeLeft.scale.set(3, background.height());
		fadeLeft.visible = background.x > 0;
		add(fadeLeft);

		fadeRight = new Image(fadeLeft);
		fadeRight.x = background.x + background.width() + 2;
		fadeRight.y = background.y + background.height();
		fadeRight.angle = 180;
		fadeRight.visible = fadeLeft.visible;
		add(fadeRight);

		Image im = new Image(TextureCache.createGradient(0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF000000)){
			@Override
			public void update() {
				super.update();
				if (lastRegion == 6)                aa = 1;
				else if (phase == Phase.FADE_IN)    aa = Math.max( 0, 2*(timeLeft - (fadeTime - 0.333f)));
				else if (phase == Phase.FADE_OUT)   aa = Math.max( 0, 2*(0.333f - timeLeft));
				//else                                aa = 0;
			}
		};
		im.angle = 90;
		im.x = Camera.main.width;
		im.scale.x = Camera.main.height/5f;
		im.scale.y = Camera.main.width;
		add(im);

		String text = Messages.get(Mode.class, mode.name());
		
		loadingText = PixelScene.renderTextBlock( text, 9 );
		loadingText.setPos(
				(Camera.main.width - loadingText.width() - 8),
				(Camera.main.height - loadingText.height() - 6)
		);
		align(loadingText);
		add(loadingText);


		if (mode == Mode.DESCEND && lastRegion <= LevelScheme.REGION_HALLS && !DeviceCompat.isDebug()){

			LevelScheme ls = Dungeon.customDungeon.getFloor(curTransition.destLevel);
			int region = ls.getVisualRegion();

			if (Dungeon.hero == null || (ls.getDepth() > Statistics.deepestFloor && ls.getNumInRegion() == 1)){
					storyMessage = PixelScene.renderTextBlock(Document.INTROS.pageBody(region), 6);
					storyMessage.maxWidth( PixelScene.landscape() ? 180 : 125);
					storyMessage.setPos((Camera.main.width-storyMessage.width())/2f, (Camera.main.height-storyMessage.height())/2f);

					storyBG = new ShadowBox();
					storyBG.boxRect(storyMessage.left()-10, storyMessage.top()-10, storyMessage.width()+20, storyMessage.height()+20);
					storyBG.alpha(0.8f);
					add(storyBG);
					add(storyMessage);

					btnContinue = new StyledButton(Chrome.Type.TOAST_TR, Messages.get(InterlevelScene.class, "continue"), 9){
						@Override
						protected void onClick() {
							phase = Phase.FADE_OUT;
							timeLeft = fadeTime;

							btnContinue.enable(false);
							Document.INTROS.readPage(region);
						}
					};
					btnContinue.icon(Icons.STAIRS.get());
					btnContinue.setSize(btnContinue.reqWidth()+10, 22);
					btnContinue.visible = false;
					btnContinue.enable(false);

					KeyEvent.addKeyListener(new Signal.Listener<KeyEvent>() {
						@Override
						public boolean onSignal(KeyEvent keyEvent) {
							if (!keyEvent.pressed && btnContinue.active){
								if (btnHideStory.active && !btnHideStory.icon().visible){
									btnHideStory.setRect(btnContinue.right()+2, btnContinue.top(), 20, 21);
									align(btnHideStory);
									btnHideStory.icon().visible = true;
									btnHideStory.parent.add(new Tweener(parent, 0.5f) {
										@Override
										protected void updateValues(float progress) {
											float uiAlpha = progress;
											btnContinue.alpha(uiAlpha);
											storyBG.alpha(uiAlpha*0.8f);
											storyMessage.alpha(uiAlpha);
											btnHideStory.icon().alpha(uiAlpha);
											loadingText.alpha(uiAlpha);
											im.am = uiAlpha;
										}
									});
								} else {
									phase = Phase.FADE_OUT;
									timeLeft = fadeTime;
									btnContinue.enable(false);
									Document.INTROS.readPage(region);
								}
								return true;
							}
							return false;
						}
					});

					btnContinue.setPos((Camera.main.width - btnContinue.width())/2f, storyMessage.bottom()+10);
					add(btnContinue);

					btnHideStory = new IconButton(Icons.CHEVRON.get()){
						@Override
						protected void onClick() {
							if (btnContinue.alpha() != 0 && btnContinue.alpha() != 1){
								return;
							}
							if (icon.visible) {
								enable(false);
								//button is effectively screen-sized, but invisible
								parent.add(new Tweener(parent, 0.5f) {
									@Override
									protected void updateValues(float progress) {
										float uiAlpha = 1 - progress;
										btnContinue.alpha(uiAlpha);
										storyBG.alpha(uiAlpha * 0.8f);
										storyMessage.alpha(uiAlpha);
										icon.alpha(uiAlpha);
										loadingText.alpha(uiAlpha);
										im.am = uiAlpha;
									}

									@Override
									protected void onComplete() {
										super.onComplete();
										setRect(0, 0, Camera.main.width, Camera.main.height);
										enable(true);
										icon.visible = false;
									}
								});
							} else {
								setRect(btnContinue.right()+2, btnContinue.top(), 20, 21);
								align(this);
								icon.visible = true;
								parent.add(new Tweener(parent, 0.5f) {
									@Override
									protected void updateValues(float progress) {
										float uiAlpha = progress;
										btnContinue.alpha(uiAlpha);
										storyBG.alpha(uiAlpha*0.8f);
										storyMessage.alpha(uiAlpha);
										icon.alpha(uiAlpha);
										loadingText.alpha(uiAlpha);
										im.am = uiAlpha;
									}
								});
							}
						}

						@Override
						protected void onPointerDown() {
							if (icon.visible) {
								super.onPointerDown();
							}
						}
					};
					btnHideStory.icon().originToCenter();
					btnHideStory.icon().angle = 180f;
					btnHideStory.setRect(btnContinue.right()+2, btnContinue.top(), 20, 21);
					align(btnHideStory);
					btnHideStory.enable(false);
					add(btnHideStory);

					btnContinue.alpha(0);
					storyBG.alpha(0);
					storyMessage.alpha(0);
					btnHideStory.icon().alpha(0);
			}
		}

		phase = Phase.FADE_IN;
		timeLeft = fadeTime;
		
		if (thread == null) {
			thread = new Thread() {
				@Override
				public void run() {
					
					try {

						Actor.fixTime();

						switch (mode) {
							case DESCEND:
								descend();
								break;
							case ASCEND:
								ascend();
								break;
							case CONTINUE:
								restore();
								break;
							case CHECKPOINT:
								restoreFromCheckpoint();
								break;
							case RESURRECT:
								resurrect();
								break;
							case RETURN:
								returnTo();
								break;
							case FALL:
								fall();
								break;
							case RESET:
								reset();
								break;
						}
						
					} catch (Exception e) {
						
						error = e;
						
					}

					synchronized (thread) {
						if (phase == Phase.STATIC && error == null) {
							afterLoading();
						}
					}
				}
			};
			thread.start();
		}
		waitingTime = 0f;
	}

	private int dots = 0;
	private boolean textFadingIn = true;

	@Override
	public void update() {
		super.update();

		if (btnContinue == null || !btnContinue.isActive()) {
			waitingTime += Game.elapsed;
		}

		if (mode != Mode.FALL && dots != Math.ceil(waitingTime / ((2*fadeTime)/3f))) {
			String text = Messages.get(Mode.class, mode.name());
			dots = (int)Math.ceil(waitingTime / ((2*fadeTime)/3f))%3;
			switch (dots){
				case 1: default:
					loadingText.text(text + ".");
					break;
				case 2:
					loadingText.text(text + "..");
					break;
				case 0:
					loadingText.text(text + "...");
					break;
			}
		}
		
		switch (phase) {
		
		case FADE_IN:
			loadingText.alpha( Math.max(0, fadeTime - (timeLeft-0.333f)));
			if ((timeLeft -= Game.elapsed) <= 0) {
				synchronized (thread) {
					if (!thread.isAlive() && error == null) {
						afterLoading();
					} else {
						phase = Phase.STATIC;
					}
				}
			}
			break;
			
		case FADE_OUT:
			background.acc.set(0);
			background.speed.set(0);

			loadingText.alpha( Math.min(1, timeLeft+0.333f) );

			if (btnContinue != null){
				btnContinue.alpha((timeLeft/fadeTime));
				storyMessage.alpha(btnContinue.alpha());
				storyBG.alpha(btnContinue.alpha()*0.8f);
				btnHideStory.icon().alpha(btnContinue.alpha());
			}
			
			if ((timeLeft -= Game.elapsed) <= 0) {
				Game.switchScene( GameScene.class );
				KeyEvent.clearListeners(); //removes potential listener for continue
				thread = null;
				error = null;
			}
			break;
			
		case STATIC:

			if (btnContinue != null && textFadingIn) {
				btnContinue.alpha(Math.min(1, btnContinue.alpha() + Game.elapsed));
				storyMessage.alpha(btnContinue.alpha());
				storyBG.alpha(btnContinue.alpha()*0.8f);
				btnHideStory.icon().alpha(btnContinue.alpha());

				if (btnContinue.alpha() == 1){
					textFadingIn = false;
					btnHideStory.enable(true);
				}
			}

			//slowly pan the background side to side in portait mode, if story text is displayed
			if (btnContinue != null && !textFadingIn && Game.width < Game.height){
				if (background.speed.isZero() && background.acc.isZero()){
					background.acc.x = background.center().x >= Camera.main.width ? -1f : 1f;
				} else {
					background.speed.x = GameMath.gate(-10, background.speed.x, 10);
					if (background.acc.x > 0 && background.x >= -25){
						background.acc.x = -2.5f;
					} else if (background.acc.x < 0 && background.x + background.width() <= Camera.main.width+25){
						background.acc.x = 2.5f;
					}
				}

			}

			if (error != null) {
				String errorMsg;
				if (error instanceof FileNotFoundException)     errorMsg = Messages.get(this, "file_not_found");
				else if (error instanceof IOException)          errorMsg = Messages.get(this, "io_error");
				else if (error.getMessage() != null &&
						error.getMessage().equals("old save")) errorMsg = Messages.get(this, "io_error");
				else if (error.getCause() instanceof CustomDungeonSaves.RenameRequiredException) errorMsg = error.getCause().getMessage();

				else throw new RuntimeException("fatal error occurred while moving between floors. " +
							"Seed:" + Dungeon.seed + " depth:" + Dungeon.depth, error);

				add( new WndError( errorMsg ) {
					{
						if (error.getCause() instanceof CustomDungeonSaves.RenameRequiredException) {
							setHighlightingEnabled(false);
						}
					}
					public void onBackPressed() {
						super.onBackPressed();
						Game.switchScene( StartScene.class );
					}
				} );
				thread = null;
				error = null;
			} else if (thread != null && (int)waitingTime == 10){
				waitingTime = 11f;
				String s = "";
				for (StackTraceElement t : thread.getStackTrace()){
					s += "\n";
					s += t.toString();
				}
				//we care about reporting game logic exceptions, not slow IO
				if (!s.contains("FileUtils.bundleToFile")){
					SandboxPixelDungeon.reportException(
							new RuntimeException("waited more than 10 seconds on levelgen. " +
									"Seed:" + Dungeon.seed + " depth:" + Dungeon.depth + " trace:" +
									s));
					add(new WndError(Messages.get(InterlevelScene.class,"could_not_generate", Dungeon.seed)) {
						public void onBackPressed() {
							super.onBackPressed();
							Game.switchScene(StartScene.class);
						}
					});
				}
			}
			break;
		}

		if (mode == Mode.FALL) {
			loadingText.setPos(
					//the randomization is effectively -2 to +2
					// we don't use the generator stack as levelgen may be occurring
					// and we don't want to accidentally use a seeded generator
					(Camera.main.width - loadingText.width() - 4) + 4*(Random.Float(false)-0.5f),
					(Camera.main.height - loadingText.height() - 6) + 4*(Random.Float(false)-0.5f)
			);
			align(loadingText);
		}
	}

	private void afterLoading(){
		if (btnContinue != null){
			btnContinue.visible = true;
			float alpha = btnContinue.alpha();
			btnContinue.enable(true);
			btnContinue.alpha(alpha);
			phase = Phase.STATIC;
		} else {
			phase = Phase.FADE_OUT;
			timeLeft = fadeTime;
		}

	}

	private void descend() throws IOException {

		if (Dungeon.hero == null) {
			Mob.clearHeldAllies();
			Dungeon.init();
			GameLog.wipe();

        	Level level = Dungeon.newLevel(null, 0);
        	Dungeon.switchLevel(level, -1);
    	} else {
        	Mob.holdAllies(Dungeon.level);
        	Dungeon.saveAll();

        	Level level;
        	Dungeon.depth = Dungeon.customDungeon.getFloor(curTransition.destLevel).getDepth();
			String oldLvlName = Dungeon.levelName;
			int oldBranch = Dungeon.branch;
        	Dungeon.levelName = curTransition.destLevel;
        	Dungeon.branch = curTransition.destBranch;
        	if (Dungeon.levelHasBeenGenerated(Dungeon.levelName, Dungeon.branch)) {
            	level = Dungeon.loadLevel(GamesInProgress.curSlot);
        	} else {
            	level = Dungeon.newLevel(oldLvlName, oldBranch);
        	}

        	int destCell = curTransition.destCell;
        	curTransition = null;
        	Dungeon.switchLevel(level, destCell);
    	}

	}

	private void fall() throws IOException {

		if (!Dungeon.hero.isAlive()) return;

		Mob.holdAllies( Dungeon.level );
		
		Buff.affect( Dungeon.hero, Chasm.Falling.class );
		Dungeon.saveAll();

		String destZone = Dungeon.level.zone[fallPosition] == null ? null : Dungeon.level.zone[fallPosition].chasmDestZone;

        Level level;
		String oldLvlName = Dungeon.levelName;
		int oldBranch = Dungeon.branch;
        Dungeon.levelName = Dungeon.customDungeon.getFloor(Dungeon.levelName).getChasm();
        Dungeon.depth = Dungeon.customDungeon.getFloor(Dungeon.levelName).getDepth();
        if (Dungeon.levelHasBeenGenerated(Dungeon.levelName, Dungeon.branch)) {
            level = Dungeon.loadLevel(GamesInProgress.curSlot);
        } else {
            level = Dungeon.newLevel(oldLvlName, oldBranch);
        }
        Dungeon.switchLevel(level, level.fallCell(fallIntoPit, destZone));
    }

    private void ascend() throws IOException {

        Mob.holdAllies(Dungeon.level);

        Dungeon.saveAll();
        Dungeon.depth = Dungeon.customDungeon.getFloor(curTransition.destLevel).getDepth();
		String oldLvlName = Dungeon.levelName;
		int oldBranch = Dungeon.branch;
        Dungeon.levelName = curTransition.destLevel;
        Dungeon.branch = curTransition.destBranch;
        Level level;
        if (Dungeon.levelHasBeenGenerated(Dungeon.levelName, Dungeon.branch)) {
            level = Dungeon.loadLevel(GamesInProgress.curSlot);
        } else {
            level = Dungeon.newLevel(oldLvlName, oldBranch);
        }

        int destCell;
        if (curTransition.destCell != -1) destCell = curTransition.destCell;
        else {
            curTransition.destType = LevelTransition.Type.REGULAR_EXIT;
            destCell = level.getTransition(curTransition.destCell).departCell;
        }
        curTransition = null;
        Dungeon.switchLevel(level, destCell);
//        LevelTransition destTransition = level.getTransition(curTransition.departCell);
//        curTransition = null;
//        Dungeon.switchLevel(level, destTransition.cell());
    }

    private void returnTo() throws IOException {

        Mob.holdAllies(Dungeon.level);

        Dungeon.saveAll();
        Dungeon.depth = Dungeon.customDungeon.getFloor(returnLevel).getDepth();
		String oldLvlName = Dungeon.levelName;
		int oldBranch = Dungeon.branch;
        Dungeon.levelName = returnLevel;
        Dungeon.branch = returnBranch;
        Level level;
        if (Dungeon.levelHasBeenGenerated(Dungeon.levelName, Dungeon.branch)) {
            level = Dungeon.loadLevel(GamesInProgress.curSlot);
        } else {
            level = Dungeon.newLevel(oldLvlName, oldBranch);
        }
        Dungeon.switchLevel(level, returnPos);
    }

    private void restore() throws IOException {

        Mob.clearHeldAllies();

		GameLog.wipe();

        Dungeon.loadGame(GamesInProgress.curSlot);
        Level level;
		if (Dungeon.levelHasBeenGenerated(Dungeon.levelName, Dungeon.branch)) {
            level = Dungeon.loadLevel(GamesInProgress.curSlot);
        } else {
            level = Dungeon.newLevel(null, 0);
        }
        Dungeon.switchLevel(level, Dungeon.hero.pos);
    }

	private void restoreFromCheckpoint() throws IOException {

		Mob.clearHeldAllies();

		GameLog.wipe();

		Dungeon.loadCheckpoint(GamesInProgress.curSlot);
		Level level;
		if (Dungeon.levelHasBeenGenerated(Dungeon.levelName, Dungeon.branch)) {
			level = Dungeon.loadLevel(GamesInProgress.curSlot);
		} else {
			level = Dungeon.newLevel(null, 0);
		}
		Dungeon.switchLevel(level, Dungeon.hero.pos);

		if (Dungeon.reachedCheckpoint != null
				&& Dungeon.levelName.equals(Dungeon.reachedCheckpoint.level)
				&& Dungeon.branch == Dungeon.reachedCheckpoint.branch) {
			Checkpoint cp = Dungeon.level.checkpoints.get(Dungeon.reachedCheckpoint.cell);
			if (cp != null) cp.use();
		}
	}

    private void resurrect() {

        Mob.holdAllies(Dungeon.level);

		Level level;
		if (Dungeon.level.locked()) {
			ArrayList<Item> preservedItems = Dungeon.level.getItemsToPreserveFromSealedResurrect();

			Dungeon.hero.resurrect();
			level = Dungeon.newLevel(null, 0);
			Dungeon.hero.pos = level.randomRespawnCell(Dungeon.hero);
			if (Dungeon.hero.pos == -1) Dungeon.hero.pos = level.entrance();

			for (Item i : preservedItems){
				int pos = level.randomRespawnCell(null);
				if (pos == -1) pos = level.entrance();
				level.drop(i, pos);
			}
			int pos = level.randomRespawnCell(null);
			if (pos == -1) pos = level.entrance();
			level.drop(new LostBackpack(), pos);

		} else {
			level = Dungeon.level;
			BArray.setFalse(level.heroFOV);
			BArray.setFalse(level.visited);
			BArray.setFalse(level.mapped);
			int invPos = Dungeon.hero.pos;
			int tries = 0;
			do {
				Dungeon.hero.pos = level.randomRespawnCell(Dungeon.hero);
				tries++;

				if (tries >= 10000) {
					Dungeon.hero.pos = -1;
					break;
				}

                //prevents spawning on traps or plants, prefers farther locations first
            } while (Dungeon.hero.pos == -1
                    || level.traps.get(Dungeon.hero.pos) != null
                    || (level.plants.get(Dungeon.hero.pos) != null && tries < 500)
                    || level.trueDistance(invPos, Dungeon.hero.pos) <= 30 - (tries / 10));

			//directly trample grass
			if (level.map[Dungeon.hero.pos] == Terrain.HIGH_GRASS || level.map[Dungeon.hero.pos] == Terrain.FURROWED_GRASS){
				level.map[Dungeon.hero.pos] = Terrain.GRASS;
			}
			Dungeon.hero.resurrect();
			level.drop(new LostBackpack(), invPos);
		}

		Dungeon.switchLevel( level, Dungeon.hero.pos );
	}

	private void reset() throws IOException {
		
		Mob.holdAllies( Dungeon.level );

        SpecialRoom.resetPitRoom(Dungeon.customDungeon.getFloor(Dungeon.levelName).getChasm());

		Level level = Dungeon.newLevel(null, 0);
		Dungeon.switchLevel( level, level.entrance() );
	}

	@Override
	protected void onBackPressed() {
		//Do nothing
	}
}