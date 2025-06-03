package com.kuvandikov.nfc

import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kuvandikov.nfc.databinding.ActivityMainBinding
import io.github.kuvandikov.scan_card_nfc.CardNfcAsyncTask
import io.github.kuvandikov.scan_card_nfc.utils.CardNfcUtils
import kotlinx.coroutines.launch

const val TAG = "NFC"
class MainActivity : AppCompatActivity(), CardNfcAsyncTask.CardNfcInterface {

    private lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    private var mNfcAdapter: NfcAdapter? = null
    private lateinit var mCardNfcUtils: CardNfcUtils
    private lateinit var mCardNfcAsyncTask: CardNfcAsyncTask
    private var mIntentFromCreate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        mCardNfcUtils = CardNfcUtils(this)

        if (mNfcAdapter?.isEnabled == true){
            mCardNfcAsyncTask = CardNfcAsyncTask.Builder(this, intent, mIntentFromCreate).build()
        }

        binding.nfcSwitch.setOnCheckedChangeListener { _, isChecked ->
            when {
                mNfcAdapter == null -> {
                    binding.nfcSwitch.isChecked = false
                    binding.nfcSwitch.isEnabled = false
                }
                mNfcAdapter?.isEnabled == true -> {
                    binding.nfcSwitch.isEnabled = true
                }
                else -> {
                    binding.nfcSwitch.isChecked = false
                    binding.nfcSwitch.isEnabled = false
                }
            }
            if (isChecked) {
                if (isNfcEnabled()) {
                    nfcEnabled()
                } else {
                    binding.nfcSwitch.isChecked = false
                }
            } else {
                nfcDisabled()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.status.collect { status ->
                    Log.i(TAG, "status: $status")
                    if (status.isNotEmpty()) {
                        binding.nfcStatus.text = status
                    }
                }
            }
        }
    }

    fun isNfcEnabled() = (mNfcAdapter != null && mNfcAdapter?.isEnabled == true)


    fun nfcEnabled() = mCardNfcUtils.enableDispatch()

    fun nfcDisabled() = mCardNfcUtils.disableDispatch()


    override fun startNfcReadCard() {
        viewModel.updateStatus("startNfcReadCard")
    }

    override fun cardIsReadyToRead() {
        viewModel.updateStatus("cardIsReadyToRead")
        val builder = StringBuilder().apply {
            append(mCardNfcAsyncTask.cardNumber)
            append("\n")
            append(mCardNfcAsyncTask.cardExpireDate)
            append("\n")
            append(mCardNfcAsyncTask.cardType)

        }
        viewModel.updateStatus(builder.toString())
        binding.instructionText.text = builder.toString()
    }

    override fun doNotMoveCardSoFast() {
        viewModel.updateStatus("doNotMoveCardSoFast")
    }

    override fun unknownEmvCard() {
        viewModel.updateStatus("unknownEmvCard")
    }

    override fun cardWithLockedNfc() {
        Log.i(TAG, "cardWithLockedNfc")
    }

    override fun finishNfcReadCard() {
        viewModel.updateStatus("finishNfcReadCard")
        nfcDisabled()
    }
}