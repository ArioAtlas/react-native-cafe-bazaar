package com.contoriel.cafebazaar;

import android.app.Activity;
import android.util.Log;
import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.Object;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ActivityEventListener;

import com.contoriel.cafebazaar.util.IabHelper;
import com.contoriel.cafebazaar.util.IabResult;
import com.contoriel.cafebazaar.util.Inventory;
import com.contoriel.cafebazaar.util.Purchase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

public class CafeBazaar extends ReactContextBaseJavaModule implements ActivityEventListener{
  private final ReactApplicationContext _reactContext;
  private String LICENSE_KEY = null;
  private IabHelper mHelper;
  private final Gson gson = new Gson();
  private static final String E_SETUP_ERROR = "E_SETUP_ERROR";
  private static final String E_SETUP_DISCONNECT = "E_SETUP_DISCONNECT";
  private static final String E_LOAD_ITEMS_FAILURE = "E_LOAD_ITEMS_FAILURE";
  private static final String E_LAYOUT_ERROR = "E_LAYOUT_ERROR";
  private static final String E_PURCHASE_DISCONNECT = "E_PURCHASE_DISCONNECT";
  private static final String E_PURCHASE_FAILURE = "E_PURCHASE_FAILURE";
  private static final String E_PURCHASE_PAYLOAD_VERIFY = "E_PURCHASE_PAYLOAD_VERIFY";
  private static final String E_PURCHASE_ERROR = "E_PURCHASE_ERROR";
  private static final String E_CONSUME_FAILURE = "E_CONSUME_FAILURE";
  private static final String E_CONSUME_ERROR = "E_CONSUME_ERROR";
  private static final String E_CONSUME_INITIAL = "E_CONSUME_INITIAL";
  static final String TAG = "CafeBazaar";
  private Inventory userInvo;

  public CafeBazaar(ReactApplicationContext reactContext, String licenseKey) {
        super(reactContext);
        _reactContext = reactContext;
        this.LICENSE_KEY = licenseKey;

        reactContext.addActivityEventListener(this);
    }

    public CafeBazaar(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
        int keyResourceId = _reactContext
                .getResources()
                .getIdentifier("CAFE_BAZAAR_PUBLIC_KEY", "string", _reactContext.getPackageName());
        this.LICENSE_KEY = _reactContext.getString(keyResourceId);

        reactContext.addActivityEventListener(this);
    }

  @Override
  public String getName(){
    return "CafeBazaar";
  }


