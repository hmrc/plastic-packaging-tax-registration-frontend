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

package base.unit

import scala.util.Random
import scala.language.postfixOps

trait CommonTestUtils {

  val alphabetAndWhitespaceChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ  "

  def randomAlphabetString(length: Int): String =
    LazyList.continually(
      alphabetAndWhitespaceChars.charAt(Random.nextInt(alphabetAndWhitespaceChars.length))
    ) take length mkString

  def randomNumericString(length: Int): String =
    (1 to length).map(_ => Random.nextInt(9)).mkString("")

}
