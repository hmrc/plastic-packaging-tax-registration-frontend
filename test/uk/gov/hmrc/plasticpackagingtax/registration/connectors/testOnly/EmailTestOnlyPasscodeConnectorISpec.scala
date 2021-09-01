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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.testOnly

import base.Injector
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, urlMatching}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{OK, await}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{DownstreamServiceError, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus.{COMPLETE, INCORRECT_PASSCODE, JOURNEY_NOT_FOUND, TOO_MANY_ATTEMPTS}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification._

class EmailTestOnlyPasscodeConnectorISpec
    extends ConnectorISpec with Injector with ScalaFutures with EitherValues {

  lazy val connector: EmailTestOnlyPasscodeConnector =
    app.injector.instanceOf[EmailTestOnlyPasscodeConnector]

  "getTestOnlyPasscode" when {
    "returns passcode" in {
      val validResponse =
        "{\"passcodes\":\"[{\"email\" : \"ppt@mail.com\", \"passcode\" : \"HDDDYX\", }]\"}"
      givenGetTestOnlyPasscode(OK, validResponse)

      val res = await(connector.getTestOnlyPasscode())

      res.value mustBe validResponse
    }

    "throws exception for errors" in {
      val validResponse =
        "{\"code\": \"UNEXPECTED_ERROR\",\n    \"message\": \"Bearer token expired\"}"
      givenGetTestOnlyPasscode(INTERNAL_SERVER_ERROR, validResponse)

      val res: Either[ServiceError, String] = await(connector.getTestOnlyPasscode())

      res.left.value.isInstanceOf[DownstreamServiceError]
    }
  }

  private def givenGetTestOnlyPasscode(status: Int, body: String) =
    stubFor(
      get(s"/test-only/passcodes")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

}
