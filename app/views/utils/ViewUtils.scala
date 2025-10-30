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

package views.utils

import com.google.inject.{Inject, Singleton}
import forms.contact.Address
import forms.{Date, OldDate}
import play.api.i18n.Messages
import play.api.mvc.Call
import services.CountryService
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryListRow}

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.util.Try

@Singleton
class ViewUtils @Inject() (countryService: CountryService) {

  def summaryListRow(key: String, value: Option[String], call: Option[Call] = None)(implicit
    messages: Messages
  ): SummaryListRow =
    SummaryListRow(
      key = Key(content = Text(messages(key))),
      value = Value(content = HtmlContent(value.getOrElse(""))),
      actions = call.flatMap(c =>
        Some(
          Actions(items =
            Seq(
              ActionItem(
                href = c.url,
                content = Text(messages("site.link.change")),
                visuallyHiddenText = Some(messages(key))
              )
            )
          )
        )
      )
    )

  def extractAddress(address: Address): String =
    Seq(
      address.addressLine1,
      address.addressLine2.getOrElse(""),
      address.addressLine3.getOrElse(""),
      address.townOrCity,
      address.maybePostcode.getOrElse(""),
      countryService.tryLookupCountryName(address.countryCode)
    ).filter(_.nonEmpty).mkString("<br>")

  def showChangeLink(call: Call): Option[Call] =
    Some(call)

  def displayOldDate(date: Option[OldDate])(implicit messages: Messages): Option[String] =
    displayLocalDate(date.map(_.asLocalDate))

  def displayDate(date: Option[Date])(implicit messages: Messages): Option[String] =
    displayLocalDate(date.map(_.date))

  def displayLocalDate(date: Option[LocalDate])(implicit messages: Messages): Option[String] =
    date.map(displayLocalDate)

  def displayLocalDate(date: LocalDate)(implicit messages: Messages): String =
    s"${date.getDayOfMonth} ${getDateMonth(date)} ${date.getYear}"

  def displayLocalDateStr(optDate: Option[String])(implicit messages: Messages): Option[String] = {
    optDate.flatMap { date =>
      Try(LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate)
        .recover { case _: Exception =>
          ZonedDateTime.parse(date).toLocalDate
        }
        .toOption
    }
      .map(displayLocalDate(_))
  }

  private def getDateMonth(date: LocalDate)(implicit messages: Messages): String =
    messages(s"date.month.${date.getMonthValue}")

}
