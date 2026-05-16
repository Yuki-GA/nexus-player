package com.zen.myapplication.nexus.core.storage

val SafManager.GameEngine.canUseHtml5Runtime: Boolean
    get() = when (this) {
        SafManager.GameEngine.RPG_MAKER_MV,
        SafManager.GameEngine.RPG_MAKER_MZ,
        SafManager.GameEngine.RENPY_WEB,
        SafManager.GameEngine.TYRANOBUILDER,
        SafManager.GameEngine.CONSTRUCT,
        SafManager.GameEngine.UNITY_WEBGL,
        SafManager.GameEngine.GODOT_HTML5,
        SafManager.GameEngine.TWINE,
        SafManager.GameEngine.BITSY,
        SafManager.GameEngine.FLASH,
        SafManager.GameEngine.HTML5_CUSTOM -> true
        SafManager.GameEngine.RENPY,
        SafManager.GameEngine.RPG_MAKER_XP_VX_ACE,
        SafManager.GameEngine.UNSUPPORTED -> false
    }

val SafManager.GameEngine.canUseNativeRuntime: Boolean
    get() = this.requiresNativeRuntime

val SafManager.GameEngine.requiresNativeRuntime: Boolean
    get() = when (this) {
        SafManager.GameEngine.RENPY,
        SafManager.GameEngine.RPG_MAKER_XP_VX_ACE -> true
        else -> false
    }

val SafManager.GameEngine.displayName: String
    get() = when (this) {
        SafManager.GameEngine.RPG_MAKER_MV -> "RPG Maker MV"
        SafManager.GameEngine.RPG_MAKER_MZ -> "RPG Maker MZ"
        SafManager.GameEngine.RENPY_WEB -> "Ren'Py Web"
        SafManager.GameEngine.RENPY -> "Ren'Py"
        SafManager.GameEngine.RPG_MAKER_XP_VX_ACE -> "RPG Maker XP/VX/Ace"
        SafManager.GameEngine.TYRANOBUILDER -> "TyranoBuilder"
        SafManager.GameEngine.CONSTRUCT -> "Construct"
        SafManager.GameEngine.UNITY_WEBGL -> "Unity WebGL"
        SafManager.GameEngine.GODOT_HTML5 -> "Godot HTML5"
        SafManager.GameEngine.TWINE -> "Twine"
        SafManager.GameEngine.BITSY -> "Bitsy"
        SafManager.GameEngine.FLASH -> "Flash"
        SafManager.GameEngine.HTML5_CUSTOM -> "HTML5"
        SafManager.GameEngine.UNSUPPORTED -> "Unsupported"
    }
