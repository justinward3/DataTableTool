import java.io.*;
import java.net.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.util.*;



public class App {
    private final String USER_AGENT = "Mozilla/5.0";
    public static final String[] language = {"En", "Fr"};
    public static String currLanguage = "";

    //returns title of table
    public String getTitle(JSONObject jsonObject, int lang) {
        return (String) jsonObject.get("cubeTitle" + currLanguage);
    }

    //returns previous table ids
    public String getCansimId(JSONObject jsonObject) {
        return (String) jsonObject.get("cansimId");
    }

    //returns and ArrayList of Strings for Subject,Survey,Classification Code
    public ArrayList<String> get(JSONObject jsonObject, JSONObject codeset, String type) {
        try {
            ArrayList<String> temparr = (ArrayList<String>) jsonObject.get(type + "Code");

            String tempstr;
            for (int i = 0; i < temparr.size(); i++) {
                tempstr = MatchCode(codeset, (String) temparr.get(i), type);
                temparr.set(i, tempstr);
            }
            return temparr;
        } catch (ClassCastException e) {
            ArrayList<String> temparr = new ArrayList<String>();
            String temp = (String) jsonObject.get(type + "Code");
            String tempstr;
            tempstr = MatchCode(codeset, (String) temp, type);
            temparr.add(tempstr);
            return temparr;
        }
    }

    //returns an ArrayList of JSONObject which are dimensions
    public LinkedHashMap<String, ArrayList<Node<JSONObject>>> getTree(JSONObject jsonObject) {
        //System.out.println(jsonObject);
        LinkedHashMap<String,ArrayList<Node<JSONObject>>> MemberMap = new LinkedHashMap<String, ArrayList<Node<JSONObject>>>();
        ArrayList<JSONObject> dimensions = (ArrayList<JSONObject>) jsonObject.get("dimension");
        for (JSONObject dimension : dimensions){
            ArrayList<JSONObject> members = (ArrayList<JSONObject>) dimension.get("member");
            Node<JSONObject> root = new Node<JSONObject>(null);
            ArrayList<Node<JSONObject>> roots = new ArrayList<Node<JSONObject>>();
            for (JSONObject temp: members){
                if(temp.get("parentMemberId")==null){
                    Node<JSONObject> newNode = new Node<JSONObject>(temp);
                    root = new Node<JSONObject>(temp);
                    roots.add(root);
                    MemberMap.put((String)dimension.get("dimensionName" + currLanguage),roots);
                }
                else if(temp.get("parentMemberId")!=null){
                    Node<JSONObject> newNode = new Node<JSONObject>(temp);
                    root.addChild(root,(long)temp.get("parentMemberId"), newNode);
                }
            }
        }
        return MemberMap;
    }

    //converts Dimension JSONObject to String
    public ArrayList<String> convert(ArrayList<Node<JSONObject>> arr) {
        JSONObject temp = new JSONObject();
        ArrayList<String> temparrlist = new ArrayList<String>();
        for (Node<JSONObject> json : arr){
            temparrlist.add((String)((JSONObject)json.getData()).get("memberName"+currLanguage));
        }
        return temparrlist;
    }

    //Search function for MemberMap and InnerNodeStructures
    public String Search(String NodeStructureName, LinkedHashMap<String,ArrayList<Node<JSONObject>>> MemberMap) {
        for(String key : MemberMap.keySet()){
            //System.out.println(NodeStructureName);
            //System.out.println("Inner Key: "+key);
            for(Node<JSONObject> root : MemberMap.get(key)){
                System.out.println(root.Search(root,NodeStructureName, currLanguage));
                if(root.Search(root, NodeStructureName, currLanguage) != null){
                    return key;
                }
            }
        }
        return null;
    }

    //returns first member of outer dimension
    public ArrayList<Node<JSONObject>> getInnerDim(LinkedHashMap<String,ArrayList<Node<JSONObject>>> MemberMap, String key) {
        return MemberMap.get(key);
    }