  @ReactMethod
  public void open(final Promise promise) {
      mHelper = new IabHelper(_reactContext, LICENSE_KEY);

      // enable debug logging (for a production application, you should set this to false).
      mHelper.enableDebugLogging(false);

      // Start setup. This is asynchronous and the specified listener
      // will be called once setup completes.
      mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
          public void onIabSetupFinished(IabResult result) {

              if (result.isSuccess()) {
                promise.resolve(gson.toJson(result));
              }
              else{
                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) {
                  promise.reject(E_SETUP_DISCONNECT,"there no connection to cafe bazaar!");
                }
                else{
                  // Oh noes, there was a problem.
                  promise.reject(E_SETUP_ERROR,"There is a problem in cafe bazaar setup");
                }
              }
          }
      });
  }


  @ReactMethod
  public void loadInventory(ReadableArray skuList,final Promise promise){
    ArrayList<String> skus = new ArrayList<>();
    for (int i = 0; i < skuList.size(); i++) {
        skus.add(skuList.getString(i));
    }
    mHelper.queryInventoryAsync(true, skus,new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) {
              promise.reject(E_SETUP_DISCONNECT,"there no connection to cafe bazaar!");
            }
            // Is it a failure?
            else if (result.isFailure()) {
              promise.reject(E_LAYOUT_ERROR,"Failed to query inventory: " + result.getMessage());
            } else {
              promise.resolve(gson.toJson(inventory));
            }
        }
    });
  }

  @ReactMethod
  public void loadOwnedItems(final Promise promise){
    mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Have we been disposed of in the meantime? If so, quit.
            WritableMap params = Arguments.createMap();
            if (mHelper == null) {
              promise.reject(E_SETUP_DISCONNECT,"there no connection to cafe bazaar!");
            }
            // Is it a failure?
            else if (result.isFailure()) {
              promise.reject(E_LOAD_ITEMS_FAILURE,result.getMessage());
            } else {
              userInvo = inventory;
              promise.resolve(gson.toJson(inventory.getAllOwnedSkus()));
            }
        }
    });
  }

  public void loadOwnedItemsWithEvent(){
    mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Have we been disposed of in the meantime? If so, quit.
            WritableMap params = Arguments.createMap();
            if (mHelper == null) {
              params.putString("LoadOwnedItem","Disposed!");
              sendEvent(TAG, params);
            }
            // Is it a failure?
            if (result.isFailure()) {
              params.putString("LoadOwnedItem",result.getMessage());
              sendEvent(TAG, params);
            }
            userInvo = inventory;
            params.putString("LoadOwnedItem",gson.toJson(inventory.getAllOwnedSkus()));
            sendEvent(TAG, params);
        }
    });
  }

  @ReactMethod
  public void purchaseWithEvent(String sku,String payload,int rcRequest) {
      try{
        mHelper.launchPurchaseFlow(getCurrentActivity(), sku, rcRequest,
        new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            // if we were disposed of in the meantime, quit.
            WritableMap params = Arguments.createMap();

            if (mHelper == null){
              params.putString("PurchaseResult","Connection Error!");
              sendEvent(TAG, params);
            }
            else{
              if (result.isFailure()) {
                  params.putString("Error",result.getMessage());
                  sendEvent(TAG, params);
              }
              else{
                if (!verifyDeveloperPayload(purchase)) {
                    params.putString("Error","could not verify developer payload");
                    sendEvent(TAG, params);
                }
                else{
                  params.putString("Details",gson.toJson(purchase));
                  sendEvent(TAG, params);
                }
              }
            }
          }
        }, payload);
      }
      catch (Exception ex){
        WritableMap params = Arguments.createMap();
        params.putString("Error",ex.getMessage());
        sendEvent(TAG, params);
      }
  }

  @ReactMethod
  public void purchase(String sku,String payload,int rcRequest,final Promise promise) {
      try{
        mHelper.launchPurchaseFlow(getCurrentActivity(), sku, rcRequest,
        new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            // if we were disposed of in the meantime, quit.
            WritableMap params = Arguments.createMap();

            if (mHelper == null){
              promise.reject(E_PURCHASE_DISCONNECT,"Connection Error!");
            }
            else{
              if (result.isFailure()) {
                  promise.reject(E_PURCHASE_FAILURE,result.getMessage());
              }
              else{
                if (!verifyDeveloperPayload(purchase)) {
                    promise.reject(E_PURCHASE_PAYLOAD_VERIFY,"could not verify developer payload");
                }
                else{
                  promise.resolve(gson.toJson(purchase));
                }
              }
            }
          }
        }, payload);
      }
      catch (Exception ex){
        promise.reject(E_PURCHASE_ERROR,ex.getMessage());
      }
  }

  /** Verifies the developer payload of a purchase. */
  boolean verifyDeveloperPayload(Purchase p) {
      String payload = p.getDeveloperPayload();

      /*
       * TODO: verify that the developer payload of the purchase is correct. It will be
       * the same one that you sent when initiating the purchase.
       *
       * WARNING: Locally generating a random string when starting a purchase and
       * verifying it here might seem like a good approach, but this will fail in the
       * case where the user purchases an item on one device and then uses your app on
       * a different device, because on the other device you will not have access to the
       * random string you originally generated.
       *
       * So a good developer payload has these characteristics:
       *
       * 1. If two different users purchase an item, the payload is different between them,
       *    so that one user's purchase can't be replayed to another user.
       *
       * 2. The payload must be such that you can verify it even when the app wasn't the
       *    one who initiated the purchase flow (so that items purchased by the user on
       *    one device work on other devices owned by the user).
       *
       * Using your own server to store and verify developer payloads across app
       * installations is recommended.
       */

      return true;
    }

    private void sendEvent(String eventName,
                         @Nullable WritableMap params) {
    _reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }

  @ReactMethod
  public void consume(String sku,final Promise promise){
    if(userInvo!=null){
      if(userInvo.hasPurchase(sku)){
        mHelper.consumeAsync(userInvo.getPurchase(sku),
        new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
          WritableMap params = Arguments.createMap();
           if (result.isSuccess()) {
              // provision the in-app purchase to the user
              promise.resolve(gson.toJson(purchase));
           }
           else {
              // handle error
              promise.reject(E_CONSUME_FAILURE,result.getMessage());
           }
          }
       });
      }
      else{
        promise.reject(E_CONSUME_ERROR,"user did not purchase item");
      }
    }
    else{
      promise.reject(E_CONSUME_INITIAL,"inventory not loaded!");
    }
  }


  @ReactMethod
  public void consumeWithEvent(String sku){
    if(userInvo!=null){
      if(userInvo.hasPurchase(sku)){
        mHelper.consumeAsync(userInvo.getPurchase(sku),
        new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
          WritableMap params = Arguments.createMap();
           if (result.isSuccess()) {
              // provision the in-app purchase to the user
              // (for example, credit 50 gold coins to player's character)
              params.putString("SuccessfulConsume",gson.toJson(purchase));
              sendEvent(TAG, params);
           }
           else {
              // handle error
              params.putString("Error",result.getMessage());
              sendEvent(TAG, params);
           }
          }
       });
      }
      else{
        WritableMap params = Arguments.createMap();
        params.putString("Error","user did not purchase item");
        sendEvent(TAG, params);
      }
    }
    else{
      WritableMap params = Arguments.createMap();
      params.putString("Error","inventory not loaded!");
      sendEvent(TAG, params);
    }
  }


  @Deprecated
  public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
    if (mHelper == null) return;

    // Pass on the activity result to the helper for handling
    if (!mHelper.handleActivityResult(requestCode, resultCode, intent)) {
        // not handled, so handle it ourselves (here's where you'd
        // perform any handling of activity results not related to in-app
        // billing...
        WritableMap params = Arguments.createMap();
        params.putString("Warning","you need to use your activity to perfom billing");
        sendEvent(TAG, params);
    }
  }

  public void onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent intent) {
    if (mHelper == null) return;

    // Pass on the activity result to the helper for handling
    if (!mHelper.handleActivityResult(requestCode, resultCode, intent)) {
        // not handled, so handle it ourselves (here's where you'd
        // perform any handling of activity results not related to in-app
        // billing...
        WritableMap params = Arguments.createMap();
        params.putString("Warning","you need to use your activity to perfom billing");
        sendEvent(TAG, params);
    }
  }

  @Override
  public void onNewIntent(Intent intent){

  }

  @ReactMethod
  public void close(final Promise promise){
    // very important:
    if (mHelper != null) {
        mHelper.dispose();
        mHelper = null;
    }
    promise.resolve(true);
  }


}
