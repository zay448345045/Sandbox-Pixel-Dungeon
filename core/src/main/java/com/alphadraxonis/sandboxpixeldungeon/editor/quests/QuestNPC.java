package com.alphadraxonis.sandboxpixeldungeon.editor.quests;

import com.alphadraxonis.sandboxpixeldungeon.actors.Char;
import com.alphadraxonis.sandboxpixeldungeon.actors.buffs.Buff;
import com.alphadraxonis.sandboxpixeldungeon.actors.mobs.npcs.NPC;
import com.alphadraxonis.sandboxpixeldungeon.editor.levels.LevelScheme;
import com.alphadraxonis.sandboxpixeldungeon.levels.RegularLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Bundle;

import java.util.List;

public abstract class QuestNPC<T extends Quest> extends NPC {

    public T quest;

    public QuestNPC() {
    }

    public QuestNPC(T quest) {
        this.quest = quest;
    }

    public void initQuest(LevelScheme levelScheme) {
        if (quest != null && quest.type() != Quest.NONE) {
            quest.initRandom(levelScheme);
        }
    }

    public abstract void createNewQuest();

    public abstract void place(RegularLevel level, List<Room> rooms);


    @Override
    public int defenseSkill(Char enemy) {
        return INFINITE_EVASION;
    }

    @Override
    public void damage(int dmg, Object src) {
        //do nothing
    }

    @Override
    public boolean add(Buff buff) {
        return false;
    }

    @Override
    public boolean reset() {
        return true;
    }


    private static final String QUEST = "quest";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        if (quest != null) bundle.put(QUEST, quest);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(QUEST)) quest = (T) bundle.get(QUEST);
    }

}