// Script to reset animation so that it can run again
// This has to be a web worker because it involves setTimeout (sleep)
// and it will hold-up the gui otherwise
// Author: Aaron Yerke
setTimeout(function(){
  postMessage('terminate_worker');
}, 10000);
