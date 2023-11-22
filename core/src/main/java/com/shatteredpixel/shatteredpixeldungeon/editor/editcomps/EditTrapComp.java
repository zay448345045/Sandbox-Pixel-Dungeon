package com.shatteredpixel.shatteredpixeldungeon.editor.editcomps;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.editor.EditorScene;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.items.TrapItem;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.EditorUtilies;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GatewayTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTabbed;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

public class EditTrapComp extends DefaultEditComp<Trap> {


    protected CheckBox visible, active;
    protected CheckBox searchable, revealedWhenTriggered, disarmedByActivation;
    protected RedButton gatewayTelePos;
    private Window windowInstance;

    private final TrapItem trapItem;//used for linking the item with the sprite in the toolbar

    private Component[] comps;

    public EditTrapComp(Trap item) {
        super(item);
        initComps();
        trapItem = null;
    }

    public EditTrapComp(TrapItem trapItem) {
        super(trapItem.trap());
        initComps();
        this.trapItem = trapItem;
    }

    private void initComps() {
        visible = new CheckBox(Messages.get(EditTrapComp.class, "visible")) {
            @Override
            public void checked(boolean value) {
                super.checked(value);
                obj.visible = value;
                updateObj();
            }
        };
        add(visible);
        active = new CheckBox(Messages.get(EditTrapComp.class, "active")) {
            @Override
            public void checked(boolean value) {
                super.checked(value);
                obj.active = value;
                EditTrapComp.this.visible.enable(value);
                updateObj();
            }
        };
        add(active);

        visible.checked(obj.visible);
        active.checked(obj.active);

        searchable = new CheckBox(Messages.get(EditTrapComp.class, "searchable")) {
            @Override
            public void checked(boolean value) {
                super.checked(value);
                obj.canBeSearched = value;
                updateObj();
            }
        };
        add(searchable);
        revealedWhenTriggered = new CheckBox(Messages.get(EditTrapComp.class, "revealed_when_triggered")) {
            @Override
            public void checked(boolean value) {
                super.checked(value);
                obj.revealedWhenTriggered = value;
                updateObj();
            }
        };
        add(revealedWhenTriggered);
        disarmedByActivation = new CheckBox(Messages.get(EditTrapComp.class, "disarmed_by_activation")) {
            @Override
            public void checked(boolean value) {
                super.checked(value);
                obj.disarmedByActivation = value;
                updateObj();
            }
        };
        add(disarmedByActivation);

        searchable.checked(obj.canBeSearched);
        revealedWhenTriggered.checked(obj.revealedWhenTriggered);
        disarmedByActivation.checked(obj.disarmedByActivation);

        if (obj instanceof GatewayTrap && obj.pos != -1) {
            int telePos = ((GatewayTrap) obj).telePos;
            gatewayTelePos = new RedButton("") {
                @Override
                protected void onClick() {
                    EditorScene.selectCell(gatewayTelePosListener);
                    windowInstance = EditorUtilies.getParentWindow(gatewayTelePos);
                    windowInstance.active = false;
                    if (windowInstance instanceof WndTabbed)
                        ((WndTabbed) windowInstance).setBlockLevelForTabs(PointerArea.NEVER_BLOCK);
                    Game.scene().remove(windowInstance);
                }
            };
            if (telePos == -1) gatewayTelePos.text(Messages.get(EditTrapComp.class, "gateway_trap_random"));
            else gatewayTelePos.text(Messages.get(EditTrapComp.class, "gateway_trap_pos", EditorUtilies.cellToString(telePos)));
            add(gatewayTelePos);
        } else gatewayTelePos = null;

        comps = new Component[]{visible, active, searchable, revealedWhenTriggered, disarmedByActivation, gatewayTelePos};
    }

    @Override
    protected void layout() {
        super.layout();
        layoutCompsLinear(comps);
    }

    @Override
    protected Component createTitle() {
        return new IconTitle(getIcon(), TrapItem.createTitle(obj));
    }

    @Override
    protected String createDescription() {
        return obj.desc();
    }

    @Override
    public Image getIcon() {
        return TrapItem.getTrapImage(obj);
    }

    @Override
    protected void updateObj() {
        if (!obj.active && !obj.visible) {
            visible.checked(true);
            return;
        }
        if (title instanceof IconTitle) {
            ((IconTitle) title).label(TrapItem.createTitle(obj));
            ((IconTitle) title).icon(TrapItem.getTrapImage(obj));
        }
        desc.text(createDescription());

        if (trapItem != null) {
            ItemSlot slot = QuickSlotButton.containsItem(trapItem);
            if (slot != null) slot.item(trapItem);
        }

        if (obj.pos != -1) EditorScene.updateMap(obj.pos);

        super.updateObj();
    }


    public static boolean areEqual(Trap a, Trap b) {
        if (a == null || b == null) return false;
        if (a.getClass() != b.getClass()) return false;
        if (a.visible != b.visible) return false;
        if (a.active != b.active) return false;
        if (a.canBeSearched != b.canBeSearched) return false;
        if (a.revealedWhenTriggered != b.revealedWhenTriggered) return false;
        if (a.disarmedByActivation != b.disarmedByActivation) return false;
        if (a instanceof GatewayTrap && ((GatewayTrap) a).telePos != ((GatewayTrap) b).telePos)
            return false;
        return true;
    }


    private final CellSelector.Listener gatewayTelePosListener = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell != null) {
                boolean validDest = Dungeon.level.passable[cell] && !Dungeon.level.secret[cell] && EditorScene.customLevel().findMob(cell) == null;
                GatewayTrap trap = (GatewayTrap) obj;
                if (!validDest) trap.telePos = -1;
                else trap.telePos = cell;
                if (trap.telePos == -1)
                    gatewayTelePos.text(Messages.get(EditTrapComp.class, "gateway_trap_random"));
                else
                    gatewayTelePos.text(Messages.get(EditTrapComp.class, "gateway_trap_pos", EditorUtilies.cellToString(trap.telePos)));
                windowInstance.active = true;
                if (windowInstance instanceof WndTabbed)
                    ((WndTabbed) windowInstance).setBlockLevelForTabs(PointerArea.ALWAYS_BLOCK);
                EditorScene.show(windowInstance);
            }
        }

        @Override
        public String prompt() {
            return Messages.get(EditTrapComp.class, "gateway_trap_prompt");
        }
    };
}