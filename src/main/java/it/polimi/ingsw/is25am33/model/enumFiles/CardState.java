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
        public void showRelatedMenu(ClientView view) {
            // TODO Probabilmente nessun menu specifico per lo stato iniziale
            //return null;
        }
    },

    CHOOSE_CANNONS {
        @Override
        public void showRelatedMenu(ClientView view) {
            view.showChooseCannonsMenu();
        }
    },

    CHOOSE_ENGINES {
        @Override
        public void showRelatedMenu(ClientView view) {
            view.showChooseEnginesMenu();
        }
    },

    REMOVE_CREW_MEMBERS {
        @Override
        public void showRelatedMenu(ClientView view){
            view.showHandleRemoveCrewMembersMenu();
        }
    },

    HANDLE_CUBES_REWARD {
        @Override
        public void showRelatedMenu(ClientView view) {
            view.showHandleCubesRewardMenu();
        }
    },

    HANDLE_CUBES_MALUS {
        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO
            view.showHandleCubesMalusMenu();
        }
    },

    CHOOSE_PLANET {
        @Override
        public void showRelatedMenu(ClientView view) {
            view.showChoosePlanetMenu();
        }
    },

    VISIT_LOCATION {
        @Override
        public void showRelatedMenu(ClientView view) {
            view.showVisitLocationMenu();
        }
    },

    DANGEROUS_ATTACK {
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
        public void showRelatedMenu(ClientView view) {
            view.showThrowDicesMenu();
        }
    },

    EPIDEMIC {
        @Override
        public void showRelatedMenu(ClientView view){
            view.showEpidemicMenu();
        }
    },
    STARDUST {
        @Override
        public void showRelatedMenu(ClientView view) {
            view.showStardustMenu();
        }
    },

    ACCEPT_THE_REWARD {
        @Override
        public void showRelatedMenu(ClientView view) {
            view.showAcceptTheRewardMenu();
        }
    },

    EVALUATE_CREW_MEMBERS{
        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },
    EVALUATE_CANNON_POWER{
        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },

    EVALUATE_ENGINE_POWER{
        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },

    STEPS_BACK{

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    },

    END_OF_CARD {

        @Override
        public void showRelatedMenu(ClientView view) {
            // TODO Potrebbe mostrare una schermata informativa o per ulteriori azioni
        }
    };

    //public abstract BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view);
    public abstract void showRelatedMenu(ClientView view);
    // TODO chi ha scritto questo todo
    //TODO setProperty
}
