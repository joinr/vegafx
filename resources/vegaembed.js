var spec = "THE-SPEC";
var noisy = true;
var options = {};

var currentview;
var currenturl;

function notify(msg){
if (noisy == true){
  alert(msg);
  }
}

function ready(v) {
currentview = v;
notify("chart-ready");
}

function imageReady(url){
currenturl = url;
notify(url);
}

vegaEmbed('#vis', spec, options).then(function(result) {
   ready(result.view);
   // Access the Vega view instance (https://vega.github.io/vega/docs/api/view/) as result.view
 }).catch(console.error);

function getSVG(){
currentview.toSVG().then(function(url){
  imageReady(url);
  })
}

function ensureBackground(){
if (currentview.background() == null){
 currentview.background(options.background);
 }
}

function getImage(format){
if (format == 'svg'){
   getSVG();
} else {
ensureBackground();
currentview.toImageURL(format).then(function(url){
  imageReady(url);
  })
 }
}


