package com.shatteredpixel.shatteredpixeldungeon.editor.editcomps;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BuffWithDuration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DelayedRockFall;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.customobjects.CustomObjectManager;
import com.shatteredpixel.shatteredpixeldungeon.customobjects.interfaces.CustomGameObjectClass;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.StyledCheckBox;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.spinner.Spinner;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.spinner.SpinnerIntegerModel;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.spinner.StyledSpinner;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.EditorUtilities;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoBuff;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.LinkedHashSet;

public class EditBuffComp extends DefaultEditComp<Buff> {


    protected StyledCheckBox permanent;

    protected RedButton removeBuff;
    protected StyledSpinner duration, level;
    protected StyledCheckBox showFx;

    private final Component[] rectComps, linearComps;

    public EditBuffComp(Buff buff, DefaultEditComp<?> editComp) {
        super(buff);

        if (!buff.zoneBuff){

            if (!(buff instanceof ChampionEnemy)) {
                permanent = new StyledCheckBox(Messages.get(this, "permanent"));
                permanent.checked(buff.permanent);
                permanent.addChangeListener(v -> {
                    buff.permanent = v;
                    updateObj();
                });
                add(permanent);
            }

            if (buff instanceof Burning || buff instanceof Corrosion || buff instanceof ToxicImbue) {
                duration = new StyledSpinner(new SpinnerIntegerModel(0, 1000, (int) ((BuffWithDuration) buff).left) {
                    @Override
                    public float getInputFieldWidth(float height) {
                        return Spinner.FILL;
                    }

                    @Override
                    public void afterClick() {
                        updateObj();
                    }
                }, Messages.get(this, "duration"));
                duration.addChangeListener(() -> {
                    if (((int) (((BuffWithDuration) buff).left = (int) duration.getValue())) % 20 == 0)
                        updateObj();//pretty expensive call for longer texts, so it is better to call this less
                });
                add(duration);
            } else if (buff instanceof FlavourBuff) {
                duration = new StyledSpinner(new SpinnerIntegerModel(1, 1000, (int) buff.visualcooldown()) {
                    @Override
                    public float getInputFieldWidth(float height) {
                        return Spinner.FILL;
                    }

                    @Override
                    public void afterClick() {
                        updateObj();
                    }
                }, Messages.get(this, "duration"));
                duration.addChangeListener(() -> {
                    int val = (int) duration.getValue();
                    buff.setDurationForFlavourBuff(val);
                    if (val % 20 == 0)
                        updateObj();//pretty expensive call for longer texts, so it is better to call this less
                });
                add(duration);
            }

        }

        if (buff instanceof BuffWithDuration
                && !(buff instanceof Burning || buff instanceof Corrosion || buff instanceof ToxicImbue)) {
            level = new StyledSpinner(new SpinnerIntegerModel(0, 1000, (int) ((BuffWithDuration) buff).left) {
                @Override
                public float getInputFieldWidth(float height) {
                    return Spinner.FILL;
                }

                @Override
                public void afterClick() {
                    updateObj();
                }
            }, Messages.get(this, "level"));
            level.addChangeListener(() -> {
                if (((int) (((BuffWithDuration) buff).left = (int) level.getValue())) % 20 == 0)
                    updateObj();//pretty expensive call for longer texts, so it is better to call this less
            });
            add(level);
        } else if (buff instanceof Corrosion) {
            level = new StyledSpinner(new SpinnerIntegerModel(0, 100, (int) ((Corrosion) buff).damage) {
                @Override
                public float getInputFieldWidth(float height) {
                    return Spinner.FILL;
                }

                @Override
                public void afterClick() {
                    updateObj();
                }
            }, Messages.get(this, "level"));
            level.addChangeListener(() -> {
                if (((int) (((Corrosion) buff).damage = (int) level.getValue())) % 20 == 0)
                    updateObj();//pretty expensive call for longer texts, so it is better to call this less
            });
            add(level);
        }

        if (buff instanceof Invulnerability || buff instanceof Barrier || buff instanceof Blocking.BlockBuff || buff instanceof Tengu.BombAbility
                || buff instanceof Burning || buff instanceof ScrollOfChallenge.ChallengeArena || buff instanceof ChampionEnemy || buff instanceof Chill
                || buff instanceof Corruption || buff instanceof DelayedRockFall || buff instanceof Doom || buff instanceof ScrollOfSirensSong.Enthralled
                || buff instanceof Ghoul.GhoulLifeLink || buff instanceof Healing || buff instanceof SummonElemental.InvisAlly || buff instanceof Levitation
                || buff instanceof SoulMark || buff instanceof DwarfKing.Summoning) {
            showFx = new StyledCheckBox(Messages.get(this, "show_fx"));
            showFx.checked(!buff.alwaysHidesFx);
            showFx.addChangeListener(v -> {
                buff.alwaysHidesFx = !v;
                if (buff.target != null) buff.fx(true);
            });
            add(showFx);
        }

        if (buff.target != null) {
            removeBuff = new RedButton(Messages.get(this, "remove")) {
                @Override
                protected void onClick() {
                    buff.detach();
                    if (editComp != null) {
                        if (editComp instanceof EditMobComp) ((EditMobComp) editComp).buffs.removeBuffFromUI(buff.getClass());
                        editComp.updateObj();
                    }
                    EditorUtilities.getParentWindow(this).hide();
                }
            };
            add(removeBuff);
        }


//        SpinnerIntegerModel spinnerModel = new SpinnerIntegerModel(1, 500, (int) buff.visualcooldown(), 1, true, INFINITY) {
//            @Override
//            public float getInputFieldWidth(float height) {
//                return height * 1.4f;
//            }
//        };

//        if (false) {//only apply permanent buffs for now
//            changeDuration = new Spinner(spinnerModel, " " + Messages.get(EditBuffComp.class, "duration"), 10) {
//                @Override
//                protected void afterClick() {
//                    onSpinnerValueChange(true);
//                }
//            };
//            add(changeDuration);
//
//            changeDuration.addChangeListener(() -> onSpinnerValueChange(false));
//        } else changeDuration = null;


        rectComps = new Component[]{permanent, duration, level, showFx};
        linearComps = new Component[]{removeBuff};

        initializeCompsForCustomObjectClass();
    }

