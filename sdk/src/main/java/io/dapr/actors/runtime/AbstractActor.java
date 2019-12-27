/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorTrace;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the base class for actors.
 *
 * The base type for actors, that provides the common functionality
 * for actors that derive from {@link Actor}.
 * The state is preserved across actor garbage collections and fail-overs.
 */
public abstract class AbstractActor {

  /**
   * Type of tracing messages.
   */
  private static final String TRACE_TYPE = "Actor";

  /**
   * Context for the Actor runtime.
   */
  private final ActorRuntimeContext<?> actorRuntimeContext;

  /**
   * Actor identifier.
   */
  private final ActorId id;

  /**
   * Manager for the states in Actors.
   */
  private final ActorStateManager actorStateManager;

  /**
   * Emits trace messages for Actors.
   */
  private final ActorTrace actorTrace;

  /**
   * Registered timers for this Actor.
   */
  private final Map<String, ActorTimer<?>> timers;

  /**
   * Instantiates a new Actor.
   * @param runtimeContext Context for the runtime.
   * @param id Actor identifier.
   */
  protected AbstractActor(ActorRuntimeContext runtimeContext, ActorId id) {
    this.actorRuntimeContext = runtimeContext;
    this.id = id;
    this.actorStateManager = new ActorStateManager(
      runtimeContext.getStateProvider(),
      runtimeContext.getActorTypeInformation().getName(),
      id);
    this.actorTrace = runtimeContext.getActorTrace();
    this.timers = Collections.synchronizedMap(new HashMap<>());
  }

