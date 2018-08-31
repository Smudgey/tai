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

import scala.util.Random
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.config._
import uk.gov.hmrc.tai.model.tai.TaxYear

class ApplicationUrlsSpec extends PlaySpec with MockitoSugar {

  "RtiUrls" must {
    "return the correct urls" when {
      "given argument values" in {
        val mockConfig = mock[DesConfig]
        when(mockConfig.baseURL)
          .thenReturn("")

        val sut = new RtiUrls(mockConfig)
        sut.paymentsForYearUrl(nino.nino, TaxYear(2017)) mustBe
          s"/rti/individual/payments/nino/${nino.nino}/tax-year/17-18"
      }
    }
  }

  "PdfUrls" must {
    "return the correct urls" in {
      val mockConfig = mock[PdfConfig]
      when(mockConfig.baseURL)
        .thenReturn("")

      val sut = new PdfUrls(mockConfig)

      sut.generatePdfUrl mustBe "/pdf-generator-service/generate"
    }
  }

  "PayeUrls" must {
    "return the correct urls" when {
      "given argument values" in {
        val mockConfig = mock[PayeConfig]
        when(mockConfig.baseURL)
          .thenReturn("")

        val sut = new PayeUrls(mockConfig)

        sut.carBenefitsForYearUrl(nino, TaxYear(2017)) mustBe s"/paye/${nino.nino}/car-benefits/2017"
        sut.ninoVersionUrl(nino) mustBe s"/paye/${nino.nino}/version"
        sut.removeCarBenefitUrl(nino, TaxYear(2017), 1, 2) mustBe s"/paye/${nino.nino}/benefits/2017/1/car/2/remove"
      }
    }
  }

  "FileUploadUrls" must {
    "return the correct urls" when {
      "no arguments are given" in {
        val mockConfig = mock[FileUploadConfig]
        when(mockConfig.baseURL)
          .thenReturn("")

        val sut = new FileUploadUrls(mockConfig)

        sut.envelopesUrl mustBe "/file-upload/envelopes"
        sut.routingUrl mustBe "/file-routing/requests"
      }

      "given argument values" in {
        val mockConfig = mock[FileUploadConfig]
        when(mockConfig.baseURL)
          .thenReturn("")
        when(mockConfig.frontendBaseURL)
          .thenReturn("")

        val sut = new FileUploadUrls(mockConfig)

        sut.fileUrl("123-123", "xml") mustBe "/file-upload/upload/envelopes/123-123/files/xml"
      }
    }
  }

  "CitizenDetailsUrls" must {
    "return the correct urls" when {
      "given argument values" in {
        val mockConfig = mock[CitizenDetailsConfig]
        when(mockConfig.baseURL)
          .thenReturn("")

        val sut = new CitizenDetailsUrls(mockConfig)

        sut.designatoryDetailsUrl(nino) mustBe s"/citizen-details/${nino.nino}/designatory-details"
      }
    }
  }

  "BbsiUrls" must {
    "return the correct urls" when {
      "given argument values" in {
        val mockConfig = mock[DesConfig]
        when(mockConfig.baseURL)
          .thenReturn("")

        val sut = new BbsiUrls(mockConfig)

        sut.bbsiUrl(nino, TaxYear(2017)) mustBe
          s"/pre-population-of-investment-income/nino/${nino.nino.take(8)}/tax-year/2017"
      }
    }
  }

  "TaxAccountUrls" must {
    "return the correct urls" when {
      "given argument values" in {
        val mockConfigNps = mock[NpsConfig]
        when(mockConfigNps.baseURL)
          .thenReturn("")

        val mockConfigDes = mock[DesConfig]
        when(mockConfigDes.baseURL)
          .thenReturn("")

        val sut = new TaxAccountUrls(mockConfigNps, mockConfigDes)

        sut.taxAccountUrlNps(nino, TaxYear(2017)) mustBe s"/person/${nino.nino}/tax-account/2017/calculation"
        sut.taxAccountUrlDes(nino, TaxYear(2017)) mustBe s"/pay-as-you-earn/individuals/${nino.nino}/tax-account/tax-year/2017?calculation=true"
      }
    }
  }

  "IabdUrls" must {
    "return the correct urls" when {
      "given argument values" in {
        val mockConfigNps = mock[NpsConfig]
        val mockConfigDes = mock[DesConfig]

        when(mockConfigNps.baseURL).thenReturn("")
        when(mockConfigDes.baseURL).thenReturn("")

        val sut = new IabdUrls(mockConfigNps, mockConfigDes)

        sut.iabdUrlNps(nino, TaxYear(2017)) mustBe s"/person/${nino.nino}/iabds/2017"
        sut.iabdUrlDes(nino, TaxYear(2017)) mustBe s"/pay-as-you-earn/individuals/${nino.nino}/iabds/tax-year/2017"
      }
    }

    "return the correct iabd employment url" when {
      "given argument values" in {
        val mockConfig = mock[NpsConfig]
        val mockConfigDes = mock[DesConfig]

        when(mockConfig.baseURL).thenReturn("")
        when(mockConfigDes.baseURL).thenReturn("")

        val sut = new IabdUrls(mockConfig, mockConfigDes)

        sut.iabdEmploymentUrl(nino, TaxYear(2017), 1) mustBe s"/person/${nino.nino}/iabds/2017/employment/1"
      }
    }
  }

  private val nino: Nino = new Generator(new Random).nextNino
}
