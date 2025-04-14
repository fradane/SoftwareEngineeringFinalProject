package it.polimi.ingsw.is25am33.model.card;

class MeteoriteStormTest {

//    private GameModel gameModel;
//    private MeteoriteStorm card;
//
//    private Meteorite big;
//    private Meteorite small;
//
//    Player player1 = new Player("Alice");
//    Player player2 = new Player("Bob");
//    Player player3 = new Player("Charlie");
//    Player player4 = new Player("Dana");
//
//    @BeforeEach
//    void setUp() {
//
//        List<Player> players = List.of(player1, player2, player3, player4);
//        gameModel = new GameModel(new Level2FlyingBoard(21), players);
//        gameModel.setCurrRanking(players);
//        big = new BigMeteorite(Direction.NORTH);
//        small = new SmallMeteorite(Direction.SOUTH);
//
//        List<Meteorite> meteorites = new ArrayList<>();
//        meteorites.add(small);
//        meteorites.add(big);
//
//        card = new MeteoriteStorm(meteorites, gameModel);
//        gameModel.setCurrState(GameState.START_CARD);
//        gameModel.setCurrAdventureCard(card);
//        gameModel.startCard();
//
//    }
//
//    @Test
//    void testGetFirstState() {
//        assertEquals(GameState.THROW_DICES, card.getFirstState());
//    }
//
//    @Test
//    void testPlayThrowDicesChangesState() throws UnknownStateException {
//        card.play(null);
//        assertEquals(GameState.DANGEROUS_ATTACK, card.currState);
//        assertEquals(big, gameModel.getCurrDangerousObj());
//    }
//
//    @Test
//    void testUnknownStateThrows() {
//        gameModel.setCurrState(GameState.END_OF_CARD);
//        card.currState = GameState.END_OF_CARD;
//        assertThrows(UnknownStateException.class, () -> card.play(null));
//    }
//
//
//    public class TestShipBoard1 extends ShipBoard {
//
//        public void handleDangerousObject(DangerousObj obj) {
//            System.out.println("");
//        }
//
//        @Override
//        public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
//            return false;
//        }
//
//        public boolean isExposed(int pos, Direction direction) throws IllegalArgumentException {
//            return true;
//        }
//
//        public boolean isItGoingToHitTheShip(DangerousObj obj) {
//            return true;
//        }
//
//    }
//
//    static class TestShipBoard2 extends ShipBoard {
//
//        @Override
//        public void handleDangerousObject(DangerousObj obj) {
//
//        }
//
//        @Override
//        public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
//            return false;
//        }
//
//    }


}