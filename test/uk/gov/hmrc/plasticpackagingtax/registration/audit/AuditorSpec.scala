/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.plasticpackagingtax.registration.audit

import base.Injector
import base.it.ConnectorISpec
import builders.RegistrationBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{VerificationException, WireMock}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration

class AuditorSpec extends ConnectorISpec with Injector with ScalaFutures with RegistrationBuilder {

  override def overrideConfig: Map[String, Any] =
    Map("auditing.enabled" -> true, "auditing.consumer.baseUri.port" -> wirePort)

  val auditor: Auditor = app.injector.instanceOf[Auditor]
  val auditUrl         = "/write/audit"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireHost, wirePort)
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  "Auditor" should {
    "post registration event" when {
      "registrationSubmitted invoked" in {
        givenAuditReturns(Status.NO_CONTENT)
        val registration = aRegistration()

        auditor.registrationSubmitted(registration)

        eventually(timeout(Span(5, Seconds))) {
          eventSendToAudit(auditUrl, registration) mustBe true
        }
      }
    }

    "not throw exception" when {
      "submit registration audit event fails" in {
        givenAuditReturns(Status.BAD_REQUEST)
        val registration = aRegistration()

        auditor.registrationSubmitted(registration)

        eventually(timeout(Span(5, Seconds))) {
          eventSendToAudit(auditUrl, registration) mustBe true
        }
      }
    }

    "post start new registration event" when {
      "newRegistrationStarted invoked" in {
        givenAuditReturns(Status.NO_CONTENT)

        auditor.newRegistrationStarted()

        eventually(timeout(Span(5, Seconds))) {
          eventSendToAudit(auditUrl, StartRegistrationEvent(UserType.NEW)) mustBe true
        }
      }
    }

    "not throw exception" when {
      "newRegistrationStarted audit event fails" in {
        givenAuditReturns(Status.BAD_REQUEST)

        auditor.newRegistrationStarted()

        eventually(timeout(Span(5, Seconds))) {
          eventSendToAudit(auditUrl, StartRegistrationEvent(UserType.NEW)) mustBe true
        }
      }
    }

    "post start new registration event" when {
      "existingRegistrationLoaded invoked" in {
        givenAuditReturns(Status.NO_CONTENT)

        auditor.existingRegistrationLoaded()

        eventually(timeout(Span(5, Seconds))) {
          eventSendToAudit(auditUrl, StartRegistrationEvent(UserType.RETURNING)) mustBe true
        }
      }
    }

    "not throw exception" when {
      "existingRegistrationLoaded audit event fails" in {
        givenAuditReturns(Status.BAD_REQUEST)

        auditor.existingRegistrationLoaded()

        eventually(timeout(Span(5, Seconds))) {
          eventSendToAudit(auditUrl, StartRegistrationEvent(UserType.RETURNING)) mustBe true
        }
      }
    }
  }

  private def givenAuditReturns(statusCode: Int): Unit =
    stubFor(
      post(auditUrl)
        .willReturn(
          aResponse()
            .withStatus(statusCode)
        )
    )

  private def eventSendToAudit(url: String, registration: Registration): Boolean =
    eventSendToAudit(url,
                     CreateRegistrationEvent.eventType,
                     Registration.format.writes(registration).toString()
    )

  private def eventSendToAudit(
    url: String,
    startRegistrationEvent: StartRegistrationEvent
  ): Boolean =
    eventSendToAudit(url,
                     StartRegistrationEvent.eventType,
                     StartRegistrationEvent.format.writes(startRegistrationEvent).toString()
    )

  private def eventSendToAudit(url: String, eventType: String, body: String): Boolean =
    try {
      verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson("""{
                  "auditSource": "plastic-packaging-tax-registration-frontend",
                  "auditType": """" + eventType + """",
                  "eventId": "${json-unit.any-string}",
                  "tags": {
                    "clientIP": "-",
                    "path": "-",
                    "X-Session-ID": "-",
                    "Akamai-Reputation": "-",
                    "X-Request-ID": "-",
                    "deviceID": "-",
                    "clientPort": "-"
                  },
                  "detail": """ + body + """,
                  "generatedAt": "${json-unit.any-string}",
                  "metadata": {
                    "sendAttemptAt": "${json-unit.any-string}",
                    "instanceID": "${json-unit.any-string}",
                    "sequence": "${json-unit.any-number}"
                  }
                }""".stripMargin, true, true))
      )
      true
    } catch {
      case _: VerificationException => false
    }

}
