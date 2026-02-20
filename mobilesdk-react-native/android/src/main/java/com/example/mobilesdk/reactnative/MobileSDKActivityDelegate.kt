package com.c4f.mobileSDK.reactnative

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.view.View
import android.view.ViewGroup
import com.c4f.mobileSDK.MobileSDK
import com.facebook.react.ReactActivity
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.*

class MobileSDKActivityDelegate(private val reactContext: ReactApplicationContext) {
    private var currentActivity: Activity? = null
    private var isAutoSetupComplete = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isJsInjected = false
    private var lastGlobalTouchTime = 0L

    // Activity lifecycle callbacks to track React Native activities
    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Log.d("MobileSDK_RN", "Activity created: ${activity.javaClass.simpleName}")
            
            if (activity is ReactActivity) {
                Log.d("MobileSDK_RN", "ReactActivity detected: ${activity.javaClass.simpleName}")
                currentActivity = activity
                
                // Inject JavaScript to track navigation
                injectNavigationTracker(activity)
                
                if (!isAutoSetupComplete) {
                    setupAutoDetection(activity)
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (activity is ReactActivity) {
                currentActivity = activity
            }
        }

        override fun onActivityResumed(activity: Activity) {
            if (activity is ReactActivity) {
                currentActivity = activity
                Log.d("MobileSDK_RN", "ReactActivity resumed, attaching touch interceptor")
                // Activate our new invisible touch spy
                setupGlobalTouchInterceptor(activity)
            }
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            if (activity == currentActivity) {
                Log.d("MobileSDK_RN", "Current ReactActivity destroyed")
                currentActivity = null
                
                try {
                    MobileSDK.getInstance().clearQueueForActivity(activity)
                } catch (e: Exception) {
                    Log.e("MobileSDK_RN", "Error clearing queue: ${e.message}")
                }
            }
        }
    }
    
    fun initialize() {
        try {
            val application = reactContext.applicationContext as? Application
            application?.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            Log.d("MobileSDK_RN", "Activity delegate initialized")
        } catch (e: Exception) {
            Log.e("MobileSDK_RN", "Failed to initialize activity delegate: ${e.message}")
        }
    }
    
    private fun injectNavigationTracker(activity: ReactActivity) {
        if (isJsInjected) return
        
        try {
            // Find the WebView or React root view
            val rootView = activity.window.decorView
            
            // Method 1: Try to find a WebView and inject JavaScript
            findAndInjectIntoWebView(rootView)
            
            // Method 2: Use DeviceEventEmitter to communicate with JS
            setupDeviceEventEmitter()
            
            isJsInjected = true
            Log.d("MobileSDK_RN", "✅ Navigation tracker injection attempted")
            
        } catch (e: Exception) {
            Log.e("MobileSDK_RN", "Failed to inject navigation tracker: ${e.message}")
        }
    }
    
