package nl.knaw.huygens.alexandria.service;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jooq.lambda.Unchecked;

/*
 * #%L
 * alexandria-service
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import nl.knaw.huygens.alexandria.api.model.AlexandriaState;
import nl.knaw.huygens.alexandria.api.model.Annotator;
import nl.knaw.huygens.alexandria.api.model.AnnotatorList;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotation;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotation.AbsolutePosition;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotation.Position;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotationList;
import nl.knaw.huygens.alexandria.api.model.text.view.TextView;
import nl.knaw.huygens.alexandria.api.model.text.view.TextViewDefinition;
import nl.knaw.huygens.alexandria.endpoint.LocationBuilder;
import nl.knaw.huygens.alexandria.exception.BadRequestException;
import nl.knaw.huygens.alexandria.exception.NotFoundException;
import nl.knaw.huygens.alexandria.model.Accountable;
import nl.knaw.huygens.alexandria.model.AlexandriaProvenance;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.model.IdentifiablePointer;
import nl.knaw.huygens.alexandria.model.TentativeAlexandriaProvenance;
import nl.knaw.huygens.alexandria.storage.DumpFormat;
import nl.knaw.huygens.alexandria.storage.Storage;
import nl.knaw.huygens.alexandria.storage.frames.AlexandriaVF;
import nl.knaw.huygens.alexandria.storage.frames.AnnotatorVF;
import nl.knaw.huygens.alexandria.storage.frames.AnnotatorVF.EdgeLabels;
import nl.knaw.huygens.alexandria.storage.frames.IdentifiableVF;
import nl.knaw.huygens.alexandria.storage.frames.ResourceVF;
import nl.knaw.huygens.alexandria.storage.frames.TextRangeAnnotationVF;
import nl.knaw.huygens.alexandria.textgraph.ParseResult;
import nl.knaw.huygens.alexandria.textgraph.TextAnnotation;
import nl.knaw.huygens.alexandria.textgraph.TextGraphSegment;
import nl.knaw.huygens.alexandria.util.StreamUtil;
import peapod.FramedGraphTraversal;

@Singleton
public class TinkerPopService implements AlexandriaService {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

  private static final TypeReference<Map<String, TextView>> TEXTVIEW_TYPEREF = new TypeReference<Map<String, TextView>>() {
  };
  private static final ObjectReader TEXTVIEW_READER = OBJECT_MAPPER.readerFor(TEXTVIEW_TYPEREF);
  private static final ObjectWriter TEXTVIEW_WRITER = OBJECT_MAPPER.writerFor(TEXTVIEW_TYPEREF);

  private static final TypeReference<Map<String, TextViewDefinition>> TEXTVIEWDEFINITION_TYPEREF = new TypeReference<Map<String, TextViewDefinition>>() {
  };
  private static final ObjectReader TEXTVIEWDEFINITION_READER = OBJECT_MAPPER.readerFor(TEXTVIEWDEFINITION_TYPEREF);
  private static final ObjectWriter TEXTVIEWDEFINITION_WRITER = OBJECT_MAPPER.writerFor(TEXTVIEWDEFINITION_TYPEREF);

  private static final TemporalAmount TENTATIVES_TTL = Duration.ofDays(1);

  private Storage storage;
  private LocationBuilder locationBuilder;
  private TextGraphService textGraphService;

  @Inject
  public TinkerPopService(Storage storage, LocationBuilder locationBuilder) {
    // Log.trace("{} created, locationBuilder=[{}]", getClass().getSimpleName(), locationBuilder);
    this.locationBuilder = locationBuilder;
    setStorage(storage);
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
    this.textGraphService = new TextGraphService(storage);
  }

  // - AlexandriaService methods -//
  // use storage.runInTransaction for transactions

  @Override
  public boolean createOrUpdateResource(UUID uuid, String ref, TentativeAlexandriaProvenance provenance, AlexandriaState state) {
    return storage.runInTransaction(() -> {
      AlexandriaResource resource;
      boolean result;

      if (storage.existsVF(ResourceVF.class, uuid)) {
        resource = getOptionalResource(uuid).get();
        result = false;

      } else {
        resource = new AlexandriaResource(uuid, provenance);
        result = true;
      }
      resource.setCargo(ref);
      resource.setState(state);
      createOrUpdateResource(resource);
      return result;
    });
  }

  @Override
  public AlexandriaResource createResource(UUID resourceUUID, String ref, TentativeAlexandriaProvenance provenance, AlexandriaState state) {
    return storage.runInTransaction(() -> {
      AlexandriaResource resource = new AlexandriaResource(resourceUUID, provenance);
      resource.setCargo(ref);
      resource.setState(state);
      createOrUpdateResource(resource);
      return resource;
    });
  }

  private Optional<AlexandriaResource> getOptionalResource(UUID uuid) {
    return storage.readVF(ResourceVF.class, uuid).map(this::deframeResource);
  }

  private Optional<AlexandriaResource> getOptionalResourceWithUniqueRef(String ref) {
    FramedGraphTraversal<Object, ResourceVF> traversal = storage.find(ResourceVF.class).has(ResourceVF.Properties.CARGO, ref);
    AlexandriaResource alexandriaResource = traversal.hasNext() ? deframeResourceLite(traversal.next()) : null;
    return Optional.ofNullable(alexandriaResource);
  }

  @Override
  public AlexandriaResource createSubResource(UUID uuid, UUID parentUuid, String sub, TentativeAlexandriaProvenance provenance) {
    AlexandriaResource subresource = new AlexandriaResource(uuid, provenance);
    subresource.setCargo(sub);
    subresource.setParentResourcePointer(new IdentifiablePointer<>(AlexandriaResource.class, parentUuid.toString()));
    createSubResource(subresource);
    return subresource;
  }

  @Override
  public Optional<? extends Accountable> dereference(IdentifiablePointer<? extends Accountable> pointer) {
    Class<? extends Accountable> aClass = pointer.getIdentifiableClass();
    UUID uuid = UUID.fromString(pointer.getIdentifier());
    if (AlexandriaResource.class.equals(aClass)) {
      return readResource(uuid);

    } else {
      throw new RuntimeException("unexpected accountableClass: " + aClass.getName());
    }
  }

  @Override
  public Optional<AlexandriaResource> readResource(UUID uuid) {
    return storage.runInTransaction(() -> getOptionalResource(uuid));
  }

  @Override
  public Optional<AlexandriaResource> readResourceWithUniqueRef(String resourceRef) {
    return storage.runInTransaction(() -> getOptionalResourceWithUniqueRef(resourceRef));
  }

  @Override
  public TemporalAmount getTentativesTimeToLive() {
    return TENTATIVES_TTL;
  }

  @Override
  public void removeExpiredTentatives() {
    // Tentative vertices should not have any outgoing or incoming edges!!
    Long threshold = Instant.now().minus(TENTATIVES_TTL).getEpochSecond();
    storage.runInTransaction(() -> storage.removeExpiredTentatives(threshold));
  }

  @Override
  public Optional<AlexandriaResource> findSubresourceWithSubAndParentId(String sub, UUID parentId) {
    return storage.runInTransaction(//
        () -> storage.getResourceVertexTraversal()//
            .has(Storage.IDENTIFIER_PROPERTY, parentId.toString())//
            .in(ResourceVF.EdgeLabels.PART_OF)//
            .has(ResourceVF.Properties.CARGO, sub)//
            .toList()//
            .stream()//
            .map(this::deframeResource)//
            .findAny()//
    );
  }

  @Override
  public List<AlexandriaResource> readSubResources(UUID uuid) {
    ResourceVF resourcevf = readExistingResourceVF(uuid);
    return resourcevf.getSubResources().stream()//
        .map(this::deframeResource)//
        .sorted()//
        .collect(toList());
  }

  @Override
  public void setResourceAnnotator(UUID resourceUUID, Annotator annotator) {
    storage.runInTransaction(() -> {
      // remove existing annotator for this resource with the same annotator code
      storage.getResourceVertexTraversal()//
          .has(Storage.IDENTIFIER_PROPERTY, resourceUUID.toString())//
          .in(EdgeLabels.HAS_RESOURCE)//
          .has("code", annotator.getCode())//
          .toList()//
          .forEach(Vertex::remove);
      AnnotatorVF avf = frameAnnotator(annotator);
      ResourceVF resourceVF = storage.readVF(ResourceVF.class, resourceUUID).get();
      avf.setResource(resourceVF);
      // Log.info("avf.resource={}", avf.getResource().getUuid());
    });

  }

  @Override
  public Optional<Annotator> readResourceAnnotator(UUID uuid, String code) {
    ResourceVF resourcevf = readExistingResourceVF(uuid);
    return resourcevf.getAnnotators().stream()//
        .map(this::deframeAnnotator)//
        .filter(a -> code.equals(a.getCode()))//
        .findAny();
  }

  @Override
  public AnnotatorList readResourceAnnotators(UUID uuid) {
    List<AnnotatorVF> annotatorVFs = storage.runInTransaction(() -> {
      ResourceVF resourceVF = readExistingResourceVF(uuid);
      List<AnnotatorVF> annotatorVFList = Lists.newArrayList();
      do {
        annotatorVFList.addAll(resourceVF.getAnnotators());
        resourceVF = resourceVF.getParentResource();
      } while (resourceVF != null);

      return annotatorVFList;
    });
    AnnotatorList annotators = new AnnotatorList();
    Set<String> codes = Sets.newHashSet();
    annotatorVFs.stream().map(this::deframeAnnotator)//
        .forEach(a -> {
          if (!codes.contains(a.getCode())) {
            codes.add(a.getCode());
            annotators.add(a);
          }
        });
    return annotators;
  }

  @Override
  public void setTextRangeAnnotation(UUID resourceUUID, TextRangeAnnotation annotation) {
    storage.runInTransaction(() -> {
      TextRangeAnnotationVF vf = storage.readVF(TextRangeAnnotationVF.class, annotation.getId())//
          .orElseGet(() -> storage.createVF(TextRangeAnnotationVF.class));
      updateTextRangeAnnotation(vf, annotation);
      textGraphService.updateTextAnnotationLink(vf, annotation, resourceUUID);
      vf.setResource(storage.readVF(ResourceVF.class, resourceUUID).get());
    });
  }

  @Override
  public TextRangeAnnotationList readTextRangeAnnotations(UUID resourceUUID) {
    return storage.runInTransaction(() -> getTextRangeAnnotationList(resourceUUID));
  }

  @Override
  public void deprecateTextRangeAnnotation(UUID annotationUUID, TextRangeAnnotation newTextRangeAnnotation) {
    storage.runInTransaction(() -> {
      TextRangeAnnotationVF oldVF = storage.readVF(TextRangeAnnotationVF.class, annotationUUID)//
          .orElseThrow(NotFoundException::new);
      Integer revision = oldVF.getRevision();
      oldVF.setUuid(annotationUUID.toString() + "." + revision);
      ResourceVF resourceVF = oldVF.getResource();
      oldVF.setResource(null);

      removeTextAnnotationFromChain(oldVF);

      TextRangeAnnotationVF newVF = storage.createVF(TextRangeAnnotationVF.class);

      newTextRangeAnnotation.setRevision(revision + 1);
      updateTextRangeAnnotation(newVF, newTextRangeAnnotation);
      UUID resourceUUID = UUID.fromString(resourceVF.getUuid());
      textGraphService.updateTextAnnotationLink(newVF, newTextRangeAnnotation, resourceUUID);
      newVF.setResource(resourceVF);
      newVF.setDeprecatedAnnotation(oldVF);
    });
  }

  private void removeTextAnnotationFromChain(TextRangeAnnotationVF oldVF) {
    FramedGraphTraversal<TextRangeAnnotationVF, Vertex> traversal = oldVF.out(TextRangeAnnotationVF.EdgeLabels.HAS_TEXTANNOTATION);

    // remove the old textAnnotationVertex without breaking the chain.
    if (traversal.hasNext()) {
      Vertex textAnnotationVertex = traversal.next();
      Vertex leftVertex = null; // in the annotation chain, the vertex to the left of this textAnnotationVertex; there is always a left vertex
      String leftEdgeLabel = null;
      Vertex rightVertex = null; // in the annotation chain, the vertex to the right of this textAnnotationVertex; there might not be a right vertex
      // it might be the first in the chain, so it has an incoming FIRST_ANNOTATION edge
      Iterator<Edge> incomingFirstAnnotationEdgeIterator = textAnnotationVertex.edges(Direction.IN, nl.knaw.huygens.alexandria.storage.EdgeLabels.FIRST_ANNOTATION);
      if (incomingFirstAnnotationEdgeIterator.hasNext()) {
        // in that case, remove the edge, and reconnect the chain.
        Edge incomingEdge = incomingFirstAnnotationEdgeIterator.next();
        leftVertex = incomingEdge.outVertex();
        leftEdgeLabel = nl.knaw.huygens.alexandria.storage.EdgeLabels.FIRST_ANNOTATION;
        incomingEdge.remove();

      } else {
        // otherwise, it's a NEXT edge
        Iterator<Edge> incomingNextEdgeIterator = textAnnotationVertex.edges(Direction.IN, nl.knaw.huygens.alexandria.storage.EdgeLabels.NEXT);
        if (incomingNextEdgeIterator.hasNext()) {
          Edge incomingNextEdge = incomingNextEdgeIterator.next();
          leftVertex = incomingNextEdge.outVertex();
          incomingNextEdge.remove();
        }
        leftEdgeLabel = nl.knaw.huygens.alexandria.storage.EdgeLabels.NEXT;
      }

      Iterator<Edge> outgoingNextEdgeIterator = textAnnotationVertex.edges(Direction.OUT, nl.knaw.huygens.alexandria.storage.EdgeLabels.NEXT);
      if (outgoingNextEdgeIterator.hasNext()) {
        Edge outgoingNextEdge = outgoingNextEdgeIterator.next();
        rightVertex = outgoingNextEdge.inVertex();
        outgoingNextEdge.remove();
      }

      if (rightVertex != null) {
        leftVertex.addEdge(leftEdgeLabel, rightVertex);
      }

      textAnnotationVertex.remove();
    }
  }

  private TextRangeAnnotationList getTextRangeAnnotationList(UUID resourceUUID) {
    TextRangeAnnotationList list = new TextRangeAnnotationList();
    storage.readVF(ResourceVF.class, resourceUUID).ifPresent(resourceVF -> {
      FramedGraphTraversal<ResourceVF, Vertex> traversal = resourceVF.start()//
          .in(TextRangeAnnotationVF.EdgeLabels.HAS_RESOURCE)//
      ;
      StreamUtil.stream(traversal)//
          .map(v -> storage.frameVertex(v, TextRangeAnnotationVF.class))//
          .map(this::deframeTextRangeAnnotation)//
          .forEach(list::add);
    });
    return list;
  }

  @Override
  public Optional<TextRangeAnnotation> readTextRangeAnnotation(UUID resourceUUID, UUID annotationUUID) {
    return storage.runInTransaction(() -> getOptionalTextRangeAnnotation(resourceUUID, annotationUUID));
  }

  @Override
  public Optional<TextRangeAnnotation> readTextRangeAnnotation(UUID resourceUUID, UUID annotationUUID, Integer revision) {
    return storage.runInTransaction(() -> {
      Optional<TextRangeAnnotationVF> versionedAnnotation = storage.readVF(TextRangeAnnotationVF.class, annotationUUID, revision);
      if (versionedAnnotation.isPresent()) {
        return versionedAnnotation.map(this::deframeTextRangeAnnotation);

      } else {
        Optional<TextRangeAnnotationVF> currentAnnotation = storage.readVF(TextRangeAnnotationVF.class, annotationUUID);
        if (currentAnnotation.isPresent() && currentAnnotation.get().getRevision().equals(revision)) {
          return currentAnnotation.map(this::deframeTextRangeAnnotation);
        } else {
          return Optional.empty();
        }
      }
    });
  }

  private Optional<TextRangeAnnotation> getOptionalTextRangeAnnotation(UUID resourceUUID, UUID annotationUUID) {
    return storage.readVF(TextRangeAnnotationVF.class, annotationUUID).map(this::deframeTextRangeAnnotation);
  }

  @Override
  public boolean nonNestingOverlapWithExistingTextRangeAnnotationForResource(TextRangeAnnotation annotation, UUID resourceUUID) {
    return storage.runInTransaction(() -> {
      AtomicBoolean overlaps = new AtomicBoolean(false);
      storage.readVF(ResourceVF.class, resourceUUID).ifPresent(resourceVF -> {
        FramedGraphTraversal<ResourceVF, Vertex> traversal = resourceVF.start()//
            .in(TextRangeAnnotationVF.EdgeLabels.HAS_RESOURCE)//
            .has("name", annotation.getName())//
            .has("annotatorCode", annotation.getAnnotator())//
        ;
        AbsolutePosition absolutePosition = annotation.getAbsolutePosition();
        String uuid1 = annotation.getId().toString();
        String xmlId1 = absolutePosition.getXmlId();
        Integer start1 = absolutePosition.getOffset();
        Integer end1 = start1 + absolutePosition.getLength();
        Predicate<Vertex> nonNestingOverlapWithAnnotation = t -> {
          String xmlId2 = (String) t.property("absoluteXmlId").value();
          Integer start2 = (Integer) t.property("absoluteOffset").value();
          Integer end2 = start2 + (Integer) t.property("absoluteLength").value();
          return xmlId1.equals(xmlId2)//
              && (start1 < end2 && start2 < end1) // annotation overlaps with existing annotation t
              && !((start1 <= start2 && start2 <= end1 && end2 <= end1) && !(start1 == start2 && end1 == end2)) // existing annotation t nested in annotation
              && !((start2 <= start1 && start1 <= end2 && end1 <= end2) && !(start1 == start2 && end1 == end2)) // annotation nested in exisiting annotation t
          ;
        };
        Predicate<Vertex> hasDifferentUUID = t -> {
          String uuid2 = (String) t.property("uuid").value();
          return !uuid1.equals(uuid2);
        };
        overlaps.set(StreamUtil.stream(traversal)//
            .filter(hasDifferentUUID)//
            .filter(nonNestingOverlapWithAnnotation)//
            .findAny()//
            .isPresent()//
        );

      });
      return overlaps.get();
    });
  }

  private void throwBadRequest(UUID annotationId, String string) {
    throw new BadRequestException("annotation " + annotationId + " is " + string);
  }

  @Override
  public void confirmResource(UUID uuid) {
    storage.runInTransaction(() -> {
      ResourceVF resourceVF = storage.readVF(ResourceVF.class, uuid)//
          .orElseThrow(resourceNotFound(uuid));
      updateState(resourceVF, AlexandriaState.CONFIRMED);
    });
  }

  @Override
  public void setTextView(UUID resourceUUID, String viewId, TextView textView, TextViewDefinition textViewDefinition) {
    storage.runInTransaction(() -> {
      ResourceVF resourceVF = storage.readVF(ResourceVF.class, resourceUUID).get();
      String json;
      try {
        String serializedTextViewMap = resourceVF.getSerializedTextViewMap();
        Map<String, TextView> textViewMap = deserializeToTextViewMap(serializedTextViewMap);
        textViewMap.put(viewId, textView);
        json = serializeTextViewMap(textViewMap);
        resourceVF.setSerializedTextViewMap(json);

        String serializedTextViewDefinitionMap = resourceVF.getSerializedTextViewDefinitionMap();
        Map<String, TextViewDefinition> textViewDefinitionMap = deserializeToTextViewDefinitionMap(serializedTextViewDefinitionMap);
        textViewDefinitionMap.put(viewId, textViewDefinition);
        json = serializeTextViewDefinitionMap(textViewDefinitionMap);
        resourceVF.setSerializedTextViewDefinitionMap(json);

      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public List<TextView> getTextViewsForResource(UUID resourceUUID) {
    List<TextView> textViews = new ArrayList<>();
    Set<String> viewNames = Sets.newHashSet();

    return storage.runInTransaction(() -> {
      ResourceVF resourceVF = storage.readVF(ResourceVF.class, resourceUUID).get();
      while (resourceVF != null) {
        String serializedTextViews = resourceVF.getSerializedTextViewMap();
        UUID uuid = UUID.fromString(resourceVF.getUuid());
        try {
          deserializeToTextViews(serializedTextViews).stream().filter(v -> !viewNames.contains(v.getName())).forEach((tv) -> {
            tv.setTextViewDefiningResourceId(uuid);
            textViews.add(tv);
            viewNames.add(tv.getName());
          });

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
        resourceVF = resourceVF.getParentResource();
      }
      return textViews;
    });
  }

  @Override
  public Optional<TextView> getTextView(UUID resourceId, String view) {
    TextView textView = storage.runInTransaction(() -> {
      ResourceVF resourceVF = storage.readVF(ResourceVF.class, resourceId).get();
      String serializedTextViews = resourceVF.getSerializedTextViewMap();
      try {
        Map<String, TextView> textViewMap = deserializeToTextViewMap(serializedTextViews);
        List<TextView> textViews = textViewMap//
            .entrySet()//
            .stream()//
            .filter(e -> e.getKey().equals(view))//
            .map(this::setName)//
            .collect(toList());

        if (textViews.isEmpty()) {
          ResourceVF parentResourceVF = resourceVF.getParentResource();
          if (parentResourceVF != null) {
            UUID parentUUID = UUID.fromString(parentResourceVF.getUuid());
            return getTextView(parentUUID, view).orElse(null);
          } else {
            return null;
          }

        } else {
          return textViews.get(0);
        }

      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
    return Optional.ofNullable(textView);
  }

  @Override
  public Optional<TextViewDefinition> getTextViewDefinition(UUID resourceId, String view) {
    TextViewDefinition textViewDefinition = storage.runInTransaction(() -> {
      ResourceVF resourceVF = storage.readVF(ResourceVF.class, resourceId).get();
      String serializedTextViewDefinitions = resourceVF.getSerializedTextViewDefinitionMap();
      try {
        Map<String, TextViewDefinition> textViewDefinitionMap = deserializeToTextViewDefinitionMap(serializedTextViewDefinitions);
        return textViewDefinitionMap.get(view);

      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
    return Optional.ofNullable(textViewDefinition);
  }

  @Override
  public Map<String, Object> getMetadata() {
    return storage.runInTransaction(() -> {
      Map<String, Object> metadata = Maps.newLinkedHashMap();
      metadata.put("type", this.getClass().getCanonicalName());
      metadata.put("storage", storage.getMetadata());
      return metadata;
    });
  }

  @Override
  public void destroy() {
    // Log.info("destroy called");
    storage.destroy();
    // Log.info("destroy done");
  }

  @Override
  public void exportDb(String format, String filename) {
    storage.runInTransaction(Unchecked.runnable(() -> storage.writeGraph(DumpFormat.valueOf(format), filename)));
  }

  @Override
  public void importDb(String format, String filename) {
    storage = clearGraph();
    storage.runInTransaction(Unchecked.runnable(() -> storage.readGraph(DumpFormat.valueOf(format), filename)));
  }

  @Override
  public void runInTransaction(Runnable runner) {
    storage.runInTransaction(runner);
  }

  @Override
  public <A> A runInTransaction(Supplier<A> supplier) {
    return storage.runInTransaction(supplier);
  }

  @Override
  public boolean storeTextGraph(UUID resourceId, ParseResult result) {
    if (readResource(resourceId).isPresent()) {
      textGraphService.storeTextGraph(resourceId, result);
      return true;
    }
    // something went wrong
    readResource(resourceId).get().setHasText(false);
    return false;
  }

  @Override
  public Stream<TextGraphSegment> getTextGraphSegmentStream(UUID resourceId, List<List<String>> orderedLayerTags) {
    return textGraphService.getTextGraphSegmentStream(resourceId, orderedLayerTags);
  }

  @Override
  public Stream<TextAnnotation> getTextAnnotationStream(UUID resourceId) {
    return textGraphService.getTextAnnotationStream(resourceId);
  }

  @Override
  public void updateTextAnnotation(TextAnnotation textAnnotation) {
    textGraphService.updateTextAnnotation(textAnnotation);
  }

  @Override
  public void wrapContentInChildTextAnnotation(TextAnnotation existingTextAnnotation, TextAnnotation newTextAnnotation) {
    textGraphService.wrapContentInChildTextAnnotation(existingTextAnnotation, newTextAnnotation);
  }

  // - other public methods -//

  public void createSubResource(AlexandriaResource subResource) {
    storage.runInTransaction(() -> {
      final ResourceVF rvf;
      final UUID uuid = subResource.getId();
      if (storage.existsVF(ResourceVF.class, uuid)) {
        rvf = storage.readVF(ResourceVF.class, uuid).get();
      } else {
        rvf = storage.createVF(ResourceVF.class);
        rvf.setUuid(uuid.toString());
      }

      rvf.setCargo(subResource.getCargo());
      final UUID parentId = UUID.fromString(subResource.getParentResourcePointer().get().getIdentifier());
      Optional<ResourceVF> parentVF = storage.readVF(ResourceVF.class, parentId);
      rvf.setParentResource(parentVF.get());

      setAlexandriaVFProperties(rvf, subResource);
    });
  }

  public void dumpToGraphSON(OutputStream os) throws IOException {
    storage.runInTransaction(Unchecked.runnable(() -> storage.dumpToGraphSON(os)));
  }

  public void dumpToGraphML(OutputStream os) throws IOException {
    storage.runInTransaction(Unchecked.runnable(() -> storage.dumpToGraphML(os)));
  }

  // - package methods -//

  Storage clearGraph() {
    storage.runInTransaction(() -> storage.getVertexTraversal()//
        .forEachRemaining(org.apache.tinkerpop.gremlin.structure.Element::remove));
    return storage;
  }

  void createOrUpdateResource(AlexandriaResource resource) {
    final UUID uuid = resource.getId();
    storage.runInTransaction(() -> {
      final ResourceVF rvf;
      if (storage.existsVF(ResourceVF.class, uuid)) {
        rvf = storage.readVF(ResourceVF.class, uuid).get();
      } else {
        rvf = storage.createVF(ResourceVF.class);
        rvf.setUuid(uuid.toString());
      }

      rvf.setCargo(resource.getCargo());

      setAlexandriaVFProperties(rvf, resource);
    });
  }

  void updateState(AlexandriaVF vf, AlexandriaState newState) {
    vf.setState(newState.name());
    vf.setStateSince(Instant.now().getEpochSecond());
  }

  // - private methods -//

  private String serializeTextViewMap(Map<String, TextView> textViewMap) throws JsonProcessingException {
    return TEXTVIEW_WRITER.writeValueAsString(textViewMap);
  }

  private Map<String, TextView> deserializeToTextViewMap(String json) throws IOException {
    if (StringUtils.isEmpty(json)) {
      return Maps.newHashMap();
    }
    return TEXTVIEW_READER.readValue(json);
  }

  private String serializeTextViewDefinitionMap(Map<String, TextViewDefinition> textViewDefinitionMap) throws JsonProcessingException {
    return TEXTVIEWDEFINITION_WRITER.writeValueAsString(textViewDefinitionMap);
  }

  private Map<String, TextViewDefinition> deserializeToTextViewDefinitionMap(String json) throws JsonProcessingException, IOException {
    if (StringUtils.isEmpty(json)) {
      return Maps.newHashMap();
    }
    return TEXTVIEWDEFINITION_READER.readValue(json);
  }

  private AlexandriaResource deframeResource(Vertex v) {
    ResourceVF rvf = storage.frameVertex(v, ResourceVF.class);
    return deframeResource(rvf);
  }

  private AlexandriaResource deframeResource(ResourceVF rvf) {
    AlexandriaResource resource = deframeResourceLite(rvf);
    setTextViews(rvf, resource);

    ResourceVF parentResource = rvf.getParentResource();
    if (parentResource != null) {
      resource.setParentResourcePointer(new IdentifiablePointer<>(AlexandriaResource.class, parentResource.getUuid()));
    }
    rvf.getSubResources().stream()//
        .forEach(vf -> resource.addSubResourcePointer(new IdentifiablePointer<>(AlexandriaResource.class, vf.getUuid())));
    return resource;
  }

  private AlexandriaResource deframeResourceLite(ResourceVF rvf) {
    TentativeAlexandriaProvenance provenance = deframeProvenance(rvf);
    UUID uuid = getUUID(rvf);
    AlexandriaResource resource = new AlexandriaResource(uuid, provenance);
    resource.setHasText(rvf.getHasText());
    resource.setCargo(rvf.getCargo());
    resource.setState(AlexandriaState.valueOf(rvf.getState()));
    resource.setStateSince(Instant.ofEpochSecond(rvf.getStateSince()));
    return resource;
  }

  private AnnotatorVF frameAnnotator(Annotator annotator) {
    AnnotatorVF avf = storage.createVF(AnnotatorVF.class);
    avf.setCode(annotator.getCode());
    avf.setDescription(annotator.getDescription());
    return avf;
  }

  private Annotator deframeAnnotator(AnnotatorVF avf) {
    return new Annotator()//
        .setCode(avf.getCode())//
        .setDescription(avf.getDescription())//
        .setResourceURI(locationBuilder.locationOf(AlexandriaResource.class, avf.getResource().getUuid()));
  }

  private void updateTextRangeAnnotation(TextRangeAnnotationVF vf, TextRangeAnnotation annotation) {
    vf.setUuid(annotation.getId().toString());
    vf.setRevision(annotation.getRevision());
    vf.setName(annotation.getName());
    vf.setAnnotatorCode(annotation.getAnnotator());
    Position position = annotation.getPosition();
    position.getXmlId().ifPresent(xmlId -> {
      vf.setXmlId(xmlId);
      if (position.getOffset().isPresent()) {
        vf.setOffset(position.getOffset().get());
      }
      if (position.getLength().isPresent()) {
        vf.setLength(position.getLength().get());
      }
    });
    position.getTargetAnnotationId().ifPresent(targetId -> vf.setTargetAnnotationId(targetId.toString()));
    AbsolutePosition absolutePosition = annotation.getAbsolutePosition();
    vf.setAbsoluteXmlId(absolutePosition.getXmlId());
    vf.setAbsoluteOffset(absolutePosition.getOffset());
    vf.setAbsoluteLength(absolutePosition.getLength());
    vf.setUseOffset(annotation.getUseOffset());
    try {
      String json = new ObjectMapper().writeValueAsString(annotation.getAttributes());
      vf.setAttributesAsJson(json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

  }

  private TextRangeAnnotation deframeTextRangeAnnotation(TextRangeAnnotationVF vf) {
    String targetAnnotationId = vf.getTargetAnnotationId();
    UUID targetAnnotationUUID = targetAnnotationId == null ? null : UUID.fromString(targetAnnotationId);
    Position position = new Position()//
        .setTargetAnnotationId(targetAnnotationUUID)//
        .setXmlId(vf.getXmlId())//
        .setOffset(vf.getOffset())//
        .setLength(vf.getLength());
    AbsolutePosition absolutePosition = new AbsolutePosition()//
        .setXmlId(vf.getAbsoluteXmlId())//
        .setOffset(vf.getAbsoluteOffset())//
        .setLength(vf.getAbsoluteLength());
    Map<String, String> attributes;
    try {
      String attributesAsJson = StringUtils.defaultIfBlank(vf.getAttributesAsJson(), "{}");
      TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
      };
      attributes = new ObjectMapper().readValue(attributesAsJson, typeRef);
      return new TextRangeAnnotation()//
          .setId(getUUID(vf))//
          .setRevision(vf.getRevision())//
          .setName(vf.getName())//
          .setAnnotator(vf.getAnnotatorCode())//
          .setAttributes(attributes)//
          .setPosition(position)//
          .setAbsolutePosition(absolutePosition);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void setTextViews(ResourceVF rvf, AlexandriaResource resource) {
    String textViewsJson = rvf.getSerializedTextViewMap();
    if (StringUtils.isNotEmpty(textViewsJson)) {
      try {
        List<TextView> textViews = deserializeToTextViews(textViewsJson);
        resource.setDirectTextViews(textViews);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  private List<TextView> deserializeToTextViews(String textViewsJson) throws IOException {
    Map<String, TextView> textViewMap = deserializeToTextViewMap(textViewsJson);
    return textViewMap.entrySet()//
        .stream()//
        .map(this::setName)//
        .collect(toList());
  }

  private TextView setName(Entry<String, TextView> entry) {
    TextView textView = entry.getValue();
    textView.setName(entry.getKey());
    return textView;
  }

  private TentativeAlexandriaProvenance deframeProvenance(AlexandriaVF avf) {
    String provenanceWhen = avf.getProvenanceWhen();
    return new TentativeAlexandriaProvenance(avf.getProvenanceWho(), Instant.parse(provenanceWhen), avf.getProvenanceWhy());
  }

  private void setAlexandriaVFProperties(AlexandriaVF vf, Accountable accountable) {
    vf.setUuid(accountable.getId().toString());

    vf.setState(accountable.getState().toString());
    vf.setStateSince(accountable.getStateSince().getEpochSecond());

    AlexandriaProvenance provenance = accountable.getProvenance();
    vf.setProvenanceWhen(provenance.getWhen().toString());
    vf.setProvenanceWho(provenance.getWho());
    vf.setProvenanceWhy(provenance.getWhy());
  }

  // framedGraph methods

  private Supplier<NotFoundException> annotationNotFound(UUID id) {
    return () -> new NotFoundException("no annotation found with uuid " + id);
  }

  private Supplier<NotFoundException> resourceNotFound(UUID id) {
    return () -> new NotFoundException("no resource found with uuid " + id);
  }

  private BadRequestException incorrectStateException(UUID oldAnnotationId, String string) {
    return new BadRequestException("annotation " + oldAnnotationId + " is " + string);
  }

  private UUID getUUID(IdentifiableVF vf) {
    return UUID.fromString(vf.getUuid().replaceFirst("\\..$", "")); // remove revision suffix for deprecated annotations
  }

  private ResourceVF readExistingResourceVF(UUID uuid) {
    return storage.runInTransaction(() -> storage.readVF(ResourceVF.class, uuid))//
        .orElseThrow(() -> new NotFoundException("no resource found with uuid " + uuid));
  }

  public Storage storage() {
    return storage;
  }

}
