package patches;

import BetterSL.modcore.BetterSL;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.AsyncSaver;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.saveAndContinue.SaveFileObfuscator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static BetterSL.SaveLoad.players;


@SpirePatch(clz = SaveAndContinue.class, method = "save")
public class SavePatch {
    @SpireInsertPatch(loc = 308,localvars = {"filepath","data"})
    public static void InsertPatch(SaveFile save,String filepath,String data) {
        int currentFloor = AbstractDungeon.floorNum; // 获取当前楼层号
        Path saveDir = Paths.get("SL", String.valueOf(currentFloor)); // 构建保存路径
        String saveFile = "SL\\" +currentFloor +"\\"+filepath.split("\\\\")[1];
        try {
            Files.createDirectories(saveDir); // 确保目录存在
            if (Settings.isBeta) {
                AsyncSaver.save(saveFile + "BETA", data);
            }

            AsyncSaver.save(saveFile, SaveFileObfuscator.encode(data, "key"));
            players.put(String.valueOf(AbstractDungeon.floorNum),AbstractDungeon.player);

            BetterSL.logger.info("Saved floor " + currentFloor + " to " + saveFile);
        } catch (IOException e) {
            BetterSL.logger.error("Error saving floor " + currentFloor + ": " + e.getMessage());
        }

    }

}