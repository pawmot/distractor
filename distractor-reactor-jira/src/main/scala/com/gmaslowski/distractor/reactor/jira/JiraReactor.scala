package com.gmaslowski.distractor.reactor.jira

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.gmaslowski.distractor.core.reactor.api.ReactorApi.{ReactorRequest, ReactorResponse}
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient}

object JiraReactor {

  def props = Props[JiraReactor]
}

class JiraReactor extends Actor with ActorLogging {

  override def receive = {
    case reactorRequest: ReactorRequest =>
      implicit val mat = ActorMaterializer.apply(ActorMaterializerSettings.create(context.system))
      implicit val ec = context.dispatcher

      val issueNumber = reactorRequest.data

      val client = new AhcWSClient(new AhcConfigBuilder().build())
      val sender = context.sender()

      client
        .url(s"${sys.env("JIRA_LINK")}/rest/api/2/issue/$issueNumber")
        .withAuth(sys.env("JIRA_USER"), sys.env("JIRA_PASS"), BASIC)
        .get()
        .onSuccess {
          case result =>
            sender forward ReactorResponse(reactorRequest.reactorId, result.body)
            client.close()
        }
  }
}
