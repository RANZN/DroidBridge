package com.ranjan.adbsend.ui

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

/**
 * AWT transferable that hands Finder a list of local files. The files are pulled from the
 * device in the background as soon as the drag starts; Finder blocks on the result at drop time.
 */
class StagedFilesTransferable(private val stagedPaths: Deferred<List<String>>) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.javaFileListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.javaFileListFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return runBlocking { stagedPaths.await() }.map(::File)
    }
}
