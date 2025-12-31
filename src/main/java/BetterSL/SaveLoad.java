package BetterSL;


import BetterSL.modcore.BetterSL;
import basemod.BaseMod;
import basemod.abstracts.CustomSavable;
import basemod.interfaces.ISubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardSave;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.exceptions.SaveFileLoadError;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.BottledFlame;
import com.megacrit.cardcrawl.relics.BottledLightning;
import com.megacrit.cardcrawl.relics.BottledTornado;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.saveAndContinue.SaveFileObfuscator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patches.DungeonInstancePatch;
import patches.GameInstancePatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// 补充导入
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import static com.megacrit.cardcrawl.core.CardCrawlGame.*;
import static com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue.getPlayerSavePath;

public class SaveLoad implements CustomSavable<ModSaveData>, ISubscriber {
    private static final Logger logger = LogManager.getLogger(CardCrawlGame.class.getName());
    public static Map<String,AbstractPlayer> players = new HashMap<>();
    public static Map<String,AbstractPlayer> save_state = new HashMap<>();

    public static Path altSave = null;
    public static String file_name = null;

    // ========== 新增：自定义 Gson 实例（核心，过滤 scale 字段冲突） ==========
    // 简化自定义 Gson 实例（仅过滤冲突字段，无需过滤类）
    private static final Gson CUSTOM_SAVE_GSON = new GsonBuilder()
            .disableHtmlEscaping() // 对齐原生 Gson 配置
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    // 仅过滤已知冲突字段（scale/color），其他核心字段正常序列化
                    return "scale".equals(f.getName()) || "color".equals(f.getName());
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();

    // 保留原有 onSaveRaw() 逻辑（使用简化后的 Gson）
    @Override
    public JsonElement onSaveRaw() {
        return CUSTOM_SAVE_GSON.toJsonTree(this.onSave());
    }
    public SaveLoad() {
        BaseMod.subscribe(this);
        BaseMod.addSaveField("BetterSL:ModSaveData", this);
        BetterSL.logger.info("SaveLoad Init");
    }
    @Override
    public ModSaveData onSave() {
        return new  ModSaveData(players, save_state);
    }
    @Override
    public void onLoad(ModSaveData save) {
        // 清空原有数据
        players.clear();
        save_state.clear();

        // 还原 players 映射
        if (save.playerCoreDataMap != null) {
            for (Map.Entry<String, ModSaveData.PlayerCoreData> entry : save.playerCoreDataMap.entrySet()) {
                String key = entry.getKey();
                ModSaveData.PlayerCoreData coreData = entry.getValue();
                AbstractPlayer player = restorePlayerFromCoreData(coreData);
                if (player != null) {
                    players.put(key, player);
                }
            }
        }

        // 还原 save_state 映射
        if (save.saveStateCoreDataMap != null) {
            for (Map.Entry<String, ModSaveData.PlayerCoreData> entry : save.saveStateCoreDataMap.entrySet()) {
                String key = entry.getKey();
                ModSaveData.PlayerCoreData coreData = entry.getValue();
                AbstractPlayer player = restorePlayerFromCoreData(coreData);
                if (player != null) {
                    save_state.put(key, player);
                }
            }
        }
    }

