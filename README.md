# Roots AppLinker Android SDK

This is a repository of open source Roots App Linker Android SDK, and the information presented here serves as a reference manual for  Roots App Linker SDK.

## Get the Demo App

This is the readme file of for open source Roots App Linker Android SDK. There's a full demo app embedded in this repository. Check out the project and run the `Roots-SDK-TestBed` for a demo.

## Installation
#### Gradle
Add the following dependency for your project.
```Java
dependencies {
       compile 'io.branch.roots.sdk.android:Roots:0.1.+'
}
```
#### Using local library
1. Copy the Roots-SDK.aar from project root to your `project\libs\` folder
2. Add the following to the “build.gradle” file

```java
repositories{
   flatDir{
       	dirs 'libs'
    }
 }
dependencies {
	compile(name:’Roots-SDK', ext:'aar’)
}
```

## Connect To External Application
Use the following api to connect to an applications using a url.

```java
new Roots(context, url).connect();
```

That's all sdk will take care of the rest. If any configured app installed it will be opened. It will fallback to the web URL if no app found. You can specify the fallback preference using `setAlwaysFallbackToWebUrl()` method.
If you’d like to listen to routing lifecyle events, set the `setRootsConnectionEventsCallback` to listen to the app connection states as follows.

```java

    new Roots(context, url)
       .setAlwaysFallbackToWebUrl(true)
       .setRootsConnectionEventsCallback(new Roots.IRootsEvents() {
           @Override
           public void onAppLaunched(String appName, String packageName) {
           }

           @Override
           public void onFallbackUrlOpened(String url) {
           }

           @Override
           public void onPlayStoreOpened(String appName, String packageName) {
           }
       })
      .connect();
```

## Configuring In-app Routing
To setup in-app routing when the app is opened by Google or Facebook App Links follow the below two steps

##### Enable in-app routing
In your `Application` class `onCreate()` method

```java
Roots.enableDeeplinkRouting(applicationInstance);
```
##### Add Routing Filters
The Activities for routing should specify an filter in the application manifest as a metadata for Activity

```xml
<activity android:name=".MyContentActivity">
   <meta-data
         android:name="al:android:url"android:value="myscheme://*/user/{userID}/{name}"/>
   <meta-data
         android:name="al:web:url" android:value="https://my_awesome_site.com/*/{userID}"/>
</activity>
```

SDK will check the manifest on launch and open the Activity on finding a pattern match with web URL.
In the routing filter the wildcard fields are specified by `*` and the parameters are specified with in `{}`. SDK capture the parameters and their values and add it as the extra value in the intent.

```java
protected void onCreate(Bundle savedInstanceState) {
    Roots.isRootsLaunched(this){
       String userId = getIntent().getStringExtra("userID");
       String userName = getIntent().getStringExtra("name");
    }
}
```
