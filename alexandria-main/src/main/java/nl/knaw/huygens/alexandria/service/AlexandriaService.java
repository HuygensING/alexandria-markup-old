package nl.knaw.huygens.alexandria.service;

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

import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import nl.knaw.huygens.alexandria.api.model.AlexandriaState;
import nl.knaw.huygens.alexandria.api.model.Annotator;
import nl.knaw.huygens.alexandria.api.model.AnnotatorList;
import nl.knaw.huygens.alexandria.api.model.search.AlexandriaQuery;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotation;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotationList;
import nl.knaw.huygens.alexandria.api.model.text.view.TextView;
import nl.knaw.huygens.alexandria.api.model.text.view.TextViewDefinition;
import nl.knaw.huygens.alexandria.endpoint.search.SearchResult;
import nl.knaw.huygens.alexandria.model.Accountable;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.model.IdentifiablePointer;
import nl.knaw.huygens.alexandria.model.TentativeAlexandriaProvenance;
import nl.knaw.huygens.alexandria.textgraph.ParseResult;
import nl.knaw.huygens.alexandria.textgraph.TextAnnotation;
import nl.knaw.huygens.alexandria.textgraph.TextGraphSegment;
import nl.knaw.huygens.alexandria.textlocator.AlexandriaTextLocator;

public interface AlexandriaService {
  // NOTE: should these service methods all be atomic?

  /**
   * @return true if the resource was created, false if it was updated
   */
  boolean createOrUpdateResource(UUID uuid, String ref, TentativeAlexandriaProvenance provenance, AlexandriaState alexandriaState);

  AlexandriaResource createResource(UUID resourceUUID, String resourceRef, TentativeAlexandriaProvenance provenance, AlexandriaState confirmed);

  AlexandriaResource createSubResource(UUID uuid, UUID parentUuid, String sub, TentativeAlexandriaProvenance provenance);

  Optional<AlexandriaResource> readResource(UUID uuid);

  Optional<AlexandriaResource> readResourceWithUniqueRef(String resourceRef);

  List<AlexandriaResource> readSubResources(UUID uuid);

  Optional<? extends Accountable> dereference(IdentifiablePointer<? extends Accountable> pointer);

  /**
   * remove all unconfirmed objects that have timed out
   */
  void removeExpiredTentatives();

  TemporalAmount getTentativesTimeToLive();


  void confirmResource(UUID id);


  SearchResult execute(AlexandriaQuery query);

  // TODO: refactor these find methods to something more generic (search)
  Optional<AlexandriaResource> findSubresourceWithSubAndParentId(String sub, UUID parentId);

  Map<String, Object> getMetadata();

  void destroy();

  void exportDb(String format, String filename);

  void importDb(String format, String filename);

  void setResourceAnnotator(UUID resourceUUID, Annotator annotator);

  Optional<Annotator> readResourceAnnotator(UUID resourceUUID, String annotatorCode);

  AnnotatorList readResourceAnnotators(UUID id);

  void setTextRangeAnnotation(UUID resourceUUID, TextRangeAnnotation annotation);

  TextRangeAnnotationList readTextRangeAnnotations(UUID resourceUUID);

  void deprecateTextRangeAnnotation(UUID annotationUUID, TextRangeAnnotation newTextRangeAnnotation);

  Optional<TextRangeAnnotation> readTextRangeAnnotation(UUID resourceUUID, UUID annotationUUID);

  Optional<TextRangeAnnotation> readTextRangeAnnotation(UUID resourceUUID, UUID annotationUUID, Integer revision);

  boolean nonNestingOverlapWithExistingTextRangeAnnotationForResource(TextRangeAnnotation annotation, UUID resourceUUID);

  void setTextView(UUID resourceUUID, String viewId, TextView textView, TextViewDefinition textViewDefinition);

  Optional<TextViewDefinition> getTextViewDefinition(UUID resourceId, String viewId);

  Optional<TextView> getTextView(UUID resourceId, String view);

  /**
   * Gets the textviews for the resource and all its ancestors
   */
  List<TextView> getTextViewsForResource(UUID resourceUUID);

  boolean storeTextGraph(UUID resourceId, ParseResult result);

  Stream<TextGraphSegment> getTextGraphSegmentStream(UUID resourceId, List<List<String>> orderedLayerTags);

  void runInTransaction(Runnable runner);

  <A> A runInTransaction(Supplier<A> supplier);

  Stream<TextAnnotation> getTextAnnotationStream(UUID resourceId);

  void updateTextAnnotation(TextAnnotation textAnnotation);

  void wrapContentInChildTextAnnotation(TextAnnotation existingTextAnnotation, TextAnnotation newChildTextAnnotation);


}
