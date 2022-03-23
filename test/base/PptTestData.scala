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

package base

import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, LoginTimes, Name}
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.models.SignedInUser
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.IdentityData

object PptTestData {

  val testUserFeatures: Map[String, Boolean] = Map(Features.isPreLaunch -> true)

  val nrsCredentials: Credentials =
    Credentials(providerId = "providerId", providerType = "providerType")

  def newUser(
    internalId: String = "Int-ba17b467-90f3-42b6-9570-73be7b78eb2b",
    featureFlags: Map[String, Boolean] = testUserFeatures
  ): SignedInUser =
    SignedInUser(Enrolments(Set()),
                 IdentityData(Some(internalId),
                              Some("123"),
                              None,
                              Some(nrsCredentials),
                              Some(L50),
                              None,
                              None,
                              Some(Name(Some("Aldo"), Some("Rain"))),
                              Some(LocalDate.now().minusYears(25)),
                              Some("amina@hmrc.co.uk"),
                              Some(
                                AgentInformation(Some("agentId"),
                                                 Some("agentCode"),
                                                 Some("agentFriendlyName")
                                )
                              ),
                              None,
                              None,
                              None,
                              None,
                              None,
                              None,
                              None,
                              Some("credentialStrength 50"),
                              Some(LoginTimes(DateTime.now, None))
                 ),
                 featureFlags
    )

}
