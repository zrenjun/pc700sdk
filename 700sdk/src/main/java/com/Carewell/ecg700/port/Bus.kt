@file:Suppress("unused")

package com.Carewell.ecg700.port

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap

class EventBusCore : ViewModel() {

    companion object {
        /**
         * 每个事件类型（非粘性事件）在 SharedFlow 中最多缓存的条数。
         * 原实现用 Int.MAX_VALUE（近似无界），当某个页面处于后台（生命周期低于
         * observeEvent 要求的 minState）时，若仍有高频事件（如心电/血氧波形、
         * 血压实时数据等）持续 postEvent，会在缓冲区中无限堆积；页面恢复到前台后，
         * collect() 恢复消费会一次性把堆积的所有事件同步重放，可能在主线程造成
         * 明显卡顿甚至 ANR（已在实际 ANR dump 中观察到该现象）。
         * 改为有限容量 + BufferOverflow.DROP_OLDEST：超出容量时丢弃最旧的事件，
         * emit() 仍然是非阻塞的，正常前台使用时行为不变，仅在后台堆积场景下生效。
         */
        private const val EVENT_BUFFER_CAPACITY = 64
    }

    //正常事件
    private val eventFlows: ConcurrentHashMap<String, MutableSharedFlow<Any>> = ConcurrentHashMap()

    //粘性事件
    private val stickyEventFlows: ConcurrentHashMap<String, MutableSharedFlow<Any>> =
        ConcurrentHashMap()

    private fun getEventFlow(eventName: String, isSticky: Boolean): MutableSharedFlow<Any> {
        return if (isSticky) {
            stickyEventFlows[eventName]
        } else {
            eventFlows[eventName]
        } ?: MutableSharedFlow<Any>(
            replay = if (isSticky) 1 else 0,
            extraBufferCapacity = EVENT_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ).also {
            if (isSticky) {
                stickyEventFlows[eventName] = it
            } else {
                eventFlows[eventName] = it
            }
        }
    }

