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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import org.luaj.vm2.LuaError;
import com.watabou.noosa.Game;

import java.io.PrintWriter;
import java.io.StringWriter;

public class WndError extends WndTitledMessage {

	public WndError( String message ) {
		super( Icons.WARNING.get(), Messages.get(WndError.class, "title"), message );
	}

	public WndError( LuaError error ) {
		this( error.getMessage() );
		content.sp.scrollTo(0, text.bottom());
		setHighligtingEnabled(false);
	}

	public WndError ( Throwable throwable ) {
		super(Icons.WARNING.get(),  Messages.get(WndError.class, "title"), Messages.get(WndError.class, "error_msg", Game.version) + createStackTrace(throwable, false) );
		Game.reportException(throwable);
	}

	public static String createStackTrace(Throwable throwable, boolean limitLength ) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		pw.flush();
		String exceptionMsg = sw.toString();

		//shorten/simplify exception message to make it easier to fit into a message box
		exceptionMsg = exceptionMsg.replaceAll("\\(.*:([0-9]*)\\)", "($1)");
		exceptionMsg = exceptionMsg.replace("com.shatteredpixel.shatteredpixeldungeon.", "");
		exceptionMsg = exceptionMsg.replace("com.alphadraxonis.sandboxpixeldungeon.", "");
		exceptionMsg = exceptionMsg.replace("com.watabou.", "");
		exceptionMsg = exceptionMsg.replace("com.badlogic.gdx.", "");
		exceptionMsg = exceptionMsg.replace("\t", "  "); //shortens length of tabs

		//replace ' and " with similar equivalents as tinyfd hates them for some reason
		exceptionMsg = exceptionMsg.replace('\'', '’');
		exceptionMsg = exceptionMsg.replace('"', '”');

		if (limitLength && exceptionMsg.length() > 1000){
			exceptionMsg = exceptionMsg.substring(0, 1000) + "...";
		}

		return exceptionMsg;
	}

}