(function() {

    var url_re = /{{URL_RE}}/;
    var ng_url_re = /^http:\/\/t\.co\/.*[()]$/;

    // substitue of Object.keys()
    function hash_keys(h) {
        var ary = [];
        for (var key in h) {
            ary.push(key);
        }
        return ary;
    }

    function tag_each(tag, callback) {
        var elms = document.getElementsByTagName(tag);
        for (var i = 0, len = elms.length; i < len; i++) {
            callback.apply(elms[i]);
        }
    }

    function gather_urls() {
        var urls_h = {};
        tag_each('a', function() {
            var u;
            if ((u = this.href) && url_re.test(u) && ! ng_url_re.test(u)) {
                urls_h[u] = true;
            }
        });
        return hash_keys(urls_h);
    }

    function replace_element_url(elm, oldurl, newurl) {
        var txt = elm.innerHTML;
        var txt2, eu;
        elm.href = newurl;
        if ((txt == oldurl) ||
            ((txt2 = 'http://' + txt) == oldurl) ||
            ((eu = elm.getAttribute('data-expanded-url')) &&
             (txt2 ==  eu) )) {
            elm.innerHTML = newurl;
        }
    }

    function replace_urls(urls_hash) {
        tag_each('a', function() {
            var u, u2;
            if ((u = this.href) && (u2 = urls_hash[u])) {
                replace_element_url(this, u, u2);
            }
        });
    }

    function call_nagaino(urls) {
        var endpoint = 'http://nagaino.herokuapp.com/api/v0/expandText';
        var xhr;
        function cbfunc() {
            var rtn = JSON.parse(xhr.responseText);
            replace_urls(rtn['data']['expand']);
        }
        if (window.XDomainRequest) {
            // XXX: for IE, but this doesn't work by lack of Content-Type
            xhr = new XDomainRequest();
            xhr.open('POST', endpoint);
            xhr.onload = cbfunc;
        } else {
            xhr = new XMLHttpRequest();
            xhr.open('POST', endpoint);
            xhr.setRequestHeader('Content-Type',
                                 'application/x-www-form-urlencoded' );
            xhr.onreadystatechange = function() {
                if ((xhr.readyState === 4) && (xhr.status == 200)) {
                    cbfunc();
                }
            };
        }
        xhr.send('format=json_simple&shortUrls=' + encodeURI(urls.join("\n")));
    }

    function expand_urls() {
        var urls = gather_urls();
        if (urls.length > 0) {
            call_nagaino(urls);
        }
    }

    expand_urls();

})()
