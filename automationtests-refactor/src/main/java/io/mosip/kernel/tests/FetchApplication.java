
package io.mosip.kernel.tests;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Verify;

import io.mosip.kernel.service.ApplicationLibrary;
import io.mosip.kernel.service.AssertKernel;
import io.mosip.kernel.util.CommonLibrary;
import io.mosip.kernel.util.KernelAuthentication;
import io.mosip.kernel.util.KernelDataBaseAccess;
import io.mosip.kernel.util.TestCaseReader;
import io.mosip.service.BaseTestCase;
import io.restassured.response.Response;

/**
 * @author Ravi Kant
 *
 */
public class FetchApplication extends BaseTestCase implements ITest {

	FetchApplication() {
		super();
	}

	private static Logger logger = Logger.getLogger(FetchApplication.class);
	private final String moduleName = "kernel";
	private final String apiName = "FetchApplication";
	private final Map<String, String> props = new CommonLibrary().readProperty("Kernel");
	private final String FetchApplication_URI = props.get("FetchApplication_URI").toString();
	private final String FetchApplication_lang_URI = props.get("FetchApplication_lang_URI").toString();
	private final String FetchApplication_id_lang_URI = props.get("FetchApplication_id_lang_URI").toString();

	protected String testCaseName = "";
	SoftAssert softAssert = new SoftAssert();
	boolean status = false;
	public JSONArray arr = new JSONArray();
	Response response = null;
	JSONObject responseObject = null;
	private AssertKernel assertions = new AssertKernel();
	private ApplicationLibrary applicationLibrary = new ApplicationLibrary();
	KernelAuthentication auth=new KernelAuthentication();
	String cookie=null;

	/**
	 * method to set the test case name to the report
	 * 
	 * @param method
	 * @param testdata
	 * @param ctx
	 */
	@BeforeMethod(alwaysRun=true)
	public  void getTestCaseName(Method method, Object[] testdata, ITestContext ctx) throws Exception {
		String object = (String) testdata[0];
		testCaseName = moduleName+"_"+apiName+"_"+object.toString();
		cookie=auth.getAuthForIndividual();
	}

	/**
	 * This data provider will return a test case name
	 * 
	 * @param context
	 * @return test case name as object
	 */
	@DataProvider(name = "fetchData")
	public Object[][] readData(ITestContext context)
			throws JsonParseException, JsonMappingException, IOException, ParseException {
		return new TestCaseReader().readTestCases(moduleName + "/" + apiName, testLevel);
	}

	/**
	 * This fetch the value of the data provider and run for each test case
	 * 
	 * @param testcaseName
	 * @param object
	 * @throws ParseException 
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProvider = "fetchData", alwaysRun = true)
	public void fetchApplication(String testcaseName) throws ParseException{
		logger.info("Test Case Name:" + testcaseName);

		// getting request and expected response jsondata from json files.
		JSONObject objectDataArray[] = new TestCaseReader().readRequestResponseJson(moduleName, apiName, testcaseName);

		JSONObject objectData = objectDataArray[0];
		responseObject = objectDataArray[1];
				if (objectData.containsKey("code"))
					response = applicationLibrary.getWithPathParam(FetchApplication_id_lang_URI, objectData,cookie);
				else
					response = applicationLibrary.getWithPathParam(FetchApplication_lang_URI, objectData,cookie);


		// sending request to get request without param
		if (response == null) {
			response = applicationLibrary.getWithoutParams(FetchApplication_URI,cookie);
			objectData = null;
		}

		if (testcaseName.toLowerCase().contains("smoke")) {

			String queryPart = "select count(*) from master.appl_form_type  where is_active = true";

			String query = queryPart;
			if (objectData != null) {
				if (objectData.containsKey("code"))
					query = query + " and code = '" + objectData.get("code") + "' and lang_code = '"
							+ objectData.get("langcode") + "'";
				else
					query = queryPart + " and lang_code = '" + objectData.get("langcode") + "'";
			}
			long obtainedObjectsCount = new KernelDataBaseAccess().validateDBCount(query,"masterdata");

			// fetching json object from response
			JSONObject responseJson = (JSONObject) ((JSONObject) new JSONParser().parse(response.asString())).get("response");
			// fetching json array of objects from response
			JSONArray responseArrayFromGet = (JSONArray) responseJson.get("applicationtypes");
			logger.info("===Dbcount===" + obtainedObjectsCount + "===Get-count===" + responseArrayFromGet.size());
			
			// validating number of objects obtained form db and from get request
			if (responseArrayFromGet.size() == obtainedObjectsCount) {

				// list to validate existance of attributes in response objects
				List<String> attributesToValidateExistance = new ArrayList<String>();
				attributesToValidateExistance.add("code");
				attributesToValidateExistance.add("description");
				attributesToValidateExistance.add("isActive");

				// key value of the attributes passed to fetch the data, should be same in all obtained objects
				HashMap<String, String> passedAttributesToFetch = new HashMap<String, String>();
				if (objectData != null) {
					if (objectData.containsKey("code")) {
						passedAttributesToFetch.put("code", objectData.get("code").toString());
						passedAttributesToFetch.put("langCode", objectData.get("langcode").toString());
					}
					passedAttributesToFetch.put("langCode", objectData.get("langcode").toString());
				}

				status = AssertKernel.validator(responseArrayFromGet, attributesToValidateExistance, passedAttributesToFetch);
			} else
				status = false;
		}

		else {
			// add parameters to remove in response before comparison like time stamp
			ArrayList<String> listOfElementToRemove = new ArrayList<String>();
			listOfElementToRemove.add("responsetime");
			status = assertions.assertKernel(response, responseObject, listOfElementToRemove);
		}

		if (!status) {
			logger.debug(response);
		}
		Verify.verify(status);
		softAssert.assertAll();
	}

	@Override
	public String getTestName() {
		return this.testCaseName;
	}

	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, testCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}


}