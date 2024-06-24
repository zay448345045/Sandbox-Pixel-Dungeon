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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.ItemsWithChanceDistrComp;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DM100Sprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class DM100 extends DMMob {

	private static final float TIME_TO_ZAP	= 1f;
	
	{
		spriteClass = DM100Sprite.class;
		
		HP = HT = 20;
		defenseSkill = 8;
		damageRollMin = 2;
		damageRollMax = 8;
		specialDamageRollMin = 3;
		specialDamageRollMax = 10;
		attackSkill = 11;
		damageReductionMax = 4;

		EXP = 6;
		maxLvl = 13;
		
		loot = Generator.Category.SCROLL;
		lootChance = 0.25f;
		
		properties.add(Property.ELECTRIC);
		properties.add(Property.INORGANIC);
	}

	@Override
	public ItemsWithChanceDistrComp.RandomItemData convertLootToRandomItemData() {
		ItemsWithChanceDistrComp.RandomItemData customLootInfo = super.convertLootToRandomItemData();
		Generator.convertGeneratorToCustomLootInfo(customLootInfo, (Generator.Category) loot, 1);
		customLootInfo.setLootChance(customLootInfo.calculateSum() * 3);
		return customLootInfo;
	}

//	@Override
//	public int damageRoll() {
//		return Char.combatRoll( 2, 8 );
//	}
//
//	@Override
//	public int attackSkill( Char target ) {
//		return 11;
//	}
//
//	@Override
//	public int drRoll() {
//		return super.drRoll() + Char.combatRoll(0, 4);
//	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return super.canAttack(enemy)
				|| new Ballistica( pos, enemy.pos, Ballistica.REAL_MAGIC_BOLT, null).collisionPos == enemy.pos;
	}

	@Override
	public void playZapAnim(int target) {
		DM100Sprite.playZap(sprite.parent, sprite, target, this);
	}
	
	//used so resistances can differentiate between melee and magical attacks
	public static class LightningBolt{}
	
	@Override
	protected boolean doAttack( Char enemy ) {

		if (Dungeon.level.adjacent( pos, enemy.pos )
				|| new Ballistica( pos, enemy.pos, Ballistica.REAL_MAGIC_BOLT, null).collisionPos != enemy.pos) {
			
			return super.doAttack( enemy );
			
		} else {
			
			if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
				if (sprite instanceof DM100Sprite) {
					zap();
				}
				sprite.zap( enemy.pos );
				return false;
			} else {
				zap();
				return true;
			}
		}
	}

	@Override
	protected void zap() {
		spend( TIME_TO_ZAP );

		Invisibility.dispel(this);
		if (hit( this, enemy, true )) {
			int dmg = Char.combatRoll(specialDamageRollMin, specialDamageRollMax);
			dmg = Math.round(dmg * AscensionChallenge.statModifier(this));
			enemy.damage( dmg, new LightningBolt() );

			if (enemy.sprite.visible) {
				enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				enemy.sprite.flash();
			}

			if (enemy == Dungeon.hero) {

				if (sprite instanceof DM100Sprite)
					PixelScene.shake( 2, 0.3f );

				if (!enemy.isAlive()) {
					Badges.validateDeathFromEnemyMagic();
					Dungeon.fail( this );
					GLog.n( Messages.get(this, "zap_kill") );
				}
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
		}
	}

	@Override
	public void onZapComplete() {
		if (!(sprite instanceof DM100Sprite)) {
			super.onZapComplete();
			return;
		}
		next();
	}

}