/*
 * Copyright 2023 HM Revenue & Customs
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

package repositories

import config.AppConfig
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsDefined, JsObject, JsPath, JsString}
import repositories.ReturnsSessionRepository.Entry
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class ReturnsSessionRepositorySpec extends AnyFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[Entry]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar
  with BeforeAndAfterAll {

  val entryValue: JsObject = JsObject.apply(Seq("key" -> JsString("value")))
  private val entry = Entry("1234", JsObject.apply(Seq("path" -> entryValue)), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.returnsSessionCacheUri).thenReturn("mongodb://localhost:27017/plastic-packaging-tax-registration-frontend")

  protected override val repository = new ReturnsSessionRepository(
    appConfig = mockAppConfig
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    deleteAll()
  }

  override def afterAll(): Unit = {
    deleteAll()
    super.afterAll()
  }

  ".get" - {
    "when there is no record for this id" - {
      "must return None" in {

        repository.get[String](entry.id, JsPath).futureValue must not be defined
      }
    }

    "when there is a record for this id" - {
      "must return Some(entry)" in {
        insert(entry).futureValue

        val result = repository.get[JsObject](entry.id, JsPath).futureValue
        result mustBe defined
        result.get \ "path" \ "key" mustBe JsDefined(JsString("value"))
      }
    }
  }

}