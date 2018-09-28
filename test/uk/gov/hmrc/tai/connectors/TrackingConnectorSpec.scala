/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.connectors

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json.parse
import play.api.libs.json.{JsResultException, JsValue}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.tai.model.domain.tracking.{TrackedForm, TrackedFormReceived}

import scala.concurrent.Future
import scala.util.Random

class TrackingConnectorSpec extends PlaySpec with MockitoSugar {
  val urlProvider: TrackingUrl = mock[TrackingUrl]
  val httpClient: HttpClient= mock[HttpClient]

  val nino: Nino = new Generator(new Random).nextNino
  val trackingUrl = "trackingUrl"

  val sut = new TrackingConnector(httpClient, urlProvider)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getUserTracking" should {
    "fetch the user tracking details" when {
      "provided with id and idType" in {
        val trackedFormJson: JsValue = parse(
          """{
           "submissions": [
             {
               "formId": "formId",
               "formName": "myForm",
               "dfsSubmissionReference": "dfsSubmissionReference",
               "milestones" : [
                 {
                   "milestone": "Received",
                   "status": "current"
                 }
               ]
             }
           ]
         }""" )

        val fakeResponse: HttpResponse = HttpResponse(200, Some(trackedFormJson))

        when(urlProvider.trackingUrl(nino.nino)).thenReturn(trackingUrl)
        when(httpClient.doGet(trackingUrl)).thenReturn(Future successful fakeResponse)

        await(sut.getUserTracking(nino.nino)) mustBe Seq(TrackedForm("formId", "myForm", TrackedFormReceived))
      }
    }

    "throw exception" when {
      "json is not valid" in {
        val fakeResponse: HttpResponse = HttpResponse(200, Some(parse("""{}""")))

        when(urlProvider.trackingUrl(nino.nino)).thenReturn(trackingUrl)
        when(httpClient.doGet(trackingUrl)).thenReturn(Future successful fakeResponse)

        intercept[JsResultException]{
          await(sut.getUserTracking(nino.nino))
        }
      }
    }
  }
}
