/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Sandbox Pixel Dungeon
 * Copyright (C) 2023-2024 AlphaDraxonis
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

package com.shatteredpixel.shatteredpixeldungeon.customobjects.interfaces;

import com.shatteredpixel.shatteredpixeldungeon.GameObject;
import com.shatteredpixel.shatteredpixeldungeon.editor.scene.undo.ActionPartModify;
import com.shatteredpixel.shatteredpixeldungeon.editor.scene.undo.parts.TrapActionPart;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;

public interface CustomTrapClass extends CustomGameObjectClass {
	
	static ActionPartModify doUpdateInheritStats(CustomGameObjectClass self, GameObject obj, CustomGameObjectClass customClass) {
		Trap m = (Trap) obj;
		Trap template = (Trap) self;
		ActionPartModify modify = new TrapActionPart.Modify(m);
		if (customClass.getInheritStats()) {
			obj.copyStats(template);
		}
//		if (m.sprite != null) {
//			EditorScene.replaceTrapSprite(m, m.spriteClass);
//		}
//		EditTrapComp.updateTrapTexture(m);

		return modify;
	}

//	default boolean usesCustomSprite() {
//		CustomTrap customTrap = (CustomTrap) UserContentManager.getUserContent(getIdentifier());
//		if (customTrap.sprite != null) {
//			return customTrap.sprite.getActualCustomCharSpriteOrNull() != null;
//		}
//		return false;
//	}

}
