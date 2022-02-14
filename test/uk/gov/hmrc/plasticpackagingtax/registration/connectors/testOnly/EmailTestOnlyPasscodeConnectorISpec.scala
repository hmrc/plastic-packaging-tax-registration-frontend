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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.testOnly

import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, OK}
import play.api.test.Injecting
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  DownstreamServiceError,
  ServiceError
}

class EmailTestOnlyPasscodeConnectorISpec
    extends ConnectorISpec with Injecting with ScalaFutures with EitherValues {

  lazy val connector: EmailTestOnlyPasscodeConnector = inject[EmailTestOnlyPasscodeConnector]

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
