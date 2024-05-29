package com.algorigo.laxthaandchestbeltcollectapp.util.rxresult

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.algorigo.laxthaandchestbeltcollectapp.util.rxresult.data.ActivityRequest
import com.algorigo.laxthaandchestbeltcollectapp.util.rxresult.data.ActivityResult
import io.reactivex.rxjava3.core.Observable

class RxIntentStarter private constructor(
    private val fragmentManager: FragmentManager
) {

    private fun getRxIntentStarterFragment(fragmentManager: FragmentManager): RxIntentStarterFragment {
        var rxIntentStarterFragment = fragmentManager.findFragmentByTag(TAG) as RxIntentStarterFragment?

        if (rxIntentStarterFragment == null) {
            rxIntentStarterFragment = RxIntentStarterFragment()
                .apply {
                    setFragmentDetachedListener {
                        throw RuntimeException("Fragment is detached.")
                    }
                }

            fragmentManager
                .beginTransaction()
                .add(rxIntentStarterFragment, TAG)
                .commitNow()
        }

        return rxIntentStarterFragment
    }

    private fun removeRxIntentStarterFragment(fragmentManager: FragmentManager) {
        val rxIntentStarterFragment = fragmentManager.findFragmentByTag(TAG) as RxIntentStarterFragment?
        if (rxIntentStarterFragment != null) {
            rxIntentStarterFragment.setFragmentDetachedListener(null)
            fragmentManager
                .beginTransaction()
                .remove(rxIntentStarterFragment)
                .commitNow()
        }
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
        var rxIntentStarterFragment: RxIntentStarterFragment? = null
        return Observable.fromIterable(activityRequest.toList())
            .concatMap { request ->
                val fragment = checkNotNull(rxIntentStarterFragment) { "RxIntentStarterFragment is detached." }
                if (request.intent.action != null && request.intent.action!!.lowercase().contains(ACTION_SETTINGS)) {
                    fragment.startActivityGrantedObservable(request.intent) {
                        request.isGranted?.invoke() ?: false
                    }
                } else {
                    fragment.startActivityForResultObservable(request.intent)
                }
            }
            .doOnSubscribe {
                rxIntentStarterFragment = getRxIntentStarterFragment(fragmentManager)
            }
            .doFinally {
                if (rxIntentStarterFragment?.isRequestInProgress() == false) {
                    removeRxIntentStarterFragment(fragmentManager)
                }
                rxIntentStarterFragment = null
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
