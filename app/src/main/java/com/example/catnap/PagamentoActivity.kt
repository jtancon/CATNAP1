package com.example.catnap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONObject

class PagamentoActivity : AppCompatActivity() {

    private lateinit var paymentsClient: PaymentsClient
    private lateinit var googlePayButton: Button
    private lateinit var valorPagamento: EditText
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagamento)

        googlePayButton = findViewById(R.id.googlePayButton)
        valorPagamento = findViewById(R.id.valor_pagamento)

        // Configuração do Google Pay
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()

        paymentsClient = Wallet.getPaymentsClient(this, walletOptions)

        // Verificar se o dispositivo está pronto para Google Pay
        checkIfReadyToPay()

        // Configurar ação do botão Google Pay
        googlePayButton.setOnClickListener {
            requestPayment()
        }
    }

    private fun checkIfReadyToPay() {
        val readyToPayRequest = IsReadyToPayRequest.fromJson(baseConfigurationJson().toString())

        val task: Task<Boolean> = paymentsClient.isReadyToPay(readyToPayRequest)
        task.addOnCompleteListener { completeTask ->
            if (completeTask.isSuccessful) {
                showGooglePayButton(completeTask.result == true)
            } else {
                showGooglePayButton(false)
            }
        }
    }

    private fun showGooglePayButton(userIsReadyToPay: Boolean) {
        googlePayButton.visibility = if (userIsReadyToPay) View.VISIBLE else View.VISIBLE
    }

    private fun requestPayment() {
        val paymentRequestJson = createPaymentRequestJson()
        val request = PaymentDataRequest.fromJson(paymentRequestJson.toString())

        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            this,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    private fun createPaymentRequestJson(): JSONObject {
        val paymentRequestJson = JSONObject()

        // Definir um valor mínimo para teste
        val totalPrice = valorPagamento.text.toString().ifEmpty { "10.00" }

        // Configuração de informações da transação
        paymentRequestJson.put(
            "transactionInfo", JSONObject()
                .put("totalPrice", totalPrice)
                .put("totalPriceStatus", "FINAL")
                .put("currencyCode", "BRL")
                .put("countryCode", "BR")
        )

        // Informações do comerciante (nome apenas para ambiente de teste)
        paymentRequestJson.put(
            "merchantInfo", JSONObject()
                .put("merchantName", "Example Merchant") // Retire o merchantID em modo de teste
        )

        // Método de pagamento permitido com tokenização direta
        val cardPaymentMethod = JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", JSONArray(arrayOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
                put("allowedCardNetworks", JSONArray(arrayOf("VISA", "MASTERCARD")))
            })
            put("tokenizationSpecification", getTokenizationSpec())
        }

        // Adicionando os métodos de pagamento permitidos
        paymentRequestJson.put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))

        return paymentRequestJson
    }

    private fun getTokenizationSpec(): JSONObject {
        return JSONObject().apply {
            put("type", "DIRECT")
            put("parameters", JSONObject().apply {
                put("protocolVersion", "ECv2")
                put(
                    "publicKey",
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGnJ7Yo1sX9b4kr4Aa5uq58JRQfzD8bIJXw7WXaap/hVE+PnFxvjx4nVxt79SdRuUVeu++HZD0cGAv4IOznc96w=="
                )
            })
        }
    }

    private fun baseConfigurationJson(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    // O pagamento foi concluído com sucesso
                    // Manipule o objeto `PaymentData` aqui
                }

                RESULT_CANCELED -> {
                    // O usuário cancelou o pagamento
                }

                AutoResolveHelper.RESULT_ERROR -> {
                    // Ocorreu um erro
                    val status = AutoResolveHelper.getStatusFromIntent(data)
                    // Trate o erro com base no `status`
                }
            }
        }
    }
}
