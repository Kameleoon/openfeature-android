# Kameleoon OpenFeature provider for Android

The Kameleoon OpenFeature provider for Android allows you to connect your OpenFeature Android implementation to Kameleoon without installing the Android Kameleoon SDK.

> [!WARNING]
> This is a beta version. Breaking changes may be introduced before general release.

## Supported Android sdk versions

This version of the SDK is built for the following targets:

* Android 5.0 (API level 21) and above.

## Get started

This section explains how to install, configure, and customize the Kameleoon OpenFeature provider.

### Install dependencies

First, choose your preferred dependency manager from the following options and install the required dependencies in your application.

<details>
  <summary>Gradle</summary>

```gradle
dependencies {
    implementation 'com.kameleoon:kameleoon-openfeature-android:0.0.1'
    // other dependencies
}
```
</details>
<details>
  <summary>Maven</summary>

```xml
<dependencies>
    <dependency>
        <groupId>com.kameleoon</groupId>
        <artifactId>kameleoon-openfeature-android</artifactId>
        <!-- Update this version to the latest one -->
        <version>0.0.1</version>
    </dependency>
    <!-- other dependencies -->
</dependencies>
```
</details>

### Usage

The following example shows how to use the Kameleoon provider with the OpenFeature SDK.

<details>
  <summary>Java</summary>

```java
KameleoonProvider provider;
String userId = "userId";
String featureKey = "featureKey";
KameleoonClientConfig clientConfig = new KameleoonClientConfig.Builder().build();

try {
	provider = new KameleoonProvider("siteCode", userId, clientConfig, getApplicationContext());
	// or if you want that visitor code will be generated automatically
	provider = new KameleoonProvider("siteCode", clientConfig, getApplicationContext());
} catch (ProviderNotReadyError e) {
	// Handle the error
	throw new RuntimeException(e);
}

OpenFeatureAPI.INSTANCE.setProvider(provider, null);

// Or use OpenFeatureAPI.setProviderAndWait for wait for the provider to be ready
KameleoonProvider finalProvider = provider;
try {
	BuildersKt.runBlocking(
		EmptyCoroutineContext.INSTANCE,
		(scope, continuation) -> OpenFeatureAPI.INSTANCE.setProviderAndWait(
			finalProvider,
			Dispatchers.getIO(),
			null,
			(Continuation<? super Unit>) continuation)
	);
} catch (Exception e) {
	throw new RuntimeException(e);
}

client = OpenFeatureAPI.INSTANCE.getClient(null, null);

Map<String, Value> dataDictionary = new HashMap<String, Value>(){{
	put("variableKey", new Value.String("variableKey"));
}};

EvaluationContext evalContext = new ImmutableContext("", dataDictionary);
OpenFeatureAPI.INSTANCE.setEvaluationContext(evalContext);
Integer numberOfRecommendedProducts = client.getIntegerValue(featureKey, 5);
showRecommendedProducts(numberOfRecommendedProducts);
```
</details>
<details>
  <summary>Kotlin</summary>

```kotlin
val userId = "userId"
val featureKey = "featureKey"
val clientConfig = KameleoonClientConfig.Builder().build()

val provider: KameleoonProvider = try {
    KameleoonProvider("siteCode", userId, clientConfig, applicationContext)
    // or if you want that visitor code will be generated automatically
    KameleoonProvider("siteCode", clientConfig, applicationContext)
} catch (e: OpenFeatureError.ProviderNotReadyError) {
    // Handle the error
    throw RuntimeException(e)
}

OpenFeatureAPI.setProvider(provider, null)
// Or use OpenFeatureAPI.setProviderAndWait for wait for the provider to be ready
runBlocking {
    OpenFeatureAPI.setProviderAndWait(provider, Dispatchers.IO)
}

val client = OpenFeatureAPI.getClient()

val dataDictionary = mapOf(
    "variableKey" to Value.String("variableKey")
)

val evalContext = ImmutableContext("", dataDictionary)
OpenFeatureAPI.setEvaluationContext(evalContext)
val numberOfRecommendedProducts = kameleoonClient.getIntegerValue(featureKey, 5)
showRecommendedProducts(numberOfRecommendedProducts)
```
</details>

