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

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import nl.knaw.huygens.alexandria.api.model.Entity;
import nl.knaw.huygens.alexandria.api.model.JsonWrapperObject;
import nl.knaw.huygens.alexandria.api.model.PropertyPrefix;
import nl.knaw.huygens.alexandria.model.AbstractAccountable;

public abstract class AbstractAccountableEntity extends JsonWrapperObject implements Entity {

  @JsonIgnore
  protected LocationBuilder locationBuilder;

  abstract protected AbstractAccountable getAccountable();

  @JsonProperty(PropertyPrefix.LINK + "provenance")
  public URI getProvenance() {
    return locationBuilder.locationOf(getAccountable(), "provenance");
  }

  public UUID getId() {
    return getAccountable().getId();
  }

  public Map<String, Object> getState() {
    return ImmutableMap.of(//
        "value", getAccountable().getState(), //
        "since", getAccountable().getStateSince().toString()//
    );
  }
}
