InApp Billing for Cafe Bazaar (Android)
=============
**React Native Cafe Bazaar** is built to provide an easy interface to InApp Billing for **Cafe Bazaar**,


## Installation

1. `npm install --save react-native-cafe-bazaar` or `yarn add react-native-cafe-bazaar`
2. Add the following in `android/settings.gradle`

  ```gradle
  ...
  include ':react-native-cafe-bazaar', ':app'
  project(':react-native-cafe-bazaar').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-cafe-bazaar/android/app')
  ```

3. And the following in `android/app/build.gradle`

  ```gradle
  ...
  dependencies {
      ...
      implementation project(':react-native-cafe-bazaar')
  }
  ```

4. Update MainActivity or MainApplication depending on React Native version.
  - React Native version >= 0.29
    Edit `MainApplication.java`.
    1. Add `import com.contoriel.cafebazaar.CafeBazaarPackage;` in the top of the file.
    2. Register package:
    ```java
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        // add package here
        new CafeBazaarPackage()
      );
    }
    ```

5. Add your CafeBazaar Public key as a line to your `android/app/src/main/res/values/strings.xml` with the name `CAFE_BAZAAR_PUBLIC_KEY`. For example:
```xml
<string name="CAFE_BAZAAR_PUBLIC_KEY">YOUR_CAFE_BAZAAR_PUBLIC_KEY_HERE</string>
```
Alternatively, you can add your license key as a parameter when registering the `CafeBazaarPackage`, like so:
```java
.addPackage(new CafeBazaarPackage("YOUR_CAFE_BAZAAR_PUBLIC_KEY_HERE"))
```
or for React Native 29+
```java
new CafeBazaarPackage("YOUR_CAFE_BAZAAR_PUBLIC_KEY_HERE")
```

Add the billing permission as follows to AndroidManifest.xml file:
```xml
<uses-permission android:name="com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR"></uses-permission>
```


### ON THE BAZAAR DEVELOPER PANEL

1. Upload the application to the [Developer Panel][panel].

[panel]: https://cafebazaar.ir/developers/panel/apps/?l=en "Cafe Bazaar Developer Panel"

2. Using the **Enter** button in In-app Billing column of the created app,
go to In-app Billing Panel.

3. In that app, create your in-app items

4. Grab the application's public key (a base-64 string) You can find the application's public key in the **Dealer Apps**
page for your application.


## Javascript API
Most of methods returns a `Promise`.

### open()

**Important:** Opens the service channel to CafeBazaar. Must be called (once!) before any other billing methods can be called.

```javascript
import CafeBazaar from 'react-native-cafe-bazaar'

...

CafeBazaar.open()
.then(() => CafeBazaar.purchase('YOUR_SKU','DEVELOPER_PAYLOAD',RC_REQUEST))
.catch(err => console.log('CafeBazaar err:', err))
```

### close()
**Important:** Must be called to close the service channel to CafeBazaar, when you are done doing billing related work. Failure to close the service channel may degrade the performance of your app.
```javascript
CafeBazaar.open()
.then(() => CafeBazaar.purchase('YOUR_SKU','DEVELOPER_PAYLOAD',RC_REQUEST))
.then((details) => {
  console.log(details)
  return CafeBazaar.close()
})
.catch(err => console.log('CafeBazaar err:', err))
```

### purchase('YOUR_SKU','DEVELOPER_PAYLOAD',RC_REQUEST)
##### Parameter(s)
* **productSKU (required):** String
* **developerPayload:** String
* **rcRequest:** Integer

##### Returns:
* **Details:** JSONObject:
  * **mDeveloperPayload:**
  * **mItemType:**
  * **mOrderId:**
  * **mOriginalJson:**
  * **mPackageName:**
  * **mPurchaseState:**
  * **mPurchaseTime:**
  * **mSignature:**
  * **mSku:**
  * **mToken:**

```javascript
CafeBazaar.purchase('YOUR_SKU','DEVELOPER_PAYLOAD',RC_REQUEST)
.then((details) => {
  console.log(details)
})
.catch(err => console.log('CafeBazaar err:', err))
```

### consume('YOUR_SKU')
##### Parameter(s)
* **productSKU (required):** String

##### Returns:
* **Details:** JSONObject:
  * **mDeveloperPayload:**
  * **mItemType:**
  * **mOrderId:**
  * **mOriginalJson:**
  * **mPackageName:**
  * **mPurchaseState:**
  * **mPurchaseTime:**
  * **mSignature:**
  * **mSku:**
  * **mToken:**

```javascript
CafeBazaar.consume('YOUR_SKU')
.then(...)
.catch(err => console.log('CafeBazaar err:', err))
```

### loadOwnedItems()

##### Returns:
* **items:** JSONArray:


```javascript
CafeBazaar.loadOwnedItems()
.then((details) => {
  console.log(details)
})
.catch(err => console.log('CafeBazaar err:', err))
```

### loadInventory([item1_SKU,item2_SKU,...])
##### Parameter(s)
* **productSKUs (required):** Array<String>

##### Returns:
* **mPurchaseMap:** JSONObject
* **mSkuMap:** JSONObject

```javascript
CafeBazaar.loadInventory([])
.then(...)
.catch(err => console.log('CafeBazaar err:', err))
```

## Use event listener
Below function dispatch **Event** instead of **Promise** and return value is same.

### purchaseWithEvent('YOUR_SKU','DEVELOPER_PAYLOAD',RC_REQUEST)
### consumeWithEvent('YOUR_SKU')
### loadOwnedItemsWithEvent()


```javascript
import {DeviceEventEmitter} from 'react-native';
...
  componentDidMount(){
    DeviceEventEmitter.addListener('CafeBazaar', function(e: Event) {
      // handle event.
      console.log(e);
    });
  }
```

### BACK TO BAZAAR DEVELOPER PANEL

Upload the updated APK to Bazaar Developer Panel