    //function to send POST request and return the response JSON
    public JSONObject sendPostRequest(String passedURL, int passedId) {
        JSONObject response_JSON = null;
        try {
            //create URL object
            URL obj = new URL(passedURL);
            //Open connection
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            //Set URL Option
            con.setRequestMethod("POST");
            //Set Headers
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US;en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            //Open Bufferwriter
            BufferedWriter httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
            //Create json object and array
            JSONObject json = new JSONObject();
            JSONArray json_array = new JSONArray();
            //Put the parameters into json and put the json into json array
            json.put("productId", passedId);
            json_array.add(json);
            //System.out.println(json_array);
            //Write to HTTP Body
            httpRequestBodyWriter.write(json_array.toString());
            httpRequestBodyWriter.close();
            //System.out.println("Status Code:" + String.valueOf(status));
            //Get Response, store in buffer
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            //Convert buffer to string
            String response_string = content.toString();
            //Create the JSONParser object
            org.json.simple.parser.JSONParser jsonParser = new org.json.simple.parser.JSONParser();
            //Parse the JSONArray
            JSONArray response_array = (JSONArray) jsonParser.parse(response_string);
            //System.out.println(response_array);
            //Convert Response Array to JSON Object
            response_JSON = (JSONObject) (response_array.get(0));
            String tempstatus = (String) response_JSON.get("status");
            if (tempstatus != "FAILED") {
                //System.out.println("Response:");
                JSONObject response_object = (JSONObject) response_JSON.get("object");
                return response_object;
            }
            //Catch for error handling
        } catch (MalformedURLException e) {
            System.out.println("EXCEPTION: " + e);
        } catch (IOException e) {
            System.out.println("EXCEPTION:" + e);
        } catch (ParseException e) {
            System.out.println("EXCEPTION:" + e);
        } catch (ClassCastException e) {
            JSONObject temp = new JSONObject();
            temp.put("Status", "Failed");
            return temp;
        }
        //return the final JSON
        return (response_JSON);
    }

