/*
 * Modified from Webtrekk Recommendation class to implement reactive
 * technology.
 * Created by Ramya on 14/01/2018.
 */

package com.webtrekk.webtrekksdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.JsonReader;
import android.util.Log;

import com.webtrekk.webtrekksdk.Configuration.TrackingConfiguration;
import com.webtrekk.webtrekksdk.Utils.HelperFunctions;
import com.webtrekk.webtrekksdk.Utils.WebtrekkLogging;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;


public class WebtrekkRecmmtn {

    public static class RecommendationProduct
    {
        private final String mId;
        private final String mTitle;
        final private Map<String, RecommendationProductValue> mValues;

        RecommendationProduct(String id, String title, Map<String, RecommendationProductValue> values)
        {
            mId = id;
            mTitle = title;
            mValues = values;
        }

        /**
         *returns the id of product
         * @return
         */
        public String getId()
        {
            return mId;
        }

        /**
         * returns recommendation Title
         * @return
         */
        public String getTitle()
        {
            return mTitle;
        }

        /**
         * returns recommendation product value based on key
         * @param key
         * @return
         */
        public String getValue(String key)
        {
            return mValues.get(key).getValue();
        }

        /**
         * get the type for this product value based on key for now type can be text, pictureURL, url
         * @param key
         * @return
         */
        public String getValueType(String key)
        {
            return mValues.get(key).getType();
        }

        /**
         * get list of all possible values. It includes value and value type in RecommendationProductValue
         * @return
         */
        public Map<String, RecommendationProductValue> getValues()
        {
            return mValues;
        }
    }

    public static class RecommendationProductValue
    {
        final private String mType;
        final private String mValue;

        RecommendationProductValue(String type, String value)
        {
            mType = type;
            mValue = value;
        }

        /**
         * return type of recommendation value
         * @return
         */
        public String getType() {
            return mType;
        }

        /**
         * return recommendation value
         * @return
         */
        public String getValue() {
            return mValue;
        }
    }

    public enum QueryRecommendationResult
    {
        RECEIVED_OK,
        NO_PLACEMENT_ID_FOUND,
        ACCOUNT_ID_NOT_FOUND,
        RECOMENDATION_API_DEACITVATED,
        NO_CONNECTION,
        INCORRECT_URL_FORMAT,
        INCORRECT_RESPONSE
    }


    private Map<String, String> mRequestParameters = new HashMap<>();
    private String mRecomendationURL;
    private String mProductID;
    private String mProductCat;
    private final String USER_ID_PAR_NAME = "userId";
    private final String PRODUCT_ID_PAR_NAME = "product";
    private final String PRODUCT_CAT_PAR_NAME = "productCat";
    final private TrackingConfiguration mConfiguration;
    final private Context mContext;
    private static RetrieveRecommendationData mRetrieveRecommendationData;

    /**
     * @hide
     * @param configuration
     * @param context
     */
    WebtrekkRecmmtn(TrackingConfiguration configuration, Context context)
    {
        mConfiguration = configuration;
        mContext = context;
        mRetrieveRecommendationData = new RetrieveRecommendationData();
    }

    /**
     * returns recommendation URL configured for this produce. List of recommendations are defined in
     * <recommendations> tag in config XML. Recommendation are returned as URL String based on provided key value,
     * @return
     */
    public String getConfiguredRecommendationURL(String key)
    {
        return mConfiguration.getRecommendationConfiguration().get(key);
    }

    public boolean configureRecommendation(String recommendationName)
    {
        mRecomendationURL = getConfiguredRecommendationURL(recommendationName);

        if (mRecomendationURL == null)
        {
            WebtrekkLogging.log("There is no recommendation found. Please check your configuration xml");
            return false;
        }

        if(mRetrieveRecommendationData != null) {

            mRetrieveRecommendationData.setmUrl(getRequestURL());
        }
        return true;
    }

    /**
     * Set product ID for request recommendation call. If product ID null it will be ignored

     * This value is optional
     * @param productID
     * @return
     */
    public WebtrekkRecmmtn setProductId(String productID)
    {
        mProductID = productID;
        return this;
    }

    /**
     * Set product category for request recommendation call. if productCat null it will be ignored.
     * Need call {@link #call()} to complete query.
     * This value is optional
     * @param productCat
     * @return
     */
/* comment out need development on server side first
    public WebtrekkRecommendations setProductCat(String productCat)
    {
        mProductCat = productCat;
        return this;
    }
*/

    /**
     * @hide
     * @return
     */
    private String getRequestURL()
    {
        final String keys[] = {USER_ID_PAR_NAME, PRODUCT_ID_PAR_NAME, PRODUCT_CAT_PAR_NAME};
        final String values[] = {HelperFunctions.getEverId(mContext), mProductID, mProductCat};

        String addString = "";

        for (int i = 0; i < keys.length ; i++)
        {
            if (values[i] != null)
              addString += "&" + keys[i] + "=" + HelperFunctions.urlEncode(values[i]);
        }

        return mRecomendationURL + addString;
    }

    public MyObservable getObservable()
    {
        return new MyObservable();
    }