    @Override
    protected void updateStates() {
        super.updateStates();

        if (permanent != null) permanent.checked(obj.permanent);
        if (duration != null) {
            if (obj instanceof FlavourBuff) duration.setValue(obj.visualcooldown());
            else duration.setValue(((BuffWithDuration) obj).left);
        }
        if (level != null) {
            if (obj instanceof Corrosion) duration.setValue(((Corrosion) obj).damage);
            else duration.setValue(((BuffWithDuration) obj).left);
        }
        if (showFx != null) showFx.checked(!obj.alwaysHidesFx);
    }

    @Override
    protected void onInheritStatsClicked(boolean flag, boolean initializing) {
        if (flag && !initializing) {
            obj.copyStats((Buff) CustomObjectManager.getLuaClass(((CustomGameObjectClass) obj).getIdentifier()));
        }

        for (Component c : rectComps) {
            if (c != null) c.setVisible(!flag);
        }

        for (Component c : linearComps) {
            if (c != null) c.setVisible(!flag);
        }

//        if (rename != null) rename.setVisible(!flag);

        ((CustomGameObjectClass) obj).setInheritStats(flag);
        
        super.onInheritStatsClicked(flag, initializing);
    }

    @Override
    protected void layout() {
        super.layout();
        layoutCompsInRectangles(rectComps);
        layoutCompsLinear(linearComps);

        layoutCustomObjectEditor();
    }

    @Override
    protected Component createTitle() {
        return WndInfoBuff.createIconTitle(obj);
    }

    @Override
    protected String createTitleText() {
        return Messages.titleCase(obj.name());
    }

    @Override
    protected String createDescription() {
        return obj.desc();
    }

    @Override
    public Image getIcon() {
        return null;
    }

    public static boolean areEqual(Buff a, Buff b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getClass() != b.getClass()) return false;

        if (a.permanent != b.permanent) return false;
        if (a.alwaysHidesFx != b.alwaysHidesFx) return false;

        if (a instanceof BuffWithDuration) {
            if (((BuffWithDuration) a).left != ((BuffWithDuration) b).left) return false;
        }
        
        if (a instanceof CustomGameObjectClass) {
            if (((CustomGameObjectClass) a).getInheritStats() != ((CustomGameObjectClass) b).getInheritStats()) return false;
        }

        return true;
    }

    public static boolean isBuffListEqual(LinkedHashSet<Buff> a, LinkedHashSet<Buff> b) {
        int sizeA = a == null ? 0 : a.size();
        int sizeB = b == null ? 0 : b.size();
        if (sizeA != sizeB) return false;
        if (a == null || b == null) return true;
        Buff[] bBuffs = new Buff[b.size()];
        int i = 0;
        for (Buff buff : b) {
            bBuffs[i++] = buff;
        }
        int index = 0;
        for (Buff buff : a) {
            if (!EditBuffComp.areEqual(buff, bBuffs[index++])) return false;
        }
        return true;
    }

}
