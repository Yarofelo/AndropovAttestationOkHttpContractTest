import model.okhttp.MyInterceptor;
import model.Company;
import model.Employee;
import model.CreateUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OkHttpContractTest {
    private String BASE_URL = "https://x-clients-be.onrender.com";
    private String PATH_AUTH = "auth/login";
    private String PATH_COMPANY = "company";
    private String PATH_EMPLOYEE = "employee";
    ObjectMapper mapper = new ObjectMapper();
    OkHttpClient client;
    MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

    @BeforeEach
    public void setUp() {
        client = new OkHttpClient.Builder().addNetworkInterceptor(new MyInterceptor()).build();
    }

    @Test
    @DisplayName("Создание сотрудника")
    public void shouldCreateEmployee() throws IOException {
        String userToken = Auth();
        int idCreatedCompany = createCompany(userToken);
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder().addPathSegments(PATH_EMPLOYEE).build();
        RequestBody body = RequestBody.create(mapper.writeValueAsString(new Employee
                (649, "Yaroslav",
                        "Andropov",
                        "Yarofelo",
                        idCreatedCompany,
                        "test",
                        "test",
                        "test",
                        "test",
                        true)), APPLICATION_JSON);
        Request request = new Request.Builder().post(body).url(url).addHeader("x-client-token", userToken).build();
        Response response = client.newCall(request).execute();
        String bodyResp = response.body().string();
        assertEquals(201, response.code());
        assertEquals(1, response.headers("content-type").size());
        assertEquals("application/json; charset=utf-8", response.header("content-type"));
        assertTrue(bodyResp.startsWith("{"));
        assertTrue(bodyResp.endsWith("}"));
        assertTrue(bodyResp.contains("\"id\":"));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -999, -1, 0, 1, 999, Integer.MAX_VALUE})
    @DisplayName("Запрос списка сотрудников компании")
    public void shouldGetCompanyEmployees(int companyId) throws IOException {
        HttpUrl url = HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addPathSegments(PATH_EMPLOYEE)
                .addQueryParameter("company", Integer.toString(companyId))
                .build();
        Request request = new Request.Builder().get().url(url).build();
        Response response = client.newCall(request).execute();
        String body = response.body().string();
        assertEquals(200, response.code());
        assertEquals(1, response.headers("content-type").size());
        assertEquals("application/json; charset=utf-8", response.header("content-type"));
        assertTrue(body.startsWith("["));
        assertTrue(body.endsWith("]"));
    }

    @Test
    @DisplayName("Изменение информации о сотруднике")
    public void shouldEditInfoEmployee() throws IOException {
        String userToken = Auth();
        int idCreatedCompany = createCompany(userToken);
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder().addPathSegments(PATH_EMPLOYEE).build();
        Employee employeeForPost = new Employee
                (1, "Fill",
                        "Bugs",
                        "Edward",
                        idCreatedCompany,
                        "test@pitmail.com",
                        "http://qweerty.ru",
                        "89764563826",
                        "1994-04-01",
                        true);
        RequestBody body = RequestBody.create(mapper.writeValueAsString(employeeForPost), APPLICATION_JSON);
        Request request = new Request.Builder().post(body).url(url).addHeader("x-client-token", userToken).build();
        Response response = client.newCall(request).execute();
        Employee newEmployee = mapper.readValue(response.body().string(), Employee.class);
//        Изменнение информации о сотруднике
        HttpUrl url1 = HttpUrl.parse(BASE_URL).newBuilder()
                .addPathSegment(PATH_EMPLOYEE)
                .addPathSegment(String.valueOf(newEmployee.getId()))
                .build();
        Employee employeeToEdit =
                new Employee("Novak", "testerYO@bugmail.com", "http://ytrewq.com", "89768765544", false);
        RequestBody body1 = RequestBody.create(mapper.writeValueAsString(employeeToEdit), APPLICATION_JSON);
        Request request1 = new Request.Builder().patch(body1).url(url1).addHeader("x-client-token", userToken).build();
        Response response1 = client.newCall(request1).execute();
        String bodyString = response1.body().string();
        Employee employeeEdited = mapper.readValue(bodyString, Employee.class);
        List<String> listOfFields = Arrays.stream(bodyString.split(",")).toList();

        assertEquals(201, response1.code());
        assertEquals(employeeToEdit.getLastName(), employeeEdited.getLastName());
        assertEquals(employeeToEdit.getEmail(), employeeEdited.getEmail());
        assertEquals(employeeToEdit.getUrl(), employeeEdited.getUrl());
        assertEquals(employeeToEdit.getPhone(), employeeEdited.getPhone());
        assertEquals(employeeToEdit.getIsActive(), employeeEdited.getIsActive());
        assertEquals(10, listOfFields.size());
    }

    private String Auth() throws IOException {
        RequestBody bodyAuth = RequestBody.create("{\"username\":\"Yaroslav\",\"password\":\"123456789\"}", APPLICATION_JSON);
        HttpUrl urlAuth = HttpUrl.parse(BASE_URL).newBuilder().addPathSegments(PATH_AUTH).build();
        Request requestAuth = new Request.Builder().post(bodyAuth).url(urlAuth).build();
        Response responseAuth = client.newCall(requestAuth).execute();
        CreateUser userInfo = mapper.readValue(responseAuth.body().string(), CreateUser.class);
        return userInfo.getUserToken();
    }

    private int createCompany(String userToken) throws IOException {
        HttpUrl urlPostComp = HttpUrl.parse(BASE_URL).newBuilder().addPathSegments(PATH_COMPANY).build();
        RequestBody bodyPostComp = RequestBody
                .create("{\"name\":\"TestCompany\",\"description\":\"Company created for test\"}", APPLICATION_JSON);
        Request requestPostComp = new Request.Builder()
                .post(bodyPostComp)
                .url(urlPostComp)
                .addHeader("x-client-token", userToken)
                .build();
        Response responsePostComp = client.newCall(requestPostComp).execute();
        Company createdCompany = mapper.readValue(responsePostComp.body().string(), Company.class);
        return createdCompany.getId();
    }
}