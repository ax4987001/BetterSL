package patches;

import BetterSL.modcore.BetterSL;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;

import java.util.ArrayList;

public class DungeonInstancePatch {
    public static AbstractDungeon DungeonInstance;

    @SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {String.class, String.class, AbstractPlayer.class, ArrayList.class})
    public static class SaveGameInstance_0 {
        @SpirePostfixPatch
        public static void saveInstance(AbstractDungeon __instance) {
            DungeonInstance = __instance;
            BetterSL.logger.info("Saved AbstractDungeon instance.");
        }
    }
    @SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {String.class, AbstractPlayer.class, SaveFile.class})
    public static class SaveGameInstance_1 {
        @SpirePostfixPatch
        public static void saveInstance(AbstractDungeon __instance) {
            DungeonInstance = __instance;
            BetterSL.logger.info("Saved AbstractDungeon instance.");
        }
    }
}
