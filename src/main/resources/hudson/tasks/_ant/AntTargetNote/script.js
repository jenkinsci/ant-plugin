(function() {
    // created on demand
    var outline = null;
    var loading = false;

    var queue = []; // ant targets are queued up until we load outline.

    function loadOutline() {
        if (outline!=null)  return false;   // already loaded

        if (!loading) {
            loading = true;
            fetch(rootURL + "/descriptor/hudson.tasks._ant.AntTargetNote/outline", {
                method: "post",
                headers: crumb.wrap({}),
            }).then(function(rsp) {
                if (rsp.ok) {
                    rsp.text().then((responseText) => {
                        document.getElementById("side-panel").insertAdjacentHTML("beforeend", responseText);
                        outline = document.getElementById("console-outline-body");
                        loading = false;
                        queue.forEach(handle);
                    });
                }
            });
        }
        return true;
    }

    function handle(e) {
        if (loadOutline()) {
            queue.push(e);
        } else {
            var id = "ant-target-"+(iota++);
            outline.appendChild(parseHtml("<li><a href='#"+id+"'>"+e.innerHTML+"</a></li>"))

            if (document.all)
                e.innerHTML += '<a name="' + id + '"/>';  // IE8 loses "name" attr in appendChild
            else {
                var a = document.createElement("a");
                a.setAttribute("name",id);
                e.appendChild(a);
            }
        }
    }

    Behaviour.register({
        // insert <a name="..."> for each Ant target and put it into the outline
        "b.ant-target" : function(e) {
            handle(e);
        }
    });
}());
