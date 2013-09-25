import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.util.FileUtils;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Model1Test {
    final String graphName = "graphModel1.db";

    @Before
    public void setUp() throws Exception {
        FileUtils.deleteRecursively(new File(graphName));
    }

    @Test
    /*
        firswood -1-> old trafford -2-> cornbrook -4-> city centre --
                                          |                          |
                                          |                          /
                                           -8-> media city <-16---- /
     */
    public void shouldFindTheQuickestPathBetweenFirswoodAndMediaCity() throws Exception {
        GraphDatabaseService graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(graphName);
        buildGraph(graphDatabaseService);
        Index<Node> tramStations = graphDatabaseService.index().forNodes("tramStations");


        PathFinder<WeightedPath> duration = GraphAlgoFactory.dijkstra(Traversal.expanderForAllTypes(), CommonEvaluators.doubleCostEvaluator("duration"));
        WeightedPath singlePath = duration.findSinglePath(
                tramStations.get("name", "firswood").getSingle(),
                tramStations.get("name", "media city").getSingle());


        System.out.print(singlePath.toString());
        assertThat(singlePath.weight(), is(11d));
    }

    private void buildGraph(GraphDatabaseService graphDatabaseService) {
        Index<Node> tramStations;
        Transaction tx = graphDatabaseService.beginTx();
        try {
            tramStations = graphDatabaseService.index().forNodes("tramStations");

            Node firswood = graphDatabaseService.createNode();
            firswood.setProperty("name", "firswood");

            Node oldTrafford = graphDatabaseService.createNode();
            oldTrafford.setProperty("name", "old trafford");

            Node cornbrook = graphDatabaseService.createNode();
            cornbrook.setProperty("name", "cornbrook");

            Node cityCentre = graphDatabaseService.createNode();
            cityCentre.setProperty("name", "city centre");

            Node mediaCity = graphDatabaseService.createNode();
            mediaCity.setProperty("name", "media city");

            tramStations.add(firswood, "name", firswood.getProperty("name"));
            tramStations.add(oldTrafford, "name", oldTrafford.getProperty("name"));
            tramStations.add(cornbrook, "name", cornbrook.getProperty("name"));
            tramStations.add(cityCentre, "name", cityCentre.getProperty("name"));
            tramStations.add(mediaCity, "name", mediaCity.getProperty("name"));

            Relationship goesToRelationship = firswood.createRelationshipTo(oldTrafford, TransportRelationshipTypes.GOES_TO);
            goesToRelationship.setProperty("duration", 1);

            goesToRelationship = oldTrafford.createRelationshipTo(cornbrook, TransportRelationshipTypes.GOES_TO);
            goesToRelationship.setProperty("duration", 2);

            goesToRelationship = cornbrook.createRelationshipTo(cityCentre, TransportRelationshipTypes.GOES_TO);
            goesToRelationship.setProperty("duration", 4);

            goesToRelationship = cornbrook.createRelationshipTo(mediaCity, TransportRelationshipTypes.GOES_TO);
            goesToRelationship.setProperty("duration", 8);

            goesToRelationship = cityCentre.createRelationshipTo(mediaCity, TransportRelationshipTypes.GOES_TO);
            goesToRelationship.setProperty("duration", 16);

            tx.success();

        } finally {
            tx.finish();
        }
    }
}

