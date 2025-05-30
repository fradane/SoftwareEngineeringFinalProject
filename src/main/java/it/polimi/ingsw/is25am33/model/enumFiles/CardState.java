package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerDeserializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.BiConsumer;

public enum CardState {

    START_CARD {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            // TODO
            return null;
        }
    },

    CHOOSE_CANNONS {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showChooseCannonsMenu();
        }
    },

    CHOOSE_ENGINES {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showChooseEnginesMenu();
        }
    },

    REMOVE_CREW_MEMBERS {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view){
            return view.showHandleRemoveCrewMembersMenu();
        }
    },

    HANDLE_CUBES_REWARD {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showHandleCubesRewardMenu();
        }
    },

    HANDLE_CUBES_MALUS {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            // TODO
            return null;
        }
    },

    CHOOSE_PLANET {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showChoosePlanetMenu();
        }
    },

    VISIT_LOCATION {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showVisitLocationMenu();
        }
    },

    DANGEROUS_ATTACK {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            DangerousObj currDangerousObject = view.getClientModel().getCurrDangerousObj();
            return currDangerousObject.showRelatedMenu(view);
        }
    },

    THROW_DICES {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showThrowDicesMenu();
        }
    },

    EPIDEMIC {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view){
            return view.showEpidemicMenu();
        }
    },
    STARDUST {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showStardustMenu();
        }
    },

    ACCEPT_THE_REWARD {
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            return view.showAcceptTheRewardMenu();
        }
    },

    EVALUATE_CREW_MEMBERS{
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            // TODO
            return null;
        }
    },
    EVALUATE_CANNON_POWER{
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            // TODO
            return null;
        }
    },

    EVALUATE_ENGINE_POWER{
        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            // TODO
            return null;
        }
    },

    STEPS_BACK{

        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            // TODO
            return null;
        }
    },

    END_OF_CARD {

        @Override
        public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
            // TODO
            return null;
        }
    };

    public abstract BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view);

    //TODO setProperty
}
