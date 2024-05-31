package com.algorigo.rxintentstarter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.algorigo.rxintentstarter.data.ActivityRequest
import com.algorigo.rxintentstarter.data.ActivityResult
import io.reactivex.rxjava3.core.Observable

class RxIntentStarter(
    fragmentManager: FragmentManager
) {

    private var rxIntentStarterFragment: RxIntentStarterFragment? = null

    init {
        rxIntentStarterFragment = getRxIntentStarterFragment(fragmentManager)
    }

    private fun getRxIntentStarterFragment(fragmentManager: FragmentManager): RxIntentStarterFragment {
        var rxIntentStarterFragment = fragmentManager.findFragmentByTag(TAG) as RxIntentStarterFragment?

        if (rxIntentStarterFragment == null) {
            rxIntentStarterFragment = RxIntentStarterFragment()
            fragmentManager
                .beginTransaction()
                .add(rxIntentStarterFragment, TAG)
                .commitNow()
        }
        return rxIntentStarterFragment
    }

    fun requestEachWithImmediateCancelSuccessful(vararg activityRequest: ActivityRequest): Observable<Boolean> {
        return requestEach(*activityRequest)
            .doOnNext {
                if (it.isCanceled()) {
                    throw RuntimeException("Request canceled: ${it.resultCode}")
                }
            }
            .map { it.isCanceled() }
    }

    fun requestEachCombinedSuccessful(vararg activityRequest: ActivityRequest): Observable<Boolean> {
        return requestEach(*activityRequest)
            .buffer(activityRequest.size)
            .flatMap { results ->
                if (results.isEmpty()) {
                    Observable.empty()
                } else {
                    results
                        .all { it.isOk() }
                        .let { isGranted ->
                            Observable.just(isGranted)
                        }
                }
            }
    }

    fun requestEachSuccessful(vararg activityRequest: ActivityRequest): Observable<Boolean> {
        return requestEach(*activityRequest)
            .flatMap {
                Observable.just(it.isOk())
            }
    }

    fun requestEachCombined(vararg activityRequest: ActivityRequest): Observable<List<ActivityResult>> {
        return requestEach(*activityRequest)
            .buffer(activityRequest.size)
            .flatMap { results ->
                if (results.isEmpty()) {
                    Observable.empty()
                } else {
                    Observable.just(results.toList())
                }
            }
    }

    fun requestEachWithImmediateCancel(vararg activityRequest: ActivityRequest): Observable<ActivityResult> {
        return requestEach(*activityRequest)
            .doOnNext {
                if (it.isCanceled()) {
                    throw RuntimeException("Request canceled: ${it.resultCode}")
                }
            }
    }

    fun requestEach(vararg activityRequest: ActivityRequest): Observable<ActivityResult> {
        return request(Observable.fromIterable(activityRequest.toList()))
    }

    private fun request(requestObservable: Observable<ActivityRequest>): Observable<ActivityResult> {
        return requestObservable
            .concatMap(::requestImpl)
    }

    private fun requestImpl(request: ActivityRequest): Observable<ActivityResult> {
        val rxIntentStarterFragment = checkNotNull(rxIntentStarterFragment) { "RxActivityResultsFragment must not be null." }
        return if (request.intent.action != null && request.intent.action!!.lowercase().contains(ACTION_SETTINGS)) {
            rxIntentStarterFragment.startActivityGrantedObservable(request.intent) {
                request.isGranted?.invoke() ?: false
            }
        } else {
            rxIntentStarterFragment.startActivityForResultObservable(request.intent)
        }
    }

    companion object {

        private val TAG: String = RxIntentStarter::class.java.simpleName

        private const val ACTION_SETTINGS = "settings"

        fun create(fragmentActivity: FragmentActivity): RxIntentStarter {
            return RxIntentStarter(fragmentActivity.supportFragmentManager)
        }

        fun create(fragment: Fragment): RxIntentStarter {
            return RxIntentStarter(fragment.childFragmentManager)
        }
    }
}