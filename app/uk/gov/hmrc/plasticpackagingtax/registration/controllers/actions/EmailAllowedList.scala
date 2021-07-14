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

import javax.inject.Provider

@ProvidedBy(classOf[EmailAllowedListProvider])
class EmailAllowedList(emails: Seq[String]) {
  def isAllowed(email: String): Boolean = emails.isEmpty || emails.contains(email)
}

class EmailAllowedListProvider @Inject() (configuration: Configuration)
    extends Provider[EmailAllowedList] {

  override def get(): EmailAllowedList =
    new EmailAllowedList(configuration.get[Seq[String]]("allowedList.emails").map(_.trim))

}
