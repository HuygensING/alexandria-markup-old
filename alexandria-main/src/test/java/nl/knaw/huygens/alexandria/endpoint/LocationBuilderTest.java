package nl.knaw.huygens.alexandria.endpoint;

/*
 * #%L
 * alexandria-main
 * =======
 * Copyright (C) 2015 - 2017 Huygens ING (KNAW)
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.UUID;

import org.junit.Test;

import nl.knaw.huygens.alexandria.api.EndpointPaths;
import nl.knaw.huygens.alexandria.config.MockConfiguration;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.model.TentativeAlexandriaProvenance;

public class LocationBuilderTest {
  LocationBuilder lb = new LocationBuilder(new MockConfiguration(), new EndpointPathResolver());

  @Test
  public void testGetLocationOfAlexandriaResourceWithUUID() {
    UUID randomUUID = UUID.randomUUID();
    TentativeAlexandriaProvenance provenance = mock(TentativeAlexandriaProvenance.class);
    AlexandriaResource resource = new AlexandriaResource(randomUUID, provenance);

    URI locationOf = lb.locationOf(resource);

    assertThat(locationOf.toString()).isEqualTo("http://alexandria.eg/resources/" + randomUUID);
  }
}
