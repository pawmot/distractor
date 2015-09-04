package com.fp.distractor.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.fp.distractor.core.reactor.ReactorRegistry
import com.fp.distractor.core.reactor.info.InfoReactor
import com.fp.distractor.core.reactor.info.InfoReactor.Information
import com.fp.distractor.core.reactor.system.SystemReactor
import com.fp.distractor.core.transport.TransportRegistry
import com.fp.distractor.registry.ActorRegistry.RegisterMsg

object Distractor {
  def props = Props[Distractor]
}

class Distractor extends Actor with ActorLogging {

  var transportRegistry: ActorRef = context.system.deadLetters
  var reactorRegistry: ActorRef = context.system.deadLetters

  def receive = {
    case AnyRef =>
  }

  override def preStart() = {
    transportRegistry = context.actorOf(TransportRegistry.props, "transport-registry")
    reactorRegistry = context.actorOf(ReactorRegistry.props, "reactor-registry")

    val mixer: ActorRef = context.actorOf(ReactorTransportMixer.props(reactorRegistry, transportRegistry), "reactor-transport-mixer")

    createAndRegisterInfoReactor
    reactorRegistry ! RegisterMsg("system", context.actorOf(SystemReactor.props(mixer)))
  }

  def createAndRegisterInfoReactor: Unit = {

    val information: Information = new Information(
      context.system.settings.config.getString("reactor.info.appName"),
      context.system.settings.config.getString("reactor.info.version"),
      context.system.settings.config.getString("reactor.info.author")
    )

    // fixme: the reactor registry was not in place yet ;(
    reactorRegistry ! RegisterMsg("info", context.actorOf(InfoReactor.props(information)))
  }
}