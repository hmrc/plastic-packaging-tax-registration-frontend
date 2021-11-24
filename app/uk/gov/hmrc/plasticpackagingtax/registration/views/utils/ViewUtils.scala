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

package uk.gov.hmrc.plasticpackagingtax.registration.views.utils

import com.google.inject.{Inject, Singleton}
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{
  ActionItem,
  Actions,
  Key,
  SummaryListRow
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  AddressDetails => GroupMemberAddress
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService

@Singleton
class ViewUtils @Inject() (countryService: CountryService) {

  def summaryListRow(key: String, value: Option[String], call: Option[Call] = None)(implicit
    messages: Messages
  ): SummaryListRow =
    SummaryListRow(key = Key(content = Text(messages(key))),
                   value = Value(content = HtmlContent(value.getOrElse(""))),
                   actions = call.flatMap(
                     c =>
                       Some(
                         Actions(items =
                           Seq(
                             ActionItem(href = c.url,
                                        content = Text(messages("site.link.change")),
                                        visuallyHiddenText = Some(messages(key))
                             )
                           )
                         )
                       )
                   )
    )

  def summaryListRowWithValue(key: String, value: Value, call: Option[Call] = None)(implicit
    messages: Messages
  ): SummaryListRow =
    SummaryListRow(key = Key(content = Text(messages(key))),
                   value = value,
                   actions = call.flatMap(
                     c =>
                       Some(
                         Actions(items =
                           Seq(
                             ActionItem(href = c.url,
                                        content = Text(messages("site.link.change")),
                                        visuallyHiddenText = Some(messages(key))
                             )
                           )
                         )
                       )
                   )
    )

  def extractAddress(address: Address) =
    Seq(address.addressLine1,
        address.addressLine2.getOrElse(""),
        address.addressLine3.getOrElse(""),
        address.townOrCity,
        address.postCode.getOrElse(""),
        countryService.getName(address.countryCode)
    ).filter(_.nonEmpty).mkString("<br>")

  def extractAddress(address: GroupMemberAddress) =
    Seq(address.addressLine1,
        address.addressLine2,
        address.addressLine3.getOrElse(""),
        address.addressLine4.getOrElse(""),
        address.postalCode.getOrElse(""),
        countryService.getName(address.countryCode)
    ).filter(_.nonEmpty).mkString("<br>")

}
