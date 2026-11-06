/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied from Compose material-icons-extended 1.7.3 so the app does not ship the
// whole 36 MB icon library for the few icons it uses.

package com.ranjan.droidbridge.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.UsbOff: ImageVector
    get() {
        if (_usbOff != null) {
            return _usbOff!!
        }
        _usbOff = materialIcon(name = "Filled.UsbOff") {
            materialPath {
                moveTo(15.0f, 8.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(-1.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(0.0f, 0.34f, -0.08f, 0.66f, -0.23f, 0.94f)
                lineTo(16.0f, 13.17f)
                verticalLineTo(12.0f)
                horizontalLineToRelative(-1.0f)
                verticalLineTo(8.0f)
                close()
                moveTo(11.0f, 8.17f)
                lineToRelative(2.0f, 2.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(-3.0f, -4.0f)
                lineTo(9.0f, 6.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(8.17f)
                close()
                moveTo(13.0f, 16.0f)
                verticalLineToRelative(2.28f)
                curveToRelative(0.6f, 0.34f, 1.0f, 0.98f, 1.0f, 1.72f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
                curveToRelative(0.0f, -0.74f, 0.4f, -1.37f, 1.0f, -1.72f)
                verticalLineTo(16.0f)
                horizontalLineTo(8.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, -0.89f, -2.0f, -2.0f)
                verticalLineToRelative(-2.28f)
                curveTo(5.4f, 11.38f, 5.0f, 10.74f, 5.0f, 10.0f)
                curveToRelative(0.0f, -0.59f, 0.26f, -1.13f, 0.68f, -1.49f)
                lineTo(1.39f, 4.22f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(18.38f, 18.38f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(13.17f, 16.0f)
                horizontalLineTo(13.0f)
                close()
                moveTo(11.0f, 14.0f)
                verticalLineToRelative(-0.17f)
                lineToRelative(-2.51f, -2.51f)
                curveToRelative(-0.14f, 0.16f, -0.31f, 0.29f, -0.49f, 0.4f)
                verticalLineTo(14.0f)
                horizontalLineTo(11.0f)
                close()
            }
        }
        return _usbOff!!
    }

private var _usbOff: ImageVector? = null
