package it.polimi.ingsw.is25am33.client.view;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.client.view.tui.MessageType;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interfaccia che definisce le operazioni di visualizzazione del client.
 * Può essere implementata sia da una CLI che da una GUI.
 */
public interface ClientView {

    ClientController getClientController();

    /**
     * Inizializza la view
     */
    void initialize();

    String askForInput(String questionDescription, String interrogationPrompt);

    void cancelInputWaiting();

    /**
     * Mostra un messaggio all'utente
     * @param message Il messaggio da mostrare
     */
    void showMessage(String message, MessageType type);

    /**
     * Mostra un messaggio di errore all'utente
     * @param errorMessage Il messaggio di errore
     */
    void showError(String errorMessage);

    /**
     * Chiede all'utente di inserire un nickname
     */
    void askNickname();

    /**
     * Chiede all'utente di creare un nuovo gioco
     * @return Un array contenente [numeroGiocatori, isTestFlight (0/1), coloreGiocatore (int)]
     */
    int[] askCreateGame();

    /**
     * Chiede all'utente di unirsi a un gioco esistente
     * @param games La lista dei giochi disponibili
     * @return Un array contenente [indiceGioco, coloreGiocatore (int)]
     */
    String[] askJoinGame(List<GameInfo> games);

    /**
     * Mostra il menù principale
     */
    void showMainMenu();

    /**
     * Mostra il menù di gioco quando l'utente è in una partita in attesa
     * @return La scelta dell'utente
     */
    int showGameMenu();

    /**
     * Notifica la view che un giocatore si è unito a un gioco
     * @param nickname Il nickname del giocatore
     * @param gameInfo Le informazioni aggiornate sul gioco
     */
    void notifyPlayerJoined(String nickname, GameInfo gameInfo);

    /**
     * Notifica la view che un giocatore ha lasciato un gioco
     * @param nickname Il nickname del giocatore
     * @param gameInfo Le informazioni aggiornate sul gioco
     */
    void notifyPlayerLeft(String nickname, GameInfo gameInfo);

    void notifyGameCreated(String gameId);

    /**
     * Notifica la view che il gioco è iniziato
     * @param gameState Lo stato iniziale del gioco
     */
    void notifyGameStarted(GameState gameState);

    /**
     * Notifica la view che il gioco è terminato
     * @param reason La ragione della terminazione
     */
    void notifyGameEnded(String reason);

    /**
     * Converte un intero in un colore del giocatore
     * @param colorChoice L'indice del colore (1-4)
     * @return Il colore del giocatore corrispondente
     */
    default PlayerColor intToPlayerColor(int colorChoice) {
        return switch (colorChoice) {
            case 1 -> PlayerColor.RED;
            case 2 -> PlayerColor.BLUE;
            case 3 -> PlayerColor.GREEN;
            case 4 -> PlayerColor.YELLOW;
            default -> PlayerColor.RED;
        };
    }

    String askPlayerColor(List<PlayerColor> availableColors);

    void showNewGameState();

    void showDangerousObj();

    void showNewCardState();

    void showCurrAdventureCard(boolean isFirstTime);

    ClientModel getClientModel();

    void showBuildShipBoardMenu();

    void notifyNoMoreComponentAvailable();

    void showPickedComponentAndMenu();

    void showShipBoard(ShipBoardClient shipBoard, String shipBoardOwnerNickname);

    void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap);

    void showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents);

