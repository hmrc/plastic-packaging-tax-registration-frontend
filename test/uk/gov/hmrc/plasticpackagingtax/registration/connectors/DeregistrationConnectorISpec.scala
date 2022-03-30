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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors

import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, put}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.test.Helpers.{await, OK}
import play.api.test.Injecting
import uk.gov.hmrc.plasticpackagingtax.registration.models.deregistration.{
  DeregistrationDetails,
  DeregistrationReason
}

class DeregistrationConnectorISpec
    extends ConnectorISpec with Injecting with ScalaFutures with EitherValues {

  lazy val connector: DeregistrationConnector =
    inject[DeregistrationConnector]

  val deregistrationDetails = DeregistrationDetails(deregister = Some(true),
                                                    reason =
                                                      Some(DeregistrationReason.CeasedTrading)
  )

  val pptReference = "XMPPT123456789"

  "Deregister" should {
    "return success response" when {

      "valid request is sent" in {

        givenPutDeregistration(pptReference, Status.OK, DeregistrationReason.CeasedTrading.toString)

        val res: Either[ServiceError, Unit] =
          await(connector.deregister(pptReference, deregistrationDetails))

        res.right.get mustBe ()
        getTimer("ppt.deregister.timer").getCount mustBe 1
      }

    }

    "return error " when {

      "service returns non success status code" in {
        givenPutDeregistration(pptReference,
                               Status.BAD_REQUEST,
                               DeregistrationReason.CeasedTrading.toString
        )

        val res: Either[ServiceError, Unit] =
          await(connector.deregister(pptReference, deregistrationDetails))

        res.left.value.getMessage must include("Failed to de-register")

        res.left.value.isInstanceOf[DownstreamServiceError]
      }

      "service returns invalid response" in {
        givenPutDeregistration(pptReference, Status.BAD_REQUEST, "something")

        val res = await(connector.deregister(pptReference, deregistrationDetails))

        res.left.value.isInstanceOf[DownstreamServiceError]
      }
    }

  }

  private def givenPutDeregistration(pptReference: String, status: Int, body: String) =
    stubFor(
      put(s"/subscriptions/deregister/${pptReference}")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

}