    // 辅助方法：从 PlayerCoreData 还原 AbstractPlayer（复用原生 API）
    private AbstractPlayer restorePlayerFromCoreData(ModSaveData.PlayerCoreData coreData) {
        try {
            // 1. 反射创建玩家实例（模仿原生存档还原逻辑）
            Class<?> playerClass = Class.forName(coreData.playerClassName);
            AbstractPlayer player = (AbstractPlayer) playerClass.getConstructor().newInstance();

            // 2. 还原基础状态
            player.currentHealth = coreData.currentHealth;
            player.maxHealth = coreData.maxHealth;
            player.gold = coreData.gold;
            player.displayGold = coreData.gold;
            player.chosenClass = AbstractPlayer.PlayerClass.valueOf(coreData.chosenClass);
            AbstractDungeon.ascensionLevel = coreData.ascensionLevel;
            AbstractDungeon.isAscensionMode = coreData.isAscensionMode;
            player.masterHandSize = coreData.masterHandSize;
            player.potionSlots = coreData.potionSlots;
            player.masterMaxOrbs = coreData.masterMaxOrbs;
            player.energy = new EnergyManager(coreData.energyCount);

            // 3. 还原卡组（复用原生 CardLibrary.getCopy()）
            player.masterDeck.clear();
            for (CardSave cardSave : coreData.masterDeckCards) {
                player.masterDeck.addToTop(CardLibrary.getCopy(cardSave.id, cardSave.upgrades, cardSave.misc));
            }

            // 4. 还原遗物（复用原生 RelicLibrary.getRelic()）
            player.relics.clear();
            int relicIndex = 0;
            for (String relicId : coreData.relicIds) {
                AbstractRelic relic = RelicLibrary.getRelic(relicId).makeCopy();
                relic.instantObtain(player, relicIndex, false);
                // 还原遗物计数器
                if (relicIndex < coreData.relicCounters.size()) {
                    relic.setCounter(coreData.relicCounters.get(relicIndex));
                }
                relic.updateDescription(player.chosenClass);
                relicIndex++;
            }

            // 5. 还原药水（复用原生 PotionHelper.getPotion()）
            player.potions.clear();
            // 先添加药水槽
            for (int i = 0; i < coreData.potionSlots; i++) {
                player.potions.add(new PotionSlot(i));
            }
            // 再添加药水实例
            int potionIndex = 0;
            for (String potionId : coreData.potionIds) {
                AbstractPotion potion = PotionHelper.getPotion(potionId);
                if (potion != null && potionIndex < coreData.potionSlots) {
                    player.obtainPotion(potionIndex, potion);
                    potionIndex++;
                }
            }

            // 6. 还原疫病（复用原生 BlightHelper.getBlight()）
            player.blights.clear();
            int blightIndex = 0;
            for (String blightId : coreData.blightIds) {
                AbstractBlight blight = BlightHelper.getBlight(blightId);
                if (blight != null) {
                    int incrementAmount = coreData.blightIncrements.get(blightIndex);
                    // 还原疫病等级
                    for (int i = 0; i < incrementAmount; i++) {
                        blight.incrementUp();
                    }
                    blight.setIncrement(incrementAmount);
                    blight.instantObtain(player, blightIndex, false);
                    // 还原疫病计数器
                    if (blightIndex < coreData.blightCounters.size()) {
                        blight.setCounter(coreData.blightCounters.get(blightIndex));
                        blight.updateDescription(player.chosenClass);
                    }
                }
                blightIndex++;
            }

            // 7. 还原瓶装卡片（模仿原生 loadPlayerSave 逻辑）
            if (coreData.bottledFlameId != null) {
                AbstractCard flameCard = null;
                for (AbstractCard card : player.masterDeck.group) {
                    if (card.cardID.equals(coreData.bottledFlameId)
                            && card.timesUpgraded == coreData.bottledFlameUpgrade
                            && card.misc == coreData.bottledFlameMisc) {
                        flameCard = card;
                        break;
                    }
                }
                if (flameCard != null) {
                    flameCard.inBottleFlame = true;
                    AbstractRelic flameRelic = player.getRelic("Bottled Flame");
                    if (flameRelic instanceof BottledFlame) {
                        ((BottledFlame) flameRelic).card = flameCard;
                        ((BottledFlame) flameRelic).setDescriptionAfterLoading();
                    }
                }
            }
            // 同理还原 bottledLightning、bottledTornado...

            return player;
        } catch (Exception e) {
            BetterSL.logger.error("还原玩家失败", e);
            return null;
        }
    }
    public static void save(String save_name) {
        BetterSL.logger.info("Saving to save state " + save_state);
        copySaveFile(save_name);
        BetterSL.logger.info("Saving success!");
        save_state.put(save_name,AbstractDungeon.player);


    }


