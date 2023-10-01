function normalizeUrl(str) {
  return ('' + str).replace(/#.*$/, '').replace(/\/+$/, '');
}

$(() => {
	//console.warn('ready!')
	$('h1, h2, h3, h4, h5, h6').click((e) => {
		document.location.hash = $(e.target).attr('id')
		//console.warn()
	})
})

// This is a functions that scrolls to #{blah}link
function goToByScrollCenter(query){
  $('#sidebar-wrapper').animate({
      scrollTop: $(query).offset().top - $('#sidebar-wrapper').height() / 2
  }, 0);
}

function updateSidebar(scroll) {
  $('#searchq').val('');
  let normalizedNavigatorUrl = normalizeUrl(document.location.href);
  //console.log('aaaaaa');
  $(".sidebar-nav a").each(function() {
    let thisNormalizedUrl = normalizeUrl(this.href)
    var active = normalizedNavigatorUrl == thisNormalizedUrl;
    //console.log(normalizedNavigatorUrl, thisNormalizedUrl, this.href);
    $(this).toggleClass('active', active);
    if (scroll && active) {
      goToByScrollCenter(this);
    }
  });
}

$(document).ready(function() {
  updateSidebar(true);
});

/*

function getQueryVariable(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) == variable) {
            return decodeURIComponent(pair[1]);
        }
    }
    //console.log('Query variable %s not found', variable);
    return undefined;
}


function fixExternalLinksTarget(element) {
  var base = document.location.protocol + '//' + document.location.host;
  $(element).find('a').each(function() {
    var internalLink = (this.href.indexOf(base) == 0);
    if (!internalLink) {
      $(this).attr('target', '_blank');
    }
  });
}

function refreshDisqus(url) {
  if (typeof DISQUS != "undefined") {
    DISQUS.reset({
      reload: true,
      config: function () {
        this.page.identifier = url;
        this.page.url = url;
      }
    });
  }
}

function changeToPage(newurl, gotosidebar) {
  $("#page-content").load(newurl + " #page-content", function(html) {
    try {
      var newhtml = $(html);
      var canonicalUrl = newhtml.filter('link[rel="canonical"]').attr('href');
      document.title = newhtml.filter('title').text();
      fixExternalLinksTarget('#page-content');
      $(document.body).animate({scrollTop: 0}, 'fast');

      refreshDisqus(canonicalUrl);

    } catch (e) {
      console.error(e);
    }
  });

  updateSidebar(gotosidebar);

  return false;
}

$(document).ready(function() {
  updateSidebar(true);
  fixExternalLinksTarget(document);

  var q = getQueryVariable('q');
  if (q !== undefined) {
    $('#searchq').val(q);
  }

  $(".sidebar-nav a").click(function() {
    if (this.target == "_blank") return true;

    var newurlBase = this.href;
    var newurl = newurlBase.replace(/^https?:\/\//, '//')
    if (newurlBase.substr(0, 8) == 'https://') {
      document.location.href = newurlBase;
      return true;
    }
    console.log('changeTo:', newurlBase, newurl);
    history.pushState({ url: newurl }, "my page", newurl);
    return changeToPage(newurl, false);
  });

  $(window).on('popstate', function(e) {
    //console.log(this);
    //console.log(e);
    //console.log();
    e.preventDefault();
    //return changeToPage(e.originalEvent.state.url);
    return changeToPage(document.location.href, true);
  });
});
*/