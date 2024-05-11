package com.shatteredpixel.shatteredpixeldungeon.editor;

import com.shatteredpixel.shatteredpixeldungeon.editor.util.EditorUtilies;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

public class ArrowCell implements Bundlable, PathFinder.ArrowCellInterface {

    // Order is important!!!
    public static final int NONE = 0;
    public static final int TOP_LEFT = 1;
    public static final int TOP = 2;
    public static final int TOP_RIGHT = 4;
    public static final int LEFT = 8;
    public static final int RIGHT = 16;
    public static final int BOTTOM_LEFT = 32;
    public static final int BOTTOM = 64;
    public static final int BOTTOM_RIGHT = 128;
    public static final int ALL = 255;


    public int pos;
//    public int directionsLeave;
    public int directionsEnter;
    public boolean allowsWaiting = true;
    public boolean visible;


    public ArrowCell() {
    }

    public ArrowCell(int pos) {
        this(pos, /*ALL,*/ ALL);
    }

    public ArrowCell(int pos, /*int directionsLeave,*/ int directionsEnter) {
        this.pos = pos;
//        this.directionsLeave = directionsLeave;
        this.directionsEnter = directionsEnter;
    }

    private static final String POS = "pos";
//    private static final String DIRECTIONS_LEAVE = "directions_leave";
    private static final String DIRECTIONS_ENTER = "directions_enter";
    private static final String ALLOWS_WAITING = "allows_waiting";
    private static final String VISIBLE = "visible";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(POS, pos);
//        bundle.put(DIRECTIONS_LEAVE, directionsLeave);
        bundle.put(DIRECTIONS_ENTER, directionsEnter);
        bundle.put(ALLOWS_WAITING, allowsWaiting);
        bundle.put(VISIBLE, visible);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        pos = bundle.getInt(POS);
//        directionsLeave = bundle.getInt(DIRECTIONS_LEAVE);
        directionsEnter = bundle.getInt(DIRECTIONS_ENTER);
        allowsWaiting = bundle.getBoolean(ALLOWS_WAITING);
        visible = bundle.getBoolean(VISIBLE);
    }

    public ArrowCell getCopy() {
        ArrowCell copy = new ArrowCell(pos, /*directionsLeave,*/ directionsEnter);
        copy.visible = visible;
        copy.allowsWaiting = allowsWaiting;
        return copy;
    }

    public Image getSprite() {
        return EditorUtilies.getArrowCellTexture(this);
    }

    public String name() {
        return Messages.get(this, "name");
    }

    public String desc() {
        String desc = Messages.get(this, "desc");
//        if (blocks == 0) desc += "\n" + Messages.get(this, "block_none");
//        else {
//            for (int i = 0; i < NUM_BLOCK_TYPES; i++) {
//                int bit = (int) Math.pow(2, i);
//                if ((blocks & bit) != 0) desc += "\n" + Messages.get(this, "block_" + getBlockKey(bit));
//            }
//        }
        return desc;
    }

//    public static String getBlockKey(int blockBit) {
//        switch (blockBit) {
//            case BLOCK_PLAYER:
//                return "player";
//            case BLOCK_MOBS:
//                return "mobs";
//            case BLOCK_ALLIES:
//                return "allies";
//            case BLOCK_PROJECTILES:
//                return "projectiles";
//            case BLOCK_BLOBS:
//                return "blobs";
//        }
//        return "none";
//    }

    public boolean allowsDirectionLeaving(int pathfinderNeighboursValue) {
//        return allowsDirection(pathfinderNeighboursValue, directionsLeave);
        return allowsDirection(pathfinderNeighboursValue, directionsEnter);
    }

    public boolean allowsDirectionEnter(int pathfinderNeighboursValue) {
        return allowsDirection(-pathfinderNeighboursValue, directionsEnter);
    }

    public static boolean allowsDirection(int pathfinderNeighboursValue, int directionsAllowed) {
        return (directionsAllowed & findDirectionBit(pathfinderNeighboursValue)) != 0;
    }

    private static int findDirectionBit(int pathfinderNeighboursValue) {
        for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
            if (PathFinder.NEIGHBOURS8[i] == pathfinderNeighboursValue) return 1 << i;
        }
        return NONE;
    }

    /**
     * Assumes from and to are <b><u>adjacent</u></b>!
     */
    public static boolean allowsStep(int from, int to) {
       return PathFinder.ArrowCellInterface.allowsStep(from, to);
    }
}