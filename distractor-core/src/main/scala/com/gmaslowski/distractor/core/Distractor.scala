package com.gmaslowski.distractor.core

import akka.actor._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.gmaslowski.distractor.core.Distractor.systemReactor
import com.gmaslowski.distractor.core.api.DistractorApi.Register
import com.gmaslowski.distractor.core.api.DistractorRequestHandler
import com.gmaslowski.distractor.core.reactor.ReactorRegistry
import com.gmaslowski.distractor.core.transport.TransportRegistry
import com.gmaslowski.distractor.reactor.foaas.FoaasReactor
import com.gmaslowski.distractor.reactor.jira.JiraReactor
import com.gmaslowski.distractor.reactor.spring.boot.actuator.SpringBootActuatorReactor
import com.gmaslowski.distractor.reactor.system.SystemReactor
import com.gmaslowski.distractor.reactor.weather.WeatherReactor
import com.gmaslowski.distractor.transport.http.rest.HttpRestTransport
import com.gmaslowski.distractor.transport.info.InfoReactor
import com.gmaslowski.distractor.transport.info.InfoReactor.Information
import com.gmaslowski.distractor.transport.slack.http.SlackHttpTransport
import com.gmaslowski.distractor.transport.telnet.TelnetTransport
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient}

object DistractorBootstrap {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("distractor")

    val distractor = system.actorOf(Props[Distractor], "distractor")

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
  def systemReactor = sys.env.get("SYSTEM_REACTOR").getOrElse("OFF")

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

    implicit val mat = ActorMaterializer.apply(ActorMaterializerSettings.create(context.system))
    val ahcWsClient = new AhcWSClient(new AhcConfigBuilder().build())

    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    transportRegistry = context.system.actorOf(TransportRegistry.props, "transport-registry")
    reactorRegistry = context.system.actorOf(ReactorRegistry.props, "reactor-registry")
    requestHandler = context.actorOf(DistractorRequestHandler.props(reactorRegistry), "request-handler")

    createAndRegisterInfoReactor

    // todo: asap find a way of dynamic registering of reactors and transports by using API only

    if ("ON" equals systemReactor) {
      reactorRegistry ! Register("system", context.actorOf(SystemReactor.props))
    }
    reactorRegistry ! Register("jira", context.actorOf(JiraReactor.props(ahcWsClient), "jira"))
    reactorRegistry ! Register("springboot", context.actorOf(SpringBootActuatorReactor.props(ahcWsClient, mapper), "springboot"))
    reactorRegistry ! Register("foaas", context.actorOf(FoaasReactor.props(ahcWsClient), "foaas"))
    // reactorRegistry ! Register("docker", context.actorOf(DockerReactor.props, "docker"))
    reactorRegistry ! Register("weather", context.actorOf(WeatherReactor.props(ahcWsClient), "weather"))

    // fixme: transports should be distractor-kernel independent
    context.actorOf(TelnetTransport.props(transportRegistry), "telnet")
    context.actorOf(HttpRestTransport.props(transportRegistry), "http-rest")
    context.actorOf(SlackHttpTransport.props(transportRegistry), "slack-http")
  }

  def createAndRegisterInfoReactor: Unit = {

    val information: Information = Information(
      context.system.settings.config.getString("reactor.info.appName"),
      context.system.settings.config.getString("reactor.info.version"),
      context.system.settings.config.getString("reactor.info.author")
    )

    reactorRegistry ! Register("info", context.actorOf(InfoReactor.props(information, transportRegistry, reactorRegistry)))
  }
}
