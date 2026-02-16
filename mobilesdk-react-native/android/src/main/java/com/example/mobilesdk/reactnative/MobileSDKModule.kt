package com.c4f.mobileSDK.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.c4f.mobileSDK.MobileSDK
import com.c4f.mobileSDK.UniversalMobileSDK
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import androidx.annotation.Nullable

@ReactModule(name = "MobileSDK")
class MobileSDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    private var currentActivity: WeakReference<Activity>? = null
    private var isAutoSetupComplete = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var registeredReactButtons = mutableSetOf<String>()
    private var scrollDetectionEnabled = false
    
    
    init {
    Log.d("MobileSDK_RN_CRITICAL", "=== MODULE LOADED ===")
    Log.d("MobileSDK_RN_CRITICAL", "Class: ${this::class.java.name}")
        
    currentActivity = WeakReference(reactContext.currentActivity)

    // Print ALL methods this module exposes
    val methods = this::class.java.methods
    methods.filter { it.declaringClass == this::class.java }
        .filter { it.name == "initialize" || it.name.contains("initialize") }
        .forEach {
            Log.d("MobileSDK_RN_CRITICAL", "📌 EXPOSED METHOD: ${it.name}")
            Log.d("MobileSDK_RN_CRITICAL", "   Parameters: ${it.parameterTypes.joinToString { it.simpleName }}")
        }
}

    override fun getName(): String = "MobileSDK"

    // ✅ FOR SIMPLE INIT - NO PARAMS ARRAY
    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        Log.d("MobileSDK_RN", "✅ initialize")
        try {
            Log.d("MobileSDK_RN", "✅ initialize(apiKey) called")
            
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            val application = activity.application
            UniversalMobileSDK.getInstance().initialize(application, apiKey)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message)
        }
    }

    // ✅ FOR INIT WITH PARAMETERS
    @ReactMethod
    fun initializeWithParams(apiKey: String, params: ReadableArray, promise: Promise) {
        Log.d("MobileSDK_RN", "✅ initializeWithParams")
        try {
            Log.d("MobileSDK_RN", "✅ initializeWithParams called")
            
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            val application = activity.application
            val anyParams = convertToAnyArray(params)
            UniversalMobileSDK.getInstance().initialize(application, apiKey, *anyParams)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message)
        }
    }

    private fun convertToAnyArray(params: ReadableArray): Array<Any> {
        val result = mutableListOf<Any>()
        
        for (i in 0 until params.size()) {
            when (params.getType(i)) {
                ReadableType.String -> {
                    params.getString(i)?.let { result.add(it) }
                }
                ReadableType.Map -> {
                    val map = params.getMap(i)
                    map?.keySetIterator()?.let { iterator ->
                        while (iterator.hasNextKey()) {
                            val key = iterator.nextKey()
                            val value = map.getString(key)
                            if (key != null && value != null) {
                                result.add(Pair(key, value))
                            }
                        }
                    }
                }
                else -> {
                    Log.w("MobileSDK_RN", "⚠️ Skipping unsupported type: ${params.getType(i)}")
                }
            }
        }
        
        return result.toTypedArray()
    }

    @ReactMethod
    fun autoSetup(promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            activity.runOnUiThread {
                MobileSDK.getInstance().autoSetup(activity)
                promise.resolve(true)
            }
        } catch (e: Exception) {
            promise.reject("SETUP_ERROR", e.message)
        }
    }

    @ReactMethod
    fun autoSetupReact(promise: Promise) {
        try {
            Log.d("MobileSDK_RN", "🔄 RN: Starting autoSetup...")
            
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            activity.runOnUiThread {
                try {
                    activity.window?.decorView?.postDelayed({
                        try {
                            val mobileSDK = MobileSDK.getInstance()
                            
                            // First, let core SDK do its normal autoSetup
                            mobileSDK.autoSetup(activity)
                            
                            // Then retry navigation detection with increasing delays
                            retryNavigationDetection(mobileSDK, activity, 0)
                            
                            isAutoSetupComplete = true
                            Log.d("MobileSDK_RN", "✅ React Native auto setup completed")
                            promise.resolve(true)
                        } catch (e: Exception) {
                            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
                        }
                    }, 500)
                } catch (e: Exception) {
                    promise.reject("SETUP_ERROR", "UI thread setup failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
        }
    }

    private fun retryNavigationDetection(mobileSDK: MobileSDK, activity: Activity, attempt: Int) {
        if (attempt > 3) return // Max 3 retries
        
        val delay = when (attempt) {
            0 -> 1000L  // 1 second
            1 -> 2000L  // 2 seconds
            2 -> 3000L  // 3 seconds
            else -> 5000L
        }
        
        activity.window?.decorView?.postDelayed({
            try {
                Log.d("MobileSDK_RN", "🔄 Retry $attempt: Re-triggering navigation detection")
                mobileSDK.reSetupNavigationDetection(activity)
            } catch (e: Exception) {
                Log.e("MobileSDK_RN", "Navigation re-trigger failed on retry $attempt", e)
                // Try again with next attempt
                retryNavigationDetection(mobileSDK, activity, attempt + 1)
            }
        }, delay)
    }

    // ====================================================================
    // AUTO DETECTION - SDK WILL DETECT EVERYTHING AUTOMATICALLY
    // ====================================================================
    
    private fun setupAutoDetection(activity: Activity) {
        try {
            Log.d("MobileSDK_RN", "🔍 Setting up React Native auto-detection...")
            
            val rootView = activity.window.decorView
            
            // Monitor for view changes - helps detect navigation elements
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                scanForNavigationElements(rootView, activity)
            }
            
            // Initial scan
            scanForNavigationElements(rootView, activity)
            
            Log.d("MobileSDK_RN", "✅ React Native auto-detection ready")
            Log.d("MobileSDK_RN", "🎯 SDK will automatically detect:")
            Log.d("MobileSDK_RN", "   • Bottom navigation tabs")
            Log.d("MobileSDK_RN", "   • In-page tabs (Mens, Womens, etc.)")
            Log.d("MobileSDK_RN", "   • Button clicks")
            Log.d("MobileSDK_RN", "   • Screen navigation")
            
        } catch (e: Exception) {
            Log.e("MobileSDK_RN", "❌ Auto-detection setup failed", e)
        }
    }

    private fun scanForNavigationElements(view: View, activity: Activity) {
        try {
            val viewClass = view.javaClass.name
            
            // Look for navigation/tab elements
            if (viewClass.contains("Tab") || 
                viewClass.contains("Navigation") ||
                viewClass.contains("BottomNavigation") ||
                viewClass.contains("TabLayout") ||
                viewClass.contains("TabBar") ||
                viewClass.contains("TabItem") ||
                viewClass.contains("TabButton") ||
                viewClass.contains("ViewPager") ||
                viewClass.contains("Pager")) {
                
                Log.d("MobileSDK_RN", "✅ Found navigation element: ${viewClass.substringAfterLast('.')}")
                
                // Extract the name/label
                val elementName = extractElementName(view)
                
                elementName?.let { name ->
                    Log.d("MobileSDK_RN", "📍 Navigation element: $name")
                    
                    // FIX: Call triggerByNavigation instead of trackScreenView
                    activity.runOnUiThread {
                        MobileSDK.getInstance().triggerByNavigation(name, activity)
                    }
                }
            }
            
            // Check for selected state (active tab)
            if (view.isSelected) {
                val selectedName = extractElementName(view)
                selectedName?.let { name ->
                    Log.d("MobileSDK_RN", "📑 Selected tab detected: $name")
                    activity.runOnUiThread {
                        // For tabs, use triggerByTabChange
                        MobileSDK.getInstance().triggerByTabChange(name, activity)
                    }
                }
            }
            
            // Also check for clicks on navigation items
            if (view.isClickable) {
                val originalOnClickListener = findOnClickListener(view)
                
                view.setOnClickListener { v ->
                    // Call original listener
                    originalOnClickListener?.onClick(v)
                    
                    // Then trigger navigation
                    val clickName = extractElementName(v)
                    clickName?.let { name ->
                        Log.d("MobileSDK_RN", "👆 Navigation click: $name")
                        activity.runOnUiThread {
                            MobileSDK.getInstance().triggerByNavigation(name, activity)
                        }
                    }
                }
            }
            
            // Recursively scan children
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    scanForNavigationElements(view.getChildAt(i), activity)
                }
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }

    // Add this helper method to find original click listener
    private fun findOnClickListener(view: View): View.OnClickListener? {
        return try {
            val getListenerInfo = View::class.java.getMethod("getListenerInfo")
            getListenerInfo.isAccessible = true
            val listenerInfo = getListenerInfo.invoke(view)
            
            val mOnClickListener = listenerInfo.javaClass.getDeclaredField("mOnClickListener")
            mOnClickListener.isAccessible = true
            mOnClickListener.get(listenerInfo) as? View.OnClickListener
        } catch (e: Exception) {
            null
        }
    }

    private fun extractElementName(view: View): String? {
        return when {
            // TextView with text
            view is android.widget.TextView && !view.text.isNullOrEmpty() -> 
                view.text.toString().trim().lowercase()
            
            // Content description
            !view.contentDescription.isNullOrEmpty() -> 
                view.contentDescription.toString().trim().lowercase()
            
            // Tag
            view.tag != null -> 
                view.tag.toString().trim().lowercase()
            
            // Check child TextViews
            view is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    if (child is android.widget.TextView && !child.text.isNullOrEmpty()) {
                        return child.text.toString().trim().lowercase()
                    }
                }
                null
            }
            
            else -> null
        }
    }

    

    private fun helpDetectReactNavigation(activity: Activity) {
        try {
            Log.d("MobileSDK_RN", "📍 Helping core SDK detect React Navigation...")
            
            val rootView = activity.window.decorView
            
            // Look for common React Navigation containers
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                findAndNotifyNavigationElements(rootView, activity)
            }
            
            // Also do an initial scan
            findAndNotifyNavigationElements(rootView, activity)
            
        } catch (e: Exception) {
            Log.e("MobileSDK_RN", "❌ Failed to help navigation detection", e)
        }
    }

    private fun findAndNotifyNavigationElements(view: View, activity: Activity) {
        try {
            val viewClass = view.javaClass.name
            
            // Look for React Navigation specific elements
            if (viewClass.contains("BottomNavigation") ||
                viewClass.contains("TabLayout") ||
                viewClass.contains("TabBar") ||
                viewClass.contains("TabNavigator") ||
                viewClass.contains("TabItem") ||
                viewClass.contains("TabButton")) {
                
                Log.d("MobileSDK_RN", "✅ Found navigation element: ${viewClass.substringAfterLast('.')}")
                
                // Get the text/content of this navigation element
                val screenName = extractScreenName(view)
                
                screenName?.let { name ->
                    Log.d("MobileSDK_RN", "📍 Navigation element content: $name")
                    
                    // Notify core SDK about this screen
                    activity.runOnUiThread {
                        MobileSDK.getInstance().trackScreenView(name, activity)
                    }
                }
            }
            
            // Recursively check children
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findAndNotifyNavigationElements(view.getChildAt(i), activity)
                }
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }

    private fun extractScreenName(view: View): String? {
        return when {
            // Try to get text from TextView
            view is android.widget.TextView && !view.text.isNullOrEmpty() -> 
                view.text.toString().trim().lowercase()
            
            // Try content description
            !view.contentDescription.isNullOrEmpty() -> 
                view.contentDescription.toString().trim().lowercase()
            
            // Try tag
            view.tag != null -> 
                view.tag.toString().trim().lowercase()
            
            // Check for child TextViews
            view is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    if (child is android.widget.TextView && !child.text.isNullOrEmpty()) {
                        return child.text.toString().trim().lowercase()
                    }
                }
                null
            }
            
            else -> null
        }
    }

    // ====================================================================
    // TRIGGER METHODS
    // ====================================================================
    @ReactMethod
    fun triggerButtonSurvey(buttonId: String, promise: Promise) {
        try {
            Log.d("MobileSDK_RN", "🎯 RN Bridge: Manual trigger for button: $buttonId")
            val activity = getCurrentActivity()
            if (activity != null) {
                MobileSDK.getInstance().triggerButtonByStringId(buttonId, activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to trigger survey: ${e.message}")
        }
    }

    @ReactMethod
    fun triggerScrollSurvey(promise: Promise) {
        try {
            Log.d("MobileSDK_RN", "📜 RN Bridge: Manual scroll trigger")
            val activity = getCurrentActivity()
            if (activity != null) {
                MobileSDK.getInstance().triggerScrollManual(activity, 1000)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to trigger scroll survey: ${e.message}")
        }
    }

    @ReactMethod
    fun triggerNavigationSurvey(screenName: String, promise: Promise) {
        try {
            Log.d("MobileSDK_RN", "📍 RN Bridge: Manual navigation trigger: $screenName")
            val activity = getCurrentActivity()
            if (activity != null) {
                MobileSDK.getInstance().triggerByNavigation(screenName, activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to trigger navigation survey: ${e.message}")
        }
    }

    @ReactMethod
    fun enableNavigationSafety(promise: Promise) {
        try {
            MobileSDK.getInstance().enableNavigationSafety()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SAFETY_ERROR", "Failed to enable navigation safety")
        }
    }

    @ReactMethod
    fun autoSetupSafe(promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                MobileSDK.getInstance().autoSetupSafe(activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("SETUP_ERROR", "Failed to safe auto setup")
        }
    }

    // ====================================================================
    // SURVEY DISPLAY METHODS
    // ====================================================================
    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                MobileSDK.getInstance().showSurvey(activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("SHOW_ERROR", "Failed to show survey: ${e.message}")
        }
    }

    @ReactMethod
    fun showSurveyById(surveyId: String, promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                MobileSDK.getInstance().showSurveyById(activity, surveyId)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("SHOW_ERROR", "Failed to show survey $surveyId: ${e.message}")
        }
    }

    // ====================================================================
    // USER PROPERTIES
    // ====================================================================
    @ReactMethod
    fun setUserProperty(key: String, value: String, promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                activity.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
                    .edit().putString(key, value).apply()
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            promise.reject("PROPERTY_ERROR", "Failed to set user property: ${e.message}")
        }
    }

    @ReactMethod
    fun setSessionData(key: String, value: String, promise: Promise) {
        try {
            MobileSDK.getInstance().setSessionData(key, value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SESSION_ERROR", "Failed to set session data: ${e.message}")
        }
    }

    @ReactMethod
    fun resetSessionData(promise: Promise) {
        try {
            MobileSDK.getInstance().resetSessionData()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SESSION_ERROR", "Failed to reset session data: ${e.message}")
        }
    }

    // ====================================================================
    // STATUS & DEBUG METHODS
    // ====================================================================
    @ReactMethod
    fun isUserExcluded(promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().isUserExcluded())
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion: ${e.message}")
        }
    }

    @ReactMethod
    fun isUserExcludedForSurvey(surveyId: String, promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().isUserExcluded(surveyId))
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion for survey $surveyId: ${e.message}")
        }
    }

    @ReactMethod
    fun getDebugStatus(promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().debugSurveyStatus())
        } catch (e: Exception) {
            promise.reject("DEBUG_ERROR", "Failed to get debug status: ${e.message}")
        }
    }

    @ReactMethod
    fun getSurveyIds(promise: Promise) {
        try {
            val surveyIds = MobileSDK.getInstance().getSurveyIds()
            val writableArray = Arguments.createArray()
            surveyIds.forEach { writableArray.pushString(it) }
            promise.resolve(writableArray)
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to get survey IDs: ${e.message}")
        }
    }

    @ReactMethod
    fun isConfigurationLoaded(promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().isConfigurationLoaded())
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to check configuration status: ${e.message}")
        }
    }

    @ReactMethod
    fun getQueueStatus(promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().getQueueStatus())
        } catch (e: Exception) {
            promise.reject("QUEUE_ERROR", "Failed to get queue status: ${e.message}")
        }
    }

    @ReactMethod
    fun clearSurveyQueue(promise: Promise) {
        try {
            MobileSDK.getInstance().clearSurveyQueue()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("QUEUE_ERROR", "Failed to clear survey queue: ${e.message}")
        }
    }

    @ReactMethod
    fun isShowingSurvey(promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().isShowingSurvey())
        } catch (e: Exception) {
            promise.reject("SURVEY_ERROR", "Failed to check if survey is showing: ${e.message}")
        }
    }

    @ReactMethod
    fun isSDKEnabled(promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().isSDKEnabled())
        } catch (e: Exception) {
            promise.reject("STATUS_ERROR", "Failed to check if SDK is enabled: ${e.message}")
        }
    }

    @ReactMethod
    fun fetchConfiguration(promise: Promise) {
        try {
            MobileSDK.getInstance().fetchConfiguration()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to fetch configuration: ${e.message}")
        }
    }

    @ReactMethod
    fun getConfigForDebug(promise: Promise) {
        try {
            promise.resolve(MobileSDK.getInstance().getConfigForDebug())
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to get config debug info: ${e.message}")
        }
    }

    @ReactMethod
    fun resetTriggers(promise: Promise) {
        try {
            MobileSDK.getInstance().resetTriggers()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to reset triggers: ${e.message}")
        }
    }

    @ReactMethod
    fun cleanup(promise: Promise) {
        try {
            MobileSDK.getInstance().cleanup()
            coroutineScope.cancel()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CLEANUP_ERROR", "Failed to cleanup SDK: ${e.message}")
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        coroutineScope.cancel()
    }
}