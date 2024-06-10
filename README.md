# RxIntentStarter
<p align="left">
  <a href="https://android.com">
    <img src="https://img.shields.io/badge/Platform-Android-brightgreen?logo=android"
      alt="Platform" />
  </a>

  <a href="https://central.sonatype.com/artifact/com.algorigo.rx/rxintentstarter">
    <img src="https://img.shields.io/maven-central/v/com.algorigo.rx/rxintentstarter.svg"
      alt="Maven Central" />
  </a>

  <a href="http://www.apache.org/licenses/LICENSE-2.0">
    <img src="https://img.shields.io/github/license/Algorigo/RxIntentStarter"
      alt="License: Apache 2.0" />
  </a>

  <a href="https://dl.circleci.com/status-badge/redirect/gh/Algorigo/RxIntentStarter/tree/main">
    <img src="https://dl.circleci.com/status-badge/img/gh/Algorigo/RxIntentStarter/tree/main.svg?style=shield" alt="Build Status">
  </a>
</p>
RxIntentStarter is an Android library that simplifies the process of sending Intents and handling their results using RxJava. It provides a reactive way to start activities and receive results, reducing boilerplate code and making your code cleaner and easier to read.<br/>

# Setup
To use RxIntentStarter in your project, follow these steps:
## Step 1: Add the Maven Central repository
Ensure that the Maven Central repository is included in your project-level build.gradle file:
```gradle
allprojects {
    repositories {
        mavenCentral()
    }
}
```

## Step 2: Add the dependency
Add the RxIntentStarter dependency to your module-level build.gradle file:
```gradle
dependencies {
    implementation "com.algorigo.rx:rxintentstarter:1.0.0"
}
```

# Usage

#### Create RxIntentStart instance:

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var rxIntentStarter: RxIntentStarter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rxIntentStarter = RxIntentStarter.create(this) // An instance should be created in the onCreate of an Activity or Fragment
    }
}
```

## Example 1: ```requestEach```
### Using ```requestEach``` to Request Overlay Permission
In this example, we are using the requestEach method of RxIntentStarter to request the overlay permission. The intent for requesting the overlay permission is wrapped in an ActivityRequest and passed to the requestEach method.

Here is the code snippet:
```kotlin
rxIntentStarter.requestEach(
    ActivityRequest(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        },
        isGranted = {
            Settings.canDrawOverlays(this)
        }),
)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        {
            if (it.isOk()) {
                Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
        }, {
            Log.e(TAG, "Overlay permission error: $it")
        }
    )
    .addTo(compositeDisposable)
```
In this example, we create an Intent to request overlay permission, wrap it in an ActivityRequest, and pass it to the requestEach method. The isGranted function checks if the overlay permission is already granted. If Settings.canDrawOverlays(this) returns true, the system settings for overlay permission will not be opened and the downstream will receive true. If Settings.canDrawOverlays(this) returns false, the system settings for overlay permission will be opened. When the user returns to the app, Settings.canDrawOverlays(this) is checked again and the result is passed downstream.

### Using requestEach to Enable Bluetooth
In this example, we are using the requestEach method of RxPermissions to request Bluetooth permissions. If the permissions are granted, we use the requestEach method of RxIntentStarter to enable Bluetooth. If the permissions are not granted, we return an ActivityResult with RESULT_CANCELED.

Here is the code snippet:
```kotlin
RxPermissions(this).requestEachCombined(*bluetoothPermissions())
    .flatMap {
        if (it.granted) {
            rxIntentStarter.requestEach(
                ActivityRequest(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                ) {
                    (this.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled ?: false
                },
            )
        } else {
            Observable.just(com.algorigo.rxintentstarter.data.ActivityResult(RESULT_CANCELED))
        }
    }
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        {
            if (it.isOk()) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show()
            }
        }, {
            Log.e(TAG, "Bluetooth permission error: $it")
        }
    )
    .addTo(compositeDisposable)
```

In this example, we first use RxPermissions to request Bluetooth permissions. If the permissions are granted, we then use requestEach to create an ActivityRequest with an intent to enable Bluetooth. The isGranted function checks if Bluetooth is enabled. If Bluetooth is not enabled, a request to enable Bluetooth is made. The result of the request (true if Bluetooth is enabled, false if not) is passed downstream. Note that this does not involve opening the system settings or returning to the app from the settings. It simply sends a request to enable Bluetooth and returns the result.


## Example 2: ```requestEachCombined```
### Using ```requestEachCombined``` to Send Multiple Intents
In this example, we are using the requestEachCombined method of RxIntentStarter to send multiple intents at once. Each intent is wrapped in an ActivityRequest and they are all passed to the requestEachCombined method. The requestEachCombined method will wait for all the ActivityRequest to complete and then send the results downstream.

Here is the code snippet:
```kotlin
rxIntentStarter.requestEachCombined(
    ActivityRequest(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Hello, world!")
        },
    ),
    ActivityRequest(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Hello, kotlin!")
        },
    ),
    ActivityRequest(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Good Bye!")
        },
    ),
)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        {
            Toast.makeText(this, "All requests have been completed", Toast.LENGTH_SHORT).show()
        }, {
            Log.e(TAG, "Action send error: $it")
        }
    )
    .addTo(compositeDisposable)
```

In this example, we are creating three ActivityRequest objects, each with an intent to send a plain text message. The messages are "Hello, world!", "Hello, kotlin!", and "Good Bye!". We then observe the result on the main thread and display a toast message when all requests have been completed. If there is an error, we log the error message.  The requestEachCombined method is particularly useful when you need to perform multiple actions that depend on each other. It ensures that all the actions are completed before sending the results downstream. This can help to simplify your code and make it easier to understand.


## Example 3: ```requestEachWithImmediateCancel```
### Using ```requestEachWithImmediateCancel``` to Handle Errors
If you want to immediately send an error downstream when an error occurs during the execution of the request, you can use the requestEachWithImmediateCancel method of RxIntentStarter. This method will immediately cancel the request and send an onError event downstream.

Here is a brief explanation:
```kotlin
rxIntentStarter.requestEachWithImmediateCancel(
    // Your ActivityRequest here
)
```

In this example, if an error occurs during the execution of the request, requestEachWithImmediateCancel will immediately cancel the request and send an onError event downstream. This allows you to handle errors immediately as they occur.

# License
```
Copyright 2024 Algorigo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```