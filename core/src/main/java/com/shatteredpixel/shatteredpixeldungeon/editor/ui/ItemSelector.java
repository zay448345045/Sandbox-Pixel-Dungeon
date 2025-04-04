package com.shatteredpixel.shatteredpixeldungeon.editor.ui;

import com.shatteredpixel.shatteredpixeldungeon.editor.EditorScene;
import com.shatteredpixel.shatteredpixeldungeon.editor.editcomps.EditCompWindow;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.EditorInventoryWindow;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.EditorInventory;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.EditorItemBag;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.Items;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.items.EditorItem;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.items.ItemItem;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.other.RandomItem;
import com.shatteredpixel.shatteredpixeldungeon.editor.levels.CustomDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.scenes.DungeonScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventorySlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.ui.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ItemSelector extends Component {

    public enum NullTypeSelector {
        DISABLED, NOTHING, RANDOM
    }

    public static final int MIN_GAP = 6;//Gap between text and title
    public static final float GAP = 2f;

    private Class<? extends Item> itemClasses;
    private Item selectedItem;
    protected final RenderedTextBlock renderedTextBlock;
    protected InventorySlot itemSlot;
    protected IconButton changeBtn;
    private final NullTypeSelector nullTypeSelector;

    protected AnyItemSelectorWnd selector;

    private int showWhenNull = -1;

    public ItemSelector(String text, Class<? extends Item> itemClasses, Item startItem, NullTypeSelector nullTypeSelector) {
        this.itemClasses = itemClasses;
        this.nullTypeSelector = nullTypeSelector;

        selector = new AnyItemSelectorWnd(itemClasses, true) {
            @Override
            public void onSelect(Item item) {
                if (item == null) return;//if window is canceled
                if (item instanceof EditorItem.NullItemClass) setSelectedItem(null);
                else
                    setSelectedItem(item instanceof ItemItem ? ((ItemItem) item).item().getCopy() : item.getCopy());
            }

            @Override
            public boolean acceptsNull() {
                return nullTypeSelector != NullTypeSelector.DISABLED;
            }

            @Override
            public EditorItem.NullItemClass getItemForNull() {
                return nullTypeSelector == NullTypeSelector.NOTHING ? EditorItem.NULL_ITEM : EditorItem.RANDOM_ITEM;
            }
        };

        renderedTextBlock = PixelScene.renderTextBlock(text, 9);
        add(renderedTextBlock);

        itemSlot = new InventorySlot(startItem) {
            @Override
            protected void onClick() {
                super.onClick();
                onItemSlotClick();
            }

            @Override
            protected boolean onLongClick() {
                return onItemSlotLongClick();
            }

            @Override
            public void item(Item item) {
                super.item(item);
                bg.visible = true;//gold and bags should have bg
            }

            @Override
            protected void viewSprite(Item item) {
                if (!(item instanceof EditorItem)) {
                    super.viewSprite(item);
                    return;
                }
                if (sprite != null) {
                    remove(sprite);
                    sprite.destroy();
                }
                sprite = ((EditorItem<?>) item).getSprite();
                if (sprite != null) addToBack(sprite);
                sendToBack(bg);
            }
        };
        add(itemSlot);

        changeBtn = new IconButton(Icons.get(Icons.CHANGES)) {
            @Override
            protected void onClick() {
                change();
            }
        };
        add(changeBtn);

        setSelectedItem(startItem);
    }

    protected void onItemSlotClick() {
        EditorScene.show(new EditCompWindow(selectedItem) {
            @Override
            protected void onUpdate() {
                super.onUpdate();
                updateItem();
            }
        });
    }

    protected boolean onItemSlotLongClick() {
        change();
        return true;
    }

    @Override
    protected void layout() {
        float btnWidth = changeBtn.icon().width();
        float slotSize = ItemSpriteSheet.SIZE;

        renderedTextBlock.maxWidth((int) (width - MIN_GAP - slotSize - GAP - btnWidth));
        renderedTextBlock.setPos(x, y + (height - renderedTextBlock.height()) * 0.5f);

        itemSlot.setRect(Math.max(x + width - btnWidth - GAP - slotSize, renderedTextBlock.right() + MIN_GAP), y, slotSize, slotSize);
        changeBtn.setRect(Math.max(x + width - btnWidth, renderedTextBlock.right() + MIN_GAP + slotSize + GAP), y, btnWidth, changeBtn.icon().height());

        height = Math.max(slotSize, renderedTextBlock.height());
    }

    public void setSelectedItem(Item selectedItem) {
        this.selectedItem = selectedItem;
        if (showWhenNull != -1 && selectedItem == null) {
            selectNullItem();
        } else {
            if (selectedItem != null)
                selectedItem.image = CustomDungeon.getItemSpriteOnSheet(selectedItem);
            itemSlot.item(selectedItem);
        }
    }

    private void selectNullItem() {
        selectedItem = new Item();
        selectedItem.image = showWhenNull;
        itemSlot.item(selectedItem);
        itemSlot.active = false;
    }

    public int getShowWhenNull() {
        return showWhenNull;
    }

    public void setShowWhenNull(int showWhenNull) {
        this.showWhenNull = showWhenNull;
        if (getSelectedItem() == null) setSelectedItem(getSelectedItem());
    }

    public Item getSelectedItem() {
        return selectedItem;
    }

    public void updateItem() {
        if (selectedItem == null) selectNullItem();
        else itemSlot.item(selectedItem);
    }

    public void change() {
        showSelectWindow(selector, nullTypeSelector, itemClasses, Items.bag(), new HashSet<>(0));
    }

    private static void addItem(ScrollingListPane sp, Item i, EditorInventoryWindow w,
                                Class<?> itemClasses, Collection<Class<?>> excludeItems, WndBag.ItemSelectorInterface selector) {
        if (i instanceof EditorItem
                && itemClasses.isAssignableFrom(((EditorItem) i).getObject().getClass())
                && !excludeItems.contains(((EditorItem) i).getObject().getClass())
                && selector.itemSelectable(i)) {
            sp.addItem(((EditorItem) i).createListItem(w));
        }
    }

    public static EditorInventoryWindow showSelectWindow(WndBag.ItemSelectorInterface selector, NullTypeSelector nullTypeSelector, Class<?> itemClasses, EditorItemBag bag, Collection<Class<?>> excludeItems) {
        return showSelectWindow(selector, nullTypeSelector, itemClasses, bag, excludeItems, true);
    }

    /**
     * <b>includeRandomItem only works if itemClasses is a SUBCLASS of item</b>
     */
    public static EditorInventoryWindow showSelectWindow(WndBag.ItemSelectorInterface selector, NullTypeSelector nullTypeSelector, Class<?> itemClasses,
                                                         EditorItemBag bag, Collection<Class<?>> excludeItems, boolean includeRandomItem) {
        final int WIDTH = Math.min(160, Window.WindowSize.WIDTH_LARGE.get());
        final int HEIGHT = Window.WindowSize.HEIGHT_SMALL.get();

        Win w = new Win(selector);
        w.resize(WIDTH, HEIGHT);
        ScrollingListPane sp = new ScrollingListPane();
        w.add(sp);
        sp.setSize(WIDTH, HEIGHT);

        if (nullTypeSelector == NullTypeSelector.NOTHING)
            sp.addItem(EditorItem.NULL_ITEM.createListItem(w));
        else if (nullTypeSelector == NullTypeSelector.RANDOM)
            sp.addItem(EditorItem.RANDOM_ITEM.createListItem(w));

        if (includeRandomItem && itemClasses != Item.class && Item.class.isAssignableFrom(itemClasses)) {
            RandomItem<?> randomItem = RandomItem.getNewRandomItem((Class<? extends Item>) itemClasses);
            sp.addItem(new ItemItem((Item) randomItem).createListItem(w));//bypass any restrictions
        }

        for (Item bagitem : bag.items) {
            if (bagitem instanceof Bag) {
                for (Item i : (Bag) bagitem) {
                    addItem(sp, i, w, itemClasses, excludeItems, selector);
                }
            } else addItem(sp, bagitem, w, itemClasses, excludeItems, selector);
        }
        Component[] comps = sp.getItems();
        if (comps.length == 0) {
            w.destroy();
            return null;
        }
        if (comps[comps.length - 1].bottom() < HEIGHT) {
            w.resize(WIDTH, (int) comps[comps.length - 1].bottom());
            sp.setSize(WIDTH, (int) comps[comps.length - 1].bottom());
        }

        DungeonScene.show(w);

        return w;
    }


    private static class Win extends Window implements EditorInventoryWindow {

        private final WndBag.ItemSelectorInterface selector;

        public Win(WndBag.ItemSelectorInterface selector) {
            this.selector = selector;
        }

        @Override
        public WndBag.ItemSelectorInterface selector() {
            return selector;
        }
    }

    public static abstract class AnyItemSelectorWnd extends WndBag.ItemSelector {
        protected final Class<? extends Item> itemClasses;
        public Class<? extends Bag> preferredBag;
        protected final boolean allowRandomItem;

        public AnyItemSelectorWnd(Class<? extends Item> itemClasses, boolean allowRandomItem) {
            this.itemClasses = itemClasses;
            this.allowRandomItem = allowRandomItem;
            preferredBag = Items.bag().getClass();
        }

        @Override
        public String textPrompt() {
            return null;
        }

        @Override
        public boolean itemSelectable(Item item) {
            Object obj = item;
            if (obj instanceof ItemItem) obj = ((ItemItem) obj).getObject();
            else if (obj instanceof EditorItem) {
                Object o = ((EditorItem<?>) obj).getObject();
                return itemClasses.isAssignableFrom(obj.getClass())
                        && (allowRandomItem || !(o instanceof RandomItem));
            }
            return itemClasses.isAssignableFrom(obj.getClass())
                    && (allowRandomItem || !(obj instanceof RandomItem));
        }

        @Override
        public Class<? extends Bag> preferredBag() {
            return preferredBag;
        }

        @Override
        public List<Bag> getBags() {
            return Collections.singletonList(EditorInventory.getBag(preferredBag));
        }
    }
}