  /**
   * Registers a reminder for this Actor.
   * @param reminderName Name of the reminder.
   * @param data Data to be send along with reminder triggers.
   * @param dueTime Due time for the first trigger.
   * @param period Frequency for the triggers.
   * @return Asynchronous void response.
   */
  protected Mono<Void> registerReminder(
          String reminderName,
          String data,
          Duration dueTime,
          Duration period) {
    try {
      ActorReminderParams params = new ActorReminderParams(data, dueTime, period);
      String serialized = this.actorRuntimeContext.getActorSerializer().serialize(params);
      return this.actorRuntimeContext.getDaprClient().registerReminder(
        this.actorRuntimeContext.getActorTypeInformation().getName(),
        this.id.toString(),
        reminderName,
        serialized);
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

  /**
   * Registers a Timer for the actor. A timer name is autogenerated by the runtime to keep track of it.
   *
   * @param timerName Name of the timer, unique per Actor (auto-generated if null).
   * @param methodName Name of the method to be called.
   * @param state State object to be passed it to the method when timer triggers.
   * @param dueTime The amount of time to delay before the async callback is first invoked.
   *                Specify negative one (-1) milliseconds to prevent the timer from starting.
   *                Specify zero (0) to start the timer immediately.
   * @param period The time interval between invocations of the async callback.
   *               Specify negative one (-1) milliseconds to disable periodic signaling.
   * @param <S> Type for the state object.
   * @return Asynchronous result.
   */
  protected <S> Mono<Void> registerActorTimer(
          String timerName,
          String methodName,
          S state,
          Duration dueTime,
          Duration period) {
    String name = timerName;
    if ((timerName == null) || (timerName.isEmpty())) {
      name = String.format("%s_Timer_%d", this.id.toString(), this.timers.size() + 1);
    }

    ActorTimer<S> actorTimer = new ActorTimer(this, name, methodName, state, dueTime, period);
    String serializedTimer = null;
    try {
      serializedTimer = this.actorRuntimeContext.getActorSerializer().serialize(actorTimer);
    } catch (IOException e) {
      return Mono.error(e);
    }
    this.timers.put(name, actorTimer);
    return this.actorRuntimeContext.getDaprClient().registerTimer(
            this.actorRuntimeContext.getActorTypeInformation().getName(),
            this.id.toString(),
            name,
            serializedTimer);
  }

  /**
   * Unregisters an Actor timer.
   * @param actorTimer Timer to be unregistered.
   * @return Asynchronous void response.
   */
  protected Mono<Void> unregister(ActorTimer<?> actorTimer) {
    return this.actorRuntimeContext.getDaprClient().unregisterTimer(
            this.actorRuntimeContext.getActorTypeInformation().getName(),
            this.id.toString(),
            actorTimer.getName())
            .then(this.onUnregisteredTimer(actorTimer));
  }

  /**
   * Callback function invoked after an Actor has been activated.
   * @return Asynchronous void response.
   */
  protected Mono<Void> onActivate() { return Mono.empty(); }

  /**
   * Callback function invoked after an Actor has been deactivated.
   * @return Asynchronous void response.
   */
  protected Mono<Void> onDeactivate() { return Mono.empty(); }

  /**
   * Callback function invoked before method is invoked.
   * @param actorMethodContext Method context.
   * @return Asynchronous void response.
   */
  protected Mono<Void> onPreActorMethod(ActorMethodContext actorMethodContext) {
    return Mono.empty();
  }

  /**
   * Callback function invoked after method is invoked.
   * @param actorMethodContext Method context.
   * @return Asynchronous void response.
   */
  protected Mono<Void> onPostActorMethod(ActorMethodContext actorMethodContext) {
    return Mono.empty();
  }

  /**
   * Saves the state of this Actor.
   * @return Asynchronous void response.
   */
  protected Mono<Void> saveState() {
    return this.actorStateManager.save();
  }

  /**
   * Resets the state of this Actor.
   * @return Asynchronous void response.
   */
  Mono<Void> resetState() { return this.actorStateManager.clear(); }

  /**
   * Gets a given timer by name.
   * @param timerName Timer name.
   * @return Asynchronous void response.
   */
  ActorTimer getActorTimer(String timerName)
  {
    return timers.getOrDefault(timerName, null);
  }

  /**
   * Internal callback when an Actor is activated.
   * @return Asynchronous void response.
   */
  Mono<Void> onActivateInternal() {
    this.actorTrace.writeInfo(TRACE_TYPE, this.id.toString(), "Activating ...");

    return this.resetState()
            .then(this.onActivate())
            .then(this.doWriteInfo(TRACE_TYPE, this.id.toString(), "Activated"))
            .then(this.saveState());
  }

  /**
   * Internal callback when an Actor is deactivated.
   * @return Asynchronous void response.
   */
  Mono<Void> onDeactivateInternal() {
    this.actorTrace.writeInfo(TRACE_TYPE, this.id.toString(), "Deactivating ...");

    return this.resetState()
            .then(this.onDeactivate())
            .then(this.doWriteInfo(TRACE_TYPE, this.id.toString(), "Deactivated"))
            .then(this.saveState());
  }

  /**
   * Internal callback prior to method be invoked.
   * @param actorMethodContext Method context.
   * @return Asynchronous void response.
   */
  Mono<Void> onPreActorMethodInternal(ActorMethodContext actorMethodContext) {
    return this.onPreActorMethod(actorMethodContext);
  }

  /**
   * Internal callback after method is invoked.
   * @param actorMethodContext Method context.
   * @return Asynchronous void response.
   */
  Mono<Void> onPostActorMethodInternal(ActorMethodContext actorMethodContext) {
    return this.onPostActorMethod(actorMethodContext)
            .then(this.saveState());
  }

  /**
   * Internal callback for when Actor timer is unregistered.
   * @param timer Timer being unregistered.
   * @return Asynchronous void response.
   */
  Mono<Void> onUnregisteredTimer(ActorTimer<?> timer) {
    this.timers.remove(timer.getName());
    return Mono.empty();
  }

  /**
   * Internal method to emit a trace message.
   * @param type Type of trace message.
   * @param id Identifier of entity relevant for the trace message.
   * @param message Message to be logged.
   * @return Asynchronous void response.
   */
  private Mono<Void> doWriteInfo(String type, String id, String message) {
    this.actorTrace.writeInfo(type, id, message);
    return Mono.empty();
  }

}
