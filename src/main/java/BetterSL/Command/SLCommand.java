//
// Source code recreated from a .class file by Vineflower
//

package BetterSL.Command;

import BetterSL.SaveLoad;
import BetterSL.modcore.BetterSL;
import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;

import java.util.ArrayList;



public class SLCommand extends ConsoleCommand {
    private static final String regex = "^\\D.*";
    public SLCommand() {
        this.minExtraTokens = 0;
        this.maxExtraTokens = 3;
        this.simpleCheck = true;
    }

    public void execute(String[] tokens, int depth) {
        for (String s : tokens){
            BetterSL.logger.info(s);
        }
        if (tokens.length < 2) {
            errorMsg();
        } else {
            String command = tokens[1];
            String argument = (tokens.length > 2) ? tokens[2] : null;
            try {
                switch (command) {
                    case "save":
                        if (argument != null && argument.matches(regex)) {
                            SaveLoad.save(argument);
                            BetterSL.logger.info("Save operation successful: " + argument);
                        } else {
                            errorMsg();
                        }
                        break;

                    case "load":
                        if (argument != null) {
                            if (SaveLoad.save_state.containsKey(argument)) {
                                SaveLoad.load(argument);
                                BetterSL.logger.info("Load operation successful: " + argument);
                            } else if (SaveLoad.players.containsKey(argument)) {
                                SaveLoad.load(Integer.parseInt(argument));
                                BetterSL.logger.info("Load operation successful with player ID: " + argument);
                            } else {
                                errorMsg();
                            }
                        } else {
                            errorMsg();
                        }
                        break;

                    case "delete":
                        if (argument != null) {
                            SaveLoad.save_state.remove(argument);
                            BetterSL.logger.info("Delete operation successful: " + argument);
                        } else {
                            errorMsg();
                        }
                        break;

                    case "list":
                        for (String s : SaveLoad.save_state.keySet()) {
                            DevConsole.log(s);
                        }
                        DevConsole.log(SaveLoad.players.keySet());
                        BetterSL.logger.info("List operation successful.");
                        break;

                    default:
                        errorMsg();
                }
            } catch (Exception e) {
                BetterSL.logger.error("An error occurred while executing SLCommand: " + e.getMessage());
                errorMsg();
            }
        }
    }

    public ArrayList<String> extraOptions(String[] tokens, int depth) {
        ArrayList<String> result = new ArrayList();
        if (tokens.length == 2) {
            // 一级命令补全
            result.add("save");
            result.add("load");
            result.add("delete");
            result.add("list");
        } else if (tokens.length >= 3) {
            String command = tokens[1];
            BetterSL.logger.info(tokens[1]);
            switch (command) {
                case "load":
                    for (String s : SaveLoad.save_state.keySet()) {
                        result.add(s);
                    }
                    for (String s : SaveLoad.players.keySet()) {
                        result.add(s);
                    }
                    break;
                case "delete":
                    for (String s : SaveLoad.save_state.keySet()) {
                        result.add(s);
                    }
                    break;
                case "save":
                    result.add("name");
                    break;
                case "list":
                    complete = true;
                    break;
                default:
                    break;
            }
        }
        System.out.println(result);
        return result;

    }

    public void errorMsg() {
        BetterSL.logger.error("Invalid command or parameters!");
        DevConsole.couldNotParse();
        DevConsole.log("Available options are:");
        DevConsole.log("* save [name]");
        DevConsole.log("* load [name or floor]");
        DevConsole.log("* delete [name]");
        DevConsole.log("* list");
    }
}
