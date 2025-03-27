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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;
import com.shatteredpixel.shatteredpixeldungeon.editor.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.editor.levels.CustomDungeon;
import com.shatteredpixel.shatteredpixeldungeon.editor.scene.undo.ActionPart;
import com.shatteredpixel.shatteredpixeldungeon.editor.scene.undo.ActionPartList;
import com.shatteredpixel.shatteredpixeldungeon.editor.scene.undo.parts.BarrierActionPart;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.EditorUtilities;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CavesPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PylonSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.NotAllowedInLua;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.WatabouRect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CavesBossLevel extends Level {

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
	}

	@Override
	public void playLevelMusic() {
		if (locked()){
			if (BossHealthBar.bleedingActive()){
				Music.INSTANCE.play(Assets.Music.CAVES_BOSS_FINALE, true);
			} else {
				Music.INSTANCE.play(Assets.Music.CAVES_BOSS, true);
			}
		//if wall isn't broken
		} else if (map[14 + 13*width()] == Terrain.CUSTOM_DECO){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(CavesLevel.CAVES_TRACK_LIST, CavesLevel.CAVES_TRACK_CHANCES, false);
		}
	}

	private static int WIDTH = 33;
	private static int HEIGHT = 42;

	public static WatabouRect diggableArea = new WatabouRect(2, 11, 31, 40);
	public static WatabouRect mainArena = new WatabouRect(5, 14, 28, 37);
	public static WatabouRect gate = new WatabouRect(14, 13, 19, 14);
	public static int[] pylonPositions = new int[]{ 4 + 13*WIDTH, 28 + 13*WIDTH, 4 + 37*WIDTH, 28 + 37*WIDTH };

	private ArenaVisuals customArenaVisuals;

	private int entranceCell, exitCell;

	@Override
	protected boolean build() {

		setSize(WIDTH, HEIGHT);

		Painter.fill(this, gate, Terrain.CUSTOM_DECO);

		//set up main boss arena
		Painter.fillEllipse(this, mainArena, Terrain.EMPTY);

		boolean[] patch = Patch.generate( width, height-14, 0.15f, 2, true );
		for (int i= 14*width(); i < length(); i++) {
			if (map[i] == Terrain.EMPTY) {
				if (patch[i - 14*width()]){
					map[i] = Terrain.WATER;
				} else if (Random.Int(Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 4 : 8) == 0){
					map[i] = Terrain.INACTIVE_TRAP;
				}
			}
		}

		buildEntrance();
		buildCorners();

		new CavesPainter().paint(this, null);

		//setup exit area above main boss arena
		Painter.fill(this, 0, 3, width(), 4, Terrain.CHASM);
		Painter.fill(this, 6, 7, 21, 1, Terrain.CHASM);
		Painter.fill(this, 10, 8, 13, 1, Terrain.CHASM);
		Painter.fill(this, 12, 9, 9, 1, Terrain.CHASM);
		Painter.fill(this, 13, 10, 7, 1, Terrain.CHASM);
		Painter.fill(this, 14, 3, 5, 10, Terrain.EMPTY);

		//fill in special floor, statues, and exits
		Painter.fill(this, 15, 2, 3, 3, Terrain.EMPTY_SP);
		Painter.fill(this, 15, 5, 3, 1, Terrain.STATUE);
		Painter.fill(this, 15, 7, 3, 1, Terrain.STATUE);
		Painter.fill(this, 15, 9, 3, 1, Terrain.STATUE);
		Painter.fill(this, 16, 5, 1, 6, Terrain.EMPTY_SP);
		Painter.fill(this, 15, 0, 3, 3, Terrain.EXIT);

		exitCell = 16 + 2*width();
		LevelTransition exit = addRegularExit(exitCell);
		if (exit != null) {
			exit.set(14, 0, 18, 2);
		}

		CustomTilemap customVisuals = new EntranceOverhang();
		customVisuals.setRect(0, 0, width(), HEIGHT);
		customWalls.add(customVisuals);

		customVisuals = customArenaVisuals = new ArenaVisuals();
		customVisuals.setRect(0, 0, width(), HEIGHT);
		customTiles.add(customVisuals);

		//ensures that all pylons can be reached without stepping over water or wires
		boolean[] pass = new boolean[length];
		for (int i = 0; i < length; i++){
			pass[i] = map[i] == Terrain.EMPTY || map[i] == Terrain.EMPTY_SP || map[i] == Terrain.EMPTY_DECO;
		}
		PathFinder.buildDistanceMap(16 + 25*width(), pass, null);
		for (int i : pylonPositions){
			if (PathFinder.distance[i] == Integer.MAX_VALUE){
				return false;
			}
		}
		return true;

	}

	@Override
	public int entrance() {
		int entr = super.entrance();
		return entr == 0 ? entranceCell : entr;
	}

	@Override
	public int exit() {
		int exit = super.exit();
		return exit == 0 ? exitCell : exit;
	}

	private static final String ENTRANCE_CELL = "entrance_cell";
	private static final String EXIT_CELL = "exit_cell";

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		entranceCell = bundle.getInt(ENTRANCE_CELL);
		exitCell = bundle.getInt(EXIT_CELL);
		for (CustomTilemap c : customTiles){
			if (c instanceof ArenaVisuals){
				customArenaVisuals = (ArenaVisuals) c;
			}
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ENTRANCE_CELL, entranceCell);
		bundle.put(EXIT_CELL, exitCell);
	}

	@Override
	protected void createMobs() {
		for (int i : pylonPositions) {
			Pylon pylon = new Pylon();
			pylon.pos = i;
			mobs.add(pylon);
		}
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				int pos;
				do {
					pos = randomRespawnCell(null);
				} while (pos == entrance());
				for (Item i : bonesItems) {
					drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = entrance() + i;
			if (isPassable(cell, ch)
					&& Actor.findChar(cell) == null
					&& (!Char.hasProp(ch, Char.Property.LARGE) || openSpace[cell])){
				candidates.add(cell);
			}
		}

		if (candidates.isEmpty()){
			return -1;
		} else {
			return Random.element(candidates);
		}
	}

	@Override
	public boolean canSetCellToWater(boolean includeTraps, int cell) {
		for (int i : pylonPositions){
			if (Dungeon.level.distance(cell, i) <= 1){
				return false;
			}
		}
		return super.canSetCellToWater(includeTraps, cell);
	}

	@Override
	public boolean invalidHeroPos(int tile) {
		//hero cannot be above gate, or above arena, when gate is closed
		if (map[gate.left + gate.top*width()] == Terrain.CUSTOM_DECO){
			Point p = cellToPoint(tile);
			if (p.y < diggableArea.top){
				return true;
			} else if (p.y < gate.bottom && p.x >= gate.left && p.x < gate.right){
				return true;
			}
		}
		return super.invalidHeroPos(tile);
	}

	@Override
	public void occupyCell(Char ch) {
		//seal the level when the hero moves near to a pylon, the level isn't already sealed, and the gate hasn't been destroyed
		int gatePos = pointToCell(new Point(gate.left, gate.top));
		if (ch == Dungeon.hero && !locked() && solid[gatePos]){
			for (int pos : pylonPositions){
				if (Dungeon.level.distance(ch.pos, pos) <= 3){
					seal();
					break;
				}
			}
		}

		super.occupyCell(ch);
	}

	@Override
	public void seal() {
		super.seal();

		int entrance = entrance();
		set( entrance, Terrain.WALL );

		Heap heap = Dungeon.level.heaps.get( entrance );
		while (heap != null && !heap.isEmpty()) {
			int n;
			do {
				n = entrance + PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.isPassableHero(n));
			Heap dropped = Dungeon.level.drop(heap.pickUp(), n);
			dropped.seen = heap.seen;
		}

		Char ch = Actor.findChar( entrance );
		if (ch != null) {
			int n;
			do {
				n = entrance + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
			} while (!Dungeon.level.isPassable(n, ch));
			ch.pos = n;
			ch.sprite.place(n);
		}

		GameScene.updateMap( entrance );
		Dungeon.observe();

		CellEmitter.get( entrance ).start( Speck.factory( Speck.ROCK ), 0.07f, 10 );
		PixelScene.shake( 3, 0.7f );
		Sample.INSTANCE.play( Assets.Sounds.ROCKS );

		DM300 boss = new DM300();
		boss.setLevel(Dungeon.depth);
		boss.state = boss.WANDERING;
		do {
			boss.pos = pointToCell(Random.element(mainArena.getPoints()));
		} while (!openSpace[boss.pos] || map[boss.pos] == Terrain.EMPTY_SP || Actor.findChar(boss.pos) != null);
		GameScene.add( boss );

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play(Assets.Music.CAVES_BOSS, true);
			}
		});

	}

	@Override
	public void unseal() {
		super.unseal();

		if (!locked()) {
			blobs.getOnly(PylonEnergy.class).fullyClear();

			set(entrance(), Terrain.ENTRANCE);
			int i = gate.top * width();
			for (int j = gate.left; j < gate.right; j++) {
				set(i + j, Terrain.EMPTY);
				if (Dungeon.level.heroFOV[i + j]) {
					CellEmitter.get(i + j).burst(BlastParticle.FACTORY, 10);
				}
			}
			GameScene.updateMap();

			if (customArenaVisuals != null) customArenaVisuals.updateState();

			Dungeon.observe();

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut(5f, new Callback() {
						@Override
						public void call() {
							Music.INSTANCE.end();
						}
					});
				}
			});
		}

	}

	public static List<Pylon> getAvailablePylons(Level level, int dm300id) {
		ArrayList<Pylon> pylons = new ArrayList<>(5);
		for (Mob m : level.mobs){
			if (m instanceof Pylon
					&& m.alignment == Char.Alignment.NEUTRAL
					&& m.playerAlignment == Mob.NORMAL_ALIGNMENT
					&& !((Pylon) m).alwaysActive
					&& (((Pylon) m).dm300id <= 0 || ((Pylon) m).dm300id == dm300id)){
				pylons.add((Pylon) m);
			}
		}
		return pylons;
	}

	public Pylon activatePylon(int dm300id){
		Pylon pylon = activatePylon(this, getAvailablePylons(this, dm300id));

		for( int i = (mainArena.top-1)*width; i <length; i++){
			if (map[i] == Terrain.INACTIVE_TRAP || map[i] == Terrain.WATER || map[i] == Terrain.CUSTOM_DECO){
				GameScene.add(Blob.seed(i, 1, PylonEnergy.class));
			}
		}

		return pylon;
	}

	public static Pylon activatePylon(Level level, List<Pylon> pylons){

		if (pylons.size() == 1){
			pylons.get(0).activate();
			return pylons.get(0);
		} else if (!pylons.isEmpty()) {
			Pylon closest = null;
			for (Pylon p : pylons){
				if (closest == null || level.trueDistance(p.pos, Dungeon.hero.pos) < level.trueDistance(closest.pos, Dungeon.hero.pos)){
					closest = p;
				}
			}
			pylons.remove(closest);
			Pylon p = Random.element(pylons);
			p.activate();
			return p;
		}
		return null;

	}

	public static void eliminatePylon(Level level, DM300 boss, boolean clearPylonEnergy){
		if (level instanceof CavesBossLevel){
			if (((CavesBossLevel) level).customArenaVisuals != null)
				((CavesBossLevel) level).customArenaVisuals.updateState();
		}
		if (boss != null) {
			boss.loseSupercharge();
			PylonEnergy.energySourceSprite = boss.sprite;
		}
		if (clearPylonEnergy) {
			Blob energy = level.blobs.getOnly(PylonEnergy.class);
			if (energy != null) energy.fullyClear();
		}
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.GRASS:
				return Messages.get(CavesLevel.class, "grass_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(CavesLevel.class, "high_grass_name");
			case Terrain.WATER:
				return Messages.get(CavesLevel.class, "water_name");
			case Terrain.STATUE:
				//city statues are used
				return Messages.get(CityLevel.class, "statue_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile, int cell ) {
		switch (tile) {
			case Terrain.WATER:
				return super.tileDesc( tile, cell ) + "\n\n" + Messages.get(CavesBossLevel.class, "water_desc");
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(CavesLevel.class, "entrance_desc") + appendNoTransWarning(cell);
			case Terrain.EXIT:
				//city exit is used
				return Messages.get(CityLevel.class, "exit_desc") + appendNoTransWarning(cell);
			case Terrain.HIGH_GRASS:
				return Messages.get(CavesLevel.class, "high_grass_desc");
			case Terrain.WALL_DECO:
				return Messages.get(CavesLevel.class, "wall_deco_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(CavesLevel.class, "bookshelf_desc");
			//city statues are used
			case Terrain.STATUE:
				return Messages.get(CityLevel.class, "statue_desc");
			default:
				return super.tileDesc( tile, cell );
		}
	}

	/**
	 * semi-randomized setup for entrance and corners
	 */

	private static final short n = -1; //used when a tile shouldn't be changed
	private static final short W = Terrain.WALL;
	private static final short e = Terrain.EMPTY;
	private static final short s = Terrain.EMPTY_SP;

	private static short[] entrance1 = {
			n, n, n, n, n, n, n, n,
			n, n, n, n, n, n, n, n,
			n, n, n, n, W, e, W, W,
			n, n, n, W, W, e, W, W,
			n, n, W, W, e, e, e, e,
			n, n, e, e, e, W, W, e,
			n, n, W, W, e, W, e, e,
			n, n, W, W, e, e, e, e
	};

	private static short[] entrance2 = {
			n, n, n, n, n, n, n, n,
			n, n, n, n, n, n, n, n,
			n, n, n, n, n, e, e, e,
			n, n, n, W, e, W, W, e,
			n, n, n, e, e, e, e, e,
			n, n, e, W, e, W, W, e,
			n, n, e, W, e, W, e, e,
			n, n, e, e, e, e, e, e
	};

	private static short[] entrance3 = {
			n, n, n, n, n, n, n, n,
			n, n, n, n, n, n, n, n,
			n, n, n, n, n, n, n, n,
			n, n, n, W, W, e, W, W,
			n, n, n, W, W, e, W, W,
			n, n, n, e, e, e, e, e,
			n, n, n, W, W, e, W, e,
			n, n, n, W, W, e, e, e
	};

	private static short[] entrance4 = {
			n, n, n, n, n, n, n, n,
			n, n, n, n, n, n, n, e,
			n, n, n, n, n, n, W, e,
			n, n, n, n, n, W, W, e,
			n, n, n, n, W, W, W, e,
			n, n, n, W, W, W, W, e,
			n, n, W, W, W, W, e, e,
			n, e, e, e, e, e, e, e
	};

	private static short[][] entranceVariants = {
			entrance1,
			entrance2,
			entrance3,
			entrance4
	};

	private void buildEntrance(){
		int entranceCell = 16 + 25*width();

		//entrance area
		int NW = entranceCell - 7 - 7*width();
		int NE = entranceCell + 7 - 7*width();
		int SE = entranceCell + 7 + 7*width();
		int SW = entranceCell - 7 + 7*width();

		short[] entranceTiles = Random.oneOf(entranceVariants);
		for (int i = 0; i < entranceTiles.length; i++){
			if (i % 8 == 0 && i != 0){
				NW += (width() - 8);
				NE += (width() + 8);
				SE -= (width() - 8);
				SW -= (width() + 8);
			}

			if (entranceTiles[i] != n) map[NW] = map[NE] = map[SE] = map[SW] = entranceTiles[i];
			NW++; NE--; SW++; SE--;
		}

		Painter.set(this, entranceCell, Terrain.ENTRANCE);
		addRegularEntrance(entranceCell);
	}

	private static short[] corner1 = {
			W, W, W, W, W, W, W, W, W, W,
			W, s, s, s, e, e, e, W, W, W,
			W, s, s, s, W, W, e, e, W, W,
			W, s, s, s, W, W, W, e, e, W,
			W, e, W, W, W, W, W, W, e, n,
			W, e, W, W, W, W, W, n, n, n,
			W, e, e, W, W, W, n, n, n, n,
			W, W, e, e, W, n, n, n, n, n,
			W, W, W, e, e, n, n, n, n, n,
			W, W, W, W, n, n, n, n, n, n,
	};

	private static short[] corner2 = {
			W, W, W, W, W, W, W, W, W, W,
			W, s, s, s, W, W, W, W, W, W,
			W, s, s, s, e, e, e, e, e, W,
			W, s, s, s, W, W, W, W, e, e,
			W, W, e, W, W, W, W, W, W, e,
			W, W, e, W, W, W, W, n, n, n,
			W, W, e, W, W, W, n, n, n, n,
			W, W, e, W, W, n, n, n, n, n,
			W, W, e, e, W, n, n, n, n, n,
			W, W, W, e, e, n, n, n, n, n,
	};

	private static short[] corner3 = {
			W, W, W, W, W, W, W, W, W, W,
			W, s, s, s, W, W, W, W, W, W,
			W, s, s, s, e, e, e, e, W, W,
			W, s, s, s, W, W, W, e, W, W,
			W, W, e, W, W, W, W, e, W, n,
			W, W, e, W, W, W, W, e, e, n,
			W, W, e, W, W, W, n, n, n, n,
			W, W, e, e, e, e, n, n, n, n,
			W, W, W, W, W, e, n, n, n, n,
			W, W, W, W, n, n, n, n, n, n,
	};

	private static short[] corner4 = {
			W, W, W, W, W, W, W, W, W, W,
			W, s, s, s, W, W, W, W, W, W,
			W, s, s, s, e, e, e, W, W, W,
			W, s, s, s, W, W, e, W, W, W,
			W, W, e, W, W, W, e, W, W, n,
			W, W, e, W, W, W, e, e, n, n,
			W, W, e, e, e, e, e, n, n, n,
			W, W, W, W, W, e, n, n, n, n,
			W, W, W, W, W, n, n, n, n, n,
			W, W, W, W, n, n, n, n, n, n,
	};

	private static short[][] cornerVariants = {
			corner1,
			corner2,
			corner3,
			corner4
	};

	private void buildCorners(){
		int NW = 2 + 11*width();
		int NE = 30 + 11*width();
		int SE = 30 + 39*width();
		int SW = 2 + 39*width();

		short[] cornerTiles = Random.oneOf(cornerVariants);
		for(int i = 0; i < cornerTiles.length; i++){
			if (i % 10 == 0 && i != 0){
				NW += (width() - 10);
				NE += (width() + 10);
				SE -= (width() - 10);
				SW -= (width() + 10);
			}

			if (cornerTiles[i] != n) map[NW] = map[NE] = map[SE] = map[SW] = cornerTiles[i];
			NW++; NE--; SW++; SE--;
		}
	}

	/**
	 * Visual Effects
	 */

	public static class CityEntrance extends CustomTilemap{

		{
			texture = Assets.Environment.CAVES_BOSS;
		}

		private static short[] entryWay = new short[]{
				-1,  7,  7,  7, -1,
				-1,  1,  2,  3, -1,
				 8,  1,  2,  3, 12,
				16,  9, 10, 11, 20,
				16, 16, 18, 20, 20,
				16, 17, 18, 19, 20,
				16, 16, 18, 20, 20,
				16, 17, 18, 19, 20,
				16, 16, 18, 20, 20,
				16, 17, 18, 19, 20,
				24, 25, 26, 27, 28
		};

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			int entryPos = 0;
			for (int i = 0; i < data.length; i++){

				//override the entryway
				if (i % tileW == tileW/2 - 2){
					data[i++] = entryWay[entryPos++];
					data[i++] = entryWay[entryPos++];
					data[i++] = entryWay[entryPos++];
					data[i++] = entryWay[entryPos++];
					data[i] = entryWay[entryPos++];

				//otherwise check if we are on row 2 or 3, in which case we need to override walls
				} else {
					if (i / tileW == 2) data[i] = 13;
					else if (i / tileW == 3) data[i] = 21;
					else data[i] = -1;
				}
			}
			v.map( data, tileW );
			return v;
		}

	}

	public static class EntranceOverhang extends CustomTilemap implements CustomTilemap.BossLevelVisuals {

		{
			texture = Assets.Environment.CAVES_BOSS;

			wallVisual = true;
		}

		private static short[] entryWay = new short[]{
				 0,  7,  7,  7,  4,
				 0, 15, 15, 15,  4,
				-1, 23, 23, 23, -1,
				-1, -1, -1, -1, -1,
				-1,  6, -1, 14, -1,
				-1, -1, -1, -1, -1,
				-1,  6, -1, 14, -1,
				-1, -1, -1, -1, -1,
				-1,  6, -1, 14, -1,
				-1, -1, -1, -1, -1,
				-1, -1, -1, -1, -1,
		};

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			updateState();
			return v;
		}

		@Override
		public void updateState() {
			if (vis != null) {
				int[] data = new int[tileW * tileH];
				Arrays.fill(data, -1);
				int length = 11 * tileW;
				for (int i = 0; i < length; i++) {

					if (i / tileW == 0) {
						if (Dungeon.level.map[i] == Terrain.WALL) {
							if (i % tileW <= tileW / 2)data[i] = Dungeon.level.map[i + 1] == Terrain.WALL ? -1 : 0;
							else data[i] = Dungeon.level.map[i - 1] == Terrain.WALL ? -1 : 4;
						} else data[i] = 7;
					} else if (i / tileW == 1) {
						if (Dungeon.level.map[i] == Terrain.WALL) {
							if (i % tileW <= tileW / 2)data[i] = Dungeon.level.map[i + 1] == Terrain.WALL ? -1 : 0;
							else data[i] = Dungeon.level.map[i - 1] == Terrain.WALL ? -1 : 4;
						} else data[i] = 15;
					} else if (i / tileW == 2) {
						if (Dungeon.level.map[i] == Terrain.WALL) data[i] = -1;
						else data[i] = 23;
					} else if (Dungeon.level.map[i + Dungeon.level.width()] == Terrain.STATUE) {
						if (i % tileW <= tileW / 2) data[i] = 6;
						else data[i] = 14;
					} else data[i] = -1;
				}
				vis.map( data, tileW );
			}
		}
	}

	private static final int IMG_INACTIVE_TRAP = 37;

	public static class ArenaVisuals extends CustomTilemap implements CustomTilemap.BossLevelVisuals {

		{
			texture = Assets.Environment.CAVES_BOSS;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			updateState( );
			return v;
		}

		@Override
		public void updateState( ){
			Set<Integer> pylons = new HashSet<>(7);
			for (Mob m : Dungeon.level.mobs) {
				if (m instanceof Pylon
						&& !((Pylon) m).alwaysActive
						&& m.playerAlignment == Mob.NORMAL_ALIGNMENT
						&& m.alignment == Char.Alignment.NEUTRAL)
					pylons.add(m.pos);
			}

			if (vis != null){
				int[] data = new int[tileW*tileH];
				Arrays.fill(data, -1);
				int j = Dungeon.level.width() * (tileY == 0 ? 12 : tileY);

				if (tileY == 0) {
					//upper part of the level, mostly city tiles
					int width = Dungeon.level.width();
					int length = 11 * width;
					for (int i = 0; i < length; i++){

						//Do not override statues, empty or empty_sp

						if (Dungeon.level.map[i] == Terrain.EMPTY) {
							if (i / tileW == 10) {
								if (Dungeon.level.map[i - 1] == Terrain.CHASM) data[i] = 24;
								else if (data[i - 1] == 24) data[i] = 25;
								else if (Dungeon.level.map[i - 1] == Terrain.EMPTY_SP) data[i] = 27;
								else if (data[i - 1] == 27) data[i] = 28;
								else if (i % tileW <= tileW / 2) data[i] = 16;
								else data[i] = 20;
							} else {
								if (i % tileW <= tileW / 2) data[i] = 16;
								else data[i] = 20;
							}
						} else if (Dungeon.level.map[i] == Terrain.STATUE) {
							if (i % tileW <= tileW / 2) data[i] = 17;
							else data[i] = 19;
						} else if (Dungeon.level.map[i] == Terrain.EMPTY_SP || Dungeon.level.map[i] == Terrain.EXIT) {
							if (i / tileW == 0) data[i] = 7;
							else {
								int neighbours = EditorUtilities.stitchNeighbours(i, Terrain.EMPTY_SP, Dungeon.level)
										| EditorUtilities.stitchNeighbours(i, Terrain.EXIT, Dungeon.level);
								if ((neighbours & EditorUtilities.BOTTOM) == 0) {
									if ((neighbours & EditorUtilities.RIGHT) != 0) data[i] = 9;
									else if ((neighbours & EditorUtilities.LEFT) != 0) data[i] = 11;
									else data[i] = 26;
								} else if ((neighbours & EditorUtilities.LEFT) == 0) {
									if ((neighbours & EditorUtilities.RIGHT) == 0) data[i] = 18;
									else data[i] = 1;
								} else if ((neighbours & EditorUtilities.RIGHT) == 0) {
									data[i] = 3;
								} else if ((neighbours & EditorUtilities.BOTTOM_RIGHT) == 0 && (neighbours & EditorUtilities.BOTTOM_LEFT) == 0) {
									data[i] = 10;
								} else data[i] = 2;
							}

						} else {
							//Override walls in row 2 and 3
							if (i / tileW == 2) data[i] = 13;
							else if (i / tileW == 3) data[i] = 21;
							else data[i] = -1;
						}
					}
				}

				int gatePos = -1;
				boolean gateClosed = true;
				int start = tileY == 12 ? 0 : 12 * Dungeon.level.width();
				for (int i = start; i < data.length; i++){
					//There is a gap between both tilemaps
//					if (tileY == 0 && i < Dungeon.level.width()) {
////						data[i] = -1;
//						continue;
//					}

					if (Dungeon.level.map[j] == Terrain.EMPTY_SP) {
						for (int k : pylons) {
							if (k == j) {
								if (Dungeon.level.locked()
										&& !(Actor.findChar(k) instanceof Pylon)) {
									data[i] = 38;
								} else {
									data[i] = -1;
								}
							} else if (Dungeon.level.adjacent(k, j)) {
								int w = Dungeon.level.width;
								data[i] = 54 + (j % w + 8 * (j / w)) - (k % w + 8 * (k / w));
							}
						}
					} else if (Dungeon.level.map[j] == Terrain.INACTIVE_TRAP){
						data[i] = IMG_INACTIVE_TRAP;
					} else if (gate.inside(Dungeon.level.cellToPoint(j))){
						if (gatePos == -1){
							gateClosed = Dungeon.level.solid[j];
							gatePos = i - (gateClosed ? 40 : 32);
						}
						if (Dungeon.level.solid[i] == gateClosed) data[i] = i - gatePos;
						else data[i] = -1;
					} else {
						data[i] = -1;
					}

					j++;
				}
				vis.map(data, tileW);
			}
		}

		@Override
		public String name(int tileX, int tileY) {
			int i = tileX + tileW*(tileY + this.tileY);
			if (Dungeon.level.map[i] == Terrain.INACTIVE_TRAP){
				return Messages.get(CavesBossLevel.class, "wires_name");
			} else if (gate.inside(Dungeon.level.cellToPoint(i))){
				return Messages.get(CavesBossLevel.class, "gate_name");
			}

			return super.name(tileX, tileY);
		}

		@Override
		public String desc(int tileX, int tileY) {
			if (tileY < 12) return super.desc(tileX, tileY);

			int i = tileX + tileW*(tileY + this.tileY);
			if (Dungeon.level.map[i] == Terrain.INACTIVE_TRAP){
				return Messages.get(CavesBossLevel.class, "wires_desc");
			} else if (gate.inside(Dungeon.level.cellToPoint(i))){
				if (Dungeon.level.solid[i]){
					return Messages.get(CavesBossLevel.class, "gate_desc");
				} else {
					return Messages.get(CavesBossLevel.class, "gate_desc_broken");
				}
			}
			return super.desc(tileX, tileY);
		}

		@Override
		public Image image(int tileX, int tileY) {
			int i = tileX + tileW*(tileY + this.tileY);
			for (int k : pylonPositions){
				if (Dungeon.level.distance(i, k) <= 1){
					return null;
				}
			}

			return super.image(tileX, tileY);

		}
	}

	public static class TrapTile extends CustomTilemap {

		{
			texture = Assets.Environment.CAVES_BOSS;

			terrain = Terrain.INACTIVE_TRAP;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			Arrays.fill(data, IMG_INACTIVE_TRAP);
			v.map( data, tileW );
			return v;
		}

		@Override
		public String name(int tileX, int tileY) {
			return Messages.get(CavesBossLevel.class, "wires_name");
		}

		@Override
		public String desc(int tileX, int tileY) {
			return Messages.get(CavesBossLevel.class, "wires_desc");
		}

	}

	public static class PylonEnergy extends Blob {

		@Override
		protected void evolve() {
			for (int cell = 0; cell < Dungeon.level.length(); cell++) {
				if (Dungeon.level.insideMap(cell)) {
					off[cell] = cur[cell];

					//instantly spreads to water cells
					if (off[cell] == 0 && Dungeon.level.water[cell]){
						off[cell]++;
					}

					volume += off[cell];

					if (off[cell] > 0){

						Char ch = Actor.findChar(cell);
						if (ch != null && !(ch instanceof DM300) && !ch.isFlying()) {
							Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
							ch.damage( Random.NormalIntRange(6, 12), new Electricity());
							ch.sprite.flash();

							if (ch == Dungeon.hero){
								if (energySourceSprite != null && energySourceSprite instanceof PylonSprite){
									//took damage while DM-300 was supercharged
									Statistics.qualifiedForBossChallengesBadge[2] = false;
								}
								Statistics.bossScores[2] -= 200;
								if ( !ch.isAlive()) {
									Dungeon.fail(DM300.class);
									GLog.n(Messages.get(Electricity.class, "ondeath"));
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void fullyClear() {
			super.fullyClear();
			energySourceSprite = null;
		}

		private static CharSprite energySourceSprite = null;

		private static Emitter.Factory DIRECTED_SPARKS = new Emitter.Factory() {
			@Override
			public void emit(Emitter emitter, int index, float x, float y) {
				if (energySourceSprite == null){
					for (Char c : Actor.chars()){
						if (c instanceof Pylon && c.alignment != Char.Alignment.NEUTRAL){
							energySourceSprite = c.sprite;
							break;
						} else if (c instanceof DM300){
							energySourceSprite = c.sprite;
						}
					}
					if (energySourceSprite == null){
						return;
					}
				}

				float dist = (float)Math.max( Math.abs(energySourceSprite.x - x), Math.abs(energySourceSprite.y - y) );
				dist = GameMath.gate(0, dist-40, 320);
				//more sparks closer up
				if (Random.Float(360) > dist) {

					SparkParticle s = ((SparkParticle) emitter.recycle(SparkParticle.class));
					s.resetAttracting(x, y, energySourceSprite);
				}
			}

			@Override
			public boolean lightMode() {
				return true;
			}
		};

		@Override
		public String tileDesc() {
			return Messages.get(CavesBossLevel.class, "energy_desc");
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use( emitter );
			energySourceSprite = null;
			emitter.pour(DIRECTED_SPARKS, 0.08f);
		}

	}
	
	public static class MetalGate extends CustomTilemap {
		
		{
			texture = Assets.Environment.METAL_GATE;
			
			tileW = 5;
			tileH = 2;
			offsetCenterX = 2;
			offsetCenterY = 1;
		}
		
		private boolean broken;
		
		@Override
		public String name() {
			return Messages.get(CavesBossLevel.class, "gate_name");
		}
		
		@Override
		public String name(int tileX, int tileY) {
			return name();
		}
		
		@Override
		public String desc() {
			if (broken) {
				return Messages.get(CavesBossLevel.class, "gate_desc_broken");
			} else {
				return Messages.get(CavesBossLevel.class, "gate_desc");
			}
		}
		
		@Override
		public String desc(int tileX, int tileY) {
			return desc();
		}
		
		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			updateState( );
			return v;
		}
		
		private void updateState() {
			if (vis != null) {
				int[] data = new int[tileW * tileH];
				Arrays.fill(data, -1);
				
				int centerColumnL, centerColumnR;
				if (tileW % 2 == 0) {
					centerColumnL = (tileW - 1) / 2;
					centerColumnR = (tileW) / 2;
				} else {
					centerColumnL = centerColumnR = tileW / 2;
				}
				if (broken) {
					int start = tileW * (tileH - 1);
					for (int i = start; i < data.length; i++) {
						if (i == 0) data[i] = 0;
						else if (i == centerColumnL || i == centerColumnR) data[i] = 2;
						else if (i == tileW - 1) data[i] = 4;
						else if (i < centerColumnL) data[i] = 1;
						else data[i] = 3;
					}
				} else {
					int i = 0;
					int xxx = 0;
					for (int j = 0; j < tileH; j++) {
						if (j == 1) {//enter second row
							xxx = 5;
						}
						for (int k = 0; k < tileW; k++) {
							if (k == 0) data[i] = 0 + xxx;
							else if (k == centerColumnL || k == centerColumnR) data[i] = 2 + xxx;
							else if (k == tileW - 1) data[i] = 4 + xxx;
							else if (k < centerColumnL) data[i] = 1 + xxx;
							else data[i] = 3 + xxx;
							i++;
						}
					}
				}
				vis.map(data, tileW);
				
			}
		}
		
		@NotAllowedInLua
		public ActionPart placeBarriers() {
			ActionPartList parts = new ActionPartList();
			if (!broken) {
				int start = (tileX - 1) + (tileY + 1) * Dungeon.level.width();
				for (int i = 1; i < tileH; i++) {//skip first row
					for (int j = 0; j < tileW; j++) {
						int cell = start + i + j;
						parts.addActionPart(new BarrierActionPart.Place(new Barrier(cell)));
					}
				}
				parts.redo();
			}
			return parts;
		}
		
		@NotAllowedInLua
		public ActionPart removeBarriers() {
			ActionPartList parts = new ActionPartList();
			if (!broken) {
				int start = (tileX - 1) + (tileY + 1) * Dungeon.level.width();
				for (int i = 1; i < tileH; i++) {//skip first row
					for (int j = 0; j < tileW; j++) {
						int cell = start + i + j;
						Barrier b = Dungeon.level.barriers.get(cell);
						if (b != null) parts.addActionPart(new BarrierActionPart.Remove(b));
					}
				}
				parts.redo();
			}
			return parts;
		}
		
		public boolean isBroken() {
			return broken;
		}
		
		public void setBroken(boolean broken) {
			if (this.broken == broken) {
				return;
			}
			if (CustomDungeon.isEditing()) {
				this.broken = broken;
				updateState();
				GameScene.updateMap();
			} else {
				if (broken) open();
				else 	    close();
			}
		}
		
		public void open() {
			if (!broken) {
				
				broken = true;
				
				int start = (tileX-1) + (tileY+1)*Dungeon.level.width();
				for (int i = 1; i < tileH; i++) {
					for (int j = 0; j < tileW; j++) {
						int cell = start + i + j;
						Dungeon.level.barriers.remove(cell);
						Level.set(cell, Dungeon.level.map[cell]);
						if (Dungeon.hero != null && Dungeon.level.heroFOV[cell]) {
							CellEmitter.get(cell).burst(BlastParticle.FACTORY, 10);
						}
					}
				}
				
				updateState();
				GameScene.updateMap();
				Dungeon.observe();
			}
		}
		
		public void close() {
			if (broken) {
				
				broken = false;
				
				int start = (tileX-1) + (tileY+1)*Dungeon.level.width();
				for (int i = 1; i < tileH; i++) {
					for (int j = 0; j < tileW; j++) {
						int cell = start + i + j;
						Dungeon.level.barriers.put(cell, new Barrier(cell));
						Level.set(cell, Dungeon.level.map[cell]);
					}
				}
				
				updateState();
				GameScene.updateMap();
				Dungeon.observe();
			}
		}
		
		private static final String BROKEN = "broken";
		
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(BROKEN, broken);
		}
		
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			broken = bundle.getBoolean(BROKEN);
		}
	}
}
