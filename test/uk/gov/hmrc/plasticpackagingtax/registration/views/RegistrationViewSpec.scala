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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.{TaskName, TaskStatus}
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationViewSpec extends UnitViewSpec with Matchers {

  val taskStatuses: Map[TaskName, TaskStatus] = Map(
    TaskName.OrganisationDetails     -> TaskStatus.Completed,
    TaskName.PlasticPackagingDetails -> TaskStatus.InProgress,
    TaskName.BusinessContactDetails  -> TaskStatus.NotStarted
  )

  private val registrationPage: registration_page = instanceOf[registration_page]

  private def createView(): Html = registrationPage(taskStatuses)

  "Registration Page view" should {

    "have proper messages for labels" in {

      messages must haveTranslationFor("registrationPage.title")
      messages must haveTranslationFor("registrationPage.organisationDetails")
      messages must haveTranslationFor("registrationPage.businessInfo")
      messages must haveTranslationFor("registrationPage.plasticPackagingDetails")
      messages must haveTranslationFor("registrationPage.plasticPackagingInfo")
      messages must haveTranslationFor("registrationPage.businessContactDetails")
      messages must haveTranslationFor("registrationPage.businessContactInfo")
      messages must haveTranslationFor("registrationPage.applicantContactDetails")
      messages must haveTranslationFor("registrationPage.applicantContactInfo")
      messages must haveTranslationFor("registrationPage.declaration.header")
      messages must haveTranslationFor("registrationPage.declaration.description")
      messages must haveTranslationFor("registrationPage.declaration.buttonName")
      messages must haveTranslationFor("task.status.notStarted")
      messages must haveTranslationFor("task.status.inProgress")
      messages must haveTranslationFor("task.status.completed")
    }

    val view: Html = createView()

    "display title" in {

      view.select("title").text() must include(messages("registrationPage.title"))
    }

    "display header" in {

      view.getElementById("title") must containMessage("registrationPage.title")
    }

    "display 'list of tasks' section" in {

      val taskListElement       = view.getElementsByClass("app-task-list").get(0)
      val taskListElementHeader = taskListElement.getElementsByClass("app-task-list__section")
      val taskItems             = taskListElement.getElementsByClass("app-task-list__item")
      taskItems must haveSize(4)

      def validateTask(
        index: Int,
        title: String,
        description: String,
        tagStatus: String,
        href: Call
      ) = {
        taskListElementHeader.get(index).text() must include(messages(title))
        val taskItem = taskItems.get(index)
        taskItem.text must include(messages(description))
        taskItem.getElementsByClass("govuk-tag").text() must include(messages(tagStatus))
        taskItem.getElementsByClass("govuk-link").get(0) must haveHref(href)
      }

      validateTask(0,
                   "registrationPage.organisationDetails",
                   "registrationPage.businessInfo",
                   "task.status.completed",
                   routes.HonestyDeclarationController.displayPage()
      )
      validateTask(1,
                   "registrationPage.plasticPackagingDetails",
                   "registrationPage.plasticPackagingInfo",
                   "task.status.inProgress",
                   routes.LiabilityStartDateController.displayPage()
      )
      validateTask(2,
                   "registrationPage.businessContactDetails",
                   "registrationPage.businessContactInfo",
                   "task.status.notStarted",
                   routes.RegistrationController.displayPage()
      )
      validateTask(3,
                   "registrationPage.applicantContactDetails",
                   "registrationPage.applicantContactInfo",
                   "task.status.notStarted",
                   routes.RegistrationController.displayPage()
      )
    }

    "review declaration section" in {
      view.getElementById("review-declaration").text() must include(
        messages("registrationPage.declaration.header")
      )
      view.getElementById("review-declaration-element").text() must include(
        messages("registrationPage.declaration.description")
      )
    }

    "display 'Review Declaration' button" in {

      view.getElementsByClass("govuk-button").first() must containMessage(
        "registrationPage.declaration.buttonName"
      )
      view.getElementsByClass("govuk-button").first() must haveHref("")
    }
  }
}
