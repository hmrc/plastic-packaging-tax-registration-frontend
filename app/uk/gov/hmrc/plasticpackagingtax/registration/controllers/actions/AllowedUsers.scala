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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

import com.google.inject.{Inject, ProvidedBy}
import play.api.Configuration
import uk.gov.hmrc.plasticpackagingtax.registration.config.AllowedUser

import javax.inject.Provider

@ProvidedBy(classOf[AllowedUsersProvider])
class AllowedUsers(users: Seq[AllowedUser]) {
  def isAllowed(email: String): Boolean = users.isEmpty || users.exists(user => user.email == email)
}

class AllowedUsersProvider @Inject() (configuration: Configuration) extends Provider[AllowedUsers] {

  override def get(): AllowedUsers =
    new AllowedUsers(configuration.get[Seq[AllowedUser]]("allowedUsers"))

}