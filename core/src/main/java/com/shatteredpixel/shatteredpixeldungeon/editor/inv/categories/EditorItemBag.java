package com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories;

import com.shatteredpixel.shatteredpixeldungeon.editor.inv.FindInBag;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.items.EditorItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.interfaces.CustomObjectClass;
import com.watabou.noosa.Image;

public class EditorItemBag extends Bag {

    private final String name;

    public EditorItemBag() {
        this("name", 0);
    }

    public EditorItemBag(int img) {
        this("name", img);
    }

    public EditorItemBag(String name, int img) {
        this.name = name;
        image = img;
    }

    @Override
    public int capacity() {
        return items.size() + 1;
    }

    public Image getCategoryImage() {
        return new ItemSprite(image);
    }

    @Override
    public String name() {
        return Messages.get(this, name);
    }

    public Item findItem(FindInBag src){
        return findItem(this, src);
    }

    private static Item findItem(Bag bag, FindInBag src){

        if (src.getType() == FindInBag.Type.CLASS) {
            for (Item item : bag.items) {
                if (item instanceof Bag) {
                    Item result = findItem((Bag) item, src);
                    if (result != null) return result;
                }
                Object realItem;
                if (item instanceof EditorItem) realItem = ((EditorItem<?>) item).getObject();
                else realItem = item;
                if ((realItem.getClass() == Class.class ? realItem : realItem.getClass()) == src.getValue()) return item;
            }
        }
        if (src.getType() == FindInBag.Type.USER_CONTENT) {
            for (Item item : bag.items) {
                if (item instanceof Bag) {
                    Item result = findItem((Bag) item, src);
                    if (result != null) return result;
                }
                Object realItem;
                if (item instanceof EditorItem) realItem = ((EditorItem<?>) item).getObject();
                else realItem = item;
                if (realItem.getClass() == src.getValue()) return item;
                if (realItem instanceof CustomObjectClass && ((CustomObjectClass) realItem).getIdentifier() == (int) src.getValue()) return item;
            }
        }

        return null;
    }

}