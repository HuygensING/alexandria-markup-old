package nl.knaw.huygens.alexandria.endpoint.annotation;

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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.endpoint.LocationBuilder;
import nl.knaw.huygens.alexandria.model.AlexandriaAnnotation;

public class AnnotationEntityBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(AnnotationEntityBuilder.class);
  private LocationBuilder locationBuilder;

  @Inject
  public AnnotationEntityBuilder(LocationBuilder locationBuilder) {
    LOG.trace("AnnotationEntityBuilder created: locationBuilder=[{}]", locationBuilder);
    this.locationBuilder = locationBuilder;
  }

  public AnnotationEntity build(AlexandriaAnnotation annotation) {
    return AnnotationEntity.of(annotation).withLocationBuilder(locationBuilder);
  }

}
