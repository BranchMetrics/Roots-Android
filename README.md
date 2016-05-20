# Roots App Connection SDK

This is a repository of open source Roots App Connection Android SDK. You can find the iOS version [here](https://github.com/BranchMetrics/Roots-iOS-SDK). This library is meant to serve two functions, to help you start linking to other apps but also receive links from others.

**Linking externally**

At Branch, we've noticed that a lot of people have adopted the Facebook App Links standard for deep linking. You can see the rules documented [here](applinks.org). We've come up with a standard behavior for what to do when a link is clicked on mobile. Here is the behavior priority:

1. Attempt to open up the native mobile app
2. Fallback to the web site
2. [optional] Fallback to the Play Store

**Receiving links**

On the other hand, the other thing we've noticed that it's very difficult to configure deep linking in native apps. We want to make it incredibly simple to map your URI path to the Activity responsible for displaying the content, then make it simple to access the referring link and metadata.

## Get the demo apps

This is the readme file of for open source Roots App Linker Android SDK. There's a full demo app embedded in this repository. Check out the project and run the `Roots-SDK-TestBed` for a demo.

## Installation

#### Using Gradle

Add the following dependency for your project.

```Java
dependencies {
       compile 'io.branch.roots.sdk.android:Roots:0.1.+'
}
```

#### Using the local library

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

## Connecting to another app

Use the following api to connect to an applications using a url.

```java
new Roots(context, url).connect();
```

That's all! The library will take care of the rest. The App Links are automatically parsed from the web link to determine the routing configuration. It will first try to open the app and then fallback to the web URL (or Play Store depending on configuration). You can specify the fallback preference using the `setAlwaysFallbackToWebUrl()` method.

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

## Configuring in-app routing

To setup in-app routing when the app is opened by Google, Facebook App Links or any URI scheme path, follow the steps below:

##### Enable in-app routing

In your `Application` class `onCreate()` method

```java
Roots.enableDeeplinkRouting(applicationInstance);
```

##### Add routing filters in the manifest

The destination activities should contain a metadata filter with the corresponding path structure. In the routing filter, wildcard fields are specified by `*` and parameters are specified with in `{}`. The SDK will capture the parameters with their values and add it to the intent extras.

```xml
<activity android:name=".MyContentActivity">
   <meta-data
         android:name="al:android:url"android:value="myscheme://*/user/{user_id}/{name}"/>
   <meta-data
         android:name="al:web:url" android:value="https://my_awesome_site.com/*/{user_id}"/>
</activity>
```

In the destination activity, you can retrieve the parameters from the intent extras.

```java
protected void onCreate(Bundle savedInstanceState) {
    Roots.isRootsLaunched(this){
       String userId = getIntent().getStringExtra("user_id");
       String userName = getIntent().getStringExtra("name");
    }
}
```
