/**
 * NexusTranslationPlugin.js
 * Hardened, High-Performance Translation Engine for Nexus Player
 * Supports RPG Maker MV & MZ. Trie-based matching + LRU Cache.
 */
(function() {
    if (window.__NexusTranslation) return;

    const CONFIG = {
        TRANSLATION_URL: 'https://nexus.local/translations.json',
        CACHE_LIMIT: 500, // Max strings in LRU cache
        MAX_RECURSION: 3
    };

    class TranslationTrie {
        constructor() {
            this.root = {};
            this.hasStrings = false;
        }

        insert(key, value) {
            let node = this.root;
            for (const char of key) {
                if (!node[char]) node[char] = {};
                node = node[char];
            }
            node._val = value;
            this.hasStrings = true;
        }

        // Returns longest match found at start of text
        findLongest(text, start) {
            let node = this.root;
            let longestValue = null;
            let longestLength = 0;
            
            for (let i = start; i < text.length; i++) {
                const char = text[i];
                if (!node[char]) break;
                node = node[char];
                if (node._val !== undefined) {
                    longestValue = node._val;
                    longestLength = (i - start) + 1;
                }
            }
            return longestLength > 0 ? { val: longestValue, len: longestLength } : null;
        }
    }

    const Engine = {
        trie: new TranslationTrie(),
        regex: [],
        cache: new Map(), // LRU Cache
        enabled: true,
        loaded: false,

        async init() {
            try {
                const response = await fetch(CONFIG.TRANSLATION_URL);
                if (!response.ok) return;
                const data = await response.json();
                this.parse(data);
                this.loaded = true;
                console.log(`[NexusTranslation] Loaded ${Object.keys(data.strings || data || {}).length} strings`);
            } catch (e) {
                console.warn("[NexusTranslation] No translation file found or invalid format.");
            }
        },

        parse(data) {
            // Support V1 (Plain Object) and V2 (Extended)
            const strings = data.strings || data;
            const regexList = data.regex || [];

            for (const [key, val] of Object.entries(strings)) {
                this.trie.insert(key, val);
            }

            this.regex = regexList.map(r => ({
                pattern: new RegExp(r.pattern, 'g'),
                replace: r.replace
            }));
        },

        translate(text) {
            if (!this.enabled || !this.loaded || !text || typeof text !== 'string') return text;
            
            // 1. Check LRU Cache
            if (this.cache.has(text)) {
                const val = this.cache.get(text);
                this.cache.delete(text);
                this.cache.set(text, val); // Move to end
                return val;
            }

            let result = "";
            let i = 0;
            let modified = false;

            // 2. Trie Matching (O(N) walk)
            while (i < text.length) {
                const match = this.trie.findLongest(text, i);
                if (match) {
                    result += match.val;
                    i += match.len;
                    modified = true;
                } else {
                    result += text[i];
                    i++;
                }
            }

            // 3. Regex Matching
            for (const r of this.regex) {
                const prev = result;
                result = result.replace(r.pattern, r.replace);
                if (prev !== result) modified = true;
            }

            // 4. Update LRU Cache
            if (modified) {
                if (this.cache.size >= CONFIG.CACHE_LIMIT) {
                    const firstKey = this.cache.keys().next().value;
                    this.cache.delete(firstKey);
                }
                this.cache.set(text, result);
            }

            return result;
        }
    };

    // --- RPG Maker Hooks ---
    
    // Hook Bitmap (Canvas text)
    const _Bitmap_drawText = Bitmap.prototype.drawText;
    Bitmap.prototype.drawText = function(text, x, y, maxWidth, lineHeight, align) {
        arguments[0] = Engine.translate(text);
        _Bitmap_drawText.apply(this, arguments);
    };

    const _Bitmap_measureTextWidth = Bitmap.prototype.measureTextWidth;
    Bitmap.prototype.measureTextWidth = function(text) {
        arguments[0] = Engine.translate(text);
        return _Bitmap_measureTextWidth.apply(this, arguments);
    };

    // Hook Window system (MV/MZ)
    const hookWindowBase = (proto) => {
        if (!proto) return;
        const _convert = proto.convertEscapeCharacters;
        if (_convert) {
            proto.convertEscapeCharacters = function(text) {
                arguments[0] = Engine.translate(text);
                return _convert.apply(this, arguments);
            };
        }
    };

    // MV & MZ have Window_Base as root for text processing
    if (window.Window_Base) hookWindowBase(Window_Base.prototype);
    
    const _Window_Message_startMessage = Window_Message.prototype.startMessage;
    Window_Message.prototype.startMessage = function() {
        if ($gameMessage) {
            const texts = $gameMessage._texts;
            for (let i = 0; i < texts.length; i++) {
                texts[i] = Engine.translate(texts[i]);
            }
        }
        _Window_Message_startMessage.apply(this, arguments);
    };

    const _Window_Command_addCommand = Window_Command.prototype.addCommand;
    Window_Command.prototype.addCommand = function(name, symbol, enabled, ext) {
        arguments[0] = Engine.translate(name);
        _Window_Command_addCommand.apply(this, arguments);
    };

    if (window.Window_BattleLog) {
        const _addText = Window_BattleLog.prototype.addText;
        Window_BattleLog.prototype.addText = function(text) {
            arguments[0] = Engine.translate(text);
            _addText.apply(this, arguments);
        }
    }

    // --- Nexus Bridge Export ---
    window.__NexusTranslation = Engine;
    window.__nexus.translation = {
        getStatus: () => ({
            loaded: Engine.loaded,
            enabled: Engine.enabled,
            cacheSize: Engine.cache.size,
            trieActive: Engine.trie.hasStrings
        }),
        setEnabled: (val) => Engine.enabled = !!val,
        reload: () => {
            Engine.cache.clear();
            Engine.trie = new TranslationTrie();
            Engine.init();
        }
    };

    Engine.init();
})();
