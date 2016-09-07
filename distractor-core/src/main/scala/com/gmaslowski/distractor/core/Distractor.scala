package com.gmaslowski.distractor.core

import akka.actor._
import com.gmaslowski.distractor.core.api.DistractorApi.RegisterMsg
import com.gmaslowski.distractor.core.api.DistractorRequestHandler
import com.gmaslowski.distractor.core.reactor.ReactorRegistry
import com.gmaslowski.distractor.core.transport.TransportRegistry
import com.gmaslowski.distractor.reactor.jira.JiraReactor
import com.gmaslowski.distractor.reactor.spring.boot.actuator.SpringBootActuatorReactor
import com.gmaslowski.distractor.reactor.system.SystemReactor
import com.gmaslowski.distractor.transport.http.rest.HttpRestTransport
import com.gmaslowski.distractor.transport.info.InfoReactor
import com.gmaslowski.distractor.transport.info.InfoReactor.Information
import com.gmaslowski.distractor.transport.telnet.TelnetTransport

object DistractorBootstrap {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("distractor")

    val distractor = system.actorOf(Props[Distractor], "distractor")

    // fixme: transports should be distractor-kernel independent
    system.actorOf(TelnetTransport.props, "telnet")
    system.actorOf(HttpRestTransport.props, "http-rest")

    // app terminator
    system.actorOf(Props(classOf[Terminator], distractor), "terminator")
  }

  class Terminator(ref: ActorRef) extends Actor with ActorLogging {
    context watch ref

    def receive = {
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system.", ref.path)
        context.system.terminate()
    }
  }

}

object Distractor {
  def props = Props[Distractor]
}

class Distractor extends Actor with ActorLogging {

  var transportRegistry: ActorRef = context.system.deadLetters
  var reactorRegistry: ActorRef = context.system.deadLetters
  var requestHandler: ActorRef = context.system.deadLetters

  def receive = {
    case msg: AnyRef => unhandled(msg)
  }

  override def preStart() = {

    transportRegistry = context.actorOf(TransportRegistry.props, "transport-registry")
    reactorRegistry = context.actorOf(ReactorRegistry.props, "reactor-registry")
    requestHandler = context.actorOf(DistractorRequestHandler.props(reactorRegistry), "request-handler")

    createAndRegisterInfoReactor
    // todo: asap find a way of dynamic registering of reactors and transports by using API only
    reactorRegistry ! RegisterMsg("system", context.actorOf(SystemReactor.props))
    reactorRegistry ! RegisterMsg("jira", context.actorOf(JiraReactor.props))
    reactorRegistry ! RegisterMsg("springboot", context.actorOf(SpringBootActuatorReactor.props))
  }

  def createAndRegisterInfoReactor: Unit = {

    val information: Information = new Information(
      context.system.settings.config.getString("reactor.info.appName"),
      context.system.settings.config.getString("reactor.info.version"),
      context.system.settings.config.getString("reactor.info.author")
    )

    reactorRegistry ! RegisterMsg("info", context.actorOf(InfoReactor.props(information, transportRegistry, reactorRegistry)))
  }
}
