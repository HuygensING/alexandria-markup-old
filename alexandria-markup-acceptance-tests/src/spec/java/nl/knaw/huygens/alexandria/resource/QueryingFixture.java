package nl.knaw.huygens.alexandria.resource;

/*
 * #%L
 * alexandria-acceptance-tests
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

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import nl.knaw.huygens.alexandria.exception.NotFoundException;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.model.TentativeAlexandriaProvenance;

@RunWith(ConcordionRunner.class)
public class QueryingFixture extends ResourcesBase {
  private AlexandriaResource resource;

  public void existingResource(String id) {
    UUID uuid = UUID.fromString(id);
    TentativeAlexandriaProvenance provenance = new TentativeAlexandriaProvenance("who", Instant.now(), "why");
    resource = new AlexandriaResource(uuid, provenance);
    Optional<AlexandriaResource> optional = Optional.of(resource);
    when(service().readResource(uuid)).thenReturn(optional);
  }

  public void withReference(String reference) {
    resource.setCargo(reference);
  }

  public void noSuchResource(String id) {
    UUID uuid = UUID.fromString(id);
    when(service().readResource(uuid)).thenThrow(new NotFoundException());
  }
}
