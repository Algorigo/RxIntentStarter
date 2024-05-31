package com.algorigo.rxintentstarterapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.algorigo.rxintentstarter.RxIntentStarter
import com.algorigo.rxintentstarter.data.ActivityRequest
import com.algorigo.rxintentstarterapp.databinding.ActivityMainBinding
import com.tbruyelle.rxpermissions3.RxPermissions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var rxIntentStarter: RxIntentStarter
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rxIntentStarter = RxIntentStarter.create(this)

        binding.btnOpenAppSettings.setOnClickListener {
            rxIntentStarter.requestEach(
                ActivityRequest(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    },
                )
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(this, "Open app settings canceled", Toast.LENGTH_SHORT).show()
                    }, {
                        Log.e(TAG, "Settings open error: $it")
                    }
                )
                .addTo(compositeDisposable)
        }

        binding.btnOverlayPermission.setOnClickListener {
            rxIntentStarter.requestEach(
                ActivityRequest(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    },
                ) {
                    Settings.canDrawOverlays(this)
                },
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
        }

        binding.btnBluetoothPermission.setOnClickListener {
            RxPermissions(this).requestEachCombined(*bluetoothPermissions())
                .flatMap {
                    if (it.granted) {
                        rxIntentStarter.requestEach(
                            ActivityRequest(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                            ) {
                                (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled
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
        }

        binding.btnActionSend.setOnClickListener {
            rxIntentStarter.requestEach(
                ActivityRequest(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Hello, world!")
                    },
                ),
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(this, "Request has been completed", Toast.LENGTH_SHORT).show()
                    }, {
                        Log.e(TAG, "Action send error: $it")
                    }
                )
                .addTo(compositeDisposable)
        }

        binding.btnActionSendMultiple.setOnClickListener {
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
        }

        binding.btnRequestAll.setOnClickListener {
            rxIntentStarter.requestEachCombined(
                ActivityRequest(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    },
                ),
                ActivityRequest(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    },
                ) {
                    Settings.canDrawOverlays(this)
                },
                ActivityRequest(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                ) {
                    (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled
                },
                ActivityRequest(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Hello, world!")
                    },
                ),
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(this, "All requests have been completed", Toast.LENGTH_SHORT).show()
                    }, {
                        Log.e(TAG, "Request all error: $it")
                    }
                )
                .addTo(compositeDisposable)
        }
    }

    private fun bluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
