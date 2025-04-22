package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CabinTest {
    private Cabin cabin;

    @BeforeEach
    void setUp() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.EMPTY);
        connectors.put(Direction.WEST, ConnectorType.EMPTY);

        cabin = new Cabin(connectors);
    }

    @Test
    void fillCabinWithHuman(){
        cabin.fillCabin(CrewMember.HUMAN);

        List<CrewMember> inhabitansExpected= new ArrayList<>();
        inhabitansExpected.add(CrewMember.HUMAN);
        inhabitansExpected.add(CrewMember.HUMAN);

        assertEquals(inhabitansExpected,cabin.getInhabitants(),"inhabitants");
    }

    @Test
    void fillCabinWithAlien(){
        cabin.fillCabin(CrewMember.BROWN_ALIEN);

        List<CrewMember> inhabitansExpected= new ArrayList<>();
        inhabitansExpected.add(CrewMember.BROWN_ALIEN);

        assertEquals(inhabitansExpected,cabin.getInhabitants(),"inhabitants");
    }

    @Test
    void removeMember(){

        cabin.fillCabin(CrewMember.HUMAN);
        cabin.removeMember();

        assertEquals(CrewMember.HUMAN, cabin.getInhabitants().getFirst(),"inhabitants");

        cabin.removeMember();
        Exception exception= assertThrows(NoSuchElementException.class, ()->cabin.removeMember());
        assertEquals(exception.getMessage(),"Empty cabin");

    }

    @Test
    void hasInhabitants(){
        cabin.fillCabin(CrewMember.HUMAN);
        assertTrue(cabin.hasInhabitants(),"inhabitants");
        cabin.removeMember();
        cabin.removeMember();
        assertFalse(cabin.hasInhabitants(),"inhabitants");
    }


}