/*
 * Copyright 2025 HM Revenue & Customs
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

package models.request

import connectors.DownstreamServiceError
import controllers.contact.RegistrationException
import uk.gov.hmrc.auth.core.retrieve.Credentials

final case class IdentityData(internalId: Option[String] = None, credentials: Option[Credentials] = None) {

  def credId: String =
    credentials.map(_.providerId).getOrElse(
      throw DownstreamServiceError(
        "Cannot find user credentials id",
        RegistrationException("Cannot find user credentials id")
      )
    )

}
