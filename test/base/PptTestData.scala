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

package base

import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, LoginTimes, Name}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.SignedInUser
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.IdentityData

object PptTestData {

  val nrsCredentials = Credentials(providerId = "providerId", providerType = "providerType")

  def newUser(externalId: String, pptEnrolmentId: String): SignedInUser =
    SignedInUser(
      Enrolments(
        Set(
          Enrolment(AuthAction.pptEnrolmentKey).withIdentifier(
            AuthAction.pptEnrolmentIdentifierName,
            pptEnrolmentId
          )
        )
      ),
      IdentityData(Some("Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"),
                   Some(externalId),
                   None,
                   Some(nrsCredentials),
                   Some(L50),
                   None,
                   None,
                   Some(Name(Some("Aldo"), Some("Rain"))),
                   Some(LocalDate.now().minusYears(25)),
                   Some("amina@hmrc.co.uk"),
                   Some(
                     AgentInformation(Some("agentId"), Some("agentCode"), Some("agentFriendlyName"))
                   ),
                   None,
                   None,
                   None,
                   None,
                   None,
                   None,
                   None,
                   Some("crdentialStrength 50"),
                   Some(LoginTimes(DateTime.now, None))
      )
    )

}
