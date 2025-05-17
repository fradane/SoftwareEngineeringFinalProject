package it.polimi.ingsw.is25am33.client.view;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.ShipBoardClient;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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
        switch (colorChoice) {
            case 1: return PlayerColor.RED;
            case 2: return PlayerColor.BLUE;
            case 3: return PlayerColor.GREEN;
            case 4: return PlayerColor.YELLOW;
            default: return PlayerColor.RED;
        }
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

    void showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents);

    BiConsumer<CallableOnGameController, String> showVisitLocationMenu();

    BiConsumer<CallableOnGameController, String> showThrowDicesMenu();

    BiConsumer<CallableOnGameController, String> showChoosePlanetMenu();

    BiConsumer<CallableOnGameController, String> showChooseEnginesMenu();

    BiConsumer<CallableOnGameController, String> showAcceptTheRewardMenu();

    BiConsumer<CallableOnGameController, String> showChooseCannonsMenu();

    BiConsumer<CallableOnGameController, String> showSmallDanObjMenu();

    BiConsumer<CallableOnGameController, String> showBigMeteoriteMenu();

    BiConsumer<CallableOnGameController, String> showBigShotMenu();

    BiConsumer<CallableOnGameController, String> showHandleRemoveCrewMembersMenu();

    BiConsumer<CallableOnGameController, String> showHandleCubesRewardMenu();

    BiConsumer<CallableOnGameController, String> showEpidemicMenu();

    BiConsumer<CallableOnGameController, String> showStardustMenu();

    BiConsumer<CallableOnGameController, String> showHandleCubesMalusMenu();

    void showLittleDeck(int littleDeckChoice);

    void updateTimeLeft(int timeLeft);

    void notifyTimerEnded(int flipsLeft);

    void notifyHourglassStarted(int flipsLeft, String nickname);

    void notifyHourglassRestarted(int flipsLeft);

    Component askComponentToRemove(ShipBoardClient shipBoard, List<Component> incorrectlyPositionedComponents);

    void showWaitingForPlayers();

    void showPickReservedComponentQuestion();
}