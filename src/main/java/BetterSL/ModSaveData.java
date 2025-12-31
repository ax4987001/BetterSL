package BetterSL;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardSave;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.BottledFlame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModSaveData {
    // 1. 玩家基础状态（对应原生 SaveFile 的 player 相关字段）
    public Map<String, PlayerCoreData> playerCoreDataMap; // key: 自定义标识（如floor/stateName）
    public Map<String, PlayerCoreData> saveStateCoreDataMap;

    // 2. 内部类：存储单个玩家的核心数据（无对象引用）
    public static class PlayerCoreData {
        // 基础信息（用于还原玩家实例）
        public String playerClassName; // 玩家类名（如 Ironclad）
        public String chosenClass; // 职业标识
        public int currentHealth;
        public int maxHealth;
        public int gold;
        public int ascensionLevel;
        public boolean isAscensionMode;
        public int masterHandSize;
        public int potionSlots;
        public int masterMaxOrbs;
        public int energyCount; // 总能量

        // 卡组信息（存储 CardSave，原生类，含ID/升级次数/misc）
        public List<CardSave> masterDeckCards;
        // 遗物信息（ID + 计数器）
        public List<String> relicIds;
        public List<Integer> relicCounters;
        // 药水信息（ID）
        public List<String> potionIds;
        // 疫病信息
        public List<String> blightIds;
        public List<Integer> blightIncrements;
        public List<Integer> blightCounters;
        // 瓶装卡片信息
        public String bottledFlameId;
        public int bottledFlameUpgrade;
        public int bottledFlameMisc;
        public String bottledLightningId;
        public int bottledLightningUpgrade;
        public int bottledLightningMisc;
        public String bottledTornadoId;
        public int bottledTornadoUpgrade;
        public int bottledTornadoMisc;

        // 空构造方法（Gson反序列化用）
        public PlayerCoreData() {}

        // 构造方法：从 AbstractPlayer 提取核心数据（模仿原生 SaveFile 数据收集逻辑）
        public PlayerCoreData(AbstractPlayer player) {
            this.playerClassName = player.getClass().getName();
            this.chosenClass = player.chosenClass.name();
            this.currentHealth = player.currentHealth;
            this.maxHealth = player.maxHealth;
            this.gold = player.gold;
            this.ascensionLevel = AbstractDungeon.ascensionLevel;
            this.isAscensionMode = AbstractDungeon.isAscensionMode;
            this.masterHandSize = player.masterHandSize;
            this.potionSlots = player.potionSlots;
            this.masterMaxOrbs = player.masterMaxOrbs;
            this.energyCount = player.energy.energyMaster;

            // 提取卡组数据（复用原生 CardSave）
            this.masterDeckCards = new ArrayList<>();
            for (AbstractCard card : player.masterDeck.group) {
                this.masterDeckCards.add(new CardSave(card.cardID, card.timesUpgraded, card.misc));
            }

            // 提取遗物数据
            this.relicIds = new ArrayList<>();
            this.relicCounters = new ArrayList<>();
            for (AbstractRelic relic : player.relics) {
                this.relicIds.add(relic.relicId);
                this.relicCounters.add(relic.counter);
            }

            // 提取药水数据（排除 PotionSlot）
            this.potionIds = new ArrayList<>();
            for (AbstractPotion potion : player.potions) {
                if (!(potion instanceof PotionSlot)) {
                    this.potionIds.add(potion.ID);
                }
            }

            // 提取疫病数据
            this.blightIds = new ArrayList<>();
            this.blightIncrements = new ArrayList<>();
            this.blightCounters = new ArrayList<>();
            for (AbstractBlight blight : player.blights) {
                this.blightIds.add(blight.blightID);
                this.blightIncrements.add(blight.increment);
                this.blightCounters.add(blight.counter);
            }

            // 提取瓶装卡片数据（模仿原生 loadPlayerSave 逻辑）
            AbstractRelic flameRelic = player.getRelic("Bottled Flame");
            if (flameRelic instanceof BottledFlame && ((BottledFlame) flameRelic).card != null) {
                AbstractCard flameCard = ((BottledFlame) flameRelic).card;
                this.bottledFlameId = flameCard.cardID;
                this.bottledFlameUpgrade = flameCard.timesUpgraded;
                this.bottledFlameMisc = flameCard.misc;
            }
            // 同理提取 bottledLightning、bottledTornado 数据...
        }
    }

    // 空构造方法（Gson反序列化用）
    public ModSaveData() {}

    // 构造方法：接收原有 Map，转换为核心数据 Map
    public ModSaveData(Map<String, AbstractPlayer> players, Map<String, AbstractPlayer> saveState) {
        this.playerCoreDataMap = new HashMap<>();
        this.saveStateCoreDataMap = new HashMap<>();

        // 转换 players 映射
        for (Map.Entry<String, AbstractPlayer> entry : players.entrySet()) {
            this.playerCoreDataMap.put(entry.getKey(), new PlayerCoreData(entry.getValue()));
        }
        // 转换 saveState 映射
        for (Map.Entry<String, AbstractPlayer> entry : saveState.entrySet()) {
            this.saveStateCoreDataMap.put(entry.getKey(), new PlayerCoreData(entry.getValue()));
        }
    }
}