package com.ranjan.adbsend.ui

import com.ranjan.adbsend.presentation.main.DragOutRequest
import kotlinx.coroutines.runBlocking
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

/**
 * AWT transferable that hands Finder a list of local files. The files are pulled from the
 * device only when the drop target asks for the data, and the drop waits until that finishes.
 */
class StagedFilesTransferable(private val request: DragOutRequest) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.javaFileListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.javaFileListFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return runBlocking { request.localPaths() }.map(::File)
    }
}
