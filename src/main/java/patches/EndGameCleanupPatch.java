package patches;

import BetterSL.modcore.BetterSL;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.metrics.Metrics;
import com.megacrit.cardcrawl.monsters.MonsterGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EndGameCleanupPatch {
    @SpirePatch(
            clz = Metrics.class,
            method = "gatherAllDataAndSave"
    )
    public static class CleanupOnGameEnd {
        @SpireInsertPatch(rloc = 0) // 在方法开始处插入逻辑
        public static void cleanup(Metrics __instance, boolean death, boolean trueVictor, MonsterGroup monsters) {
            Path saveDir = Paths.get("SL"); // 存档基础路径
            try {
                if (Files.exists(saveDir)) {
                    Files.walk(saveDir)
                            .sorted((a, b) -> b.compareTo(a)) // 先删除子文件
                            .forEach(p -> p.toFile().delete());
                    BetterSL.logger.info("All saves deleted from " + saveDir.toString());
                }
            } catch (IOException e) {
                BetterSL.logger.error("Error deleting saves: " + e.getMessage());
            }
        }
    }
}