#### Customize the Kameleoon provider

You can customize the Kameleoon provider by changing the `KameleoonClientConfig` object that you passed to the constructor above. For example:

<details>
  <summary>Java</summary>

```java
KameleoonClientConfig config = new KameleoonClientConfig.Builder()
	.defaultTimeoutMillisecond(10_000) // in milliseconds, 10 seconds by default, optional
	.environment("staging") // optional
	.refreshIntervalMinute(60)     // in minutes. Optional field
	.build();

try {		
	provider = new KameleoonProvider("siteCode", "userId", config, getApplicationContext());
} catch (ProviderNotReadyError e) {		
	// Handle the error
	throw new RuntimeException(e);
}
```
</details>
<details>
  <summary>Kotlin</summary>

```kotlin
val config = KameleoonClientConfig.Builder()
    .defaultTimeoutMillisecond(10_000) // in milliseconds, 10 seconds by default, optional
    .environment("staging") // optional
    .refreshIntervalMinute(60) // in minutes. Optional field
    .build()

val provider: KameleoonProvider = try {
    KameleoonProvider("siteCode", "userId", config, applicationContext)
} catch (e: ProviderNotReadyError) {
    // Handle the error
    throw RuntimeException(e)
}
```
</details>

> [!NOTE]
> For additional configuration options, see the [Kameleoon documentation](https://developers.kameleoon.com/feature-management-and-experimentation/mobile-sdks/android-sdk/#example-code).

## EvaluationContext and Kameleoon Data

Kameleoon uses the concept of associating `Data` to users, while the OpenFeature SDK uses the concept of an `EvaluationContext`, which is a dictionary of string keys and values. The Kameleoon provider maps the `EvaluationContext` to the Kameleoon `Data`.

<details>
  <summary>Java</summary>

```java
EvaluationContext context = new ImmutableContext("", emptyMap());
OpenFeatureAPI.INSTANCE.setEvaluationContext(context);
```
</details>
<details>
  <summary>Kotlin</summary>

```kotlin
val context = ImmutableContext("", emptyMap<String, Value>())
OpenFeatureAPI.setEvaluationContext(context)
```
</details>

The Kameleoon provider provides a few predefined parameters that you can use to target a visitor from a specific audience and track each conversion. These are:

| Parameter              | Description                                                                                                                                                                 |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DataType.CUSTOM_DATA` | The parameter is used to set [`CustomData`](https://developers.kameleoon.com/feature-management-and-experimentation/mobile-sdks/android-sdk/#customdata) for a visitor.     |
| `DataType.CONVERSION`  | The parameter is used to track a [`Conversion`](https://developers.kameleoon.com/feature-management-and-experimentation/mobile-sdks/android-sdk/#conversion) for a visitor. |

### DataType.CUSTOM_DATA

Use `DataType.CUSTOM_DATA` to set [`CustomData`](https://developers.kameleoon.com/feature-management-and-experimentation/mobile-sdks/android-sdk/#customdata) for a visitor. The `DataType.CUSTOM_DATA` field has the following parameters:

| Parameter               | Type   | Description                                                       |
|-------------------------|--------|-------------------------------------------------------------------|
| `CustomDataType.INDEX`  | int    | Index or ID of the custom data to store. This field is mandatory. |
| `CustomDataType.VALUES` | String | Value of the custom data to store. This field is mandatory.       |

#### Example

<details>
  <summary>Java</summary>

```java
Map<String, Value> customDataDictionary = new HashMap<String, Value>() {{
	put(DataType.CUSTOM_DATA.getValue(), new Structure(new HashMap<String, Value>() {{
		put(CustomDataType.INDEX.getValue(), new Integer(1));
		put(CustomDataType.VALUES.getValue(), new Value.String("10"));			
	}}));
}};

EvaluationContext context = new ImmutableContext("", customDataDictionary);
OpenFeatureAPI.INSTANCE.setEvaluationContext(context);
```
</details>
<details>
  <summary>Kotlin</summary>

```kotlin
val customDataDictionary = mapOf(
    DataType.CUSTOM_DATA.value to Value.Structure(mapOf(
        CustomDataType.INDEX.value to Value.Integer(1),
        CustomDataType.VALUES.value to Value.String("10")
    ))
)

val context = ImmutableContext("", customDataDictionary)
OpenFeatureAPI.setEvaluationContext(context)
```
</details>

### DataType.CONVERSION

Use `DataType.CONVERSION` to track a [`Conversion`](https://developers.kameleoon.com/feature-management-and-experimentation/mobile-sdks/android-sdk/#conversion) for a visitor. The `DataType.CONVERSION` field has the following parameters:

| Parameter                | Type  | Description                                                     |
|--------------------------|-------|-----------------------------------------------------------------|
| `ConversionType.GOAL_ID` | int   | Identifier of the goal. This field is mandatory.                |
| `ConversionType.REVENUE` | float | Revenue associated with the conversion. This field is optional. |

#### Example

<details>
  <summary>Java</summary>

```java
Map<String, Value> conversionDictionary = new HashMap<String, Value>() {{
	put(DataType.CONVERSION.getValue(), new Value.Structure(new HashMap<String, Value>() {{
		put(ConversionType.GOAL_ID.getValue(), new Value.Integer(1));
		put(ConversionType.REVENUE.getValue(), new Value.Integer(200));
	}}));
}};

EvaluationContext context = new ImmutableContext("", conversionDictionary);
OpenFeatureAPI.INSTANCE.setEvaluationContext(context);
```
</details>
<details>
  <summary>Kotlin</summary>

```kotlin
val conversionDictionary = mapOf(
    DataType.CONVERSION.value to Value.Structure(mapOf(
        ConversionType.GOAL_ID.value to Value.Integer(1),
        ConversionType.REVENUE.value to Value.Integer(200)
    ))
)

val context = ImmutableContext("", conversionDictionary)
OpenFeatureAPI.setEvaluationContext(context)
```
</details>

### Use multiple Kameleoon Data types

You can provide many different kinds of Kameleoon data within a single `EvaluationContext` instance.

For example, the following code provides one `DataType.CONVERSION` instance and two `DataType.CUSTOM_DATA` instances.

<details>
  <summary>Java</summary>

```java
Map<String, Value> dataDictionary = new HashMap<String, Value>() {{
	put(DataType.CONVERSION.getValue(), new Value.Structure(new HashMap<String, Value>() {{
		put(ConversionType.GOAL_ID.getValue(), new Value.Integer(1));
		put(ConversionType.REVENUE.getValue(), new Value.Integer(200));
	}}));
	put(DataType.CUSTOM_DATA.getValue(), new Value.List(Arrays.asList(
		new Value.Structure(new HashMap<String, Value>() {{
		    put(CustomDataType.INDEX.getValue(), new Value.Integer(1));
		    put(CustomDataType.VALUES.getValue(), new Value.List(Arrays.asList(new Value.String("10"), new Value.String("30"))));
		}}),
        new Value.Structure(new HashMap<String, Value>() {{
		    put(CustomDataType.INDEX.getValue(), new Value.Integer(2));
		    put(CustomDataType.VALUES.getValue(), new Value.String("20"));
		}})
    )));
}};

EvaluationContext context = new ImmutableContext("", dataDictionary);
OpenFeatureAPI.INSTANCE.setEvaluationContext(context);
```
</details>
<details>
  <summary>Kotlin</summary>

```kotlin
val dataDictionary = mapOf(
    DataType.CONVERSION.value to Value.Structure(mapOf(
        ConversionType.GOAL_ID.value to Value.Integer(1),
        ConversionType.REVENUE.value to Value.Integer(200)
    )),
    DataType.CUSTOM_DATA.value to Value.List(listOf(
        Value.Structure(mapOf(
            CustomDataType.INDEX.value to Value.Integer(1),
            CustomDataType.VALUES.value to Value.List(listOf(Value.String("10"), Value.String("30")))
        )),
        Value.Structure(mapOf(
            CustomDataType.INDEX.value to Value.Integer(2),
            CustomDataType.VALUES.value to Value.String("20")
        ))
    ))
)

val context = ImmutableContext("", dataDictionary)
OpenFeatureAPI.setEvaluationContext(context)
```
</details>
