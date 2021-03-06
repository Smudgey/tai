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

package uk.gov.hmrc.tai.repositories

import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.tai.connectors._
import uk.gov.hmrc.tai.controllers.FakeTaiPlayApplication
import uk.gov.hmrc.tai.model.domain.response.{HodUpdateFailure, HodUpdateSuccess}
import uk.gov.hmrc.tai.model.nps2.IabdType.NewEstimatedPay
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.util.{HodsSource, MongoConstants}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random

class TaxAccountRepositorySpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with HodsSource
  with MongoConstants {

  "updateTaxCodeAmount" should {
    "update tax code amount" in {

      val mockConnector = mock[TaxAccountConnector]

      when(mockConnector.updateTaxCodeAmount(any(), any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HodUpdateSuccess))

      val SUT = createSUT(taxAccountConnector = mockConnector)

      val responseFuture = SUT.updateTaxCodeAmount(nino, TaxYear(), 1, 1, NewEstimatedPay.code, 12345)

      val result = Await.result(responseFuture, 5 seconds)
      result mustBe HodUpdateSuccess

    }

    "return an error status when amount can't be updated" in {

      val mockConnector = mock[TaxAccountConnector]

      when(mockConnector.updateTaxCodeAmount(any(), any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HodUpdateFailure))

      val SUT = createSUT(taxAccountConnector = mockConnector)

      val responseFuture = SUT.updateTaxCodeAmount(nino, TaxYear(), 1, 1, NewEstimatedPay.code, 12345)

      val result = Await.result(responseFuture, 5 seconds)
      result mustBe HodUpdateFailure

    }

    "update income" in {
      val mockConnector = mock[TaxAccountConnector]

      when(mockConnector.updateTaxCodeAmount(any(), any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HodUpdateSuccess))

      val SUT = createSUT(taxAccountConnector = mockConnector)

      val responseFuture = SUT.updateTaxCodeAmount(nino, TaxYear(), 1, 1, NewEstimatedPay.code, 12345)

      val result = Await.result(responseFuture, 5 seconds)
      result mustBe HodUpdateSuccess
    }

    "return an error status when income can't be updated" in {
      val mockConnector = mock[TaxAccountConnector]

      when(mockConnector.updateTaxCodeAmount(any(), any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HodUpdateFailure))

      val SUT = createSUT(taxAccountConnector = mockConnector)

      val responseFuture = SUT.updateTaxCodeAmount(nino, TaxYear(), 1, 1, NewEstimatedPay.code, 12345)

      val result = Await.result(responseFuture, 5 seconds)
      result mustBe HodUpdateFailure
    }
  }

  "taxAccount" must {
    "return Tax Account as Json in the response" when {
      "tax account is in cache" in {
        val taxYear = TaxYear(2017)

        val mockCacheConnector = mock[CacheConnector]

        when(mockCacheConnector.findJson(Matchers.eq(sessionId), Matchers.eq(s"$TaxAccountBaseKey${taxYear.year}")))
          .thenReturn(Future.successful(Some(taxAccountJsonResponse)))

        val sut = createSUT(cacheConnector = mockCacheConnector)
        val result = Await.result(sut.taxAccount(randomNino, taxYear), 5 seconds)

        result mustBe taxAccountJsonResponse
      }

      "tax account is NOT in cache" in {
        val taxYear = TaxYear(2017)

        val nino = randomNino

        val mockConnector = mock[TaxAccountConnector]

        val mockCacheConnector = mock[CacheConnector]

        when(mockCacheConnector.findJson(
          Matchers.eq(sessionId),
          Matchers.eq(s"$TaxAccountBaseKey${taxYear.year}"))
        ).thenReturn(Future.successful(None))

        when(mockConnector.taxAccount(Matchers.eq(nino), Matchers.eq(taxYear))(any()))
          .thenReturn(Future.successful(taxAccountJsonResponse))

        when(mockCacheConnector.createOrUpdateJson(
          Matchers.eq(sessionId),
          Matchers.eq(taxAccountJsonResponse),
          Matchers.eq(s"$TaxAccountBaseKey${taxYear.year}"))
        ).thenReturn(Future.successful(taxAccountJsonResponse))

        val sut = createSUT(taxAccountConnector = mockConnector, cacheConnector = mockCacheConnector)
        val result = Await.result(sut.taxAccount(nino, taxYear), 5 seconds)

        result mustBe taxAccountJsonResponse
      }
    }
  }

  private def randomNino: Nino = new Generator(new Random).nextNino

  private val taxAccountJsonResponse = Json.obj(
    "taxYear" -> 2017,
    "totalLiability" -> Json.obj(
      "untaxedInterest" -> Json.obj(
        "totalTaxableIncome" -> 123)),
    "incomeSources" -> Json.arr(
      Json.obj(
        "employmentId" -> 1,
        "taxCode" -> "1150L",
        "name" -> "Employer1",
        "basisOperation" -> 1),
      Json.obj(
        "employmentId" -> 2,
        "taxCode" -> "1100L",
        "name" -> "Employer2",
        "basisOperation" -> 2)))

  private val sessionId = "1212"

  private val nino: Nino = new Generator(new Random).nextNino

  private implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSession")))

  private def createSUT(
                         cacheConnector: CacheConnector = mock[CacheConnector],
                         taxAccountConnector: TaxAccountConnector = mock[TaxAccountConnector]) =
    new TaxAccountRepository(cacheConnector, taxAccountConnector) {
      override def fetchSessionId(headerCarrier: HeaderCarrier): String = sessionId
    }
}