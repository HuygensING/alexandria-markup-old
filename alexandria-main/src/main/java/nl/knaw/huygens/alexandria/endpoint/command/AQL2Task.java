package nl.knaw.huygens.alexandria.endpoint.command;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.UUID;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.antlr.AQL2Lexer;
import nl.knaw.huygens.alexandria.antlr.AQL2Parser;
import nl.knaw.huygens.alexandria.antlr.QueryErrorListener;
import nl.knaw.huygens.alexandria.api.model.CommandStatus;

public class AQL2Task implements Runnable {
  private CommandStatus status;
  private UUID uuid;
  private String aql2Command;

  public AQL2Task(String aql2Command) {
    this.aql2Command = aql2Command;
    this.uuid = UUID.randomUUID();
    this.status = new CommandStatus();
  }

  public CommandStatus getStatus() {
    return status;
  }

  @Override
  public void run() {
    status.setStarted();
    Object result = process();
    status.setResult(result);
    status.setDone();
  }

  private Object process() {
    QueryVisitor visitor = new QueryVisitor();

    parse(visitor);

    String function = visitor.getFunction();
    List<Object> parameters = visitor.getParameters();

    Object result;
    switch (function) {
    case "hello":
      result = parameters.stream().map(this::hello).collect(joining("\n"));
      break;

    case "bye":
      result = parameters.stream().map(this::bye).collect(joining("\n"));
      break;

    default:
      result = "Unknown Command: " + aql2Command;
    }
    return result;
  }

  private void parse(QueryVisitor visitor) {
    QueryErrorListener errorListener = new QueryErrorListener();
    CharStream stream = new ANTLRInputStream(aql2Command);
    AQL2Lexer lex = new AQL2Lexer(stream);
    lex.removeErrorListeners();
    CommonTokenStream tokenStream = new CommonTokenStream(lex);
    AQL2Parser parser = new AQL2Parser(tokenStream);
    parser.removeErrorListeners();
    parser.addErrorListener(errorListener);
    parser.setBuildParseTree(true);
    ParseTree tree = parser.root();
    Log.info("tree={}", tree.toStringTree(parser));
    if (errorListener.heardErrors()) {
      // parseErrors.addAll(errorListener.getParseErrors().stream()//
      // .map(AlexandriaQueryParser::clarifyParseError)//
      // .collect(toList()));
      // result = "error";
    }

    visitor.visit(tree);
    // parseErrors.addAll(errorListener.getParseErrors());
  }

  public UUID getUUID() {
    return uuid;
  }

  public String hello(Object parameter) {
    return "Hello and welcome, " + parameter + "!";
  }

  public String bye(Object parameter) {
    return "Goodbye " + parameter + "!";
  }

}
