package com.algorigo.laxthaandchestbeltcollectapp.util.rxresult

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.algorigo.laxthaandchestbeltcollectapp.util.rxresult.callbacks.FragmentDetachedListener
import com.algorigo.laxthaandchestbeltcollectapp.util.rxresult.data.ActivityResult
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class RxIntentStarterFragment : Fragment() {

    private val requestSubjectMap = mutableMapOf<Int, PublishSubject<ActivityResult>>()
    private var fragmentDetachedListener: FragmentDetachedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onDetach() {
        super.onDetach()
        if (activity?.isFinishing == false) {
            fragmentDetachedListener?.onFragmentDetached()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        requestSubjectMap[requestCode]?.apply {
            onNext(ActivityResult(resultCode, data))
            onComplete()
        }
    }

    fun startActivityGrantedObservable(
        intent: Intent,
        isGranted: () -> Boolean,
    ): Observable<ActivityResult> {
        var counter = 0
        return Observable.create { emitter ->
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    counter++
                    if (counter >= 2) {
                        if (isGranted()) {
                            emitter.onNext(ActivityResult(Activity.RESULT_OK, null))
                        } else {
                            emitter.onNext(ActivityResult(Activity.RESULT_CANCELED, null))
                        }
                        emitter.onComplete()
                    }
                }
            }

            lifecycle.addObserver(observer)

            if (!isGranted()) {
                startActivity(intent)
            } else {
                emitter.onNext(ActivityResult(Activity.RESULT_OK, null))
                emitter.onComplete()
            }

            emitter.setCancellable {
                lifecycle.removeObserver(observer)
            }
        }
    }

    fun startActivityForResultObservable(intent: Intent): Observable<ActivityResult> {
        return getSubject(intent) ?: PublishSubject.create<ActivityResult>()
            .also { subject ->
                intent.apply {
                    setSubject(intent, subject)
                }
                    .also {
                        startActivityForResult(it, intent.hashCode())
                    }
            }
            .doFinally {
                removeSubject(intent)
            }
    }

    fun setFragmentDetachedListener(fragmentDetachedListener: FragmentDetachedListener?) {
        this.fragmentDetachedListener = fragmentDetachedListener
    }

    fun isRequestInProgress(): Boolean {
        return requestSubjectMap.isNotEmpty()
    }

    private fun getSubject(intent: Intent): PublishSubject<ActivityResult>? {
        return requestSubjectMap[intent.hashCode()]
    }

    private fun setSubject(intent: Intent, subject: PublishSubject<ActivityResult>) {
        requestSubjectMap[intent.hashCode()] = subject
    }

    private fun removeSubject(intent: Intent) {
        requestSubjectMap.remove(intent.hashCode())
    }
}
