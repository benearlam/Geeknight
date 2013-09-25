import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Node;

public enum TransportRelationshipTypes implements RelationshipType {
    GOES_TO, BOARD, DEPART;

    public void setGoesTo(Node from, Node to, int duration) {
        Relationship goesTo = from.createRelationshipTo(to, GOES_TO);
        goesTo.setProperty("duration", duration);
    }
}
