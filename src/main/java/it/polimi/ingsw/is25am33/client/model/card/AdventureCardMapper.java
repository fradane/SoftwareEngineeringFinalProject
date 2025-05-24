package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.card.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping server-side adventure cards to client-side card representations.
 * This enables efficient transfer of card data while maintaining proper object structure
 * for the client UI to access.
 */
public class AdventureCardMapper {

    /**
     * Maps a server-side AdventureCard to its corresponding client-side representation.
     *
     * @param serverCard The server-side card to convert
     * @return A client-side card representation with all relevant data
     * @throws IllegalArgumentException if the card type is not supported
     */
    public static ClientCard toClientCard(AdventureCard serverCard) {
        if (serverCard == null) {
            return null;
        }

//        if (serverCard instanceof Planets) {
//            return toPlanetsCard((Planets) serverCard);
//        }
//        else if (serverCard instanceof AbandonedShip) {
//            return toAbandonedShipCard((AbandonedShip) serverCard);
//        } else if (serverCard instanceof AbandonedStation) {
//            return toAbandonedStationCard((AbandonedStation) serverCard);
//        } else if (serverCard instanceof Pirates) {
//            return toPiratesCard((Pirates) serverCard);
//        } else if (serverCard instanceof SlaveTraders) {
//            return toSlaveTradersCard((SlaveTraders) serverCard);
//        } else if (serverCard instanceof Smugglers) {
//            return toSmugglersCard((Smugglers) serverCard);
//        } else if (serverCard instanceof MeteoriteStorm) {
//            return toMeteoriteStormCard((MeteoriteStorm) serverCard);
//        } else if (serverCard instanceof FreeSpace) {
//            return toFreeSpaceCard((FreeSpace) serverCard);
//        } else if (serverCard instanceof Epidemic) {
//            return toEpidemicCard((Epidemic) serverCard);
//        } else if (serverCard instanceof Stardust) {
//            return toStardustCard((Stardust) serverCard);
//        } else if (serverCard instanceof WarField) {
//            return toWarFieldCard((WarField) serverCard);
//        }

        throw new IllegalArgumentException("Unsupported card type: " + serverCard.getClass().getName());
    }

//    private static ClientPlanets toPlanetsCard(Planets serverCard) {
//        ClientPlanets clientCard = new ClientPlanets();
//        // Set common properties
//        setCommonProperties(clientCard, serverCard);
//
//        // Set Planets-specific properties
//        clientCard.setStepsBack(serverCard.getStepsBack());
//
//        // Convert server-side planets to client-side planets
//        List<ClientPlanet> clientPlanets = new ArrayList<>();
//        if (serverCard.getAvailablePlanets() != null) {
//            for (Planet serverPlanet : serverCard.getAvailablePlanets()) {
//                ClientPlanet clientPlanet = new ClientPlanet();
//                clientPlanet.setBusy(serverPlanet.isBusy());
//                if (serverPlanet.getReward() != null) {
//                    clientPlanet.setReward(new ArrayList<>(serverPlanet.getReward()));
//                } else {
//                    clientPlanet.setReward(new ArrayList<>());
//                }
//                clientPlanets.add(clientPlanet);
//            }
//        }
//        clientCard.setAvailablePlanets(clientPlanets);
//
//        return clientCard;
//    }

//    private static ClientAbandonedShip toAbandonedShipCard(AbandonedShip serverCard) {
//        ClientAbandonedShip clientCard = new ClientAbandonedShip();
//        setCommonProperties(clientCard, serverCard);
//
//        clientCard.setCrewMalus(serverCard.getCrewMalus());
//        clientCard.setReward(serverCard.getReward());
//        clientCard.setStepsBack(serverCard.getStepsBack());
//
//        return clientCard;
//    }

//    private static ClientAbandonedStation toAbandonedStationCard(AbandonedStation serverCard) {
//        ClientAbandonedStation clientCard = new ClientAbandonedStation();
//        setCommonProperties(clientCard, serverCard);
//
//        clientCard.setRequiredCrewMembers(serverCard.getRequiredCrewMembers());
//
//        // Handle null reward
//        if (serverCard.getReward() != null) {
//            clientCard.setReward(new ArrayList<>(serverCard.getReward()));
//        } else {
//            clientCard.setReward(new ArrayList<>());
//        }
//
//        clientCard.setStepsBack(serverCard.getStepsBack());
//
//        return clientCard;
//    }

//    private static ClientPirates toPiratesCard(Pirates serverCard) {
//        ClientPirates clientCard = new ClientPirates();
//        setCommonProperties(clientCard, serverCard);
//
//        clientCard.setRequiredFirePower(serverCard.getRequiredFirePower());
//        clientCard.setReward(serverCard.getReward());
//        clientCard.setStepsBack(serverCard.getStepsBack());
//
//        // Convert shots to client-side dangerous objects
//        if (serverCard.getShots() != null) {
//            List<ClientDangerousObject> clientShots = serverCard.getShots().stream()
//                    .map(AdventureCardMapper::toDangerousObject)
//                    .collect(Collectors.toList());
//            clientCard.setShots(clientShots);
//        } else {
//            clientCard.setShots(new ArrayList<>());
//        }
//
//        return clientCard;
//    }

//    private static ClientSlaveTraders toSlaveTradersCard(SlaveTraders serverCard) {
//        ClientSlaveTraders clientCard = new ClientSlaveTraders();
//        setCommonProperties(clientCard, serverCard);
//
//        clientCard.setRequiredFirePower(serverCard.getRequiredFirePower());
//        clientCard.setCrewMalus(serverCard.getCrewMalus());
//        clientCard.setReward(serverCard.getReward());
//        clientCard.setStepsBack(serverCard.getStepsBack());
//
//        return clientCard;
//    }

//    private static ClientSmugglers toSmugglersCard(Smugglers serverCard) {
//        ClientSmugglers clientCard = new ClientSmugglers();
//        setCommonProperties(clientCard, serverCard);
//
//        clientCard.setRequiredFirePower(serverCard.getRequiredFirePower());
//        clientCard.setCubeMalus(serverCard.getCubeMalus());
//
//        // Handle null reward
//        if (serverCard.getReward() != null) {
//            clientCard.setReward(new ArrayList<>(serverCard.getReward()));
//        } else {
//            clientCard.setReward(new ArrayList<>());
//        }
//
//        clientCard.setStepsBack(serverCard.getStepsBack());
//
//        return clientCard;
//    }

//    private static ClientMeteoriteStorm toMeteoriteStormCard(MeteoriteStorm serverCard) {
//        ClientMeteoriteStorm clientCard = new ClientMeteoriteStorm();
//        setCommonProperties(clientCard, serverCard);
//
//        // Convert meteorites to client-side dangerous objects
//        if (serverCard.getMeteorites() != null) {
//            List<ClientDangerousObject> clientMeteorites = serverCard.getMeteorites().stream()
//                    .map(AdventureCardMapper::toDangerousObject)
//                    .collect(Collectors.toList());
//            clientCard.setMeteorites(clientMeteorites);
//        } else {
//            clientCard.setMeteorites(new ArrayList<>());
//        }
//
//        return clientCard;
//    }

//    private static ClientFreeSpace toFreeSpaceCard(FreeSpace serverCard) {
//        ClientFreeSpace clientCard = new ClientFreeSpace();
//        setCommonProperties(clientCard, serverCard);
//        return clientCard;
//    }

//    private static ClientEpidemic toEpidemicCard(Epidemic serverCard) {
//        ClientEpidemic clientCard = new ClientEpidemic();
//        setCommonProperties(clientCard, serverCard);
//        return clientCard;
//    }

//    private static ClientStardust toStardustCard(Stardust serverCard) {
//        ClientStardust clientCard = new ClientStardust();
//        setCommonProperties(clientCard, serverCard);
//        return clientCard;
//    }

//    private static ClientWarField toWarFieldCard(WarField serverCard) {
//        ClientWarField clientCard = new ClientWarField();
//        setCommonProperties(clientCard, serverCard);
//
//        clientCard.setCrewMalus(serverCard.getCrewMalus());
//        clientCard.setCubeMalus(serverCard.getCubeMalus());
//        clientCard.setStepsBack(serverCard.getStepsBack());
//
//        // Convert shots to client-side dangerous objects
//        if (serverCard.getShots() != null) {
//            List<ClientDangerousObject> clientShots = serverCard.getShots().stream()
//                    .map(AdventureCardMapper::toDangerousObject)
//                    .collect(Collectors.toList());
//            clientCard.setShots(clientShots);
//        } else {
//            clientCard.setShots(new ArrayList<>());
//        }
//
//        return clientCard;
//    }

//    private static ClientDangerousObject toDangerousObject(DangerousObj serverObj) {
//        if (serverObj == null) {
//            return null;
//        }
//
//        ClientDangerousObject clientObj = new ClientDangerousObject();
//        clientObj.setDirection(serverObj.getDirection());
//        clientObj.setCoordinate(serverObj.getCoordinate());
//
//        // Set type based on the class
//        if (serverObj instanceof Meteorite) {
//            String size = serverObj.getClass().getSimpleName().contains("Big") ? "Big" : "Small";
//            clientObj.setType(size + "Meteorite");
//        } else if (serverObj instanceof Shot) {
//            String size = serverObj.getClass().getSimpleName().contains("Big") ? "Big" : "Small";
//            clientObj.setType(size + "Shot");
//        }
//
//        return clientObj;
//    }

    private static void setCommonProperties(ClientCard clientCard, AdventureCard serverCard) {
        clientCard.setCardName(serverCard.getCardName());
        clientCard.setImageName(serverCard.getImageName());

        // Handle null currState
        CardState currState = serverCard.getCurrState();
        if (currState != null) {
            clientCard.setCurrState(currState);
        }
    }
}