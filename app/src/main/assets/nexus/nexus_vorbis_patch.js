/**
 * nexus_vorbis_patch.js
 * Hardened Audio Recovery for Nexus Player.
 * Provides a safe compatibility decoder and deep async diagnostics.
 */
(function() {
    console.log("[NEXUS_WASM] Forensic recovery pass starting...");

    // 1. Full Environment Diagnostics
    const env = {
        windowKeys: Object.keys(window).filter(k => k.length < 25),
        webAssembly: typeof WebAssembly,
        audioContext: typeof AudioContext,
        sharedArrayBuffer: typeof SharedArrayBuffer,
        userAgent: navigator.userAgent
    };
    console.log("[NEXUS_WASM] Environment State:", JSON.stringify(env));

    // 2. Global Forensic Listeners
    window.addEventListener('error', function(e) {
        console.error("[NEXUS_WASM] RUNTIME ERROR:", {
            message: e.message,
            filename: e.filename,
            lineno: e.lineno,
            colno: e.colno,
            stack: e.error ? e.error.stack : "NO STACK"
        });
        
        if (e.filename && e.filename.includes('vorbisdecoder')) {
            activateSafeFallback("Runtime Error in Decoder");
            e.preventDefault();
        }
    }, true);

    window.addEventListener('unhandledrejection', function(e) {
        console.error("[NEXUS_WASM] PROMISE REJECTION:", e.reason);
        activateSafeFallback("Promise Rejection in Decoder");
    });

    // 3. Safe Compatibility Decoder
    // Prevents ".length" crash by returning a valid empty structure
    function activateSafeFallback(reason) {
        console.warn("[NEXUS_WASM] FALLBACK ACTIVATED: " + reason);
        
        window.VorbisDecoder = {
            decode: async function() {
                console.log("[NEXUS_WASM] Silent decoder providing empty buffer structure.");
                return {
                    samples: new Float32Array(0),
                    sampleRate: 44100,
                    channels: 2,
                    length: 0
                };
            }
        };

        // Tell RPG Maker to use HTML5 audio path
        if (window.WebAudio) WebAudio._canPlayOgg = false;
        if (window.AudioManager) AudioManager.shouldUseHtml5Audio = () => true;
    }

    // 4. Hard WASM Loader Override
    window.Module = window.Module || {};
    Module.instantiateWasm = async function(imports, successCallback) {
        console.log("[NEXUS_WASM] Manual instantiateWasm triggered");
        console.log("[NEXUS_WASM] Import namespaces:", Object.keys(imports || {}));

        try {
            const response = await fetch("https://nexus.local/js/libs/vorbisdecoder.wasm");
            if (!response.ok) throw new Error("WASM fetch failed: " + response.status);
            
            const bytes = await response.arrayBuffer();
            console.log("[NEXUS_WASM] WASM binary loaded:", bytes.byteLength, "bytes");

            const result = await WebAssembly.instantiate(bytes, imports);
            console.log("[NEXUS_WASM] WASM instantiation success.");

            // Call successCallback BEFORE returning exports
            successCallback(result.instance);
            return result.instance.exports;
        } catch (e) {
            console.error("[NEXUS_WASM] instantiateWasm FAILED", e);
            activateSafeFallback("Loader Failure: " + e.message);
            return {};
        }
    };

    console.log("[NEXUS_WASM] Forensic Patch Active.");
})();
