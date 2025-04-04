package patches;

import BetterSL.modcore.BetterSL;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;

public class GameInstancePatch {
    public static CardCrawlGame gameInstance;

    @SpirePatch(clz = CardCrawlGame.class, method = "create")
    public static class SaveGameInstance {
        @SpirePostfixPatch
        public static void saveInstance(CardCrawlGame __instance) {
            gameInstance = __instance;
            BetterSL.logger.info("Saved CardCrawlGame instance.");
        }
    }

}
