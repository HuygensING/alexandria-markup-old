package nl.knaw.huygens.alexandria.endpoint.command;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import nl.knaw.huygens.alexandria.api.model.CommandResponse;
import nl.knaw.huygens.alexandria.api.model.CommandStatus;
import nl.knaw.huygens.alexandria.api.model.Commands;
import nl.knaw.huygens.alexandria.api.model.ProcessStatusMap;
import nl.knaw.huygens.alexandria.config.AlexandriaConfiguration;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

public class AQL2Command extends ResourcesCommand {

  public static final String COMMAND_PARAMETER = "command";
  private CommandResponse commandResponse = new CommandResponse();
  private AlexandriaService service;
  private final ProcessStatusMap<CommandStatus> commandStatusMap;
  private AlexandriaConfiguration config;
  private ExecutorService executorService;

  @Inject
  public AQL2Command(AlexandriaService service, //
      AlexandriaConfiguration config, //
      ExecutorService executorService, //
      ProcessStatusMap<CommandStatus> taskStatusMap) {
    this.service = service;
    this.config = config;
    this.executorService = executorService;
    this.commandStatusMap = taskStatusMap;
  }

  @Override
  public String getName() {
    return Commands.AQL2;
  }

  @Override
  public CommandResponse runWith(Map<String, Object> parameterMap) {
    String aql2Command = (String) parameterMap.get(COMMAND_PARAMETER);
    commandResponse.setASync(true);
    startCommandProcessing(aql2Command);
    // String result = process(aql2Command);
    // commandResponse.setResult(result);
    commandResponse.setParametersAreValid(true);
    return commandResponse;
  }

  private void startCommandProcessing(String aql2Command) {
    AQL2Task task = new AQL2Task(aql2Command);
    commandResponse.setStatusId(task.getUUID());
    commandStatusMap.put(task.getUUID(), task.getStatus());
    if (config.asynchronousEndpointsAllowed()) {
      executorService.execute(task);
    } else {
      // For now, for the acceptance tests.
      task.run();
    }

  }


}
