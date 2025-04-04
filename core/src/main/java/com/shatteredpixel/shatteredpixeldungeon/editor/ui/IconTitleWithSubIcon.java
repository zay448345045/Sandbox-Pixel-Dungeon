package com.shatteredpixel.shatteredpixeldungeon.editor.ui;

import com.shatteredpixel.shatteredpixeldungeon.editor.levels.CustomDungeon;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.EditorUtilities;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Image;

public class IconTitleWithSubIcon extends IconTitle {


    private Image subIcon;

    public IconTitleWithSubIcon() {
        super();
        subIcon = null;
    }

    public IconTitleWithSubIcon(Item item) {
        Image icon = CustomDungeon.getDungeon().getItemImage(item);
        icon(icon);
        subIcon = EditorUtilities.createSubIcon(item);
        if (subIcon != null) add(subIcon);
        label(Messages.titleCase(item.title()));
    }

    public IconTitleWithSubIcon(Heap heap) {
        ItemSprite icon = new ItemSprite();
        subIcon = null;
        icon(icon);
        label(Messages.titleCase(heap.title()));
        icon.view(heap);
    }

    public IconTitleWithSubIcon(Image icon, Image subIcon, String label) {
        super();

        this.subIcon = subIcon;
        icon(icon);
        label(label);
        if (subIcon != null) addToFront(this.subIcon);

    }

    @Override
    protected void layout() {
        super.layout();

        if (subIcon != null && imIcon != null) {
            subIcon.x = x + Math.max(imIcon.width(), 16) - (ItemSpriteSheet.Icons.SIZE + subIcon.width()) / 2f;
            subIcon.y = y - 0.5f + (ItemSpriteSheet.Icons.SIZE - subIcon.height()) / 2f;
            PixelScene.align(subIcon);
        }

    }

    @Override
    public void icon(Image icon) {
        super.icon(icon);
        if (subIcon != null) bringToFront(subIcon);
    }

}