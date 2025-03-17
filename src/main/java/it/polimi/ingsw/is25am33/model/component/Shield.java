package it.polimi.ingsw.is25am33.model.component;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import java.util.List;
import java.util.Map;

public class Shield extends Component implements Activable, Rotatable {
        private List<Direction> direction;
        public Shield(Map<Direction, ConnectorType> connectors,List<Direction> direction) {
            super(connectors);
            this.direction=direction;
        }

        public List<Direction> getDirections() {
            return direction;
        }

        public void setDirection() {
            for(int i=0; i<getRotation()%4;i++){
                for(int j=0; j<2; j++)
                    this.direction.set(j,shiftDirection(this.direction.get(j)));
            }
        }

    }


