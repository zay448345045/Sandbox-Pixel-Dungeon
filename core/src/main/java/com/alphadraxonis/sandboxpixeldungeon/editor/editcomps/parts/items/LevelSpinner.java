package com.alphadraxonis.sandboxpixeldungeon.editor.editcomps.parts.items;

import com.alphadraxonis.sandboxpixeldungeon.editor.ui.spinner.Spinner;
import com.alphadraxonis.sandboxpixeldungeon.editor.ui.spinner.SpinnerIntegerModel;
import com.alphadraxonis.sandboxpixeldungeon.items.Item;
import com.alphadraxonis.sandboxpixeldungeon.items.artifacts.Artifact;
import com.alphadraxonis.sandboxpixeldungeon.messages.Messages;

public class LevelSpinner extends Spinner {


    public LevelSpinner(Item item) {
        super(new LevelSpinnerModel(item.level(), item instanceof Artifact ? ((Artifact) item).levelCap() : 100),
                " "+ Messages.get(LevelSpinner.class,"label") + ":", 10);
        addChangeListener(() -> {
            item.level((int) getValue());
            onChange();
        });
    }

    protected void onChange() {
    }

    public static class LevelSpinnerModel extends SpinnerIntegerModel {

        public LevelSpinnerModel(int level) {
            this(level, 100);
        }
        public LevelSpinnerModel(int level, int max) {
            super(-10, max, level, 1, false, null);
        }

        @Override
        public float getInputFieldWith(float height) {
            return height * 1.3f;
        }

        @Override
        public int getClicksPerSecondWhileHolding() {
            return 12;
        }

        @Override
        public void displayInputAnyNumberDialog() {
            if (getMaximum() == 100) displayInputAnyNumberDialog(-100, Integer.MAX_VALUE);
            else displayInputAnyNumberDialog(getMinimum(), getMaximum());
        }
    }
}