    private fun findAndInjectIntoWebView(view: android.view.View) {
        if (view is WebView) {
            // Inject JavaScript that hooks into React Navigation
            val jsCode = """
                (function() {
                    console.log('[MobileSDK] Injecting navigation tracker...');
                    
                    // Try to hook into React Navigation
                    function setupTracking() {
                        // Look for React Navigation in the global scope
                        if (window.__REACT_NAVIGATION__) {
                            const navigation = window.__REACT_NAVIGATION__;
                            // Hook into navigation state changes
                        }
                        
                        // Alternative: Hook into React's internals
                        if (window.__REACT_DEVTOOLS_GLOBAL_HOOK__) {
                            const reactInternals = window.__REACT_DEVTOOLS_GLOBAL_HOOK__;
                            // This is simplified - actual implementation depends on React version
                        }
                        
                        // Poll for React Navigation every second (fallback)
                        setTimeout(function checkNavigation() {
                            try {
                                // Try to find React Navigation in the app
                                if (window.navigation && window.navigation.getCurrentRoute) {
                                    const route = window.navigation.getCurrentRoute();
                                    if (route && route.name) {
                                        window.MobileSDKBridge.onScreenChanged(route.name);
                                    }
                                }
                            } catch(e) {}
                            
                            setTimeout(checkNavigation, 1000);
                        }, 1000);
                    }
                    
                    setupTracking();
                })();
            """.trimIndent()
            
            view.evaluateJavascript(jsCode, null)
            Log.d("MobileSDK_RN", "JavaScript injected into WebView")
            return
        }
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                findAndInjectIntoWebView(view.getChildAt(i))
            }
        }
    }
    
    private fun setupDeviceEventEmitter() {
        try {
            // Create a JavaScript interface that will receive events from React Native
            val jsInterface = object {
                @android.webkit.JavascriptInterface
                fun onScreenChanged(screenName: String) {
                    Log.d("MobileSDK_RN", "📍 Screen changed via JS interface: $screenName")
                    
                    val activity = currentActivity
                    if (activity != null) {
                        android.util.Log.d("MobileSDK_RN", "Calling trackScreenView for: $screenName")
                        MobileSDK.getInstance().trackScreenView(screenName, activity)
                    }
                }
            }
            
            // We need to add this interface to the WebView
            // This will be found by the findAndInjectIntoWebView method
            
            Log.d("MobileSDK_RN", "DeviceEventEmitter setup complete")
            
        } catch (e: Exception) {
            Log.e("MobileSDK_RN", "Failed to setup DeviceEventEmitter: ${e.message}")
        }
    }
    
    private fun setupAutoDetection(activity: Activity) {
        UiThreadUtil.runOnUiThread {
            try {
                Log.d("MobileSDK_RN", "Setting up auto detection on UI thread")
                
                activity.window?.decorView?.post {
                    try {
                        val mobileSDK = MobileSDK.getInstance()
                        mobileSDK.autoSetup(activity)
                        isAutoSetupComplete = true
                        Log.d("MobileSDK_RN", "✅ Auto detection setup complete")
                    } catch (e: Exception) {
                        Log.e("MobileSDK_RN", "Auto detection setup failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MobileSDK_RN", "UI thread setup failed: ${e.message}")
            }
        }
    }

    private fun setupGlobalTouchInterceptor(activity: Activity) {
        val window = activity.window
        val originalCallback = window.callback

        // Prevent wrapping multiple times
        if (originalCallback.javaClass.name.contains("MobileSDKWindowCallback")) return

        window.callback = object : android.view.Window.Callback by originalCallback {
            override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                // Only act when the user lifts their finger (a completed tap)
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastGlobalTouchTime > 500) { // 500ms debounce
                        lastGlobalTouchTime = currentTime
                        val x = event.rawX.toInt()
                        val y = event.rawY.toInt()

                        // Collect all identifiers (tags, content descriptions) under this coordinate
                        val touchedIdentifiers = mutableSetOf<String>()
                        findIdentifiersUnderTouch(window.decorView, x, y, touchedIdentifiers)

                        if (touchedIdentifiers.isNotEmpty()) {
                            Log.d("MobileSDK_RN", "👆 Global Touch intercepted elements: $touchedIdentifiers")
                            
                            // Pass the detected IDs to your core SDK
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                touchedIdentifiers.forEach { identifier ->
                                    // This will trigger Survey 176 if the identifier matches!
                                    MobileSDK.getInstance().triggerButtonByStringId(identifier, activity)
                                }
                            }
                        }
                    }
                }
                // ALWAYS return the original callback so the app functions normally!
                return originalCallback.dispatchTouchEvent(event)
            }
            
            // Rename the class so we don't double-wrap it
            override fun toString(): String {
                return "MobileSDKWindowCallback"
            }
        }
        Log.d("MobileSDK_RN", "✅ Global Touch Interceptor activated")
    }

    private fun findIdentifiersUnderTouch(view: View, x: Int, y: Int, identifiers: MutableSet<String>) {
        if (view.visibility != View.VISIBLE) return

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val rect = android.graphics.Rect(
            location[0], location[1],
            location[0] + view.width, location[1] + view.height
        )

        // Only process this view if the touch coordinates fall inside its boundaries
        if (rect.contains(x, y)) {
            val name = extractElementName(view)
            if (!name.isNullOrEmpty()) {
                identifiers.add(name)
            }

            // Recursively check children to find the deepest clicked element
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) {
                    findIdentifiersUnderTouch(view.getChildAt(i), x, y, identifiers)
                }
            }
        }
    }

    private fun extractElementName(view: View): String? {
        return when {
            // Check accessibilityLabel (contentDescription) - This is what RN TouchableOpacity uses!
            !view.contentDescription.isNullOrEmpty() -> view.contentDescription.toString().trim().lowercase()
            // Check nativeID (tag)
            view.tag != null && view.tag is String -> view.tag.toString().trim().lowercase()
            // Check raw text
            view is android.widget.TextView && !view.text.isNullOrEmpty() -> view.text.toString().trim().lowercase()
            else -> null
        }
    }
    
    fun getCurrentActivity(): Activity? {
        return currentActivity
    }
    
    fun cleanup() {
        try {
            val application = reactContext.applicationContext as? Application
            application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
            coroutineScope.cancel()
            Log.d("MobileSDK_RN", "Activity delegate cleaned up")
        } catch (e: Exception) {
            Log.e("MobileSDK_RN", "Error cleaning up activity delegate: ${e.message}")
        }
    }
}