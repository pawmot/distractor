package com.gmaslowski.distractor.transport.slack.http

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.pattern.Patterns.ask
import akka.stream.ActorMaterializer
import com.gmaslowski.distractor.core.api.DistractorApi.{DistractorRequest, Register}
import com.gmaslowski.distractor.core.reactor.api.ReactorApi.ReactorResponse
import com.gmaslowski.distractor.transport.slack.http.SlackHttpTransport.{HTTP_PORT, makeDistractorCommand, makeResponseUrl}
import org.apache.commons.codec.net.URLCodec.decodeUrl
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient}
import spray.json.JsonParser

object SlackHttpTransport {
  val HTTP_PORT: Int = 8081

  def props = Props[SlackHttpTransport]

  def makeDistractorCommand(slackCommand: String): String = {
    val command = slackCommand.split("&")
      .map(keyVal => (keyVal.split("=")(0), keyVal.split("=")(1)))
      .toMap
      .apply("text")

    "/" + new String(decodeUrl(command.getBytes))
  }

  def makeResponseUrl(slackCommand: String): String =
    new String(decodeUrl(slackCommand.split("&")
      .map(keyVal => (keyVal.split("=")(0), keyVal.split("=")(1)))
      .toMap
      .apply("response_url").getBytes))

}

class SlackHttpTransport extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  val client = new AhcWSClient(new AhcConfigBuilder().build())

  val route =
    (post & path("command") & entity(as[String])) { slackMessageBody =>
      complete {

        val command = makeDistractorCommand(slackMessageBody)
        val responseUrl = makeResponseUrl(slackMessageBody)

        ask(context.actorSelection("akka://distractor/user/distractor/request-handler"), DistractorRequest(command), 10 seconds)
          .onSuccess({
            case ReactorResponse(reactorId, message) => {
              client
                .url(responseUrl)
                .post(s"""{"response_type": "in_channel", "text": "```${formatMessage(message)}```"}""")
            }
          })

        HttpResponse(200, entity = HttpEntity(ContentType(MediaTypes.`application/json`),s"""{\"response_type\": \"in_channel\"}"""))
      }
    }

  def formatMessage(json: String): String = {
    JsonParser(json).prettyPrint.replaceAll("\"", "")
  }

  Http(context.system).bindAndHandle(route, "0.0.0.0", HTTP_PORT)

  override def preStart() {
    // todo: fix that; should be provided via props, and not on preStart
    context.actorSelection("akka://distractor/user/distractor/transport-registry") ! Register("slack-http", self)
  }

  override def receive: Receive = {
    case ReactorResponse(reactorId, message) =>
  }
}