package nl.knaw.huygens.alexandria.textgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import nl.knaw.huygens.alexandria.service.AlexandriaService;

public class DotFactory {
  public static String createDot(AlexandriaService service, UUID resourceId) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("digraph TextGraph {\n")//
        .append("  ranksep=1.0\n\n");
    AtomicInteger textCounter = new AtomicInteger(0);
    AtomicInteger annotationCounter = new AtomicInteger(0);
    Stack<Integer> openAnnotations = new Stack<>();
    Multimap<Integer, Integer> annotationDepthMap = ArrayListMultimap.create();
    service.runInTransaction(() -> service.getTextGraphSegmentStream(resourceId, new ArrayList<List<String>>()).forEach((s) -> {
      int tn = textCounter.getAndIncrement();
      appendTextVertex(stringBuilder, s.getTextSegment(), tn);
      if (tn > 0) {
        appendNextTextEdge(stringBuilder, tn);
      }
      List<TextAnnotation> textAnnotationsToOpen = s.getTextAnnotationsToOpen();
      s.getMilestoneTextAnnotation().ifPresent(textAnnotationsToOpen::add);
      for (TextAnnotation textAnnotation : textAnnotationsToOpen) {
        int an = annotationCounter.getAndIncrement();
        appendAnnotationVertex(stringBuilder, an, textAnnotation);
        appendParentAnnotationEdge(stringBuilder, an, openAnnotations);
        annotationDepthMap.put(openAnnotations.size(), an);
        openAnnotations.push(an);
      }
      appendAnnotationEdge(stringBuilder, tn, openAnnotations.peek());
      int numberOftextAnnotationsToClose = s.getTextAnnotationsToClose().size();
      if (s.getMilestoneTextAnnotation().isPresent()) {
        numberOftextAnnotationsToClose += 1;
      }
      for (int i = 0; i < numberOftextAnnotationsToClose; i++) {
        if (!openAnnotations.isEmpty()) {
          openAnnotations.pop();
        }
      }
      stringBuilder.append("\n");
    }));
    appendTextHasSameRank(stringBuilder, textCounter.get());
    for (Collection<Integer> values : annotationDepthMap.asMap().values()) {
      appendAnnotationsWithSameRank(stringBuilder, values);
    }
    stringBuilder.append("}");
    return stringBuilder.toString();
  }

  private static void appendTextVertex(StringBuilder stringBuilder, String textSegment, int n) {
    stringBuilder.append("  t")//
        .append(n)//
        .append(" [shape=box, label=\"")//
        .append(escape(textSegment)//
            .replace(" ", "␣")//
            .replace("\n", "↵"))//
        .append("\"];\n");
  }

  private static String escape(String textSegment) {
    return textSegment.replace("\"", "\\\"");
  }

  private static void appendNextTextEdge(StringBuilder stringBuilder, int n) {
    stringBuilder.append("  t").append(n - 1).append(" -> t").append(n).append(";\n");
  }

  private static void appendAnnotationVertex(StringBuilder stringBuilder, int an, TextAnnotation textAnnotation) {
    stringBuilder.append("  a")//
        .append(an)//
        .append(" [label=\"")//
        .append(escape(textAnnotation.getName()))//
        // .append(" (" + textAnnotation.getDepth() + ")")//
        .append("\"];\n");
  }

  private static void appendParentAnnotationEdge(StringBuilder stringBuilder, int an, Stack<Integer> parents) {
    if (!parents.isEmpty()) {
      Integer parent = parents.peek();
      stringBuilder.append("  a").append(parent).append(" -> a").append(an).append(";\n");
    }
  }

  private static void appendAnnotationEdge(StringBuilder stringBuilder, int tn, Integer an) {
    stringBuilder.append("  a").append(an).append(" -> t").append(tn).append(" [color=\"blue\"];\n");
  }

  private static void appendTextHasSameRank(StringBuilder stringBuilder, int size) {
    stringBuilder.append("  {rank=same;");
    for (int i = 0; i < size; i++) {
      stringBuilder.append("t").append(i).append(";");
    }
    stringBuilder.append("}\n");
  }

  private static void appendAnnotationsWithSameRank(StringBuilder stringBuilder, Collection<Integer> annotations) {
    stringBuilder.append("  {rank=same;");
    for (int annotation : annotations) {
      stringBuilder.append("a").append(annotation).append(";");
    }
    stringBuilder.append("}\n");
  }

}
