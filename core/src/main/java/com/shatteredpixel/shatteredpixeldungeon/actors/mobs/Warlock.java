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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.ItemsWithChanceDistrComp;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WarlockSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Warlock extends Mob implements Callback {
	
	private static final float TIME_TO_ZAP	= 1f;
	
	{
		spriteClass = WarlockSprite.class;
		
		HP = HT = 70;
		defenseSkill = 18;
		attackSkill = 25;
		damageRollMin = 12;
		damageRollMax = 18;
		specialDamageRollMin = 12;
		specialDamageRollMax = 18;
		damageReductionMax = 8;

		EXP = 11;
		maxLvl = 21;
		
		loot = Generator.Category.POTION;
		lootChance = 0.5f;

		properties.add(Property.UNDEAD);
	}
	
//	@Override
//	public int damageRoll() {
//		return Random.NormalIntRange( 12, 18 );
//	}
//
//	@Override
//	public int attackSkill( Char target ) {
//		return 25;
//	}
//
//	@Override
//	public int drRoll() {
//		return super.drRoll() + Random.NormalIntRange(0, 8);
//	}
	
	@Override
	protected boolean canAttack( Char enemy ) {
		return super.canAttack(enemy)
				|| new Ballistica( pos, enemy.pos, Ballistica.REAL_MAGIC_BOLT, null).collisionPos == enemy.pos;
	}
	
	protected boolean doAttack( Char enemy ) {

		if (Dungeon.level.adjacent( pos, enemy.pos )
				|| new Ballistica( pos, enemy.pos, Ballistica.REAL_MAGIC_BOLT, null).collisionPos != enemy.pos) {
			
			return super.doAttack( enemy );
			
		} else {
			
			return doRangedAttack();
		}
	}

	@Override
	public void playZapAnim(int target) {
		WarlockSprite.playZap(sprite.parent, sprite, target, this);
	}

	//used so resistances can differentiate between melee and magical attacks
	public static class DarkBolt{}

	@Override
	public void zap() {
		spend( TIME_TO_ZAP );

		Invisibility.dispel(this);
		Char enemy = this.enemy;
		if (hit( this, enemy, true )) {
			//TODO would be nice for this to work on ghost/statues too
			if (enemy == Dungeon.hero && Random.Int( 2 ) == 0) {
				Buff.prolong( enemy, Degrade.class, Degrade.DURATION );
				Sample.INSTANCE.play( Assets.Sounds.DEGRADE );
			}
			
			int dmg = Random.NormalIntRange( specialDamageRollMin, specialDamageRollMax );
			dmg = Math.round(dmg * AscensionChallenge.statModifier(this));

			//logic for DK taking 1/2 damage from aggression stoned minions
			if ( enemy.buff(StoneOfAggression.Aggression.class) != null
					&& enemy.alignment == alignment
					&& (Char.hasProp(enemy, Property.BOSS) || Char.hasProp(enemy, Property.MINIBOSS))){
				dmg *= 0.5f;
			}

			enemy.damage( dmg, new DarkBolt() );
			
			if (enemy == Dungeon.hero && !enemy.isAlive()) {
				Badges.validateDeathFromEnemyMagic();
				Dungeon.fail( this );
				GLog.n( Messages.get(this, "bolt_kill") );
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
		}
	}

	@Override
	public void call() {
		next();
	}

	@Override
	public Item createLoot(){

		// 1/6 chance for healing, scaling to 0 over 8 drops
		if (Random.Int(3) == 0 && Random.Int(8) > Dungeon.LimitedDrops.WARLOCK_HP.count ){
			Dungeon.LimitedDrops.WARLOCK_HP.count++;
			return new PotionOfHealing();
		} else {
			Item i;
			do {
				i = Generator.randomUsingDefaults(Generator.Category.POTION);
			} while (i instanceof PotionOfHealing);
			return i;
		}

	}

	@Override
	public ItemsWithChanceDistrComp.RandomItemData convertLootToRandomItemData() {
		return new ItemsWithChanceDistrComp.RandomItemData();
	}

	@Override
	public void increaseLimitedDropCount(Item generatedLoot) {
		if (generatedLoot instanceof PotionOfHealing)
			Dungeon.LimitedDrops.WARLOCK_HP.count++;
		super.increaseLimitedDropCount(generatedLoot);
	}
}