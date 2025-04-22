package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerDeserializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public enum CardState {

    START_CARD {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            // implementazione specifica
            return null;
        }
    },

    CHOOSE_CANNONS {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) throws IOException {
            BufferedReader reader = new BufferedReader(new StringReader(json));
            String doubleCannonLine = reader.readLine();
            List<Cannon> DoubleCannonsChoice = ServerDeserializer.fromComponentToDoubleCannon(doubleCannonLine, game.getCurrPlayer());
            String batteryBoxLine = reader.readLine();
            List<BatteryBox> batteryBoxesChoice = ServerDeserializer.fromComponentToBatteryBox(batteryBoxLine, game.getCurrPlayer());

            return new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenDoubleCannons(DoubleCannonsChoice)
                    .setChosenBatteryBoxes(batteryBoxesChoice)
                    .build();
        }
    },

    CHOOSE_ENGINES {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) throws IOException {
            BufferedReader reader = new BufferedReader(new StringReader(json));
            String doubleEnginesLine = reader.readLine();
            List<Engine> doubleEnginesChoice = ServerDeserializer.fromComponentToDoubleEngine(doubleEnginesLine, game.getCurrPlayer());
            String batteryBoxLine = reader.readLine();
            List<BatteryBox> batteryBoxesChoice = ServerDeserializer.fromComponentToBatteryBox(batteryBoxLine, game.getCurrPlayer());

            return new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenDoubleEngines(doubleEnginesChoice)
                    .setChosenBatteryBoxes(batteryBoxesChoice)
                    .build();
        }
    },

    REMOVE_CREW_MEMBERS {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            List<Cabin> cabinsChoice = ServerDeserializer.fromComponentToCabin(json, game.getCurrPlayer());
            return new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenCabins(cabinsChoice)
                    .build();
        }
    },

    HANDLE_CUBES_REWARD {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json)  {
            Storage storageChoice = ServerDeserializer.deserializeObj(json, Storage.class);
            return new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenStorage(storageChoice)
                    .build();
        }
    },

    HANDLE_CUBES_MALUS {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            Storage storageChoice = ServerDeserializer.deserializeObj(json, Storage.class);
            return new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenStorage(storageChoice)
                    .build();
        }
    },

    CHOOSE_PLANET {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            int playerChoice = ServerDeserializer.deserializeObj(json, Integer.class);
            return new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenPlanetIndex(playerChoice)
                    .build();
        }
    },

    VISIT_LOCATION {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            boolean playerChoice = ServerDeserializer.deserializeObj(json, Boolean.class);
            return new PlayerChoicesDataStructure
                    .Builder()
                    .setWantsToVisit(playerChoice)
                    .build();
        }
    },

    DANGEROUS_ATTACK {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) throws IOException {
            BufferedReader reader = new BufferedReader(new StringReader(json));
            String doubleEnginesLine = reader.readLine();
            Shield shieldChoice = ServerDeserializer.deserializeObj(doubleEnginesLine, Shield.class);
            String batteryBoxLine = reader.readLine();
            List<BatteryBox> batteryBoxesChoice = ServerDeserializer.fromComponentToBatteryBox(batteryBoxLine, game.getCurrPlayer());

            return new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenShield(shieldChoice)
                    .setChosenBatteryBoxes(batteryBoxesChoice)
                    .build();
        }
    },
    THROW_DICES {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }
    },
    EPIDEMIC {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }
    },
    STARDUST {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }
    },
    ACCEPT_THE_REWARD {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            boolean playerChoice = ServerDeserializer.deserializeObj(json, Boolean.class);
            return new PlayerChoicesDataStructure
                    .Builder()
                    .setHasAcceptedTheReward(playerChoice)
                    .build();
        }
    },
    END_OF_CARD {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }
    };

    public abstract PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) throws IOException;
}