    //function to GET the CodeSets
    public JSONObject getCodeSets() {
        //create URL object
        URL obj = null;
        try {
            obj = new URL("https://www150.statcan.gc.ca/t1/wds/rest/getCodeSets");
            //Open connection
            HttpsURLConnection con = null;
            con = (HttpsURLConnection) obj.openConnection();

            //Set URL Option
            con.setRequestMethod("GET");

            //Set Headers
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US;en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            //Store response code and print
            int status = con.getResponseCode();
            //Get Response, store in buffer
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            //Convert buffer to string
            String response_string = content.toString();
            //Create the JSONParser object
            org.json.simple.parser.JSONParser jsonParser = new org.json.simple.parser.JSONParser();
            //Parse the JSONArray
            //Convert Response Array to JSON Object
            JSONObject response_JSON = null;
            try {
                response_JSON = (JSONObject) jsonParser.parse(response_string);
                JSONObject response_Object = (JSONObject) response_JSON.get("object");
                return response_Object;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            System.out.println("EXCEPTION: MalformedURL");
        } catch (IOException e) {
            System.out.println("EXCEPTION: IOException");
            System.out.println("EXCEPTION:" + e);
        }
        return null;
    }

    public void SavetoDoc(Boolean Levels, JSONObject MatchCode, LinkedHashMap<String,ArrayList<Node<JSONObject>>> MemberMap, String id, String title){
        BufferedWriter writer = null;
        try {
            //create a temporary file
            File logFile;
            if(Levels){
                logFile = new File(id+"_L_"+currLanguage+".txt");
            }
            else{
                logFile = new File(id+"_"+currLanguage+".txt");
            }
            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(id +" , "+title);
            writer.newLine();
            writer.newLine();
            for(String dim : MemberMap.keySet()){
                writer.newLine();
                writer.newLine();
                writer.write(dim + " : Dimension" );
                for(Node<JSONObject> root : MemberMap.get(dim)){
                        root.Print(Levels,(JSONArray) MatchCode.get("uom"),writer,root,"\t",currLanguage,0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }
    }

    public void SavetoDoc(Boolean Levels, JSONObject MatchCode, LinkedHashMap<String,ArrayList<Node<JSONObject>>> MemberMap, String id, String title, LinkedHashMap<Long,HashMap<Long,String>> FootnoteMap){
        BufferedWriter writer = null;
        try {
            //create a temporary file
            //System.out.println(System.getProperty("user.dir"));
            File logFile;
            if(Levels){
                logFile = new File(id+"_LF_"+currLanguage+".txt");
            }
            else{
                logFile = new File(id+"_F_"+currLanguage+".txt");
            }
            //logFile.getParentFile().mkdirs();
            //logFile.createNewFile();
            // This will output the full path where the file will be written to...
            //System.out.println(logFile.getCanonicalPath());

            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(id +" , "+title);
            writer.newLine();
            writer.newLine();
            long i=1;
            for(String dim : MemberMap.keySet()){
                writer.newLine();
                writer.newLine();
                writer.write(dim + " : Dimension" );
                for(Node<JSONObject> root : MemberMap.get(dim)){
                    if(FootnoteMap.containsKey(i)) {
                        //System.out.println("FNM : " + FootnoteMap.get(i));
                        root.Print(Levels,(JSONArray) MatchCode.get("uom"),writer, root, "\t", currLanguage, 0, FootnoteMap.get(i));
                    }
                    else{
                        root.Print(Levels,(JSONArray) MatchCode.get("uom"),writer,root,"\t",currLanguage,0);
                    }
                }
                i+=1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }
    }

    //function to match codes with the CodeSets
    public String MatchCode(JSONObject jsonSets, String code, String type) {
        JSONArray needed_CodeSet = (JSONArray) jsonSets.get(type);
        for (int i = 0; i < needed_CodeSet.size(); i++) {
            JSONObject temp = (JSONObject) needed_CodeSet.get(i);
            try {
                int subjectCode = Integer.valueOf((String) temp.get(type + "Code"));
                if (subjectCode == Integer.valueOf(code)) {
                    String foundStr = (String) (temp.get(type + currLanguage));
                    return foundStr;
                }
            } catch (ClassCastException e) {
                long subjectCode = (long) temp.get(type + "Code");
                if (code != null) {
                    if (subjectCode == Integer.valueOf(code)) {
                        String foundStr = (String) (temp.get(type + currLanguage));
                        return foundStr;
                    }
                }
            }
        }
        return null;
    }

    //Temp batch function
    public JSONObject getAllTables() {
        System.setProperty("java.net.useSystemProxies", "true");
        int call = 0;
        //create URL object
        URL obj = null;
        try {
            obj = new URL("https://ndmsolrp1.stcpaz.statcan.gc.ca:8983/solr/ndm_pc/query?q=*%3A*&fq=product_type_code%3A10&fl=product_id%2Ccansim_id%2Clast_release_date%2Cfrc%2Cfrequency_code%2Carchive_status_code%2Cen%3Atitle_en%2Cfr%3Atitle_fr&sort=en%20asc&wt=csv&rows=10000");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //obj = new URL("https://www.earthquakescanada.nrcan.gc.ca/api/earthquakes");
        //Open connection
        System.out.println("b4 open");
        boolean success = false;
        int i=135;
        while(!success){
            try {
                CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("stc.ca", 8080));
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection(proxy);
                con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36");
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
                con.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                con.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
                con.setRequestProperty("Upgrade-Insecure-Requests", "1");
                con.setRequestProperty("Cookie", "AMCV_A90F2A0D55423F537F000101%40AdobeOrg=1099438348%7CMCIDTS%7C17724%7CMCMID%7C31690393898812013551273723845806254820%7CMCAAMLH-1531330930%7C7%7CMCAAMB-1531918571%7C6G1ynYcLPuiQxYZrsz_pkqfLG9yMXBpb2zX5dvJdYQJzPXImdj0y%7CMCOPTOUT-1531320971s%7CNONE%7CMCAID%7CNONE%7CMCSYNCSOP%7C411-17731%7CvVersion%7C2.1.0; Domain=.statcan.gc.ca; Path=/; AMCV_A90F2A0D55423F537F000101%40AdobeOrg=1099438348%7CMCIDTS%7C17724%7CMCMID%7C31690393898812013551273723845806254820%7CMCAAMLH-1531330930%7C7%7CMCAAMB-1531918571%7C6G1ynYcLPuiQxYZrsz_pkqfLG9yMXBpb2zX5dvJdYQJzPXImdj0y%7CMCOPTOUT-1531320971s%7CNONE%7CMCAID%7CNONE%7CMCSYNCSOP%7C411-17731%7CvVersion%7C2.1.0");
                System.out.println(con.getResponseMessage());
                con.getInputStream();
                System.out.println("aft open");
                //Set URL Option
                con.setRequestMethod("GET");

                //Set Headers
                //con.connect();
                //Store response code and print
                int status = con.getResponseCode();
                System.out.println(status);
                //Get Response, store in buffer
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                //Convert buffer to string
                String response_string = content.toString();
                //Create the JSONParser object
                org.json.simple.parser.JSONParser jsonParser = new org.json.simple.parser.JSONParser();
                //Parse the JSONArray
                //Convert Response Array to JSON Object
                JSONObject response_JSON = null;
                try {
                    response_JSON = (JSONObject) jsonParser.parse(response_string);
                    System.out.println(response_JSON);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            catch(ProtocolException e){
                e.printStackTrace();
            } catch(MalformedURLException e){
                e.printStackTrace();
            } catch(IOException e){
                i++;
            }
        }

        return null;
    }
}

