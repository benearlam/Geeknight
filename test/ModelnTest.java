import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.util.FileUtils;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ModelnTest {
    final String graphName = "graphModelN.db";
    GraphDatabaseService graphDatabaseService;

    @Before
    public void setUp() throws Exception {
        FileUtils.deleteRecursively(new File(graphName));
        graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(graphName);
    }

    @Test
    public void shouldFindTheQuickestPathBetweenFirswoodAndMediaCity() throws Exception {
        createRouteTrip("firswood 2 old-trafford 4 cornbrook 8 city-centre", "route1");
        createRouteTrip("deansgate 2 cornbrook 3 city-centre", "route2");

        Index<Node> tramStations = graphDatabaseService.index().forNodes("tramStations");


        PathFinder<WeightedPath> duration = GraphAlgoFactory.dijkstra(Traversal.expanderForAllTypes(),
                CommonEvaluators.doubleCostEvaluator("duration"));
        WeightedPath singlePath = duration.findSinglePath(
                tramStations.get("name", "firswood").getSingle(),
                tramStations.get("name", "city-centre").getSingle());


        System.out.print(singlePath.toString());
        assertThat(singlePath.weight(), is(9d));
    }

    private void createRouteTrip(String description, String routeId) {
        Transaction tx = graphDatabaseService.beginTx();
        Node startStationRoute = null;

        try {
            String[] split = description.split(" ");
            for (int i = 0; i + 2 < split.length; i += 2) {
                Node tram1 = createTramStation(split[i]);
                Node tram2 = createTramStation(split[i + 2]);
                if (startStationRoute == null) {
                    startStationRoute = createStationRoute(tram1, routeId);
                }
                Node nextStationRoute = createStationRoute(tram2, routeId);
                int duration = Integer.parseInt(split[i + 1]);
                Relationship relationshipTo = startStationRoute.createRelationshipTo(nextStationRoute, TransportRelationshipTypes.GOES_TO);
                relationshipTo.setProperty("duration", duration);
                startStationRoute = nextStationRoute;
            }
            tx.success();

        } finally {
            tx.finish();
        }
    }

    private Node createTramStation(String stationName) {
        Index<Node> tramStations = graphDatabaseService.index().forNodes("tramStations");
        Node node = tramStations.get("name", stationName).getSingle();
        if (node == null) {
            node = graphDatabaseService.createNode();
            node.setProperty("name", stationName);
            tramStations.add(node, "name", stationName);
        }

        return node;
    }

    private Node createStationRoute(Node station, String routeId) {
        String name = station.getProperty("name") + routeId;
        Node stationRouteNode = graphDatabaseService.createNode();
        stationRouteNode.setProperty("name", name);
        createBoardDepartRelationships(station, stationRouteNode);
        return stationRouteNode;
    }

    private void createBoardDepartRelationships(Node station, Node stationRoute){
        Relationship board = station.createRelationshipTo(stationRoute, TransportRelationshipTypes.BOARD);
        board.setProperty("duration", 0);
        Relationship depart = stationRoute.createRelationshipTo(station, TransportRelationshipTypes.DEPART);
        depart.setProperty("duration", 5);
    }
}
