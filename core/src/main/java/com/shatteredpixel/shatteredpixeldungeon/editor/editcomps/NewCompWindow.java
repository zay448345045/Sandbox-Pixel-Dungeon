package com.shatteredpixel.shatteredpixeldungeon.editor.editcomps;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.editor.overview.dungeon.WndNewDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextInput;
import com.watabou.noosa.ui.Component;

import static com.shatteredpixel.shatteredpixeldungeon.editor.overview.floor.WndNewFloor.BUTTON_HEIGHT;

public abstract class NewCompWindow<T> extends Window {

    private static final int MARGIN = 2;

    protected Component spContent;
    private final ScrollPane sp;
    protected IconTitle title;

    protected RedButton create, cancel;

    protected TextInput textBox;

    private DefaultEditComp<?> editObjComp;
    protected T obj;

    public NewCompWindow(T obj) {
        this(obj, null);
    }

    public NewCompWindow(T obj, String titleText) {

        this.obj = obj;

        resize(WindowSize.WIDTH_LARGE_S.get(), 100);

        title = new IconTitle(getIcon(), titleText != null ? titleText : Messages.titleCase( Messages.get(this, "title") ));
        add(title);

        textBox = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, PixelScene.uiCamera.zoom) {
            @Override
            public void enterPressed() {
                if (!getText().isEmpty()) {
                    create(textBox.getText());
                }
            }
        };
        textBox.setMaxLength(50);
        add(textBox);
        Game.platform.setOnscreenKeyboardVisible(false);

        create = new RedButton(Messages.get(WndNewDungeon.class, "yes")) {
            @Override
            protected void onClick() {
                if (!textBox.getText().isEmpty()) {
                    create(textBox.getText());
                }
            }
        };
        add(create);
        cancel = new RedButton(Messages.get(WndNewDungeon.class, "no")) {
            @Override
            protected void onClick() {
                create(null);
            }
        };
        add(cancel);

        spContent = new Component() {
            @Override
            protected void layout() {
                editObjComp.setRect(0, 0, width, -1);
                height = editObjComp.height() + 1;
            }
        };

        editObjComp = createEditComp();

        //no title or description
        editObjComp.rename.setVisible(false);
        editObjComp.title.setVisible(false);
        editObjComp.desc.setVisible(false);

        spContent.add(editObjComp);

        sp = new ScrollPane(spContent);
        add(sp);

        float posY = MARGIN;
        title.setRect(MARGIN, posY, width, title.height());
        posY = title.bottom() + MARGIN;

        final float textBoxPos = posY;
        posY = textBoxPos + 16 + MARGIN;

        spContent.setSize(width, -1);
        final float spPos = posY;
        final float spHeight = Math.min(WindowSize.HEIGHT_SMALL.get() - posY - BUTTON_HEIGHT - 1, spContent.height());
        posY += spHeight + MARGIN * 2;

        create.setRect(MARGIN, posY, (width - MARGIN * 3) / 2, BUTTON_HEIGHT + 1);
        cancel.setRect(create.right() + MARGIN * 2, posY, (width - MARGIN * 3) / 2, BUTTON_HEIGHT + 1);
        posY = create.bottom() + MARGIN;

        resize(width, (int) Math.ceil(posY));

        textBox.setRect(MARGIN, textBoxPos, width - MARGIN * 2, 16);
        sp.setRect(0, spPos, width, spHeight);
    }

    @Override
    public void onBackPressed() {
    }

    protected abstract Image getIcon();

    protected void create(String name) {
        hide();
    }

    protected abstract DefaultEditComp<T> createEditComp();
}