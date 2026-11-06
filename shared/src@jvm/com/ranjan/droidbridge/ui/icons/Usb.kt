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

public val Icons.Filled.Usb: ImageVector
    get() {
        if (_usb != null) {
            return _usb!!
        }
        _usb = materialIcon(name = "Filled.Usb") {
            materialPath {
                moveTo(15.0f, 7.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(1.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-3.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(-3.0f, -4.0f)
                lineToRelative(-3.0f, 4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(8.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(-2.07f)
                curveToRelative(0.7f, -0.37f, 1.2f, -1.08f, 1.2f, -1.93f)
                curveToRelative(0.0f, -1.21f, -0.99f, -2.2f, -2.2f, -2.2f)
                curveToRelative(-1.21f, 0.0f, -2.2f, 0.99f, -2.2f, 2.2f)
                curveToRelative(0.0f, 0.85f, 0.5f, 1.56f, 1.2f, 1.93f)
                verticalLineTo(13.0f)
                curveToRelative(0.0f, 1.11f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(3.05f)
                curveToRelative(-0.71f, 0.37f, -1.2f, 1.1f, -1.2f, 1.95f)
                curveToRelative(0.0f, 1.22f, 0.99f, 2.2f, 2.2f, 2.2f)
                curveToRelative(1.21f, 0.0f, 2.2f, -0.98f, 2.2f, -2.2f)
                curveToRelative(0.0f, -0.85f, -0.49f, -1.58f, -1.2f, -1.95f)
                verticalLineTo(15.0f)
                horizontalLineToRelative(3.0f)
                curveToRelative(1.11f, 0.0f, 2.0f, -0.89f, 2.0f, -2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(1.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }
        return _usb!!
    }

private var _usb: ImageVector? = null
