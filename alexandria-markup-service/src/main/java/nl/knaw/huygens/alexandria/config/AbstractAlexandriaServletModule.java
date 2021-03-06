package nl.knaw.huygens.alexandria.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;

import nl.knaw.huygens.alexandria.endpoint.resource.ResourceEntityBuilder;
import nl.knaw.huygens.alexandria.service.AlexandriaService;
import nl.knaw.huygens.alexandria.service.TinkerPopService;
import nl.knaw.huygens.alexandria.util.Scheduler;

public abstract class AbstractAlexandriaServletModule extends ServletModule {
  private static final int NTHREADS = 5;

  @Override
  protected void configureServlets() {
    // guice binds here
    // Log.trace("configureServlets(): setting up Guice bindings");
    Class<? extends TinkerPopService> tinkerpopServiceClass = getTinkerPopServiceClass();
    bind(AlexandriaService.class).to(tinkerpopServiceClass);
    bind(TinkerPopService.class).to(tinkerpopServiceClass);
    bind(ResourceEntityBuilder.class).in(Scopes.SINGLETON);
    bind(Scheduler.class).in(Scopes.SINGLETON);
    ExecutorService executorService = Executors.newFixedThreadPool(NTHREADS);
    bind(ExecutorService.class).toInstance(executorService);
    super.configureServlets();
  }

  abstract public Class<? extends TinkerPopService> getTinkerPopServiceClass();
}
