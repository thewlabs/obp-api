/**
Open Bank Project - API
Copyright (C) 2011-2019, TESOBE GmbH.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE GmbH.
Osloer Strasse 16/17
Berlin 13359, Germany

This product includes software developed at
TESOBE (http://www.tesobe.com/)

  */

package code.setup

import java.net.URI

import _root_.net.liftweb.json.JsonAST.JObject
import code.TestServer
import code.api.util.APIUtil._
import code.api.util.{APIUtil, CustomJsonFormats}
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.model.{AccountId, BankId}
import dispatch._
import net.liftweb.common.{Empty, Full}
import net.liftweb.json.JsonDSL._
import org.scalatest._

trait ServerSetup extends FeatureSpec with SendServerRequests
  with BeforeAndAfterEach with GivenWhenThen
  with BeforeAndAfterAll
  with Matchers with MdcLoggable with CustomJsonFormats with PropsReset{

  setPropsValues("migration_scripts.execute_all" -> "true")
  setPropsValues("migration_scripts.execute" -> "true")
  setPropsValues("allow_dauth" -> "true")
  setPropsValues("dauth.host" -> "127.0.0.1")
  setPropsValues("jwt.token_secret"->"your-at-least-256-bit-secret-token")
  setPropsValues("jwt.public_key_rsa" -> "src/test/resources/cert/public_dauth.pem")
  setPropsValues("transactionRequests_supported_types" -> "SEPA,SANDBOX_TAN,FREE_FORM,COUNTERPARTY,ACCOUNT,ACCOUNT_OTP,SIMPLE,CARD")
  setPropsValues("CARD_OTP_INSTRUCTION_TRANSPORT" -> "DUMMY")
  setPropsValues("api_instance_id" -> "1_final")
  setPropsValues("starConnector_supported_types" -> "mapped,internal")
  setPropsValues("connector" -> "star")
  
  val server = TestServer
  def baseRequest = host(server.host, server.port)
  val secured = APIUtil.getPropsAsBoolValue("external.https", false)
  def externalBaseRequest = (server.externalHost, server.externalPort) match {
    case (Full(h), Full(p)) if secured  => host(h, p).secure
    case (Full(h), Full(p)) if !secured => host(h, p)
    case (Full(h), Empty) if secured  => host(h).secure
    case (Full(h), Empty) if !secured => host(h)
    case (Full(h), Empty) => host(h)
    case _ => baseRequest
  }
  
  val exampleDate = DateWithSecondsExampleObject
  
  // @code.setup.TestConnectorSetup.createBanks we can know, the bankIds in test database.
  val testBankId1 = BankId(APIUtil.defaultBankId)
  val testBankId2 = BankId("testBank2")
  
 // @code.setup.TestConnectorSetup.createAccounts we can know, the accountIds in test database.
  val testAccountId1 = AccountId("testAccount1")
  val testAccountId0 = AccountId("testAccount0")
  
  val mockCustomerNumber1 = "93934903201"
  val mockCustomerNumber2 = "93934903202"
  
  val mockCustomerNumber = "93934903208565488"
  val mockCustomerId = "cba6c9ef-73fa-4032-9546-c6f6496b354a"
  
  val emptyJSON : JObject = ("error" -> "empty List")
  val errorAPIResponse = new APIResponse(400,emptyJSON, None)
  
}

trait ServerSetupWithTestData extends ServerSetup with DefaultConnectorTestSetup with DefaultUsers{

  override def beforeEach() = {
    super.beforeEach()
    wipeTestData()
    //create fake data for the tests
    //fake banks
    val banks = createBanks()
    //fake bank accounts, views, accountHolders, AccountAccess
    val accounts = createAccountRelevantResources(resourceUser1, banks)
    //fake transactions
    createTransactions(accounts)
    //fake transactionRequests
    createTransactionRequests(accounts)
    
  }

  override def afterEach() = {
    super.afterEach()
    wipeTestData()
  }

}