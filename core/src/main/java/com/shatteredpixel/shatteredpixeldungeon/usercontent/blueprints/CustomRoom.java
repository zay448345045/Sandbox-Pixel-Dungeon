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

package com.shatteredpixel.shatteredpixeldungeon.usercontent.blueprints;

import com.shatteredpixel.shatteredpixeldungeon.editor.editcomps.DefaultEditComp;
import com.shatteredpixel.shatteredpixeldungeon.editor.editcomps.EditRoomComp;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.GameObjectCategory;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.Rooms;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.RoomLayoutLevel;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.interfaces.CustomRoomClass;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.interfaces.LuaClassGenerator;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.ui.editcomps.CustomObjectEditor;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.ui.editcomps.CustomRoomEditor;
import com.watabou.utils.Reflection;

public class CustomRoom extends CustomGameObject<CustomRoomClass> {

	//TODO make sure that all calls also reach this level
	public RoomLayoutLevel predefinedRoomLayout;//can be null
	//paint must be overriden in a way that it only calls the real super if predefinedRoomLayout is null, otherwise the predefined one is painted on top

	//paint documentation: paints the room into the level. If a predefined layout is set, it will use this
	// or else it generates the room using the paint method from the superclass and uses other methods as parameter
	//you can always edit the level after calling super
	//TODO tzz also add the painter static mehtods to lua

	@Override
	public Class<? extends Bag> preferredBag() {
		return Rooms.bag().getClass();
	}

	@Override
	public String defaultSaveDir() {
		return "rooms/";
	}

	@Override
	public DefaultEditComp<?> createEditComp() {
		return new EditRoomComp((Room) userContentClass);
	}

	@Override
	public CustomObjectEditor<?> createCustomObjectEditor(Runnable onUpdateObj) {
		return new CustomRoomEditor(onUpdateObj, this);
	}

	@Override
	public GameObjectCategory<?> inventoryCategory() {
		return Rooms.instance();
	}

	@Override
	public boolean isSuperclassValid(Class<?> superClass) {
		return Room.class.isAssignableFrom(superClass);
	}

	@Override
	public void setTargetClass(String superClass) {
		Class<?> clazz = LuaClassGenerator.luaUserContentClass(Reflection.forName(superClass));
		setUserContentClass(!CustomRoomClass.class.isAssignableFrom(clazz) ? null : (CustomRoomClass) Reflection.newInstance(clazz));
	}

}