    fun <T : Any> observeEvent(
        lifecycleOwner: LifecycleOwner,
        eventName: String,
        minState: Lifecycle.State,
        dispatcher: CoroutineDispatcher,
        isSticky: Boolean,
        onReceived: (T) -> Unit
    ) {
        lifecycleOwner.launchWhenStateAtLeast(minState) {
            try {
                getEventFlow(eventName, isSticky).collect { value ->
                    if (dispatcher == Dispatchers.Main.immediate || dispatcher == Dispatchers.Main) {
                        invokeReceived(value, onReceived)
                    } else {
                        withContext(dispatcher) {
                            invokeReceived(value, onReceived)
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    suspend fun <T : Any> observeWithoutLifecycle(
        eventName: String,
        isSticky: Boolean,
        onReceived: (T) -> Unit
    ) {
        try {
            getEventFlow(eventName, isSticky).collect { value ->
                invokeReceived(value, onReceived)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }


    fun postEvent(eventName: String, value: Any, timeMillis: Long) {
        listOfNotNull(
            getEventFlow(eventName, false),
            getEventFlow(eventName, true)
        ).forEach { flow ->
            viewModelScope.launch {
                delay(timeMillis)
                flow.emit(value)
            }
        }
    }


    fun removeStickEvent(eventName: String) {
        stickyEventFlows.remove(eventName)
    }


    private fun <T : Any> invokeReceived(value: Any, onReceived: (T) -> Unit) {
        try {
            onReceived.invoke(value as T)
        } catch (e: ClassCastException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


fun <T> LifecycleOwner.launchWhenStateAtLeast(
    minState: Lifecycle.State,
    block: suspend CoroutineScope.() -> T
) {
    lifecycleScope.launch {
        lifecycle.whenStateAtLeast(minState, block)
    }
}


//_______________________________________
//          observe event
//_______________________________________

//监听App Scope 事件
@InternalCoroutinesApi
inline fun <reified T> LifecycleOwner.observeEvent(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    isSticky: Boolean = false,
    noinline onReceived: (T) -> Unit
) {
    ApplicationScopeViewModelProvider.getApplicationScopeViewModel(EventBusCore::class.java)
        .observeEvent(
            this,
            T::class.java.name,
            minActiveState,
            dispatcher,
            isSticky,
            onReceived
        )
}

//监听Fragment Scope 事件
@InternalCoroutinesApi
inline fun <reified T> observeEvent(
    scope: Fragment,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    isSticky: Boolean = false,
    noinline onReceived: (T) -> Unit
) {
    ViewModelProvider(scope).get(EventBusCore::class.java)
        .observeEvent(
            scope,
            T::class.java.name,
            minActiveState,
            dispatcher,
            isSticky,
            onReceived
        )
}

// 监听Activity Scope 事件
@InternalCoroutinesApi
inline fun <reified T> observeEvent(
    scope: ComponentActivity,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    isSticky: Boolean = false,
    noinline onReceived: (T) -> Unit
) {
    ViewModelProvider(scope).get(EventBusCore::class.java)
        .observeEvent(
            scope,
            T::class.java.name,
            minActiveState,
            dispatcher,
            isSticky,
            onReceived
        )
}


@InternalCoroutinesApi
inline fun <reified T> observeEvent(
    coroutineScope: CoroutineScope,
    isSticky: Boolean = false,
    noinline onReceived: (T) -> Unit
) {
    coroutineScope.launch {
        ApplicationScopeViewModelProvider.getApplicationScopeViewModel(EventBusCore::class.java)
            .observeWithoutLifecycle(
                T::class.java.name,
                isSticky,
                onReceived
            )
    }
}

//_______________________________________
//          post event
//_______________________________________

//Application范围的事件
inline fun <reified T> postEvent(event: T, timeMillis: Long = 0L) {
    ApplicationScopeViewModelProvider.getApplicationScopeViewModel(EventBusCore::class.java)
        .postEvent(T::class.java.name, event!!, timeMillis)
}

//Activity范围的事件
inline fun <reified T> postEvent(scope: ComponentActivity, event: T, timeMillis: Long = 0L) {
    ViewModelProvider(scope).get(EventBusCore::class.java)
        .postEvent(T::class.java.name, event!!, timeMillis)
}

//Fragment范围的事件
inline fun <reified T> postEvent(scope: Fragment, event: T, timeMillis: Long = 0L) {
    ViewModelProvider(scope).get(EventBusCore::class.java)
        .postEvent(T::class.java.name, event!!, timeMillis)
}

inline fun <reified T> removeStickyEvent(event: Class<T>) {
    ApplicationScopeViewModelProvider.getApplicationScopeViewModel(EventBusCore::class.java)
        .removeStickEvent(event.name)
}


inline fun <reified T> removeStickyEvent(scope: Fragment, event: Class<T>) {
    ViewModelProvider(scope).get(EventBusCore::class.java)
        .removeStickEvent(event.name)
}


inline fun <reified T> removeStickyEvent(scope: ComponentActivity, event: Class<T>) {
    ViewModelProvider(scope).get(EventBusCore::class.java)
        .removeStickEvent(event.name)
}

object ApplicationScopeViewModelProvider : ViewModelStoreOwner {

    private val eventViewModelStore: ViewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = eventViewModelStore

    private val mApplicationProvider: ViewModelProvider by lazy {
        ViewModelProvider(
            ApplicationScopeViewModelProvider,
            ViewModelProvider.AndroidViewModelFactory.getInstance(CommonApp.app)
        )
    }

    fun <T : ViewModel> getApplicationScopeViewModel(modelClass: Class<T>): T {
        return mApplicationProvider[modelClass]
    }
}


// 1. implementation("androidx.startup:startup-runtime:1.1.1")
// 2. class CommonApp : Initializer<Unit> {
//    override fun create(context: Context) {
//        //执行初始化逻辑
//    }
//
//    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
//}
// 3. <provider
//    android:name="androidx.startup.InitializationProvider"
//    android:authorities="${applicationId}.androidx-startup"
//    android:exported="false"
//    tools:node="merge">
//    <meta-data
//        android:name="com.Carewell.ecg700.CommonApp"
//        android:value="androidx.startup" />
//   </provider>


object CommonApp {
    private lateinit var _app: Application
    val app get() = _app

    fun init(app: Application) {
        _app = app
    }
}


