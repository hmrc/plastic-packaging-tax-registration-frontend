/*
 * Copyright 2022 HM Revenue & Customs
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

import base.it.ConnectorISpec
import builders.RegistrationBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{VerificationException, WireMock}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType

class AuditorSpec extends ConnectorISpec with Injecting with ScalaFutures with RegistrationBuilder {

  val auditor: Auditor = inject[Auditor]
  val auditUrl         = "/write/audit"

  override def overrideConfig: Map[String, Any] =
    Map("auditing.enabled" -> true, "auditing.consumer.baseUri.port" -> wirePort)

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

        auditor.registrationSubmitted(registration, None, None)

        eventually(timeout(Span(5, Seconds))) {
          verifyEventSentToAudit(auditUrl,
                                 CreateRegistrationEvent(registration, None, None)
          ) mustBe true
        }
      }
    }

    "post group registration event" when {
      "registrationSubmitted invoked" in {
        givenAuditReturns(Status.NO_CONTENT)
        val registration = aRegistration(withGroupDetail(Some(groupDetailsWithMembers)),
                                         withRegistrationType(Some(RegType.GROUP))
        )

        auditor.registrationSubmitted(registration, None, Some("123456"))

        eventually(timeout(Span(5, Seconds))) {
          verifyEventSentToAudit(auditUrl,
                                 CreateRegistrationEvent(registration, None, Some("123456"))
          ) mustBe true
        }
      }
    }

    "not throw exception" when {
      "submit registration audit event fails" in {
        givenAuditReturns(Status.BAD_REQUEST)
        val registration = aRegistration()

        auditor.registrationSubmitted(registration, None, Some("123456"))

        eventually(timeout(Span(5, Seconds))) {
          verifyEventSentToAudit(auditUrl,
                                 CreateRegistrationEvent(registration, None, Some("123456"))
          ) mustBe true
        }
      }
    }

    "post start new registration event" when {
      "newRegistrationStarted invoked" in {
        givenAuditReturns(Status.NO_CONTENT)

        auditor.newRegistrationStarted("123456")

        eventually(timeout(Span(5, Seconds))) {
          verifyEventSentToAudit(auditUrl,
                                 StartRegistrationEvent(UserType.NEW, "123456")
          ) mustBe true
        }
      }
    }

    "not throw exception" when {
      "newRegistrationStarted audit event fails" in {
        givenAuditReturns(Status.BAD_REQUEST)

        auditor.newRegistrationStarted("123456")

        eventually(timeout(Span(5, Seconds))) {
          verifyEventSentToAudit(auditUrl,
                                 StartRegistrationEvent(UserType.NEW, "123456")
          ) mustBe true
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

  private def verifyEventSentToAudit(
    url: String,
    createRegistrationEvent: CreateRegistrationEvent
  ): Boolean =
    verifyEventSentToAudit(url,
                           CreateRegistrationEvent.eventType,
                           Json.toJson(createRegistrationEvent).toString
    )

  private def verifyEventSentToAudit(
    url: String,
    startRegistrationEvent: StartRegistrationEvent
  ): Boolean =
    verifyEventSentToAudit(url,
                           StartRegistrationEvent.eventType,
                           Json.toJson(startRegistrationEvent).toString()
    )

  private def verifyEventSentToAudit(url: String, eventType: String, body: String): Boolean =
    try {
      verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson(s"""{
               |                  "auditSource": "plastic-packaging-tax-registration-frontend",
               |                  "auditType": "$eventType",
               |                  "eventId": "$${json-unit.any-string}",
               |                  "tags": {
               |                    "clientIP": "-",
               |                    "path": "-",
               |                    "X-Session-ID": "-",
               |                    "Akamai-Reputation": "-",
               |                    "X-Request-ID": "-",
               |                    "deviceID": "-",
               |                    "clientPort": "-"
               |                  },
               |                  "detail": $body,
               |                  "generatedAt": "$${json-unit.any-string}"
               |                  }
               |                }""".stripMargin, true, true))
      )
      true
    } catch {
      case _: VerificationException => false
    }

}
