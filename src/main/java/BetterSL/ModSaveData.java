package BetterSL;

import com.megacrit.cardcrawl.characters.AbstractPlayer;

import java.util.Map;

public class ModSaveData {
    public Map<String, AbstractPlayer> players;
    public Map<String,AbstractPlayer> save_state;

    public ModSaveData(Map<String,AbstractPlayer> players, Map<String,AbstractPlayer> save_state) {
        this.players = players;
        this.save_state = save_state;
    }
}