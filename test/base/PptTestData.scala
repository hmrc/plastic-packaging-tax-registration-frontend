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

package base

import models.SignedInUser
import models.request.IdentityData
import uk.gov.hmrc.auth.core.retrieve.Credentials

object PptTestData {

  val nrsCredentials: Credentials =
    Credentials(providerId = "providerId", providerType = "providerType")

  def newUser(internalId: String = "Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"): SignedInUser = SignedInUser(
    IdentityData(Some(internalId), Some(nrsCredentials))
  )

}