//    BiConsumer<CallableOnGameController, String> showVisitLocationMenu();
//
//    BiConsumer<CallableOnGameController, String> showThrowDicesMenu();
//
//    BiConsumer<CallableOnGameController, String> showChoosePlanetMenu();
//
//    BiConsumer<CallableOnGameController, String> showChooseEnginesMenu();
//
//    BiConsumer<CallableOnGameController, String> showAcceptTheRewardMenu();
//
//    BiConsumer<CallableOnGameController, String> showChooseCannonsMenu();
//
//    BiConsumer<CallableOnGameController, String> showSmallDanObjMenu();
//
//    BiConsumer<CallableOnGameController, String> showBigMeteoriteMenu();
//
//    BiConsumer<CallableOnGameController, String> showBigShotMenu();
//
//    BiConsumer<CallableOnGameController, String> showHandleRemoveCrewMembersMenu();
//
//    BiConsumer<CallableOnGameController, String> showHandleCubesRewardMenu();
//
//    BiConsumer<CallableOnGameController, String> showEpidemicMenu();
//
//    BiConsumer<CallableOnGameController, String> showStardustMenu();
//
//    BiConsumer<CallableOnGameController, String> showHandleCubesMalusMenu();

    void showLittleDeck(int littleDeckChoice);

    void updateTimeLeft(int timeLeft, int flipsLeft);

    void notifyTimerEnded(int flipsLeft);

    void notifyHourglassStarted(int flipsLeft, String nickname);

    void notifyHourglassRestarted(int flipsLeft);

    Component askComponentToRemove(ShipBoardClient shipBoard, List<Component> incorrectlyPositionedComponents);

    void showWaitingForPlayers();

    void showPickReservedComponentQuestion();

    void showExitMenu();

    void showInvalidShipBoardMenu();

    void showValidShipBoardMenu();

    void showChooseComponentToRemoveMenu();

    void showChooseShipPartsMenu(List<Set<Coordinates>> shipParts);


    // ----------------- CARD PHASE MENU -----------------

    void showVisitLocationMenu();

    void showThrowDicesMenu();

    void showChoosePlanetMenu();

    void showChooseEnginesMenu();

    void showAcceptTheRewardMenu();

    void showChooseCannonsMenu();

    void showSmallDanObjMenu();

    void showBigMeteoriteMenu();

    void showBigShotMenu();

    void showHandleRemoveCrewMembersMenu();

    void showHandleCubesRewardMenu();

    void showEpidemicMenu();

    void showStardustMenu();

    void showHandleCubesMalusMenu();

    void checkShipBoardAfterAttackMenu();

    // ---------------------------------------------------


    void showFirstToEnter();

    void setIsTestFlight(boolean isTestFlight);

    void showCurrentRanking();

    void showCrewPlacementMenu();

    void showPrefabShipsMenu(List<PrefabShipInfo> prefabShips);

    void showComponentHitInfo(Coordinates coordinates);

    default ClientState cardStateToClientState(CardState cardState, ClientModel clientModel) {
        switch (cardState) {
            case VISIT_LOCATION:
                return ClientState.VISIT_LOCATION_MENU;
            case CHOOSE_PLANET:
                return ClientState.CHOOSE_PLANET_MENU;
            case CHOOSE_CANNONS:
                return ClientState.CHOOSE_CANNONS_MENU;
            case CHOOSE_ENGINES:
                return ClientState.CHOOSE_ENGINES_MENU;
            case THROW_DICES:
                return ClientState.THROW_DICES_MENU;
            case DANGEROUS_ATTACK:
                // Determine specific type based on dangerous object
                ClientDangerousObject obj = clientModel.getCurrDangerousObj();
                if (obj != null) {
                    String type = obj.getType();
                    if (type.contains("Small")) {
                        return ClientState.HANDLE_SMALL_DANGEROUS_MENU;
                    } else if (type.contains("bigMeteorite")) {
                        return ClientState.HANDLE_BIG_METEORITE_MENU;
                    } else if (type.contains("bigShot")) {
                        return ClientState.HANDLE_BIG_SHOT_MENU;
                    }
                }
                return ClientState.HANDLE_SMALL_DANGEROUS_MENU;// Default
            case ACCEPT_THE_REWARD:
                return ClientState.ACCEPT_REWARD_MENU;
            case HANDLE_CUBES_REWARD:
                return ClientState.HANDLE_CUBES_REWARD_MENU;
            case HANDLE_CUBES_MALUS:
                return ClientState.HANDLE_CUBES_MALUS_MENU;
            case REMOVE_CREW_MEMBERS:
                return ClientState.CHOOSE_CABIN_MENU;
            case EPIDEMIC:
                return ClientState.EPIDEMIC_MENU;
            case STARDUST:
                return ClientState.STARDUST_MENU;
            case CHECK_SHIPBOARD_AFTER_ATTACK:
                return ClientState.CHECK_SHIPBOARD_AFTER_ATTACK;
            default:
                return ClientState.PLAY_CARD;
        }
    }

    default void showCardStateMenu(ClientState mappedState) {
        // Automatically show the appropriate menu based on the mapped state
        switch (mappedState) {
            case VISIT_LOCATION_MENU:
                showVisitLocationMenu();
                break;
            case CHOOSE_CABIN_MENU:
                showHandleRemoveCrewMembersMenu();
                break;
            case CHOOSE_PLANET_MENU:
                showChoosePlanetMenu();
                break;
            case CHOOSE_CANNONS_MENU:
                showChooseCannonsMenu();
                break;
            case CHOOSE_ENGINES_MENU:
                showChooseEnginesMenu();
                break;
            case THROW_DICES_MENU:
                showThrowDicesMenu();
                break;
            case ACCEPT_REWARD_MENU:
                showAcceptTheRewardMenu();
                break;
            case HANDLE_SMALL_DANGEROUS_MENU:
                showSmallDanObjMenu();
                break;
            case HANDLE_BIG_METEORITE_MENU:
                showBigMeteoriteMenu();
                break;
            case HANDLE_BIG_SHOT_MENU:
                showBigShotMenu();
                break;
            case HANDLE_CUBES_REWARD_MENU:
                showHandleCubesRewardMenu();
                break;
            case EPIDEMIC_MENU:
                showEpidemicMenu();
                break;
            case STARDUST_MENU:
                showStardustMenu();
                break;
            case CHECK_SHIPBOARD_AFTER_ATTACK:
                checkShipBoardAfterAttackMenu();
                break;
        }
    }
}