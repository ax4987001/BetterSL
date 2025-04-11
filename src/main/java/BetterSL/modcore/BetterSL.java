package BetterSL.modcore;

import BetterSL.Command.SLCommand;
import BetterSL.SaveLoad;
import basemod.BaseMod;
import basemod.devcommands.ConsoleCommand;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scannotation.AnnotationDB;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;


@SpireInitializer
public class BetterSL implements PostInitializeSubscriber {
    public static ModInfo info;
    public static String modID = "BetterSL";
    public static final Logger logger = LogManager.getLogger(modID);

    public static void initialize(){
        new BetterSL();
        new SaveLoad();

    }
    public BetterSL() {
        BaseMod.subscribe(this);
    }
    private static void loadModInfo() {
        Optional<ModInfo> infos = Arrays.stream(Loader.MODINFOS)
                .filter(
                        modInfo -> {
                            AnnotationDB annotationDB = Patcher.annotationDBMap.get(modInfo.jarURL);
                            if (annotationDB == null) {
                                return false;
                            } else {
                                Set<String> initializers = annotationDB.getAnnotationIndex()
                                        .getOrDefault(SpireInitializer.class.getName(), Collections.emptySet());
                                return initializers.contains(BetterSL.class.getName());
                            }
                        }
                )
                .findFirst();
        if (infos.isPresent()) {
            info = infos.get();
            modID = info.ID;
        } else {
            throw new RuntimeException("Failed to determine mod info/ID based on initializer.");
        }
    }
    static {
        loadModInfo();
    }
    @Override
    public void receivePostInitialize() {
        try {
            ConsoleCommand.addCommand("sl", SLCommand.class);
            logger.info("SLCommand successfully registered.");
        } catch (Exception e) {
            logger.error("Failed to register SLCommand: " + e.getMessage());
            throw new RuntimeException("SLCommand registration failed.", e);
        }
    }



}
