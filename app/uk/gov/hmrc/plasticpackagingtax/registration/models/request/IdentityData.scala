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

package uk.gov.hmrc.plasticpackagingtax.registration.models.request

import org.joda.time.LocalDate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes, Name, _}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}

case class IdentityData(
  internalId: Option[String] = None,
  externalId: Option[String] = None,
  agentCode: Option[String] = None,
  credentials: Option[Credentials] = None,
  confidenceLevel: Option[ConfidenceLevel] = None,
  nino: Option[String] = None,
  saUtr: Option[String] = None,
  name: Option[Name] = None,
  dateOfBirth: Option[LocalDate] = None,
  email: Option[String] = None,
  agentInformation: Option[AgentInformation] = None,
  groupIdentifier: Option[String] = None,
  credentialRole: Option[String] = None,
  mdtpInformation: Option[MdtpInformation] = None,
  itmpName: Option[ItmpName] = None,
  itmpDateOfBirth: Option[LocalDate] = None,
  itmpAddress: Option[ItmpAddress] = None,
  affinityGroup: Option[AffinityGroup] = None,
  credentialStrength: Option[String] = None,
  loginTimes: Option[LoginTimes] = None
)
