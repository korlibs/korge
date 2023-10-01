function replaceItemBody($item, source, url) {
    const canvas = document.createElement('canvas');
    nomnoml.draw(canvas, source);
    $(canvas).css("max-width", "100%");
    $(canvas).css("height", "auto");
    $item.css("display", "block");
    if (url !== undefined) {
        const a = document.createElement('a');
        $(a).attr("href", url).attr("target", "_blank");
        a.appendChild(canvas);
        $item.replaceWith(a);
    } else {
        $item.replaceWith(canvas);
    }
}

async function replaceItem($item, url) {
    const data = await fetch(url);
    const text = await data.text();
    replaceItemBody($item, text, url)
}

window.addEventListener('load', function() {
    //console.log('loaded!', $);
    $('pre .language-nomnoml').each(function(index, item) {
        const $item = $(item);
        const $parent = $item.parent();
        replaceItemBody($parent, $item.text());
    });
});
