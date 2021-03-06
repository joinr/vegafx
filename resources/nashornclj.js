//We just want a simple way to interop with
//js objects from clojure...
var fn = Java.type ("clojure.lang.IFn");

var compiler = Java.type("clojure.lang.Compiler");
var RT = Java.type("clojure.lang.RT");

function evalClojure(form){
    return compiler.eval(form);
}

function readClojure(txt){
    return RT.readString(txt);
}

function clojurePromise(){
    return evalClojure(readClojure("(promise)"));
}

//we can communicate using promises...
function deliver(p, result){
    p.invoke(result);
}

//vega3
function specToView(spec){
    var view = new vega.View(vega.parse(spec))
        .renderer('none')
        .initialize();
    return view;
}

function viewToSVG(view){
    var result = clojurePromise();
    view.toSVG()
        .then(function(svg) {
            deliver(result, svg);
        })
        .catch(function(err) {
            deliver(result,'<svg>err</svg>');
            console.error(err); });
    return result;
}

//vg.parse.spec(spec, function(chart) {
//    var view = chart({ renderer: "canvas" })
//        .update();

//    var canvas = view.canvas();
    // do something with the node-canvas instance...
//}
