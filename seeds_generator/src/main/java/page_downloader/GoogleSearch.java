import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONString;


public class GoogleSearch {

    private String accountKey;
    private String cseID;
    private Properties prop;

    public GoogleSearch(){
	try{
	    prop = new Properties();
	    FileInputStream is = new FileInputStream("conf/config.properties");
	    prop.load(is);
	    accountKey = prop.getProperty("ACCOUNTKEY_GOOG");
	    cseID = prop.getProperty("CSE_ID_GOOG");
	}
	catch(Exception e){
	    e.printStackTrace();
	    prop = null;
	}
    }



    public ArrayList<String> search(String query, String begin, String top, String es_index, String es_doc_type, String es_server){
        int nTop = Integer.valueOf(top);
	int start = Integer.valueOf(begin);
	if (this.prop == null){
	    System.err.println("Error: config file is not loaded yet");
	    return null;

	}

	Download download = new Download(query, null, es_index, es_doc_type, es_server);

	ArrayList<String> urls = new ArrayList<String>();

	URL query_url;
	try {
	    int step = 10; //10 is the maximum number of results to return in each query
	    query = "&num=" + String.valueOf(step) + "&key=" + accountKey + "&cx=" + cseID + "&q=" + query.replaceAll(" ", "%20");
	    for (; start < nTop; start += step){

		long startTime = System.currentTimeMillis();
		long elapsedTime = 0L;
		query_url = new URL("https://www.googleapis.com/customsearch/v1?start=" + String.valueOf(start) + query);


		HttpURLConnection conn = (HttpURLConnection)query_url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String output = "";
		String line;
		while ((line = br.readLine()) != null) {
		    output = output + line;
		}
		conn.disconnect();

		JSONObject obj = new JSONObject(output);
		JSONArray items = obj.getJSONArray("items");

		for(int i=0; i < items.length(); ++i){
		    JSONObject item = items.getJSONObject(i);
		    //All keys of the json object: snippet, htmlFormattedUrl, htmlTitle
		    //kind, pagemap, displayLink, link, htmlSnippet, title, formatedUrl, cacheId

		    String link = (String)item.get("link");
		    urls.add(link);
		    item.put("rank", Integer.toString(start + i));

		    download.addTask(item);
		}

		elapsedTime = (new Date()).getTime() - startTime;
	    }

	}
	catch (MalformedURLException e1) {
	    e1.printStackTrace();
	}
	catch (IOException e) {
	    e.printStackTrace();
	}
	catch (Exception e){
	    e.printStackTrace();
	}

	download.shutdown();


	System.out.println("Number of results: " + String.valueOf(urls.size()));

	return urls;


    }

    public static void main(String[] args) {

	String query = ""; //default
	String top = "50"; //default
	String start = "1"; //default

	String es_index = "memex"; //default
	String es_doc_type = "page"; //default
	String es_server = "localhost"; //default


	int i = 0;
	while (i < args.length){
	    String arg = args[i];
	    if(arg.equals("-q")){
		query = args[++i];
	    } else if(arg.equals("-t")){
		top = args[++i];
	    } else if(arg.equals("-b")){
		start = args[++i];
	    } else if(arg.equals("-i")){
		es_index = args[++i];
	    } else if(arg.equals("-d")){
		es_doc_type = args[++i];
	    } else if(arg.equals("-s")){
		es_server = args[++i];
	    }else {
		System.err.println("Unrecognized option");
		break;
	    }
	    ++i;
	}

	GoogleSearch bs = new GoogleSearch();
	bs.search(query, start, top, es_index, es_doc_type, es_server);
    }
}