    public interface IRecommendationObservable {
        Observable<List<WebtrekkRecmmtn.RecommendationProduct>> requestRecommendationData();
    }


    /**
     * Creating an class to implement Observable pattern.
     */
    public class MyObservable implements IRecommendationObservable {
        public Observable<List<RecommendationProduct>> mObservable;

        public Observable<List<RecommendationProduct>> requestRecommendationData()
        {
            mObservable = Observable.create(
                    new ObservableOnSubscribe<List<RecommendationProduct>>() {

                        @Override
                        public void subscribe(ObservableEmitter<List<RecommendationProduct>> e) throws Exception {
                            mRetrieveRecommendationData.setObservableEmitter(e);
                          //  mRetrieveRecommendationData = new RetrieveRecommendationData();
                            mRetrieveRecommendationData.establishConnection();
                        }
                    }
            );
            if(mObservable != null)
                Log.d(" Observable ", " is not nullllllllll... ");

            return mObservable;
        }

    }
    /**
     * This class does http connection, and process it's response and reco item
     * Modified from RecommendationCall thread class to avoid using Thread.
     */

    private static class RetrieveRecommendationData {

        private String mUrl;
        private ObservableEmitter<List<RecommendationProduct>> mObsEmit;
        private List<RecommendationProduct> mRequestResults;
        volatile private QueryRecommendationResult mQueryResult;

        private void setmUrl(String url)
        {
            mUrl = url;
        }

        private void setObservableEmitter(ObservableEmitter<List<RecommendationProduct>> e)
        {
            mObsEmit = e;
        }

        private boolean processResponseMain(JsonReader reader)
        {
            try {
                mRequestResults = new ArrayList<>();
                reader.beginArray();
                while (reader.hasNext())
                {
                    reader.beginObject();
                    if (reader.nextName().equals("reco"))
                    {
                        processResponseRecoItem(reader, mRequestResults);
                    }else
                        reader.skipValue();
                    reader.endObject();
                }
                reader.endArray();
            } catch (IOException e) {
                mQueryResult = QueryRecommendationResult.INCORRECT_RESPONSE;
                WebtrekkLogging.log("Incorrect response structure:"+e);
                mRequestResults = null;
                return false;
            }
            return true;
        }
        private void processResponseRecoItem(JsonReader reader, List<RecommendationProduct> recommendationProducts) throws IOException
        {
            String title = null, id = null;
            Map<String, RecommendationProductValue> recommendationValues = new HashMap<>();

            reader.beginArray();//begin array of one recommendation
            while (reader.hasNext())
            {

                String value = null, identifier = null, type = null;

                reader.beginObject(); //read recommendation item
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("value"))
                        value = reader.nextString();
                    else if (name.equals("identifier"))
                        identifier = reader.nextString();
                    else if (name.equals("type"))
                        type = reader.nextString();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                if (identifier.equals("id"))
                    id = value;
                else if(identifier.equals("campaignTitle"))
                    title = value;
                else
                    recommendationValues.put(identifier, new RecommendationProductValue(type, value));
            }
            reader.endArray();
            if (id != null)
                recommendationProducts.add(new RecommendationProduct(id, title, recommendationValues));
            else
            {
                WebtrekkLogging.log("Incorrect recommendation. Lack of id. Title:"+title);
            }
        }
        private void establishConnection(){
            JsonReader reader = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(mUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                WebtrekkLogging.log("Sending recommendation request: "+mUrl);
                int resp = connection.getResponseCode();

                if (resp == 200)
                {
                    reader = new JsonReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    if (processResponseMain(reader))
                        mQueryResult = QueryRecommendationResult.RECEIVED_OK;
                }else if (resp == 400) {
                    mQueryResult = QueryRecommendationResult.NO_PLACEMENT_ID_FOUND;
                }else if (resp == 401) {
                    mQueryResult = QueryRecommendationResult.ACCOUNT_ID_NOT_FOUND;
                }else if (resp == 403)
                    mQueryResult = QueryRecommendationResult.RECOMENDATION_API_DEACITVATED;
                else if (resp == 404)
                    mQueryResult = QueryRecommendationResult.INCORRECT_URL_FORMAT;
                else {
                    mQueryResult = QueryRecommendationResult.INCORRECT_RESPONSE;
                    WebtrekkLogging.log("Recommendation request Incorrect Response:"+resp);
                }

                if(mQueryResult == QueryRecommendationResult.RECEIVED_OK) {
                    mObsEmit.onNext(mRequestResults);
                    mObsEmit.onComplete();
                }
                else
                    mObsEmit.onError(new Throwable(mQueryResult+""));

            } catch (MalformedURLException e) {
                mQueryResult = QueryRecommendationResult.NO_PLACEMENT_ID_FOUND;
                WebtrekkLogging.log("Incorrect recommendation URL. Check your configuration xml." + e);
            } catch (IOException e) {
                mQueryResult = QueryRecommendationResult.NO_CONNECTION;
                WebtrekkLogging.log("Can't connect to reco server:"+e);
            }finally {
                try {
                    if (reader != null)
                        reader.close();
                    if (connection != null)
                        connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }


}
