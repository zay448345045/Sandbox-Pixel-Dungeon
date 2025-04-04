/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;

public class WaterOfTransmutation extends WellWater {

    @Override
    protected Item affectItem(Item item, int pos) {

        item = ScrollOfTransmutation.changeItem(item);

        Sample.INSTANCE.play( Assets.Sounds.DRINK );

        //incase a never-seen item pops out
        if (item != null && item.isIdentified()) {
            Catalog.setSeen(item.getClass());
        }

        return item;

    }

    @Override
    protected boolean affectHero(Hero hero) {
        return false;
//        if (!hero.isAlive()) return false;
//
//        Sample.INSTANCE.play( Assets.Sounds.DRINK );
//
//        new ScrollOfMetamorphosis(){
//            {curItem = new Item();identifiedByUse = true;}
//        };
//        Game.runOnRenderThread(()-> GameScene.show(new ScrollOfMetamorphosis.WndMetamorphChoose()));
//
//        hero.sprite.emitter().start( Speck.factory( Speck.CHANGE ), 0.4f, 4 );
//
//        CellEmitter.get( hero.pos ).start( ShaftParticle.FACTORY, 0.2f, 3 );
//
//        Dungeon.hero.interrupt();
//
//        return true;
    }

    @Override
    public void use(BlobEmitter emitter) {
        super.use(emitter);
        emitter.pour( Speck.factory(Speck.CHANGE), 0.2f );
    }

    @Override
    public Notes.Landmark landmark() {
        return Notes.Landmark.WELL_OF_TRANSMUTATION;
    }

    @Override
    public String tileDesc() {
        return Messages.get(this, "desc");
    }
}