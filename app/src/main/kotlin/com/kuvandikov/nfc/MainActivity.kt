package com.kuvandikov.nfc

import android.annotation.SuppressLint
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kuvandikov.nfc.ui.theme.MyApplicationTheme
import io.github.kuvandikov.scan_card_nfc.CardNfcAsyncTask
import io.github.kuvandikov.scan_card_nfc.utils.CardNfcUtils

const val TAG = "NFC"
class MainActivity : ComponentActivity(), CardNfcAsyncTask.CardNfcInterface{

    private val viewModel: MainViewModel by viewModels()
    private var mNfcAdapter: NfcAdapter? = null
    private lateinit var mCardNfcUtils: CardNfcUtils
    private lateinit var mCardNfcAsyncTask: CardNfcAsyncTask

    private var mIntent: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        mCardNfcUtils = CardNfcUtils(this)
        mIntent = true
        handleIntent(intent)

        if (mNfcAdapter?.isEnabled == true){
            mCardNfcAsyncTask = CardNfcAsyncTask.Builder(this, intent, mIntent).build()
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HomeScreen(
                    nfcEnabled = {
                        nfcEnabled()
                    },
                    viewModel = viewModel
                )
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let(::handleIntent)
    }

    private fun handleIntent(intent: Intent) {
        //HandleNFC
        if (mNfcAdapter?.isEnabled == true) {
            mCardNfcAsyncTask = CardNfcAsyncTask.Builder(this, intent, mIntent).build()
        }
    }

    fun isNfcEnabled() = (mNfcAdapter != null && mNfcAdapter?.isEnabled == true)


    fun nfcEnabled() = mCardNfcUtils.enableDispatch()

    fun nfcDisabled() = mCardNfcUtils.disableDispatch()

    override fun startNfcReadCard() {
    }

    override fun cardIsReadyToRead() {
        viewModel.readCard(
            number = mCardNfcAsyncTask.cardNumber,
            expireDate = mCardNfcAsyncTask.cardExpireDate,
            type = mCardNfcAsyncTask.cardType
        )
    }

    override fun doNotMoveCardSoFast() {
    }

    override fun unknownEmvCard() {
    }

    override fun cardWithLockedNfc() {
    }

    override fun finishNfcReadCard() {
        nfcDisabled()
    }
}
