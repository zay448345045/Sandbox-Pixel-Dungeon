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

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.editor.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.other.CustomParticle;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.other.PermaGas;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import com.watabou.utils.WatabouRect;

import java.util.HashMap;
import java.util.Map;

public class Blob extends Actor {

	public static final Map<Class<? extends Blob>, Integer> volumeInInv = new HashMap<>(7);

	{
		actPriority = BLOB_PRIO;
	}
	
	public int volume = 0;
	
	public int[] cur;
	protected int[] off;
	
	public BlobEmitter emitter;

	public WatabouRect area = new WatabouRect();
	
	public boolean alwaysVisible = false;

	private static final String CUR		= "cur";
	private static final String START	= "start";
	private static final String LENGTH	= "length";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		
		if (volume > 0) {
		
			int start;
			for (start=0; start < Dungeon.level.length(); start++) {
				if (cur[start] > 0) {
					break;
				}
			}
			int end;
			for (end=Dungeon.level.length()-1; end > start; end--) {
				if (cur[end] > 0) {
					break;
				}
			}
			
			bundle.put( START, start );
			bundle.put( LENGTH, cur.length );
			bundle.put( CUR, trim( start, end + 1, cur ) );
			
		}
	}
	
	protected static int[] trim(int start, int end, int[] array) {
		int len = end - start;
		int[] copy = new int[len];
		System.arraycopy( array, start, copy, 0, len );
		return copy;
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		
		super.restoreFromBundle( bundle );

		if (bundle.contains( CUR )) {

			cur = new int[bundle.getInt(LENGTH)];
			off = new int[cur.length];

			int[] data = bundle.getIntArray(CUR);
			int start = bundle.getInt(START);
			for (int i = 0; i < data.length; i++) {
				cur[i + start] = data[i];
				volume += data[i];
			}

		}
	}

	public void changeSizeOfOffToNewMapSizeAndClearIt(int newLength){
		off = new int[newLength];
	}
	
	@Override
	public boolean act() {
		
		spend( TICK );
		
		if (volume > 0) {

			if (area.isEmpty())
				setupArea();

			volume = 0;

			evolve();
			int[] tmp = off;
			off = cur;
			cur = tmp;
			
		} else {
			if (!area.isEmpty()) {
				area.setEmpty();
				//clear any values remaining in off
				System.arraycopy(cur, 0, off, 0, cur.length);
			}
		}
		
		return true;
	}

	public void setupArea(){
		for (int cell=0; cell < cur.length; cell++) {
			if (cur[cell] != 0){
				area.union(cell%Dungeon.level.width(), cell/Dungeon.level.width());
			}
		}
	}
	
	public void use( BlobEmitter emitter ) {
		this.emitter = emitter;
	}
	
	protected void evolve() {

		if (this instanceof PermaGas) return;
		
		boolean[] blocking = Dungeon.level.solid;
		for (Barrier b : Dungeon.level.barriers.values()) {
			blocking[b.pos] |= b.blocksBlobs();
		}
		int cell;
		for (int i=area.top-1; i <= area.bottom; i++) {
			for (int j = area.left-1; j <= area.right; j++) {
				cell = j + i*Dungeon.level.width();
				if (Dungeon.level.insideMap(cell)) {
					if (!blocking[cell]) {

						int count = 1;
						int sum = cur[cell];

						if (j > area.left && !blocking[cell-1]) {
							sum += cur[cell-1];
							count++;
						}
						if (j < area.right && !blocking[cell+1]) {
							sum += cur[cell+1];
							count++;
						}
						if (i > area.top && !blocking[cell-Dungeon.level.width()]) {
							sum += cur[cell-Dungeon.level.width()];
							count++;
						}
						if (i < area.bottom && !blocking[cell+Dungeon.level.width()]) {
							sum += cur[cell+Dungeon.level.width()];
							count++;
						}

						int value = sum >= count ? (sum / count) - 1 : 0;
						off[cell] = value;

						if (value > 0){
							if (i < area.top)
								area.top = i;
							else if (i >= area.bottom)
								area.bottom = i+1;
							if (j < area.left)
								area.left = j;
							else if (j >= area.right)
								area.right = j+1;
						}

						volume += value;
					} else {
						off[cell] = 0;
					}
				}
			}
		}
	}

	public void seed( Level level, int cell, int amount ) {
		if (cur == null) cur = new int[level.length()];
		if (off == null) off = new int[cur.length];

		cur[cell] += amount;
		volume += amount;

		area.union(cell%level.width(), cell/level.width());
	}
	
	public void clear( int cell ) {
		if (volume == 0) return;
		volume -= cur[cell];
		cur[cell] = 0;
	}

	public void fullyClear(){
		volume = 0;
		area.setEmpty();
		cur = new int[Dungeon.level.length()];
		off = new int[Dungeon.level.length()];
	}

	public void onBuildFlagMaps( Level l ){
		//do nothing by default, only some blobs affect flags
	}

	//some blobs have an associated landmark entry, which is added when the hero sees them
	//blobs may also remove this landmark in some cases, such as when they expire or are consumed
	public Notes.Landmark landmark(){
		return null;
	}
	
	public String tileDesc() {
		return null;
	}
	
	public static<T extends Blob> T seed( int cell, int amount, Class<T> type ) {
		return seed(cell, amount, type, Dungeon.level);
	}
	
	@SuppressWarnings("unchecked")
	public static<T extends Blob> T seed( int cell, int amount, Class<T> type, Level level ) {

		T gas = (T)level.blobs.getOnly( type );
		
		if (gas == null) {
			gas = Reflection.newInstance(type);
			//this ensures that gasses do not get an 'extra turn' if they are added by a mob or buff
			if (Actor.curActorPriority() < gas.actPriority) {
				gas.spend(1f);
			}
		}
		
		if (Dungeon.level.barriers.get(cell) == null || !Dungeon.level.barriers.get(cell).blocksBlobs()){
			level.blobs.put( type, gas );
			gas.seed( level, cell, amount );
		}
		
		return gas;
	}

	public static CustomParticle addParticle(int cell, int particleID) {
		return addParticle(cell, particleID, Dungeon.level);
	}

	public static CustomParticle addParticle(int cell, int particleID, Level level) {

		CustomParticle particle = level.particles.get( particleID );

		if (particle == null) {
			particle = new CustomParticle(particleID);
		}

		level.particles.put( particleID, particle );
		particle.seed( level, cell, CustomParticle.CELL_ACTIVE );

		return particle;
	}

	public static int volumeAt( int cell, Class<? extends Blob> type ){
		Blob[] gasses = Dungeon.level.blobs.get( type );
		int volume = 0;
		for (Blob gas : gasses) {
			if (gas.volume != 0) volume += gas.cur[cell];
		}
		return volume;
	}

	public static int totalVolume(Class<? extends Blob> type){
		Blob[] gasses = Dungeon.level.blobs.get( type );
		int volume = 0;
		for (Blob gas : gasses) {
			if (gas.volume != 0) volume += gas.volume;
		}
		return volume;
	}
}