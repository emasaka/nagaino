(function() {

    var url_re = new RegExp('^(?:http://t\.co/|http://tinyurl\.com/|http://is\.gd/|http://ff\.im/|http://j\.mp/|http://goo\.gl/|http://tr\.im/|http://short\.to/|http://ow\.ly/|http://u\.nu/|http://twurl\.nl/|http://icio\.us/|http://htn\.to/|http://cot\.ag/|http://ht\.ly/|http://p\.tl/|http://url4\.eu/|http://ur1\.ca/|http://bit\.ly/|http://amzn\.to/|http://s\.nikkei\.com/|http://t\.asahi\.com/|http://nyti\.ms/|http://tcrn\.ch/|http://huff\.to/|http://on\.cnn\.com/)');

    function hash_keys(h) {
        var ary = [];
        for (var key in h) {
            ary.push(key);
        }
        return ary;
    }

    function gather_urls() {
        var urls_h = {};
        var u;
        var elms = document.getElementsByTagName('a');
        for (var i = 0, len = elms.length; i < len; i++) {
            var elm = elms[i];
            if (elm && (u = elm.href) && url_re.test(u)) {
                urls_h[u] = true;
            }
        }
        return hash_keys(urls_h);
    }

    function replace_element_url(elm, oldurl, newurl) {
        var txt = elm.innerHTML;
        var eu;
        elm.href = newurl;
        if ((txt == oldurl) ||
            ((eu = elm.getAttribute('data-expanded-url')) &&
             (('http://' + txt) ==  eu) )) {
            elm.innerHTML = newurl;
        }
    }

    function replace_urls(urls_hash) {
        var elm, u, u2;
        var elms = document.getElementsByTagName('a');
        for (var i = 0, len = elms.length; i < len; i++) {
            var elm = elms[i];
            if (elm && (u = elm.href) && (u2 = urls_hash[u])) {
                replace_element_url(elm, u, u2);
            }
        }
    }

    function call_nagaio(urls) {
        var xhr = window.XDomainRequest ? new XDomainRequest() :
                                          new XMLHttpRequest() ;
        xhr.open('POST', 'http://nagaino.herokuapp.com/api/v0/expandText');
        if (xhr.setRequestHeader) {
            xhr.setRequestHeader('Content-Type',
                                 'application/x-www-form-urlencoded' );
        } else {
            // XXX: for IE, but this doesn't work
            xhr.contentType = 'application/x-www-form-urlencoded';
        }
        xhr.onreadystatechange = function() {
            var txt;
            if ((xhr.readyState === 4) && (xhr.status == 200) &&
                (txt = xhr.responseText) ) {
                var rtn = JSON.parse(txt);
                replace_urls(rtn['data']['expand']);
            }
        };
        xhr.send('format=json_simple&shortUrls=' + encodeURI(urls.join("\n")));
    }

    function expand_urls() {
        var urls = gather_urls();
        if (urls.length > 0) {
            call_nagaio(urls);
        }
    }

    expand_urls();

})()
