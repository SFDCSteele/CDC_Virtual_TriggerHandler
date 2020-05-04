package salesforce_rest;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.*;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONException;


public class CICDRestClient {

    static List<String> LOGIN_VARS = new ArrayList<String>();
    static List<String> LOGIN_VALS = new ArrayList<String>();
    static List<String> BUILDSTATUS_VARS = new ArrayList<String>();
    static List<String> BUILDSTATUS_MSG  = new ArrayList<String>();
    static List<String> BUILDSTATUS_VALS = new ArrayList<String>();

    private static final String RUH_VERSION       = "1.05";
    private static final int USERNAME_LOC = 0;
    private static final int PASSWORD_LOC = 1;
    private static final int LOGINURL_LOC = 2;
    private static final int GRANTSERVICE_LOC = 3;
    private static final int CLIENTID_LOC = 4;
    private static final int CLIENTSECRET_LOC = 5;
    private static final String USERNAME = "";
    private static final String PASSWORD = "";
    private static final String LOGINURL = "";
    private static final String GRANTSERVICE = "";
    private static final String CLIENTID = "";
    private static final String CLIENTSECRET = "";
    private static String REST_ENDPOINT = "/services/apexrest/";
    private static String API_VERSION = "/v45.0";
    private static String baseUri;
    private static Header oauthHeader;
    private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    private static String leadId;
    private static String leadFirstName;
    private static String leadLastName;
    private static String leadCompany;

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        System.out.println("==========================================\nCICDRestClient version: " + RUN_VERSION +
                " run option: " + args[0] + "\n==========================================");
        loadEnvironmentVars("LOGIN");
        HttpClient httpclient = HttpClientBuilder.create().build();
        
        // Assemble the login request URL
        String loginURL = LOGIN_VALS.get(LOGINURL_LOC) +
                          LOGIN_VALS.get(GRANTSERVICE_LOC) +
                          "&client_id=" + LOGIN_VALS.get(CLIENTID_LOC) +
                          "&client_secret=" + LOGIN_VALS.get(CLIENTSECRET_LOC) +
                          "&username=" + LOGIN_VALS.get(USERNAME_LOC) +
                          "&password=" + LOGIN_VALS.get(PASSWORD_LOC);
 
        // Login requests must be POSTs
        HttpPost httpPost = new HttpPost(loginURL);
        HttpResponse response = null;
 
        try {
            // Execute the login POST request
            response = httpclient.execute(httpPost);
        } catch (ClientProtocolException cpException) {
            cpException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
 
        // verify response is HTTP OK
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            System.out.println("Error authenticating to Force.com: "+statusCode);
            // Error is in EntityUtils.toString(response.getEntity())
            return;
        }
 
        String getResult = null;
        try {
            getResult = EntityUtils.toString(response.getEntity());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
 
        JSONObject jsonObject = null;
        String loginAccessToken = null;
        String loginInstanceUrl = null;
 
        try {
            jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
            loginAccessToken = jsonObject.getString("access_token");
            loginInstanceUrl = jsonObject.getString("instance_url");
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
 
        if ( args[0].equals("buildStatus")) {
            REST_ENDPOINT = "/services/apexrest/buildUpdate" ;
        } else if (args[0].equals("submitPackage")) {
            REST_ENDPOINT = "/services/apexrest/packageUpdate" ;
        }

        baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION ;
        oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken) ;
        System.out.println("oauthHeader1: " + oauthHeader);
        System.out.println("\n" + response.getStatusLine());
        System.out.println("Successful login");
        System.out.println("instance URL: "+loginInstanceUrl);
        System.out.println("access token/session ID: "+loginAccessToken);
        System.out.println("baseUri: "+ baseUri);        

        if ( args[0].equals("buildStatus")) {
            submitBuildStatus();
        } else if (args[0].equals("submitPackage")) {
            submitPackage(args);
        }
 
        // release connection
        httpPost.releaseConnection();
        
    }

