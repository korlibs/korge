"use strict";
Map.prototype.map = (function (gen) {
    const out = [];
    for (const [key, value] of this.entries()) {
        out.push(gen(key, value));
    }
    return out;
});
Number.prototype.mod = function (n) {
    return ((this % n) + n) % n;
};
Array.prototype.clear = function () {
    this.length = 0;
};
Array.prototype.unique = (function () {
    const set = new Set();
    const out = [];
    for (const it of this) {
        if (set.has(it))
            continue;
        set.add(it);
        out.push(it);
    }
    return out;
});
Array.prototype.sorted = (function () {
    const array = this.slice();
    array.sort();
    return array;
});
Array.prototype.sortedBy = (function (gen) {
    const array = this.slice();
    array.sortBy(gen);
    return array;
});
Array.prototype.sortBy = (function (gen) {
    this.sort((a, b) => {
        const aa = gen(a);
        const bb = gen(b);
        if (aa < bb)
            return -1;
        if (aa > bb)
            return +1;
        return 0;
    });
});
Array.prototype.any = (function (func) {
    for (const item of this)
        if (func(item))
            return true;
    return false;
});
Array.prototype.all = (function (func) {
    for (const item of this)
        if (!func(item))
            return false;
    return true;
});
Array.prototype.filterUpTo = (function (maxItems, func) {
    const out = [];
    for (const item of this) {
        if (func(item)) {
            out.push(item);
            if (out.length >= maxItems)
                break;
        }
    }
    return out;
});
Array.prototype.groupBy = (function (gen) {
    const out = new Map();
    for (const item of this) {
        const key = gen(item);
        if (!out.has(key))
            out.set(key, []);
        out.get(key).push(item);
    }
    return out;
});
const replacements = new Map();
replacements.set("an", "a");
class TextProcessor {
    static tokenize(text) {
        const out = [];
        for (const it of text.toLowerCase().split(/\W+/g)) {
            const res = it.trim().replace(/c/g, 'k').replace(/l+/g, 'l').replace(/s+$/g, '');
            const res2 = replacements.get(res) || res;
            if (res2.length > 0) {
                out.push(res2);
            }
        }
        return out;
    }
}
class TokenizedText {
    constructor(text, words = TextProcessor.tokenize(text).unique()) {
        this.text = text;
        this.words = words;
        this.length = words.length;
    }
}
class QueryResult {
    constructor(text, words, section) {
        this.text = text;
        this.words = words;
        this.section = section;
        this.paragraph = section.matches(words) ?? section.matchesAnyOrder(words) ?? section.matchesAny(words);
        this.score = 0;
        const sectionFullTitle = section.titles.join(" ").toLowerCase();
        for (const word of words) {
            let wordInTitle = sectionFullTitle.toLowerCase().indexOf(word.toLowerCase()) >= 0;
            let scoreMultiplier = wordInTitle ? 2 : 1;
            if (wordInTitle) {
                this.score += 10;
            }
            if (section.words.has(word)) {
                this.score += Number(section.words.get(word)) * scoreMultiplier;
            }
        }
    }
    get doc() {
        return this.section.doc;
    }
}
class DocQueryResult {
    constructor(doc, results) {
        this.doc = doc;
        this.results = results;
        this.score = 0;
        results.sortBy(it => -it.score);
        this.score = 0;
        for (const result of results)
            this.score += result.score;
    }
}
class DocQueryResultResult {
    constructor(results = [], wordsInIndex = 0, iterations = 0, queryTimeMs = 0) {
        this.results = results;
        this.wordsInIndex = wordsInIndex;
        this.iterations = iterations;
        this.queryTimeMs = queryTimeMs;
    }
    append(other) {
        this.results.push(...other.results);
        this.wordsInIndex += other.wordsInIndex;
        this.iterations += other.iterations;
        this.queryTimeMs += other.queryTimeMs;
    }
}
class WordWithVariants {
    constructor(words) {
        this.words = words;
    }
}
class DocIndices {
    constructor() {
        this.indices = [];
    }
    addIndex(index) {
        this.indices.push(index);
    }
    query(text, maxResults = 7, debug = false) {
        const out = new DocQueryResultResult();
        for (const index of this.indices) {
            out.append(index.query(text, maxResults, debug));
        }
        return out;
    }
}
class DocIndex {
    constructor() {
        this.allWords = new Set();
        this.wordsToSection = new Map();
    }
    addWords(section, text) {
        const words = new Set(text.words);
        for (const word of words) {
            if (word.length == 0)
                continue;
            if (!this.wordsToSection.has(word))
                this.wordsToSection.set(word, new Set());
            this.wordsToSection.get(word).add(section);
            this.allWords.add(word);
        }
    }
    findWords(word) {
        let lcWord = word.toLowerCase();
        const out = [];
        for (const key of this.allWords.keys()) {
            if (key.indexOf(lcWord) >= 0) {
                const score = Math.abs(word.length - key.length);
                out.push([key, score]);
            }
        }
        out.sortBy(it => {
            return it[1];
        });
        return new WordWithVariants(out.map(it => it[0]).slice(0, 15));
    }
    getRepetition(word) {
        if (!this.wordsToSection.has(word))
            return 0;
        return this.wordsToSection.get(word).size;
    }
    getTotalDocuments(words) {
        let sum = 0;
        for (const word of words.words) {
            if (this.wordsToSection.has(word)) {
                sum += this.wordsToSection.get(word).size;
            }
        }
        return sum;
    }
    query(text, maxResults = 7, debug = false) {
        const time0 = Date.now();
        const tokenizedText = new TokenizedText(text).words;
        let allWordsSep = tokenizedText.map(it => this.findWords(it));
        if (debug)
            console.info(JSON.stringify(allWordsSep), tokenizedText);
        if (allWordsSep.length == 0)
            return new DocQueryResultResult([], this.wordsToSection.size);
        let intersectionSections = new Set();
        let exploredSections = new Set();
        const allWordsSepSorted = allWordsSep.sortedBy(it => this.getTotalDocuments(it));
        for (const searchWord of allWordsSepSorted[0].words) {
            const sectionsToSearch = [...(this.wordsToSection.get(searchWord) || [])];
            const toExploreSections = [];
            for (const section of sectionsToSearch) {
                if (exploredSections.has(section))
                    continue;
                exploredSections.add(section);
                toExploreSections.push(section);
            }
            const intersectionSectionsPart = [...toExploreSections]
                .filterUpTo(maxResults * 5, (section) => {
                return tokenizedText
                    .all((token) => {
                    let words = this.findWords(token).words;
                    const res = words.any((word) => section.hasWord(word));
                    if (!res)
                        return false;
                    return true;
                });
            });
            for (const part of intersectionSectionsPart) {
                intersectionSections.add(part);
            }
            if (intersectionSections.size >= maxResults * 5) {
                break;
            }
        }
        const results = [...intersectionSections]
            .map(it => new QueryResult(text, tokenizedText, it))
            .sortedBy(it => -it.score)
            .slice(0, maxResults)
            .groupBy(it => it.doc)
            .map((key, value) => new DocQueryResult(key, value))
            .sortedBy(it => -it.score);
        const time1 = Date.now();
        return new DocQueryResultResult(results, this.wordsToSection.size, 0, time1 - time0);
    }
}
class DocParagraphResult {
    constructor(paragraph, index, count) {
        this.paragraph = paragraph;
        this.index = index;
        this.count = count;
        this.words = paragraph.words.slice(index, index + count);
    }
}
var DocParagraphKind;
(function (DocParagraphKind) {
    DocParagraphKind[DocParagraphKind["TEXT"] = 0] = "TEXT";
    DocParagraphKind[DocParagraphKind["PRE"] = 1] = "PRE";
    DocParagraphKind[DocParagraphKind["TITLE"] = 2] = "TITLE";
    DocParagraphKind[DocParagraphKind["SUBTITLE"] = 3] = "SUBTITLE";
})(DocParagraphKind || (DocParagraphKind = {}));
class DocParagraph {
    constructor(texts, kind, scoreMultiplier) {
        this.texts = texts;
        this.kind = kind;
        this.scoreMultiplier = scoreMultiplier;
    }
    get text() { return this.texts.text; }
    get words() { return this.texts.words; }
    matchesWord(word, origin) {
        return origin.toLowerCase().indexOf(word.toLowerCase()) >= 0;
    }
    matches(words) {
        if (words.length == 0)
            return null;
        for (let n = 0; n < this.words.length - words.length + 1; n++) {
            let matches = true;
            for (let m = 0; m < words.length; m++) {
                if (!this.matchesWord(words[m], this.words[n + m])) {
                    matches = false;
                    break;
                }
            }
            if (matches)
                return new DocParagraphResult(this, n, words.length);
        }
        return null;
    }
    matchesAnyOrder(words) {
        for (const word of words) {
            if (!this.words.any(it => this.matchesWord(word, it)))
                return null;
        }
        return new DocParagraphResult(this, 0, this.words.length);
    }
    matchesAny(words) {
        for (const word of words) {
            if (this.words.any(it => this.matchesWord(word, it)))
                return new DocParagraphResult(this, 0, this.words.length);
        }
        return null;
    }
}
class DocSection {
    constructor(doc, id, title, parentSection) {
        this.doc = doc;
        this.id = id;
        this.title = title;
        this.parentSection = parentSection;
        this.words = new Map();
        this.paragraphs = [];
        this.titles = [];
        this.image = null;
        if (parentSection) {
            this.titles = [...parentSection.titles, title];
        }
        else {
            this.titles = (title.length) ? [title] : [];
        }
    }
    get anyImage() {
        return this.image || this.parentSection?.anyImage;
    }
    hasWord(word) {
        if (this.words.has(word))
            return true;
        for (const w of this.words.keys()) {
            if (w.indexOf(word) >= 0)
                return true;
        }
        return false;
    }
    addText(text, kind, scoreMultiplier) {
        if (text.length == 0)
            return;
        this.paragraphs.push(new DocParagraph(text, kind, scoreMultiplier));
        this.doc.index.addWords(this, text);
        for (const word of text.words) {
            if (!this.words.has(word))
                this.words.set(word, 0);
            this.words.set(word, this.words.get(word) + 1);
        }
    }
    addRawText(text, kind, scoreMultiplier) {
        this.addText(new TokenizedText(text), kind, scoreMultiplier);
    }
    matches(words) {
        for (const p of this.paragraphs) {
            const result = p.matches(words);
            if (result)
                return result;
        }
        return null;
    }
    matchesAnyOrder(words) {
        for (const p of this.paragraphs) {
            const result = p.matchesAnyOrder(words);
            if (result)
                return result;
        }
        return null;
    }
    matchesAny(words) {
        if (this.paragraphs.length == 0)
            return null;
        for (let n = 1; n < this.paragraphs.length; n++) {
            const p = this.paragraphs[n];
            const result = p.matchesAny(words);
            if (result)
                return result;
        }
        return this.paragraphs[0].matchesAny(words);
    }
    addImage(src) {
        if (!this.image) {
            this.image = src;
        }
    }
}
class Doc {
    constructor(index, url, baseUrl) {
        this.index = index;
        this.url = url;
        this.baseUrl = baseUrl;
        this.title = '';
        this.sections = [];
        this.fullUrl = `${baseUrl}${url}`;
    }
    createSection(id, title, parentSection) {
        let docSection = new DocSection(this, id, title, parentSection);
        this.sections.push(docSection);
        return docSection;
    }
}
class DocIndexer {
    constructor(index, url, baseUrl) {
        this.doc = new Doc(index, url, baseUrl);
        this.section = this.doc.createSection("", "", null);
        this.hSections = [this.section, this.section];
    }
    getHNum(tagName) {
        switch (tagName) {
            case "h1": return 1;
            case "h2": return 2;
            case "h3": return 3;
            case "h4": return 4;
            case "h5": return 5;
            case "h6": return 6;
            case "h7": return 7;
            default: return -1;
        }
    }
    index(element) {
        const id = element.getAttribute("id");
        const children = element.children;
        const tagName = element.tagName.toLowerCase();
        if (id != null) {
            const headerNum = this.getHNum(tagName);
            const textContent = element.textContent || "";
            this.section = this.doc.createSection(id, textContent, this.hSections[headerNum - 1]);
            this.section.addRawText(this.doc.title, DocParagraphKind.TITLE, 10.0);
            for (const title of this.section.titles) {
                this.section.addRawText(title, DocParagraphKind.SUBTITLE, 2.0);
            }
            this.section.addRawText(textContent, DocParagraphKind.TEXT, 1.0);
            if (headerNum >= 0) {
                this.hSections[headerNum] = this.section;
            }
        }
        if (tagName == 'title') {
            this.doc.title = element.textContent || "";
        }
        if (tagName == 'pre') {
            for (const line of (element.textContent || "").split(/\n/g)) {
                this.section.addRawText(line, DocParagraphKind.PRE, 0.9);
            }
        }
        else if (children.length == 0 || tagName == 'p' || tagName == 'code') {
            this.section.addRawText(element.textContent || "", DocParagraphKind.TEXT, 1.0);
            this.indexParagraph(element);
        }
        else {
            for (let n = 0; n < children.length; n++) {
                const child = children[n];
                this.index(child);
            }
        }
    }
    indexParagraph(element) {
        const tagName = element.tagName.toLowerCase();
        const children = element.children;
        if (tagName == 'img') {
            this.section.addImage(element.src);
        }
        for (let n = 0; n < children.length; n++) {
            const child = children[n];
            this.indexParagraph(child);
        }
    }
}
async function fetchParts(allLink) {
    const time0 = Date.now();
    let response = await fetch(allLink);
    let text = await response.text();
    const time1 = Date.now();
    console.log(`Fetched '${allLink}' in`, time1 - time0);
    return text.split("!!!$PAGE$!!!");
}
function createIndexFromParts(parts, baseUrl) {
    const time0 = Date.now();
    const parser = new DOMParser();
    const index = new DocIndex();
    for (const part of parts) {
        const breakPos = part.indexOf("\n");
        if (breakPos < 0)
            continue;
        const url = part.substr(0, breakPos);
        const content = part
            .substr(breakPos + 1)
            .replace(/{%\s*include\s*(.*?)\s*%}/g, '');
        const xmlDoc = parser.parseFromString(content, "text/html");
        const baseElem = xmlDoc.createElement("base");
        baseElem.href = url;
        xmlDoc.head.appendChild(baseElem);
        const indexer = new DocIndexer(index, url, baseUrl);
        indexer.index(xmlDoc.documentElement);
    }
    const time1 = Date.now();
    console.log("Created index in", time1 - time0);
    return index;
}
async function getIndex(allLink, baseUrl) {
    const parts = await fetchParts(allLink);
    return createIndexFromParts(parts, baseUrl);
}
async function getIndexOnce(allLink, name, baseUrl) {
    var _a;
    let awindow = window;
    awindow.searchIndexPromiseCache || (awindow.searchIndexPromiseCache = {});
    (_a = awindow.searchIndexPromiseCache)[name] || (_a[name] = getIndex(allLink, baseUrl));
    return await awindow.searchIndexPromiseCache[name];
}
HTMLElement.prototype.createChild = (function (tagName, gen) {
    const element = document.createElement(tagName);
    if (gen) {
        gen(element);
    }
    this.appendChild(element);
    return element;
});
async function newSearchHook(query) {
    console.log("ready");
    const searchBox = document.querySelector(query);
    const searchResults = document.createElement("div");
    searchResults.classList.add("newsearch");
    document.body.appendChild(searchResults);
    if (!searchBox)
        return;
    function updatePositions() {
        searchResults.style.position = 'fixed';
        searchResults.style.left = `${searchBox?.offsetLeft}px`;
        searchResults.style.top = `${searchBox.offsetTop + searchBox.offsetHeight + 2}px`;
    }
    updatePositions();
    const foundResults = [];
    let selectedIndex = 0;
    function setSelectedResult(newIndex = selectedIndex) {
        for (const result of foundResults) {
            result.classList.remove('active');
        }
        selectedIndex = newIndex;
        let selectedItem = foundResults[selectedIndex];
        if (selectedItem) {
            selectedItem.classList.add('active');
        }
        return selectedItem;
    }
    function highlightText(node, text) {
        if (node.children.length == 0) {
            const textContent = node.textContent || "";
            node.innerHTML = textContent.replace(new RegExp(text, "gi"), (r) => {
                return `<span class="search-highlight">${r}</span>`;
            });
        }
        else {
            for (let n = 0; n < node.children.length; n++) {
                const child = node.children[n];
                highlightText(child, text);
            }
        }
    }
    let lastText = '';
    let lastTimeout = 0;
    searchBox.addEventListener("keydown", (e) => {
        clearTimeout(lastTimeout);
        switch (e.key) {
            case 'ArrowUp':
            case 'ArrowDown':
                {
                    e.preventDefault();
                    const up = e.key == 'ArrowUp';
                    const offset = up ? -1 : +1;
                    const element = setSelectedResult((selectedIndex + offset).mod(foundResults.length));
                    element?.scrollIntoView({ behavior: "smooth", block: "center" });
                    return;
                }
            case 'Enter':
                {
                    e.preventDefault();
                    const element = setSelectedResult();
                    if (element) {
                        element.click();
                    }
                    return;
                }
        }
    });
    searchBox.addEventListener("blur", (e) => {
        lastTimeout = setTimeout(() => {
            searchResults.classList.remove('search-show');
        }, 200);
    });
    searchBox.addEventListener("focus", (e) => {
        updatePositions();
        const currentText = searchBox.value;
        searchResults.classList.toggle('search-show', currentText != '');
    });
    searchBox.addEventListener("keyup", async (e) => {
        if (e.key == 'F5')
            return;
        const currentText = searchBox.value;
        searchResults.classList.toggle('search-show', currentText != '');
        if (lastText == currentText)
            return;
        searchResults.classList.add('search-loading');
        const indices = new DocIndices();
        indices.addIndex(await getIndexOnce("/all.html", 'main', ''));
        indices.addIndex(await getIndexOnce("https://store.korge.org/all.html", 'store', '/store_proxy/?url='));
        searchResults.classList.remove('search-loading');
        switch (e.key) {
            case 'ArrowUp':
            case 'ArrowDown':
                e.preventDefault();
                return;
        }
        searchResults.innerHTML = '';
        foundResults.clear();
        lastText = currentText;
        const debug = false;
        const results = indices.query(currentText, 7, debug);
        searchResults.classList.toggle('search-no-results', results.results.length == 0);
        let resultIndex = 0;
        const usedImages = new Set();
        for (const result of results.results) {
            searchResults.createChild("h2", (it) => {
                it.title = `Title: ${result.doc.title}, Score: ${result.score}`;
                if (debug) {
                    it.innerText = `${result.doc.title} (${result.score})`;
                }
                else {
                    it.innerText = `${result.doc.title}`;
                }
            });
            result.results.forEach((res) => {
                const index = resultIndex++;
                const section = res.section;
                const href = `${res.doc.fullUrl}#${section.id}`;
                const div = searchResults.createChild("a", (it) => {
                    it.href = href;
                    it.id = `result${index}`;
                    it.className = "block";
                    it.createChild("div", (it) => {
                        it.className = "section";
                        it.innerText = section.titles.join(" > ");
                        const sectionImage = section.anyImage;
                        if (sectionImage && !usedImages.has(sectionImage)) {
                            usedImages.add(sectionImage);
                            console.error("section.image", sectionImage, "in", res.doc.url);
                            it.createChild("img", (it) => {
                                it.src = sectionImage;
                                it.style.display = 'block';
                                it.style.maxHeight = '100px';
                            });
                        }
                    });
                    const isPre = res.paragraph?.paragraph?.kind == DocParagraphKind.PRE;
                    it.createChild(isPre ? "pre" : "div", (it) => {
                        it.className = "content";
                        it.innerText = res.paragraph?.paragraph?.text || "";
                    });
                });
                div.onmousedown = (e) => {
                    clearTimeout(lastTimeout);
                };
                div.onmousemove = (e) => {
                    setSelectedResult(index);
                };
                div.onmouseover = (e) => {
                };
                foundResults.push(div);
            });
        }
        highlightText(searchResults, new RegExp("(" + currentText.split(" ").join("|") + ")"));
        setSelectedResult(0);
    });
}
async function newSearchMain() {
    await newSearchHook("input#searchbox");
}
