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

package com.shatteredpixel.shatteredpixeldungeon.customobjects.blueprints;

import com.shatteredpixel.shatteredpixeldungeon.customobjects.interfaces.CustomTrapClass;
import com.shatteredpixel.shatteredpixeldungeon.customobjects.interfaces.LuaClassGenerator;
import com.shatteredpixel.shatteredpixeldungeon.customobjects.ui.editcomps.CustomObjectEditor;
import com.shatteredpixel.shatteredpixeldungeon.customobjects.ui.editcomps.CustomTrapEditor;
import com.shatteredpixel.shatteredpixeldungeon.editor.editcomps.DefaultEditComp;
import com.shatteredpixel.shatteredpixeldungeon.editor.editcomps.EditTrapComp;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.GameObjectCategory;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.Traps;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Reflection;

public class CustomTrap extends CustomGameObject<CustomTrapClass> {

	@Override
	public Class<? extends Bag> preferredBag() {
		return Traps.bag().getClass();
	}

	@Override
	public String defaultSaveDir() {
		return "traps/";
	}

	@Override
	public DefaultEditComp<?> createEditComp() {
		return new EditTrapComp((Trap) userContentClass);
	}

	@Override
	public CustomObjectEditor<?> createCustomObjectEditor(Runnable onUpdateObj) {
		return new CustomTrapEditor(onUpdateObj, this);
	}

	@Override
	public GameObjectCategory<?> inventoryCategory() {
		return Traps.instance();
	}

	@Override
	public boolean isSuperclassValid(Class<?> superClass) {
		return Trap.class.isAssignableFrom(superClass);
	}

	@Override
	public void setTargetClass(String superClass) {
		Class<?> clazz = LuaClassGenerator.luaUserContentClass(Reflection.forName(superClass));
		setUserContentClass(!CustomTrapClass.class.isAssignableFrom(clazz) ? null : (CustomTrapClass) Reflection.newInstance(clazz));
	}

}