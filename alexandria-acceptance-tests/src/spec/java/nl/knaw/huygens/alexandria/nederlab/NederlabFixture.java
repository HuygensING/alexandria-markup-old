package nl.knaw.huygens.alexandria.nederlab;

import java.time.Instant;
import java.util.UUID;

import nl.knaw.huygens.alexandria.concordion.AlexandriaAcceptanceTest;
import nl.knaw.huygens.alexandria.endpoint.annotation.AnnotationsEndpoint;
import nl.knaw.huygens.alexandria.endpoint.resource.ResourcesEndpoint;
import nl.knaw.huygens.alexandria.model.AlexandriaState;
import nl.knaw.huygens.alexandria.model.TentativeAlexandriaProvenance;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(ConcordionRunner.class)
public class NederlabFixture extends AlexandriaAcceptanceTest {
  @BeforeClass
  public static void registerEndpoints() {
    register(ResourcesEndpoint.class);
    register(AnnotationsEndpoint.class);
  }

  public void resourceExists(String id) {
    UUID uuid = UUID.fromString(id);
    service().createOrUpdateResource(uuid, aRef(), aProvenance(), AlexandriaState.CONFIRMED);
  }

  private TentativeAlexandriaProvenance aProvenance() {
    return new TentativeAlexandriaProvenance("who", Instant.now(), "why");
  }

  private String aRef() {
    return "http://www.example.com/some/ref";
  }

}