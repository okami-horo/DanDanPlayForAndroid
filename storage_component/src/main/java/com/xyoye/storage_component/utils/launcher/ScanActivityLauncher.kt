package com.xyoye.storage_component.utils.launcher

import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.xyoye.data_component.data.RemoteScanData
import com.xyoye.storage_component.ui.activities.remote_scan.RemoteScanActivity

/**
 * Created by xyoye on 2023/4/24
 */

class ScanActivityLauncher(
    private val activity: FragmentActivity,
    private val onResult: (RemoteScanData?) -> Unit
) : DefaultLifecycleObserver {
    companion object {
        private const val KEY_LAUNCH_SCAN_ACTIVITY = "key_launch_scan_activity"
    }

    private lateinit var launchScanActivity: ActivityResultLauncher<Intent?>

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        launchScanActivity =
            activity.activityResultRegistry.register(
                KEY_LAUNCH_SCAN_ACTIVITY,
                owner,
                ActivityResultContracts.StartActivityForResult(),
            ) {
                val scanData =
                    it.data?.let { intent ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra("scan_data", RemoteScanData::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra("scan_data")
                        }
                    }
                onResult.invoke(scanData)
            }
    }

    fun launch() {
        launchScanActivity.launch(Intent(activity, RemoteScanActivity::class.java))
    }
}