    public static void load(int floor) {
        file_name = getPlayerSavePath(AbstractDungeon.player.chosenClass).split("\\\\")[1];
        BetterSL.logger.info("Loading from save state " + floor + file_name);
        altSave = Paths.get("SL", String.valueOf(floor), file_name); // 设置路径
        SaveAndContinue.deleteSave(AbstractDungeon.player);
        try {
            saveFile = loadSaveFile(altSave);
            GameInstancePatch.gameInstance.getDungeon(saveFile.level_name, AbstractDungeon.player, saveFile);
            DungeonInstancePatch.DungeonInstance.loadSave(saveFile);
            DungeonInstancePatch.DungeonInstance.player = players.get(String.valueOf(floor));
            loadPlayerSave(DungeonInstancePatch.DungeonInstance.player, saveFile);


            BetterSL.logger.info("Loading success!");
            BetterSL.logger.info(CardCrawlGame.loadingSave);
        } catch (SaveFileLoadError e) {
            throw new RuntimeException(e);
        }

    }
    public static void load(String state_name) {
        file_name = getPlayerSavePath(AbstractDungeon.player.chosenClass).split("\\\\")[1];
        BetterSL.logger.info("Loading from save state " + state_name + file_name);
        altSave = Paths.get("SL", state_name, file_name); // 设置路径
        SaveAndContinue.deleteSave(AbstractDungeon.player);
        try {
            saveFile = loadSaveFile(altSave);
            GameInstancePatch.gameInstance.getDungeon(saveFile.level_name, AbstractDungeon.player, saveFile);
            DungeonInstancePatch.DungeonInstance.loadSave(saveFile);
            DungeonInstancePatch.DungeonInstance.player = save_state.get(state_name);
            loadPlayerSave(DungeonInstancePatch.DungeonInstance.player, saveFile);


            BetterSL.logger.info("Loading success!");
            BetterSL.logger.info(CardCrawlGame.loadingSave);
        } catch (SaveFileLoadError e) {
            throw new RuntimeException(e);
        }

    }

    private static SaveFile loadSaveFile(Path altSave) throws SaveFileLoadError {
        try {
            FileHandle file = new FileHandle(altSave.toFile());
            String data = file.readString();
            String savestr = SaveFileObfuscator.isObfuscated(data) ? SaveFileObfuscator.decode(data, "key") : data;
            // 复用自定义 Gson（与序列化逻辑一致）
            return CUSTOM_SAVE_GSON.fromJson(savestr, SaveFile.class);
        } catch (Exception var7) {
            throw new SaveFileLoadError("Unable to load save file: " + altSave, var7);
        }
    }
    public static void copySaveFile(String targetDirectory) {
        file_name = getPlayerSavePath(AbstractDungeon.player.chosenClass).split("\\\\")[1];
        try {
            FileHandle sourceFile = Gdx.files.local("saves/" + file_name);
            FileHandle targetDir = Gdx.files.local("SL/" + targetDirectory);
            FileHandle targetFile = targetDir.child(file_name);

            // 原生文件操作，更稳定
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            sourceFile.copyTo(targetFile);

            BetterSL.logger.info("File copy succeeded：from " + sourceFile.path() + " to " + targetFile.path());
        } catch (Exception e) {
            BetterSL.logger.error("File copy failure：" + e.getMessage());
        }
    }