    private static void loadEnvironmentVars(String opt) {

        if (opt.equals("LOGIN")) {
            LOGIN_VARS.add("CICD_USERNAME");
            LOGIN_VARS.add("CICD_PASSWORD");
            LOGIN_VARS.add("CICD_LOGINURL");
            LOGIN_VARS.add("CICD_GRANTSERVICE");
            LOGIN_VARS.add("CICD_CLIENTID");
            LOGIN_VARS.add("CICD_CLIENTSECRET");
            for (int i=0;i<LOGIN_VARS.size();i++ ) {
                LOGIN_VALS.add((String)System.getenv(LOGIN_VARS.get(i)));
            }
        } else if (opt.equals("BUILDSTATUS")) {

            BUILDSTATUS_VARS.add("envName");//0
            BUILDSTATUS_VARS.add("CI_BUILD_URL");//1
            BUILDSTATUS_VARS.add("CI_BUILD_ID");//2
            BUILDSTATUS_VARS.add("CI_STATUS");//3
            BUILDSTATUS_VARS.add("CI_NAME");//4
            BUILDSTATUS_VARS.add("CI_REPO_NAME");//5
            BUILDSTATUS_VARS.add("CI_COMMIT_ID");//6
            BUILDSTATUS_VARS.add("CI_MESSAGE");//7
            BUILDSTATUS_VARS.add("CI_COMMITTER_NAME");//8
            BUILDSTATUS_VARS.add("CI_BRANCH");//9
            for (int i=0;i<BUILDSTATUS_VARS.size();i++ ) {
                if ( i==7) {
                    BUILDSTATUS_VALS.add((String)System.getenv(BUILDSTATUS_VARS.get(i)).replaceAll("\\r?\\n\\'","\""));
                } else {
                    BUILDSTATUS_VALS.add((String)System.getenv(BUILDSTATUS_VARS.get(i)));
                }
                System.out.println ("loadEnvironmentVars:BUILDSTATUS_VARS: "+BUILDSTATUS_VARS.get(i)+" value: "+BUILDSTATUS_VALS.get(i));
            }
        }

    }

    
    // Submit buildStatus using REST HttpPost
    public static void submitBuildStatus() {
        System.out.println("\n_______________ Build Status _______________");
 
        String uri = baseUri + "/services/apexrest/buildUpdate/saveBuildDetails";

        loadEnvironmentVars("BUILDSTATUS");
        try {
 
        	String buildStatus = "{\"build\": {" + 
                    "\"envName\":\""+BUILDSTATUS_VALS.get(0)+"\"," + 
                    "\"build_url\":\""+BUILDSTATUS_VALS.get(1)+"\"," + 
        			"\"commit_url\":\""+BUILDSTATUS_VALS.get(1)+"\"," + 
        			"\"project_id\":"+BUILDSTATUS_VALS.get(2)+"," + 
        			"\"build_id\":"+BUILDSTATUS_VALS.get(2)+"," + 
        			"\"status\":\""+BUILDSTATUS_VALS.get(3)+"\"," + 
        			"\"project_name\":\""+BUILDSTATUS_VALS.get(4)+"\"," + 
        			"\"commit_id\":\""+BUILDSTATUS_VALS.get(6)+"\"," + 
        			"\"short_commit_id\":\""+BUILDSTATUS_VALS.get(2)+"\"," + 
        			"\"message\":\""+BUILDSTATUS_VALS.get(7)+"\"," + 
        			"\"committer\":\""+BUILDSTATUS_VALS.get(8)+"\"," + 
        			"\"branch\":\""+BUILDSTATUS_VALS.get(9)+"\"}}";
 
            System.out.println("Build to be inserted:\n" + buildStatus);
 
            //Construct the objects needed for the request
            HttpClient httpClient = HttpClientBuilder.create().build();
 
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader(oauthHeader);
            httpPost.addHeader(prettyPrintHeader);
            // The message we are going to post
            StringEntity body = new StringEntity(buildStatus);
            body.setContentType("application/json");
            httpPost.setEntity(body);
 
            //Make the request
            HttpResponse response = httpClient.execute(httpPost);
 
            //Process the results
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                String response_string = EntityUtils.toString(response.getEntity());
                System.out.println("New build status update from response: " + response_string);
            } else {
                System.out.println("Insertion unsuccessful. Status code returned is " + statusCode);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }
    
    // Submit package contents using REST HttpPost
    public static void submitPackage(String[] args) {
        System.out.println("\n_______________ package contents _______________");
 
        String uri = baseUri + "/services/apexrest/packageUpdate/savePackageXML";

        try {
 
            String packageXML = loadPackageXML (args[1]);
 
            System.out.println("package.xml to be saved:\n" + packageXML);
 
            //Construct the objects needed for the request
            HttpClient httpClient = HttpClientBuilder.create().build();
 
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader(oauthHeader);
            httpPost.addHeader(prettyPrintHeader);
            // The message we are going to post
            StringEntity body = new StringEntity(packageXML);
            body.setContentType("application/json");
            httpPost.setEntity(body);
 
            //Make the request
            HttpResponse response = httpClient.execute(httpPost);
 
            //Process the results
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                String response_string = EntityUtils.toString(response.getEntity());
                System.out.println("New package submission from response: " + response_string);
            } else {
                System.out.println("Insertion unsuccessful. Status code returned is " + statusCode);
            }
            
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    private static String loadPackageXML (String packageName) {

        BufferedReader reader;
        
        List<String> members = new ArrayList<String>();
        boolean nameFound = false;
        boolean memberFound = false;
        boolean firstName = true;
        List<String> existingMembers = new ArrayList<String>();
        Map<String,List<String>> nameMap = new TreeMap<String,List<String>>();
        String mapName      = "";
		String returnMessage = "{";

        try {
            File inpuFile = new File(packageName);
            FileReader fileReader = new FileReader(packageName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            List<String> lines = new ArrayList<String>();
            String line = null;
            returnMessage += 
            "                \"packageName\" : \""+inpuFile.getName()+"\","+
            "                \"packageTypes\" : [";
            while ((line = bufferedReader.readLine()) != null) {
                //System.out.println("reading line: "+line);
                if ( line.contains("<members>")) {
                    members.add(line.split("<members>")[1].split("</members>")[0]);
                } else if ( line.contains("<name>")) {
                    nameFound   = false;
                    mapName     = line.split("<name>")[1].split("</name>")[0];
                    if ( !firstName ) {
                        returnMessage += ",";
                        firstName = false;
                    }
                    if(nameMap.containsKey(mapName)) {
                        nameFound = true;
                        memberFound = false;
                        existingMembers = nameMap.get(mapName);
                        for (int k=0;k<existingMembers.size(); k++ ) {
                            for ( int l=0;l<members.size();l++ ) {
                                if ( existingMembers.get(k) == members.get(l) ) {
                                    memberFound = true;
                                    break;
                                }
                            }
                            if ( !memberFound ) {
                                /*if ( nameMap[j].name == "Flow" && flowExists('getFileContents',nameMap[j].name,existingMembers[k]+".flow") ) {
                                    java.lang.System.out.println("skipping flow member from nameMap: name: "+nameMap[j].name+
                                                " existing member: "+existingMembers[k]);
                                } else {
                                    members.push(existingMembers[k]);
                                }*/
                                members.add(existingMembers.get(k));
                            } else {
                            }
                            memberFound = false;
                        }//for
                        nameMap.put(mapName,members);
                        members = new ArrayList<String>();
                        break;
                    }
                    if ( !nameFound) {
                        nameMap.put(mapName,members);
                        returnMessage += 
                        "                    {\"packageType\" : \""+mapName+"\", \"packageMembers\" : [";
                        for ( int i=0;i<members.size();i++ ) {
                            if ( i > 0 ) {
                                returnMessage += ",";
                            }
                            returnMessage += "\""+members.get(i)+"\"";
                        }
                        returnMessage += "] }";
                    }
                    members = new ArrayList<String>();
                }//if
                        
            }//while
            lines.add(line);
            bufferedReader.close();

            //finish building the JSON
            returnMessage += "] }";
    
        } catch (IOException ioe) {
            System.out.println("Could not Read file: "+ioe.getMessage());
        }
        return returnMessage;
    }

 
    private static String getBody(InputStream inputStream) {
        String result = "";
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(inputStream)
            );
            String inputLine;
            while ( (inputLine = in.readLine() ) != null ) {
                result += inputLine;
                result += "\n";
            }
            in.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return result;
    }



}
