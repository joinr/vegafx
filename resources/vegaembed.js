var spec = "THE-SPEC";
var noisy = true;

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

vegaEmbed('#vis', spec, {"actions":false}).then(function(result) {
   ready(result.view);
   // Access the Vega view instance (https://vega.github.io/vega/docs/api/view/) as result.view
 }).catch(console.error);

function getSVG(){
currentview.toSVG().then(function(url){
  imageReady(url);
  })
}

function getImage(format){
if (format == 'svg'){
   getSVG();
} else {
currentview.toImageURL(format).then(function(url){
  imageReady(url);
  })
 }
}