    private static void loadPlayerSave(AbstractPlayer p,SaveFile saveFile) {
        AbstractDungeon.loading_post_combat = false;
        Settings.seed = saveFile.seed;
        Settings.isFinalActAvailable = saveFile.is_final_act_on;
        Settings.hasRubyKey = saveFile.has_ruby_key;
        Settings.hasEmeraldKey = saveFile.has_emerald_key;
        Settings.hasSapphireKey = saveFile.has_sapphire_key;
        Settings.isDailyRun = saveFile.is_daily;
        if (Settings.isDailyRun) {
            Settings.dailyDate = saveFile.daily_date;
        }

        Settings.specialSeed = saveFile.special_seed;
        Settings.seedSet = saveFile.seed_set;
        Settings.isTrial = saveFile.is_trial;
        if (Settings.isTrial) {
            ModHelper.setTodaysMods(Settings.seed, AbstractDungeon.player.chosenClass);
            AbstractPlayer.customMods = saveFile.custom_mods;
        } else if (Settings.isDailyRun) {
            ModHelper.setTodaysMods(Settings.specialSeed, AbstractDungeon.player.chosenClass);
        }

        AbstractPlayer.customMods = saveFile.custom_mods;
        if (AbstractPlayer.customMods == null) {
            AbstractPlayer.customMods = new ArrayList<>();
        }

        p.currentHealth = saveFile.current_health;
        p.maxHealth = saveFile.max_health;
        p.gold = saveFile.gold;
        p.displayGold = p.gold;
        p.masterHandSize = saveFile.hand_size;
        p.potionSlots = saveFile.potion_slots;
        if (p.potionSlots == 0) {
            p.potionSlots = 3;
        }

        p.potions.clear();

        for (int i = 0; i < p.potionSlots; i++) {
            p.potions.add(new PotionSlot(i));
        }

        p.masterMaxOrbs = saveFile.max_orbs;
        p.energy = new EnergyManager(saveFile.red + saveFile.green + saveFile.blue);
        monstersSlain = saveFile.monsters_killed;
        elites1Slain = saveFile.elites1_killed;
        elites2Slain = saveFile.elites2_killed;
        elites3Slain = saveFile.elites3_killed;
        goldGained = saveFile.gold_gained;
        champion = saveFile.champions;
        perfect = saveFile.perfect;
        combo = saveFile.combo;
        overkill = saveFile.overkill;
        mysteryMachine = saveFile.mystery_machine;
        playtime = (float)saveFile.play_time;
        AbstractDungeon.ascensionLevel = saveFile.ascension_level;
        AbstractDungeon.isAscensionMode = saveFile.is_ascension_mode;
        p.masterDeck.clear();

        for (CardSave s : saveFile.cards) {
            logger.info(s.id + ", " + s.upgrades);
            p.masterDeck.addToTop(CardLibrary.getCopy(s.id, s.upgrades, s.misc));
        }

        Settings.isEndless = saveFile.is_endless_mode;
        int index = 0;
        p.blights.clear();
        if (saveFile.blights != null) {
            for (String b : saveFile.blights) {
                AbstractBlight blight = BlightHelper.getBlight(b);
                if (blight != null) {
                    int incrementAmount = saveFile.endless_increments.get(index);

                    for (int i = 0; i < incrementAmount; i++) {
                        blight.incrementUp();
                    }

                    blight.setIncrement(incrementAmount);
                    blight.instantObtain(AbstractDungeon.player, index, false);
                }

                index++;
            }

            if (saveFile.blight_counters != null) {
                index = 0;

                for (Integer i : saveFile.blight_counters) {
                    p.blights.get(index).setCounter(i);
                    p.blights.get(index).updateDescription(p.chosenClass);
                    index++;
                }
            }
        }

        p.relics.clear();
        index = 0;

        for (String s : saveFile.relics) {
            AbstractRelic r = RelicLibrary.getRelic(s).makeCopy();
            r.instantObtain(p, index, false);
            if (index < saveFile.relic_counters.size()) {
                r.setCounter(saveFile.relic_counters.get(index));
            }

            r.updateDescription(p.chosenClass);
            index++;
        }

        index = 0;

        for (String s : saveFile.potions) {
            AbstractPotion potion = PotionHelper.getPotion(s);
            if (potion != null) {
                AbstractDungeon.player.obtainPotion(index, potion);
            }

            index++;
        }

        AbstractCard tmpCard = null;
        if (saveFile.bottled_flame != null) {
            for (AbstractCard i : AbstractDungeon.player.masterDeck.group) {
                if (i.cardID.equals(saveFile.bottled_flame)) {
                    tmpCard = i;
                    if (i.timesUpgraded == saveFile.bottled_flame_upgrade && i.misc == saveFile.bottled_flame_misc) {
                        break;
                    }
                }
            }

            if (tmpCard != null) {
                tmpCard.inBottleFlame = true;
                ((BottledFlame)AbstractDungeon.player.getRelic("Bottled Flame")).card = tmpCard;
                ((BottledFlame)AbstractDungeon.player.getRelic("Bottled Flame")).setDescriptionAfterLoading();
            }
        }

        tmpCard = null;
        if (saveFile.bottled_lightning != null) {
            for (AbstractCard ix : AbstractDungeon.player.masterDeck.group) {
                if (ix.cardID.equals(saveFile.bottled_lightning)) {
                    tmpCard = ix;
                    if (ix.timesUpgraded == saveFile.bottled_lightning_upgrade && ix.misc == saveFile.bottled_lightning_misc) {
                        break;
                    }
                }
            }

            if (tmpCard != null) {
                tmpCard.inBottleLightning = true;
                ((BottledLightning)AbstractDungeon.player.getRelic("Bottled Lightning")).card = tmpCard;
                ((BottledLightning)AbstractDungeon.player.getRelic("Bottled Lightning")).setDescriptionAfterLoading();
            }
        }

        tmpCard = null;
        if (saveFile.bottled_tornado != null) {
            for (AbstractCard ixx : AbstractDungeon.player.masterDeck.group) {
                if (ixx.cardID.equals(saveFile.bottled_tornado)) {
                    tmpCard = ixx;
                    if (ixx.timesUpgraded == saveFile.bottled_tornado_upgrade && ixx.misc == saveFile.bottled_tornado_misc) {
                        break;
                    }
                }
            }

            if (tmpCard != null) {
                tmpCard.inBottleTornado = true;
                ((BottledTornado)AbstractDungeon.player.getRelic("Bottled Tornado")).card = tmpCard;
                ((BottledTornado)AbstractDungeon.player.getRelic("Bottled Tornado")).setDescriptionAfterLoading();
            }
        }

        if (saveFile.daily_mods != null && saveFile.daily_mods.size() > 0) {
            ModHelper.setMods(saveFile.daily_mods);
        }

        metricData.clearData();
        metricData.campfire_rested = saveFile.metric_campfire_rested;
        metricData.campfire_upgraded = saveFile.metric_campfire_upgraded;
        metricData.purchased_purges = saveFile.metric_purchased_purges;
        metricData.potions_floor_spawned = saveFile.metric_potions_floor_spawned;
        metricData.current_hp_per_floor = saveFile.metric_current_hp_per_floor;
        metricData.max_hp_per_floor = saveFile.metric_max_hp_per_floor;
        metricData.gold_per_floor = saveFile.metric_gold_per_floor;
        metricData.path_per_floor = saveFile.metric_path_per_floor;
        metricData.path_taken = saveFile.metric_path_taken;
        metricData.items_purchased = saveFile.metric_items_purchased;
        metricData.items_purged = saveFile.metric_items_purged;
        metricData.card_choices = saveFile.metric_card_choices;
        metricData.event_choices = saveFile.metric_event_choices;
        metricData.damage_taken = saveFile.metric_damage_taken;
        metricData.boss_relics = saveFile.metric_boss_relics;
        if (saveFile.metric_potions_obtained != null) {
            metricData.potions_obtained = saveFile.metric_potions_obtained;
        }

        if (saveFile.metric_relics_obtained != null) {
            metricData.relics_obtained = saveFile.metric_relics_obtained;
        }

        if (saveFile.metric_campfire_choices != null) {
            metricData.campfire_choices = saveFile.metric_campfire_choices;
        }

        if (saveFile.metric_item_purchase_floors != null) {
            metricData.item_purchase_floors = saveFile.metric_item_purchase_floors;
        }

        if (saveFile.metric_items_purged_floors != null) {
            metricData.items_purged_floors = saveFile.metric_items_purged_floors;
        }

        if (saveFile.neow_bonus != null) {
            metricData.neowBonus = saveFile.neow_bonus;
        }

        if (saveFile.neow_cost != null) {
            metricData.neowCost = saveFile.neow_cost;
        }
    }



}
