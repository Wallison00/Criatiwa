package com.walli.flexcriatiwa

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.Normalizer
import java.util.UUID

class EscPosPrinter {
    private val buffer = ByteArrayOutputStream()

    private val CMD_RESET = byteArrayOf(0x1B, 0x40)
    private val CMD_TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    private val CMD_TEXT_DOUBLE_HEIGHT = byteArrayOf(0x1D, 0x21, 0x11)
    private val CMD_TEXT_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val CMD_TEXT_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val CMD_FEED = byteArrayOf(0x0A)

    private fun addCommand(cmd: ByteArray) {
        try {
            buffer.write(cmd)
        } catch (ignored: IOException) {}
    }

    private fun addText(text: String) {
        val cleanText = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        for (c in cleanText.toCharArray()) {
            buffer.write(c.code)
        }
    }

    private fun addLine(text: String) {
        addText(text)
        addCommand(CMD_FEED)
    }

    private fun addLine() {
        addCommand(CMD_FEED)
    }

    private fun addSeparator() {
        val chars = CharArray(32) { '-' }
        addLine(String(chars))
    }

    private fun splitText(text: String, maxLength: Int): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val end = (i + maxLength).coerceAtMost(text.length)
            result.add(text.substring(i, end))
            i += maxLength
        }
        return result
    }

    fun addQRCode(content: String) {
        try {
            val data = content.toByteArray(charset("UTF-8"))
            val storeLen = data.size + 3
            val pL = (storeLen % 256).toByte()
            val pH = (storeLen / 256).toByte()

            // Selecionar Modelo (Model 2)
            addCommand(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))

            // Tamanho do módulo QR Code (ajustado para ficar fácil de ler)
            addCommand(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06))

            // Correção de Erro Nível M
            addCommand(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))

            // Coloca os dados na memória da impressora
            val storeCommand = byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30)
            buffer.write(storeCommand)
            buffer.write(data)

            // Imprime o QR Code atual e limpa memória
            addCommand(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
        } catch (ignored: Exception) {}
    }

    data class Pedido(
        val id: String,
        val mesa: String,
        val tipoDestino: String,
        val hora: String,
        val itens: List<Item>
    )

    data class Item(
        val qtd: Int,
        val nome: String,
        val observacoes: List<String> = emptyList()
    )

    fun gerarBufferBytes(cabecalho: String, pedido: Pedido): ByteArray {
        addCommand(CMD_RESET)

        addCommand(CMD_ALIGN_CENTER)
        addCommand(CMD_TEXT_BOLD_ON)
        addLine(cabecalho)
        addCommand(CMD_TEXT_BOLD_OFF)
        addLine()
        
        addCommand(CMD_TEXT_DOUBLE_HEIGHT)
        addCommand(CMD_TEXT_BOLD_ON)
        addLine("[ ${pedido.tipoDestino.uppercase()} ]")
        addCommand(CMD_TEXT_BOLD_OFF)
        addCommand(CMD_TEXT_NORMAL)
        addLine()

        addCommand(CMD_ALIGN_LEFT)
        addCommand(CMD_TEXT_DOUBLE_HEIGHT)
        addLine(pedido.mesa)
        addLine("PEDIDO: ${pedido.id}")
        addCommand(CMD_TEXT_NORMAL)
        addSeparator()
        addLine()

        for (item in pedido.itens) {
            val qtdFormatada = String.format("%02d", item.qtd)
            val linhaItem = "${qtdFormatada}x ${item.nome}"
            addLine(linhaItem.substring(0, linhaItem.length.coerceAtMost(32)))

            for (obs in item.observacoes) {
                val obsLines = splitText("  - $obs", 32)
                for (line in obsLines) {
                    addLine(line)
                }
            }
        }

        addSeparator()
        addLine()
        addCommand(CMD_ALIGN_CENTER)
        addLine(pedido.hora)
        addLine()
        addLine("Desenvolvido por: Criatiwa")
        addLine()
        addLine()
        addLine()

        return buffer.toByteArray()
    }

    fun gerarEtiquetaQRCodeDesktop(nomeMae: String, macAddress: String): ByteArray {
        addCommand(CMD_RESET)
        addCommand(CMD_ALIGN_CENTER)

        addCommand(CMD_TEXT_BOLD_ON)
        addLine("ETIQUETA DE MAQUINA P/ LEITURA")
        addCommand(CMD_TEXT_BOLD_OFF)
        addSeparator()

        addLine("Máquina: $nomeMae")
        addCommand(CMD_TEXT_BOLD_ON)
        addLine("MAC: $macAddress")
        addCommand(CMD_TEXT_BOLD_OFF)
        addLine()

        addQRCode(macAddress) // Apenas o MAC vai no QR Code do Papel

        addLine()
        addLine("Para imprimir desta maquina, abra")
        addLine("no App PDV e BIPE o leitor aqui!")

        addLine()
        addLine()
        addLine()
        return buffer.toByteArray()
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        @SuppressLint("MissingPermission")
        fun imprimirBuffer(context: Context, macAddress: String, buffer: ByteArray): Boolean {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter ?: return false

            if (!adapter.isEnabled) return false

            try {
                val device = adapter.getRemoteDevice(macAddress)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val outStream = socket.outputStream
                outStream.write(buffer)
                outStream.flush()
                Thread.sleep(200)
                socket.close()
                return true
            } catch (e: Exception) {
                Log.e("EscPosPrinter", "Erro ao imprimir", e)
                try {
                    // Tenta método alternativo com reflection caso falhe a conexão padrão
                    val device = adapter.getRemoteDevice(macAddress)
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    val socket = m.invoke(device, 1) as android.bluetooth.BluetoothSocket
                    socket.connect()
                    val outStream = socket.outputStream
                    outStream.write(buffer)
                    outStream.flush()
                    Thread.sleep(200)
                    socket.close()
                    return true
                } catch (e2: Exception) {
                    Log.e("EscPosPrinter", "Erro ao imprimir no método alternativo", e2)
                }
                return false
            }
        }
    }
}
