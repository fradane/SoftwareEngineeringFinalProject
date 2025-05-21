package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
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

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Probabilmente nessun menu specifico per lo stato iniziale
            //return null;
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

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showChooseCannonsMenu();
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

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showChooseEnginesMenu();
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

        @Override
        public void showRelatedMenu(ClientView view){
            view.showHandleRemoveCrewMembersMenu();
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

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showHandleCubesRewardMenu();
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

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO
            view.showHandleCubesMalusMenu();
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

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showChoosePlanetMenu();
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

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showVisitLocationMenu();
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

        @Override
        public void showRelatedMenu(ClientView view) {
            DangerousObj currDangerousObject = view.getClientModel().getCurrDangerousObj();
            String type = currDangerousObject.getDangerousObjType();

            if (type.contains("SmallMeteorite") || type.contains("SmallShot")) {
                view.showSmallDanObjMenu();
            } else if (type.contains("BigMeteorite")) {
                view.showBigMeteoriteMenu();
            } else if (type.contains("BigShot")) {
                view.showBigShotMenu();
            }
        }
    },

    THROW_DICES {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showThrowDicesMenu();
        }
    },

    EPIDEMIC {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }
        public void showRelatedMenu(ClientView view){
            view.showEpidemicMenu();
        }
    },
    STARDUST {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showStardustMenu();
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

        @Override
        public void showRelatedMenu(ClientView view) {
            view.showAcceptTheRewardMenu();
        }
    },

    EVALUATE_CREW_MEMBERS{
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },
    EVALUATE_CANNON_POWER{
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },
    EVALUATE_ENGINE_POWER{
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },
    STEPS_BACK{
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },

    END_OF_CARD {
        @Override
        public PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) {
            return null;
        }

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    };

    public abstract PlayerChoicesDataStructure handleJsonDeserialization(GameModel game, String json) throws IOException;

    //public abstract BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view);
    public abstract void showRelatedMenu(ClientView view);
}
